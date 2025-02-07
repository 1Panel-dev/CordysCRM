package io.cordys.crm.system.excel.constants;


import io.cordys.crm.system.excel.domain.UserExcelData;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

/**
 * @author wx
 */

public enum UserImportFiled {

    EMPLOYEE_ID("employeeId", "工号", "EmployeeId", UserExcelData::getEmployeeId),
    NAME("name", "姓名", "Name", UserExcelData::getName),
    GENDER("gender", "性别", "Gender", UserExcelData::getGender),
    DEPARTMENT("department", "部门", "Department", UserExcelData::getDepartment),
    POSITION("position", "职位", "Position", UserExcelData::getPosition),
    PHONE("phone", "手机号", "Phone", UserExcelData::getPhone),
    EMAIL("email", "邮箱", "Email", UserExcelData::getEmail),
    SUPERVISOR("supervisor", "直属上级", "Supervisor", UserExcelData::getSupervisor),
    WORK_CITY("workCity", "工作城市", "Work City", UserExcelData::getWorkCity),
    EMPLOYEE_TYPE("employeeType", "员工类型", "Employee type", UserExcelData::getEmployeeType);

    private Map<Locale, String> filedLangMap;
    private Function<UserExcelData, String> parseFunc;
    private String value;

    UserImportFiled(String value, String zn, String us, Function<UserExcelData, String> parseFunc) {
        this.filedLangMap = new HashMap<Locale, String>();
        filedLangMap.put(Locale.SIMPLIFIED_CHINESE, zn);
        filedLangMap.put(Locale.US, us);
        this.value = value;
        this.parseFunc = parseFunc;
    }

    public Map<Locale, String> getFiledLangMap() {
        return this.filedLangMap;
    }

    public String getValue() {
        return value;
    }

    public String parseExcelDataValue(UserExcelData excelData) {
        return parseFunc.apply(excelData);
    }

    public boolean containsHead(String head) {
        return filedLangMap.values().contains(head);
    }
}
