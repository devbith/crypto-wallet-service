package com.crypto.wallet.application.service;

import com.crypto.wallet.application.port.in.PriceUpdateUseCase;
import com.crypto.wallet.application.port.out.AssetRepository;
import com.crypto.wallet.application.port.out.CryptoGateway;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultPriceUpdateService implements PriceUpdateUseCase {

  private static final Logger logger = LoggerFactory.getLogger(DefaultPriceUpdateService.class);

  private final AssetRepository assetRepository;
  private final CryptoGateway cryptoGateway;

  public DefaultPriceUpdateService(AssetRepository assetRepository, CryptoGateway cryptoGateway) {
    this.assetRepository = assetRepository;
    this.cryptoGateway = cryptoGateway;
  }

  @Override
  public void updateAllPrices() {
    logger.info("Starting update of all unique symbol prices");

    var uniqueSymbols = assetRepository.findAllUniqueSymbols();

    if (uniqueSymbols.isEmpty()) {
      logger.info("No symbols found to update");
      return;
    }
    logger.info("Found {} unique symbols to update", uniqueSymbols.size());
    var symbolList = List.copyOf(uniqueSymbols);
    var currentPrices = cryptoGateway.getPrices(symbolList);

    assetRepository.updatePricesForSymbols(currentPrices);
    logger.info("Completed price update for {} symbols", currentPrices.size());
  }


}
