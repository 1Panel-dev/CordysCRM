package io.cordys.crm.system.excel.domain;

import com.alibaba.excel.annotation.ExcelIgnore;
import io.cordys.crm.system.excel.constants.UserImportFiled;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * @author wx
 */
@Getter
@Setter
public class UserExcelData {
    /**
     * 工号
     */
    @ExcelIgnore
    private String employeeId;
    /**
     * 姓名
     */
    @ExcelIgnore
    private String name;
    /**
     * 性别
     */
    @ExcelIgnore
    private String gender;
    /**
     * 部门
     */
    @ExcelIgnore
    private String department;
    /**
     * 职位
     */
    @ExcelIgnore
    private String position;
    /**
     * 手机号
     */
    @ExcelIgnore
    private String phone;
    /**
     * 邮箱
     */
    @ExcelIgnore
    private String email;
    /**
     * 直属上级
     */
    @ExcelIgnore
    private String supervisor;
    /**
     * 工作城市
     */
    @ExcelIgnore
    private String workCity;
    /**
     * 员工类型
     */
    @ExcelIgnore
    private String employeeType;


    public List<List<String>> getHead() {
        return new ArrayList<>();
    }

    public List<List<String>> getHead(Locale lang) {
        List<List<String>> heads = new ArrayList<>();
        UserImportFiled[] fields = UserImportFiled.values();
        for (UserImportFiled field : fields) {
            heads.add(Arrays.asList(field.getFiledLangMap().get(lang)));
        }
        return heads;
    }
}
