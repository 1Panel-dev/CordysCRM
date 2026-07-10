package cn.cordys.crm.form.dto.request;

import cn.cordys.crm.system.dto.request.ImportRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class CustomerFormImportRequest extends ImportRequest {

    @Schema(description = "自定义表单id")
    private String customFormId;

}
