package io.diagrid.dapr;

import dev.openfeature.sdk.OpenFeatureAPI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dev.openfeature.contrib.providers.flagd.FlagdOptions;
import dev.openfeature.contrib.providers.flagd.FlagdProvider;

@Configuration
public class OpenFeatureBeans {

  private static String fhost = System.getenv("FLAGD_HOST");
  private static String flagdHost = (fhost == null) ? "localhost" : fhost;
  
  private static String fport = System.getenv("FLAGD_PORT");
  private static String flagdPort = (fport == null) ? "8013" : fport;

  @Bean
  public OpenFeatureAPI OpenFeatureAPI() {
      final OpenFeatureAPI openFeatureAPI = OpenFeatureAPI.getInstance();
      // Use flagd as the OpenFeature provider and use default configurations

      // Create a flagd instance with default options
      FlagdProvider flagd = new FlagdProvider(FlagdOptions.builder()
      .host(flagdHost)
      .port(Integer.parseInt(flagdPort))
      .build()
      );

      openFeatureAPI.setProvider(flagd);
      return openFeatureAPI;
  }
}
