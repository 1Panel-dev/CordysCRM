package io.cordys.crm.clue.service;

import io.cordys.common.constants.FormKey;
import io.cordys.common.service.BaseResourceFieldService;
import io.cordys.crm.clue.domain.ClueField;
import io.cordys.crm.clue.domain.ClueFieldBlob;
import io.cordys.mybatis.BaseMapper;
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