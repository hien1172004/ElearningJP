package com.jp.elearningjp.mapper.notebook;

import com.jp.elearningjp.dto.request.notebook.CreateNotebookGroupRequest;
import com.jp.elearningjp.dto.request.notebook.CreateNotebookRequest;
import com.jp.elearningjp.dto.request.notebook.UpdateNotebookGroupRequest;
import com.jp.elearningjp.dto.request.notebook.UpdateNotebookRequest;
import com.jp.elearningjp.dto.response.notebook.NotebookGroupResponse;
import com.jp.elearningjp.dto.response.notebook.NotebookResponse;
import com.jp.elearningjp.entity.notebook.Notebook;
import com.jp.elearningjp.entity.notebook.NotebookGroup;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface NotebookMapper {

    @Mapping(target = "user", ignore = true)
    @Mapping(target = "notebooks", ignore = true)
    NotebookGroup toNotebookGroup(CreateNotebookGroupRequest request);

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "notebookCount", expression = "java(group.getNotebooks() != null ? group.getNotebooks().size() : 0)")
    NotebookGroupResponse toNotebookGroupResponse(NotebookGroup group);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "notebooks", ignore = true)
    void updateNotebookGroupFromRequest(UpdateNotebookGroupRequest request, @MappingTarget NotebookGroup group);

    @Mapping(target = "user", ignore = true)
    @Mapping(target = "group", ignore = true)
    @Mapping(target = "sourceNotebook", ignore = true)
    @Mapping(target = "shareCode", ignore = true)
    @Mapping(target = "sharingEnabled", ignore = true)
    @Mapping(target = "wordCount", ignore = true)
    @Mapping(target = "likeCount", ignore = true)
    @Mapping(target = "viewCount", ignore = true)
    @Mapping(target = "downloadCount", ignore = true)
    @Mapping(target = "srEnabled", ignore = true)
    Notebook toNotebook(CreateNotebookRequest request);

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "authorName", source = "user.fullName")
    @Mapping(target = "authorAvatar", source = "user.avatarUrl")
    @Mapping(target = "groupId", source = "group.id")
    @Mapping(target = "groupName", source = "group.name")
    @Mapping(target = "isLiked", ignore = true)
    @Mapping(target = "isOwner", ignore = true)
    @Mapping(target = "isImported", ignore = true)
    @Mapping(target = "userNotebookId", ignore = true)
    NotebookResponse toNotebookResponse(Notebook notebook);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "group", ignore = true)
    @Mapping(target = "sourceNotebook", ignore = true)
    @Mapping(target = "shareCode", ignore = true)
    @Mapping(target = "sharingEnabled", ignore = true)
    @Mapping(target = "wordCount", ignore = true)
    @Mapping(target = "likeCount", ignore = true)
    @Mapping(target = "viewCount", ignore = true)
    @Mapping(target = "downloadCount", ignore = true)
    void updateNotebookFromRequest(UpdateNotebookRequest request, @MappingTarget Notebook notebook);

    @Mapping(target = "notebook", ignore = true)
    @Mapping(target = "vocabulary", ignore = true)
    @Mapping(target = "displayOrder", ignore = true)
    @Mapping(target = "memoryLevel", ignore = true)
    @Mapping(target = "intervalDays", ignore = true)
    @Mapping(target = "repetitionCount", ignore = true)
    @Mapping(target = "lapseCount", ignore = true)
    @Mapping(target = "easeFactor", ignore = true)
    @Mapping(target = "nextReviewAt", ignore = true)
    @Mapping(target = "lastReviewedAt", ignore = true)
    @Mapping(target = "audioUrl", ignore = true)
    com.jp.elearningjp.entity.notebook.NotebookWord toNotebookWord(com.jp.elearningjp.dto.request.notebook.CreateNotebookWordRequest request);

    @Mapping(target = "notebookId", source = "notebook.id")
    @Mapping(target = "vocabularyId", source = "vocabulary.id")
    @Mapping(target = "memoryLevelTitle", expression = "java(word.getMemoryLevel() != null ? word.getMemoryLevel().getTitle() : null)")
    @Mapping(target = "memoryLevelColor", expression = "java(word.getMemoryLevel() != null ? word.getMemoryLevel().getColorHex() : null)")
    com.jp.elearningjp.dto.response.notebook.NotebookWordResponse toNotebookWordResponse(com.jp.elearningjp.entity.notebook.NotebookWord word);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "notebook", ignore = true)
    @Mapping(target = "vocabulary", ignore = true)
    @Mapping(target = "memoryLevel", ignore = true)
    @Mapping(target = "intervalDays", ignore = true)
    @Mapping(target = "repetitionCount", ignore = true)
    @Mapping(target = "lapseCount", ignore = true)
    @Mapping(target = "easeFactor", ignore = true)
    @Mapping(target = "nextReviewAt", ignore = true)
    @Mapping(target = "lastReviewedAt", ignore = true)
    @Mapping(target = "audioUrl", ignore = true)
    void updateNotebookWordFromRequest(com.jp.elearningjp.dto.request.notebook.UpdateNotebookWordRequest request, @MappingTarget com.jp.elearningjp.entity.notebook.NotebookWord word);
}

