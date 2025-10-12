package com.crypto.wallet.application.service;

import com.crypto.wallet.application.model.Asset;
import com.crypto.wallet.application.model.Wallet;
import com.crypto.wallet.application.model.primitives.Price;
import com.crypto.wallet.application.model.primitives.Quantity;
import com.crypto.wallet.application.model.primitives.Symbol;
import java.math.BigDecimal;

public class WalletDomainService {

  public Asset mergeAssetQuantities(Asset existingAsset, Quantity newQuantity, Price newPrice) {
    Quantity totalQuantity = existingAsset.quantity().add(newQuantity);

    BigDecimal existingValue = existingAsset.calculateValue().value();
    BigDecimal newValue = newQuantity.value().multiply(newPrice.value());
    BigDecimal totalValue = existingValue.add(newValue);

    Price weightedAveragePrice = Price.of(totalValue.divide(totalQuantity.value(), Price.SCALE, Price.ROUNDING_MODE));

    return existingAsset.updateQuantity(totalQuantity).updatePrice(weightedAveragePrice);
  }

  public boolean canAddAssetToWallet(Wallet wallet, Symbol symbol, Quantity quantity, Price price) {
    if (quantity.value().compareTo(BigDecimal.valueOf(0.00000001)) < 0) {
      return false;
    }

    if (price.value().compareTo(BigDecimal.ZERO) <= 0) {
      return false;
    }

    return wallet.getAssetCount() < 100 || wallet.hasAsset(symbol);
  }

  public boolean isValidWallet(Wallet wallet) {
    if (wallet.walletId() == null) {
      return false;
    }

    return wallet.assets().stream().allMatch(asset -> asset.quantity().value().compareTo(BigDecimal.ZERO) > 0);
  }
}
