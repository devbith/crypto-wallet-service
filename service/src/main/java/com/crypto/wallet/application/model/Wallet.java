package com.crypto.wallet.application.model;

import com.crypto.wallet.application.model.primitives.Price;
import com.crypto.wallet.application.model.primitives.Symbol;
import com.crypto.wallet.application.model.primitives.WalletId;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record Wallet(WalletId walletId, List<Asset> assets, OffsetDateTime createdAt) {
    
    public Wallet {
        Objects.requireNonNull(walletId, "WalletId cannot be null");
        Objects.requireNonNull(assets, "Assets cannot be null");
        Objects.requireNonNull(createdAt, "CreatedAt cannot be null");
        
        assets = List.copyOf(assets);
        
        assets.forEach(asset -> {
            if (!asset.walletId().equals(walletId)) {
                throw new IllegalArgumentException("Asset does not belong to this wallet: " + asset.symbol());
            }
        });
    }
    
    public static Wallet create(WalletId walletId) {
        return new Wallet(walletId, Collections.emptyList(), OffsetDateTime.now());
    }
    
    public static Wallet of(WalletId walletId, List<Asset> assets, OffsetDateTime createdAt) {
        return new Wallet(walletId, assets, createdAt);
    }
    
    public Price calculateTotalValue() {
        return assets.stream()
                .map(Asset::calculateValue)
                .reduce(Price.zero(), Price::add);
    }
    
    public Optional<Asset> findAssetBySymbol(Symbol symbol) {
        return assets.stream()
                .filter(asset -> asset.symbol().equals(symbol))
                .findFirst();
    }
    
    public boolean hasAsset(Symbol symbol) {
        return findAssetBySymbol(symbol).isPresent();
    }
    
    public int getAssetCount() {
        return assets.size();
    }
    
    @Override
    public String toString() {
        return "Wallet{" +
                "walletId=" + walletId +
                ", assetCount=" + assets.size() +
                ", totalValue=" + calculateTotalValue() +
                ", createdAt=" + createdAt +
                '}';
    }
}
