package com.crypto.wallet.infrastructure.config.postgres;

import com.crypto.wallet.infrastructure.config.Profiles;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@SpringBootApplication
public class LiquibaseRunner {

    @Profile(Profiles.LIQUIBASE)
    @Configuration
    static class LiquibaseConfig {

    }
}