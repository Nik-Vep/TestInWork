package leon.task.timeindb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.scheduling.annotation.EnableScheduling;


@EnableScheduling
@SpringBootApplication // Аннотация, объединяющая @Configuration, @EnableAutoConfiguration и @ComponentScan
public class TimeInDbApplication {

    public static void main(String[] args) {
        SpringApplication.run(TimeInDbApplication.class, args);
    }

}
