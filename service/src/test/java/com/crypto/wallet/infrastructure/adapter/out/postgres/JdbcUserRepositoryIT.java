package com.crypto.wallet.infrastructure.adapter.out.postgres;

import static org.assertj.core.api.Assertions.assertThat;

import com.crypto.wallet.BaseIT;
import com.crypto.wallet.application.port.out.UserRepository;
import com.crypto.wallet.application.model.User;
import com.crypto.wallet.application.model.primitives.Email;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class JdbcUserRepositoryIT extends BaseIT {

  @Autowired
  private UserRepository userRepository;

  @Test
  void userRepository_ShouldSaveAndFindUser() {
    var email = Email.of("test@example.com");
    var user = User.create(email);

    userRepository.save(user);

    Optional<User> foundByEmail = userRepository.findByEmail(email);
    assertThat(foundByEmail).isPresent();
    assertThat(foundByEmail.get().email()).isEqualTo(email);

    Optional<User> foundByWalletId = userRepository.findByWalletId(user.walletId());
    assertThat(foundByWalletId).isPresent();
    assertThat(foundByWalletId.get().walletId()).isEqualTo(user.walletId());

    boolean exists = userRepository.existsByEmail(email);
    assertThat(exists).isTrue();
  }
}
