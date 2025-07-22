package io.cordys.crm.system.service;

import io.cordys.aspectj.annotation.OperationLog;
import io.cordys.aspectj.constants.LogModule;
import io.cordys.aspectj.constants.LogType;
import io.cordys.aspectj.context.OperationLogContext;
import io.cordys.aspectj.dto.LogContextInfo;
import io.cordys.common.exception.GenericException;
import io.cordys.common.uid.IDGenerator;
import io.cordys.common.util.BeanUtils;
import io.cordys.common.util.Translator;
import io.cordys.crm.system.domain.Dict;
import io.cordys.crm.system.dto.request.DictAddRequest;
import io.cordys.crm.system.dto.request.DictUpdateRequest;
import io.cordys.mybatis.BaseMapper;
import io.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(rollbackFor = Exception.class)
public class DictService {

	@Resource
	private BaseMapper<Dict> dictMapper;

	public List<Dict> getDictListByType(String type, String orgId) {
		LambdaQueryWrapper<Dict> dictLambdaQueryWrapper = new LambdaQueryWrapper<>();
		dictLambdaQueryWrapper.eq(Dict::getType, type).eq(Dict::getOrganizationId, orgId);
		dictLambdaQueryWrapper.orderByAsc(Dict::getCreateTime);
		return dictMapper.selectListByLambda(dictLambdaQueryWrapper);
	}

	/**
	 * 添加字典值
	 * @param request 请求参数
	 * @param currentUser 当前用户
	 */
	@OperationLog(module = LogModule.SYSTEM_MODULE, type = LogType.ADD)
	public void addDict(DictAddRequest request, String currentUser, String orgId) {
		Dict dict = new Dict();
		dict.setId(IDGenerator.nextStr());
		dict.setName(request.getName());
		dict.setType(request.getType());
		dict.setOrganizationId(orgId);
		dict.setCreateUser(currentUser);
		dict.setCreateTime(System.currentTimeMillis());
		dict.setUpdateUser(currentUser);
		dict.setUpdateTime(System.currentTimeMillis());
		dictMapper.insert(dict);

		// 添加日志上下文
		OperationLogContext.setContext(LogContextInfo.builder()
				.modifiedValue(dict)
				.resourceId(dict.getId())
				.resourceName(Translator.get("module.reason.setting"))
				.build());
	}

	/**
	 * 更新字典值
	 * @param request 请求参数
	 * @param currentUser 当前用户
	 */
	@OperationLog(module = LogModule.SYSTEM_MODULE, type = LogType.UPDATE)
	public void updateDict(DictUpdateRequest request, String currentUser) {
		Dict oldDict = dictMapper.selectByPrimaryKey(request.getId());
		if (oldDict == null) {
			throw new GenericException(Translator.get("dict.not_exist"));
		}
		Dict dict = BeanUtils.copyBean(new Dict(), request);
		dict.setUpdateUser(currentUser);
		dict.setUpdateTime(System.currentTimeMillis());
		dictMapper.updateById(dict);

		OperationLogContext.setContext(
				LogContextInfo.builder()
						.resourceId(request.getId())
						.resourceName(Translator.get("module.customer.capacity.setting"))
						.originalValue(oldDict)
						.modifiedValue(dict)
						.build()
		);
	}

	/**
	 * 删除字典值
	 * @param id 字典ID
	 */
	@OperationLog(module = LogModule.SYSTEM_MODULE, type = LogType.DELETE, resourceId = "{#id}")
	public void deleteDict(String id) {
		Dict dict = dictMapper.selectByPrimaryKey(id);
		if (dict == null) {
			throw new GenericException(Translator.get("dict.not_exist"));
		}
		dictMapper.deleteByPrimaryKey(id);
	}
}
