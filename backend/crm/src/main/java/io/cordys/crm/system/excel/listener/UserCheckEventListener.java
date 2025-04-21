package io.cordys.crm.system.excel.listener;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import io.cordys.common.dto.BaseTreeNode;
import io.cordys.common.exception.GenericException;
import io.cordys.common.util.CommonBeanFactory;
import io.cordys.common.util.LogUtils;
import io.cordys.common.util.Translator;
import io.cordys.crm.system.excel.annotation.NotRequired;
import io.cordys.crm.system.excel.domain.UserExcelData;
import io.cordys.crm.system.excel.domain.UserExcelDataFactory;
import io.cordys.crm.system.service.DepartmentService;
import io.cordys.crm.system.service.OrganizationUserService;
import io.cordys.excel.domain.ExcelErrData;
import io.cordys.excel.utils.ExcelValidateHelper;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Field;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UserCheckEventListener extends AnalysisEventListener<Map<Integer, String>> {

    private final Class excelDataClass;
    private Map<Integer, String> headMap;
    private final Map<String, String> excelHeadToFieldNameDic = new HashMap<>();
    @Getter
    protected List<UserExcelData> list = new ArrayList<>();
    @Getter
    protected List<ExcelErrData<UserExcelData>> errList = new ArrayList<>();
    private static final String ERROR_MSG_SEPARATOR = ";";
    protected static final int NAME_LENGTH = 255;
    protected static final int PHONE_LENGTH = 20;
    private static final String EMAIL_REGEX = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
    private final List<BaseTreeNode> departmentTree;
    private final OrganizationUserService organizationUserService;

    public UserCheckEventListener(Class clazz, String orgId) {
        excelDataClass = clazz;
        DepartmentService departmentService = CommonBeanFactory.getBean(DepartmentService.class);
        organizationUserService = CommonBeanFactory.getBean(OrganizationUserService.class);
        assert departmentService != null;
        departmentTree = departmentService.getTree(orgId);
    }

    @Override
    public void invokeHeadMap(Map<Integer, String> headMap, AnalysisContext context) {
        this.headMap = headMap;
        try {
            genExcelHeadToFieldNameDicAndGetNotRequiredFields();
        } catch (NoSuchFieldException e) {
            LogUtils.error(e);
        }
        formatHeadMap();
        super.invokeHeadMap(headMap, context);
    }


    @Override
    public void invoke(Map<Integer, String> data, AnalysisContext analysisContext) {
        if (headMap == null) {
            throw new GenericException(Translator.get("user_import_table_header_missing"));
        }
        Integer rowIndex = analysisContext.readRowHolder().getRowIndex();
        if (rowIndex >= 3) {
            UserExcelData userExcelData = parseDataToModel(data);
            //校验数据
            buildUpdateOrErrorList(rowIndex, userExcelData);
        }

    }


    @Override
    public void doAfterAllAnalysed(AnalysisContext analysisContext) {
    }


    /**
     * 构建数据
     *
     * @param rowIndex
     * @param userExcelData
     */
    private void buildUpdateOrErrorList(Integer rowIndex, UserExcelData userExcelData) {
        StringBuilder errMsg;
        try {
            //根据excel数据实体中的javax.validation + 正则表达式来校验excel数据
            errMsg = new StringBuilder(ExcelValidateHelper.validateEntity(userExcelData));
            //自定义校验规则
            if (StringUtils.isEmpty(errMsg)) {
                //开始校验
                validate(userExcelData, errMsg);
            }
        } catch (NoSuchFieldException e) {
            errMsg = new StringBuilder(Translator.get("parse_data_error"));
            LogUtils.error(e.getMessage(), e);
        }

        if (StringUtils.isNotEmpty(errMsg)) {
            ExcelErrData excelErrData = new ExcelErrData(rowIndex,
                    Translator.get("number")
                            .concat(StringUtils.SPACE)
                            .concat(String.valueOf(rowIndex + 1)).concat(StringUtils.SPACE)
                            .concat(Translator.get("row"))
                            .concat(Translator.get("error"))
                            .concat("：")
                            .concat(errMsg.toString()));
            //错误信息
            errList.add(excelErrData);
        } else {
            //通过数量
            list.add(userExcelData);
        }
    }


    public void validate(UserExcelData data, StringBuilder errMsg) {
        //校验姓名
        validateName(data, errMsg);
        //校验手机
        validatePhone(data, errMsg);
        //校验邮件
        validateEmail(data, errMsg);
        //校验顶级部门
        validateDepartment(data, errMsg);

    }

    /**
     * 校验顶级部门
     *
     * @param data
     * @param errMsg
     */
    private void validateDepartment(UserExcelData data, StringBuilder errMsg) {
        String departments = data.getDepartment();
        if (StringUtils.isNotEmpty(departments)) {
            String topDepartment = departments.split("/")[0];
            if (!StringUtils.equalsIgnoreCase(departmentTree.getFirst().getName(), topDepartment)) {
                errMsg.append(Translator.get("top_department_not_exist"))
                        .append(ERROR_MSG_SEPARATOR);
            }
        }
    }

    /**
     * 校验邮件
     *
     * @param data
     * @param errMsg
     */
    private void validateEmail(UserExcelData data, StringBuilder errMsg) {
        Pattern pattern = Pattern.compile(EMAIL_REGEX);
        Matcher matcher = pattern.matcher(data.getEmail());
        if (!matcher.matches()) {
            errMsg.append(Translator.get("email_format_error"))
                    .append(ERROR_MSG_SEPARATOR);
        }
        if (organizationUserService.checkEmail(data.getEmail())) {
            errMsg.append(Translator.get("email.exist"))
                    .append(ERROR_MSG_SEPARATOR);
        }
    }

    /**
     * 校验手机
     *
     * @param data
     * @param errMsg
     */
    private void validatePhone(UserExcelData data, StringBuilder errMsg) {
        if (data.getPhone().length() > PHONE_LENGTH) {
            errMsg.append(Translator.get("phone_length"))
                    .append(ERROR_MSG_SEPARATOR);
        }
        if (organizationUserService.checkPhone(data.getPhone())) {
            errMsg.append(Translator.get("phone.exist"))
                    .append(ERROR_MSG_SEPARATOR);
        }
    }

    /**
     * 校验姓名
     *
     * @param data
     * @param errMsg
     */
    private void validateName(UserExcelData data, StringBuilder errMsg) {
        if (data.getName().length() > NAME_LENGTH) {
            errMsg.append(Translator.get("name_length"))
                    .append(ERROR_MSG_SEPARATOR);
        }
    }


    /**
     * 数据转换
     *
     * @param row
     * @return
     */
    private UserExcelData parseDataToModel(Map<Integer, String> row) {
        UserExcelData data = new UserExcelDataFactory().getUserExcelDataLocal();
        for (Map.Entry<Integer, String> headEntry : headMap.entrySet()) {
            Integer index = headEntry.getKey();
            String field = headEntry.getValue();
            if (StringUtils.isBlank(field)) {
                continue;
            }
            String value = StringUtils.isEmpty(row.get(index)) ? StringUtils.EMPTY : row.get(index);

            if (excelHeadToFieldNameDic.containsKey(field)) {
                field = excelHeadToFieldNameDic.get(field);
            }

            if (StringUtils.equals(field, "employeeId")) {
                data.setEmployeeId(value);
            } else if (StringUtils.equals(field, "name")) {
                data.setName(value);
            } else if (StringUtils.equals(field, "gender")) {
                data.setGender(value);
            } else if (StringUtils.equals(field, "department")) {
                data.setDepartment(value);
            } else if (StringUtils.equals(field, "position")) {
                data.setPosition(value);
            } else if (StringUtils.equals(field, "phone")) {
                data.setPhone(value);
            } else if (StringUtils.equals(field, "email")) {
                data.setEmail(value);
            } else if (StringUtils.equals(field, "supervisor")) {
                data.setSupervisor(value);
            } else if (StringUtils.equals(field, "workCity")) {
                data.setWorkCity(value);
            } else if (StringUtils.equals(field, "employeeType")) {
                data.setEmployeeType(value);
            }
        }
        return data;
    }


    /**
     * @description: 获取注解里ExcelProperty的value
     */
    public Set<String> genExcelHeadToFieldNameDicAndGetNotRequiredFields() throws NoSuchFieldException {

        Set<String> result = new HashSet<>();
        Field field;
        Field[] fields = excelDataClass.getDeclaredFields();
        for (Field item : fields) {
            field = excelDataClass.getDeclaredField(item.getName());
            field.setAccessible(true);
            ExcelProperty excelProperty = field.getAnnotation(ExcelProperty.class);
            if (excelProperty != null) {
                StringBuilder value = new StringBuilder();
                for (String v : excelProperty.value()) {
                    value.append(v);
                }
                excelHeadToFieldNameDic.put(value.toString(), field.getName());
                // 检查是否必有的头部信息
                if (field.getAnnotation(NotRequired.class) != null) {
                    result.add(value.toString());
                }
            }
        }
        return result;
    }

    private void formatHeadMap() {
        for (Integer key : headMap.keySet()) {
            String name = headMap.get(key);
            if (excelHeadToFieldNameDic.containsKey(name)) {
                headMap.put(key, excelHeadToFieldNameDic.get(name));
            }
        }
    }

}
