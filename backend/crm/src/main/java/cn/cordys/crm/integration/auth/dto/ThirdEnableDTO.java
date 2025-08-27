package cn.cordys.crm.integration.auth.dto;

import lombok.Data;

@Data
public class ThirdEnableDTO {
    private boolean syncEnable;
    private boolean codeEnable;
    private boolean noticeEnable;
    private boolean boardEnable;
    private boolean chatEnable;
}
