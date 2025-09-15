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
        int inserted = 0;
        int duplicates = 0;

        for (Instant ts : batch) {
            try {
                repo.save(new TimeEntity(null, ts));
                inserted++;
            } catch (DataIntegrityViolationException e) {
                duplicates++;
            }
        }
        log.debug("Батч записан: всего={}, вставлено={}, дублей={}", batch.size(), inserted, duplicates);
    }
}

