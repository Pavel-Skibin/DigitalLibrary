package org.nahap.bookcatalogservice.service;

import org.nahap.bookcatalogservice.dto.request.BookmarkCreateRequest;
import org.nahap.bookcatalogservice.dto.request.BookmarkUpdateRequest;
import org.nahap.bookcatalogservice.dto.response.BookmarkResponse;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface BookmarkService {
    BookmarkResponse createBookmark(BookmarkCreateRequest request, Integer currentUserId);

    BookmarkResponse updateBookmark(Integer bookmarkId, BookmarkUpdateRequest request, Integer currentUserId);

    void deleteBookmark(Integer bookmarkId, Integer currentUserId);

    List<BookmarkResponse> getBookmarksByUser(Integer userId, Pageable pageable);

    BookmarkResponse getBookmarkById(Integer bookmarkId, Integer currentUserId);

    Page<BookmarkResponse> getBookmarksByUserAndBook(Integer userId, Integer bookId, Pageable pageable);
}