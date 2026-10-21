package cn.cordys.crm.integration.dataease.dto.response;

import lombok.Data;

/**
 * @author jianxing
 */
@Data
public class DataEaseResponse<T> extends DataEaseBaseResponse {
    private T data;
}
