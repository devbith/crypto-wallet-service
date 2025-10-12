package com.crypto.wallet;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import com.crypto.wallet.infrastructure.config.Profiles;
import com.crypto.wallet.application.port.out.CryptoGateway;
import com.crypto.wallet.application.model.primitives.Price;
import com.crypto.wallet.application.model.primitives.Symbol;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import jakarta.annotation.PostConstruct;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = RANDOM_PORT, classes = {CryptoWalletApplication.class, BaseIT.TestConfig.class})
@ActiveProfiles({Profiles.TEST, Profiles.LIQUIBASE})
public class BaseIT {

    @Autowired
    private DataSource dataSource;

    @Autowired
    protected TestRestTemplate restTemplate;

    @LocalServerPort
    protected int port;

    private static final List<String> POSTGRES_TABLES = List.of(
        "users",
        "assets"
    );

    @PostConstruct
    public void init() {
    }

    @AfterEach
    void cleanUp() {
        cleanupDatabase();
    }

    private void cleanupDatabase() {
        POSTGRES_TABLES.forEach(tableName -> {
            try (Connection con = dataSource.getConnection()) {
                con.setAutoCommit(false);
                try (Statement stmt = con.createStatement()) {
                    stmt.execute("TRUNCATE TABLE %s CASCADE".formatted(tableName));
                    con.commit();
                }
            } catch (SQLException e) {
                throw new RuntimeException("Failed to truncate table: " + tableName, e);
            }
        });
    }
    
    @TestConfiguration
    public static class TestConfig {

        @Bean
        @Primary
        public CryptoGateway mockCryptoGateway() {
            return new MockCryptoGateway();
        }

        private static class MockCryptoGateway implements CryptoGateway {

            private static final Map<String, BigDecimal> MOCK_PRICES = Map.of(
                "BTC", new BigDecimal("70000"),
                "ETH", new BigDecimal("3600"),
                "ADA", new BigDecimal("0.50"),
                "DOT", new BigDecimal("25.00"),
                "SOL", new BigDecimal("100.00")
            );

            @Override
            public Optional<Price> getPrice(Symbol symbol) {
                BigDecimal mockPrice = MOCK_PRICES.get(symbol.value().toUpperCase());
                return mockPrice != null ? Optional.of(Price.of(mockPrice)) : Optional.empty();
            }

            @Override
            public Map<Symbol, Price> getPrices(List<Symbol> symbols) {
                return symbols.stream()
                    .collect(Collectors.toMap(
                        symbol -> symbol,
                        symbol -> getPrice(symbol).orElse(Price.zero())
                    ));
            }

            @Override
            public boolean validateSymbol(Symbol symbol) {
                return MOCK_PRICES.containsKey(symbol.value().toUpperCase());
            }
        }
    }
}
