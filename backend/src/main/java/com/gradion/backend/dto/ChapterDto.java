package com.gradion.backend.dto;

import com.gradion.backend.model.Chapter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChapterDto {
    private Long id;
    private String name;
    private String imagePrompt;
    private String illustrationPath;

    public static ChapterDto fromEntity(Chapter entity) {
        return ChapterDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .imagePrompt(entity.getImagePrompt())
                .illustrationPath(entity.getIllustrationPath())
                .build();
    }
}
