package com.crypto.wallet.infrastructure.adapter.in.rest;

import com.crypto.wallet.application.port.in.ProfitSimulationUseCase;
import com.crypto.wallet.application.port.in.ProfitSimulationUseCase.AssetSimulation;
import com.crypto.wallet.application.model.primitives.Price;
import com.crypto.wallet.application.model.primitives.Quantity;
import com.crypto.wallet.application.model.primitives.Symbol;
import com.crypto.wallet.infrastructure.adapter.in.rest.dto.AssetSimulationDto;
import com.crypto.wallet.infrastructure.adapter.in.rest.dto.ProfitSimulationRequest;
import com.crypto.wallet.infrastructure.adapter.in.rest.dto.ProfitSimulationResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/profit-simulation")
public class ProfitSimulationController {

  private static final Logger logger = LoggerFactory.getLogger(ProfitSimulationController.class);

  private final ProfitSimulationUseCase profitSimulationUseCase;

  public ProfitSimulationController(ProfitSimulationUseCase profitSimulationUseCase) {
    this.profitSimulationUseCase = profitSimulationUseCase;
  }

  @PostMapping
  public ResponseEntity<ProfitSimulationResponse> simulateProfit(@Valid @RequestBody ProfitSimulationRequest request) {
    logger.info("Running profit simulation for {} assets", request.assets().size());

    List<ProfitSimulationUseCase.AssetSimulation> assetSimulations = request.assets().stream()
        .map(this::mapToAssetSimulation)
        .toList();

    var result = profitSimulationUseCase.simulateProfit(assetSimulations);

    var response = new ProfitSimulationResponse(result.totalCurrentValue().value(),
        result.bestAsset() != null ? result.bestAsset().value() : null, result.bestPerformance(),
        result.worstAsset() != null ? result.worstAsset().value() : null, result.worstPerformance());

    logger.info("Simulation completed - Total value: {}, Best: {} ({}%), Worst: {} ({}%)", response.total(), response.bestAsset(),
        response.bestPerformance(), response.worstAsset(), response.worstPerformance());

    return ResponseEntity.ok(response);
  }

  private AssetSimulation mapToAssetSimulation(AssetSimulationDto dto) {
    return new AssetSimulation(Symbol.of(dto.symbol()), Quantity.of(dto.quantity()), Price.of(dto.value()));
  }
}
