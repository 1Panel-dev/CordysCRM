package cn.cordys.crm.contract.excel.domain;

import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.write.style.ColumnWidth;
import java.util.List;
import java.util.Locale;
import lombok.Data;

@Data
@ColumnWidth(15)
public class BusinessTitleExcelDataCn extends BusinessTitleExcelData {

  /** 公司名称 */
  @ColumnWidth(255)
  @ExcelProperty("公司名称")
  private String name;

  /** 纳税人识别号 */
  @ColumnWidth(255)
  @ExcelProperty("纳税人识别号")
  private String identificationNumber;

  /** 开户银行 */
  @ColumnWidth(255)
  @ExcelProperty("开户银行")
  private String openingBank;

  /** 银行账户 */
  @ColumnWidth(255)
  @ExcelProperty("银行账户")
  private String bankAccount;

  /** 注册地址 */
  @ColumnWidth(255)
  @ExcelProperty("注册地址")
  private String registrationAddress;

  /** 注册电话 */
  @ColumnWidth(255)
  @ExcelProperty("注册电话")
  private String phoneNumber;

  /** 注册资本 */
  @ColumnWidth(255)
  @ExcelProperty("注册资本")
  private String registeredCapital;

  /** 公司规模 */
  @ColumnWidth(255)
  @ExcelProperty("公司规模")
  private String companySize;

  /** 工商注册账号 */
  @ColumnWidth(255)
  @ExcelProperty("工商注册账号")
  private String registrationNumber;

  @Override
  public List<List<String>> getHead() {
    return super.getHead(Locale.SIMPLIFIED_CHINESE);
  }
}
