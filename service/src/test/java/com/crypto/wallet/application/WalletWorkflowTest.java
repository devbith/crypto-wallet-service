package com.crypto.wallet.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.crypto.wallet.application.port.out.AssetRepository;
import com.crypto.wallet.application.port.out.CryptoGateway;
import com.crypto.wallet.application.port.out.UserRepository;
import com.crypto.wallet.application.model.Asset;
import com.crypto.wallet.application.model.User;
import com.crypto.wallet.application.model.Wallet;
import com.crypto.wallet.application.model.primitives.Email;
import com.crypto.wallet.application.model.primitives.Price;
import com.crypto.wallet.application.model.primitives.Quantity;
import com.crypto.wallet.application.model.primitives.Symbol;
import com.crypto.wallet.application.model.primitives.WalletId;
import com.crypto.wallet.application.service.WalletDomainService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WalletWorkflowTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private AssetRepository assetRepository;

  @Mock
  private CryptoGateway cryptoGateway;

  @Mock
  private WalletDomainService walletDomainService;

  private WalletWorkflow walletWorkFlow;

  private Email testEmail;
  private WalletId testWalletId;
  private Symbol testSymbol;
  private Quantity testQuantity;
  private Price testPrice;

  @BeforeEach
  void setUp() {
    walletWorkFlow = new WalletWorkflow(userRepository, assetRepository, cryptoGateway, walletDomainService);

    testEmail = Email.of("spider.man@marvel.com");
    testWalletId = WalletId.generate();
    testSymbol = Symbol.of("BTC");
    testQuantity = Quantity.of(new BigDecimal("1.5"));
    testPrice = Price.of(new BigDecimal("50000.00"));
  }

  @Test
  void createWallet_WhenEmailDoesNotExist_ShouldCreateAndReturnUser() {
    when(userRepository.existsByEmail(testEmail)).thenReturn(false);
    doNothing().when(userRepository).save(any(User.class));

    User result = walletWorkFlow.createWallet(testEmail);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.email()).isEqualTo(testEmail);
    assertThat(result.walletId()).isNotNull();
    verify(userRepository).existsByEmail(testEmail);
    verify(userRepository).save(any(User.class));
  }

  @Test
  void getWallet_WhenWalletExists_ShouldReturnWalletWithAssets() {
    User user = User.create(testEmail);
    Asset asset = Asset.create(testWalletId, testSymbol, testQuantity, testPrice);
    List<Asset> assets = List.of(asset);

    when(userRepository.findByWalletId(testWalletId)).thenReturn(Optional.of(user));
    when(assetRepository.findByWalletId(testWalletId)).thenReturn(assets);

    var result = walletWorkFlow.getWallet(testWalletId);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.walletId()).isEqualTo(testWalletId);
    assertThat(result.assets()).hasSize(1);
    assertThat(result.assets().get(0).symbol()).isEqualTo(testSymbol);

    verify(userRepository).findByWalletId(testWalletId);
    verify(assetRepository).findByWalletId(testWalletId);
  }

  @Test
  void getWallet_WhenWalletDoesNotExist_ShouldThrowException() {
    when(userRepository.findByWalletId(testWalletId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> walletWorkFlow.getWallet(testWalletId)).isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Wallet not found: " + testWalletId.value());

    verify(userRepository).findByWalletId(testWalletId);
    verify(assetRepository, never()).findByWalletId(any());
  }

  @Test
  void findUserByEmail_WhenUserExists_ShouldReturnUser() {
    User user = User.create(testEmail);
    when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(user));

    // When
    User result = walletWorkFlow.findUserByEmail(testEmail);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.email()).isEqualTo(testEmail);
    verify(userRepository).findByEmail(testEmail);
  }

  @Test
  void emailExists_WhenEmailExists_ShouldReturnTrue() {
    when(userRepository.existsByEmail(testEmail)).thenReturn(true);

    boolean result = walletWorkFlow.emailExists(testEmail);

    assertThat(result).isTrue();
    verify(userRepository).existsByEmail(testEmail);
  }

  @Test
  void emailExists_WhenEmailDoesNotExist_ShouldReturnFalse() {
    when(userRepository.existsByEmail(testEmail)).thenReturn(false);

    boolean result = walletWorkFlow.emailExists(testEmail);

    assertThat(result).isFalse();
    verify(userRepository).existsByEmail(testEmail);
  }

  @Test
  void validateSymbol_WhenSymbolIsValid_ShouldNotThrow() {
    when(cryptoGateway.validateSymbol(testSymbol)).thenReturn(true);

    walletWorkFlow.validateSymbol(testSymbol);
    verify(cryptoGateway).validateSymbol(testSymbol);
  }

  @Test
  void getCurrentPrice_WhenPriceIsAvailable_ShouldReturnPrice() {
    when(cryptoGateway.getPrice(testSymbol)).thenReturn(Optional.of(testPrice));
    var result = walletWorkFlow.getCurrentPrice(testSymbol);
    // Then
    assertThat(result).isEqualTo(testPrice);
    verify(cryptoGateway).getPrice(testSymbol);
  }

  @Test
  void addAssetToWallet_WhenNewAsset_ShouldCreateAndSaveAsset() {
    var user = User.create(testEmail);
    user = User.of(user.email(), testWalletId, user.createdAt());
    List<Asset> currentAssets = List.of();
    var newAsset = Asset.create(testWalletId, testSymbol, testQuantity, testPrice);

    when(userRepository.findByWalletId(testWalletId)).thenReturn(Optional.of(user));
    when(assetRepository.findByWalletId(testWalletId)).thenReturn(currentAssets);
    when(walletDomainService.canAddAssetToWallet(any(Wallet.class), eq(testSymbol), eq(testQuantity), eq(testPrice))).thenReturn(true);
    when(assetRepository.findByWalletIdAndSymbol(testWalletId, testSymbol)).thenReturn(Optional.empty());
    var expectedWallet = Wallet.of(testWalletId, List.of(newAsset), user.createdAt());
    when(assetRepository.saveAssetAndReturnWallet(eq(testWalletId), any(Asset.class), eq(user))).thenReturn(expectedWallet);
    when(walletDomainService.isValidWallet(expectedWallet)).thenReturn(true);

    var result = walletWorkFlow.addAssetToWallet(testWalletId, testSymbol, testQuantity, testPrice);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.walletId()).isEqualTo(testWalletId);
    assertThat(result.assets()).hasSize(1);
    assertThat(result.assets().get(0).symbol()).isEqualTo(testSymbol);

    verify(walletDomainService).canAddAssetToWallet(any(Wallet.class), eq(testSymbol), eq(testQuantity), eq(testPrice));
    verify(assetRepository).saveAssetAndReturnWallet(eq(testWalletId), any(Asset.class), eq(user));
    verify(walletDomainService).isValidWallet(expectedWallet);
  }

  @Test
  void addAssetToWallet_WhenExistingAsset_ShouldMergeQuantities() {
    var user = User.create(testEmail);
    user = User.of(user.email(), testWalletId, user.createdAt());
    var existingAsset = Asset.create(testWalletId, testSymbol, testQuantity, testPrice);
    var mergedAsset = Asset.create(testWalletId, testSymbol, Quantity.of(new BigDecimal("3.0")), testPrice);

    when(userRepository.findByWalletId(testWalletId)).thenReturn(Optional.of(user));
    when(assetRepository.findByWalletId(testWalletId)).thenReturn(List.of(existingAsset));
    when(walletDomainService.canAddAssetToWallet(any(Wallet.class), eq(testSymbol), eq(testQuantity), eq(testPrice))).thenReturn(true);
    when(assetRepository.findByWalletIdAndSymbol(testWalletId, testSymbol)).thenReturn(Optional.of(existingAsset));
    when(walletDomainService.mergeAssetQuantities(existingAsset, testQuantity, testPrice)).thenReturn(mergedAsset);
    var expectedWallet = Wallet.of(testWalletId, List.of(mergedAsset), user.createdAt());
    when(assetRepository.saveAssetAndReturnWallet(eq(testWalletId), eq(mergedAsset), eq(user))).thenReturn(expectedWallet);
    when(walletDomainService.isValidWallet(expectedWallet)).thenReturn(true);

    // When
    var result = walletWorkFlow.addAssetToWallet(testWalletId, testSymbol, testQuantity, testPrice);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.walletId()).isEqualTo(testWalletId);
    assertThat(result.assets()).hasSize(1);

    verify(walletDomainService).mergeAssetQuantities(existingAsset, testQuantity, testPrice);
    verify(assetRepository).saveAssetAndReturnWallet(eq(testWalletId), eq(mergedAsset), eq(user));
    verify(walletDomainService).isValidWallet(expectedWallet);
  }

}
