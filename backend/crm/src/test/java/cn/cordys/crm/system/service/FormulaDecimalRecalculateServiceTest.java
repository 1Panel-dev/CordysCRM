package cn.cordys.crm.system.service;

import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.formula.FormulaEngine;
import cn.cordys.common.util.JSON;
import cn.cordys.crm.contract.domain.Contract;
import cn.cordys.crm.contract.domain.ContractField;
import cn.cordys.crm.contract.domain.ContractSnapshot;
import cn.cordys.crm.contract.dto.response.ContractGetResponse;
import cn.cordys.crm.system.dto.field.FormulaField;
import cn.cordys.crm.system.dto.field.base.SubField;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.mybatis.BaseMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FormulaDecimalRecalculateServiceTest {

    @SuppressWarnings("unchecked")
    @Test
    void zeroContractAmountIsRestoredFromLegacyProductAmount() {
        FormulaDecimalRecalculateService service = new FormulaDecimalRecalculateService();
        ModuleFormCacheService moduleFormCacheService = mock(ModuleFormCacheService.class);
        ModuleFormService moduleFormService = mock(ModuleFormService.class);
        BaseMapper<Contract> contractMapper = mock(BaseMapper.class);
        BaseMapper<ContractField> contractFieldMapper = mock(BaseMapper.class);
        BaseMapper<ContractSnapshot> contractSnapshotMapper = mock(BaseMapper.class);
        ReflectionTestUtils.setField(service, "moduleFormCacheService", moduleFormCacheService);
        ReflectionTestUtils.setField(service, "moduleFormService", moduleFormService);
        ReflectionTestUtils.setField(service, "formulaEngine", new FormulaEngine());
        ReflectionTestUtils.setField(service, "contractMapper", contractMapper);
        ReflectionTestUtils.setField(service, "contractFieldMapper", contractFieldMapper);
        ReflectionTestUtils.setField(service, "contractSnapshotMapper", contractSnapshotMapper);

        Contract contract = new Contract();
        contract.setId("contract-1");
        contract.setAmount(BigDecimal.ZERO);
        contract.setOrganizationId("org-1");

        ContractField legacyProductAmount = new ContractField();
        legacyProductAmount.setResourceId("contract-1");
        legacyProductAmount.setRefSubId("products");
        legacyProductAmount.setFieldId("sumAmount");
        legacyProductAmount.setFieldValue("30000.126");

        FormulaField productAmountField = formulaField("product-amount", "contractProductSumAmount", 2);
        FormulaField totalAmountField = formulaField("total-amount", "contractTotalAmount", 2);
        SubField productsField = new SubField();
        productsField.setId("products");
        productsField.setInternalKey("contractProducts");
        productsField.setType("SUB_PRODUCT");
        productsField.setSubFields(List.of(productAmountField));
        ModuleFormConfigDTO formConfig = new ModuleFormConfigDTO();
        formConfig.setFields(List.of(productsField, totalAmountField));

        when(moduleFormCacheService.getBusinessFormConfig("contract", "org-1")).thenReturn(formConfig);
        when(moduleFormService.flattenFormAllFields(any())).thenReturn(List.of(productAmountField, totalAmountField));
        when(contractMapper.selectListByLambda(any())).thenReturn(List.of(contract));
        when(contractMapper.selectByPrimaryKey("contract-1")).thenReturn(contract);
        when(contractFieldMapper.selectListByLambda(any())).thenReturn(List.of(legacyProductAmount));

        ContractGetResponse snapshotValue = new ContractGetResponse();
        snapshotValue.setAmount(new BigDecimal("30000.126"));
        snapshotValue.setModuleFields(List.of());
        snapshotValue.setProducts(List.of(new HashMap<>(Map.of("sumAmount", "30000.126"))));
        ContractSnapshot snapshot = new ContractSnapshot();
        snapshot.setId("snapshot-1");
        snapshot.setContractId("contract-1");
        snapshot.setContractValue(JSON.toJSONString(snapshotValue));
        when(contractSnapshotMapper.selectListByLambda(any())).thenReturn(List.of(snapshot));

        FormulaDecimalRecalculateService.RecalculateResult result = service.recalculateContract("org-1");

        assertEquals(1, result.getOriginalFields());
        assertEquals(1, result.getBusinessAmounts());
        ArgumentCaptor<Contract> updateCaptor = ArgumentCaptor.forClass(Contract.class);
        verify(contractMapper).update(updateCaptor.capture());
        assertEquals(0, new BigDecimal("30000.13").compareTo(updateCaptor.getValue().getAmount()));

        verify(contractSnapshotMapper).update(snapshot);
        ContractGetResponse updatedSnapshot = JSON.parseObject(snapshot.getContractValue(), ContractGetResponse.class);
        assertEquals("30000.13", updatedSnapshot.getProducts().getFirst().get("sumAmount"));
        assertEquals(0, new BigDecimal("30000.13").compareTo(updatedSnapshot.getAmount()));
    }

    @SuppressWarnings("unchecked")
    @Test
    void contractAmountUsesSubFieldReferencedByConfiguredFormula() {
        FormulaDecimalRecalculateService service = new FormulaDecimalRecalculateService();
        ModuleFormCacheService moduleFormCacheService = mock(ModuleFormCacheService.class);
        ModuleFormService moduleFormService = mock(ModuleFormService.class);
        BaseMapper<Contract> contractMapper = mock(BaseMapper.class);
        BaseMapper<ContractField> contractFieldMapper = mock(BaseMapper.class);
        BaseMapper<ContractSnapshot> contractSnapshotMapper = mock(BaseMapper.class);
        ReflectionTestUtils.setField(service, "moduleFormCacheService", moduleFormCacheService);
        ReflectionTestUtils.setField(service, "moduleFormService", moduleFormService);
        ReflectionTestUtils.setField(service, "formulaEngine", new FormulaEngine());
        ReflectionTestUtils.setField(service, "contractMapper", contractMapper);
        ReflectionTestUtils.setField(service, "contractFieldMapper", contractFieldMapper);
        ReflectionTestUtils.setField(service, "contractSnapshotMapper", contractSnapshotMapper);

        Contract contract = new Contract();
        contract.setId("contract-1");
        contract.setAmount(BigDecimal.ZERO);
        contract.setOrganizationId("org-1");

        ContractField finalDealAmount = new ContractField();
        finalDealAmount.setResourceId("contract-1");
        finalDealAmount.setRefSubId("subscriptions");
        finalDealAmount.setFieldId("final-deal-amount");
        finalDealAmount.setFieldValue("30000");
        finalDealAmount.setBizId("row-1");

        ContractField defaultAmount = new ContractField();
        defaultAmount.setResourceId("contract-1");
        defaultAmount.setRefSubId("products");
        defaultAmount.setFieldId("default-amount");
        defaultAmount.setFieldValue("10000");
        defaultAmount.setBizId("row-2");

        FormulaField defaultAmountField = formulaField("default-amount", "contractProductSumAmount", 2);
        FormulaField finalDealAmountField = formulaField("final-deal-amount", "finalDealAmount", 2);
        FormulaField totalAmountField = formulaField("total-amount", "contractTotalAmount", 2);
        totalAmountField.setFormula(sumFormula("products.default-amount", "subscriptions.final-deal-amount"));
        SubField productsField = new SubField();
        productsField.setId("products");
        productsField.setInternalKey("contractProducts");
        productsField.setType("SUB_PRODUCT");
        productsField.setSubFields(List.of(defaultAmountField));
        SubField subscriptionsField = new SubField();
        subscriptionsField.setId("subscriptions");
        subscriptionsField.setInternalKey("subscriptions");
        subscriptionsField.setType("SUB_PRODUCT");
        subscriptionsField.setSubFields(List.of(finalDealAmountField));
        ModuleFormConfigDTO formConfig = new ModuleFormConfigDTO();
        formConfig.setFields(List.of(productsField, subscriptionsField, totalAmountField));

        when(moduleFormCacheService.getBusinessFormConfig("contract", "org-1")).thenReturn(formConfig);
        when(moduleFormService.flattenFormAllFields(any()))
                .thenReturn(List.of(defaultAmountField, finalDealAmountField, totalAmountField));
        when(contractMapper.selectListByLambda(any())).thenReturn(List.of(contract));
        when(contractMapper.selectByPrimaryKey("contract-1")).thenReturn(contract);
        when(contractFieldMapper.selectListByLambda(any())).thenReturn(List.of(defaultAmount, finalDealAmount));

        ContractGetResponse snapshotValue = new ContractGetResponse();
        snapshotValue.setAmount(BigDecimal.ZERO);
        snapshotValue.setProducts(List.of(new HashMap<>(Map.of("default-amount", "10000"))));
        snapshotValue.setModuleFields(List.of(new BaseModuleFieldValue("subscriptions",
                List.of(new HashMap<>(Map.of("final-deal-amount", "30000"))))));
        ContractSnapshot snapshot = new ContractSnapshot();
        snapshot.setId("snapshot-1");
        snapshot.setContractId("contract-1");
        snapshot.setContractValue(JSON.toJSONString(snapshotValue));
        when(contractSnapshotMapper.selectListByLambda(any())).thenReturn(List.of(snapshot));

        FormulaDecimalRecalculateService.RecalculateResult result = service.recalculateContract("org-1");

        assertEquals(1, result.getBusinessAmounts());
        ArgumentCaptor<Contract> updateCaptor = ArgumentCaptor.forClass(Contract.class);
        verify(contractMapper).update(updateCaptor.capture());
        assertEquals(0, new BigDecimal("40000.00").compareTo(updateCaptor.getValue().getAmount()));

        verify(contractSnapshotMapper).update(snapshot);
        ContractGetResponse updatedSnapshot = JSON.parseObject(snapshot.getContractValue(), ContractGetResponse.class);
        assertEquals(0, new BigDecimal("40000.00").compareTo(updatedSnapshot.getAmount()));
    }

    private FormulaField formulaField(String id, String internalKey, int precision) {
        FormulaField field = new FormulaField();
        field.setId(id);
        field.setInternalKey(internalKey);
        field.setType("FORMULA");
        field.setFormulaResultFormat("number");
        field.setDecimalPlaces(true);
        field.setPrecision(precision);
        return field;
    }

    private String sumFormula(String... fieldIds) {
        return JSON.toJSONString(Map.of("ir", Map.of(
                "type", "function",
                "name", "SUM",
                "args", java.util.Arrays.stream(fieldIds)
                        .map(fieldId -> Map.of("type", "field", "fieldId", fieldId))
                        .toList()
        )));
    }
}
