package cn.cordys.crm.contract.service;

import cn.cordys.common.constants.FormKey;
import cn.cordys.common.service.BaseResourceFieldService;
import cn.cordys.crm.contract.domain.ContractPaymentRecordField;
import cn.cordys.crm.contract.domain.ContractPaymentRecordFieldBlob;
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author song-cc-rock
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class ContractPaymentRecordFieldService extends BaseResourceFieldService<ContractPaymentRecordField, ContractPaymentRecordFieldBlob> {

	@Resource
	private BaseMapper<ContractPaymentRecordField> contractPaymentRecordFieldMapper;
	@Resource
	private BaseMapper<ContractPaymentRecordFieldBlob> contractPaymentRecordFieldBlobMapper;

	@Override
	protected String getFormKey() {
		return FormKey.CONTRACT_PAYMENT_RECORD.getKey();
	}

	@Override
	protected BaseMapper<ContractPaymentRecordField> getResourceFieldMapper() {
		return contractPaymentRecordFieldMapper;
	}

	@Override
	protected BaseMapper<ContractPaymentRecordFieldBlob> getResourceFieldBlobMapper() {
		return contractPaymentRecordFieldBlobMapper;
	}
}
