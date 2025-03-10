package io.cordys.crm.clue.service;

import io.cordys.common.domain.BaseModuleFieldValue;
import io.cordys.common.dto.DeptDataPermissionDTO;
import io.cordys.common.dto.UserDeptDTO;
import io.cordys.common.exception.GenericException;
import io.cordys.common.service.BaseService;
import io.cordys.common.uid.IDGenerator;
import io.cordys.common.util.BeanUtils;
import io.cordys.crm.clue.constants.ClueResultCode;
import io.cordys.crm.clue.domain.Clue;
import io.cordys.crm.clue.dto.request.ClueAddRequest;
import io.cordys.crm.clue.dto.request.ClueBatchTransferRequest;
import io.cordys.crm.clue.dto.request.CluePageRequest;
import io.cordys.crm.clue.dto.request.ClueUpdateRequest;
import io.cordys.crm.clue.dto.response.ClueGetResponse;
import io.cordys.crm.clue.dto.response.ClueListResponse;
import io.cordys.crm.clue.mapper.ExtClueMapper;
import io.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author jianxing
 * @date 2025-02-08 16:24:22
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class ClueService {

    @Resource
    private BaseMapper<Clue> clueMapper;
    @Resource
    private ExtClueMapper extClueMapper;
    @Resource
    private BaseService baseService;
    @Resource
    private ClueFieldService clueFieldService;

    public List<ClueListResponse> list(CluePageRequest request, String userId, String orgId,
                                           DeptDataPermissionDTO deptDataPermission) {
        List<ClueListResponse> list = extClueMapper.list(request, orgId, userId, deptDataPermission);
        return buildListData(list, orgId);
    }

    private List<ClueListResponse> buildListData(List<ClueListResponse> list, String orgId) {
        List<String> clueIds = list.stream().map(ClueListResponse::getId)
                .collect(Collectors.toList());

        Map<String, List<BaseModuleFieldValue>> caseCustomFiledMap = clueFieldService.getResourceFieldMap(clueIds);

        List<String> ownerIds = list.stream()
                .map(ClueListResponse::getOwner)
                .distinct()
                .collect(Collectors.toList());

        Map<String, UserDeptDTO> userDeptMap = baseService.getUserDeptMapByUserIds(ownerIds, orgId);

        list.forEach(clueListResponse -> {
            // 获取自定义字段
            List<BaseModuleFieldValue> clueFields = caseCustomFiledMap.get(clueListResponse.getId());
            clueListResponse.setModuleFields(clueFields);

            if (clueListResponse.getCollectionTime() != null) {
                // 将毫秒数转换为天数, 并向上取整
                int days = (int) Math.ceil(clueListResponse.getCollectionTime() * 1.0 / 86400000);
                clueListResponse.setReservedDays(days);
            }
            UserDeptDTO userDeptDTO = userDeptMap.get(clueListResponse.getOwner());
            if (userDeptDTO != null) {
                clueListResponse.setDepartmentId(userDeptDTO.getDeptId());
                clueListResponse.setDepartmentName(userDeptDTO.getDeptName());
            }
        });

        return baseService.setCreateAndUpdateUserName(list);
    }

    public ClueGetResponse get(String id) {
        Clue clue = clueMapper.selectByPrimaryKey(id);
        ClueGetResponse clueGetResponse = BeanUtils.copyBean(new ClueGetResponse(), clue);

        // 获取模块字段
        List<BaseModuleFieldValue> clueFields = clueFieldService.getModuleFieldValuesByResourceId(id);

        clueGetResponse.setModuleFields(clueFields);
        return baseService.setCreateAndUpdateUserName(clueGetResponse);
    }

    public Clue add(ClueAddRequest request, String userId, String orgId) {
        Clue clue = BeanUtils.copyBean(new Clue(), request);
        clue.setCreateTime(System.currentTimeMillis());
        clue.setUpdateTime(System.currentTimeMillis());
        clue.setCollectionTime(clue.getCreateTime());
        clue.setUpdateUser(userId);
        clue.setCreateUser(userId);
        clue.setOrganizationId(orgId);
        clue.setId(IDGenerator.nextStr());
        clue.setInSharedPool(false);

        // 校验名称重复
        checkAddExist(clue);

        clueMapper.insert(clue);

        //保存自定义字段
        clueFieldService.saveModuleField(clue.getId(), request.getModuleFields());
        return clue;
    }

    public Clue update(ClueUpdateRequest request, String userId) {
        Clue clue = BeanUtils.copyBean(new Clue(), request);
        clue.setUpdateTime(System.currentTimeMillis());
        clue.setUpdateUser(userId);

        // 校验名称重复
        checkUpdateExist(clue);

        clueMapper.update(clue);

        // 更新模块字段
        updateModuleField(request.getId(), request.getModuleFields());
        return clueMapper.selectByPrimaryKey(clue.getId());
    }

    private void updateModuleField(String clueId, List<BaseModuleFieldValue> moduleFields) {
        if (moduleFields == null) {
            // 如果为 null，则不更新
            return;
        }
        // 先删除
        clueFieldService.deleteByResourceId(clueId);
        // 再保存
        clueFieldService.saveModuleField(clueId, moduleFields);
    }

    private void checkAddExist(Clue clue) {
        if (extClueMapper.checkAddExist(clue)) {
            throw new GenericException(ClueResultCode.CLUE_EXIST);
        }
    }

    private void checkUpdateExist(Clue clue) {
        if (extClueMapper.checkUpdateExist(clue)) {
            throw new GenericException(ClueResultCode.CLUE_EXIST);
        }
    }

    public void delete(String id) {
        // 删除客户
        clueMapper.deleteByPrimaryKey(id);
        // 删除客户模块字段
        clueFieldService.deleteByResourceId(id);
    }

    public void batchTransfer(ClueBatchTransferRequest request) {
        extClueMapper.batchTransfer(request);
    }

    public void batchDelete(List<String> ids) {
        // 删除客户
        clueMapper.deleteByIds(ids);
        // 删除客户模块字段
        clueFieldService.deleteByResourceIds(ids);
    }
}