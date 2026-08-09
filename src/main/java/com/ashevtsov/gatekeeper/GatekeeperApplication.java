package com.ashevtsov.gatekeeper;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Точка входа в GateKeeper — OAuth2/OIDC Authorization Server + API Gateway
 */
@SpringBootApplication
@EnableScheduling
@EnableAsync
public class GatekeeperApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatekeeperApplication.class, args);
    }
}
