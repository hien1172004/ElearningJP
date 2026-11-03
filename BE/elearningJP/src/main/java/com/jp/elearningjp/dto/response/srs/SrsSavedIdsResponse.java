package com.jp.elearningjp.dto.response.srs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SrsSavedIdsResponse {
    private Set<Long> vocabIds;
    private Set<Long> characterIds;
    private Set<Long> grammarIds;
}