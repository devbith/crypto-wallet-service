package com.crypto.wallet.infrastructure.adapter.out.postgres;

import static org.assertj.core.api.Assertions.assertThat;

import com.crypto.wallet.BaseIT;
import com.crypto.wallet.application.port.out.AssetRepository;
import com.crypto.wallet.application.port.out.UserRepository;
import com.crypto.wallet.application.model.Asset;
import com.crypto.wallet.application.model.User;
import com.crypto.wallet.application.model.primitives.Email;
import com.crypto.wallet.application.model.primitives.Price;
import com.crypto.wallet.application.model.primitives.Quantity;
import com.crypto.wallet.application.model.primitives.Symbol;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class JdbcAssetRepositoryIT extends BaseIT {

  @Autowired
  private AssetRepository assetRepository;

  @Autowired
  private UserRepository userRepository;

  @Test
  void assetRepository_ShouldSaveAndFindAssets() {
    var email = Email.of("test@example.com");
    var user = User.create(email);
    userRepository.save(user);
    var walletId = user.walletId();

    var btcSymbol = Symbol.of("BTC");
    var ethSymbol = Symbol.of("ETH");
    var btcAsset = Asset.create(walletId, btcSymbol, Quantity.of(new BigDecimal("1.5")), Price.of(new BigDecimal("50000")));
    var ethAsset = Asset.create(walletId, ethSymbol, Quantity.of(new BigDecimal("4.25")), Price.of(new BigDecimal("4000")));

    assetRepository.save(btcAsset);
    assetRepository.save(ethAsset);

    List<Asset> assets = assetRepository.findByWalletId(walletId);
    assertThat(assets).hasSize(2);

    Optional<Asset> foundBtc = assetRepository.findByWalletIdAndSymbol(walletId, btcSymbol);
    assertThat(foundBtc).isPresent();
    assertThat(foundBtc.get().symbol()).isEqualTo(btcSymbol);

    Asset updatedBtc = Asset.create(walletId, btcSymbol, Quantity.of(new BigDecimal("2.0")), Price.of(new BigDecimal("55000")));
    assetRepository.save(updatedBtc);

    List<Asset> assetsAfterUpdate = assetRepository.findByWalletId(walletId);
    assertThat(assetsAfterUpdate).hasSize(2);
    assertThat(assetsAfterUpdate.get(0).quantity().value()).isEqualByComparingTo(new BigDecimal("2.0"));
  }
}
