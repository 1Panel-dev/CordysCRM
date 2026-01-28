package cn.cordys.crm.system.excel.constants;

import cn.cordys.crm.system.excel.domain.UserExcelData;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import lombok.Getter;

/**
 * @author wx
 */
public enum UserImportFiled {
  EMPLOYEE_ID("employeeId", "工号", "Employee id", UserExcelData::getEmployeeId),
  NAME("name", "姓名", "Name", UserExcelData::getName),
  GENDER("gender", "性别", "Gender", UserExcelData::getGender),
  DEPARTMENT("department", "部门", "Department", UserExcelData::getDepartment),
  POSITION("position", "职位", "Position", UserExcelData::getPosition),
  PHONE("phone", "手机号", "Phone", UserExcelData::getPhone),
  EMAIL("email", "邮箱", "Email", UserExcelData::getEmail),
  SUPERVISOR("supervisor", "直属上级", "Supervisor", UserExcelData::getSupervisor),
  WORK_CITY("workCity", "工作城市", "Work City", UserExcelData::getWorkCity),
  EMPLOYEE_TYPE("employeeType", "员工类型", "Employee type", UserExcelData::getEmployeeType);

  @Getter private final Map<Locale, String> filedLangMap;
  private final Function<UserExcelData, String> parseFunc;
  @Getter private final String value;

  UserImportFiled(String value, String zn, String us, Function<UserExcelData, String> parseFunc) {
    this.filedLangMap = new HashMap<>();
    filedLangMap.put(Locale.SIMPLIFIED_CHINESE, zn);
    filedLangMap.put(Locale.US, us);
    this.value = value;
    this.parseFunc = parseFunc;
  }

  public String parseExcelDataValue(UserExcelData excelData) {
    return parseFunc.apply(excelData);
  }

  public boolean containsHead(String head) {
    return filedLangMap.containsValue(head);
  }
}
