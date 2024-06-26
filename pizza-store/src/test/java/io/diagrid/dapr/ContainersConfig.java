package io.diagrid.dapr;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.MountableFile;
import org.wiremock.integrations.testcontainers.WireMockContainer;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

@TestConfiguration(proxyBeanMethods = false)
public class ContainersConfig {


    @Bean
    public DaprContainer getDapr(DynamicPropertyRegistry registry) {
        DaprContainer dapr = (
                new DaprContainer("daprio/daprd")).
                withAppName("local-dapr-app")
                .withAppPort(8080)
                .withAppChannelAddress("host.testcontainers.internal");
        Testcontainers.exposeHostPorts(new int[]{8080});
        dapr.start();

        System.setProperty("dapr.grpc.port", Integer.toString(dapr.getGRPCPort()));
        System.setProperty("dapr.http.port", Integer.toString(dapr.getHTTPPort()));

        registry.add("dapr.grpc.port", dapr::getGRPCPort);
        registry.add("dapr.http.port", dapr::getHTTPPort);

        return dapr;

    }

    @Bean
    WireMockContainer wireMockContainer(DynamicPropertyRegistry properties) {
        var container = new WireMockContainer("wiremock/wiremock:3.1.0")
                .withMappingFromResource("kitchen", "kitchen-service-stubs.json");

        properties.add("dapr-http.base-url", container::getBaseUrl);
        properties.add("DAPR_HTTP_ENDPOINT", container::getBaseUrl);
        return container;
    }

    @Bean
    public GenericContainer getFlagd(DynamicPropertyRegistry registry) {
        String config = """
                {
                  "$schema": "https://flagd.dev/schema/v0/flags.json",
                  "flags": {
                    "v2_enabled": {
                      "state": "ENABLED",
                      "variants": {
                        "true": true,
                        "false": false
                      },
                      "defaultVariant": "true"
                    }
                  }
                }""";

        GenericContainer flagd = new GenericContainer("ghcr.io/open-feature/flagd:latest")
                .withExposedPorts(8013)
                .withCommand("start", "--uri", "file:/my.flagd.json")
                .withCopyToContainer(Transferable.of(config),
                        "/my.flagd.json")
                .waitingFor(Wait.forLogMessage(".*Flag Evaluation listening at.*", 1));


        registry.add("FLAGD_PRT", flagd::getFirstMappedPort);

        return flagd;
    }









}
