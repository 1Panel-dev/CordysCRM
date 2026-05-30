package cn.cordys.crm.customer.dto.request;

import lombok.Builder;
import lombok.Data;

/**
 * @author song-cc-rock
 */
@Data
@Builder
public class ContactUniqueRequest {

    /**
     * 联系人名称
     */
    private String name;
    /**
     * 联系人电话
     */
    private String phone;
    /**
     * 是否名称唯一
     */
    private Boolean nameUnique;
    /**
     * 是否电话唯一
     */
    private Boolean phoneUnique;
}
