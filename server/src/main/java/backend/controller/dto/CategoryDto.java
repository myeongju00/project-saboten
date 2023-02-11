package backend.controller.dto;

import backend.model.category.CategoryEntity;
import common.model.reseponse.category.CategoryResponse;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class CategoryDto {
    private Long categoryId;

    private String categoryName;

    private String categoryIconUrl;

    public CategoryResponse toCategoryResponse() {
        return new CategoryResponse(this.categoryId, this.categoryName, this.categoryIconUrl);
    }

    public CategoryEntity toEntity() {
        return CategoryEntity.builder()
                .categoryId(categoryId)
                .categoryName(categoryName)
                .categoryIconUrl(categoryIconUrl)
                .build();
    }
}