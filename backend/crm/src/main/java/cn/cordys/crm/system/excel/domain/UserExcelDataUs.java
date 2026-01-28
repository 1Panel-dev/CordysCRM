package cn.cordys.crm.system.excel.domain;

import cn.cordys.crm.system.excel.annotation.NotRequired;
import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.write.style.ColumnWidth;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Locale;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

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

  @ExcelProperty("Department")
  @ColumnWidth(30)
  private String department;

  @ColumnWidth(50)
  @ExcelProperty("Position")
  private String position;

  @NotBlank(message = "{cannot_be_null}")
  @ColumnWidth(50)
  @Length(max = 255)
  @ExcelProperty("Phone")
  private String phone;

  @NotBlank(message = "{cannot_be_null}")
  @ColumnWidth(50)
  @ExcelProperty("Email")
  private String email;

  @ColumnWidth(50)
  @ExcelProperty("Supervisor")
  private String supervisor;

  @ColumnWidth(50)
  @ExcelProperty("Employee type")
  private String employeeType;

  @Override
  public List<List<String>> getHead() {
    return super.getHead(Locale.US);
  }
}
