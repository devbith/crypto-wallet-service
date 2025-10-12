package com.crypto.wallet.infrastructure.adapter.in.rest;

import com.crypto.wallet.application.port.in.WalletUseCase;
import com.crypto.wallet.application.model.Asset;
import com.crypto.wallet.application.model.Wallet;
import com.crypto.wallet.application.model.primitives.Email;
import com.crypto.wallet.application.model.primitives.Price;
import com.crypto.wallet.application.model.primitives.Quantity;
import com.crypto.wallet.application.model.primitives.Symbol;
import com.crypto.wallet.application.model.primitives.WalletId;
import com.crypto.wallet.infrastructure.adapter.in.rest.dto.AddAssetRequest;
import com.crypto.wallet.infrastructure.adapter.in.rest.dto.AssetResponse;
import com.crypto.wallet.infrastructure.adapter.in.rest.dto.CreateWalletRequest;
import com.crypto.wallet.infrastructure.adapter.in.rest.dto.UserResponse;
import com.crypto.wallet.infrastructure.adapter.in.rest.dto.WalletResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/wallets")
public class WalletController {

  private static final Logger logger = LoggerFactory.getLogger(WalletController.class);

  private final WalletUseCase walletUseCase;

  public WalletController(WalletUseCase walletUseCase) {
    this.walletUseCase = walletUseCase;
  }

  @PostMapping
  public ResponseEntity<UserResponse> createWallet(@Valid @RequestBody CreateWalletRequest request) {
    logger.info("Creating wallet for email: {}", request.email());

    var email = Email.of(request.email());
    var user = walletUseCase.createWallet(email);

    var response = new UserResponse(user.email().value(), user.walletId().value(), user.createdAt());

    logger.info("Successfully created wallet with ID: {}", user.walletId());
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @PostMapping("/{walletId}/assets")
  public ResponseEntity<WalletResponse> addAsset(@PathVariable String walletId, @Valid @RequestBody AddAssetRequest request) {
    logger.info("Adding asset {} to wallet {}", request.symbol(), walletId);

    var wallet = WalletId.of(walletId);
    var symbol = Symbol.of(request.symbol());
    var quantity = Quantity.of(request.quantity());
    var price = request.price() != null ? Price.of(request.price()) : Price.zero();

    var walletWithAsset = walletUseCase.addAsset(wallet, symbol, quantity, price);
    var response = mapToWalletResponse(walletWithAsset);

    logger.info("Successfully added asset {} to wallet {}", symbol, walletId);
    return ResponseEntity.ok(response);
  }

  @GetMapping
  public ResponseEntity<List<WalletResponse>> getAllWallets() {
    logger.info("Retrieving all wallets");

    List<Wallet> wallets = walletUseCase.getAllWallets();
    List<WalletResponse> responses = wallets.stream().map(this::mapToWalletResponse).toList();

    logger.info("Successfully retrieved {} wallets", wallets.size());
    return ResponseEntity.ok(responses);
  }

  @GetMapping("/{walletId}")
  public ResponseEntity<WalletResponse> getWallet(@PathVariable String walletId) {
    var walletIdToGet = WalletId.of(walletId);
    var wallet = walletUseCase.getWallet(walletIdToGet);
    var response = mapToWalletResponse(wallet);

    logger.info("Successfully retrieved wallet {} with {} assets", walletId, wallet.getAssetCount());
    return ResponseEntity.ok(response);
  }

  private WalletResponse mapToWalletResponse(Wallet wallet) {
    var assets = wallet.assets().stream().map(this::mapToAssetResponse).toList();

    return new WalletResponse(wallet.walletId().value(), wallet.calculateTotalValue().value(), assets, wallet.createdAt());
  }

  private AssetResponse mapToAssetResponse(Asset asset) {
    return new AssetResponse(asset.symbol().value(), asset.quantity().value(), asset.price().value(), asset.calculateValue().value(),
        asset.updatedAt());
  }
}
