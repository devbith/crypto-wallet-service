package com.crypto.wallet.application.port.in;

import com.crypto.wallet.application.model.User;
import com.crypto.wallet.application.model.Wallet;
import com.crypto.wallet.application.model.primitives.Email;
import com.crypto.wallet.application.model.primitives.Price;
import com.crypto.wallet.application.model.primitives.Quantity;
import com.crypto.wallet.application.model.primitives.Symbol;
import com.crypto.wallet.application.model.primitives.WalletId;
import java.util.List;

public interface WalletUseCase {

  User createWallet(Email email);

  Wallet addAsset(WalletId walletId, Symbol symbol, Quantity quantity, Price price);

  Wallet addAssetToWallet(WalletId walletId, Symbol symbol, Quantity quantity, Price price);

  Wallet getWallet(WalletId walletId);

  List<Wallet> getAllWallets();

  User findUserByEmail(Email email);

  boolean emailExists(Email email);

  void validateSymbol(Symbol symbol);

  Price getCurrentPrice(Symbol symbol);

}
