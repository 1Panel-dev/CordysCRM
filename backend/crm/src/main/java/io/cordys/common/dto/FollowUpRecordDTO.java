package io.cordys.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
    public class FollowUpRecordDTO {

        @Schema(description = "资源id")
        private String sourceId;

        @Schema(description = "最新跟进人")
        private String follower;

        @Schema(description = "最新跟进人名称")
        private String followerName;

        @Schema(description = "最新跟进日期")
        private Long followTime;
    }
