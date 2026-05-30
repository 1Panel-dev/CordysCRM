package cn.cordys.crm.system.dto.request;

import cn.cordys.common.dto.BasePageRequest;
import cn.cordys.common.groups.Created;
import cn.cordys.common.groups.Updated;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AnnouncementPageRequest extends BasePageRequest {
    @Size(min = 1, max = 32, message = "{announcement.organizationId.length_range}", groups = {Created.class, Updated.class})
    @NotBlank(message = "{announcement.organizationId.not_blank}", groups = {Created.class, Updated.class})
    private String organizationId;
}
