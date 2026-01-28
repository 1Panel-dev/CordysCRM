package cn.cordys.crm.system.service;

import cn.cordys.common.constants.FormKey;
import cn.cordys.common.dto.JsonDifferenceDTO;
import cn.cordys.common.util.Translator;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(rollbackFor = Exception.class)
public class OrganizationLogService extends BaseModuleLogService {
  @Resource private DepartmentService departmentService;
  @Resource private OrganizationUserService organizationUserService;

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
    differences = super.handleModuleLogField(differences, orgId, FormKey.OPPORTUNITY.getKey());

    // 构建列名 -> 处理函数映射
    Map<String, Consumer<JsonDifferenceDTO>> handlers =
        Map.of(
            Translator.get("log.roles"), this::handRoleValueName,
            Translator.get("log.commander"), this::setUserFieldName,
            Translator.get("log.departmentId"), this::setDepartmentName,
            Translator.get("log.supervisorId"), this::setSupervisorName,
            Translator.get("log.enable"),
                differ -> {
                  differ.setOldValueName(
                      toText(differ.getOldValueName(), "log.enable.true", "log.enable.false"));
                  differ.setNewValueName(
                      toText(differ.getNewValueName(), "log.enable.true", "log.enable.false"));
                },
            Translator.get("log.gender"),
                differ -> {
                  differ.setOldValueName(toText(differ.getOldValueName(), "woman", "man"));
                  differ.setNewValueName(toText(differ.getNewValueName(), "woman", "man"));
                },
            Translator.get("log.employeeType"),
                differ -> {
                  differ.setOldValueName(Translator.get(differ.getOldValue().toString()));
                  differ.setNewValueName(Translator.get(differ.getNewValue().toString()));
                });

    // 流式处理
    differences.forEach(
        differ ->
            handlers.entrySet().stream()
                .filter(e -> Strings.CS.equals(differ.getColumnName(), e.getKey()))
                .findFirst()
                .ifPresent(e -> e.getValue().accept(differ)));

    return differences;
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
