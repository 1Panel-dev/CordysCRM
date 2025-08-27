package cn.cordys.crm.clue.service;

import cn.cordys.common.dto.UserDeptDTO;
import cn.cordys.common.service.BaseService;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.BeanUtils;
import cn.cordys.crm.clue.domain.Clue;
import cn.cordys.crm.clue.domain.ClueOwner;
import cn.cordys.crm.clue.dto.request.ClueBatchTransferRequest;
import cn.cordys.crm.clue.dto.response.ClueOwnerListResponse;
import cn.cordys.crm.clue.mapper.ExtClueOwnerMapper;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
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
        LambdaQueryWrapper<ClueOwner> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ClueOwner::getClueId, clueId);
        wrapper.orderByDesc(ClueOwner::getEndTime);
        List<ClueOwner> owners = clueOwnerMapper.selectListByLambda(wrapper);
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