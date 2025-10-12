package com.crypto.wallet;

import com.crypto.wallet.infrastructure.config.Profiles;
import com.crypto.wallet.infrastructure.config.postgres.LiquibaseRunner;
import java.util.Set;
import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableScheduling
@EnableTransactionManagement
public class CryptoWalletApplication {

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        var app = new SpringApplication();
        if ("true".equalsIgnoreCase(System.getenv("MIGRATE_DATABASE"))) {
            runLiquibaseAndExit(app, args);
        } else {
            app.addPrimarySources(Set.of(CryptoWalletApplication.class));
            app.run(args);
        }
    }

    private static void runLiquibaseAndExit(SpringApplication app, String[] args) {
        app.addPrimarySources(Set.of(LiquibaseRunner.class));
        app.setWebApplicationType(WebApplicationType.NONE);
        app.setAdditionalProfiles(Profiles.LIQUIBASE);
        app.run(args).close();
    }
}
