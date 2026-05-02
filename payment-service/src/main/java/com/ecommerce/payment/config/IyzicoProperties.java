package com.ecommerce.payment.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "iyzico")
@Getter
@Setter
public class IyzicoProperties {
    private String apiKey;
    private String secretKey;
    private String baseUrl;
}
