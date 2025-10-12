package com.crypto.wallet.application.port.in;

import com.crypto.wallet.application.model.primitives.Price;
import com.crypto.wallet.application.model.primitives.Quantity;
import com.crypto.wallet.application.model.primitives.Symbol;
import java.math.BigDecimal;
import java.util.List;

public interface ProfitSimulationUseCase {
    
    ProfitSimulationResult simulateProfit(List<AssetSimulation> assets);
    
    record AssetSimulation(Symbol symbol, Quantity quantity, Price originalValue) {}
    
    record ProfitSimulationResult(
            Price totalCurrentValue,
            Symbol bestAsset,
            BigDecimal bestPerformance,
            Symbol worstAsset,
            BigDecimal worstPerformance
    ) {}
}
