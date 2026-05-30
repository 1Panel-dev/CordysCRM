package cn.cordys.crm.integration.sync.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ThirdOrgDataDTO {
    private List<ThirdDepartment> departments;
    private Map<String, List<ThirdUser>> users;
}
