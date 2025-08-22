package io.cordys.crm.integration.dataease.dto;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * @Author: jianxing
 * @CreateTime: 2025-08-22  11:42
 */
@Data
public class DeUserCreateExcelDTO {
    @ExcelProperty("账号（必填，文本）")
    private String account;
    @ExcelProperty("账号（必填，文本）")
    private String name;
    @ExcelProperty("邮箱（必填，文本）")
    private String email;
    @ExcelProperty("用户状态（必填，文本）")
    private Boolean enable = true;
    @ExcelProperty("角色（必填，文本）")
    private String roleNames;
    @ExcelProperty("系统变量（非必填，文本）")
    private String sysVariable;
}
