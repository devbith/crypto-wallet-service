package com.crypto.wallet.infrastructure.adapter.in.cronjob;

import com.crypto.wallet.application.port.in.PriceUpdateUseCase;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

public class PriceUpdateCronJob {

  private static final Logger logger = LoggerFactory.getLogger(PriceUpdateCronJob.class);

  private final PriceUpdateUseCase priceUpdateUseCase;

  public PriceUpdateCronJob(PriceUpdateUseCase priceUpdateUseCase) {
    this.priceUpdateUseCase = priceUpdateUseCase;
  }

  @Scheduled(fixedDelayString = "${price.update.frequency-in-ms:300000}")
  @SchedulerLock(name = "priceUpdateTask", lockAtMostFor = "9m", lockAtLeastFor = "1m")
  public void updatePrices() {
    try {
      priceUpdateUseCase.updateAllPrices();
      logger.info("Completed price update cron job");
    } catch (Exception e) {
      logger.error("Error during scheduled price update cron job: {}", e.getMessage(), e);
    }
  }

}
