package org.nahap.commentratingservice.configuration;

import feign.Feign;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import org.nahap.commentratingservice.client.book.InternalBookApiApi;
import org.nahap.commentratingservice.client.user.InternalUserApiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Feign clients to communicate with User and Book services
 */
@Configuration
public class FeignClientConfiguration {

    @Value("${services.user.url:http://localhost:8081}")
    private String userServiceUrl;

    @Value("${services.book.url:http://localhost:8091}")
    private String bookServiceUrl;

    @Bean
    public InternalUserApiApi userServiceClient() {
        return Feign.builder()
                .encoder(new JacksonEncoder())
                .decoder(new JacksonDecoder())
                .target(InternalUserApiApi.class, userServiceUrl);
    }

    @Bean
    public InternalBookApiApi bookServiceClient() {
        return Feign.builder()
                .encoder(new JacksonEncoder())
                .decoder(new JacksonDecoder())
                .target(InternalBookApiApi.class, bookServiceUrl);
    }
}
