package io.cordys.crm.integration.dataease.dto.response;

import lombok.Data;

/**
 * @Author: jianxing
 * @CreateTime: 2025-08-15  15:48
 */
@Data
public class DataEaseResponse<T> {
    private int code;

    private String msg;

    private T data;
}
