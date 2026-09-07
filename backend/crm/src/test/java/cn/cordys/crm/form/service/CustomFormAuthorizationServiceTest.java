package cn.cordys.crm.form.service;

import cn.cordys.common.constants.InternalUser;
import cn.cordys.common.exception.GenericException;
import cn.cordys.crm.form.domain.CustomForm;
import cn.cordys.crm.form.domain.CustomFormAdmin;
import cn.cordys.crm.form.domain.CustomFormRole;
import cn.cordys.crm.form.domain.CustomFormRoleUser;
import cn.cordys.crm.form.dto.request.CustomFormAddRequest;
import cn.cordys.crm.form.dto.request.CustomFormAdminBatchRequest;
import cn.cordys.crm.form.dto.request.CustomFormRoleUserBatchRequest;
import cn.cordys.crm.form.dto.response.CustomFormListResponse;
import cn.cordys.crm.form.mapper.ExtCustomFormMapper;
import cn.cordys.crm.system.dto.form.FormProp;
import cn.cordys.crm.system.service.ModuleFormService;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CustomFormAuthorizationServiceTest {

    @Test
    @SuppressWarnings("unchecked")
    void 管理员与跨组织角色用户的目录和选项均限制当前组织() {
        for (String userId : List.of(InternalUser.ADMIN.getValue(), "cross-org-user")) {
            BaseMapper<CustomForm> formMapper = mock(BaseMapper.class);
            BaseMapper<CustomFormAdmin> adminMapper = mock(BaseMapper.class);
            BaseMapper<CustomFormRole> roleMapper = mock(BaseMapper.class);
            BaseMapper<CustomFormRoleUser> roleUserMapper = mock(BaseMapper.class);
            CustomFormService service = new CustomFormService();
            ReflectionTestUtils.setField(service, "customFormMapper", formMapper);
            ReflectionTestUtils.setField(service, "customFormAdminMapper", adminMapper);
            ReflectionTestUtils.setField(service, "customFormRoleMapper", roleMapper);
            ReflectionTestUtils.setField(service, "customFormRoleUserMapper", roleUserMapper);
            CustomForm enabled = form("enabled", "org-1");
            enabled.setEnable(true);
            CustomForm disabled = form("disabled", "org-1");
            disabled.setEnable(false);
            CustomForm foreign = form("foreign", "org-2");
            foreign.setEnable(true);
            List<CustomForm> allForms = List.of(enabled, disabled, foreign);
            when(formMapper.selectListByLambda(any())).thenAnswer(invocation -> {
                LambdaQueryWrapper<CustomForm> query = invocation.getArgument(0);
                Map<String, Object> params = query.getParams();
                assertEquals("org-1", params.get("organization_id_eq"));
                assertTrue(query.getWhereClause().contains("organization_id ="));
                return allForms.stream().filter(form -> Objects.equals(form.getOrganizationId(), params.get("organization_id_eq")))
                        .filter(form -> !(params.get("array_id") instanceof List<?> ids) || ids.contains(form.getId())).toList();
            });
            CustomFormAdmin localAdmin = new CustomFormAdmin();
            localAdmin.setCustomFormId("disabled");
            CustomFormAdmin foreignAdmin = new CustomFormAdmin();
            foreignAdmin.setCustomFormId("foreign");
            when(adminMapper.selectListByLambda(any())).thenReturn(List.of(localAdmin, foreignAdmin));
            CustomFormRoleUser membership = new CustomFormRoleUser();
            membership.setRoleId("role");
            when(roleUserMapper.selectListByLambda(any())).thenReturn(List.of(membership));
            CustomFormRole localRole = new CustomFormRole();
            localRole.setCustomFormId("enabled");
            localRole.setInternalKey("MANAGE_OWN");
            CustomFormRole foreignRole = new CustomFormRole();
            foreignRole.setCustomFormId("foreign");
            foreignRole.setInternalKey("MANAGE_OWN");
            when(roleMapper.selectListByLambda(any())).thenReturn(List.of(localRole, foreignRole));

            List<CustomFormListResponse> result = service.list(userId, "org-1");
            assertEquals(List.of("enabled", "disabled"), result.stream().map(CustomFormListResponse::getId).toList());
            assertTrue(result.stream().allMatch(form -> Boolean.TRUE.equals(form.getHasCreateDataPermission())));
            assertEquals(List.of("enabled"), service.getOptions(userId, "org-1").stream().map(option -> option.getId()).toList());
            verify(formMapper, never()).selectAll(any());
            verify(formMapper, never()).selectByIds(anyList());
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void 目录缺少组织或身份时不能让查询器省略安全条件() {
        BaseMapper<CustomForm> formMapper = mock(BaseMapper.class);
        CustomFormService service = new CustomFormService();
        ReflectionTestUtils.setField(service, "customFormMapper", formMapper);
        assertThrows(GenericException.class, () -> service.list(InternalUser.ADMIN.getValue(), ""));
        assertThrows(GenericException.class, () -> service.getOptions(null, "org-1"));
        verifyNoInteractions(formMapper);
    }

    @Test
    @SuppressWarnings("unchecked")
    void 创建名称唯一性应复用现有组织内校验而非全局重名查询() {
        BaseMapper<CustomForm> formMapper = mock(BaseMapper.class);
        ExtCustomFormMapper extMapper = mock(ExtCustomFormMapper.class);
        CustomFormService service = new CustomFormService();
        ReflectionTestUtils.setField(service, "customFormMapper", formMapper);
        ReflectionTestUtils.setField(service, "extCustomFormMapper", extMapper);
        for (String mapper : List.of("moduleFormMapper", "moduleFormBlobMapper", "customFormRoleMapper", "customFormAdminMapper")) {
            ReflectionTestUtils.setField(service, mapper, mock(BaseMapper.class));
        }
        ReflectionTestUtils.setField(service, "moduleFormService", mock(ModuleFormService.class));
        when(extMapper.checkAddExist(any())).thenAnswer(invocation -> {
            CustomForm target = invocation.getArgument(0);
            assertEquals("org-1", target.getOrganizationId());
            assertEquals("跨组织同名表单", target.getName());
            return false;
        });
        CustomFormAddRequest request = new CustomFormAddRequest();
        request.setName("跨组织同名表单");
        request.setFormProp(new FormProp());
        request.setFields(List.of());
        assertEquals("org-1", service.create(request, "user", "org-1").getOrganizationId());
        verify(extMapper).checkAddExist(any());
        verify(formMapper, never()).selectListByLambda(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void updateEnableRejectsOtherOrganizationBeforeMutation() {
        BaseMapper<CustomForm> formMapper = mock(BaseMapper.class);
        BaseMapper<CustomFormAdmin> adminMapper = mock(BaseMapper.class);
        CustomFormService service = new CustomFormService();
        ReflectionTestUtils.setField(service, "customFormMapper", formMapper);
        ReflectionTestUtils.setField(service, "customFormAdminMapper", adminMapper);
        when(formMapper.selectByPrimaryKey("form-1"))
                .thenReturn(form("form-1", "org-2"));

        assertThrows(GenericException.class,
                () -> service.updateEnable("form-1", InternalUser.ADMIN.getValue(), "org-1", true));

        verify(formMapper, never()).update(any());
        verifyNoInteractions(adminMapper);
    }

    @Test
    @SuppressWarnings("unchecked")
    void setAdminsRejectsOtherOrganizationBeforeMutation() {
        BaseMapper<CustomForm> formMapper = mock(BaseMapper.class);
        BaseMapper<CustomFormAdmin> adminMapper = mock(BaseMapper.class);
        CustomFormService service = new CustomFormService();
        ReflectionTestUtils.setField(service, "customFormMapper", formMapper);
        ReflectionTestUtils.setField(service, "customFormAdminMapper", adminMapper);
        when(formMapper.selectByPrimaryKey("form-1"))
                .thenReturn(form("form-1", "org-2"));

        CustomFormAdminBatchRequest request = new CustomFormAdminBatchRequest();
        request.setCustomFormId("form-1");
        request.setUserIds(List.of("user-1"));

        assertThrows(GenericException.class,
                () -> service.setAdmins(request, InternalUser.ADMIN.getValue(), "org-1"));

        verify(adminMapper, never()).deleteByLambda(any());
        verify(adminMapper, never()).batchInsert(anyList());
    }

    @Test
    @SuppressWarnings("unchecked")
    void addRoleUsersRejectsOtherOrganizationBeforeMutation() {
        BaseMapper<CustomForm> formMapper = mock(BaseMapper.class);
        BaseMapper<CustomFormRole> roleMapper = mock(BaseMapper.class);
        BaseMapper<CustomFormRoleUser> roleUserMapper = mock(BaseMapper.class);
        CustomFormService authorizationService = new CustomFormService();
        ReflectionTestUtils.setField(authorizationService, "customFormMapper", formMapper);

        CustomFormRoleService roleService = new CustomFormRoleService();
        ReflectionTestUtils.setField(roleService, "customFormRoleMapper", roleMapper);
        ReflectionTestUtils.setField(roleService, "customFormRoleUserMapper", roleUserMapper);
        ReflectionTestUtils.setField(roleService, "customFormService", authorizationService);

        CustomFormRole role = new CustomFormRole();
        role.setId("role-1");
        role.setCustomFormId("form-1");
        when(roleMapper.selectByPrimaryKey("role-1")).thenReturn(role);
        when(formMapper.selectByPrimaryKey("form-1"))
                .thenReturn(form("form-1", "org-2"));

        CustomFormRoleUserBatchRequest request = new CustomFormRoleUserBatchRequest();
        request.setCustomFormRoleId("role-1");
        request.setUserIds(List.of("user-1"));

        assertThrows(GenericException.class,
                () -> roleService.addUsers(request, InternalUser.ADMIN.getValue(), "org-1"));

        verify(roleUserMapper, never()).batchInsert(anyList());
    }

    private CustomForm form(String id, String orgId) {
        CustomForm form = new CustomForm();
        form.setId(id);
        form.setOrganizationId(orgId);
        return form;
    }
}
