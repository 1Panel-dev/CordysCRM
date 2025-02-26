package io.cordys.common.service;

import io.cordys.common.domain.BaseModuleFieldValue;
import io.cordys.common.resolver.field.AbstractModuleFieldResolver;
import io.cordys.common.resolver.field.ModuleFieldResolverFactory;
import io.cordys.common.util.LogUtils;
import io.cordys.context.OrganizationContext;
import io.cordys.crm.system.dto.field.base.BaseField;
import io.cordys.crm.system.service.ModuleFormService;
import io.cordys.mybatis.BaseMapper;
import io.cordys.mybatis.lambda.LambdaQueryWrapper;
import io.cordys.mybatis.lambda.XFunction;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 资源与模块字段值的公共处理类
 *
 * @author jianxing
 * @date 2025-01-03 12:01:54
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class BaseModuleFieldValueService {

    @Resource
    private ModuleFormService moduleFormService;

    /**
     * 获取资源和模块字段的 map
     *
     * @param resourceIds
     * @param resourceIdGetFunc
     * @param <T>
     * @return
     */
    public <T extends BaseModuleFieldValue> Map<String, List<T>> getResourceFiledMap(String formKey,
                                                                                     List<String> resourceIds,
                                                                                     XFunction<T, String> resourceIdGetFunc,
                                                                                     BaseMapper<T> mapper) {
        if (CollectionUtils.isEmpty(resourceIds)) {
            return Map.of();
        }
        List<T> customerFields = getModuleFieldValuesByResourceIds(formKey, resourceIds, resourceIdGetFunc, mapper);
        return customerFields.stream()
                .collect(Collectors.groupingBy(resourceIdGetFunc));
    }

    /**
     * 查询指定资源的模块字段值
     *
     * @param resourceIds
     * @param resourceIdGetFunc
     * @param <T>
     * @return
     */
    public <T extends BaseModuleFieldValue> List<T> getModuleFieldValuesByResourceIds(String formKey,
                                                                                      List<String> resourceIds,
                                                                                      XFunction<T, String> resourceIdGetFunc,
                                                                                      BaseMapper<T> mapper) {
        Map<String, BaseField> fieldConfigMap = moduleFormService.getAllFields(formKey, OrganizationContext.getOrganizationId())
                .stream()
                .collect(Collectors.toMap(BaseField::getId, Function.identity()));

        LambdaQueryWrapper<T> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(resourceIdGetFunc, resourceIds);
        List<T> moduleFieldValues = mapper.selectListByLambda(wrapper);
        moduleFieldValues.forEach(moduleFieldValue -> {
            if (moduleFieldValue.getFieldValue() != null) {
                BaseField fieldConfig = fieldConfigMap.get(moduleFieldValue.getFieldId());
                if (fieldConfig == null) {
                    return;
                }
                // 获取字段解析器
                AbstractModuleFieldResolver customFieldResolver = ModuleFieldResolverFactory.getResolver(fieldConfig.getType());
                // 将数据库中的字符串值,转换为对应的对象值
                Object objectValue = customFieldResolver.parse2Value(fieldConfig, moduleFieldValue.getFieldValue().toString());
                moduleFieldValue.setFieldValue(objectValue);
            }

        });
        return moduleFieldValues;
    }

    public <T extends BaseModuleFieldValue> List<T> getCustomerFields(String formKey,
                                                                      List<BaseModuleFieldValue> moduleFieldValues,
                                                                      Class<T> clazz) {
        if (CollectionUtils.isEmpty(moduleFieldValues)) {
            return List.of();
        }

        Map<String, BaseField> fieldConfigMap = moduleFormService.getAllFields(formKey, OrganizationContext.getOrganizationId())
                .stream()
                .collect(Collectors.toMap(BaseField::getId, Function.identity()));

        List<T> customerFields = new ArrayList<>();
        moduleFieldValues.stream()
                .filter(BaseModuleFieldValue::valid)
                .forEach(fieldValue -> {
                    BaseField fieldConfig = fieldConfigMap.get(fieldValue.getFieldId());
                    if (fieldConfig == null) {
                        return;
                    }

                    // 获取字段解析器
                    AbstractModuleFieldResolver customFieldResolver = ModuleFieldResolverFactory.getResolver(fieldConfig.getType());
                    // 校验参数值
                    customFieldResolver.validate(fieldConfig, fieldValue.getFieldValue());
                    // 将参数值转换成字符串入库
                    String strValue = customFieldResolver.parse2String(fieldConfig, fieldValue.getFieldValue());

                    try {
                        T moduleField = clazz.getConstructor().newInstance();
                        moduleField.setFieldId(fieldValue.getFieldId());
                        moduleField.setFieldValue(strValue);
                        customerFields.add(moduleField);
                    } catch (Exception e) {
                        LogUtils.error(e);
                    }
                });
        return customerFields;
    }
}