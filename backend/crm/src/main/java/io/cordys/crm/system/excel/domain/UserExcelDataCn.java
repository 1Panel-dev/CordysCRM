package io.cordys.crm.system.excel.domain;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import io.cordys.crm.system.excel.annotation.NotRequired;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import java.util.List;
import java.util.Locale;

/**
 * @author wx
 */
@Data
@ColumnWidth(15)
public class UserExcelDataCn extends UserExcelData {

    @ColumnWidth(50)
    @ExcelProperty("工号")
    @NotRequired
    private String employeeId;

    @NotBlank(message = "{cannot_be_null}")
    @Length(max = 255)
    @ExcelProperty("姓名")
    private String name;

    @ColumnWidth(50)
    @ExcelProperty("性别")
    private String gender;

    @ExcelProperty("部门")
    @ColumnWidth(50)
    private String department;


    @ColumnWidth(50)
    @ExcelProperty("职位")
    private String position;

    @NotBlank(message = "{cannot_be_null}")
    @ColumnWidth(50)
    @Length(max = 255)
    @ExcelProperty("手机号")
    private String phone;

    @NotBlank(message = "{cannot_be_null}")
    @ColumnWidth(50)
    @ExcelProperty("邮箱")
    private String email;

    @ColumnWidth(50)
    @ExcelProperty("直属上级")
    private String supervisor;

    @ColumnWidth(50)
    @ExcelProperty("工作城市")
    private String workCity;

    @ColumnWidth(50)
    @ExcelProperty("员工类型")
    private String employeeType;


    @Override
    public List<List<String>> getHead() {
        return super.getHead(Locale.SIMPLIFIED_CHINESE);
    }
}
