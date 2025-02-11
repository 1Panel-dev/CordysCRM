package io.cordys.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class NodeSortCountResultDTO {
    private boolean isRefreshNum;
    private long num;
}
