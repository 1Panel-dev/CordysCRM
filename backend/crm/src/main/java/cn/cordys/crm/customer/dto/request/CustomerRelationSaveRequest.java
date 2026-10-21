package cn.cordys.crm.customer.dto.request;

import cn.cordys.common.constants.EnumValue;
import cn.cordys.crm.customer.constants.CustomerRelationType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.groups.Default;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * @author jianxing
 * @date 2025-02-08 16:24:22
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerRelationSaveRequest {

    /**
     * 单条添加/修改（add、update）时使用的校验分组。
     * 整体保存（save）允许传入空行并由服务层忽略，因此这些必填约束不能放在 Default 分组；
     * 继承 Default 是为了单条校验时同时执行 @Size、@EnumValue 以及更新请求中 id 的必填校验。
     */
    public interface SingleOperation extends Default {
    }

    @NotBlank(groups = SingleOperation.class)
    @Size(max = 32)
    @Schema(description = "关联客户ID")
    private String customerId;

    @NotBlank(groups = SingleOperation.class)
    @Schema(description = "关系类型")
    @EnumValue(enumClass = CustomerRelationType.class)
    private String relationType;
}