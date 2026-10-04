package com.jp.elearningjp.dto.request.auth;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class LogoutRequest {
    String token;
}