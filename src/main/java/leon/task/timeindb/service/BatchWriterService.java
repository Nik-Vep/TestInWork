package leon.task.timeindb.service;

import leon.task.timeindb.entity.TimeEntity;
import leon.task.timeindb.repository.TimeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BatchWriterService {

    private final TimeRepository repo;

    @Transactional(noRollbackFor = DataIntegrityViolationException.class)
    public void insertBatch(List<Instant> batch) {
        int inserted = 0; // Счетчик успешно вставленных записей
        int duplicates = 0;   // Счетчик дубликатов (нарушение уникальности)

        for (Instant ts : batch) { // Итерация по всем элементам переданного списка
            try {
                // Создание новой сущности с временной меткой и сохранение в БД
                // null - ID будет сгенерирован автоматически
                repo.save(new TimeEntity(null, ts));
                inserted++; // Увеличение счетчика успешных вставок


            } catch (DataIntegrityViolationException e) {
                // Обработка нарушения целостности данных (дубликат уникального поля)
                // Увеличение счетчика дубликатов
                duplicates++;
            }
        }
        log.debug("Батч записан: всего={}, вставлено={}, дублей={}", batch.size(), inserted, duplicates);
    }
}

