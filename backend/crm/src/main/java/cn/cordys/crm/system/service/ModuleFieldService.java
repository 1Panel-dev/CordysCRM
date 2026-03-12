package cn.cordys.crm.system.service;

import cn.cordys.common.constants.BusinessModuleField;
import cn.cordys.common.constants.FormKey;
import cn.cordys.common.domain.BaseModuleFieldValue;
import cn.cordys.common.dto.BaseTreeNode;
import cn.cordys.common.dto.OptionDTO;
import cn.cordys.common.exception.GenericException;
import cn.cordys.common.mapper.CommonMapper;
import cn.cordys.common.util.CommonBeanFactory;
import cn.cordys.common.util.JSON;
import cn.cordys.common.util.Translator;
import cn.cordys.crm.product.service.ProductPriceService;
import cn.cordys.crm.product.service.ProductService;
import cn.cordys.crm.system.constants.FieldSourceType;
import cn.cordys.crm.system.constants.FieldType;
import cn.cordys.crm.system.domain.ModuleField;
import cn.cordys.crm.system.domain.ModuleFieldBlob;
import cn.cordys.crm.system.dto.DatasourceRefDTO;
import cn.cordys.crm.system.dto.field.DatasourceField;
import cn.cordys.crm.system.dto.field.DateTimeField;
import cn.cordys.crm.system.dto.request.DatasourceRefQueryRequest;
import cn.cordys.crm.system.dto.request.FieldRepeatCheckRequest;
import cn.cordys.crm.system.dto.response.FieldRepeatCheckResponse;
import cn.cordys.mybatis.BaseMapper;
import cn.cordys.mybatis.lambda.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author song-cc-rock
 */
@Service
@Slf4j
public class ModuleFieldService {

    /**
     * 表单表格映射
     */
    private static final Map<String, String> FORM_TABLE = new HashMap<>(8);

	private static final Map<String, Class<?>> SOURCE_REF_CLASS = new HashMap<>();

    static {
        FORM_TABLE.put(FormKey.CLUE.getKey(), "clue");
        FORM_TABLE.put(FormKey.CUSTOMER.getKey(), "customer");
        FORM_TABLE.put(FormKey.CONTACT.getKey(), "customer_contact");
        FORM_TABLE.put(FormKey.OPPORTUNITY.getKey(), "opportunity");
        FORM_TABLE.put(FormKey.QUOTATION.getKey(), "opportunity_quotation");
        FORM_TABLE.put(FormKey.PRODUCT.getKey(), "product");
        FORM_TABLE.put(FormKey.PRICE.getKey(), "product_price");
        FORM_TABLE.put(FormKey.FOLLOW_RECORD.getKey(), "follow_up_record");
        FORM_TABLE.put(FormKey.FOLLOW_PLAN.getKey(), "follow_up_plan");
        FORM_TABLE.put(FormKey.CONTRACT.getKey(), "contract");
        FORM_TABLE.put(FormKey.CONTRACT_PAYMENT_PLAN.getKey(), "contract_payment_plan");
        FORM_TABLE.put(FormKey.CONTRACT_PAYMENT_RECORD.getKey(), "contract_payment_record");
        FORM_TABLE.put(FormKey.INVOICE.getKey(), "contract_invoice");
        FORM_TABLE.put(FormKey.ORDER.getKey(), "sales_order");

		SOURCE_REF_CLASS.put(FieldSourceType.PRODUCT.name(), ProductService.class);
		SOURCE_REF_CLASS.put(FieldSourceType.PRICE.name(), ProductPriceService.class);
    }

    @Resource
    private BaseMapper<ModuleField> moduleFieldMapper;
    @Resource
    private DepartmentService departmentService;
    @Resource
    private BaseMapper<ModuleFieldBlob> moduleFieldBlobMapper;
    @Resource
    private CommonMapper commonMapper;

    /**
     * 获取不带用户的信息的部门树
     *
     * @return 部门树
     */
    public List<BaseTreeNode> getDeptTree(String orgId) {
        return departmentService.getTree(orgId);
    }

    /**
     * 修改日期时间类型的字段部分属性
     */
    public void modifyDateProp() {
        LambdaQueryWrapper<ModuleField> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ModuleField::getType, FieldType.DATE_TIME.name());
        List<ModuleField> moduleFields = moduleFieldMapper.selectListByLambda(queryWrapper);
        List<String> fieldIds = moduleFields.stream().map(ModuleField::getId).toList();
        if (CollectionUtils.isNotEmpty(fieldIds)) {
            LambdaQueryWrapper<ModuleFieldBlob> blobWrapper = new LambdaQueryWrapper<>();
            blobWrapper.in(ModuleFieldBlob::getId, fieldIds);
            List<ModuleFieldBlob> moduleFieldBlobs = moduleFieldBlobMapper.selectListByLambda(blobWrapper);
            for (ModuleFieldBlob blob : moduleFieldBlobs) {
                DateTimeField dateTimeField = JSON.parseObject(blob.getProp(), DateTimeField.class);
                if (StringUtils.isEmpty(dateTimeField.getDateDefaultType())) {
                    dateTimeField.setDateDefaultType("custom");
                }
                blob.setProp(JSON.toJSONString(dateTimeField));
                moduleFieldBlobMapper.updateById(blob);
            }
        }
    }

    /**
     * 校验字段值是否唯一
     *
     * @param request 请求参数
     * @return 是否唯一
     */
    public FieldRepeatCheckResponse checkRepeat(FieldRepeatCheckRequest request, String currentOrg) {
        ModuleField field = moduleFieldMapper.selectByPrimaryKey(request.getId());
        if (field == null) {
            throw new GenericException(Translator.get("module.field.not_exist"));
        }
        String tableName = FORM_TABLE.get(request.getFormKey());
        if (StringUtils.isBlank(tableName)) {
            throw new GenericException(Translator.get("module.form.illegal.unique.check"));
        }
        String value = request.getValue();
        if (Strings.CI.equals(field.getType(), FieldType.PHONE.toString())) {
            value = StringUtils.deleteWhitespace(value);
        }

        BusinessModuleField businessField = BusinessModuleField.ofKey(field.getInternalKey());
        String repeatName;
        if (businessField != null) {
            // 业务字段
            repeatName = commonMapper.checkInternalRepeatName(tableName, businessField.getBusinessKey(), value, currentOrg);
        } else {
            repeatName = commonMapper.checkFieldRepeatName(tableName, tableName + "_field", request.getId(), value, currentOrg);
        }
        return FieldRepeatCheckResponse.builder().name(repeatName).repeat(StringUtils.isNotBlank(repeatName)).build();
    }

	@SuppressWarnings("unchecked")
	public List<DatasourceRefDTO> getSourceRefDetail(DatasourceRefQueryRequest request) {
		List<String> sourceIds = request.getSourceIds().stream().distinct().toList();
		Class<?> sourceClass = SOURCE_REF_CLASS.get(request.getDataSourceType());
		if (sourceClass == null) {
			return new ArrayList<>();
		}

		Object service = CommonBeanFactory.getBean(sourceClass);
		List<DatasourceRefDTO> result = new ArrayList<>();
		try {
			Method executeMethod = sourceClass.getMethod("get", String.class);
			for (String id : sourceIds) {
				Object res = executeMethod.invoke(service, id);
				if (res == null) {
					continue;
				}
				DatasourceRefDTO dto = new DatasourceRefDTO();
				dto.setId(id);
				dto.setName(getField(res, "name", String.class));
				dto.setModuleFields((List<BaseModuleFieldValue>) getField(res, "moduleFields", List.class));
				dto.setProducts((List<Map<String, Object>>) getField(res, "products", List.class));
				dto.setOptionMap((Map<String, List<OptionDTO>>) getField(res, "optionMap", Map.class));
				dto.setStatus(getField(res, "status", String.class));

				if (Strings.CI.equals(request.getDataSourceType(), FieldSourceType.PRODUCT.name())) {
					// 产品需要获取系统价格字段
					dto.setPrice(getField(res, "price", BigDecimal.class));
				}
				result.add(dto);
			}
		} catch (Exception e) {
			log.error("获取数据源引用详细失败, {}", e.getMessage());
			return new ArrayList<>();
		}

		return result;
	}

    public void modifyInvoiceShowFields() {
        LambdaQueryWrapper<ModuleField> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ModuleField::getInternalKey, BusinessModuleField.INVOICE_CONTRACT_ID.getKey());
        ModuleField example = new ModuleField();
        example.setInternalKey(BusinessModuleField.INVOICE_CONTRACT_ID.getKey());
        ModuleField contractField = moduleFieldMapper.selectOne(example);
        if (contractField != null) {
            ModuleFieldBlob contractFieldBlob = moduleFieldBlobMapper.selectByPrimaryKey(contractField.getId());
            String prop = contractFieldBlob.getProp();
            DatasourceField datasourceField = JSON.parseObject(prop, DatasourceField.class);
            List<String> showFields = datasourceField.getShowFields();
            if (CollectionUtils.isNotEmpty(showFields)) {
                for (int i = 0; i < showFields.size(); i++) {
                    // 合同总金额字段初始化之后，显示字段替换成字段ID
                    if (Strings.CI.equalsAny(showFields.get(i),
                            "contractProductSumAmount",
                            BusinessModuleField.CONTRACT_TOTAL_AMOUNT.getKey())) {
                        ModuleField contractNameField = selectFieldByInternalKey(BusinessModuleField.CONTRACT_TOTAL_AMOUNT.getKey());
                        if (contractNameField != null) {
                            showFields.set(i, contractNameField.getId());
                            contractFieldBlob.setProp(JSON.toJSONString(datasourceField));
                            moduleFieldBlobMapper.updateById(contractFieldBlob);
                            break;
                        }
                    }
                }
            }
        }
    }

    public List<ModuleField> selectFieldsByInternalKeys(List<String> internalKeys) {
        return moduleFieldMapper.selectListByLambda(new LambdaQueryWrapper<ModuleField>()
                .in(ModuleField::getInternalKey, internalKeys));
    }

    public ModuleField selectFieldByInternalKey(String internalKey) {
        ModuleField field = new ModuleField();
        field.setInternalKey(internalKey);
        return moduleFieldMapper.selectOne(field);
    }

	@SuppressWarnings("unchecked")
	private <T> T getField(Object obj, String fieldName, Class<T> type) {
		Class<?> clazz = obj.getClass();

		while (clazz != null) {
			try {
				Field field = clazz.getDeclaredField(fieldName);
				field.setAccessible(true);

				Object value = field.get(obj);
				if (type.isInstance(value)) {
					return (T) value;
				}

				return null;
			} catch (NoSuchFieldException e) {
				clazz = clazz.getSuperclass();
			} catch (IllegalAccessException e) {
				return null;
			}
		}

		return null;
	}

    /**
     * 订单模块字段初始化
     */
    public void initOrderFields() {
        initOrderContractFilter();
        initOrderProducts();
    }

    /**
     * 订单产品字段增加金额计算公式，订单增加订单金额计算公式
     */
    private void initOrderProducts() {
        ModuleField orderProducts = selectFieldByInternalKey("orderProducts");
        ModuleFieldBlob orderProductsFieldBlob = moduleFieldBlobMapper.selectByPrimaryKey(orderProducts.getId());
        Map<String, Object> orderProductsField = JSON.parseMap(orderProductsFieldBlob.getProp());

        List<Map<String, Object>> subFields = (List<Map<String, Object>>) orderProductsField.get("subFields");
        Map<String, Object> orderProductAmount = null;
        String orderProductPriceId = null;
        String orderProductNumberId = null;
        for (Map<String, Object> subField : subFields) {
            String subFieldInternalKey = (String) subField.get("internalKey");
            if (Strings.CI.equals(subFieldInternalKey, "orderProductPrice")) {
                orderProductPriceId = (String) subField.get("id");
            }
            if (Strings.CI.equals(subFieldInternalKey, "orderProductNumber")) {
                orderProductNumberId = (String) subField.get("id");
            }
            if (Strings.CI.equals(subFieldInternalKey, "orderProductAmount")) {
                orderProductAmount = subField;
            }
        }
        orderProductAmount.put("formula", getOrderProductAmountFormula(orderProductPriceId, orderProductNumberId));

        orderProductsFieldBlob.setProp(JSON.toJSONString(orderProductsField));
        moduleFieldBlobMapper.updateById(orderProductsFieldBlob);

        ModuleField orderAmount = selectFieldByInternalKey("orderAmount");
        String orderAmountFormulaId = orderProductsField.get("id").toString() + "." + orderProductAmount.get("id").toString();
        ModuleFieldBlob orderAmountFieldBlob = moduleFieldBlobMapper.selectByPrimaryKey(orderAmount.getId());
        Map<String, Object> orderAmountField = JSON.parseMap(orderAmountFieldBlob.getProp());
        orderAmountField.put("formula", getOrderAmountFormula(orderAmountFormulaId));
        orderAmountFieldBlob.setProp(JSON.toJSONString(orderAmountField));
        moduleFieldBlobMapper.updateById(orderAmountFieldBlob);
    }

    private String getOrderAmountFormula(String orderAmountFormulaId) {
        String orderProductAmountFormula = String.format("""
                {
                    "source": "SUM(${%s})",
                    "display": "SUM(产品明细.金额)",
                    "fields": [
                        {
                            "fieldId": "%s",
                            "fieldType": "FORMULA",
                            "numberType": "number"
                        }
                    ],
                    "ir": {
                        "type": "function",
                        "name": "SUM",
                        "args": [
                            {
                                "type": "field",
                                "fieldId": "%s",
                                "name": "产品明细.金额",
                                "fieldType": "FORMULA",
                                "numberType": "number",
                                "startTokenIndex": 2,
                                "endTokenIndex": 2
                            }
                        ]
                    }
                }""", orderAmountFormulaId, orderAmountFormulaId, orderAmountFormulaId);
        return orderProductAmountFormula;
    }

    private String getOrderProductAmountFormula(String orderProductPriceId, String orderProductNumberId) {
        return String.format("""
                {
                     "source": "${%s} * ${%s}",
                     "display": "产品单价 * 数量",
                     "fields": [
                         {
                             "fieldId": "%s",
                             "fieldType": "INPUT_NUMBER",
                             "numberType": "number"
                         },
                         {
                             "fieldId": "%s",
                             "fieldType": "INPUT_NUMBER",
                             "numberType": "number"
                         }
                     ],
                     "ir": {
                         "type": "binary",
                         "operator": "*",
                         "left": {
                             "type": "field",
                             "fieldId": "%s",
                             "name": "产品单价",
                             "fieldType": "INPUT_NUMBER",
                             "numberType": "number",
                             "startTokenIndex": 0,
                             "endTokenIndex": 0
                         },
                         "right": {
                             "type": "field",
                             "fieldId": "%s",
                             "name": "数量",
                             "fieldType": "INPUT_NUMBER",
                             "numberType": "number",
                             "startTokenIndex": 2,
                             "endTokenIndex": 2
                         }
                     }
                 }""", orderProductPriceId, orderProductNumberId, orderProductPriceId, orderProductNumberId, orderProductPriceId, orderProductNumberId);
    }

    /**
     * 订单合同字段增加订单客户过滤条件
     */
    private void initOrderContractFilter() {
        ModuleField orderContract = selectFieldByInternalKey(BusinessModuleField.ORDER_CONTRACT.getKey());
        ModuleField orderCustomer = selectFieldByInternalKey(BusinessModuleField.ORDER_CUSTOMER.getKey());
        ModuleFieldBlob orderContractFieldBlob = moduleFieldBlobMapper.selectByPrimaryKey(orderContract.getId());
        DatasourceField datasourceField = JSON.parseObject(orderContractFieldBlob.getProp(), DatasourceField.class);
        String customerConditionJson = String.format("""
                {
                  "searchMode": "OR",
                  "conditions": [
                    {
                      "leftFieldId": "customerId",
                      "leftFieldType": "DATA_SOURCE",
                      "operator": "IN",
                      "rightFieldId": "%s",
                      "rightFieldCustom": false,
                      "rightFieldCustomValue": "",
                      "rightFieldType": "INPUT"
                    }
                  ]
                }""", orderCustomer.getId());
        datasourceField.setCombineSearch(JSON.parseToMap(customerConditionJson));
        orderContractFieldBlob.setProp(JSON.toJSONString(datasourceField));
        moduleFieldBlobMapper.updateById(orderContractFieldBlob);
    }
}
