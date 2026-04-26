package com.tpa.mapper;

import com.tpa.dto.response.ClaimResponse;
import com.tpa.entity.Claim;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ClaimMapper {

    @Mapping(source = "user.username", target = "username")
    ClaimResponse toDto(Claim claim);

    @Mapping(source = "user.username", target = "username")
    List<ClaimResponse> toDtoList(List<Claim> claims);
}
