package io.cordys.crm.lead.service;

import io.cordys.common.uid.IDGenerator;
import io.cordys.common.util.BeanUtils;
import io.cordys.common.util.Translator;
import io.cordys.crm.lead.domain.LeadCapacity;
import io.cordys.crm.lead.dto.request.LeadCapacityPageRequest;
import io.cordys.crm.lead.dto.request.LeadCapacitySaveRequest;
import io.cordys.mybatis.BaseMapper;
import io.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(rollbackFor = Exception.class)
public class LeadCapacityService {

	@Resource
	private BaseMapper<LeadCapacity> leadCapacityMapper;

	/**
	 * 分页获取线索库容设置
	 * @param request 请求参数
	 * @return 线索库容设置列表
	 */
	public List<LeadCapacity> page(LeadCapacityPageRequest request) {
		LambdaQueryWrapper<LeadCapacity> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(LeadCapacity::getOrganizationId, request.getOrganizationId());
		return leadCapacityMapper.selectListByLambda(wrapper);
	}

	/**
	 * 保存线索库容规则
	 * @param request 请求参数
	 * @param currentUserId 当前用户ID
	 */
	public void save(LeadCapacitySaveRequest request, String currentUserId) {
		LeadCapacity leadCapacity = new LeadCapacity();
		BeanUtils.copyBean(leadCapacity, request);
		if (leadCapacity.getId() == null) {
			leadCapacity.setId(IDGenerator.nextStr());
			leadCapacity.setCreateTime(System.currentTimeMillis());
			leadCapacity.setCreateUser(currentUserId);
			leadCapacity.setUpdateTime(System.currentTimeMillis());
			leadCapacity.setUpdateUser(currentUserId);
			leadCapacityMapper.insert(leadCapacity);
		} else {
			checkLeadCapacityExist(leadCapacity.getId());
			leadCapacity.setUpdateUser(currentUserId);
			leadCapacity.setUpdateTime(System.currentTimeMillis());
			leadCapacityMapper.update(leadCapacity);
		}
	}

	/**
	 * 删除线索库容规则
	 * @param id 线索库容规则ID
	 */
	public void delete(String id) {
		checkLeadCapacityExist(id);
		leadCapacityMapper.deleteByPrimaryKey(id);
	}

	/**
	 * 校验线索库容规则是否存在
	 * @param id 线索库容规则ID
	 */
	private void checkLeadCapacityExist(String id) {
		LeadCapacity leadCapacity = leadCapacityMapper.selectByPrimaryKey(id);
		if (leadCapacity == null) {
			throw new RuntimeException(Translator.get("lead_capacity_not_exist"));
		}
	}
}
