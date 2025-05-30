package com.example.xiangqi.mapper;

import com.example.xiangqi.dto.request.MatchContractRequest;
import com.example.xiangqi.dto.response.MatchStateResponse;
import com.example.xiangqi.entity.redis.MatchContractEntity;
import com.example.xiangqi.entity.redis.MatchStateEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface MatchStateMapper {
	// Add
	@Mapping(target = "redPlayer", source = "redPlayer")
	@Mapping(target = "blackPlayer", source = "blackPlayer")
	MatchStateResponse toResponse(MatchStateEntity entity);
}
