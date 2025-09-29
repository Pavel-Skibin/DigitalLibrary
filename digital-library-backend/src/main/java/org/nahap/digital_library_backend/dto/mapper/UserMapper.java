package org.nahap.digital_library_backend.dto.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.nahap.digital_library_backend.dto.request.UserCreateRequest;
import org.nahap.digital_library_backend.dto.request.UserUpdateRequest;
import org.nahap.digital_library_backend.dto.response.UserResponse;
import org.nahap.digital_library_backend.entity.User;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "roleName", source = "role.name")
    @Mapping(target = "isDeleted", expression = "java(user.getDeletedAt() != null)")
    UserResponse toResponse(User user);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "comments", ignore = true)
    @Mapping(target = "ratings", ignore = true)
    @Mapping(target = "bookmarks", ignore = true)
    User toEntity(UserCreateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "comments", ignore = true)
    @Mapping(target = "ratings", ignore = true)
    @Mapping(target = "bookmarks", ignore = true)
    void updateEntityFromRequest(@MappingTarget User user, UserUpdateRequest request);
}