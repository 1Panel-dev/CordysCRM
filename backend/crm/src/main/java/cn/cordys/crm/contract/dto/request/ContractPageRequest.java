package cn.cordys.crm.contract.dto.request;

import cn.cordys.common.dto.BasePageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;


@Data
public class ContractPageRequest extends BasePageRequest {

    @Schema(description = "看板模式")
    private Boolean board = false;
    public String getCustomerId() {
        return null;
    }
}
