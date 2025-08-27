package cn.cordys.crm.clue.service;

import cn.cordys.common.constants.FormKey;
import cn.cordys.common.service.BaseResourceFieldService;
import cn.cordys.crm.clue.domain.ClueField;
import cn.cordys.crm.clue.domain.ClueFieldBlob;
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author jianxing
 * @date 2025-02-08 16:24:22
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class ClueFieldService extends BaseResourceFieldService<ClueField, ClueFieldBlob> {
    @Resource
    private BaseMapper<ClueField> clueFieldMapper;
    @Resource
    private BaseMapper<ClueFieldBlob> clueFieldBlobMapper;

    @Override
    protected String getFormKey() {
        return FormKey.CLUE.getKey();
    }

    @Override
    protected BaseMapper<ClueField> getResourceFieldMapper() {
        return clueFieldMapper;
    }

    @Override
    protected BaseMapper<ClueFieldBlob> getResourceFieldBlobMapper() {
        return clueFieldBlobMapper;
    }
}