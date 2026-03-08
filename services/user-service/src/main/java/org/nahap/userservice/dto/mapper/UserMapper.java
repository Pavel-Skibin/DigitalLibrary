package org.nahap.userservice.dto.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.nahap.userservice.dto.response.UserResponse;
import org.nahap.userservice.entity.User;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(source = "role.name", target = "roleName")
    UserResponse toResponse(User user);
}
