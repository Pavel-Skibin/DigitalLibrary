package org.nahap.digital_library_backend.service;

import org.nahap.digital_library_backend.entity.UserRole;

public interface UserRoleService {
    UserRole getRoleByName(String roleName);
    UserRole getRoleById(Integer roleId);
}