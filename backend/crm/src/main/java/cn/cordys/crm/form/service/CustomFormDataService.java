package cn.cordys.crm.form.service;

import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.pager.PageUtils;
import cn.cordys.common.pager.Pager;
import cn.cordys.common.response.result.CrmHttpResultCode;
import cn.cordys.common.service.BaseService;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.BeanUtils;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.form.domain.CustomFormAdmin;
import cn.cordys.crm.form.domain.CustomFormData;
import cn.cordys.crm.form.domain.CustomFormRole;
import cn.cordys.crm.form.domain.CustomFormRoleKey;
import cn.cordys.crm.form.domain.CustomFormRoleUser;
import cn.cordys.crm.form.dto.request.CustomFormDataAddRequest;
import cn.cordys.crm.form.dto.request.CustomFormDataBatchUpdateRequest;
import cn.cordys.crm.form.dto.request.CustomFormDataPageRequest;
import cn.cordys.crm.form.dto.request.CustomFormDataUpdateRequest;
import cn.cordys.crm.form.dto.response.CustomFormDataGetResponse;
import cn.cordys.crm.form.dto.response.CustomFormDataListResponse;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(rollbackFor = Exception.class)
public class CustomFormDataService {

    @Resource
    private BaseMapper<CustomFormData> customFormDataMapper;
    @Resource
    private BaseMapper<CustomFormAdmin> customFormAdminMapper;
    @Resource
    private BaseMapper<CustomFormRole> customFormRoleMapper;
    @Resource
    private BaseMapper<CustomFormRoleUser> customFormRoleUserMapper;
    @Resource
    private CustomFormDataFieldService customFormDataFieldService;
    @Resource
    private BaseService baseService;

    public Pager<List<CustomFormDataListResponse>> page(CustomFormDataPageRequest request, String userId, String orgId) {
        String formId = request.getCustomFormId();
        CustomFormRoleKey dataScope = getDataScope(formId, userId);

        LambdaQueryWrapper<CustomFormData> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomFormData::getCustomFormId, formId);

        if (dataScope == CustomFormRoleKey.MANAGE_OWN) {
            wrapper.eq(CustomFormData::getCreateUser, userId);
        }

        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize());
        List<CustomFormData> dataList = customFormDataMapper.selectListByLambda(wrapper);

        if (CollectionUtils.isEmpty(dataList)) {
            return PageUtils.setPageInfo(page, Collections.emptyList());
        }

        List<String> dataIds = dataList.stream().map(CustomFormData::getId).toList();
        Map<String, List<BaseModuleFieldValue>> fieldMap = customFormDataFieldService.getResourceFieldMap(dataIds, true);

        Map<String, String> userNameMap = baseService.getUserNameMap(
                dataList.stream().map(CustomFormData::getOwner).filter(Objects::nonNull).distinct().toList()
        );

        List<CustomFormDataListResponse> respList = dataList.stream().map(data -> {
            CustomFormDataListResponse resp = BeanUtils.copyBean(new CustomFormDataListResponse(), data);
            resp.setModuleFields(fieldMap.get(data.getId()));
            resp.setOwnerName(userNameMap.get(data.getOwner()));
            return resp;
        }).toList();

        return PageUtils.setPageInfo(page, respList);
    }

    public CustomFormDataGetResponse get(String id, String userId) {
        CustomFormData data = customFormDataMapper.selectByPrimaryKey(id);
        if (data == null) {
            throw new GenericException(Translator.get("custom.form.data.not.exist"));
        }

        CustomFormRoleKey dataScope = getDataScope(data.getCustomFormId(), userId);
        if (dataScope == CustomFormRoleKey.MANAGE_OWN && !StringUtils.equals(data.getCreateUser(), userId)) {
            throw new GenericException(CrmHttpResultCode.FORBIDDEN);
        }

        CustomFormDataGetResponse resp = BeanUtils.copyBean(new CustomFormDataGetResponse(), data);

        Map<String, String> userNameMap = baseService.getUserNameMap(
                List.of(data.getOwner(), data.getCreateUser(), data.getUpdateUser())
        );
        resp.setOwnerName(userNameMap.get(data.getOwner()));
        resp.setCreateUserName(userNameMap.get(data.getCreateUser()));
        resp.setUpdateUserName(userNameMap.get(data.getUpdateUser()));

        List<BaseModuleFieldValue> moduleFields = customFormDataFieldService.getModuleFieldValuesByResourceId(id);
        resp.setModuleFields(moduleFields);

        return resp;
    }

    public CustomFormData add(CustomFormDataAddRequest request, String userId, String orgId) {
        CustomFormData data = new CustomFormData();
        data.setId(IDGenerator.nextStr());
        data.setCustomFormId(request.getCustomFormId());
        data.setName(request.getName());
        data.setOwner(StringUtils.isNotBlank(request.getOwner()) ? request.getOwner() : userId);
        data.setOrganizationId(orgId);
        data.setCreateTime(System.currentTimeMillis());
        data.setUpdateTime(System.currentTimeMillis());
        data.setCreateUser(userId);
        data.setUpdateUser(userId);

        customFormDataFieldService.saveModuleField(data, orgId, userId, request.getModuleFields(), false);
        customFormDataMapper.insert(data);

        return data;
    }

    public void update(CustomFormDataUpdateRequest request, String userId, String orgId) {
        CustomFormData data = customFormDataMapper.selectByPrimaryKey(request.getId());
        if (data == null) {
            throw new GenericException(Translator.get("custom.form.data.not.exist"));
        }

        CustomFormRoleKey dataScope = getDataScope(data.getCustomFormId(), userId);
        checkWritePermission(dataScope, data.getCreateUser(), userId);

        CustomFormData updateData = new CustomFormData();
        updateData.setId(request.getId());
        updateData.setName(request.getName());
        updateData.setOwner(request.getOwner());
        updateData.setUpdateTime(System.currentTimeMillis());
        updateData.setUpdateUser(userId);
        customFormDataMapper.update(updateData);

        customFormDataFieldService.deleteByResourceId(request.getId());
        customFormDataFieldService.saveModuleField(updateData, orgId, userId, request.getModuleFields(), true);
    }

    public void delete(String id, String userId) {
        CustomFormData data = customFormDataMapper.selectByPrimaryKey(id);
        if (data == null) {
            throw new GenericException(Translator.get("custom.form.data.not.exist"));
        }

        CustomFormRoleKey dataScope = getDataScope(data.getCustomFormId(), userId);
        checkWritePermission(dataScope, data.getCreateUser(), userId);

        customFormDataFieldService.deleteByResourceId(id);
        customFormDataMapper.deleteByIds(List.of(id));
    }

    public void batchUpdate(CustomFormDataBatchUpdateRequest request, String userId, String orgId) {
        List<CustomFormData> dataList = customFormDataMapper.selectByIds(request.getIds());
        if (CollectionUtils.isEmpty(dataList)) {
            return;
        }

        String formId = request.getCustomFormId();
        CustomFormRoleKey dataScope = getDataScope(formId, userId);
        if (dataScope == CustomFormRoleKey.VIEW_ALL) {
            throw new GenericException(CrmHttpResultCode.FORBIDDEN);
        }

        for (CustomFormData data : dataList) {
            if (dataScope == CustomFormRoleKey.MANAGE_OWN && !StringUtils.equals(data.getCreateUser(), userId)) {
                continue;
            }

            CustomFormData updateData = new CustomFormData();
            updateData.setId(data.getId());
            updateData.setName(request.getName());
            updateData.setOwner(request.getOwner());
            updateData.setUpdateTime(System.currentTimeMillis());
            updateData.setUpdateUser(userId);
            customFormDataMapper.update(updateData);

            customFormDataFieldService.deleteByResourceId(data.getId());
            customFormDataFieldService.saveModuleField(updateData, orgId, userId, request.getModuleFields(), true);
        }
    }

    public void batchDelete(List<String> ids, String userId) {
        List<CustomFormData> dataList = customFormDataMapper.selectByIds(ids);
        if (CollectionUtils.isEmpty(dataList)) {
            return;
        }

        String formId = dataList.getFirst().getCustomFormId();
        CustomFormRoleKey dataScope = getDataScope(formId, userId);
        if (dataScope == CustomFormRoleKey.VIEW_ALL) {
            throw new GenericException(CrmHttpResultCode.FORBIDDEN);
        }

        List<String> deletableIds = dataList.stream()
                .filter(data -> {
                    if (dataScope == CustomFormRoleKey.MANAGE_ALL) {
                        return true;
                    }
                    return dataScope == CustomFormRoleKey.MANAGE_OWN && StringUtils.equals(data.getCreateUser(), userId);
                })
                .map(CustomFormData::getId)
                .toList();

        if (CollectionUtils.isNotEmpty(deletableIds)) {
            deletableIds.forEach(id -> customFormDataFieldService.deleteByResourceId(id));
            customFormDataMapper.deleteByIds(deletableIds);
        }
    }

    // --- Permission helpers ---

    private void checkWritePermission(CustomFormRoleKey dataScope, String dataCreateUser, String currentUserId) {
        if (dataScope == CustomFormRoleKey.VIEW_ALL) {
            throw new GenericException(CrmHttpResultCode.FORBIDDEN);
        }
        if (dataScope == CustomFormRoleKey.MANAGE_OWN && !StringUtils.equals(dataCreateUser, currentUserId)) {
            throw new GenericException(CrmHttpResultCode.FORBIDDEN);
        }
    }

    CustomFormRoleKey getDataScope(String formId, String userId) {
        // check if admin
        LambdaQueryWrapper<CustomFormAdmin> adminWrapper = new LambdaQueryWrapper<>();
        adminWrapper.eq(CustomFormAdmin::getCustomFormId, formId).eq(CustomFormAdmin::getUserId, userId);
        if (!customFormAdminMapper.selectListByLambda(adminWrapper).isEmpty()) {
            return CustomFormRoleKey.MANAGE_ALL;
        }

        // check role membership
        LambdaQueryWrapper<CustomFormRole> roleWrapper = new LambdaQueryWrapper<>();
        roleWrapper.eq(CustomFormRole::getCustomFormId, formId);
        List<CustomFormRole> roles = customFormRoleMapper.selectListByLambda(roleWrapper);
        if (CollectionUtils.isEmpty(roles)) {
            throw new GenericException(CrmHttpResultCode.FORBIDDEN);
        }

        Map<String, CustomFormRoleKey> roleKeyMap = roles.stream()
                .collect(Collectors.toMap(CustomFormRole::getId, r -> {
                    for (CustomFormRoleKey key : CustomFormRoleKey.values()) {
                        if (key.getKey().equals(r.getInternalKey())) {
                            return key;
                        }
                    }
                    return null;
                }));
        List<String> roleIds = roles.stream().map(CustomFormRole::getId).toList();

        LambdaQueryWrapper<CustomFormRoleUser> ruWrapper = new LambdaQueryWrapper<>();
        ruWrapper.in(CustomFormRoleUser::getRoleId, roleIds).eq(CustomFormRoleUser::getUserId, userId);
        List<CustomFormRoleUser> roleUsers = customFormRoleUserMapper.selectListByLambda(ruWrapper);

        if (CollectionUtils.isEmpty(roleUsers)) {
            throw new GenericException(CrmHttpResultCode.FORBIDDEN);
        }

        Set<CustomFormRoleKey> userRoleKeys = roleUsers.stream()
                .map(ru -> roleKeyMap.get(ru.getRoleId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (userRoleKeys.contains(CustomFormRoleKey.MANAGE_ALL)) {
            return CustomFormRoleKey.MANAGE_ALL;
        }
        if (userRoleKeys.contains(CustomFormRoleKey.VIEW_ALL)) {
            return CustomFormRoleKey.VIEW_ALL;
        }
        if (userRoleKeys.contains(CustomFormRoleKey.MANAGE_OWN)) {
            return CustomFormRoleKey.MANAGE_OWN;
        }

        throw new GenericException(CrmHttpResultCode.FORBIDDEN);
    }
}
