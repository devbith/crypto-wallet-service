package com.crypto.wallet.application.model;

import com.crypto.wallet.application.model.primitives.Price;
import com.crypto.wallet.application.model.primitives.Quantity;
import com.crypto.wallet.application.model.primitives.Symbol;
import com.crypto.wallet.application.model.primitives.WalletId;
import java.time.OffsetDateTime;
import java.util.Objects;

public record Asset(WalletId walletId, Symbol symbol, Quantity quantity, Price price, OffsetDateTime updatedAt) {

  public Asset {
    Objects.requireNonNull(walletId, "WalletId cannot be null");
    Objects.requireNonNull(symbol, "Symbol cannot be null");
    Objects.requireNonNull(quantity, "Quantity cannot be null");
    Objects.requireNonNull(price, "Price cannot be null");
    Objects.requireNonNull(updatedAt, "UpdatedAt cannot be null");

    if (quantity.isZero()) {
      throw new IllegalArgumentException("Asset quantity must be positive");
    }
  }

  public static Asset create(WalletId walletId, Symbol symbol, Quantity quantity, Price price) {
    return new Asset(walletId, symbol, quantity, price, OffsetDateTime.now());
  }

  public static Asset of(WalletId walletId, Symbol symbol, Quantity quantity, Price price, OffsetDateTime updatedAt) {
    return new Asset(walletId, symbol, quantity, price, updatedAt);
  }

  public Asset updatePrice(Price newPrice) {
    return new Asset(this.walletId, this.symbol, this.quantity, newPrice, OffsetDateTime.now());
  }

  public Asset updateQuantity(Quantity newQuantity) {
    return new Asset(this.walletId, this.symbol, newQuantity, this.price, OffsetDateTime.now());
  }

  public Price calculateValue() {
    return price.multiply(quantity);
  }

  @Override
  public String toString() {
    return "Asset{" + "walletId=" + walletId + ", symbol=" + symbol + ", quantity=" + quantity + ", price=" + price + ", value="
           + calculateValue() + ", updatedAt=" + updatedAt + '}';
  }
}
