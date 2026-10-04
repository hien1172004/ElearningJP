package com.jp.elearningjp.dto.response.auth;

import lombok.Builder;
import lombok.Data;

@Builder
@Data

public class RefreshResponse {
    Boolean success;
    String accessToken;
}