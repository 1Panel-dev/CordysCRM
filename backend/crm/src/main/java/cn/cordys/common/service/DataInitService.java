package cn.cordys.common.service;


import cn.cordys.common.util.OnceInterface;
import cn.cordys.common.util.OnceInterfaceAction;
import cn.cordys.crm.clue.service.ClueService;
import cn.cordys.crm.system.domain.Parameter;
import cn.cordys.crm.system.service.ModuleFieldExtService;
import cn.cordys.crm.system.service.ModuleFieldService;
import cn.cordys.crm.system.service.ModuleFormService;
import cn.cordys.crm.system.service.ModuleService;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class DataInitService {
    @Resource
    private ModuleService moduleService;
    @Resource
    private ModuleFormService moduleFormService;
    @Resource
    private ModuleFieldExtService moduleFieldExtService;
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
            initOneTime(moduleFormService::initUpgradeForm, "init.upgrade.form.v1.4.0");
            initOneTime(moduleFormService::initUpgradeForm, "init.upgrade.form.v1.5.0");
            initOneTime(moduleFormService::initUpgradeForm, "init.upgrade.form.v1.5.1");
            initOneTime(moduleFormService::initExtFieldsByVer, "1.5.1", "init.ext.fields.v1.5.1");
            initOneTime(moduleFieldExtService::setDefaultOptionSource, "set.default.option.source");
            initOneTime(moduleFieldExtService::refreshPlanFieldPos, "refresh.plan.field.pos");
            initOneTime(moduleFormService::initInvoiceFormScenarioProp, "init.invoice.form.scenario");
            initOneTime(moduleFieldService::modifyInvoiceShowFields, "init.invoice.show.fields");
            initOneTime(moduleService::deleteExtraModules, "delete.extra.modules");
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
            log.error(e.getMessage(), e);
        }
    }

    /**
     * 执行单次接口 (带参数)
     *
     * @param onceFunc 执行函数
     * @param param    参数
     * @param key      执行Key
     * @param <P>      参数类型
     */
    private <P> void initOneTime(OnceInterfaceAction<P> onceFunc, P param, final String key) {
        try {
            LambdaQueryWrapper<Parameter> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Parameter::getParamKey, key);
            List<Parameter> parameters = parameterMapper.selectListByLambda(queryWrapper);
            if (CollectionUtils.isEmpty(parameters)) {
                onceFunc.execute(param);
                insertParameterOnceKey(key);
            }
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
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