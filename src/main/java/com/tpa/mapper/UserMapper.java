package com.tpa.mapper;

import com.tpa.dto.request.UserRequest;
import com.tpa.dto.response.UserResponse;
import com.tpa.entity.User;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserMapper {

    User toUser(UserRequest userRequest);

    UserResponse toUserResponse(User user);

    List<UserResponse> toUserResponses(List<User> users);
}