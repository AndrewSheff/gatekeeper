package com.ashevtsov.gatekeeper.tenant;

import com.ashevtsov.gatekeeper.tenant.dto.TenantResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TenantMapper {

    TenantResponse toResponse(Tenant tenant);
}
