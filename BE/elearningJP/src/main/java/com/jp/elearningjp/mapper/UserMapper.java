package com.jp.elearningjp.mapper;

import com.jp.elearningjp.dto.request.user.RegisterRequest;
import com.jp.elearningjp.dto.response.user.UserInfoResponse;
import com.jp.elearningjp.dto.response.user.UserResponse;
import com.jp.elearningjp.entity.user.Role;
import com.jp.elearningjp.entity.user.User;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserInfoResponse toUserInfoResponse(User userEntity);

    User toUserEntity(RegisterRequest registerRequest);

    UserResponse toUserResponse(User userEntity);


    default String map(Role roleEntity) {
        return roleEntity.getName();
    }
}