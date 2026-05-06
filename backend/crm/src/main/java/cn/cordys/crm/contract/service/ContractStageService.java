package cn.cordys.crm.contract.service;

import cn.cordys.aspectj.annotation.OperationLog;
import cn.cordys.aspectj.constants.LogModule;
import cn.cordys.aspectj.constants.LogType;
import cn.cordys.aspectj.context.OperationLogContext;
import cn.cordys.aspectj.dto.LogContextInfo;
import cn.cordys.common.dto.stage.StageAddRequest;
import cn.cordys.common.dto.stage.StageConfigResponse;
import cn.cordys.common.dto.stage.StageRollBackRequest;
import cn.cordys.common.dto.stage.StageUpdateRequest;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.contract.domain.ContractStageConfig;
import cn.cordys.crm.contract.dto.response.ContractStageConfigListResponse;
import cn.cordys.crm.contract.mapper.ExtContractMapper;
import cn.cordys.crm.contract.mapper.ExtContractStageConfigMapper;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(rollbackFor = Exception.class)
public class ContractStageService {
    public static final Long DEFAULT_POS = 1L;
    @Resource
    private ExtContractStageConfigMapper extContractStageConfigMapper;
    @Resource
    private ExtContractMapper extContractMapper;
    @Resource
    private BaseMapper<ContractStageConfig> contractStageConfigMapper;


    /**
     * 列表
     *
     * @param orgId
     * @return
     */
    public ContractStageConfigListResponse getStageConfigList(String orgId) {
        ContractStageConfigListResponse stageConfigListResponse = new ContractStageConfigListResponse();
        List<StageConfigResponse> stageConfigList = extContractStageConfigMapper.getStageConfigList(orgId);
        buildList(stageConfigList, stageConfigListResponse);
        return stageConfigListResponse;
    }

    private void buildList(List<StageConfigResponse> stageConfigList, ContractStageConfigListResponse response) {
        response.setStageConfigList(stageConfigList);
        if (CollectionUtils.isNotEmpty(stageConfigList)) {
            var first = stageConfigList.getFirst();
            response.setEndRollBack(first.getEndRollBack());
            response.setAfootRollBack(first.getAfootRollBack());
            stageConfigList.forEach(sc -> sc.setStageHasData(extContractMapper.countByStage(sc.getId()) > 0));
        }
    }


    /**
     * 添加合同状态流配置
     *
     * @param request
     * @param userId
     * @param orgId
     * @return
     */
    @OperationLog(module = LogModule.SYSTEM_MODULE, type = LogType.ADD)
    public String addStageConfig(StageAddRequest request, String userId, String orgId) {
        checkConfigCount(orgId);
        Long pos = DEFAULT_POS;
        Boolean afootRollBack = true;
        Boolean endRollBack = false;
        //源节点
        ContractStageConfig target = contractStageConfigMapper.selectByPrimaryKey(request.getTargetId());
        if (target != null) {
            pos = target.getPos();
            //target正常不会为空
            if (request.getDropPosition() == -1) {
                extContractStageConfigMapper.moveUpStageConfig(pos, orgId, DEFAULT_POS);
            } else {
                extContractStageConfigMapper.moveDownStageConfig(pos, orgId, DEFAULT_POS);
                pos = pos + 1;
            }
            afootRollBack = target.getAfootRollBack();
            endRollBack = target.getEndRollBack();
        }

        ContractStageConfig stageConfig = new ContractStageConfig();
        stageConfig.setId(IDGenerator.nextStr());
        stageConfig.setName(request.getName());
        stageConfig.setType(request.getType());
        stageConfig.setAfootRollBack(afootRollBack);
        stageConfig.setEndRollBack(endRollBack);
        stageConfig.setPos(pos);
        stageConfig.setOrganizationId(orgId);
        stageConfig.setCreateUser(userId);
        stageConfig.setUpdateUser(userId);
        stageConfig.setCreateTime(System.currentTimeMillis());
        stageConfig.setUpdateTime(System.currentTimeMillis());
        contractStageConfigMapper.insert(stageConfig);

        OperationLogContext.setContext(LogContextInfo.builder()
                .modifiedValue(stageConfig)
                .resourceId(stageConfig.getId())
                .resourceName(Translator.get("contract_stage_setting").concat(":").concat(request.getName()))
                .build());

        return stageConfig.getId();
    }


    /**
     * 配置数量校验
     *
     * @param orgId
     */
    private void checkConfigCount(String orgId) {
        if (extContractStageConfigMapper.countStageConfig(orgId) > 10) {
            throw new GenericException(Translator.get("contract_stage_config_list"));
        }
    }


    /**
     * 删除
     *
     * @param id
     * @param orgId
     */
    @OperationLog(module = LogModule.SYSTEM_MODULE, type = LogType.DELETE, resourceId = "{#id}")
    public void delete(String id, String orgId) {
        ContractStageConfig stageConfig = deletePreCheck(id, orgId);
        contractStageConfigMapper.deleteByPrimaryKey(id);
        // 设置操作对象
        OperationLogContext.setResourceName(Translator.get("contract_stage_setting").concat(":").concat(stageConfig.getName()));
    }


    private ContractStageConfig deletePreCheck(String id, String orgId) {
        ContractStageConfig stageConfig = contractStageConfigMapper.selectByPrimaryKey(id);
        if (stageConfig == null) {
            throw new GenericException(Translator.get("contract_stage_delete"));
        }
        return stageConfig;
    }


    /**
     * 更新订单流阶段回退设置
     *
     * @param request
     * @param orgId
     */
    @OperationLog(module = LogModule.SYSTEM_MODULE, type = LogType.UPDATE)
    public void updateRollBack(StageRollBackRequest request, String orgId) {
        LambdaQueryWrapper<ContractStageConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ContractStageConfig::getOrganizationId, orgId);
        List<ContractStageConfig> stageConfigList = contractStageConfigMapper.selectListByLambda(wrapper);
        extContractStageConfigMapper.updateRollBack(request, orgId);

        Map<String, String> originalVal = new HashMap<>(1);
        originalVal.put("afootRollBack", Translator.get("log.enable.".concat(stageConfigList.getFirst().getAfootRollBack().toString())));
        originalVal.put("endRollBack", Translator.get("log.enable.".concat(stageConfigList.getFirst().getEndRollBack().toString())));
        Map<String, String> modifiedVal = new HashMap<>(1);
        modifiedVal.put("afootRollBack", Translator.get("log.enable.".concat(request.getAfootRollBack().toString())));
        modifiedVal.put("endRollBack", Translator.get("log.enable.".concat(request.getEndRollBack().toString())));
        OperationLogContext.setContext(LogContextInfo.builder()
                .originalValue(originalVal)
                .resourceName(Translator.get("contract_stage_setting"))
                .modifiedValue(modifiedVal)
                .resourceId(orgId)
                .build());
    }


    /**
     * 更新配置
     *
     * @param request
     * @param userId
     */
    @OperationLog(module = LogModule.SYSTEM_MODULE, type = LogType.UPDATE)
    public void update(StageUpdateRequest request, String userId) {
        ContractStageConfig oldStageConfig = contractStageConfigMapper.selectByPrimaryKey(request.getId());
        if (oldStageConfig == null) {
            throw new GenericException(Translator.get("order_stage_not_exist"));
        }
        extContractStageConfigMapper.updateStageConfig(request, userId);

        Map<String, String> originalVal = new HashMap<>(1);
        originalVal.put("stage", oldStageConfig.getName());
        Map<String, String> modifiedVal = new HashMap<>(1);
        modifiedVal.put("stage", request.getName());
        OperationLogContext.setContext(
                LogContextInfo.builder()
                        .resourceId(request.getId())
                        .resourceName(Translator.get("contract_stage_setting"))
                        .originalValue(originalVal)
                        .modifiedValue(modifiedVal)
                        .build()
        );
    }


    /**
     * 排序
     *
     * @param ids
     * @param orgId
     */
    @OperationLog(module = LogModule.SYSTEM_MODULE, type = LogType.UPDATE)
    public void sort(List<String> ids, String orgId) {
        List<StageConfigResponse> oldStageConfigList = extContractStageConfigMapper.getStageConfigList(orgId);
        List<String> oldNames = oldStageConfigList.stream().map(StageConfigResponse::getName).toList();

        for (int i = 0; i < ids.size(); i++) {
            extContractStageConfigMapper.updatePos(ids.get(i), (long) (i + 1));
        }

        List<StageConfigResponse> newStageConfigList = extContractStageConfigMapper.getStageConfigList(orgId);
        List<String> newNames = newStageConfigList.stream().map(StageConfigResponse::getName).toList();

        Map<String, List<String>> originalVal = new HashMap<>(1);
        originalVal.put("stageSort", oldNames);
        Map<String, List<String>> modifiedVal = new HashMap<>(1);
        modifiedVal.put("stageSort", newNames);
        OperationLogContext.setContext(
                LogContextInfo.builder()
                        .resourceId(orgId)
                        .resourceName(Translator.get("contract_stage_setting"))
                        .originalValue(originalVal)
                        .modifiedValue(modifiedVal)
                        .build()
        );
    }
}
