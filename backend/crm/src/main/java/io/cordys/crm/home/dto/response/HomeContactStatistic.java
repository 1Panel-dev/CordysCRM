package io.cordys.crm.home.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @Author: jianxing
 * @CreateTime: 2025-07-18  16:07
 */
@Data
public class HomeContactStatistic {

    @Schema(description = "新增联系人")
    private HomeStatisticSearchResponse newContact;
}
