package cn.cordys.crm.integration.dataease.dto.request;

import cn.cordys.crm.integration.dataease.dto.SysVariableDTO;
import lombok.Data;

import java.util.List;

/**
 * @Author: jianxing
 * @CreateTime: 2025-08-15  14:52
 */
@Data
public class UserCreateRequest {

    private String account;

    private String name;

    private String email;

    private boolean enable = true;

    private String phone;

    private String phonePrefix;

    private List<String> roleIds;

    private List<Variable> variables;

    @Data
    public static class Variable {
        private String variableId;

        private String variableValueId;

        private SysVariableDTO sysVariableDto;

        private List<String> variableValueIds;
    }
}
