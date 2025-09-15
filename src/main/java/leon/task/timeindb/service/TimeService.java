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

    private final TimeRepository repo;
    private final BatchWriterService writer;


    private final LinkedBlockingDeque<Instant> buffer = new LinkedBlockingDeque<>(100_000);

    private volatile boolean dbUp = true;
    private volatile long lastReconnectTryMs = 0L;
    private final AtomicLong dropped = new AtomicLong();

    @Scheduled(fixedRate = 1000)
    public void generateTick() {
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        if (!buffer.offer(now)) {
            buffer.pollFirst();
            buffer.offer(now);
            long n = dropped.incrementAndGet();
            log.warn("Очередь на запись переполнена — удаляем самую старую метку. Потеряно с начала работы: {}", n);
        }
    }

    @Scheduled(fixedDelay = 200)
    public void backgroundFlush() {
        flushIfPossible();
    }

    @Scheduled(fixedDelay = 1000)
    public void reconnectLoop() {
        if (dbUp) return;
        long now = System.currentTimeMillis();
        if (now - lastReconnectTryMs < TimeUnit.SECONDS.toMillis(5)) return;

        lastReconnectTryMs = now;
        try {
            repo.count();
            dbUp = true;
            log.info("Соединение с БД восстановлено. Начинаем дозапись {} накопленных меток.", buffer.size());
            flushIfPossible();
        } catch (CannotCreateTransactionException | JDBCConnectionException | DataAccessException e) {
            log.warn("БД всё ещё недоступна. Следующая попытка через 5 секунд. Причина: {}", safeMsg(e));
        } catch (RuntimeException e) {
            log.warn("Неожиданная ошибка при проверке БД: {}. Повторим через 5 секунд.", safeMsg(e));
        }
    }

    private void flushIfPossible() {
        if (!dbUp) return;

        int drainedTotal = 0;
        while (true) {
            List<Instant> batch = drainUpTo(500);
            if (batch.isEmpty()) break;

            try {
                writer.insertBatch(batch);
                drainedTotal += batch.size();
            } catch (CannotCreateTransactionException | JDBCConnectionException | DataAccessException e) {
                rollbackBatchToBuffer(batch);
                dbUp = false;
                log.error("БД недоступна: {}. Переходим в офлайн-режим. Будем пытаться переподключиться каждые 5 секунд.",
                        safeMsg(e));
                break;
            } catch (RuntimeException e) {
                rollbackBatchToBuffer(batch);
                dbUp = false;
                log.error("Неожиданная ошибка при записи в БД: {}. Переходим в офлайн-режим.", safeMsg(e));
                break;
            }

            if (drainedTotal >= 2000) break;
        }
    }

    private List<Instant> drainUpTo(int max) {
        List<Instant> list = new ArrayList<>(Math.min(max, buffer.size()));
        buffer.drainTo(list, max);
        return list;
    }

    private void rollbackBatchToBuffer(List<Instant> batch) {
        for (int i = batch.size() - 1; i >= 0; i--) {
            buffer.offerFirst(batch.get(i));
        }
    }

    @PreDestroy
    public void onShutdown() {
        try {
            flushIfPossible();
            if (!buffer.isEmpty()) {
                log.warn("Завершение работы: в очереди осталось {} меток (не успели записать).", buffer.size());
            }
        } catch (Exception e) {
            log.warn("Ошибка при финальной дозаписи: {}", safeMsg(e));
        }
    }

    private String safeMsg(Throwable t) {
        String m = t.getMessage();
        return m == null ? t.getClass().getSimpleName() : m;
    }
}
