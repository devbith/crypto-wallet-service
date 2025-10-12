package com.crypto.wallet.infrastructure.config;

import com.crypto.wallet.application.port.in.PriceUpdateUseCase;
import com.crypto.wallet.application.port.in.ProfitSimulationUseCase;
import com.crypto.wallet.application.port.in.WalletUseCase;
import com.crypto.wallet.application.port.out.AssetRepository;
import com.crypto.wallet.application.port.out.CryptoGateway;
import com.crypto.wallet.application.port.out.UserRepository;
import com.crypto.wallet.application.service.DefaultPriceUpdateService;
import com.crypto.wallet.application.ProfitSimulationWorkflow;
import com.crypto.wallet.application.WalletWorkflow;
import com.crypto.wallet.application.service.ProfitCalculationService;
import com.crypto.wallet.application.service.WalletDomainService;
import com.crypto.wallet.infrastructure.adapter.in.cronjob.PriceUpdateCronJob;
import com.crypto.wallet.infrastructure.adapter.out.coinmarketcap.CoinMarketCap;
import com.crypto.wallet.infrastructure.adapter.out.postgres.JdbcAssetRepository;
import com.crypto.wallet.infrastructure.adapter.out.postgres.JdbcUserRepository;
import com.crypto.wallet.infrastructure.config.properties.CoinCapProperties;
import com.crypto.wallet.infrastructure.config.properties.DataSourceProperties;
import com.crypto.wallet.infrastructure.config.properties.LiquibaseProperties;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import liquibase.integration.spring.SpringLiquibase;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableTransactionManagement
@EnableSchedulerLock(defaultLockAtMostFor = "10m")
@EnableConfigurationProperties({DataSourceProperties.class, LiquibaseProperties.class, CoinCapProperties.class})
public class ApplicationConfig {

  private ExecutorService virtualThreadExecutor;
  private static final Logger logger = LoggerFactory.getLogger(ApplicationConfig.class);

  @Bean
  public DataSource dataSource(DataSourceProperties dataSourceProperties) {
    HikariConfig config = new HikariConfig();
    config.setJdbcUrl(dataSourceProperties.url());
    config.setUsername(dataSourceProperties.username());
    config.setPassword(dataSourceProperties.password());
    return new HikariDataSource(config);
  }

  @Bean
  public PlatformTransactionManager transactionManager(DataSource dataSource) {
    return new DataSourceTransactionManager(dataSource);
  }

  @Bean
  public JdbcClient jdbcClient(DataSource dataSource) {
    return JdbcClient.create(dataSource);
  }

  @Bean
  public JdbcTemplate jdbcTemplate(DataSource dataSource) {
    return new JdbcTemplate(dataSource);
  }

  @Bean
  public LockProvider lockProvider(JdbcTemplate jdbcTemplate) {
    return new JdbcTemplateLockProvider(JdbcTemplateLockProvider.Configuration.builder()
        .withJdbcTemplate(jdbcTemplate)
        .usingDbTime()
        .build());
  }

  @Bean
  @Profile("!test")
  public SpringLiquibase liquibase(DataSource dataSource, LiquibaseProperties liquibaseProperties) {
    SpringLiquibase liquibase = new SpringLiquibase();
    liquibase.setDataSource(dataSource);
    liquibase.setChangeLog(liquibaseProperties.changeLog());
    liquibase.setContexts(liquibaseProperties.contexts());
    return liquibase;
  }

  @Bean
  public ObjectMapper objectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    return mapper;
  }


  @Bean
  public UserRepository userRepository(JdbcClient jdbcClient) {
    return new JdbcUserRepository(jdbcClient);
  }

  @Bean
  public AssetRepository assetRepository(JdbcClient jdbcClient) {
    return new JdbcAssetRepository(jdbcClient);
  }

  @Bean
  public CryptoGateway cryptoPriceGateway(ExecutorService virtualThreadExecutor, CoinCapProperties coinCapProperties) {

    String apiKey = coinCapProperties.key();
    RestClient.Builder restClientBuilder = RestClient.builder()
        .baseUrl(coinCapProperties.baseUrl())
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);

    if (apiKey != null && !apiKey.trim().isEmpty()) {
      restClientBuilder = restClientBuilder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey.trim());
      logger.info("CoinCap API configured with authentication");
    } else {
      logger.warn("CoinCap API configured without authentication - rate limits may apply");
    }

    RestClient restClient = restClientBuilder.build();

    return new CoinMarketCap(restClient, virtualThreadExecutor);
  }

  @Bean
  public WalletDomainService walletDomainService() {
    return new WalletDomainService();
  }

  @Bean
  public ProfitCalculationService profitCalculationService() {
    return new ProfitCalculationService();
  }

  @Bean
  public WalletUseCase walletUseCase(UserRepository userRepository, AssetRepository assetRepository, CryptoGateway cryptoGateway,
      WalletDomainService walletDomainService) {
    return new WalletWorkflow(userRepository, assetRepository, cryptoGateway, walletDomainService);
  }

  @Bean
  public ProfitSimulationUseCase profitSimulationUseCase(CryptoGateway cryptoGateway, ProfitCalculationService profitCalculationService) {
    return new ProfitSimulationWorkflow(cryptoGateway, profitCalculationService);
  }

  @Bean
  public PriceUpdateUseCase priceUpdateUseCase(AssetRepository assetRepository, CryptoGateway cryptoGateway) {
    return new DefaultPriceUpdateService(assetRepository, cryptoGateway);
  }

  @Bean
  @ConditionalOnProperty(name = "price.update.enabled", havingValue = "true", matchIfMissing = true)
  public PriceUpdateCronJob priceUpdateCronJob(PriceUpdateUseCase priceUpdateUseCase) {
    return new PriceUpdateCronJob(priceUpdateUseCase);
  }

  @Bean
  public ExecutorService virtualThreadExecutor() {
    this.virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
    return this.virtualThreadExecutor;
  }

  @Bean
  public WebMvcConfigurer corsConfigurer() {
    return new WebMvcConfigurer() {
      @Override
      public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOriginPatterns("http://localhost:*", "http://127.0.0.1:*")
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true)
            .maxAge(3600);
      }
    };
  }

  @PreDestroy
  public void shutdown() {
    if (virtualThreadExecutor != null && !virtualThreadExecutor.isShutdown()) {
      logger.info("Shutting down virtual thread executor...");
      virtualThreadExecutor.shutdown();

      try {
        if (!virtualThreadExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
          logger.warn("Virtual thread executor did not terminate gracefully, forcing shutdown...");
          virtualThreadExecutor.shutdownNow();

          if (!virtualThreadExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
            logger.error("Virtual thread executor did not terminate after forced shutdown");
          }
        } else {
          logger.info("Virtual thread executor shutdown completed gracefully");
        }
      } catch (InterruptedException e) {
        logger.warn("Interrupted while waiting for virtual thread executor shutdown");
        virtualThreadExecutor.shutdownNow();
        Thread.currentThread().interrupt();
      }
    }
  }

}
