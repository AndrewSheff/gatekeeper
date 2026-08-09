package com.ashevtsov.gatekeeper.gateway;

import com.ashevtsov.gatekeeper.gateway.dto.RouteResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Маппер маршрутов — Entity -> Response DTO. MapStruct разрулит вложенные трансформации.
 */
@Mapper(componentModel = "spring")
public interface RouteMapper {

    @Mapping(target = "tenantId", source = "tenant.id")
    RouteResponse toResponse(GatewayRoute route);

    RouteResponse.TransformationResponse toTransformationResponse(RouteTransformation transformation);
}
