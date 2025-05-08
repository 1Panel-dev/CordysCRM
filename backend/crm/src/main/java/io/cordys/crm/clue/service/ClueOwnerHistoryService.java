package io.cordys.crm.clue.service;

import io.cordys.common.dto.UserDeptDTO;
import io.cordys.common.service.BaseService;
import io.cordys.common.uid.IDGenerator;
import io.cordys.common.util.BeanUtils;
import io.cordys.crm.clue.domain.Clue;
import io.cordys.crm.clue.domain.ClueOwner;
import io.cordys.crm.clue.dto.request.ClueBatchTransferRequest;
import io.cordys.crm.clue.dto.response.ClueOwnerListResponse;
import io.cordys.crm.clue.mapper.ExtClueOwnerMapper;
import io.cordys.mybatis.BaseMapper;
import io.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * @author jianxing
 * @date 2025-02-08 16:24:22
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class ClueOwnerHistoryService {
    @Resource
    private BaseMapper<ClueOwner> clueOwnerMapper;
    @Resource
    private BaseService baseService;
    @Resource
    private ExtClueOwnerMapper extClueOwnerMapper;

    public List<ClueOwnerListResponse> list(String clueId, String orgId) {
        ClueOwner example = new ClueOwner();
        example.setClueId(clueId);
        List<ClueOwner> owners = clueOwnerMapper.select(example);
        return buildListData(orgId, owners);
    }

    private List<ClueOwnerListResponse> buildListData(String orgId, List<ClueOwner> owners) {
        if (CollectionUtils.isEmpty(owners)) {
            return List.of();
        }
        Set<String> userIds = new HashSet<>();
        Set<String> ownerIds = new HashSet<>();

        for (ClueOwner owner : owners) {
            userIds.add(owner.getOwner());
            userIds.add(owner.getOperator());
            ownerIds.add(owner.getOwner());
        }

        Map<String, UserDeptDTO> userDeptMap = baseService.getUserDeptMapByUserIds(new ArrayList<>(ownerIds), orgId);

        Map<String, String> userNameMap = baseService.getUserNameMap(userIds);

        return owners
                .stream()
                .map(item -> {
                    ClueOwnerListResponse clueOwner =
                            BeanUtils.copyBean(new ClueOwnerListResponse(), item);
                    UserDeptDTO userDeptDTO = userDeptMap.get(clueOwner.getOwner());
                    if (userDeptDTO != null) {
                        clueOwner.setDepartmentId(userDeptDTO.getDeptId());
                        clueOwner.setDepartmentName(userDeptDTO.getDeptName());
                    }
                    clueOwner.setOwnerName(userNameMap.get(clueOwner.getOwner()));
                    clueOwner.setOperatorName(userNameMap.get(clueOwner.getOperator()));
                    return clueOwner;
                }).toList();
    }

    public ClueOwner add(Clue clue, String userId) {
        ClueOwner clueOwner = new ClueOwner();
        clueOwner.setOwner(clue.getOwner());
        clueOwner.setOperator(userId);
        clueOwner.setClueId(clue.getId());
        clueOwner.setCollectionTime(clue.getCollectionTime());
        clueOwner.setEndTime(System.currentTimeMillis());
        clueOwner.setId(IDGenerator.nextStr());
        clueOwnerMapper.insert(clueOwner);
        return clueOwner;
    }

    public void batchAdd(ClueBatchTransferRequest transferRequest, String userId) {
        extClueOwnerMapper.batchAdd(transferRequest, userId);
    }

    public void deleteByClueIds(List<String> clueIds) {
        LambdaQueryWrapper<ClueOwner> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(ClueOwner::getClueId, clueIds);
        clueOwnerMapper.deleteByLambda(wrapper);
    }
}