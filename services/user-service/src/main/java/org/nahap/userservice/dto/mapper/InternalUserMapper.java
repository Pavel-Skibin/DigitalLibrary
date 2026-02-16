package org.nahap.userservice.dto.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.nahap.userservice.api.internal.model.UserResponse;
import org.nahap.userservice.entity.User;

/**
 * Mapper for converting User entity to OpenAPI generated UserResponse DTO
 */
@Mapper(componentModel = "spring")
public interface InternalUserMapper {

    @Mapping(source = "role.name", target = "role")
    @Mapping(target = "isDeleted", expression = "java(user.getDeletedAt() != null)")
    UserResponse toResponse(User user);
}
