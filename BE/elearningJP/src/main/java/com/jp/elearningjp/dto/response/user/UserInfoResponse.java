package com.jp.elearningjp.dto.response.user;

import lombok.Data;

import com.jp.elearningjp.shared.enums.UserStatus;

@Data
public class UserInfoResponse {
    String fullName;
    String email;
    UserStatus status;

}