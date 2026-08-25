package cn.cordys.crm.system.service;

import cn.cordys.common.constants.FormKey;
import cn.cordys.common.dto.JsonDifferenceDTO;
import cn.cordys.common.util.Translator;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Service
@Transactional(rollbackFor = Exception.class)
public class OrganizationLogService extends BaseModuleLogService {
    @Resource
    private DepartmentService departmentService;
    @Resource
    private OrganizationUserService organizationUserService;

    private void handRoleValueName(JsonDifferenceDTO differ) {
        differ.setOldValueName(resolveRoleNames(differ.getOldValue()));
        differ.setNewValueName(resolveRoleNames(differ.getNewValue()));
    }

    private static List<String> resolveRoleNames(Object value) {
        if (!(value instanceof List<?> list)) {
            return new ArrayList<>();
        }

        List<String> result = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> map)) {
                continue;
            }

            Object nameValue = map.get("name");
            if (nameValue == null) {
                continue;
            }

            result.add(resolveRoleName(nameValue.toString()));
        }
        return result;
    }

    private static String resolveRoleName(String roleKey) {
        return switch (roleKey) {
            case "org_admin" -> Translator.get("role.org_admin");
            case "sales_manager" -> Translator.get("role.sales_staff");
            case "role.sales_manager" -> Translator.get("role.sales_manager");
            default -> roleKey;
        };
    }

    @Override
    public List<JsonDifferenceDTO> handleLogField(List<JsonDifferenceDTO> differences, String orgId) {

        differences.forEach(differ -> {
            if (Strings.CS.equals(differ.getColumnName(), "roles")) {
                handRoleValueName(differ);
            }

            if (Strings.CS.equals(differ.getColumnName(), "commander")) {
                setUserFieldName(differ);
            }

            if (Strings.CS.equals(differ.getColumnName(), "departmentId")) {
                setDepartmentName(differ);
            }

            if (Strings.CS.equals(differ.getColumn(), "supervisorId")) {
                setSupervisorName(differ);
            }

            if (Strings.CS.equals(differ.getColumn(), "enable")) {
                differ.setOldValueName(Boolean.valueOf(differ.getOldValueName().toString()) ? Translator.get("log.enable.true") : Translator.get("log.enable.false"));
                differ.setNewValueName(Boolean.valueOf(differ.getNewValueName().toString()) ? Translator.get("log.enable.true") : Translator.get("log.enable.false"));
            }

            if (Strings.CS.equals(differ.getColumn(), "gender")) {
                differ.setOldValueName(Boolean.valueOf(differ.getOldValueName().toString()) ? Translator.get("woman") : Translator.get("man"));
                differ.setNewValueName(Boolean.valueOf(differ.getNewValueName().toString()) ? Translator.get("woman") : Translator.get("man"));
            }

            if (Strings.CS.equals(differ.getColumn(), "employeeType")) {
                differ.setOldValueName(Translator.get(differ.getOldValue().toString()));
                differ.setNewValueName(Translator.get(differ.getNewValue().toString()));
            }

            if (Strings.CS.equals(differ.getColumn(), "name")) {
                setName(differ);
            }
        });

        return differences;
    }

    private void setName(JsonDifferenceDTO jsonDifferenceDTO) {
        BaseModuleLogService.translatorDifferInfo(jsonDifferenceDTO);
    }

    // 通用 Boolean / Object 转译方法
    private String toText(Object value, String trueKey, String falseKey) {
        return Translator.get(Boolean.parseBoolean(value.toString()) ? trueKey : falseKey);
    }

    protected void setDepartmentName(JsonDifferenceDTO differ) {
        if (differ.getOldValue() != null) {
            String userName = departmentService.getDepartmentName(differ.getOldValue().toString());
            differ.setOldValueName(userName);
        }
        if (differ.getNewValue() != null) {
            String userName = departmentService.getDepartmentName(differ.getNewValue().toString());
            differ.setNewValueName(userName);
        }
    }

    protected void setSupervisorName(JsonDifferenceDTO differ) {
        if (differ.getOldValue() != null) {
            String userName = organizationUserService.getSupervisorName(differ.getOldValue().toString());
            differ.setOldValueName(userName);
        }
        if (differ.getNewValue() != null) {
            String userName = organizationUserService.getSupervisorName(differ.getNewValue().toString());
            differ.setNewValueName(userName);
        }
    }
}
