package org.nahap.userservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nahap.userservice.api.internal.InternalUserApiApi;
import org.nahap.userservice.api.internal.model.UserResponse;
import org.nahap.userservice.api.internal.model.ValidationResponse;
import org.nahap.userservice.dto.mapper.InternalUserMapper;
import org.nahap.userservice.dto.response.BookViewResponse;
import org.nahap.userservice.entity.User;
import org.nahap.userservice.repository.UserRepository;
import org.nahap.userservice.service.UserBookFavoriteService;
import org.nahap.userservice.service.UserBookViewService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZoneOffset;
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
    private final UserBookViewService userBookViewService;
    private final UserBookFavoriteService userBookFavoriteService;

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

    @Override
    public ResponseEntity<List<org.nahap.userservice.api.internal.model.BookViewResponse>> getUserHistory(Integer userId) {
        log.debug("Internal API: Getting user history for userId: {}", userId);

        if (!userRepository.existsById(userId)) {
            return ResponseEntity.notFound().build();
        }

        Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE);
        List<BookViewResponse> history = userBookViewService.getUserHistory(userId, pageable).getContent();

        List<org.nahap.userservice.api.internal.model.BookViewResponse> response = history.stream()
                .map(view -> {
                    var apiView = new org.nahap.userservice.api.internal.model.BookViewResponse();
                    apiView.setBookId(view.getBookId());
                    apiView.setViewCount(view.getSessionsCount());
                    if (view.getLastReadAt() != null) {
                        apiView.setLastViewedAt(view.getLastReadAt().atOffset(ZoneOffset.UTC));
                    }
                    apiView.setTotalTimeSpent(view.getTotalReadingTimeSeconds() != null ? view.getTotalReadingTimeSeconds().longValue() : 0L);
                    apiView.setProgressPercent(view.getIsCompleted() != null && view.getIsCompleted() ? 100.0 : null);
                    return apiView;
                })
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<List<Integer>> getUserFavorites(Integer userId) {
        log.debug("Internal API: Getting user favorites for userId: {}", userId);

        if (!userRepository.existsById(userId)) {
            return ResponseEntity.notFound().build();
        }
        
        List<Integer> favoriteBooks = userBookFavoriteService.getUserFavoriteBookIds(userId);
        return ResponseEntity.ok(favoriteBooks);
    }

    @Override
    public ResponseEntity<List<Integer>> getUserViewedBooks(Integer userId) {
        log.debug("Internal API: Getting user viewed books for userId: {}", userId);

        if (!userRepository.existsById(userId)) {
            return ResponseEntity.notFound().build();
        }
        
        List<Integer> viewedBooks = userBookViewService.getUserViewedBookIds(userId);
        return ResponseEntity.ok(viewedBooks);
    }
}
