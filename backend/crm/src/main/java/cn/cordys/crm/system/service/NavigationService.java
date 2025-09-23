package cn.cordys.crm.system.service;

import cn.cordys.aspectj.annotation.OperationLog;
import cn.cordys.aspectj.constants.LogModule;
import cn.cordys.aspectj.constants.LogType;
import cn.cordys.aspectj.context.OperationLogContext;
import cn.cordys.aspectj.dto.LogContextInfo;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.system.domain.Navigation;
import cn.cordys.crm.system.dto.request.ModuleSortRequest;
import cn.cordys.crm.system.mapper.ExtNavigationMapper;
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(rollbackFor = Exception.class)
public class NavigationService {

    @Resource
    private BaseMapper<Navigation> navigationMapper;
    @Resource
    private ExtNavigationMapper extNavigationMapper;


    /**
     * 获取系统导航栏配置列表
     *
     * @param orgId
     *
     * @return
     */
    public List<Navigation> getNavigationList(String orgId) {
        List<Navigation> navigations = extNavigationMapper.selectList(orgId);
        return navigations;
    }


    /**
     * 导航栏排序
     *
     * @param request
     * @param userId
     */
    @OperationLog(module = LogModule.SYSTEM_MODULE, type = LogType.UPDATE, operator = "{#currentUser}")
    public void sort(ModuleSortRequest request, String userId) {
        Navigation navigation = navigationMapper.selectByPrimaryKey(request.getDragModuleId());
        if (navigation == null) {
            throw new GenericException(Translator.get("module.not_exist"));
        }
        List<String> beforeKeys = getNavigationSortKeys(navigation.getOrganizationId());
        if (request.getStart() < request.getEnd()) {
            // start < end, 区间模块上移, pos - 1
            extNavigationMapper.moveUpNavigation(request.getStart(), request.getEnd());
        } else {
            // start > end, 区间模块下移, pos + 1
            extNavigationMapper.moveDownNavigation(request.getEnd(), request.getStart());
        }
        Navigation dragNavigation = new Navigation();
        dragNavigation.setId(request.getDragModuleId());
        dragNavigation.setPos(request.getEnd());
        dragNavigation.setUpdateUser(userId);
        dragNavigation.setUpdateTime(System.currentTimeMillis());
        navigationMapper.updateById(dragNavigation);
        List<String> afterKeys = getNavigationSortKeys(navigation.getOrganizationId());

        //添加日志上下文
        Map<String, List<String>> originalVal = new HashMap<>(1);
        originalVal.put("navigationSort", beforeKeys);
        Map<String, List<String>> modifiedVal = new HashMap<>(1);
        modifiedVal.put("navigationSort", afterKeys);
        OperationLogContext.setContext(LogContextInfo.builder()
                .originalValue(originalVal)
                .resourceName(Translator.get("top.navigation"))
                .modifiedValue(modifiedVal)
                .resourceId(navigation.getId())
                .build());
    }

    private List<String> getNavigationSortKeys(String orgId) {
        List<Navigation> navigationList = getNavigationList(orgId);
        return navigationList.stream().map(Navigation::getNavigationKey).toList();
    }
}
