package cn.cordys.common.service;

import cn.cordys.common.util.LogUtils;
import cn.cordys.common.util.OnceInterface;
import cn.cordys.crm.clue.service.ClueService;
import cn.cordys.crm.system.domain.Parameter;
import cn.cordys.crm.system.service.ModuleFieldService;
import cn.cordys.crm.system.service.ModuleFormService;
import cn.cordys.crm.system.service.ModuleService;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author jianxing
 * @date 2025-01-03 12:01:54
 */
@Service
public class DataInitService {
    @Resource
    private ModuleService moduleService;
    @Resource
    private ModuleFormService moduleFormService;
    @Resource
    private BaseMapper<Parameter> parameterMapper;
    @Resource
    private Redisson redisson;
    @Resource
    private ModuleFieldService moduleFieldService;
    @Resource
    private ClueService clueService;

    public void initOneTime() {
        RLock lock = redisson.getLock("init_data_lock");
        lock.lock();
        try {
            initOneTime(moduleService::initDefaultOrgModule, "init.module");
            initOneTime(moduleFormService::initForm, "init.form");
            initOneTime(moduleFieldService::modifyDateProp, "modify.form.date");
            initOneTime(moduleFormService::modifyFormLinkProp, "modify.form.link");
            initOneTime(moduleFormService::modifyFormProp, "modify.form.prop");
            initOneTime(moduleFormService::modifyFieldMobile, "modify.field.mobile");
            initOneTime(moduleFormService::modifyPhoneFieldFormat, "modify.field.format");
            initOneTime(moduleFormService::processOldLinkData, "process.old.link.data");
            initOneTime(moduleFormService::initFormScenarioProp, "init.record.form.scenario");
            initOneTime(clueService::processTransferredCluePlanAndRecord, "process.transferred.clue");
        } finally {
            lock.unlock();
        }
    }

    private void initOneTime(OnceInterface onceFunc, final String key) {
        try {
            LambdaQueryWrapper<Parameter> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Parameter::getParamKey, key);
            List<Parameter> parameters = parameterMapper.selectListByLambda(queryWrapper);
            if (CollectionUtils.isEmpty(parameters)) {
                onceFunc.execute();
                insertParameterOnceKey(key);
            }
        } catch (Throwable e) {
            LogUtils.error(e.getMessage(), e);
        }
    }

    private void insertParameterOnceKey(String key) {
        Parameter parameter = new Parameter();
        parameter.setParamKey(key);
        parameter.setParamValue("done");
        parameter.setType("text");
        parameterMapper.insert(parameter);
    }
}