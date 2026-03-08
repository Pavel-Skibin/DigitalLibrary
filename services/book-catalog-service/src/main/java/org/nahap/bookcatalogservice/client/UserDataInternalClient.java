package org.nahap.bookcatalogservice.client;

import org.nahap.bookcatalogservice.client.user.InternalUserApiApi;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * Feign client for User Service Internal User API (for user data enrichment)
 */
@FeignClient(
        name = "user-service-data",
        url = "${USER_SERVICE_URL:http://localhost:8081}"
)
public interface UserDataInternalClient extends InternalUserApiApi {
    // Methods inherited from InternalUserApiApi:
    // - getUserById(Integer userId)
    // - validateUser(Integer userId)
    // - getUsersBatch(List<Integer> userIds)
}
