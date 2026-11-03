package com.jp.elearningjp.dto.response.notebook;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NotebookResponse {

    Long id;
    Long userId;
    String authorName;
    String authorAvatar;
    Long groupId;
    String groupName;
    String name;
    String description;
    String coverColor;
    String coverIcon;
    Boolean isPublic;
    Boolean sharingEnabled;
    String shareCode;
    String jlptLevel;
    Integer wordCount;
    Integer likeCount;
    Integer viewCount;
    Integer downloadCount;
    Boolean srEnabled;
    Boolean isLiked;
    Boolean isOwner;
    Boolean isImported;
    Long userNotebookId;
    Instant createdAt;
    Instant updatedAt;
}

