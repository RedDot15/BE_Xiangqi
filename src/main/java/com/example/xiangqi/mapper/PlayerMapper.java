package com.example.xiangqi.mapper;

import com.example.xiangqi.dto.request.PlayerRequest;
import com.example.xiangqi.dto.response.PlayerResponse;
import com.example.xiangqi.entity.PlayerEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface PlayerMapper {
	// Add
	@Mapping(target = "password", ignore = true)
	@Mapping(target = "role", ignore = true)
	@Mapping(target = "rating", ignore = true)
	PlayerEntity toPlayerEntity(PlayerRequest playerRequest);

	// Response
	PlayerResponse toPlayerResponse(PlayerEntity playerEntity);
}
