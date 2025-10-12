package com.crypto.wallet.application.port.out;

import com.crypto.wallet.application.model.Asset;
import com.crypto.wallet.application.model.User;
import com.crypto.wallet.application.model.Wallet;
import com.crypto.wallet.application.model.primitives.Price;
import com.crypto.wallet.application.model.primitives.Symbol;
import com.crypto.wallet.application.model.primitives.WalletId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface AssetRepository {

  void save(Asset asset);

  List<Asset> findByWalletId(WalletId walletId);

  Optional<Asset> findByWalletIdAndSymbol(WalletId walletId, Symbol symbol);

  Set<Symbol> findAllUniqueSymbols();

  void updatePricesForSymbols(Map<Symbol, Price> symbolPrices);

  Wallet saveAssetAndReturnWallet(WalletId walletId, Asset assetToSave, User walletOwner);

  void delete(WalletId walletId, Symbol symbol);
}
