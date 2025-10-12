package com.crypto.wallet.infrastructure.adapter.out.coinmarketcap;

import com.crypto.wallet.application.port.out.CryptoGateway;
import com.crypto.wallet.application.model.primitives.Price;
import com.crypto.wallet.application.model.primitives.Symbol;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;

public class CoinMarketCap implements CryptoGateway {

  private static final Logger logger = LoggerFactory.getLogger(CoinMarketCap.class);
  private static final int CONCURRENT_PRICE_FETCH_BATCH_SIZE = 3;
  private static final String PRICE_ENDPOINT = "/price/bysymbol/";

  private final RestClient restClient;
  private final ExecutorService virtualThreadExecutor;

  public CoinMarketCap(RestClient restClient, ExecutorService virtualThreadExecutor) {
    this.restClient = restClient;
    this.virtualThreadExecutor = virtualThreadExecutor;
  }

  @Override
  public Optional<Price> getPrice(Symbol symbol) {
    try {
      var url = PRICE_ENDPOINT + symbol.value().toUpperCase();
      var priceResponse = restClient.get().uri(url).retrieve().body(PriceResponse.class);

      return extractPriceFromResponse(priceResponse);
    } catch (Exception e) {
      logger.warn("Failed to fetch price for symbol {}: {}", symbol, e.getMessage());
      return Optional.empty();
    }
  }

  @Override
  public Map<Symbol, Price> getPrices(List<Symbol> symbols) {
    if (symbols.isEmpty()) {
      return Collections.emptyMap();
    }

    logger.info("Fetching prices for {} symbols", symbols.size());
    var allPrices = new ConcurrentHashMap<Symbol, Price>();
    var symbolsProcessedSoFar = 0;

    while (symbolsProcessedSoFar < symbols.size()) {
      var remainingSymbolsCount = symbols.size() - symbolsProcessedSoFar;

      // Take up to 3 symbols at a time
      var symbolsToProcessInThisBatch = Math.min(CONCURRENT_PRICE_FETCH_BATCH_SIZE, remainingSymbolsCount);
      var nextBatchEndPosition = symbolsProcessedSoFar + symbolsToProcessInThisBatch;
      var nextBatchOfSymbols = symbols.subList(symbolsProcessedSoFar, nextBatchEndPosition);

      // Process current batch concurrently (max 3 symbols at once)
      fetchPricesForBatchConcurrently(nextBatchOfSymbols, allPrices);
      symbolsProcessedSoFar = nextBatchEndPosition;
    }
    return allPrices;
  }

  @Override
  public boolean validateSymbol(Symbol symbol) {
    return getPrice(symbol).isPresent();
  }

  private void fetchPricesForBatchConcurrently(List<Symbol> batchOfSymbols, Map<Symbol, Price> allPrices) {
    List<CompletableFuture<Void>> concurrentPriceFetchTasks = batchOfSymbols.stream()
        .map(symbol -> CompletableFuture.runAsync(() -> getPrice(symbol).ifPresent(price -> allPrices.put(symbol, price)),
            virtualThreadExecutor))
        .toList();
    CompletableFuture.allOf(concurrentPriceFetchTasks.toArray(CompletableFuture[]::new)).join();
  }


  private Optional<Price> extractPriceFromResponse(PriceResponse priceResponse) {
    if (priceResponse != null && priceResponse.data() != null && !priceResponse.data().isEmpty()) {
      var priceStr = priceResponse.data().get(0);
      if (priceStr != null) {
        return Optional.of(Price.of(new BigDecimal(priceStr)));
      }
    }
    return Optional.empty();
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record PriceResponse(long timestamp, List<String> data) {

  }
}
