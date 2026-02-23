package com.smartlogix.mapper;

import com.smartlogix.domain.entity.Order;
import com.smartlogix.dto.OrderRequestDTO;
import com.smartlogix.dto.OrderResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    @Mapping(source = "tenant.id", target = "tenantId")
    OrderResponseDTO toResponseDTO(Order order);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenant", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Order toEntity(OrderRequestDTO dto);
}
