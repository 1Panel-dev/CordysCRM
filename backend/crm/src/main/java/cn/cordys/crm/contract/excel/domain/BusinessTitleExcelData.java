package cn.cordys.crm.contract.excel.domain;

import cn.cordys.crm.contract.excel.constants.BusinessTitleImportFiled;
import cn.idev.excel.annotation.ExcelIgnore;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;


@Getter
@Setter
public class BusinessTitleExcelData {
    /**
     * 公司名称
     */
    @ExcelIgnore
    private String name;
    /**
     * 纳税人识别号
     */
    @ExcelIgnore
    private String identificationNumber;
    /**
     * 开户银行
     */
    @ExcelIgnore
    private String openingBank;
    /**
     * 银行账户
     */
    @ExcelIgnore
    private String bankAccount;
    /**
     * 注册地址
     */
    @ExcelIgnore
    private String registrationAddress;
    /**
     * 注册电话
     */
    @ExcelIgnore
    private String phoneNumber;
    /**
     * 注册资本
     */
    @ExcelIgnore
    private String registeredCapital;
    /**
     * 公司规模
     */
    @ExcelIgnore
    private String companySize;
    /**
     * 工商注册账号
     */
    @ExcelIgnore
    private String registrationNumber;


    public List<List<String>> getHead() {
        return new ArrayList<>();
    }

    public List<List<String>> getHead(Locale lang) {
        List<List<String>> heads = new ArrayList<>();
        BusinessTitleImportFiled[] fields = BusinessTitleImportFiled.values();
        for (BusinessTitleImportFiled field : fields) {
            heads.add(Collections.singletonList(field.getFiledLangMap().get(lang)));
        }
        return heads;
    }
}
