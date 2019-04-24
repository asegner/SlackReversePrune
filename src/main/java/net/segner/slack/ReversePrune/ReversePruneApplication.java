package net.segner.slack.ReversePrune;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.util.StringUtils;

import java.util.Collections;

@SpringBootApplication
@Slf4j
public class ReversePruneApplication {
    private static String port = System.getenv("PORT");
    private final PruneManager pruneManager;

    public ReversePruneApplication(PruneManager pruneManager) {
        this.pruneManager = pruneManager;
    }

    public static void main(String[] args) {
        log.info("Starting");
        SpringApplication app = new SpringApplication(ReversePruneApplication.class);
        if (!StringUtils.isEmpty(port)) {
            app.setDefaultProperties(Collections.singletonMap("server.port", port));
        }
        app.run(args);
        log.info("Exiting");
    }

    @Bean
    public CommandLineRunner commandLineRunner(ApplicationContext ctx) {
        return args -> {
            pruneManager.run();
        };
    }
}
