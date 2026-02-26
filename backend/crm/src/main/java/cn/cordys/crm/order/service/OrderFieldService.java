package cn.cordys.crm.order.service;

import cn.cordys.common.constants.FormKey;
import cn.cordys.common.service.BaseResourceFieldService;
import cn.cordys.crm.order.domain.OrderField;
import cn.cordys.crm.order.domain.OrderFieldBlob;
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(rollbackFor = Exception.class)
public class OrderFieldService extends BaseResourceFieldService<OrderField, OrderFieldBlob> {

    @Resource
    private BaseMapper<OrderField> contractFieldMapper;
    @Resource
    private BaseMapper<OrderFieldBlob> contractFieldBlobMapper;

    @Override
    protected String getFormKey() {
        return FormKey.CONTRACT.getKey();
    }

    @Override
    protected BaseMapper<OrderField> getResourceFieldMapper() {
        return contractFieldMapper;
    }

    @Override
    protected BaseMapper<OrderFieldBlob> getResourceFieldBlobMapper() {
        return contractFieldBlobMapper;
    }
}
