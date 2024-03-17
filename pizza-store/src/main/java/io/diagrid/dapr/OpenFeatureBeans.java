package io.diagrid.dapr;

import dev.openfeature.sdk.EvaluationContext;
import dev.openfeature.sdk.ImmutableContext;
import dev.openfeature.sdk.OpenFeatureAPI;
import dev.openfeature.sdk.Value;

import java.util.HashMap;
import java.util.Map;

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

  private static String fdeadline = System.getenv("FLAGD_DEADLINE_MS");
  private static String flagdDeadline = (fdeadline == null) ? "1000" : fdeadline;

  @Bean
  public OpenFeatureAPI OpenFeatureAPI() {
      final OpenFeatureAPI openFeatureAPI = OpenFeatureAPI.getInstance();
      
      // Use flagd as the OpenFeature provider and use default configurations
      FlagdProvider flagd = new FlagdProvider(FlagdOptions.builder()
      .host(flagdHost)
      .port(Integer.parseInt(flagdPort))
      .deadline(Integer.parseInt(flagdDeadline))
      .build()
      );

      openFeatureAPI.setProviderAndWait(flagd);
      return openFeatureAPI;
  }
}
