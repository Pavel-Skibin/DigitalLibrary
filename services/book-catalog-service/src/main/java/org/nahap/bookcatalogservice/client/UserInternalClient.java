package org.nahap.bookcatalogservice.client;

import org.nahap.bookcatalogservice.client.user.InternalStatisticsApiApi;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * Feign client for User Service Internal API
 */
@FeignClient(
        name = "user-service",
        url = "${USER_SERVICE_URL:http://localhost:8081}"
)
public interface UserInternalClient extends InternalStatisticsApiApi {
    // Methods inherited from InternalStatisticsApiApi:
    // - getTotalUsersCount()
    // - getUsersByRole()
}
