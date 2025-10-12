package com.crypto.wallet.application.model;

import com.crypto.wallet.application.model.primitives.Email;
import com.crypto.wallet.application.model.primitives.WalletId;
import java.time.OffsetDateTime;
import java.util.Objects;

public record User(Email email, WalletId walletId, OffsetDateTime createdAt) {

  public User {
    Objects.requireNonNull(email, "Email cannot be null");
    Objects.requireNonNull(walletId, "WalletId cannot be null");
    Objects.requireNonNull(createdAt, "CreatedAt cannot be null");
  }

  public static User create(Email email) {
    return new User(email, WalletId.generate(), OffsetDateTime.now());
  }

  public static User of(Email email, WalletId walletId, OffsetDateTime createdAt) {
    return new User(email, walletId, createdAt);
  }

}
