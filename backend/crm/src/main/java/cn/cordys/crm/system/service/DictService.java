package cn.cordys.crm.system.service;

import cn.cordys.aspectj.annotation.OperationLog;
import cn.cordys.aspectj.constants.LogModule;
import cn.cordys.aspectj.constants.LogType;
import cn.cordys.aspectj.context.OperationLogContext;
import cn.cordys.aspectj.dto.LogContextInfo;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.common.util.BeanUtils;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.system.constants.DictModule;
import cn.cordys.crm.system.domain.Dict;
import cn.cordys.crm.system.domain.DictConfig;
import cn.cordys.crm.system.dto.DictConfigDTO;
import cn.cordys.crm.system.dto.request.DictAddRequest;
import cn.cordys.crm.system.dto.request.DictSortRequest;
import cn.cordys.crm.system.dto.request.DictSwitchRequest;
import cn.cordys.crm.system.dto.request.DictUpdateRequest;
import cn.cordys.crm.system.mapper.ExtDictMapper;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import java.util.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(rollbackFor = Exception.class)
public class DictService {

  @Resource private BaseMapper<Dict> dictMapper;
  @Resource private BaseMapper<DictConfig> dictConfigMapper;
  @Resource private ExtDictMapper extDictMapper;

  public List<Dict> getDictListByType(String module, String orgId) {
    LambdaQueryWrapper<Dict> dictLambdaQueryWrapper = new LambdaQueryWrapper<>();
    dictLambdaQueryWrapper.eq(Dict::getModule, module).eq(Dict::getOrganizationId, orgId);
    List<Dict> dictList = dictMapper.selectListByLambda(dictLambdaQueryWrapper);
    dictList.sort(Comparator.comparingLong(Dict::getPos));
    return dictList;
  }

  /**
   * 添加字典值
   *
   * @param request 请求参数
   * @param currentUser 当前用户
   */
  @OperationLog(module = LogModule.SYSTEM_MODULE, type = LogType.ADD)
  public void addDict(DictAddRequest request, String currentUser, String orgId) {
    Dict dict = new Dict();
    dict.setId(IDGenerator.nextStr());
    dict.setName(request.getName());
    dict.setModule(request.getModule());
    dict.setOrganizationId(orgId);
    dict.setType("TEXT");
    Long nextPos = extDictMapper.getNextPos(request.getModule(), orgId);
    dict.setPos(nextPos == null ? 1L : nextPos);
    dict.setCreateUser(currentUser);
    dict.setCreateTime(System.currentTimeMillis());
    dict.setUpdateUser(currentUser);
    dict.setUpdateTime(System.currentTimeMillis());
    dictMapper.insert(dict);

    // 添加日志上下文
    OperationLogContext.setContext(
        LogContextInfo.builder()
            .modifiedValue(dict)
            .resourceId(dict.getId())
            .resourceName(
                Translator.get("module." + request.getModule().toLowerCase())
                    + Translator.get("module.reason.setting"))
            .build());
  }

  /**
   * 更新字典值
   *
   * @param request 请求参数
   * @param currentUser 当前用户
   */
  @OperationLog(module = LogModule.SYSTEM_MODULE, type = LogType.UPDATE)
  public void updateDict(DictUpdateRequest request, String currentUser) {
    Dict oldDict = dictMapper.selectByPrimaryKey(request.getId());
    if (oldDict == null) {
      throw new GenericException(Translator.get("dict.not_exist"));
    }
    Dict dict = BeanUtils.copyBean(new Dict(), oldDict);
    dict.setName(request.getName());
    dict.setUpdateUser(currentUser);
    dict.setUpdateTime(System.currentTimeMillis());
    dictMapper.updateById(dict);

    OperationLogContext.setContext(
        LogContextInfo.builder()
            .resourceId(request.getId())
            .resourceName(
                Translator.get("module." + oldDict.getModule().toLowerCase())
                    + Translator.get("module.reason.setting"))
            .originalValue(oldDict)
            .modifiedValue(dict)
            .build());
  }

  /**
   * 删除字典值
   *
   * @param id 字典ID
   */
  @OperationLog(module = LogModule.SYSTEM_MODULE, type = LogType.DELETE, resourceId = "{#id}")
  public void deleteDict(String id) {
    Dict dict = dictMapper.selectByPrimaryKey(id);
    if (dict == null) {
      throw new GenericException(Translator.get("dict.not_exist"));
    }
    dictMapper.deleteByPrimaryKey(id);
    OperationLogContext.setResourceName(
        Translator.get("module." + dict.getModule().toLowerCase())
            + Translator.get("module.reason.setting")
            + "/"
            + dict.getName());
  }

  /**
   * 切换字典配置开关
   *
   * @param request 请求参数
   * @param orgId 组织ID
   */
  @OperationLog(
      module = LogModule.SYSTEM_MODULE,
      type = LogType.UPDATE,
      operator = "{#currentUser}")
  public void switchDict(DictSwitchRequest request, String orgId) {
    LambdaQueryWrapper<DictConfig> configLambdaQueryWrapper = new LambdaQueryWrapper<>();
    configLambdaQueryWrapper
        .eq(DictConfig::getModule, request.getModule())
        .eq(DictConfig::getOrganizationId, orgId);
    List<DictConfig> configs = dictConfigMapper.selectListByLambda(configLambdaQueryWrapper);
    if (CollectionUtils.isNotEmpty(configs)) {
      DictConfig config = configs.getFirst();
      config.setEnabled(request.getEnable());
      extDictMapper.updateModuleConfig(config);
    } else {
      DictConfig config = new DictConfig();
      BeanUtils.copyBean(config, request);
      config.setEnabled(request.getEnable());
      config.setOrganizationId(orgId);
      dictConfigMapper.insert(config);
    }

    // 添加日志上下文
    Map<String, String> originalVal = new HashMap<>(1);
    originalVal.put(
        "module.switch",
        !request.getEnable()
            ? Translator.get("log.enable.true")
            : Translator.get("log.enable.false"));
    Map<String, String> modifiedVal = new HashMap<>(1);
    modifiedVal.put(
        "module.switch",
        request.getEnable()
            ? Translator.get("log.enable.true")
            : Translator.get("log.enable.false"));
    OperationLogContext.setContext(
        LogContextInfo.builder()
            .originalValue(originalVal)
            .resourceName(
                Translator.get("module." + request.getModule().toLowerCase())
                    + Translator.get("module.reason.setting"))
            .modifiedValue(modifiedVal)
            .resourceId(request.getModule())
            .build());
  }

  /**
   * 字典排序
   *
   * @param request 请求参数
   * @param currentUser 当前用户
   */
  @OperationLog(
      module = LogModule.SYSTEM_MODULE,
      type = LogType.UPDATE,
      operator = "{#currentUser}")
  public void sort(DictSortRequest request, String currentUser) {
    Dict oldDict = dictMapper.selectByPrimaryKey(request.getDragDictId());
    if (oldDict == null) {
      throw new GenericException(Translator.get("dict.not_exist"));
    }
    // sort-before
    List<String> beforeKeys =
        getDictListByType(oldDict.getModule(), oldDict.getOrganizationId()).stream()
            .map(Dict::getName)
            .toList();
    if (request.getStart() < request.getEnd()) {
      // start < end, 区间模块上移, pos - 1
      extDictMapper.moveUpDict(
          request.getStart(), request.getEnd(), oldDict.getModule(), oldDict.getOrganizationId());
    } else {
      // start > end, 区间模块下移, pos + 1
      extDictMapper.moveDownDict(
          request.getEnd(), request.getStart(), oldDict.getModule(), oldDict.getOrganizationId());
    }
    extDictMapper.updateDictPos(request.getDragDictId(), request.getEnd(), currentUser);
    // sort-after
    List<String> afterKeys =
        getDictListByType(oldDict.getModule(), oldDict.getOrganizationId()).stream()
            .map(Dict::getName)
            .toList();

    // 添加日志上下文
    Map<String, List<String>> originalVal = new HashMap<>(1);
    originalVal.put("sort", beforeKeys);
    Map<String, List<String>> modifiedVal = new HashMap<>(1);
    modifiedVal.put("sort", afterKeys);
    OperationLogContext.setContext(
        LogContextInfo.builder()
            .originalValue(originalVal)
            .resourceName(
                Translator.get("module." + oldDict.getModule().toLowerCase())
                    + Translator.get("module.reason.setting"))
            .modifiedValue(modifiedVal)
            .resourceId(request.getDragDictId())
            .build());
  }

  /**
   * 获取模块字典配置
   *
   * @param module 模块
   * @param orgId 组织ID
   * @return 字典配置
   */
  public DictConfigDTO getDictConf(String module, String orgId) {
    List<Dict> dictList = new ArrayList<>(getDictListByType(module, orgId));
    Dict sysDt = new Dict();
    sysDt.setId("system");
    if (Strings.CS.equalsAny(
        module, DictModule.CLUE_POOL_RS.name(), DictModule.CUSTOMER_POOL_RS.name())) {
      sysDt.setName(Translator.get("system.auto.recycle"));
    } else {
      sysDt.setName(Translator.get("system.auto.closed"));
    }
    dictList.addLast(sysDt);
    LambdaQueryWrapper<DictConfig> configLambdaQueryWrapper = new LambdaQueryWrapper<>();
    configLambdaQueryWrapper
        .eq(DictConfig::getModule, module)
        .eq(DictConfig::getOrganizationId, orgId);
    List<DictConfig> configs = dictConfigMapper.selectListByLambda(configLambdaQueryWrapper);
    return DictConfigDTO.builder()
        .dictList(dictList)
        .enable(CollectionUtils.isNotEmpty(configs) && configs.getFirst().getEnabled())
        .build();
  }
}
