package cn.cordys.crm.system.service;

import cn.cordys.common.dto.JsonDifferenceDTO;
import cn.cordys.common.util.JSON;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.system.domain.OrganizationUser;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mockStatic;

class OrganizationLogServiceTest {

    @ParameterizedTest
    @CsvSource({ "false,true,log.enable.false,log.enable.true", "true,false,log.enable.true,log.enable.false",
            ",true,,log.enable.true", "true,,log.enable.true," })
    void translatesEmployeeStatusChanges(Boolean oldStatus, Boolean newStatus, String oldLabel, String newLabel)
            throws Exception {
        OrganizationUser oldUser = new OrganizationUser();
        oldUser.setEnable(oldStatus);
        OrganizationUser newUser = new OrganizationUser();
        newUser.setEnable(newStatus);
        List<JsonDifferenceDTO> differences = new SysOperationLogService().getJsonDifferences(
                JSON.toJSONString(oldUser), JSON.toJSONString(newUser));

        try (var translator = mockStatic(Translator.class, invocation -> invocation.getArgument(0))) {
            JsonDifferenceDTO change = new OrganizationLogService().handleLogField(differences, "org-id").getFirst();
            assertEquals("enable", change.getColumn());
            assertEquals("log.enable", change.getColumnName());
            assertEquals(oldLabel, change.getOldValueName());
            assertEquals(newLabel, change.getNewValueName());
        }
    }
}
