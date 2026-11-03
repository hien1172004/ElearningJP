package com.jp.elearningjp.dto.request.notebook;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateNotebookWordRequest {

    @Size(max = 100, message = "Từ vựng không được vượt quá 100 ký tự")
    String word;

    @Size(max = 100, message = "Phiên âm không được vượt quá 100 ký tự")
    String reading;

    String meaning;

    @Size(max = 50, message = "Từ loại không được vượt quá 50 ký tự")
    String partOfSpeech;

    String userNote;

    @Size(max = 100, message = "Âm Hán Việt không được vượt quá 100 ký tự")
    String sinoVietnamese;

    String exampleSentence;

    String exampleMeaning;

    @Pattern(regexp = "^(N[1-5])?$", message = "Cấp độ JLPT phải là N1, N2, N3, N4 hoặc N5")
    String jlptLevel;

    Integer displayOrder;
}

