package cn.cordys.crm.opportunity.service;

import cn.cordys.common.constants.FormKey;
import cn.cordys.common.dto.JsonDifferenceDTO;
import cn.cordys.common.util.CommonBeanFactory;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.opportunity.mapper.ExtOpportunityStageConfigMapper;
import cn.cordys.crm.system.dto.response.ModuleFormConfigDTO;
import cn.cordys.crm.system.service.ModuleFormCacheService;
import cn.cordys.crm.system.service.ModuleFormService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class OpportunityLogServiceTest {

    @ParameterizedTest
    @CsvSource({ ",2026-02-03", "2026-02-03,2026-11-27", "2026-11-27," })
    void formatsActualEndTimeChanges(String oldDate, String newDate) {
        String orgId = "org-id";
        ModuleFormConfigDTO formConfig = new ModuleFormConfigDTO();
        formConfig.setFields(List.of());
        ModuleFormCacheService formCacheService = mock(ModuleFormCacheService.class);
        when(formCacheService.getBusinessFormConfig(FormKey.OPPORTUNITY.getKey(), orgId)).thenReturn(formConfig);
        ModuleFormService formService = mock(ModuleFormService.class);
        when(formService.getOptionMap(eq(formConfig), anyList())).thenReturn(Map.of());
        ExtOpportunityStageConfigMapper stageConfigMapper = mock(ExtOpportunityStageConfigMapper.class);
        when(stageConfigMapper.getStageConfigList(orgId)).thenReturn(List.of());
        OpportunityLogService logService = new OpportunityLogService();
        ReflectionTestUtils.setField(logService, "extOpportunityStageConfigMapper", stageConfigMapper);

        JsonDifferenceDTO difference = new JsonDifferenceDTO();
        difference.setColumn("actualEndTime");
        difference.setOldValue(toTimestamp(oldDate));
        difference.setNewValue(toTimestamp(newDate));

        try (var beans = mockStatic(CommonBeanFactory.class);
                var translator = mockStatic(Translator.class, invocation -> invocation.getArgument(0))) {
            beans.when(() -> CommonBeanFactory.getBean(ModuleFormCacheService.class)).thenReturn(formCacheService);
            beans.when(() -> CommonBeanFactory.getBean(ModuleFormService.class)).thenReturn(formService);

            JsonDifferenceDTO result = logService.handleLogField(List.of(difference), orgId).getFirst();
            assertEquals(oldDate, result.getOldValueName());
            assertEquals(newDate, result.getNewValueName());
        }
    }

    private String toTimestamp(String date) {
        return date == null ? null
                : Long.toString(LocalDate.parse(date).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());
    }
}
