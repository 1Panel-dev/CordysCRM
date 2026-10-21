package cn.cordys.crm.integration.dataease.dto.response;

import lombok.Data;

import java.util.List;

/**
 * @author jianxing
 */
@Data
public class DataEaseListResponse<T> extends DataEaseBaseResponse {
    private List<T> data;
}
