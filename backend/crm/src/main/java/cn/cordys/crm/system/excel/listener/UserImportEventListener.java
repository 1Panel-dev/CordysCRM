package cn.cordys.crm.system.excel.listener;

import cn.cordys.common.dto.BaseTreeNode;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.util.CommonBeanFactory;
import cn.cordys.common.util.LogUtils;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.system.excel.domain.UserExcelData;
import cn.cordys.crm.system.excel.domain.UserExcelDataFactory;
import cn.cordys.crm.system.service.DepartmentService;
import cn.cordys.crm.system.service.OrganizationUserService;
import cn.cordys.excel.domain.ExcelErrData;
import cn.cordys.excel.utils.ExcelValidateHelper;
import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.context.AnalysisContext;
import cn.idev.excel.event.AnalysisEventListener;
import lombok.Getter;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UserImportEventListener extends AnalysisEventListener<Map<Integer, String>> {

    protected static final int NAME_LENGTH = 255;
    protected static final int PHONE_LENGTH = 20;
    protected static final int BATCH_COUNT = 1000;
    private static final String ERROR_MSG_SEPARATOR = ";";
    private static final String EMAIL_REGEX = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
    private static final String PHONE_REGEX = "^1[0-9]\\d{9}$";
    private static final Pattern EMAIL_PATTERN = Pattern.compile(EMAIL_REGEX);
    private static final Pattern PHONE_PATTERN = Pattern.compile(PHONE_REGEX);
    // 字段赋值映射，简化 parseDataToModel
    private static final Map<String, BiConsumer<UserExcelData, String>> FIELD_SETTERS = new HashMap<>();

    static {
        FIELD_SETTERS.put("employeeId", UserExcelData::setEmployeeId);
        FIELD_SETTERS.put("name", UserExcelData::setName);
        FIELD_SETTERS.put("gender", UserExcelData::setGender);
        FIELD_SETTERS.put("department", UserExcelData::setDepartment);
        FIELD_SETTERS.put("position", UserExcelData::setPosition);
        FIELD_SETTERS.put("phone", UserExcelData::setPhone);
        FIELD_SETTERS.put("email", UserExcelData::setEmail);
        FIELD_SETTERS.put("supervisor", UserExcelData::setSupervisor);
        FIELD_SETTERS.put("workCity", UserExcelData::setWorkCity);
        FIELD_SETTERS.put("employeeType", UserExcelData::setEmployeeType);
    }

    private final Class<?> excelDataClass;
    private final Map<String, String> excelHeadToFieldNameDic = new HashMap<>();
    private final String operatorId;
    private final String orgId;
    private final OrganizationUserService organizationUserService;
    private final List<BaseTreeNode> departmentTree;
    private final Map<String, String> departmentMap = new HashMap<>();
    @Getter
    protected List<UserExcelData> list = new ArrayList<>();
    @Getter
    protected List<ExcelErrData> errList = new ArrayList<>();
    private Map<Integer, String> headMap;
    @Getter
    private int successCount = 0;

    public UserImportEventListener(Class<?> clazz, String operatorId, String orgId) {
        this.excelDataClass = clazz;
        this.organizationUserService = CommonBeanFactory.getBean(OrganizationUserService.class);
        this.departmentTree = Objects.requireNonNull(CommonBeanFactory.getBean(DepartmentService.class)).getTree(orgId);
        this.operatorId = operatorId;
        this.orgId = orgId;
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
            buildUpdateOrErrorList(rowIndex, userExcelData);
            if (list.size() > BATCH_COUNT) {
                saveData();
                this.successCount += list.size();
                list.clear();
            }
        }
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext analysisContext) {
        saveData();
        this.successCount += list.size();
        list.clear();
    }

    private void saveData() {
        if (CollectionUtils.isNotEmpty(list)) {
            organizationUserService.saveImportData(list, departmentTree, departmentMap, operatorId, orgId);
        }
    }

    private void buildUpdateOrErrorList(Integer rowIndex, UserExcelData userExcelData) {
        StringBuilder errMsg;
        try {
            String baseErr = ExcelValidateHelper.validateEntity(userExcelData);
            errMsg = new StringBuilder(baseErr);
            if (StringUtils.isEmpty(errMsg)) {
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
            errList.add(excelErrData);
        } else {
            list.add(userExcelData);
        }
    }

    public void validate(UserExcelData data, StringBuilder errMsg) {
        validateName(data, errMsg);
        validatePhone(data, errMsg);
        validateEmail(data, errMsg);
        validateDepartment(data, errMsg);
        validateLength(data, errMsg);
        handleGender(data);
        handleEmployeeType(data);
    }

    private void handleEmployeeType(UserExcelData data) {
        if (StringUtils.isNotEmpty(data.getEmployeeType())) {
            if (Strings.CI.equals(data.getEmployeeType(), Translator.get("formal"))) {
                data.setEmployeeType("formal");
            } else if (Strings.CI.equals(data.getEmployeeType(), Translator.get("internship"))) {
                data.setEmployeeType("internship");
            } else if (Strings.CI.equals(data.getEmployeeType(), Translator.get("outsourcing"))) {
                data.setEmployeeType("outsourcing");
            } else {
                data.setEmployeeType(null);
            }
        }
    }

    private void handleGender(UserExcelData data) {
        if (StringUtils.isNotEmpty(data.getGender())) {
            if (data.getGender().equals(Translator.get("man"))) {
                data.setGender("false");
            } else if (data.getGender().equals(Translator.get("woman"))) {
                data.setGender("true");
            } else {
                data.setGender(null);
            }
        }
    }

    private void validateLength(UserExcelData data, StringBuilder errMsg) {
        String employeeId = data.getEmployeeId();
        if (StringUtils.isNotBlank(employeeId) && employeeId.length() > NAME_LENGTH) {
            errMsg.append(Translator.get("employee_length")).append(ERROR_MSG_SEPARATOR);
        }

        String position = data.getPosition();
        if (StringUtils.isNotBlank(position) && position.length() > NAME_LENGTH) {
            errMsg.append(Translator.get("position_length")).append(ERROR_MSG_SEPARATOR);
        }
    }

    private void validateDepartment(UserExcelData data, StringBuilder errMsg) {
        String departments = data.getDepartment();
        if (StringUtils.isNotEmpty(departments)) {
            String topDepartment = departments.split("/")[0];
            if (!Strings.CI.equals(departmentTree.getFirst().getName(), topDepartment)) {
                errMsg.append(Translator.get("top_department_not_exist")).append(ERROR_MSG_SEPARATOR);
            }
        } else {
            data.setDepartment(departmentTree.getFirst().getName());
        }
    }

    private void validateEmail(UserExcelData data, StringBuilder errMsg) {
        String email = data.getEmail();
        if (StringUtils.isBlank(email)) {
            return; // 交给 Bean 校验，不在此重复报错
        }

        Matcher matcher = EMAIL_PATTERN.matcher(email);
        if (!matcher.matches()) {
            errMsg.append(Translator.get("email_format_error")).append(ERROR_MSG_SEPARATOR);
        }
        if (organizationUserService.checkEmail(email)) {
            errMsg.append(Translator.get("email.exist")).append(ERROR_MSG_SEPARATOR);
        }

        if (CollectionUtils.isNotEmpty(list)) {
            for (UserExcelData userExcelData : list) {
                if (Strings.CI.equals(userExcelData.getEmail(), email)) {
                    errMsg.append(Translator.get("email.repeat"))
                            .append(email)
                            .append(ERROR_MSG_SEPARATOR);
                    break;
                }
            }
        }
    }

    private void validatePhone(UserExcelData data, StringBuilder errMsg) {
        String phone = data.getPhone();
        if (StringUtils.isBlank(phone)) {
            return; // 交给 Bean 校验，不在此重复报错
        }

        if (phone.length() > PHONE_LENGTH) {
            errMsg.append(Translator.get("phone_length")).append(ERROR_MSG_SEPARATOR);
        }
        if (organizationUserService.checkPhone(phone)) {
            errMsg.append(Translator.get("phone.exist")).append(ERROR_MSG_SEPARATOR);
        }

        if (!PHONE_PATTERN.matcher(phone).matches()) {
            errMsg.append(Translator.get("import_phone_validate")).append(ERROR_MSG_SEPARATOR);
        }

        if (CollectionUtils.isNotEmpty(list)) {
            for (UserExcelData userExcelData : list) {
                if (Strings.CI.equals(userExcelData.getPhone(), phone)) {
                    errMsg.append(Translator.get("phone.repeat"))
                            .append(phone)
                            .append(ERROR_MSG_SEPARATOR);
                    break;
                }
            }
        }
    }

    private void validateName(UserExcelData data, StringBuilder errMsg) {
        String name = data.getName();
        if (StringUtils.isNotEmpty(name) && name.length() > NAME_LENGTH) {
            errMsg.append(Translator.get("name_length")).append(ERROR_MSG_SEPARATOR);
        }
    }

    private UserExcelData parseDataToModel(Map<Integer, String> row) {
        UserExcelData data = new UserExcelDataFactory().getUserExcelDataLocal();
        for (Map.Entry<Integer, String> headEntry : headMap.entrySet()) {
            Integer index = headEntry.getKey();
            String field = headEntry.getValue();
            if (StringUtils.isBlank(field)) {
                continue;
            }
            String value = StringUtils.defaultString(row.get(index));
            String mappedField = excelHeadToFieldNameDic.getOrDefault(field, field);

            BiConsumer<UserExcelData, String> setter = FIELD_SETTERS.get(mappedField);
            if (setter != null) {
                setter.accept(data, value);
            }
        }
        return data;
    }

    // 获取注解里 ExcelProperty 的值映射
    public void genExcelHeadToFieldNameDicAndGetNotRequiredFields() throws NoSuchFieldException {
        Field[] fields = excelDataClass.getDeclaredFields();
        for (Field item : fields) {
            Field field = excelDataClass.getDeclaredField(item.getName());
            field.setAccessible(true);
            ExcelProperty excelProperty = field.getAnnotation(ExcelProperty.class);
            if (excelProperty != null) {
                String value = String.join("", excelProperty.value());
                excelHeadToFieldNameDic.put(value, field.getName());
            }
        }
    }

    private void formatHeadMap() {
        for (Map.Entry<Integer, String> entry : headMap.entrySet()) {
            String name = entry.getValue();
            String mapped = excelHeadToFieldNameDic.get(name);
            if (mapped != null) {
                entry.setValue(mapped);
            }
        }
    }
}