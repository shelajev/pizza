package io.diagrid.dapr;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.wiremock.integrations.testcontainers.WireMockContainer;

import io.diagrid.dapr.profiles.DaprBasicProfile;


@SpringBootApplication
public class PizzaStoreAppTest {
    public static void main(String[] args) {
        SpringApplication.from(PizzaStore::main)
                .with(ContainersConfig.class)
                .run(args);
    }


}
