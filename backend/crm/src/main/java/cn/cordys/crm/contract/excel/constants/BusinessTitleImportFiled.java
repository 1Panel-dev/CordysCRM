package cn.cordys.crm.contract.excel.constants;

import cn.cordys.crm.contract.excel.domain.BusinessTitleExcelData;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import lombok.Getter;

public enum BusinessTitleImportFiled {
  NAME("name", "公司名称", "Name", BusinessTitleExcelData::getName),
  IDENTIFICATION_NUMBER(
      "identificationNumber",
      "纳税人识别号",
      "Identification number",
      BusinessTitleExcelData::getIdentificationNumber),
  OPENING_BANK("openingBank", "开户银行", "Opening bank", BusinessTitleExcelData::getOpeningBank),
  BANK_ACCOUNT("bankAccount", "银行账户", "Bank account", BusinessTitleExcelData::getBankAccount),
  REGISTRATION_ADDRESS(
      "registrationAddress",
      "注册地址",
      "Registration address",
      BusinessTitleExcelData::getRegistrationAddress),
  PHONE_NUMBER("phoneNumber", "注册电话", "Phone number", BusinessTitleExcelData::getPhoneNumber),
  REGISTERED_CAPITAL(
      "registeredCapital",
      "注册资本",
      "Registered capital",
      BusinessTitleExcelData::getRegisteredCapital),
  COMPANY_SIZE("companySize", "公司规模", "Customer size", BusinessTitleExcelData::getCompanySize),
  registration_number(
      "registrationNumber",
      "工商注册账号",
      "Registration number",
      BusinessTitleExcelData::getRegistrationNumber);

  @Getter private final Map<Locale, String> filedLangMap;
  private final Function<BusinessTitleExcelData, String> parseFunc;
  @Getter private final String value;

  BusinessTitleImportFiled(
      String value, String zn, String us, Function<BusinessTitleExcelData, String> parseFunc) {
    this.filedLangMap = new HashMap<>();
    filedLangMap.put(Locale.SIMPLIFIED_CHINESE, zn);
    filedLangMap.put(Locale.US, us);
    this.value = value;
    this.parseFunc = parseFunc;
  }

  public String parseExcelDataValue(BusinessTitleExcelData excelData) {
    return parseFunc.apply(excelData);
  }

  public boolean containsHead(String head) {
    return filedLangMap.containsValue(head);
  }

  public static BusinessTitleImportFiled fromHeader(String header) {
    if (header == null) {
      return null;
    }

    for (BusinessTitleImportFiled field : values()) {
      if (field.filedLangMap.containsValue(header)) {
        return field;
      }
    }
    return null;
  }
}
