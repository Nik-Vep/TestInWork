package leon.task.timeindb.service;

import leon.task.timeindb.repository.TimeRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import java.time.Instant;
import java.util.concurrent.LinkedBlockingDeque;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;

@RunWith(MockitoJUnitRunner.class)  // Запускает тест с поддержкой Mockito фреймворк
public class TimeServiceUnitTest {

    @Mock // Создание мок-объекта для TimeRepository
    private TimeRepository repo;
    @Mock // Создание мок-объекта для BatchWriterService
    private BatchWriterService writer;

    @Test // Тест проверяет поведение при переполнении буфера
    public void generateTick_dropsOldestWhenBufferFull() {
        LinkedBlockingDeque<Instant> smallBuffer = new LinkedBlockingDeque<>(2); // Создание маленького буфера емкостью 2 элемента для тестирования граничных условий
        TimeService svc = new TimeService(repo, writer, smallBuffer); // Создание экземпляра сервиса с тестовым буфером и моками

        // Генерация трех временных меток (на одну больше, чем емкость буфера)
        svc.generateTick();
        svc.generateTick();
        svc.generateTick();

        // Проверка, что в буфере осталось ровно 2 элемента (самые новые)
        assertThat(smallBuffer.size()).isEqualTo(2);
    }

    @Test // Тест проверяет восстановление данных в буфер при ошибке записи
    public void backgroundFlush_movesBatchBackOnError() {
        LinkedBlockingDeque<Instant> smallBuffer = new LinkedBlockingDeque<>(10); // Создание буфера для тестирования
        TimeService svc = new TimeService(repo, writer, smallBuffer);  // Создание экземпляра сервиса

        // Добавление тестовых данных в буфер вручную
        smallBuffer.offer(Instant.parse("2025-09-15T00:00:00Z"));
        smallBuffer.offer(Instant.parse("2025-09-15T00:00:01Z"));

        // Настройка мока: при вызове insertBatch выбрасывается исключение
        doThrow(new RuntimeException("DB down")).when(writer).insertBatch(anyList());

        // Вызов метода, который пытается записать данные в БД
        svc.backgroundFlush();

        // Проверка, что данные вернулись в буфер после ошибки
        assertThat(smallBuffer.size()).isGreaterThanOrEqualTo(2);
    }
}
