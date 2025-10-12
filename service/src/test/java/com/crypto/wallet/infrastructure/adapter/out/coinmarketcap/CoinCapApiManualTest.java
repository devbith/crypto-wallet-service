package com.crypto.wallet.infrastructure.adapter.out.coinmarketcap;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.moreThanOrExactly;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;

import com.crypto.wallet.application.model.primitives.Price;
import com.crypto.wallet.application.model.primitives.Symbol;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

public class CoinCapApiManualTest {

  private WireMockServer wireMockServer;
  private CoinMarketCap coinCapClient;

  @BeforeEach
  void setUp() {
    wireMockServer = new WireMockServer(0);
    wireMockServer.start();
    WireMock.configureFor(wireMockServer.port());

    coinCapClient = createCoinCapClient();
    setupMockResponses();
  }

  @AfterEach
  void tearDown() {
    if (wireMockServer != null) {
      wireMockServer.stop();
    }
  }

  @Test
  void testSinglePriceFetching() {
    String[] symbols = {"BTC", "ETH", "ADA", "DOT", "LINK"};

    for (String symbolStr : symbols) {
      Symbol symbol = Symbol.of(symbolStr);
      Optional<Price> price = coinCapClient.getPrice(symbol);

      assertThat(price).isPresent();
      assertThat(price.get().value()).isPositive();
    }
    verify(exactly(5), getRequestedFor(urlMatching("/v2/price/bysymbol/.*")));
  }

  @Test
  void testBatchPriceFetching() {
    List<Symbol> symbols = List.of(Symbol.of("BTC"), Symbol.of("ETH"), Symbol.of("ADA"), Symbol.of("DOT"), Symbol.of("LINK"),
        Symbol.of("MATIC"), Symbol.of("AVAX"), Symbol.of("SOL"));

    Map<Symbol, Price> prices = coinCapClient.getPrices(symbols);

    assertThat(prices).hasSize(symbols.size());
    prices.forEach((symbol, price) -> {
      assertThat(price.value()).isPositive();
    });
    verify(moreThanOrExactly(1), getRequestedFor(urlMatching("/v2/price/bysymbol/.*")));
  }

  @Test
  void testSymbolValidation() {
    String[] validSymbols = {"BTC", "ETH", "ADA"};

    for (String symbolStr : validSymbols) {
      Symbol symbol = Symbol.of(symbolStr);
      boolean isValid = coinCapClient.validateSymbol(symbol);
      assertThat(isValid).isTrue();
    }
    verify(exactly(3), getRequestedFor(urlMatching("/v2/price/bysymbol/.*")));
  }

  @Test
  void testBearerTokenAuthentication() {
    Symbol btc = Symbol.of("BTC");
    Optional<Price> price = coinCapClient.getPrice(btc);

    assertThat(price).isPresent();
    verify(exactly(1), getRequestedFor(urlEqualTo("/v2/price/bysymbol/BTC"))
        .withHeader("Authorization", equalTo("Bearer test-api-key")));
  }


  private CoinMarketCap createCoinCapClient() {
    RestClient restClient = RestClient.builder()
        .baseUrl(wireMockServer.baseUrl() + "/v2")
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer test-api-key")
        .build();

    return new CoinMarketCap(restClient, Executors.newVirtualThreadPerTaskExecutor());
  }

  private void setupMockResponses() {
    setupSinglePriceResponse("BTC", "65432.50");
    setupSinglePriceResponse("ETH", "3456.78");
    setupSinglePriceResponse("ADA", "0.45");
    setupSinglePriceResponse("DOT", "6.78");
    setupSinglePriceResponse("LINK", "14.56");
    setupSinglePriceResponse("MATIC", "0.89");
    setupSinglePriceResponse("AVAX", "89.12");
    setupSinglePriceResponse("SOL", "123.45");

    setupBatchPriceResponse();
  }

  private void setupSinglePriceResponse(String symbol, String price) {
    stubFor(get(urlEqualTo("/v2/price/bysymbol/" + symbol)).willReturn(
        aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody("""
            {
              "timestamp": %d,
              "data": ["%s"]
            }
            """.formatted(System.currentTimeMillis(), price))));
  }

  private void setupBatchPriceResponse() {
    stubFor(get(urlMatching("/v2/price/bysymbol/.*")).willReturn(
        aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody("""
            {
              "timestamp": %d,
              "data": ["65432.50", "3456.78", "0.45", "6.78", "14.56", "0.89", "89.12", "123.45"]
            }
            """.formatted(System.currentTimeMillis()))));
  }


}
