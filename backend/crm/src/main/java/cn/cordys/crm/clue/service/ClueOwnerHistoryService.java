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
import cn.cordys.crm.system.constants.DictModule;
import cn.cordys.crm.system.domain.Dict;
import cn.cordys.crm.system.domain.DictConfig;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

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
    @Resource
    private BaseMapper<DictConfig> dictConfigMapper;
    @Resource
    private BaseMapper<Dict> dictMapper;

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
        Set<String> reasonIds = new HashSet<>();

        for (ClueOwner owner : owners) {
            userIds.add(owner.getOwner());
            userIds.add(owner.getOperator());
            ownerIds.add(owner.getOwner());
            reasonIds.add(owner.getReasonId());
        }

        LambdaQueryWrapper<DictConfig> configLambdaQueryWrapper = new LambdaQueryWrapper<>();
        configLambdaQueryWrapper.eq(DictConfig::getModule, DictModule.CLUE_POOL_RS.name()).eq(DictConfig::getOrganizationId, orgId);
        List<DictConfig> configs = dictConfigMapper.selectListByLambda(configLambdaQueryWrapper);
        boolean showReason = CollectionUtils.isNotEmpty(configs) && configs.getFirst().getEnabled();

        Map<String, UserDeptDTO> userDeptMap = baseService.getUserDeptMapByUserIds(new ArrayList<>(ownerIds), orgId);

        Map<String, String> userNameMap = baseService.getUserNameMap(userIds);

        List<Dict> dictList = dictMapper.selectByIds(new ArrayList<>(reasonIds));
        Map<String, String> dictNameMap = dictList.stream().collect(Collectors.toMap(Dict::getId, Dict::getName));

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
                    if (!showReason) {
                        clueOwner.setReasonId(null);
                        clueOwner.setReasonName(null);
                    } else {
                        clueOwner.setReasonName(dictNameMap.get(clueOwner.getReasonId()));
                    }
                    clueOwner.setOwnerName(userNameMap.get(clueOwner.getOwner()));
                    clueOwner.setOperatorName(userNameMap.get(clueOwner.getOperator()));
                    return clueOwner;
                }).toList();
    }

    public ClueOwner add(Clue clue, String userId, Boolean addReason) {
        ClueOwner clueOwner = new ClueOwner();
        clueOwner.setOwner(clue.getOwner());
        clueOwner.setOperator(userId);
        clueOwner.setClueId(clue.getId());
        clueOwner.setCollectionTime(clue.getCollectionTime());
        clueOwner.setEndTime(System.currentTimeMillis());
        clueOwner.setId(IDGenerator.nextStr());
        if (addReason && StringUtils.isNotBlank(clue.getReasonId()) && !Strings.CS.equals(clue.getReasonId(), "system")) {
            clueOwner.setReasonId(clue.getReasonId());
        }
        clueOwnerMapper.insert(clueOwner);
        return clueOwner;
    }

    public void batchAdd(ClueBatchTransferRequest transferRequest, String userId) {
        extClueOwnerMapper.batchAdd(transferRequest, userId);
    }

    /**
     * 删除转移写入的那条负责人变更记录
     * <p>
     * 转移时 batchAdd 会抄下转移前的负责人与领取时间写一条记录, 这两个值也正好在编辑前快照里,
     * 因此按它们定位就能精确命中这次转移写入的那条; 取最新一条是为了不误删同一负责人历史上更早的记录。
     *
     * @param clueId         线索ID
     * @param owner          转移前的负责人
     * @param collectionTime 转移前的领取时间
     */
    public void deleteTransferHistory(String clueId, String owner, Long collectionTime) {
        extClueOwnerMapper.deleteTransferHistory(clueId, owner, collectionTime);
    }

    public void deleteByClueIds(List<String> clueIds) {
        LambdaQueryWrapper<ClueOwner> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(ClueOwner::getClueId, clueIds);
        clueOwnerMapper.deleteByLambda(wrapper);
    }
}