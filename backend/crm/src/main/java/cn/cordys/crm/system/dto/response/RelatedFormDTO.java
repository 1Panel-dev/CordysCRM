package cn.cordys.crm.system.dto.response;

import cn.cordys.common.dto.OptionDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 当前表单可配置的详情标签来源，包含内置标签和动态关联表单。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RelatedFormDTO {

    @Schema(description = "关联表单 Key；独立内置标签与 internalKey 保持一致")
    private String id;

    @Schema(description = "关联表单名称")
    private String name;

    @Schema(description = "关联表单中指向当前表单的数据源单选字段；间接关联或独立标签为空数组")
    private List<OptionDTO> sourceTypeFields;

    @Schema(description = "内置标签的稳定标识；自定义关联表单为空")
    private String internalKey;
}
