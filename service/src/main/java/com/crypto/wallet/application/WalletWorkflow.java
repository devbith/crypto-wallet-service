package com.crypto.wallet.application;

import com.crypto.wallet.application.port.in.WalletUseCase;
import com.crypto.wallet.application.port.out.AssetRepository;
import com.crypto.wallet.application.port.out.CryptoGateway;
import com.crypto.wallet.application.port.out.UserRepository;
import com.crypto.wallet.application.service.WalletDomainService;
import com.crypto.wallet.application.model.Asset;
import com.crypto.wallet.application.model.User;
import com.crypto.wallet.application.model.Wallet;
import com.crypto.wallet.application.model.primitives.Email;
import com.crypto.wallet.application.model.primitives.Price;
import com.crypto.wallet.application.model.primitives.Quantity;
import com.crypto.wallet.application.model.primitives.Symbol;
import com.crypto.wallet.application.model.primitives.WalletId;
import java.util.List;

public class WalletWorkflow implements WalletUseCase {

  private final UserRepository userRepository;
  private final AssetRepository assetRepository;
  private final CryptoGateway cryptoGateway;
  private final WalletDomainService walletDomainService;

  public WalletWorkflow(UserRepository userRepository, AssetRepository assetRepository, CryptoGateway cryptoGateway,
      WalletDomainService walletDomainService) {
    this.userRepository = userRepository;
    this.assetRepository = assetRepository;
    this.cryptoGateway = cryptoGateway;
    this.walletDomainService = walletDomainService;
  }

  @Override
  public User createWallet(Email email) {
    if (userRepository.existsByEmail(email)) {
      throw new IllegalArgumentException("Email already exists: " + email.value());
    }
    var user = User.create(email);
    userRepository.save(user);
    return user;
  }

  @Override
  public Wallet addAsset(WalletId walletId, Symbol symbol, Quantity quantity, Price price) {
    validateSymbol(symbol);

    var finalPrice = price;
    if (price.isZero()) {
      finalPrice = getCurrentPrice(symbol);
    }

    return addAssetToWallet(walletId, symbol, quantity, finalPrice);
  }

  @Override
  public Wallet addAssetToWallet(WalletId walletId, Symbol symbol, Quantity quantity, Price price) {
    var user = userRepository.findByWalletId(walletId)
        .orElseThrow(() -> new IllegalArgumentException("Wallet not found: " + walletId.value()));

    var wallet = Wallet.of(walletId, assetRepository.findByWalletId(walletId), user.createdAt());

    if (!walletDomainService.canAddAssetToWallet(wallet, symbol, quantity, price)) {
      throw new IllegalArgumentException("Cannot add asset to wallet");
    }

    var mergedAsset = assetRepository.findByWalletIdAndSymbol(walletId, symbol)
        .map(existing -> walletDomainService.mergeAssetQuantities(existing, quantity, price))
        .orElseGet(() -> Asset.create(walletId, symbol, quantity, price));

    var updatedWallet = assetRepository.saveAssetAndReturnWallet(walletId, mergedAsset, user);

    if (!walletDomainService.isValidWallet(updatedWallet)) {
      throw new IllegalStateException("Wallet violates business invariants after update");
    }
    return updatedWallet;
  }

  @Override
  public Wallet getWallet(WalletId walletId) {
    var user = userRepository.findByWalletId(walletId)
        .orElseThrow(() -> new IllegalArgumentException("Wallet not found: " + walletId.value()));

    var assets = assetRepository.findByWalletId(walletId);
    return Wallet.of(walletId, assets, user.createdAt());
  }

  @Override
  public List<Wallet> getAllWallets() {
    var users = userRepository.findAllUsers();
    return users.stream().map(user -> {
      var assets = assetRepository.findByWalletId(user.walletId());
      return Wallet.of(user.walletId(), assets, user.createdAt());
    }).toList();
  }

  @Override
  public User findUserByEmail(Email email) {
    return userRepository.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("User not found: " + email.value()));
  }

  @Override
  public boolean emailExists(Email email) {
    return userRepository.existsByEmail(email);
  }

  @Override
  public void validateSymbol(Symbol symbol) {
    if (!cryptoGateway.validateSymbol(symbol)) {
      throw new IllegalArgumentException("Invalid or unsupported symbol: " + symbol.value());
    }
  }

  @Override
  public Price getCurrentPrice(Symbol symbol) {
    return cryptoGateway.getPrice(symbol)
        .orElseThrow(() -> new IllegalArgumentException("Unable to fetch price for symbol: " + symbol.value()));
  }


}
