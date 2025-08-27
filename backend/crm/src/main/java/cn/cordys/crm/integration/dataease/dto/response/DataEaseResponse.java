package cn.cordys.crm.integration.dataease.dto.response;

import lombok.Data;

/**
 * @Author: jianxing
 * @CreateTime: 2025-08-15  15:48
 */
@Data
public class DataEaseResponse<T> extends DataEaseBaseResponse {
    private T data;
}
