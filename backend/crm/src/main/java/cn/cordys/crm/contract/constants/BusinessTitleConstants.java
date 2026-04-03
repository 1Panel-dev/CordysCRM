package cn.cordys.crm.contract.constants;

public enum BusinessTitleConstants {

    NAME("name", "公司名称", "Name"),
    IDENTIFICATION_NUMBER("identificationNumber", "纳税人识别号", "Identification number"),
    OPENING_BANK("openingBank", "开户银行", "Opening bank"),
    BANK_ACCOUNT("bankAccount", "银行账户", "Bank account"),
    REGISTRATION_ADDRESS("registrationAddress", "注册地址", "Registration address"),
    PHONE_NUMBER("phoneNumber", "注册电话", "Phone number"),
    REGISTERED_CAPITAL("registeredCapital", "注册资本", "Registered capital"),
    COMPANY_SIZE("companySize", "公司规模", "Customer size"),
    REGISTRATION_NUMBER("registrationNumber", "工商注册账号", "Registration number"),
    PROVINCE("province", "省", "Province"),
    CITY("city", "市", "City"),
    SCALE("scale", "企业规模", "Scale"),
    INDUSTRY("industry", "国标行业", "Industry");

    private final String key;
    private final String ch;
    private final String us;

    BusinessTitleConstants(String key, String ch, String us) {
        this.key = key;
        this.ch = ch;
        this.us = us;
    }

    public String getKey() {
        return key;
    }

    public String getCh() {
        return ch;
    }

    public String getUs() {
        return us;
    }

}
