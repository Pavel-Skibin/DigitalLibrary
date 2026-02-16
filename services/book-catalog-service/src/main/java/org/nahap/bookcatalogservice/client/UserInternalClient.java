package org.nahap.bookcatalogservice.client;

import org.nahap.bookcatalogservice.client.user.InternalStatisticsApiApi;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * Feign client for User Service Internal API
 */
@FeignClient(
        name = "user-service",
        url = "${services.user.url}"
)
public interface UserInternalClient extends InternalStatisticsApiApi {
    // Methods inherited from InternalStatisticsApiApi:
    // - getTotalUsersCount()
    // - getUsersByRole()
}
