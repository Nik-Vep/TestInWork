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

@RunWith(SpringRunner.class)
@SpringBootTest(properties = {
        "spring.task.scheduling.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "app.buffer-capacity=3",
        "app.time-zone=UTC"
})
@AutoConfigureMockMvc
public class AppIntegrationTest {

    @Autowired private MockMvc mvc;
    @Autowired private TimeService timeService;
    @Autowired private TimeRepository repo;

    @Test
    public void ticks_endpoint_e2e() throws Exception {
        timeService.generateTick();
        timeService.generateTick();
        timeService.backgroundFlush();

        assertThat(repo.count()).isGreaterThan(0);

        mvc.perform(get("/ticks"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"));
    }
}
