package io.cordys.crm.system.dto.request;

import lombok.Data;

@Data
public class UserBatchEditRequest extends UserBatchRequest{

    private String departmentId;

    private String supervisorId;

    private String workCity;
}
