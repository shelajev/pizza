//package io.diagrid.dapr;
//
//import dev.openfeature.sdk.*;
//
//import java.util.HashMap;
//import java.util.Map;
//
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//import dev.openfeature.contrib.providers.flagd.FlagdOptions;
//import dev.openfeature.contrib.providers.flagd.FlagdProvider;
//
//@Configuration
//public class OpenFeatureBeans {
//    @Value("${FLAGD_HOST:localhost}")
//    private String flagdHost;
//
//    @Value("${FLAGD_PRT:8013}")
//    private String flagdPort;
//
//    @Value("${FLAGD_DEADLINE_MS:1000}")
//    private String flagdDeadline;
//
//    @Bean
//    public OpenFeatureAPI OpenFeatureAPI() {
//        final OpenFeatureAPI openFeatureAPI = OpenFeatureAPI.getInstance();
//
//        // Use flagd as the OpenFeature provider and use default configurations
//        FlagdProvider flagd = new FlagdProvider(FlagdOptions.builder()
//                .host(flagdHost)
//                .port(Integer.parseInt(flagdPort))
//                .deadline(Integer.parseInt(flagdDeadline))
//                .build()
//        );
//
//        openFeatureAPI.setProviderAndWait(flagd);
//        return openFeatureAPI;
//    }
//
//    @Bean
//    public Client client(OpenFeatureAPI openFeatureAPI) {
//        return openFeatureAPI.getClient();
//    }
//
//}
