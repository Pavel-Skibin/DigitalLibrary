package org.nahap.bookcatalogservice.client;

import org.nahap.bookcatalogservice.client.commentrating.InternalStatisticsApiApi;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * Feign client for Comment Rating Service Internal API
 */
@FeignClient(
        name = "comment-rating-service",
        url = "${services.comment-rating.url}"
)
public interface CommentRatingInternalClient extends InternalStatisticsApiApi {
    // Methods inherited from InternalStatisticsApiApi:
    // - getTotalRatingsCount()
    // - getTotalCommentsCount()
    // - getDeletedCommentsPercentage()
    // - getRatingDistribution()
    // - getMostActiveCommenters(Integer size)
    // - getRecentComments(Integer size)
    // - getGlobalAverageRating()
    // - getAllBooksStatistics()
}
