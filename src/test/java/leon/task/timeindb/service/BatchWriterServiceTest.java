package leon.task.timeindb.service;

import leon.task.timeindb.entity.TimeEntity;
import leon.task.timeindb.repository.TimeRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.dao.DataIntegrityViolationException;
import java.time.Instant;
import java.util.List;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)  // Запускает тест с поддержкой Mockito фреймворка
public class BatchWriterServiceTest {

    @Mock     // Создание мок-объекта для TimeRepository
    private TimeRepository repo;
    @InjectMocks    // Создание экземпляра тестируемого сервиса с внедренными моками
    private BatchWriterService service;

    @Test    // Тест проверяет, что метод корректно обрабатывает дубликаты и не выбрасывает исключение
    public void insertBatch_ignoresDuplicates_andDoesNotThrow() {
        when(repo.save(any(TimeEntity.class)))  // Настройка поведения мока: три последовательных вызова save с разным поведением
                .thenAnswer(inv -> inv.getArgument(0))  // Первый вызов: успешное сохранение - возвращает переданную сущность
                .thenThrow(new DataIntegrityViolationException("duplicate")) // Второй вызов: имитация нарушения уникальности - выбрасывает исключение
                .thenAnswer(inv -> inv.getArgument(0)); // Третий вызов: снова успешное сохранение

        // Создание тестовых временных меток
        Instant t1 = Instant.parse("2025-09-15T00:00:00Z");
        Instant t2 = Instant.parse("2025-09-15T00:00:01Z");
        Instant t3 = Instant.parse("2025-09-15T00:00:02Z");

        service.insertBatch(List.of(t1, t2, t3));  // Вызов тестируемого метода с списком из трех временных меток

        verify(repo, times(3)).save(any(TimeEntity.class)); // Проверка, что метод save был вызван ровно 3 раза (для каждой метки)
        verifyNoMoreInteractions(repo); // Проверка, что других взаимодействий с моком не было
    }
}