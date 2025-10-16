package leon.task.timeindb.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import leon.task.timeindb.repository.TimeRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.hibernate.exception.JDBCConnectionException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import javax.annotation.PreDestroy;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
@Slf4j
public class TimeService {

    private final TimeRepository repo;  // Репозиторий для работы с БД
    private final BatchWriterService writer; // Сервис для пакетной записи
    private final LinkedBlockingDeque<Instant> buffer;   // Буфер-очередь для временных меток (потокобезопасная)

    private volatile boolean dbUp = true;  // Флаг доступности БД (volatile для visibility между потоками)
    private volatile long lastReconnectTryMs = 0L;  // Время последней попытки переподключения
    private final AtomicLong dropped = new AtomicLong();  // Счетчик потерянных меток (атомарный для thread-safety)

    @Scheduled(fixedRate = 1000)  // Запускается каждую секунду - генерирует новую временную метку
    public void generateTick() {
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS); // Получение текущего времени с округлением до секунд
        // Пытаемся добавить в очередь, если не получается - вытесняем самую старую
        if (!buffer.offer(now)) {
            buffer.pollFirst(); // Если очередь полна - удаляем самую старую метку из начала
            buffer.offer(now); // Добавляем новую метку
            long n = dropped.incrementAndGet(); // Увеличиваем счетчик потерянных меток и получаем новое значение
            log.warn("Очередь на запись переполнена — удаляем самую старую метку. Потеряно с начала работы: {}", n);
        }
    }

    @Scheduled(fixedDelay = 200)    // Частая фоновая запись - каждые 200мс
    public void backgroundFlush() {
        flushIfPossible();  // Вызов метода фоновой записи данных в БД
    }

    @Scheduled(fixedDelay = 1000)     // Проверка восстановления соединения с БД - каждую секунду
    public void reconnectLoop() {
        if (dbUp) return; // Если БД доступна - выходим
        long now = System.currentTimeMillis(); // Получение текущего времени в миллисекундах
        // Проверяем не чаще чем раз в 5 секунд
        if (now - lastReconnectTryMs < TimeUnit.SECONDS.toMillis(5)) return;

        lastReconnectTryMs = now; // Обновление времени последней попытки подключения
        try {
            repo.count(); // Простая проверка доступности БД
            dbUp = true; // Установка флага доступности БД
            log.info("Соединение с БД восстановлено. Начинаем дозапись {} накопленных меток.", buffer.size());
            flushIfPossible(); // Запуск процесса записи накопленных данных
        } catch (CannotCreateTransactionException | JDBCConnectionException | DataAccessException e) {
            log.warn("БД всё ещё недоступна. Следующая попытка через 5 секунд. Причина: {}", safeMsg(e));
        } catch (RuntimeException e) {
            log.warn("Неожиданная ошибка при проверке БД: {}. Повторим через 5 секунд.", safeMsg(e));
        }
    }

    // Основной метод записи данных из буфера в БД
    private void flushIfPossible() {
        if (!dbUp) return; // Проверка доступности БД перед записью

        int drainedTotal = 0; // Счетчик обработанных меток за текущую итерацию

        // Бесконечный цикл для обработки всех данных в буфере
        while (true) {
            List<Instant> batch = drainUpTo(500);  // Извлечение пачки данных из буфера (до 500 элементов)
            if (batch.isEmpty()) break; // Если буфер пуст - выход из цикла

            try {
                writer.insertBatch(batch); // Пакетная запись в БД
                drainedTotal += batch.size(); // Увеличение счетчика обработанных меток
            } catch (CannotCreateTransactionException | JDBCConnectionException | DataAccessException e) {
                rollbackBatchToBuffer(batch); // Возвращаем данные в буфер при ошибке
                dbUp = false; // Установка флага недоступности БД
                log.error("БД недоступна: {}. Переходим в офлайн-режим. Будем пытаться переподключиться каждые 5 секунд.",
                        safeMsg(e));
                break; // Выход из цикла при ошибке
            } catch (RuntimeException e) {
                rollbackBatchToBuffer(batch); // Возврат данных в буфер при неожиданных ошибках
                dbUp = false; // Установка флага недоступности БД
                log.error("Неожиданная ошибка при записи в БД: {}. Переходим в офлайн-режим.", safeMsg(e));
                break; // Выход из цикла при ошибке
            }
            // Ограничиваем максимальное количество за одну итерацию
            if (drainedTotal >= 2000) break;
        }
    }

    // Выборка элементов из очереди (неблокирующая)
    private List<Instant> drainUpTo(int max) {
        List<Instant> list = new ArrayList<>(Math.min(max, buffer.size())); // Создание списка с начальной емкостью (минимум из max и размера буфера)
        buffer.drainTo(list, max);   // Неблокирующее извлечение элементов из очереди в список
        return list; // Возврат извлеченных элементов
    }

    // Возврат данных в начало очереди при ошибках
    private void rollbackBatchToBuffer(List<Instant> batch) {
        for (int i = batch.size() - 1; i >= 0; i--) { // Обратный обход списка для сохранения исходного порядка
            buffer.offerFirst(batch.get(i)); // Добавление элемента в начало очереди
        }
    }

    @PreDestroy // Метод, выполняемый перед уничтожением бина Spring
    public void onShutdown() {
        try {
            flushIfPossible(); // Финальная попытка записать оставшиеся данные
            if (!buffer.isEmpty()) {   // Проверка наличия не записанных данных
                log.warn("Завершение работы: в очереди осталось {} меток (не успели записать).", buffer.size());
            }
        } catch (Exception e) {
            log.warn("Ошибка при финальной дозаписи: {}", safeMsg(e));
        }
    }

    // Вспомогательный метод для безопасного получения сообщения об ошибке
    private String safeMsg(Throwable t) {
        String m = t.getMessage(); // Получение сообщения об ошибке
        return m == null ? t.getClass().getSimpleName() : m; // Возврат сообщения или имени класса, если сообщение null
    }
}
