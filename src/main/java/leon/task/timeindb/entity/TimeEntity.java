package leon.task.timeindb.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.time.Instant;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, // NOT NULL, гарантирует, что у каждой записи будет время создания
            updatable = false, // Запрет на обновление после создания, защита от случайного изменения
            unique = true)  // Уникальность значения в таблице, предотвращает дублирование временных меток
    // Хранение момента времени (timestamp) в БД
    private Instant createdAt;
}
