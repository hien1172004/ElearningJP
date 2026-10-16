package com.jp.elearningjp.dto.response.dictionary;

import com.jp.elearningjp.shared.enums.CharacterType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class KanaMatrixResponse {
    CharacterType charType; // HIRAGANA hoặc KATAKANA
    List<KanaRowGroupResponse> seion;   // Âm cơ bản (Hàng A, Ka, Sa, Ta, Na, Ha, Ma, Ya, Ra, Wa, N)
    List<KanaRowGroupResponse> dakuon;  // Âm đục & bán đục (Hàng Ga, Za, Da, Ba, Pa)
    List<KanaRowGroupResponse> yoon;    // Âm ghép (Kya, Sha, Cha, Nya, Hya, Mya, Rya, Gya, Ja, Byo, Pya...)
}
