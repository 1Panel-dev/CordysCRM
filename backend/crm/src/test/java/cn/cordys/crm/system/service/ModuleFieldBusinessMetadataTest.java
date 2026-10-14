package cn.cordys.crm.system.service;

import cn.cordys.common.constants.BusinessModuleField;
import cn.cordys.common.util.JSON;
import cn.cordys.crm.system.constants.FieldSourceType;
import cn.cordys.crm.system.constants.FieldType;
import cn.cordys.crm.system.domain.ModuleField;
import cn.cordys.crm.system.domain.ModuleFieldBlob;
import cn.cordys.crm.system.dto.field.DatasourceField;
import cn.cordys.crm.system.dto.field.InputField;
import cn.cordys.crm.system.dto.field.ProductSubField;
import cn.cordys.crm.system.dto.field.SelectField;
import cn.cordys.crm.system.dto.field.base.BaseField;
import cn.cordys.crm.system.dto.field.base.OptionProp;
import cn.cordys.mybatis.BaseMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ModuleFieldBusinessMetadataTest {
    private static final String UNTRUSTED_KEY = "(1+1)";

    @Test
    void systemFlagCannotBypassBusinessFieldResolution() {
        ModuleFormService service = new ModuleFormService();
        InputField business = input("business");
        business.setInternalKey(BusinessModuleField.OPPORTUNITY_NAME.getKey());
        InputField custom = input("custom");
        custom.setDisabledProps(Set.of("editable"));

        service.setFieldBusinessParam(business);
        service.setFieldBusinessParam(custom);

        assertEquals(BusinessModuleField.OPPORTUNITY_NAME.getBusinessKey(), business.getBusinessKey());
        assertEquals(BusinessModuleField.OPPORTUNITY_NAME.getDisabledProps(), business.getDisabledProps());
        assertFalse(business.isSys());
        assertNull(custom.getBusinessKey());
        assertNull(custom.getDisabledProps());
        assertFalse(custom.isSys());
    }

    @Test
    void saveRebuildsBusinessMetadataBeforeSerialization() {
        Fixture fixture = new Fixture();
        InputField business = input("business");
        business.setInternalKey(BusinessModuleField.OPPORTUNITY_NAME.getKey());
        InputField custom = input("custom");

        fixture.service.saveFields(List.of(business, custom), "form", "user");

        assertEquals("name", fixture.stored("business").getBusinessKey());
        assertFalse(fixture.stored("business").isSys());
        assertNull(fixture.stored("custom").getBusinessKey());
        assertFalse(fixture.stored("custom").isSys());
        assertEquals("name", fixture.service.getAllFields("form").getFirst().getBusinessKey());
    }

    @Test
    void oldBlobsAndSubfieldsDoNotKeepClientBusinessMetadata() {
        Fixture fixture = new Fixture();
        InputField business = input("business");
        business.setInternalKey(BusinessModuleField.OPPORTUNITY_NAME.getKey());
        ProductSubField sub = sub("sub", List.of(input("child")));
        sub.setSys(true);
        sub.setBusinessKey(UNTRUSTED_KEY);
        fixture.seed(List.of(business, sub));

        List<BaseField> restored = fixture.service.getAllFields("form");

        assertEquals("name", restored.getFirst().getBusinessKey());
        ProductSubField restoredSub = (ProductSubField) restored.get(1);
        assertFalse(restoredSub.isSys());
        assertNull(restoredSub.getBusinessKey());
        assertFalse(restoredSub.getSubFields().getFirst().isSys());
        assertNull(restoredSub.getSubFields().getFirst().getBusinessKey());
    }

    @Test
    void saveAndReadPreserveTrustedSystemAndBusinessTitleReferences() {
        Fixture fixture = new Fixture();
        DatasourceField contract = source("contract-source", FieldSourceType.CONTRACT, "approvalStatus");
        SelectField approval = systemReference("contract-source_ref_approvalStatus", contract.getId());
        approval.setName("审批状态展示名称");
        approval.setFieldWidth(0.5f);
        OptionProp option = new OptionProp();
        option.setLabel("已通过");
        option.setValue("APPROVED");
        approval.setOptions(List.of(option));
        DatasourceField title = source("title-source", FieldSourceType.BUSINESS_TITLE, "business_title_name");
        InputField titleName = input("title-source_ref_business_title_name");
        titleName.setResourceFieldId(title.getId());
        titleName.setName("工商抬头展示名称");

        fixture.service.saveFields(List.of(contract, approval, title, titleName), "form", "user");

        DatasourceField storedContract = (DatasourceField) fixture.stored(contract.getId());
        BaseField storedApproval = storedContract.getRefFields().getFirst();
        assertTrue(storedApproval.isSys());
        assertEquals("approvalStatus", storedApproval.getBusinessKey());
        assertEquals("审批状态展示名称", storedApproval.getName());
        assertEquals(0.5f, storedApproval.getFieldWidth());
        assertEquals("APPROVED", ((SelectField) storedApproval).getOptions().getFirst().getValue());
        List<BaseField> restored = fixture.service.getAllFields("form");
        BaseField restoredTitle = ((DatasourceField) restored.get(1)).getRefFields().getFirst();
        assertFalse(restoredTitle.isSys());
        assertEquals("name", restoredTitle.getBusinessKey());
        assertEquals("工商抬头展示名称", restoredTitle.getName());
    }

    @Test
    void subtableReferencesUseTheirActualDataSourceIncludingLegacyIds() {
        Fixture fixture = new Fixture();
        DatasourceField quotation = source("quotation-source", FieldSourceType.QUOTATION, "invalid");
        SelectField invalid = systemReference("invalid", quotation.getId());
        SelectField wrongSource = systemReference("quotation-source_ref_stage", quotation.getId());
        ProductSubField sub = sub("sub", List.of(quotation, invalid, wrongSource));
        fixture.seed(List.of(sub));

        ProductSubField restored = (ProductSubField) fixture.service.getAllFields("form").getFirst();

        assertTrue(restored.getSubFields().get(1).isSys());
        assertEquals("invalid", restored.getSubFields().get(1).getBusinessKey());
        assertFalse(restored.getSubFields().get(2).isSys());
        assertNull(restored.getSubFields().get(2).getBusinessKey());
    }

    @Test
    void flatteningPreservesSystemDisplayPropertiesAndTypedDefinitions() {
        Fixture fixture = new Fixture();
        DatasourceField source = source("contract-source", FieldSourceType.CONTRACT, "approvalStatus");
        SelectField approval = systemReference("contract-source_ref_approvalStatus", source.getId());
        approval.setName("自定义展示名称");
        approval.setFieldWidth(0.75f);
        approval.setPos(1L);
        source.setRefFields(new ArrayList<>(List.of(approval)));
        source.setPos(0L);
        when(fixture.blobMapper.selectByIds(anyList())).thenReturn(List.of());

        BaseField flattened = fixture.service.flattenSourceRefFields(List.of(source), Map.of()).get(1);
        fixture.service.setFieldBusinessParam(flattened);

        assertTrue(flattened.isSys());
        assertEquals("approvalStatus", flattened.getBusinessKey());
        assertEquals("自定义展示名称", flattened.getName());
        assertEquals(0.75f, flattened.getFieldWidth());
        List<BaseField> definitions = fixture.service.initSourceSystemFields(FieldSourceType.CONTRACT);
        assertEquals(List.of("stage", "approvalStatus"), definitions.stream().map(BaseField::getId).toList());
        for (BaseField definition : definitions) {
            BaseField copy = JSON.parseObject(JSON.toJSONString(definition), BaseField.class);
            assertEquals(FieldType.SELECT.name(), copy.getType());
            assertEquals(copy.getId(), copy.getBusinessKey());
            assertTrue(copy.isSys());
        }
    }

    @Test
    void reloadingSubtableReferencesCannotReintroducePollutedOldBlobs() {
        Fixture fixture = new Fixture();
        DatasourceField source = source("customer-source", FieldSourceType.CUSTOMER, "referenced");
        InputField old = input("customer-source_ref_referenced");
        old.setResourceFieldId(source.getId());
        ProductSubField sub = sub("sub", List.of(source, old));
        ModuleFieldBlob polluted = new ModuleFieldBlob();
        polluted.setId("referenced");
        polluted.setProp(JSON.toJSONString(input("referenced")));
        when(fixture.blobMapper.selectByIds(anyList())).thenReturn(List.of(polluted));

        fixture.service.reloadPropOfSubRefFields(sub, Map.of());

        BaseField restored = sub.getSubFields().get(1);
        assertFalse(restored.isSys());
        assertNull(restored.getBusinessKey());
    }

    private static InputField input(String id) {
        InputField field = new InputField();
        field.setId(id);
        field.setType(FieldType.INPUT.name());
        field.setSys(true);
        field.setBusinessKey(UNTRUSTED_KEY);
        return field;
    }

    private static SelectField systemReference(String id, String sourceId) {
        SelectField field = new SelectField();
        field.setId(id);
        field.setResourceFieldId(sourceId);
        field.setType(FieldType.SELECT.name());
        field.setSys(true);
        field.setBusinessKey(UNTRUSTED_KEY);
        return field;
    }

    private static DatasourceField source(String id, FieldSourceType type, String showField) {
        DatasourceField field = new DatasourceField();
        field.setId(id);
        field.setType(FieldType.DATA_SOURCE.name());
        field.setDataSourceType(type.name());
        field.setShowFields(new ArrayList<>(List.of(showField)));
        return field;
    }

    private static ProductSubField sub(String id, List<BaseField> fields) {
        ProductSubField field = new ProductSubField();
        field.setId(id);
        field.setType(FieldType.SUB_PRODUCT.name());
        field.setSubFields(new ArrayList<>(fields));
        return field;
    }

    private static class Fixture {
        private final ModuleFormService service = new ModuleFormService();
        private final List<ModuleField> fields = new ArrayList<>();
        private final List<ModuleFieldBlob> blobs = new ArrayList<>();
        private final BaseMapper<ModuleFieldBlob> blobMapper;

        @SuppressWarnings("unchecked")
        private Fixture() {
            BaseMapper<ModuleField> fieldMapper = mock(BaseMapper.class);
            blobMapper = mock(BaseMapper.class);
            ReflectionTestUtils.setField(service, "moduleFieldMapper", fieldMapper);
            ReflectionTestUtils.setField(service, "moduleFieldBlobMapper", blobMapper);
            doAnswer(invocation -> {
                fields.addAll(invocation.getArgument(0));
                return null;
            }).when(fieldMapper).batchInsert(any());
            doAnswer(invocation -> {
                blobs.addAll(invocation.getArgument(0));
                return null;
            }).when(blobMapper).batchInsert(any());
            when(fieldMapper.selectListByLambda(any())).thenReturn(fields);
            when(blobMapper.selectListByLambda(any())).thenReturn(blobs);
        }

        private void seed(List<BaseField> sourceFields) {
            for (BaseField field : sourceFields) {
                ModuleField stored = new ModuleField();
                stored.setId(field.getId());
                stored.setType(field.getType());
                stored.setInternalKey(field.getInternalKey());
                stored.setPos((long) fields.size());
                fields.add(stored);
                ModuleFieldBlob blob = new ModuleFieldBlob();
                blob.setId(field.getId());
                blob.setProp(JSON.toJSONString(field));
                blobs.add(blob);
            }
        }

        private BaseField stored(String id) {
            return JSON.parseObject(blobs.stream().filter(blob -> blob.getId().equals(id))
                    .findFirst().orElseThrow().getProp(), BaseField.class);
        }
    }
}
