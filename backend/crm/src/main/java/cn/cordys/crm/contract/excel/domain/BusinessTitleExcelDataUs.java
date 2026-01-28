package cn.cordys.crm.contract.excel.domain;

import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.write.style.ColumnWidth;
import java.util.List;
import java.util.Locale;
import lombok.Data;

@Data
@ColumnWidth(15)
public class BusinessTitleExcelDataUs extends BusinessTitleExcelData {

  /** 公司名称 */
  @ColumnWidth(255)
  @ExcelProperty("Name")
  private String name;

  /** 纳税人识别号 */
  @ColumnWidth(255)
  @ExcelProperty("Identification number")
  private String identificationNumber;

  /** 开户银行 */
  @ColumnWidth(255)
  @ExcelProperty("Opening bank")
  private String openingBank;

  /** 银行账户 */
  @ColumnWidth(255)
  @ExcelProperty("Bank account")
  private String bankAccount;

  /** 注册地址 */
  @ColumnWidth(255)
  @ExcelProperty("Registration address")
  private String registrationAddress;

  /** 注册电话 */
  @ColumnWidth(255)
  @ExcelProperty("Phone number")
  private String phoneNumber;

  /** 注册资本 */
  @ColumnWidth(255)
  @ExcelProperty("Registered capital")
  private String registeredCapital;

  /** 公司规模 */
  @ColumnWidth(255)
  @ExcelProperty("Customer size")
  private String companySize;

  /** 工商注册号 */
  @ColumnWidth(255)
  @ExcelProperty("Registration number")
  private String registrationNumber;

  @Override
  public List<List<String>> getHead() {
    return super.getHead(Locale.US);
  }
}
