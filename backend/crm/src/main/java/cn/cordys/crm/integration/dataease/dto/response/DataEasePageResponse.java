package cn.cordys.crm.integration.dataease.dto.response;

import cn.cordys.crm.integration.dataease.dto.PageDTO;
import lombok.Data;

/**
 * @author jianxing
 */
@Data
public class DataEasePageResponse<T> extends DataEaseResponse<PageDTO<T>> {
}
