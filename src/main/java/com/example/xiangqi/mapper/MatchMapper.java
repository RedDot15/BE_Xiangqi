package com.example.xiangqi.mapper;

import com.example.xiangqi.dto.request.PlayerRequest;
import com.example.xiangqi.dto.response.MatchResponse;
import com.example.xiangqi.dto.response.PlayerResponse;
import com.example.xiangqi.entity.MatchEntity;
import com.example.xiangqi.entity.PlayerEntity;
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
