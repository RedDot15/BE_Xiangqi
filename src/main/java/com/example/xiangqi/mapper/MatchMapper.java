package com.example.xiangqi.mapper;

import com.example.xiangqi.dto.response.MatchResponse;
import com.example.xiangqi.entity.my_sql.MatchEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface MatchMapper {
	// Response
	@Mapping(target = "redPlayerResponse", source = "redPlayerEntity")
	@Mapping(target = "blackPlayerResponse", source = "blackPlayerEntity")
	MatchResponse toResponse(MatchEntity matchEntity);
}
