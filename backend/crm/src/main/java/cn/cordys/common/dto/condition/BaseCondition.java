package cn.cordys.common.dto.condition;

import cn.cordys.common.utils.ConditionFilterUtils;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.Data;
import org.apache.commons.lang3.Strings;

import java.util.List;

/**
 * 表示 CRM 系统中的基础条件类，用于支持过滤和搜索操作。
 */
@Data
public class BaseCondition {

    @Schema(description = "视图ID")
    private String viewId;

    @Schema(description = "关键字，用于搜索匹配")
    private String keyword;

    @Schema(description = "筛选条件列表，用于定义多个搜索条件")
    @Valid
    private List<FilterCondition> filters;

    @Schema(description = "高级搜索条件，支持组合搜索")
    @Valid
    private CombineSearch combineSearch;

    private CombineSearch viewCombineSearch;

    /**
     * 转义关键字中的特殊字符。
     *
     * @param keyword 输入的关键字
     *
     * @return 转义后的关键字
     */
    public static String transferKeyword(String keyword) {
        if (Strings.CS.contains(keyword, "\\") && !Strings.CS.contains(keyword, "\\\\")) {
            keyword = Strings.CS.replace(keyword, "\\", "\\\\");
        }
        // 判断是否已经转义过，未转义才进行转义。
        if (Strings.CS.contains(keyword, "%") && !Strings.CS.contains(keyword, "\\%")) {
            keyword = Strings.CS.replace(keyword, "%", "\\%");
        }
        if (Strings.CS.contains(keyword, "_") && !Strings.CS.contains(keyword, "\\_")) {
            keyword = Strings.CS.replace(keyword, "_", "\\_");
        }
        return keyword;
    }

    public CombineSearch getCombineSearch() {
        return combineSearch == null ? new CombineSearch() : combineSearch;
    }

    public List<FilterCondition> getFilters() {
        return ConditionFilterUtils.getValidConditions(filters);
    }

    /**
     * 初始化关键字，直接设置字段值。
     *
     * @param keyword 初始化的关键字
     */
    public void initKeyword(String keyword) {
        this.keyword = keyword;
    }
}
