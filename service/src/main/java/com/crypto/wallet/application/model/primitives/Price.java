package com.crypto.wallet.application.model.primitives;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public record Price(BigDecimal value) {

  public static final int SCALE = 6;
  public static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

  private static final int PRIVATE_SCALE = 6;

  public Price {
    Objects.requireNonNull(value, "Price cannot be null");
    if (value.compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException("Price cannot be negative");
    }
    value = value.setScale(PRIVATE_SCALE, RoundingMode.HALF_UP);
  }

  public static Price of(BigDecimal value) {
    return new Price(value);
  }

  public static Price zero() {
    return new Price(BigDecimal.ZERO);
  }

  public Price multiply(Quantity quantity) {
    return new Price(this.value.multiply(quantity.value()));
  }

  public Price add(Price other) {
    return new Price(this.value.add(other.value));
  }

  public Price subtract(Price other) {
    return new Price(this.value.subtract(other.value));
  }

  public BigDecimal percentageChange(Price previousPrice) {
    if (previousPrice.value.compareTo(BigDecimal.ZERO) == 0) {
      return BigDecimal.ZERO;
    }
    return this.value.subtract(previousPrice.value).divide(previousPrice.value, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
  }

  public boolean isZero() {
    return value.compareTo(BigDecimal.ZERO) == 0;
  }

  @Override
  public String toString() {
    return "$" + value.stripTrailingZeros().toPlainString();
  }
}
