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
@RequiredArgsConstructor
public class TimeController {
    private final TimeRepository repo;

    @Value("${app.time-zone:UTC}")
    private ZoneId appZone;

    @GetMapping("/ticks")
    public ResponseEntity<List<String>> ticks() {
        var body = repo.findAll().stream()
                .map(e -> ZonedDateTime.ofInstant(e.getCreatedAt(), appZone).toString())
                .collect(Collectors.toList());
        return ResponseEntity.ok(body);
    }
}
