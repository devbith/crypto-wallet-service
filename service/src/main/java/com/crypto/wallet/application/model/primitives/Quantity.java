package com.crypto.wallet.application.model.primitives;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public record Quantity(BigDecimal value) {

  private static final int SCALE = 8;

  public Quantity {
    Objects.requireNonNull(value, "Quantity cannot be null");
    if (value.compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException("Quantity cannot be negative");
    }
    value = value.setScale(SCALE, RoundingMode.HALF_UP);
  }

  public static Quantity of(BigDecimal value) {
    return new Quantity(value);
  }

  public Quantity add(Quantity other) {
    return new Quantity(this.value.add(other.value));
  }

  public boolean isZero() {
    return value.compareTo(BigDecimal.ZERO) == 0;
  }

  public boolean isPositive() {
    return value.compareTo(BigDecimal.ZERO) > 0;
  }

  @Override
  public String toString() {
    return value.stripTrailingZeros().toPlainString();
  }
}
