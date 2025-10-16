package leon.task.timeindb.integration;

import leon.task.timeindb.repository.TimeRepository;
import leon.task.timeindb.service.TimeService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(SpringRunner.class) // Запускает тест с поддержкой Spring контекста
@SpringBootTest(properties = { // Загружает полный Spring Boot контекст с указанными свойствами
        "spring.task.scheduling.enabled=false", // Отключает планировщик задач Spring (чтобы @Scheduled методы не запускались автоматически)
        "spring.jpa.hibernate.ddl-auto=create-drop", // Настройка Hibernate: создает таблицы при старте и удаляет при завершении
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL", // URL для in-memory H2 базы данных в режиме PostgreSQL
        "spring.datasource.driverClassName=org.h2.Driver", // Драйвер для H2 базы данных
        "spring.datasource.username=sa",  // Имя пользователя для H2 (стандартное)
        "spring.datasource.password=",  // Пароль для H2 (пустой)
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect", // Диалект Hibernate для H2 базы данных
        "app.buffer-capacity=3", // Кастомное свойство: емкость буфера для временных меток
        "app.time-zone=UTC"  // Кастомное свойство: временная зона приложения
})
@AutoConfigureMockMvc // Автоматическая настройка MockMvc для тестирования веб-слоя
public class AppIntegrationTest {

    @Autowired private MockMvc mvc;  // Внедрение MockMvc для тестирования HTTP endpoints
    @Autowired private TimeService timeService; // Внедрение сервиса для тестирования бизнес-логики
    @Autowired private TimeRepository repo; // Внедрение репозитория для проверки состояния БД

    @Test // Тестовый метод для сквозного (end-to-end) тестирования
    public void ticks_endpoint_e2e() throws Exception {
        timeService.generateTick(); // Генерация первой временной метки
        timeService.generateTick(); // Генерация второй временной метки
        timeService.backgroundFlush();  // Принудительная запись данных из буфера в БД

        assertThat(repo.count()).isGreaterThan(0); // Проверка, что в БД есть хотя бы одна запись

        mvc.perform(get("/ticks")) // Выполнение HTTP GET запроса к endpoint /ticks
                .andExpect(status().isOk()) // Проверка, что статус ответа 200 OK
                .andExpect(content().contentType("application/json"));  // Проверка, что Content-Type соответствует application/json
    }
}
