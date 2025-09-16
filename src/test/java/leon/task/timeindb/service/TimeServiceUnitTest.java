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

@RunWith(MockitoJUnitRunner.class)
public class TimeServiceUnitTest {

    @Mock
    private TimeRepository repo;
    @Mock private BatchWriterService writer;

    @Test
    public void generateTick_dropsOldestWhenBufferFull() {
        LinkedBlockingDeque<Instant> smallBuffer = new LinkedBlockingDeque<>(2);
        TimeService svc = new TimeService(repo, writer, smallBuffer);

        svc.generateTick();
        svc.generateTick();
        svc.generateTick();

        assertThat(smallBuffer.size()).isEqualTo(2);
    }

    @Test
    public void backgroundFlush_movesBatchBackOnError() {
        LinkedBlockingDeque<Instant> smallBuffer = new LinkedBlockingDeque<>(10);
        TimeService svc = new TimeService(repo, writer, smallBuffer);

        smallBuffer.offer(Instant.parse("2025-09-15T00:00:00Z"));
        smallBuffer.offer(Instant.parse("2025-09-15T00:00:01Z"));

        doThrow(new RuntimeException("DB down")).when(writer).insertBatch(anyList());

        svc.backgroundFlush();

        assertThat(smallBuffer.size()).isGreaterThanOrEqualTo(2);
    }
}
