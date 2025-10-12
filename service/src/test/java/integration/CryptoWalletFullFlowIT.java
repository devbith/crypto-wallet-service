package integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.crypto.wallet.BaseIT;
import com.crypto.wallet.infrastructure.adapter.in.rest.dto.AddAssetRequest;
import com.crypto.wallet.infrastructure.adapter.in.rest.dto.AssetSimulationDto;
import com.crypto.wallet.infrastructure.adapter.in.rest.dto.CreateWalletRequest;
import com.crypto.wallet.infrastructure.adapter.in.rest.dto.ProfitSimulationRequest;
import com.crypto.wallet.infrastructure.adapter.in.rest.dto.ProfitSimulationResponse;
import com.crypto.wallet.infrastructure.adapter.in.rest.dto.UserResponse;
import com.crypto.wallet.infrastructure.adapter.in.rest.dto.WalletResponse;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class CryptoWalletFullFlowIT extends BaseIT {

  @Test
  void cryptoWalletFullFlowTest() {
    // Create wallet
    CreateWalletRequest createRequest = new CreateWalletRequest("spider.man@marvel.com");
    ResponseEntity<UserResponse> createResponse = restTemplate.postForEntity("/api/v1/wallets", createRequest, UserResponse.class);

    assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    String walletId = createResponse.getBody().walletId();

    // Add asset
    AddAssetRequest btcRequest = new AddAssetRequest("BTC", new BigDecimal("0.5"), new BigDecimal("70000"));
    ResponseEntity<WalletResponse> btcResponse = restTemplate.postForEntity("/api/v1/wallets/" + walletId + "/assets", btcRequest,
        WalletResponse.class);

    assertThat(btcResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(btcResponse.getBody().assets()).hasSize(1);
    assertThat(btcResponse.getBody().total()).isEqualByComparingTo(new BigDecimal("35000"));

    // Add asset
    AddAssetRequest ethRequest = new AddAssetRequest("ETH", new BigDecimal("4.25"), new BigDecimal("3600"));
    ResponseEntity<WalletResponse> ethResponse = restTemplate.postForEntity("/api/v1/wallets/" + walletId + "/assets", ethRequest,
        WalletResponse.class);

    assertThat(ethResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(ethResponse.getBody().assets()).hasSize(2);
    assertThat(ethResponse.getBody().total()).isEqualByComparingTo(new BigDecimal("50300")); // 35000 + 15300

    // Get wallet
    ResponseEntity<WalletResponse> walletResponse = restTemplate.getForEntity("/api/v1/wallets/" + walletId, WalletResponse.class);

    assertThat(walletResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(walletResponse.getBody().assets()).hasSize(2);

    // Run profit simulation
    AssetSimulationDto btcSimulation = new AssetSimulationDto("BTC", new BigDecimal("0.5"), new BigDecimal("25000"));
    AssetSimulationDto ethSimulation = new AssetSimulationDto("ETH", new BigDecimal("4.25"), new BigDecimal("10000"));
    ProfitSimulationRequest simulationRequest = new ProfitSimulationRequest(List.of(btcSimulation, ethSimulation));

    ResponseEntity<ProfitSimulationResponse> simulationResponse = restTemplate.postForEntity("/api/v1/profit-simulation", simulationRequest,
        ProfitSimulationResponse.class);

    assertThat(simulationResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(simulationResponse.getBody().total()).isGreaterThan(BigDecimal.ZERO);
    assertThat(simulationResponse.getBody().bestAsset()).isNotNull();
    assertThat(simulationResponse.getBody().worstAsset()).isNotNull();
  }
}
