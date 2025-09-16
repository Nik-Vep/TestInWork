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

@RunWith(MockitoJUnitRunner.class)
public class BatchWriterServiceTest {

    @Mock
    private TimeRepository repo;
    @InjectMocks
    private BatchWriterService service;

    @Test
    public void insertBatch_ignoresDuplicates_andDoesNotThrow() {
        when(repo.save(any(TimeEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0))
                .thenThrow(new DataIntegrityViolationException("duplicate"))
                .thenAnswer(inv -> inv.getArgument(0));

        Instant t1 = Instant.parse("2025-09-15T00:00:00Z");
        Instant t2 = Instant.parse("2025-09-15T00:00:01Z");
        Instant t3 = Instant.parse("2025-09-15T00:00:02Z");

        service.insertBatch(List.of(t1, t2, t3));

        verify(repo, times(3)).save(any(TimeEntity.class));
        verifyNoMoreInteractions(repo);
    }
}