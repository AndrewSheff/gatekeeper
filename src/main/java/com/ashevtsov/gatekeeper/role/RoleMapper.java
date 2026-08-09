package com.ashevtsov.gatekeeper.role;

import com.ashevtsov.gatekeeper.role.dto.PermissionResponse;
import com.ashevtsov.gatekeeper.role.dto.RoleResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RoleMapper {

    @Mapping(target = "parentId", source = "parent.id")
    @Mapping(target = "tenantId", source = "tenant.id")
    RoleResponse toResponse(Role role);

    PermissionResponse toPermissionResponse(Permission permission);
}
