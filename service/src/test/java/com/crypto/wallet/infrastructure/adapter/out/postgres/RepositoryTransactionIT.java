package com.crypto.wallet.infrastructure.adapter.out.postgres;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.crypto.wallet.BaseIT;
import com.crypto.wallet.application.port.out.AssetRepository;
import com.crypto.wallet.application.port.out.UserRepository;
import com.crypto.wallet.application.model.Asset;
import com.crypto.wallet.application.model.User;
import com.crypto.wallet.application.model.primitives.Email;
import com.crypto.wallet.application.model.primitives.Price;
import com.crypto.wallet.application.model.primitives.Quantity;
import com.crypto.wallet.application.model.primitives.Symbol;
import com.crypto.wallet.application.model.primitives.WalletId;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Import(RepositoryTransactionIT.TestConfig.class)
class RepositoryTransactionIT extends BaseIT {

  @Autowired
  private AssetRepository assetRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private TransactionalTestRepository transactionalTestRepository;

  @Test
  void transaction_shouldCommitOnSuccess() {
    var email = Email.of("test@example.com");
    var user = User.create(email);

    userRepository.save(user);
    assertThat(userRepository.findByEmail(email)).isPresent();
  }

  @Test
  void transaction_shouldRollbackOnError() {
    var user = createTestUser("rollback-test@example.com");
    var walletId = user.walletId();

    assertThatThrownBy(() -> {
      transactionalTestRepository.saveAssetsAndFail(walletId);
    }).isInstanceOf(RuntimeException.class);

    List<Asset> assets = assetRepository.findByWalletId(walletId);
    assertThat(assets).isEmpty();
  }

  private User createTestUser(String emailAddress) {
    var email = Email.of(emailAddress);
    var user = User.create(email);
    userRepository.save(user);
    return user;
  }

  @TestConfiguration
  static class TestConfig {

    @Bean
    public TransactionalTestRepository transactionalTestRepository(AssetRepository assetRepository) {
      return new TransactionalTestRepository(assetRepository);
    }
  }

  @Repository
  static class TransactionalTestRepository {

    private final AssetRepository assetRepository;

    public TransactionalTestRepository(AssetRepository assetRepository) {
      this.assetRepository = assetRepository;
    }

    @Transactional
    public void saveAssetsAndFail(WalletId walletId) {
      Asset btc = Asset.create(walletId, Symbol.of("BTC"), Quantity.of(new BigDecimal("1.0")), Price.of(new BigDecimal("50000")));
      Asset eth = Asset.create(walletId, Symbol.of("ETH"), Quantity.of(new BigDecimal("5.0")), Price.of(new BigDecimal("4000")));

      assetRepository.save(btc);
      assetRepository.save(eth);

      throw new RuntimeException("Test rollback");
    }
  }
}
