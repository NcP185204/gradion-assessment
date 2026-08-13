package com.gradion.backend.dto;

import com.gradion.backend.model.Character;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CharacterDto {
    private Long id;
    private String name;
    private String imagePrompt;
    private String portraitPath;
    private int displayOrder;

    public static CharacterDto fromEntity(Character entity) {
        return CharacterDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .imagePrompt(entity.getImagePrompt())
                .portraitPath(entity.getPortraitPath())
                .displayOrder(entity.getDisplayOrder())
                .build();
    }
}
