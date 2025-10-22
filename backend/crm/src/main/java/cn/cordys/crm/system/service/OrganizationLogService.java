package cn.cordys.crm.system.service;

import cn.cordys.common.constants.FormKey;
import cn.cordys.common.dto.JsonDifferenceDTO;
import cn.cordys.common.util.Translator;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Transactional(rollbackFor = Exception.class)
public class OrganizationLogService extends BaseModuleLogService {
    @Resource
    private DepartmentService departmentService;
    @Resource
    private OrganizationUserService organizationUserService;

    private static void handRoleValueName(JsonDifferenceDTO differ) {
        List<String> oldValueNames = new ArrayList<>();
        if (differ.getOldValue() instanceof List) {
            for (Object item : (List<?>) differ.getOldValue()) {
                if (item instanceof Map) {
                    Object nameValue = ((Map<?, ?>) item).get("name");
                    if (nameValue != null) {
                        switch (nameValue.toString()) {
                            case "org_admin" -> oldValueNames.add(Translator.get("role.org_admin"));
                            case "sales_manager" -> oldValueNames.add(Translator.get("role.sales_staff"));
                            case "role.sales_manager" -> oldValueNames.add(Translator.get("role.sales_manager"));
                            default -> oldValueNames.add(nameValue.toString());
                        }
                    }
                }
            }
        }
        differ.setOldValueName(oldValueNames);

        List<String> newValueNames = new ArrayList<>();
        if (differ.getNewValue() instanceof List) {
            for (Object item : (List<?>) differ.getNewValue()) {
                if (item instanceof Map) {
                    Object nameValue = ((Map<?, ?>) item).get("name");
                    if (nameValue != null) {
                        switch (nameValue.toString()) {
                            case "org_admin" -> newValueNames.add(Translator.get("role.org_admin"));
                            case "sales_manager" -> newValueNames.add(Translator.get("role.sales_staff"));
                            case "role.sales_manager" -> newValueNames.add(Translator.get("role.sales_manager"));
                            default -> newValueNames.add(nameValue.toString());
                        }
                    }
                }
            }
        }
        differ.setNewValueName(newValueNames);

    }

    @Override
    public void handleLogField(List<JsonDifferenceDTO> differenceDTOS, String orgId) {
        super.handleModuleLogField(differenceDTOS, orgId, FormKey.OPPORTUNITY.getKey());

        differenceDTOS.forEach(differ -> {
            if (Strings.CS.equals(differ.getColumnName(), Translator.get("log.roles"))) {
                handRoleValueName(differ);
            }

            if (Strings.CS.equals(differ.getColumnName(), Translator.get("log.commander"))) {
                setUserFieldName(differ);
            }

            if (Strings.CS.equals(differ.getColumnName(), Translator.get("log.departmentId"))) {
                setDepartmentName(differ);
            }

            if (Strings.CS.equals(differ.getColumnName(), Translator.get("log.supervisorId"))) {
                setSupervisorName(differ);
            }

            if (Strings.CS.equals(differ.getColumnName(), Translator.get("log.enable"))) {
                differ.setOldValueName(Boolean.valueOf(differ.getOldValueName().toString()) ? Translator.get("log.enable.true") : Translator.get("log.enable.false"));
                differ.setNewValueName(Boolean.valueOf(differ.getNewValueName().toString()) ? Translator.get("log.enable.true") : Translator.get("log.enable.false"));
            }

            if (Strings.CS.equals(differ.getColumnName(), Translator.get("log.gender"))) {
                differ.setOldValueName(Boolean.valueOf(differ.getOldValueName().toString()) ? Translator.get("woman") : Translator.get("man"));
                differ.setNewValueName(Boolean.valueOf(differ.getNewValueName().toString()) ? Translator.get("woman") : Translator.get("man"));
            }

            if (Strings.CS.equals(differ.getColumnName(), Translator.get("log.employeeType"))) {
                differ.setOldValueName(Translator.get(differ.getOldValue().toString()));
                differ.setNewValueName(Translator.get(differ.getNewValue().toString()));
            }
        });
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
