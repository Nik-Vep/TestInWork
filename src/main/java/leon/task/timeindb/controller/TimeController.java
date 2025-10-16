package leon.task.timeindb.controller;

import leon.task.timeindb.repository.TimeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Value;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor  // Аннотация Lombok - генерирует конструктор с обязательными полями (final)
public class TimeController {
    private final TimeRepository repo;

    // Загрузка значения из конфигурации с дефолтным значением "UTC"
    @Value("${app.time-zone:UTC}")
    private ZoneId appZone;

    @GetMapping("/ticks")
    public ResponseEntity<List<String>> ticks() {
        // Получение всех записей из БД, преобразование в stream
        var body = repo.findAll().stream()
                // Преобразование каждой сущности: конвертация Instant в ZonedDateTime, для отображения пользователю с учетом его региона
                .map(e -> ZonedDateTime.ofInstant(e.getCreatedAt(), appZone).toString())
                // Сбор результатов в List
                .collect(Collectors.toList());
        // Возврат HTTP 200 OK с телом в виде списка строк
        return ResponseEntity.ok(body);
    }
}
