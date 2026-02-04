package cn.cordys.crm.system.service;

import cn.cordys.common.constants.LinkScenarioKey;
import cn.cordys.common.dto.JsonDifferenceDTO;
import cn.cordys.common.util.JSON;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.search.constants.SearchModuleEnum;
import cn.cordys.crm.system.domain.ModuleField;
import cn.cordys.crm.system.dto.ScopeNameDTO;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(rollbackFor = Exception.class)
public class SystemModuleLogService extends BaseModuleLogService {

    public static final String LINK_KEY_SPILT = "-";
    @Resource
    private BaseMapper<ModuleField> moduleFieldMapper;
    @Resource
    private UserExtendService userExtendService;

    @Override
    public List<JsonDifferenceDTO> handleLogField(List<JsonDifferenceDTO> differences, String orgId) {
        differences.forEach(differ -> {
            if (isLinkFormKey(differ.getColumn()) && differ.getColumn().split(LINK_KEY_SPILT).length == 2) {
                String[] splitKey = differ.getColumn().split(LINK_KEY_SPILT);
                differ.setColumnName(Translator.get("log." + splitKey[0] + ".link") + "-" + Translator.get(splitKey[1].toLowerCase()));
                handleLinkFieldsLogDetail(differ);
            }
            if (Strings.CS.equals("module.switch", differ.getColumn())) {
                differ.setColumnName(Translator.get(differ.getColumn()));
                differ.setOldValueName(differ.getOldValue());
                differ.setNewValueName(differ.getNewValue());
            }
            if (Strings.CS.equals("fields", differ.getColumn())) {
                differ.setColumnName(Translator.get("log." + differ.getColumn()));
                handleFieldsLogDetail(differ);
            }
            if (Strings.CS.equals("sort", differ.getColumn())) {
                differ.setColumnName(Translator.get("module.main.nav"));
                handleModuleMainNav(differ);
            }

            if (Strings.CS.equals("navigationSort", differ.getColumn())) {
                differ.setColumnName(Translator.get("top.navigation"));
                handleModuleMainNav(differ);
            }


            if (Strings.CI.equalsAny(differ.getColumn(),
                    "rate", "stage", "afootRollBack", "endRollBack","name")) {
                differ.setColumnName(Translator.get("log.".concat(differ.getColumn())));
                differ.setNewValueName(differ.getNewValue());
                differ.setOldValueName(differ.getOldValue());
            }

            if (Strings.CS.equals("stageSort", differ.getColumn())) {
                differ.setColumnName(Translator.get("opportunity_stage_setting"));
                differ.setNewValueName(differ.getNewValue());
                differ.setOldValueName(differ.getOldValue());
            }

            handlePoolConfig(differ);

            searchSetting(differ);
        });

        return differences;
    }

    private void handlePoolConfig(JsonDifferenceDTO differ) {
        if (Strings.CS.equals("scopeId", differ.getColumn())) {
            differ.setColumnName(Translator.get("log.pool.members"));
            handlePoolOwnerOrMember(differ);
        }

        if (Strings.CS.equals("ownerId", differ.getColumn())) {
            differ.setColumnName(Translator.get("log.pool.owner"));
            handlePoolOwnerOrMember(differ);
        }

        if (Strings.CS.equals("auto", differ.getColumn())) {
            differ.setColumnName(Translator.get("log.pool.auto"));
            differ.setNewValueName(differ.getNewValue());
            differ.setOldValueName(differ.getOldValue());
        }
    }

    private void searchSetting(JsonDifferenceDTO differ) {
        List<String> oldValues = parseFieldList(differ.getOldValue()).stream().map(String::valueOf).toList();
        List<String> newValues = parseFieldList(differ.getNewValue()).stream().map(String::valueOf).toList();
        if (CollectionUtils.isNotEmpty(oldValues)) {
            List<ModuleField> moduleFields = moduleFieldMapper.selectByIds(oldValues);
            differ.setOldValueName(moduleFields.stream()
                    .map(ModuleField::getName)
                    .toList());
        }
        if (CollectionUtils.isNotEmpty(newValues)) {
            List<ModuleField> moduleFields = moduleFieldMapper.selectByIds(newValues);
            differ.setNewValueName(moduleFields.stream()
                    .map(ModuleField::getName)
                    .toList());
        }
        switch (differ.getColumn()) {
            case SearchModuleEnum.SEARCH_ADVANCED_CLUE:
                differ.setColumnName(Translator.get("clue"));
                break;
            case SearchModuleEnum.SEARCH_ADVANCED_CUSTOMER:
                differ.setColumnName(Translator.get("customer"));
                break;
            case SearchModuleEnum.SEARCH_ADVANCED_CONTACT:
                differ.setColumnName(Translator.get("contact"));
                break;
            case SearchModuleEnum.SEARCH_ADVANCED_PUBLIC:
                differ.setColumnName(Translator.get("customer_pool"));
                break;
            case SearchModuleEnum.SEARCH_ADVANCED_CLUE_POOL:
                differ.setColumnName(Translator.get("clue_pool"));
                break;
            case SearchModuleEnum.SEARCH_ADVANCED_OPPORTUNITY:
                differ.setColumnName(Translator.get("opportunity"));
                break;
        }
    }

    private void handlePoolOwnerOrMember(JsonDifferenceDTO differ) {

        if (differ.getOldValue() != null) {
            List<ScopeNameDTO> oldScope = userExtendService.getScope(JSON.parseArray((String) differ.getOldValue(), String.class));
            differ.setOldValueName(JSON.toJSONString(oldScope.stream().map(ScopeNameDTO::getName).toList()));
        }

        if (differ.getNewValue() != null) {
            List<ScopeNameDTO> newScope = userExtendService.getScope(JSON.parseArray((String) differ.getNewValue(), String.class));
            differ.setNewValue(JSON.toJSONString(newScope.stream().map(ScopeNameDTO::getName).toList()));
        }
    }

    private void handleModuleMainNav(JsonDifferenceDTO differ) {
        // 将 oldValue 和 newValue 转为列表并映射为名称
        differ.setOldValueName(parseFieldList(differ.getOldValue())
                .stream()
                .map(String::valueOf)
                .map(Translator::get)
                .toList());

        differ.setNewValueName(parseFieldList(differ.getNewValue())
                .stream()
                .map(String::valueOf)
                .map(Translator::get)
                .toList());
    }


    /**
     * 待定: 目前就只粗略展示字段的变更
     *
     * @param differ json-difference dto
     */
    private void handleFieldsLogDetail(JsonDifferenceDTO differ) {
        differ.setOldValueName(parseFieldList(differ.getOldValue()).stream()
                .map(f -> String.valueOf(((Map<?, ?>) f).get("name")))
                .toList());
        differ.setNewValueName(parseFieldList(differ.getNewValue()).stream()
                .map(f -> String.valueOf(((Map<?, ?>) f).get("name")))
                .toList());
    }


    private void handleLinkFieldsLogDetail(JsonDifferenceDTO differ) {
        Map<String, String> oldPairs = parseLinkFieldMap(differ.getOldValue());
        Map<String, String> newPairs = parseLinkFieldMap(differ.getNewValue());

        // 去重且保序
        Set<String> fieldIds = new LinkedHashSet<>();
        oldPairs.forEach((k, v) -> {
            fieldIds.add(k);
            fieldIds.add(v);
        });
        newPairs.forEach((k, v) -> {
            fieldIds.add(k);
            fieldIds.add(v);
        });

        Map<String, String> fieldMap = new HashMap<>();
        if (!fieldIds.isEmpty()) {
            LambdaQueryWrapper<ModuleField> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.in(ModuleField::getId, new ArrayList<>(fieldIds));
            List<ModuleField> moduleFields = moduleFieldMapper.selectListByLambda(queryWrapper);
            fieldMap = moduleFields.stream()
                    .collect(Collectors.toMap(ModuleField::getId, ModuleField::getName, (a, b) -> a));
        }

        if (!oldPairs.isEmpty()) {
            differ.setOldValueName(toNamePairs(oldPairs, fieldMap));
        }
        if (!newPairs.isEmpty()) {
            differ.setNewValueName(toNamePairs(newPairs, fieldMap));
        }
    }

    private Map<String, String> parseLinkFieldMap(Object value) {
        Map<String, String> result = new LinkedHashMap<>();
        if (!(value instanceof List<?> list)) {
            return result;
        }
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> m)) {
                continue;
            }
            Object current = m.get("current");
            Object link = m.get("link");
            if (current != null && link != null) {
                result.put(String.valueOf(current), String.valueOf(link));
            }
        }
        return result;
    }

    private List<?> parseFieldList(Object value) {
        if (value instanceof List<?> list) {
            return list;
        }
        return Collections.emptyList();
    }

    private List<String> toNamePairs(Map<String, String> pairs, Map<String, String> fieldMap) {
        return pairs.entrySet().stream()
                .map(e -> fieldMap.getOrDefault(e.getKey(), e.getKey())
                        + "-" + fieldMap.getOrDefault(e.getValue(), e.getValue()))
                .collect(Collectors.toList());
    }

    private boolean isLinkFormKey(String key) {
        for (LinkScenarioKey linkKey : LinkScenarioKey.values()) {
            if (key.contains(linkKey.name())) {
                return true;
            }
        }
        return false;
    }
}
