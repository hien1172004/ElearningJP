package com.jp.elearningjp.dto.response.dictionary;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class KanaRowGroupResponse {
    String rowName;        // Ví dụ: "Hàng A (あ / ア)"
    String rowRomaji;      // Ví dụ: "a, i, u, e, o"
    List<KanaCharacterResponse> characters; // 5 chữ cái trong hàng (hoặc ít hơn với hàng Ya, Wa, N)
}
