package io.cordys.crm.integration.dataease.dto.response;

import lombok.Data;

import java.util.List;

/**
 * @Author: jianxing
 * @CreateTime: 2025-08-15  15:48
 */
@Data
public class DataEaseListResponse<T> {
    private int code;

    private String msg;

    private List<T> data;
}
