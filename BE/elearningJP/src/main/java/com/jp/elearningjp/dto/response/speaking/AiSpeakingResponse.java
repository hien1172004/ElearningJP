package com.jp.elearningjp.dto.response.speaking;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Định dạng JSON chuẩn LLM trả về khi đối đáp luyện nói.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiSpeakingResponse {

    @JsonProperty("reply_ja")
    private String replyJa;

    @JsonProperty("reply_vi")
    private String replyVi;

    private GrammarCorrectionDto correction;

    @JsonProperty("completed_task_ids")
    @Builder.Default
    private List<Integer> completedTaskIds = new ArrayList<>();

    @Builder.Default
    private Integer tokensUsed = 0;
}
