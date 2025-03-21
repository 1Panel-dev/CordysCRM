package io.cordys.crm.follow.dto.response;

import io.cordys.common.dto.OptionDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class FollowUpRecordDetailResponse extends FollowUpRecordListResponse {

    @Schema(description = "选项集合")
    private Map<String, List<OptionDTO>> optionMap;
}
