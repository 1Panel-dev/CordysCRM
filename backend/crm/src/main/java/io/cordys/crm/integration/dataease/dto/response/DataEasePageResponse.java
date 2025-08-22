package io.cordys.crm.integration.dataease.dto.response;

import io.cordys.crm.integration.dataease.dto.PageDTO;
import lombok.Data;

/**
 * @Author: jianxing
 * @CreateTime: 2025-08-15  15:48
 */
@Data
public class DataEasePageResponse<T> extends DataEaseResponse<PageDTO<T>> {
}
