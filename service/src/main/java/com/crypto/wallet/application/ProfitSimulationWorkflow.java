package com.crypto.wallet.application;

import com.crypto.wallet.application.port.in.ProfitSimulationUseCase;
import com.crypto.wallet.application.port.out.CryptoGateway;
import com.crypto.wallet.application.service.ProfitCalculationService;
import com.crypto.wallet.application.model.primitives.Price;
import com.crypto.wallet.application.model.primitives.Symbol;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ProfitSimulationWorkflow implements ProfitSimulationUseCase {

  private final CryptoGateway cryptoGateway;
  private final ProfitCalculationService profitCalculationService;

  public ProfitSimulationWorkflow(CryptoGateway cryptoGateway, ProfitCalculationService profitCalculationService) {
    this.cryptoGateway = cryptoGateway;
    this.profitCalculationService = profitCalculationService;
  }

  @Override
  public ProfitSimulationResult simulateProfit(List<AssetSimulation> assets) {
    if (assets.isEmpty()) {
      return emptyResult();
    }
    validateAssets(assets);

    var currentPrices = cryptoGateway.getPrices(getSymbols(assets));
    var profitLossMap = new HashMap<Symbol, BigDecimal>();
    var assetValues = calculateAssetValues(assets, currentPrices, profitLossMap);

    return buildResult(assetValues, profitLossMap);
  }

  private void validateAssets(List<AssetSimulation> assets) {
    for (var asset : assets) {
      if (!profitCalculationService.isValidSimulationInput(asset.quantity(), asset.originalValue())) {
        throw new IllegalArgumentException("Invalid simulation input for symbol: " + asset.symbol().value());
      }
    }
  }

  private List<Symbol> getSymbols(List<AssetSimulation> assets) {
    return assets.stream().map(AssetSimulation::symbol).toList();
  }

  private List<Price> calculateAssetValues(List<AssetSimulation> assets, Map<Symbol, Price> currentPrices, Map<Symbol, BigDecimal> profitLossMap) {
    return assets.stream()
        .map(asset -> calculateAssetValue(asset, currentPrices, profitLossMap))
        .filter(Objects::nonNull)
        .toList();
  }

  private Price calculateAssetValue(AssetSimulation asset, Map<Symbol, Price> currentPrices, Map<Symbol, BigDecimal> profitLossMap) {
    var currentPrice = currentPrices.get(asset.symbol());
    if (currentPrice == null) {
      return null;
    }
    var currentValue = profitCalculationService.calculateCurrentValue(asset.quantity(), currentPrice);
    var originalPricePerUnit = Price.of(asset.originalValue().value().divide(asset.quantity().value(), Price.SCALE, Price.ROUNDING_MODE));
    var profitLoss = profitCalculationService.calculateProfitLossPercentage(originalPricePerUnit, currentPrice);

    profitLossMap.put(asset.symbol(), profitLoss);

    return currentValue;
  }

  private ProfitSimulationResult buildResult(List<Price> assetValues, Map<Symbol, BigDecimal> profitLossMap) {
    var totalCurrentValue = profitCalculationService.calculateTotalPortfolioValue(assetValues);
    var best = profitCalculationService.findBestPerformingAsset(profitLossMap);
    var worst = profitCalculationService.findWorstPerformingAsset(profitLossMap);

    return new ProfitSimulationResult(totalCurrentValue, best != null ? best.getKey() : null,
        best != null ? best.getValue() : BigDecimal.ZERO, worst != null ? worst.getKey() : null,
        worst != null ? worst.getValue() : BigDecimal.ZERO);
  }

  private ProfitSimulationResult emptyResult() {
    return new ProfitSimulationResult(Price.of(BigDecimal.ZERO), null, BigDecimal.ZERO, null, BigDecimal.ZERO);
  }

}
