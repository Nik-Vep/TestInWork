package leon.task.timeindb.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;
import java.util.concurrent.LinkedBlockingDeque;

@Configuration
public class BufferConfig {

    @Bean
    public LinkedBlockingDeque<Instant> ticksBuffer(
            @Value("${app.buffer-capacity:100000}") int capacity) {
        return new LinkedBlockingDeque<>(capacity);
    }
}
