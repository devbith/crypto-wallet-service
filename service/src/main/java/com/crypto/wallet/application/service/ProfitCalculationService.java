package com.crypto.wallet.application.service;

import com.crypto.wallet.application.model.primitives.Price;
import com.crypto.wallet.application.model.primitives.Quantity;
import com.crypto.wallet.application.model.primitives.Symbol;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class ProfitCalculationService {

  public BigDecimal calculateProfitLossPercentage(Price originalPrice, Price currentPrice) {
    if (originalPrice.value().compareTo(BigDecimal.ZERO) == 0) {
      return BigDecimal.ZERO;
    }

    BigDecimal difference = currentPrice.value().subtract(originalPrice.value());
    BigDecimal percentage = difference.divide(originalPrice.value(), 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));

    return percentage.setScale(2, RoundingMode.HALF_UP);
  }

  public Price calculateCurrentValue(Quantity quantity, Price currentPrice) {
    BigDecimal currentValue = quantity.value().multiply(currentPrice.value());
    return Price.of(currentValue);
  }

  public Entry<Symbol, BigDecimal> findBestPerformingAsset(Map<Symbol, BigDecimal> profitLossMap) {
    return profitLossMap.entrySet().stream().max(Map.Entry.comparingByValue()).orElse(null);
  }

  public Entry<Symbol, BigDecimal> findWorstPerformingAsset(Map<Symbol, BigDecimal> profitLossMap) {
    return profitLossMap.entrySet().stream().min(Map.Entry.comparingByValue()).orElse(null);
  }

  public Price calculateTotalPortfolioValue(List<Price> assetValues) {
    BigDecimal total = assetValues.stream().map(Price::value).reduce(BigDecimal.ZERO, BigDecimal::add);

    return Price.of(total);
  }

  public boolean isValidSimulationInput(Quantity quantity, Price originalValue) {
    if (quantity.value().compareTo(BigDecimal.ZERO) <= 0) {
      return false;
    }

    if (originalValue.value().compareTo(BigDecimal.ZERO) <= 0) {
      return false;
    }

    return true;
  }

}
