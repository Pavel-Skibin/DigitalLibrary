package org.nahap.commentratingservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class CommentRatingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CommentRatingServiceApplication.class, args);
    }
}
