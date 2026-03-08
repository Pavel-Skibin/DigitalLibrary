package org.nahap.userservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nahap.userservice.api.internal.InternalStatisticsApiApi;
import org.nahap.userservice.api.internal.model.RoleStatistics;
import org.nahap.userservice.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Internal API Controller for user statistics
 * Implements OpenAPI generated interface
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class UserStatisticsInternalController implements InternalStatisticsApiApi {

    private final UserRepository userRepository;

    @Override
    public ResponseEntity<Long> getTotalUsersCount() {
        log.debug("Internal API: Getting total users count");
        Long count = userRepository.countActiveUsers();
        return ResponseEntity.ok(count);
    }

    @Override
    public ResponseEntity<List<RoleStatistics>> getUsersByRole() {
        log.debug("Internal API: Getting users count by role");
        
        List<Object[]> results = userRepository.countUsersByRole();
        
        List<RoleStatistics> statistics = results.stream()
                .map(row -> {
                    RoleStatistics stat = new RoleStatistics();
                    stat.setRoleName((String) row[0]);
                    stat.setUserCount(((Number) row[1]).longValue());
                    return stat;
                })
                .toList();
        
        return ResponseEntity.ok(statistics);
    }
}
