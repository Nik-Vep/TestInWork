package leon.task.timeindb.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

@Configuration
@EnableScheduling  // Включает поддержку планировщика задач (scheduler)
public class SchedulingConfig implements SchedulingConfigurer {  // Реализует интерфейс для кастомной настройки задач
    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler(); // Создание планировщика с пулом потоков
        scheduler.setPoolSize(4); // Установка количества потоков в пуле
        scheduler.setThreadNamePrefix("sched-");  // Префикс для имён потоков, для логов и дампов
        scheduler.initialize();  // Инициализация планировщика, переводит его в готовое к работе состояние.
        registrar.setTaskScheduler(scheduler);  /* Регистрация кастомного планировщика в Spring,
                                                   Все @Scheduled -задачи в приложении будут планироваться и исполняться
                                                   через этот ThreadPoolTaskScheduler.
                                                 */
    }
}