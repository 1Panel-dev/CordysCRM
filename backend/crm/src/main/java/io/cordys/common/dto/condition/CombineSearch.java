package io.cordys.common.dto.condition;

import io.cordys.common.constants.EnumValue;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.Data;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * 表示组合搜索条件，用于支持复杂的搜索逻辑。
 * 包含匹配模式（所有/任一）和筛选条件列表。
 */
@Data
public class CombineSearch {

    @Schema(description = "匹配模式，支持“所有”或“任一”", allowableValues = {"AND", "OR"})
    @EnumValue(enumClass = SearchMode.class)
    private String searchMode = SearchMode.AND.name();

    @Schema(description = "筛选条件列表，用于定义多个搜索条件")
    @Valid
    private List<FilterCondition> conditions;

    public List<FilterCondition> getConditions() {
        if (CollectionUtils.isEmpty(conditions)) {
            return List.of();
        }
        return conditions.stream()
                .filter(FilterCondition::valid)
                .toList();
    }

    /**
     * 获取当前的匹配模式。如果未设置，则默认返回 "AND"。
     *
     * @return 当前的匹配模式
     */
    public String getSearchMode() {
        return StringUtils.isBlank(searchMode) ? SearchMode.AND.name() : searchMode;
    }

    /**
     * 枚举：搜索模式，定义了“所有”与“任一”两种匹配模式。
     */
    public enum SearchMode {
        /**
         * 所有条件都匹配（“与”操作）
         */
        AND,

        /**
         * 任一条件匹配（“或”操作）
         */
        OR
    }
}
