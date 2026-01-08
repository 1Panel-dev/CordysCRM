package cn.cordys.crm.contract.service;

import cn.cordys.crm.contract.domain.BusinessTitleConfig;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
@Transactional(rollbackFor = Exception.class)
@Slf4j
public class BusinessTitleConfigService {

    @Resource
    private BaseMapper<BusinessTitleConfig> businessTitleConfigMapper;


    /**
     * 获取工商抬头配置
     *
     * @param orgId
     * @return
     */
    public List<BusinessTitleConfig> getConfigs(String orgId) {
        LambdaQueryWrapper<BusinessTitleConfig> configWrapper = new LambdaQueryWrapper<>();
        configWrapper.eq(BusinessTitleConfig::getOrganizationId, orgId);
        return businessTitleConfigMapper.selectListByLambda(configWrapper);
    }


    /**
     * 更新
     *
     * @param id
     */
    public void switchRequired(String id) {
        BusinessTitleConfig businessTitleConfig = businessTitleConfigMapper.selectByPrimaryKey(id);
        businessTitleConfig.setRequired(!businessTitleConfig.getRequired());
        businessTitleConfigMapper.update(businessTitleConfig);
    }
}
