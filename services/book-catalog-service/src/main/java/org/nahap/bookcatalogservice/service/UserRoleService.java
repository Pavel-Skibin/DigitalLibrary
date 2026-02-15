package org.nahap.bookcatalogservice.service;

import org.nahap.bookcatalogservice.entity.UserRole;

public interface UserRoleService {
    UserRole getRoleByName(String roleName);
    UserRole getRoleById(Integer roleId);
}