package com.crypto.wallet.infrastructure.adapter.out.postgres;

import com.crypto.wallet.application.port.out.AssetRepository;
import com.crypto.wallet.application.model.Asset;
import com.crypto.wallet.application.model.User;
import com.crypto.wallet.application.model.Wallet;
import com.crypto.wallet.application.model.primitives.Price;
import com.crypto.wallet.application.model.primitives.Quantity;
import com.crypto.wallet.application.model.primitives.Symbol;
import com.crypto.wallet.application.model.primitives.WalletId;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JdbcAssetRepository implements AssetRepository {

  private final JdbcClient jdbcClient;

  public JdbcAssetRepository(JdbcClient jdbcClient) {
    this.jdbcClient = jdbcClient;
  }

  @Override
  @Transactional
  public void save(Asset asset) {
    jdbcClient.sql("""
            INSERT INTO assets (wallet_id, symbol, quantity, price, updated_at) 
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT (wallet_id, symbol) 
            DO UPDATE SET 
                quantity = EXCLUDED.quantity,
                price = EXCLUDED.price,
                updated_at = EXCLUDED.updated_at
            """)
        .param(asset.walletId().value())
        .param(asset.symbol().value())
        .param(asset.quantity().value())
        .param(asset.price().value())
        .param(asset.updatedAt())
        .update();
  }

  @Override
  @Transactional(readOnly = true)
  public List<Asset> findByWalletId(WalletId walletId) {
    return jdbcClient.sql("""
        SELECT wallet_id, symbol, quantity, price, updated_at 
        FROM assets 
        WHERE wallet_id = ?
        ORDER BY symbol
        """).param(walletId.value()).query(this::mapAsset).list();
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<Asset> findByWalletIdAndSymbol(WalletId walletId, Symbol symbol) {
    return jdbcClient.sql("""
        SELECT wallet_id, symbol, quantity, price, updated_at 
        FROM assets 
        WHERE wallet_id = ? AND symbol = ?
        """).param(walletId.value()).param(symbol.value()).query(this::mapAsset).optional();
  }

  @Override
  @Transactional(readOnly = true)
  public Set<Symbol> findAllUniqueSymbols() {
    return jdbcClient.sql("""
        SELECT DISTINCT symbol 
        FROM assets
        """).query(String.class).set().stream().map(Symbol::of).collect(Collectors.toSet());
  }

  @Override
  @Transactional
  public void updatePricesForSymbols(Map<Symbol, Price> symbolPrices) {
    if (symbolPrices.isEmpty()) {
      return;
    }

    OffsetDateTime now = OffsetDateTime.now();

    StringBuilder valuesClause = new StringBuilder();
    List<Object> params = new ArrayList<>();

    boolean first = true;
    for (Map.Entry<Symbol, Price> entry : symbolPrices.entrySet()) {
      if (!first) {
        valuesClause.append(", ");
      }
      valuesClause.append("(?, ?, ?)");
      params.add(entry.getKey().value());
      params.add(entry.getValue().value());
      params.add(now);
      first = false;
    }

    String sql = """
        UPDATE assets
        SET price = new_values.price, updated_at = new_values.updated_at
        FROM (VALUES %s) AS new_values(symbol, price, updated_at)
        WHERE assets.symbol = new_values.symbol
        """.formatted(valuesClause.toString());

    jdbcClient.sql(sql).params(params.toArray()).update();
  }

  @Override
  @Transactional
  public Wallet saveAssetAndReturnWallet(WalletId walletId, Asset assetToSave, User walletOwner) {
    save(assetToSave);
    List<Asset> allAssets = findByWalletId(walletId);
    return Wallet.of(walletId, allAssets, walletOwner.createdAt());
  }

  @Override
  @Transactional
  public void delete(WalletId walletId, Symbol symbol) {
    jdbcClient.sql("""
        DELETE FROM assets
        WHERE wallet_id = ? AND symbol = ?
        """).param(walletId.value()).param(symbol.value()).update();
  }

  @Override
  @Transactional(readOnly = true)
  public List<Wallet> findAllWalletsWithAssets() {
    Map<WalletId, WalletData> walletMap = new LinkedHashMap<>();

    jdbcClient.sql("""
        SELECT u.email, u.wallet_id, u.created_at,
               a.symbol, a.quantity, a.price, a.updated_at
        FROM users u
        LEFT JOIN assets a ON u.wallet_id = a.wallet_id
        ORDER BY u.created_at DESC, a.symbol
        """).query((rs, rowNum) -> {
          WalletId walletId = WalletId.of(rs.getString("wallet_id"));

          WalletData walletData = walletMap.computeIfAbsent(walletId, k -> {
            try {
              return new WalletData(walletId, rs.getObject("created_at", OffsetDateTime.class), new ArrayList<>());
            } catch (SQLException e) {
              throw new RuntimeException("Error reading wallet data", e);
            }
          });

          String symbol = rs.getString("symbol");
          if (symbol != null) {
            Asset asset = Asset.of(
                walletId,
                Symbol.of(symbol),
                Quantity.of(rs.getBigDecimal("quantity")),
                Price.of(rs.getBigDecimal("price")),
                rs.getObject("updated_at", OffsetDateTime.class)
            );
            walletData.assets().add(asset);
          }

          return null;
        }).list();

    return walletMap.values().stream()
        .map(walletData -> Wallet.of(walletData.walletId(), walletData.assets(), walletData.createdAt()))
        .toList();
  }

  private Asset mapAsset(ResultSet rs, int rowNum) throws SQLException {
    return Asset.of(WalletId.of(rs.getString("wallet_id")), Symbol.of(rs.getString("symbol")), Quantity.of(rs.getBigDecimal("quantity")),
        Price.of(rs.getBigDecimal("price")), rs.getObject("updated_at", OffsetDateTime.class));
  }

  private record WalletData(WalletId walletId, OffsetDateTime createdAt, List<Asset> assets) {}
}
