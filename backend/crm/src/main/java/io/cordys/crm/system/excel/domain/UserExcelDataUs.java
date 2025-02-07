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
public class UserExcelDataUs extends UserExcelData {

    @ColumnWidth(50)
    @ExcelProperty("Employee id")
    @NotRequired
    private String employeeId;

    @NotBlank(message = "{cannot_be_null}")
    @Length(max = 255)
    @ExcelProperty("Name")
    private String name;

    @ColumnWidth(50)
    @ExcelProperty("Gender")
    private String gender;

    @NotBlank(message = "{cannot_be_null}")
    @Length(max = 100)
    @ExcelProperty("Department")
    @ColumnWidth(30)
    private String department;


    @ColumnWidth(50)
    @ExcelProperty("Position")
    private String position;

    @ColumnWidth(50)
    @ExcelProperty("Phone")
    private String phone;

    @ColumnWidth(50)
    @ExcelProperty("Email")
    private String email;

    @ColumnWidth(50)
    @ExcelProperty("Supervisor")
    private String supervisor;

    @ColumnWidth(50)
    @ExcelProperty("Work city")
    private String workCity;

    @ColumnWidth(50)
    @ExcelProperty("Employee type")
    private String employeeType;

    @Override
    public List<List<String>> getHead() {
        return super.getHead(Locale.US);
    }
}
