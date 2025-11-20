package cn.cordys.crm.product.dto.response;

import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.dto.OptionDTO;
import cn.cordys.crm.system.domain.Attachment;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * @author song-cc-rock
 */
@Data
public class ProductPriceGetResponse extends ProductPriceResponse{

    @Schema(description = "选项集合")
    private Map<String, List<OptionDTO>> optionMap;

    @Schema(description = "附件集合")
    private Map<String, List<Attachment>> attachmentMap;
}
