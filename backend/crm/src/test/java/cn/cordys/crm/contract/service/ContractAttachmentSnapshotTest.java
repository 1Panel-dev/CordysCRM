package cn.cordys.crm.contract.service;

import cn.cordys.common.constants.FormKey;
import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.util.CommonBeanFactory;
import cn.cordys.common.util.JSON;
import cn.cordys.crm.contract.domain.Contract;
import cn.cordys.crm.contract.domain.ContractSnapshot;
import cn.cordys.crm.contract.dto.request.ContractApprovalSnapshotRequest;
import cn.cordys.crm.contract.dto.request.ContractUpdateRequest;
import cn.cordys.crm.contract.dto.response.ContractGetResponse;
import cn.cordys.crm.contract.mapper.ExtContractMapper;
import cn.cordys.crm.system.dto.field.AttachmentField;
import cn.cordys.crm.system.dto.request.ResourceBatchEditRequest;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.crm.system.service.ModuleFormCacheService;
import cn.cordys.crm.system.service.ModuleFormService;
import cn.cordys.mybatis.BaseMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ContractAttachmentSnapshotTest {

    private ContractService service;
    private ContractFieldService fields;
    private BaseMapper<ContractSnapshot> snapshots;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        service = new ContractService();
        fields = mock(ContractFieldService.class);
        snapshots = mock(BaseMapper.class);
        ReflectionTestUtils.setField(service, "contractFieldService", fields);
        ReflectionTestUtils.setField(service, "snapshotBaseMapper", snapshots);
    }

    @Test
    void attachmentOriginsIncludeServerSnapshotWithoutChangingCurrentFields() {
        List<BaseModuleFieldValue> current = new ArrayList<>(
                List.of(new BaseModuleFieldValue("current-files", List.of("file-current"))));
        ContractGetResponse snapshotValue = new ContractGetResponse();
        snapshotValue.setModuleFields(List.of(new BaseModuleFieldValue("old-files", List.of("file-old"))));
        snapshotValue.setProducts(List.of(Map.of("id", "product-old")));
        ContractSnapshot snapshot = new ContractSnapshot();
        snapshot.setContractValue(JSON.toJSONString(snapshotValue));
        when(snapshots.selectListByLambda(any())).thenReturn(List.of(snapshot));
        when(snapshots.selectOne(any())).thenReturn(snapshot);

        List<BaseModuleFieldValue> origins = ReflectionTestUtils.invokeMethod(service, "getAttachmentOriginFields",
                "contract-1", current);

        assertNotSame(current, origins);
        assertEquals(1, current.size());
        assertEquals(List.of("file-current"), current.getFirst().getFieldValue());
        assertTrue(origins.stream().anyMatch(field -> "old-files".equals(field.getFieldId())
                && List.of("file-old").equals(field.getFieldValue())));
        assertTrue(origins.stream().anyMatch(field -> "products".equals(field.getFieldId())
                && snapshotValue.getProducts().equals(field.getFieldValue())));
    }

    @Test
    @SuppressWarnings("unchecked")
    void preUpdateSnapshotSerializesNormalizedAttachmentValues() {
        BaseMapper<Contract> contracts = mock(BaseMapper.class);
        ModuleFormCacheService formCache = mock(ModuleFormCacheService.class);
        ModuleFormService forms = mock(ModuleFormService.class);
        ReflectionTestUtils.setField(service, "contractMapper", contracts);
        ReflectionTestUtils.setField(service, "moduleFormCacheService", formCache);
        ReflectionTestUtils.setField(service, "moduleFormService", forms);
        Contract contract = new Contract();
        contract.setId("contract-1");
        contract.setOrganizationId("org-1");
        List<BaseModuleFieldValue> storedFields = new ArrayList<>(
                List.of(new BaseModuleFieldValue("files", List.of("file-stale", "file-kept"))));
        ModuleFormConfigDTO config = new ModuleFormConfigDTO();
        config.setFields(List.of());
        when(contracts.selectByPrimaryKey("contract-1")).thenReturn(contract);
        when(fields.getModuleFieldValuesByResourceIdStrict("contract-1")).thenReturn(storedFields);
        when(formCache.getBusinessFormConfig(FormKey.CONTRACT.getKey(), "org-1")).thenReturn(config);
        doAnswer(invocation -> {
            List<BaseModuleFieldValue> values = invocation.getArgument(2);
            List<BaseModuleFieldValue> originals = invocation.getArgument(3);
            assertNotSame(values, originals);
            assertEquals(List.of("file-stale", "file-kept"), originals.getFirst().getFieldValue());
            values.set(0, new BaseModuleFieldValue("files", List.of("file-kept")));
            return null;
        }).when(fields).normalizeAttachmentReferences(eq("org-1"), eq("contract-1"), anyList(), anyList());
        doCallRealMethod().when(forms).processBusinessFieldValues(any(), anyList(), eq(config));

        ContractApprovalSnapshotRequest saved = JSON.parseObject(
                service.getPreUpdateSnapshotData("contract-1", "user-1", "org-1"),
                ContractApprovalSnapshotRequest.class);

        assertEquals(List.of("file-kept"), saved.getModuleFields().getFirst().getFieldValue());
        InOrder order = inOrder(fields, forms);
        order.verify(fields).getModuleFieldValuesByResourceIdStrict("contract-1");
        order.verify(fields).normalizeAttachmentReferences(eq("org-1"), eq("contract-1"), anyList(), anyList());
        order.verify(forms).processBusinessFieldValues(any(), anyList(), eq(config));
    }

    @Test
    void restoreNormalizesTrustedSnapshotBeforeUpdatingContract() {
        ContractService updater = mock(ContractService.class);
        ContractApprovalSnapshotRequest snapshot = new ContractApprovalSnapshotRequest();
        snapshot.setId("contract-1");
        snapshot.setUpdateType("approval");
        snapshot.setModuleFields(List.of(new BaseModuleFieldValue("files", List.of("file-stale", "file-kept"))));
        doAnswer(invocation -> {
            List<BaseModuleFieldValue> values = invocation.getArgument(2);
            List<BaseModuleFieldValue> originals = invocation.getArgument(3);
            assertNotSame(values, originals);
            values.set(0, new BaseModuleFieldValue("files", List.of("file-kept")));
            assertEquals(List.of("file-stale", "file-kept"), originals.getFirst().getFieldValue());
            return null;
        }).when(fields).normalizeAttachmentReferences(eq("org-1"), eq("contract-1"), anyList(), anyList());
        try (MockedStatic<CommonBeanFactory> beans = mockStatic(CommonBeanFactory.class)) {
            beans.when(() -> CommonBeanFactory.getBean(ContractService.class)).thenReturn(updater);

            service.revertToSnapshot("contract-1", "user-1", "org-1", JSON.toJSONString(snapshot));
        }

        ArgumentCaptor<ContractUpdateRequest> restored = ArgumentCaptor.forClass(ContractUpdateRequest.class);
        InOrder order = inOrder(fields, updater);
        order.verify(fields).normalizeAttachmentReferences(eq("org-1"), eq("contract-1"), anyList(), anyList());
        order.verify(updater).update(restored.capture(), eq("user-1"), eq("org-1"));
        assertEquals("contract-1", restored.getValue().getId());
        assertEquals("approval", restored.getValue().getUpdateType());
        assertEquals(List.of("file-kept"), restored.getValue().getModuleFields().getFirst().getFieldValue());
    }

    @Test
    void restoreUpdateFailurePropagatesToKeepApprovalBackup() {
        ContractService updater = mock(ContractService.class);
        IllegalStateException failure = new IllegalStateException("合同恢复失败");
        doThrow(failure).when(updater).update(any(ContractUpdateRequest.class), eq("user-1"), eq("org-1"));
        ContractApprovalSnapshotRequest snapshot = new ContractApprovalSnapshotRequest();
        snapshot.setId("contract-1");
        snapshot.setModuleFields(List.of(new BaseModuleFieldValue("files", List.of("file-old"))));
        try (MockedStatic<CommonBeanFactory> beans = mockStatic(CommonBeanFactory.class)) {
            beans.when(() -> CommonBeanFactory.getBean(ContractService.class)).thenReturn(updater);

            assertSame(failure, assertThrows(IllegalStateException.class,
                    () -> service.revertToSnapshot("contract-1", "user-1", "org-1", JSON.toJSONString(snapshot))));
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void stageAttachmentUpdateAlsoPersistsCleanupOfOtherStoredAttachmentFields() {
        Contract contract = new Contract();
        contract.setId("contract-1");
        contract.setOrganizationId("org-1");
        ModuleFormCacheService formCache = mock(ModuleFormCacheService.class);
        ReflectionTestUtils.setField(service, "moduleFormCacheService", formCache);
        ReflectionTestUtils.setField(service, "extContractMapper", mock(ExtContractMapper.class));
        AttachmentField first = new AttachmentField();
        first.setId("files-a");
        first.setType("ATTACHMENT");
        AttachmentField second = new AttachmentField();
        second.setId("files-b");
        second.setType("ATTACHMENT");
        ModuleFormConfigDTO config = new ModuleFormConfigDTO();
        config.setFields(List.of(first, second));
        when(formCache.getBusinessFormConfig(FormKey.CONTRACT.getKey(), "org-1")).thenReturn(config);
        when(fields.getModuleFieldValuesByResourceIdStrict("contract-1")).thenReturn(new ArrayList<>(
                List.of(new BaseModuleFieldValue("files-b", List.of("file-stale")))));
        doAnswer(invocation -> {
            List<BaseModuleFieldValue> values = invocation.getArgument(2);
            for (int i = 0; i < values.size(); i++) {
                BaseModuleFieldValue value = values.get(i);
                if ("files-b".equals(value.getFieldId()) && List.of("file-stale").equals(value.getFieldValue())) {
                    values.set(i, new BaseModuleFieldValue(value.getFieldId(), List.of()));
                }
            }
            return null;
        }).when(fields).normalizeAttachmentReferences(eq("org-1"), eq("contract-1"), anyList(), anyList());
        List<BaseModuleFieldValue> requested = new ArrayList<>(
                List.of(new BaseModuleFieldValue("files-a", List.of("file-new"))));

        ReflectionTestUtils.invokeMethod(service, "updateFieldAndSnapshot", contract, requested, "user-1");

        ArgumentCaptor<ResourceBatchEditRequest> updates = ArgumentCaptor.forClass(ResourceBatchEditRequest.class);
        verify(fields, times(2)).batchUpdate(updates.capture(), any(), anyList(), eq(Contract.class), anyString(), any(),
                eq("user-1"), eq("org-1"), eq(false));
        assertTrue(updates.getAllValues().stream().anyMatch(update -> "files-a".equals(update.getFieldId())
                && List.of("file-new").equals(update.getFieldValue())));
        assertTrue(updates.getAllValues().stream().anyMatch(update -> "files-b".equals(update.getFieldId())
                && List.of().equals(update.getFieldValue())));
        ArgumentCaptor<Map<String, BaseModuleFieldValue>> synced = ArgumentCaptor.forClass(Map.class);
        verify(fields).processAttachmentFields(eq("org-1"), eq("contract-1"), eq("user-1"), anyList(), synced.capture());
        assertEquals(List.of("file-new"), synced.getValue().get("files-a").getFieldValue());
        assertEquals(List.of(), synced.getValue().get("files-b").getFieldValue());
    }
}
