package org.nahap.userservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nahap.userservice.api.internal.InternalUserApiApi;
import org.nahap.userservice.api.internal.model.UserResponse;
import org.nahap.userservice.api.internal.model.ValidationResponse;
import org.nahap.userservice.dto.mapper.InternalUserMapper;
import org.nahap.userservice.entity.User;
import org.nahap.userservice.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Internal API Controller for inter-service communication
 * Implements OpenAPI generated interface
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class UserInternalController implements InternalUserApiApi {

    private final UserRepository userRepository;
    private final InternalUserMapper internalUserMapper;

    @Override
    public ResponseEntity<UserResponse> getUserById(Integer id) {
        log.debug("Internal API: Getting user by id: {}", id);
        
        return userRepository.findActiveById(id)
                .map(internalUserMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Override
    public ResponseEntity<ValidationResponse> validateUser(Integer id) {
        log.debug("Internal API: Validating user existence: {}", id);
        
        boolean exists = userRepository.findActiveById(id).isPresent();
        
        ValidationResponse response = new ValidationResponse(exists);
        if (!exists) {
            response.setMessage("User not found or deleted");
        }
        
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<Map<String, UserResponse>> getUsersBatch(List<Integer> userIds) {
        log.debug("Internal API: Getting batch of users: {}", userIds);
        
        List<User> users = userRepository.findAllById(userIds);
        
        // Filter only active users and map to UserResponse
        Map<String, UserResponse> userMap = users.stream()
                .filter(user -> user.getDeletedAt() == null)
                .collect(Collectors.toMap(
                        user -> String.valueOf(user.getId()),
                        internalUserMapper::toResponse
                ));
        
        return ResponseEntity.ok(userMap);
    }
}
