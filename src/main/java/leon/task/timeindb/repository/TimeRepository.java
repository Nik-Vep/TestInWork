package leon.task.timeindb.repository;

import leon.task.timeindb.entity.TimeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TimeRepository extends JpaRepository<TimeEntity, Long> {
}
