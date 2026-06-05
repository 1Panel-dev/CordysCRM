package cn.cordys.crm.form.dto.response;

import cn.cordys.common.dto.OptionDTO;
import cn.cordys.crm.form.domain.CustomForm;
import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.system.dto.form.FormProp;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class CustomFormGetResponse extends CustomForm {

    @Schema(description = "当前用户是否是管理员")
    private Boolean isAdmin;

    @Schema(description = "字段集合及其属性")
    private List<BaseField> fields;

    @Schema(description = "表单属性")
    private FormProp formProp;

    @Schema(description = "创建人")
    private OptionDTO creator;
}
