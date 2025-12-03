package cn.cordys.crm.contract.service;

import cn.cordys.common.constants.FormKey;
import cn.cordys.common.dto.JsonDifferenceDTO;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.system.service.BaseModuleLogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(rollbackFor = Exception.class)
public class ContractLogService extends BaseModuleLogService {
    @Override
    public List<JsonDifferenceDTO> handleLogField(List<JsonDifferenceDTO> differences, String orgId) {
        differences = super.handleModuleLogField(differences, orgId, FormKey.CONTRACT.getKey());

        for (JsonDifferenceDTO differ : differences) {

            if (differ.getColumn().contains(Translator.get("products_info"))) {
                differ.setColumnName(differ.getColumn());
            }

        }

        return differences;
    }
}
