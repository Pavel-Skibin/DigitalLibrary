
package org.nahap.digital_library_backend.service.impl;

import lombok.RequiredArgsConstructor;
import org.nahap.digital_library_backend.entity.UserRole;
import org.nahap.digital_library_backend.exception.RoleNotFoundException;
import org.nahap.digital_library_backend.repository.UserRoleRepository;
import org.nahap.digital_library_backend.service.UserRoleService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserRoleServiceImpl implements UserRoleService {

    private final UserRoleRepository userRoleRepository;

    @Override
    public UserRole getRoleByName(String roleName) {
        if (roleName == null || roleName.isBlank()) {
            throw new RoleNotFoundException("Название роли не может быть пустым");
        }
        return userRoleRepository.findByName(roleName.trim())
                .orElseThrow(() -> new RoleNotFoundException("Роль '" + roleName.trim() + "' не найдена"));
    }

    @Override
    public UserRole getRoleById(Integer roleId) {
        if (roleId == null) {
            throw new RoleNotFoundException("ID роли не может быть null");
        }
        return userRoleRepository.findById(roleId)
                .orElseThrow(() -> new RoleNotFoundException("Роль с ID " + roleId + " не найдена"));
    }
}