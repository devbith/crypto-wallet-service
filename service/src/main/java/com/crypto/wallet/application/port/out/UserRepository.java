package com.crypto.wallet.application.port.out;

import com.crypto.wallet.application.model.User;
import com.crypto.wallet.application.model.primitives.Email;
import com.crypto.wallet.application.model.primitives.WalletId;
import java.util.List;
import java.util.Optional;

public interface UserRepository {

  void save(User user);

  Optional<User> findByEmail(Email email);

  Optional<User> findByWalletId(WalletId walletId);

  boolean existsByEmail(Email email);

  List<User> findAllUsers();

}
