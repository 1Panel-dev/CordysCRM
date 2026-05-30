package cn.cordys.crm.clue.service;

import cn.cordys.aspectj.annotation.OperationLog;
import cn.cordys.aspectj.constants.LogModule;
import cn.cordys.aspectj.constants.LogType;
import cn.cordys.aspectj.context.OperationLogContext;
import cn.cordys.aspectj.dto.LogContextInfo;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.JSON;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.clue.domain.ClueCapacity;
import cn.cordys.crm.clue.dto.ClueCapacityDTO;
import cn.cordys.crm.clue.mapper.ExtClueCapacityMapper;
import cn.cordys.crm.system.dto.request.CapacityAddRequest;
import cn.cordys.crm.system.dto.request.CapacityUpdateRequest;
import cn.cordys.crm.system.service.UserExtendService;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Transactional(rollbackFor = Exception.class)
public class ClueCapacityService {

    @Resource
    private BaseMapper<ClueCapacity> clueCapacityMapper;
    @Resource
    private UserExtendService userExtendService;
    @Resource
    private ExtClueCapacityMapper extClueCapacityMapper;

    /**
     * 分页获取线索库容设置（按创建时间升序）
     */
    public List<ClueCapacityDTO> list(String currentOrgId) {
        LambdaQueryWrapper<ClueCapacity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ClueCapacity::getOrganizationId, currentOrgId)
                .orderByAsc(ClueCapacity::getCreateTime);  // 直接数据库升序，避免内存排序

        List<ClueCapacity> capacities = clueCapacityMapper.selectListByLambda(wrapper);
        if (CollectionUtils.isEmpty(capacities)) {
            return new ArrayList<>();
        }

        // 转换DTO，注意 scopeId 可能为空的情况
        return capacities.stream().map(capacity -> {
            ClueCapacityDTO dto = new ClueCapacityDTO();
            dto.setId(capacity.getId());
            dto.setCapacity(capacity.getCapacity());
            List<String> scopeIds = JSON.parseArray(capacity.getScopeId(), String.class);
            dto.setMembers(CollectionUtils.isEmpty(scopeIds) ? new ArrayList<>() : userExtendService.getScope(scopeIds));
            return dto;
        }).collect(Collectors.toList());
    }

    @OperationLog(module = LogModule.SYSTEM_MODULE, type = LogType.ADD)
    public void add(CapacityAddRequest request, String currentUserId, String currentOrgId) {
        // 仅查询当前组织下的库容，避免原 selectAll(null) 导致的全表扫描
        List<ClueCapacity> existingCapacities = getByOrgId(currentOrgId);
        List<String> existingScopeIds = flatMapScopeIds(existingCapacities);

        if (userExtendService.hasDuplicateScopeObj(request.getScopeIds(), existingScopeIds, currentOrgId)) {
            throw new GenericException(Translator.get("capacity.scope.duplicate"));
        }

        ClueCapacity capacity = new ClueCapacity();
        capacity.setId(IDGenerator.nextStr());
        capacity.setOrganizationId(currentOrgId);
        capacity.setCapacity(request.getCapacity());
        capacity.setScopeId(JSON.toJSONString(request.getScopeIds()));
        long now = System.currentTimeMillis();
        capacity.setCreateTime(now);
        capacity.setCreateUser(currentUserId);
        capacity.setUpdateTime(now);
        capacity.setUpdateUser(currentUserId);
        clueCapacityMapper.insert(capacity);

        OperationLogContext.setContext(LogContextInfo.builder()
                .modifiedValue(capacity)
                .resourceId(capacity.getId())
                .resourceName(Translator.get("module.clue.capacity.setting"))
                .build());
    }

    @OperationLog(module = LogModule.SYSTEM_MODULE, type = LogType.UPDATE)
    public void update(CapacityUpdateRequest request, String currentUserId, String currentOrgId) {
        ClueCapacity oldCapacity = clueCapacityMapper.selectByPrimaryKey(request.getId());
        if (oldCapacity == null) {
            throw new GenericException(Translator.get("capacity.not.exist"));
        }

        // 克隆原始值用于日志，避免后续修改导致原始值丢失
        ClueCapacity originalValue = new ClueCapacity();
        BeanUtils.copyProperties(oldCapacity, originalValue);

        // 查询除当前记录外，同组织下的其他库容
        List<ClueCapacity> otherCapacities = getByOrgIdExclude(currentOrgId, request.getId());
        List<String> otherScopeIds = flatMapScopeIds(otherCapacities);

        if (userExtendService.hasDuplicateScopeObj(request.getScopeIds(), otherScopeIds, currentOrgId)) {
            throw new GenericException(Translator.get("capacity.scope.duplicate"));
        }

        oldCapacity.setScopeId(JSON.toJSONString(request.getScopeIds()));
        oldCapacity.setCapacity(request.getCapacity());
        oldCapacity.setUpdateTime(System.currentTimeMillis());
        oldCapacity.setUpdateUser(currentUserId);
        extClueCapacityMapper.updateCapacity(oldCapacity);

        // 重新查询作为修改后的值
        ClueCapacity modifiedValue = clueCapacityMapper.selectByPrimaryKey(request.getId());
        OperationLogContext.setContext(
                LogContextInfo.builder()
                        .resourceId(request.getId())
                        .resourceName(Translator.get("module.clue.capacity.setting"))
                        .originalValue(originalValue)
                        .modifiedValue(modifiedValue)
                        .build()
        );
    }

    @OperationLog(module = LogModule.SYSTEM_MODULE, type = LogType.DELETE, resourceId = "{#id}")
    public void delete(String id) {
        clueCapacityMapper.deleteByPrimaryKey(id);
        OperationLogContext.setResourceName(Translator.get("module.clue.capacity.setting"));
    }

    private List<ClueCapacity> getByOrgId(String orgId) {
        LambdaQueryWrapper<ClueCapacity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ClueCapacity::getOrganizationId, orgId);
        return clueCapacityMapper.selectListByLambda(wrapper);
    }

    private List<ClueCapacity> getByOrgIdExclude(String orgId, String excludeId) {
        LambdaQueryWrapper<ClueCapacity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ClueCapacity::getOrganizationId, orgId)
                .nq(ClueCapacity::getId, excludeId);
        return clueCapacityMapper.selectListByLambda(wrapper);
    }

    /**
     * 将所有库容记录中的 scopeId 平铺为字符串列表，且过滤 null 元素
     */
    private List<String> flatMapScopeIds(List<ClueCapacity> capacities) {
        if (CollectionUtils.isEmpty(capacities)) {
            return new ArrayList<>();
        }
        return capacities.stream()
                .flatMap(c -> {
                    List<String> ids = JSON.parseArray(c.getScopeId(), String.class);
                    return ids != null ? ids.stream() : Stream.empty();
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}