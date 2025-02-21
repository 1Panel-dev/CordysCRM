package io.cordys.common.dto;

import io.cordys.common.dto.condition.BaseCondition;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

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

    @Valid
    @Schema(description = "排序字段")
    private SortRequest sort;
}
