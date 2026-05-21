package com.AQARK.AQARK_RENT_MANAGEMENT.Security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "app.security")
@Data
public class SecurityProperties {

    private Cookie cookie = new Cookie();
    private Cors cors = new Cors();
    private Endpoints endpoints = new Endpoints();
    private Sas sas = new Sas();

    @Data
    public static class Cookie {
        private String tokenName = "SAS_TOKEN";
    }

    @Data
    public static class Cors {
        private List<String> allowedOrigins = new ArrayList<>();
        private List<String> allowedMethods = new ArrayList<>();
        private List<String> allowedHeaders = new ArrayList<>();
        private boolean allowCredentials = true;
        private long maxAge = 3600;
    }

    @Data
    public static class Endpoints {
        private List<String> publicEndpoints = new ArrayList<>();
        private List<String> authenticatedEndpoints = new ArrayList<>();
    }

    @Data
    public static class Sas {
        private String baseUrl;
    }
}