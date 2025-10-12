package com.crypto.wallet.application.port.out;

import com.crypto.wallet.application.model.primitives.Price;
import com.crypto.wallet.application.model.primitives.Symbol;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface CryptoGateway {

  Optional<Price> getPrice(Symbol symbol);

  Map<Symbol, Price> getPrices(List<Symbol> symbols);

  boolean validateSymbol(Symbol symbol);

}
