package cn.cordys.crm.opportunity.dto.response;

import cn.cordys.common.dto.OptionDTO;
import cn.cordys.crm.system.domain.Attachment;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 商机报价单响应类;
 *
 * @author guoyuqi
 */
@Data
public class OpportunityQuotationGetResponse extends OpportunityQuotationListResponse {

    @Schema(description = "选项集合")
    private Map<String, List<OptionDTO>> optionMap;

    /**
     * 附件集合
     */
    @Schema(description = "附件集合")
    private Map<String, List<Attachment>> attachmentMap;


}
