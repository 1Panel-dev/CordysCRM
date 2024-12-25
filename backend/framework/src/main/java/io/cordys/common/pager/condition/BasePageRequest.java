package io.cordys.common.pager.condition;

import com.google.common.base.CaseFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

/**
 * <p>表示分页请求的 DTO 类，继承自 {@link BaseCondition} 类，包含了分页参数和排序字段。</p>
 * <p>用于分页查询时传递当前页码、每页条数和排序信息。</p>
 */
@Data
public class BasePageRequest extends BaseCondition {

    /**
     * 当前页码，最小值为 1
     */
    @Min(value = 1, message = "当前页码必须大于0")
    @Schema(description = "当前页码")
    private int current;

    /**
     * 每页显示条数，范围为 5 到 500
     */
    @Min(value = 5, message = "每页显示条数必须不小于5")
    @Max(value = 500, message = "每页显示条数不能大于500")
    @Schema(description = "每页显示条数")
    private int pageSize;

    /**
     * 排序字段和排序方式（升序或降序）
     */
    @Schema(description = "排序字段（model中的字段 : asc/desc）")
    private Map<@Valid @Pattern(regexp = "^[A-Za-z]+$") String, @Valid @NotBlank String> sort;

    /**
     * 获取排序字符串，格式为 "column_name ASC/DESC"
     *
     * @return 排序字段字符串，例如 "column_name ASC"
     */
    public String getSortString() {
        return getSortString(null, null);
    }

    /**
     * 获取排序字符串，格式为 "table_alias.column_name ASC/DESC"
     *
     * @param defaultColumn  默认排序字段
     * @param tableAliasName 表的别名
     * @return 排序字段字符串，例如 "table_alias.column_name ASC"
     */
    public String getSortString(String defaultColumn, String tableAliasName) {
        if (sort == null || sort.isEmpty()) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : sort.entrySet()) {
            String column = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, entry.getKey());
            if (tableAliasName != null) {
                sb.append(tableAliasName).append(".");
            }
            sb.append(column)
                    .append(StringUtils.SPACE)
                    .append(StringUtils.equalsIgnoreCase(entry.getValue(), "DESC") ? "DESC" : "ASC")
                    .append(",");
        }

        // 去除最后一个逗号
        sb.setLength(sb.length() - 1);

        // 如果传入了 defaultColumn，添加默认排序字段
        if (defaultColumn != null) {
            sb.append(",").append(tableAliasName != null ? tableAliasName + "." : "").append(defaultColumn)
                    .append(StringUtils.SPACE).append("ASC");
        }

        return sb.toString();
    }
}
