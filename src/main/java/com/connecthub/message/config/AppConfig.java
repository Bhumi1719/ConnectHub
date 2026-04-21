package com.connecthub.message.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig {

    // RestTemplate — used to call Room Service
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
