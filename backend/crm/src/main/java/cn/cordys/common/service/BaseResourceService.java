package cn.cordys.common.service;

import cn.cordys.aspectj.constants.LogType;
import cn.cordys.aspectj.dto.LogDTO;
import cn.cordys.common.domain.BaseResourceField;
import cn.cordys.common.resolver.field.AbstractModuleFieldResolver;
import cn.cordys.common.resolver.field.ModuleFieldResolverFactory;
import cn.cordys.common.util.CaseFormatUtils;
import cn.cordys.common.util.LogUtils;
import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.system.dto.request.ResourceBatchEditRequest;
import cn.cordys.crm.system.service.LogService;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 资源服务通用类
 *
 * @author song-cc-rock
 */
public abstract class BaseResourceService {

	@Resource
	private LogService logService;

	/**
	 * 获取资源字段值
	 *
	 * @param resource 资源对象
	 * @param fieldName 字段名称
	 * @return 值
	 * @param <K> 资源类型
	 */
	protected <K> Object getResourceFieldValue(K resource, String fieldName) {
		Class<?> clazz = resource.getClass();
		// 获取字段值
		Object fieldValue = null;
		try {
			fieldValue = clazz.getMethod("get" + CaseFormatUtils.capitalizeFirstLetter(fieldName))
					.invoke(resource);
		} catch (Exception e) {
			LogUtils.error(e);
		}
		return fieldValue;
	}

	/**
	 * 设置资源字段值
	 *
	 * @param resource 资源对象
	 * @param fieldName 字段名称
	 * @param value 值
	 * @param <K> 资源类型
	 */
	protected <K> void setResourceFieldValue(K resource, String fieldName, Object value) {
		Class<?> clazz = resource.getClass();
		// 设置字段值
		try {
			if (value != null) {
				Class<?> valueClass = value.getClass();
				switch (value) {
					case List<?> ignored -> valueClass = List.class;
					case Map<?, ?> ignored -> valueClass = Map.class;
					case Integer i -> {
						Class<?> type = clazz.getDeclaredField(fieldName).getType();
						if (type.equals(BigDecimal.class)) {
							value = BigDecimal.valueOf(i);
						} else if (type.equals(Long.class)) {
							value = Long.valueOf(i);
						}
						valueClass = value.getClass();
					}
					default -> {
					}
				}
				clazz.getMethod("set" + CaseFormatUtils.capitalizeFirstLetter(fieldName), valueClass)
						.invoke(resource, value);
			}
		} catch (Exception e) {
			LogUtils.error(e);
		}
	}

	/**
	 * 创建资源实例
	 * @param clazz 资源类对象
	 * @param <K> 资源类型
	 * @return 资源实例
	 */
	protected <K> K newInstance(Class<K> clazz) {
		K resource;
		try {
			resource = clazz.getConstructor().newInstance();
		} catch (Exception e) {
			LogUtils.error(e);
			throw new RuntimeException(e);
		}
		return resource;
	}

	/**
	 * 判断值非空
	 *
	 * @param value 值
	 * @return 是否非空
	 */
	@SuppressWarnings("rawtypes")
	protected boolean isNotBlank(Object value) {
		switch (value) {
			case null -> {
				return false;
			}
			case String str -> {
				return StringUtils.isNotBlank(str);
			}
			case List list -> {
				return CollectionUtils.isNotEmpty(list);
			}
			default -> {
				return true;
			}
		}
	}

	/**
	 * 记录自定义字段批量更新日志
	 *
	 * @param originResourceList 资源集合
	 * @param originFields       旧的字段值
	 * @param field              字段信息
	 * @param request            请求参数
	 * @param userId             用户ID
	 * @param orgId              组织ID
	 * @param <K>                资源类型
	 */
	@SuppressWarnings({"rawtypes", "unchecked"})
	protected <K> void addCustomFieldBatchUpdateLog(List<K> originResourceList,
										  List<? extends BaseResourceField> originFields,
										  ResourceBatchEditRequest request,
										  BaseField field,
										  String logModule,
										  String userId,
										  String orgId) {
		Map<String, ? extends BaseResourceField> fieldMap = originFields.stream()
				.collect(Collectors.toMap(BaseResourceField::getResourceId, Function.identity()));
		// 记录日志
		List<LogDTO> logs = originResourceList.stream()
				.map(resource -> {
					Object id = getResourceFieldValue(resource, "id");
					Object name = getResourceFieldValue(resource, "name");

					BaseResourceField baseResourceField = fieldMap.get(id.toString());
					Map originResource = new HashMap();
					if (baseResourceField != null && isNotBlank(baseResourceField.getFieldValue())) {
						// 获取字段解析器
						AbstractModuleFieldResolver customFieldResolver = ModuleFieldResolverFactory.getResolver(field.getType());
						// 将数据库中的字符串值,转换为对应的对象值
						Object objectValue = customFieldResolver.convertToValue(field, baseResourceField.getFieldValue().toString());
						baseResourceField.setFieldValue(objectValue);
						originResource.put(request.getFieldId(), baseResourceField.getFieldValue());
					}

					Map modifiedResource = new HashMap();
					if (isNotBlank(request.getFieldValue())) {
						modifiedResource.put(request.getFieldId(), request.getFieldValue());
					}

					LogDTO logDTO = new LogDTO(orgId, id.toString(), userId, LogType.UPDATE, logModule, name.toString());
					logDTO.setOriginalValue(originResource);
					logDTO.setModifiedValue(modifiedResource);
					return logDTO;
				}).toList();

		logService.batchAdd(logs);
	}

	/**
	 * 记录业务字段批量更新日志
	 *
	 * @param originResourceList 资源集合
	 * @param field              字段信息
	 * @param request            请求参数
	 * @param userId             用户ID
	 * @param orgId              组织ID
	 * @param <K>                资源类型
	 */
	@SuppressWarnings({"rawtypes", "unchecked"})
	protected  <K> void addBusinessFieldBatchUpdateLog(List<K> originResourceList,
													BaseField field,
													ResourceBatchEditRequest request,
													String logModule,
													String userId,
													String orgId) {
		// 记录日志
		List<LogDTO> logs = originResourceList.stream()
				.map(resource -> {
					Map originResource = new HashMap();
					if (isNotBlank(getResourceFieldValue(resource, field.getBusinessKey()))) {
						originResource.put(field.getBusinessKey(), getResourceFieldValue(resource, field.getBusinessKey()));
					}

					Map modifiedResource = new HashMap();
					if (isNotBlank(request.getFieldValue())) {
						modifiedResource.put(field.getBusinessKey(), request.getFieldValue());
					}

					Object id = getResourceFieldValue(resource, "id");
					Object name = getResourceFieldValue(resource, "name");

					LogDTO logDTO = new LogDTO(orgId, id.toString(), userId, LogType.UPDATE, logModule, name.toString());
					logDTO.setOriginalValue(originResource);
					logDTO.setModifiedValue(modifiedResource);
					return logDTO;
				}).toList();

		logService.batchAdd(logs);
	}
}
