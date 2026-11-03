package com.jp.elearningjp.dto.response.notebook;

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
public class NotebookStatsResponse {

    long totalWords;
    long unlearnedCount;
    long notLearnedCount;
    long barelyCount;
    long moderateCount;
    long masteredCount;
    boolean srEnabled;
}

