package com.crypto.wallet.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.crypto.wallet.application.port.in.ProfitSimulationUseCase;
import com.crypto.wallet.application.port.in.ProfitSimulationUseCase.AssetSimulation;
import com.crypto.wallet.application.port.out.CryptoGateway;
import com.crypto.wallet.application.model.primitives.Price;
import com.crypto.wallet.application.model.primitives.Quantity;
import com.crypto.wallet.application.model.primitives.Symbol;
import com.crypto.wallet.application.service.ProfitCalculationService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProfitSimulationWorkflowTest {

  @Mock
  private CryptoGateway cryptoGateway;

  @Mock
  private ProfitCalculationService profitCalculationService;

  private ProfitSimulationWorkflow profitSimulationWorkflow;

  private Symbol btcSymbol;
  private Symbol ethSymbol;
  private Quantity btcQuantity;
  private Quantity ethQuantity;
  private Price btcOriginalValue;
  private Price ethOriginalValue;
  private Price btcCurrentPrice;
  private Price ethCurrentPrice;

  @BeforeEach
  void setUp() {
    profitSimulationWorkflow = new ProfitSimulationWorkflow(cryptoGateway, profitCalculationService);

    btcSymbol = Symbol.of("BTC");
    ethSymbol = Symbol.of("ETH");
    btcQuantity = Quantity.of(new BigDecimal("0.5"));
    ethQuantity = Quantity.of(new BigDecimal("4.25"));
    btcOriginalValue = Price.of(new BigDecimal("35000"));
    ethOriginalValue = Price.of(new BigDecimal("15310.71"));
    btcCurrentPrice = Price.of(new BigDecimal("94700.00"));
    ethCurrentPrice = Price.of(new BigDecimal("4123.52"));
  }

  @Test
  void simulateProfit_WhenEmptyAssetList_ShouldReturnZeroResult() {
    List<AssetSimulation> emptyAssets = List.of();
    var result = profitSimulationWorkflow.simulateProfit(emptyAssets);

    assertThat(result).isNotNull();
    assertThat(result.totalCurrentValue().value()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(result.bestAsset()).isNull();
    assertThat(result.bestPerformance()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(result.worstAsset()).isNull();
    assertThat(result.worstPerformance()).isEqualByComparingTo(BigDecimal.ZERO);

    verifyNoInteractions(cryptoGateway, profitCalculationService);
  }

  @Test
  void simulateProfit_WhenInvalidInput_ShouldThrowException() {
    var invalidAsset = new ProfitSimulationUseCase.AssetSimulation(btcSymbol, btcQuantity, btcOriginalValue);
    List<AssetSimulation> assets = List.of(invalidAsset);

    when(profitCalculationService.isValidSimulationInput(btcQuantity, btcOriginalValue)).thenReturn(false);

    assertThatThrownBy(() -> profitSimulationWorkflow.simulateProfit(assets)).isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Invalid simulation input for symbol: " + btcSymbol.value());

    verify(profitCalculationService).isValidSimulationInput(btcQuantity, btcOriginalValue);
    verifyNoInteractions(cryptoGateway);
  }

  @Test
  void simulateProfit_WhenValidAssets_ShouldCalculateCorrectResult() {
    var btcAsset = new AssetSimulation(btcSymbol, btcQuantity, btcOriginalValue);
    var ethAsset = new AssetSimulation(ethSymbol, ethQuantity, ethOriginalValue);
    List<AssetSimulation> assets = List.of(btcAsset, ethAsset);

    var btcCurrentValue = Price.of(new BigDecimal("47350.00"));
    var ethCurrentValue = Price.of(new BigDecimal("17524.97"));
    var totalPortfolioValue = Price.of(new BigDecimal("64874.97"));

    var btcProfitPercentage = new BigDecimal("35.35");
    var ethProfitPercentage = new BigDecimal("2.70");

    when(profitCalculationService.isValidSimulationInput(btcQuantity, btcOriginalValue)).thenReturn(true);
    when(profitCalculationService.isValidSimulationInput(ethQuantity, ethOriginalValue)).thenReturn(true);

    Map<Symbol, Price> currentPrices = Map.of(btcSymbol, btcCurrentPrice, ethSymbol, ethCurrentPrice);
    when(cryptoGateway.getPrices(List.of(btcSymbol, ethSymbol))).thenReturn(currentPrices);

    when(profitCalculationService.calculateCurrentValue(btcQuantity, btcCurrentPrice)).thenReturn(btcCurrentValue);
    when(profitCalculationService.calculateCurrentValue(ethQuantity, ethCurrentPrice)).thenReturn(ethCurrentValue);
    when(profitCalculationService.calculateTotalPortfolioValue(List.of(btcCurrentValue, ethCurrentValue))).thenReturn(totalPortfolioValue);

    Price btcOriginalPricePerUnit = Price.of(new BigDecimal("70000.00"));
    Price ethOriginalPricePerUnit = Price.of(new BigDecimal("3602.52"));
    when(profitCalculationService.calculateProfitLossPercentage(btcOriginalPricePerUnit, btcCurrentPrice)).thenReturn(btcProfitPercentage);
    when(profitCalculationService.calculateProfitLossPercentage(ethOriginalPricePerUnit, ethCurrentPrice)).thenReturn(ethProfitPercentage);

    when(profitCalculationService.findBestPerformingAsset(any(Map.class))).thenReturn(Map.entry(btcSymbol, btcProfitPercentage));
    when(profitCalculationService.findWorstPerformingAsset(any(Map.class))).thenReturn(Map.entry(ethSymbol, ethProfitPercentage));

    // When
    var result = profitSimulationWorkflow.simulateProfit(assets);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.totalCurrentValue()).isEqualTo(totalPortfolioValue);
    assertThat(result.bestAsset()).isEqualTo(btcSymbol);
    assertThat(result.bestPerformance()).isEqualByComparingTo(btcProfitPercentage);
    assertThat(result.worstAsset()).isEqualTo(ethSymbol);
    assertThat(result.worstPerformance()).isEqualByComparingTo(ethProfitPercentage);

    verify(profitCalculationService).isValidSimulationInput(btcQuantity, btcOriginalValue);
    verify(profitCalculationService).isValidSimulationInput(ethQuantity, ethOriginalValue);
    verify(cryptoGateway).getPrices(List.of(btcSymbol, ethSymbol));
    verify(profitCalculationService).calculateCurrentValue(btcQuantity, btcCurrentPrice);
    verify(profitCalculationService).calculateCurrentValue(ethQuantity, ethCurrentPrice);
    verify(profitCalculationService).calculateTotalPortfolioValue(List.of(btcCurrentValue, ethCurrentValue));
    verify(profitCalculationService).findBestPerformingAsset(any(Map.class));
    verify(profitCalculationService).findWorstPerformingAsset(any(Map.class));
  }

  @Test
  void simulateProfit_WhenSomePricesNotAvailable_ShouldSkipMissingAssets() {
    // Given
    var btcAsset = new AssetSimulation(btcSymbol, btcQuantity, btcOriginalValue);
    var ethAsset = new AssetSimulation(ethSymbol, ethQuantity, ethOriginalValue);
    List<AssetSimulation> assets = List.of(btcAsset, ethAsset);

    var btcCurrentValue = Price.of(new BigDecimal("47350.00"));
    var totalPortfolioValue = Price.of(new BigDecimal("47350.00"));
    var btcProfitPercentage = new BigDecimal("35.35");

    when(profitCalculationService.isValidSimulationInput(btcQuantity, btcOriginalValue)).thenReturn(true);
    when(profitCalculationService.isValidSimulationInput(ethQuantity, ethOriginalValue)).thenReturn(true);
    Map<Symbol, Price> currentPrices = Map.of(btcSymbol, btcCurrentPrice);
    when(cryptoGateway.getPrices(List.of(btcSymbol, ethSymbol))).thenReturn(currentPrices);
    when(profitCalculationService.calculateCurrentValue(btcQuantity, btcCurrentPrice)).thenReturn(btcCurrentValue);
    when(profitCalculationService.calculateTotalPortfolioValue(List.of(btcCurrentValue))).thenReturn(totalPortfolioValue);

    Price btcOriginalPricePerUnit = Price.of(new BigDecimal("70000.00"));
    when(profitCalculationService.calculateProfitLossPercentage(btcOriginalPricePerUnit, btcCurrentPrice)).thenReturn(btcProfitPercentage);

    when(profitCalculationService.findBestPerformingAsset(any(Map.class))).thenReturn(Map.entry(btcSymbol, btcProfitPercentage));
    when(profitCalculationService.findWorstPerformingAsset(any(Map.class))).thenReturn(Map.entry(btcSymbol, btcProfitPercentage));

    var result = profitSimulationWorkflow.simulateProfit(assets);

    assertThat(result).isNotNull();
    assertThat(result.totalCurrentValue()).isEqualTo(totalPortfolioValue);
    assertThat(result.bestAsset()).isEqualTo(btcSymbol);
    assertThat(result.worstAsset()).isEqualTo(btcSymbol);

    verify(profitCalculationService, never()).calculateCurrentValue(ethQuantity, ethCurrentPrice);
  }

  @Test
  void simulateProfit_WhenNoBestOrWorstAsset_ShouldReturnNullValues() {
    var btcAsset = new AssetSimulation(btcSymbol, btcQuantity, btcOriginalValue);
    List<AssetSimulation> assets = List.of(btcAsset);

    Price totalPortfolioValue = Price.of(new BigDecimal("47350.00"));

    when(profitCalculationService.isValidSimulationInput(btcQuantity, btcOriginalValue)).thenReturn(true);
    Map<Symbol, Price> currentPrices = Map.of(btcSymbol, btcCurrentPrice);
    when(cryptoGateway.getPrices(List.of(btcSymbol))).thenReturn(currentPrices);

    when(profitCalculationService.calculateCurrentValue(btcQuantity, btcCurrentPrice)).thenReturn(Price.of(new BigDecimal("47350.00")));
    when(profitCalculationService.calculateTotalPortfolioValue(any(List.class))).thenReturn(totalPortfolioValue);
    when(profitCalculationService.calculateProfitLossPercentage(any(Price.class), eq(btcCurrentPrice))).thenReturn(new BigDecimal("35.35"));

    // Mock best/worst asset finding - return null
    when(profitCalculationService.findBestPerformingAsset(any(Map.class))).thenReturn(null);
    when(profitCalculationService.findWorstPerformingAsset(any(Map.class))).thenReturn(null);

    // When
    ProfitSimulationUseCase.ProfitSimulationResult result = profitSimulationWorkflow.simulateProfit(assets);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.totalCurrentValue()).isEqualTo(totalPortfolioValue);
    assertThat(result.bestAsset()).isNull();
    assertThat(result.bestPerformance()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(result.worstAsset()).isNull();
    assertThat(result.worstPerformance()).isEqualByComparingTo(BigDecimal.ZERO);
  }
}
