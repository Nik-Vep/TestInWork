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
            @Value("${app.buffer-capacity:100000}") int capacity) {  // Внедрение значения из настроек, по умолчанию 100000
        return new LinkedBlockingDeque<>(capacity);  // Создание очереди с указанной ёмкостью и возврат как бин
    }
}
