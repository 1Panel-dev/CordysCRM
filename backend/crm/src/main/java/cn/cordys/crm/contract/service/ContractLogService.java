package cn.cordys.crm.contract.service;

import cn.cordys.common.constants.FormKey;
import cn.cordys.common.dto.JsonDifferenceDTO;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.system.service.BaseModuleLogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(rollbackFor = Exception.class)
public class ContractLogService extends BaseModuleLogService {
    @Override
    public List<JsonDifferenceDTO> handleLogField(List<JsonDifferenceDTO> differenceDTOS, String orgId) {
        differenceDTOS = super.handleModuleLogField(differenceDTOS, orgId, FormKey.CONTRACT.getKey());

        List<JsonDifferenceDTO> toRemove = new ArrayList<>();

        for (JsonDifferenceDTO differ : differenceDTOS) {

            if (differ.getColumn().contains(Translator.get("products_info"))) {
                differ.setColumnName(differ.getColumn());
            }
            //去掉表格类型的变更日志
            if (!(differ.getNewValue() instanceof List) && !(differ.getOldValue() instanceof List)) {
                toRemove.add(differ);
            }
        }

        return toRemove;
    }
}
