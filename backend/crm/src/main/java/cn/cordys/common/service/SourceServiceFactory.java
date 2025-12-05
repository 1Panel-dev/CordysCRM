package cn.cordys.common.service;

import cn.cordys.common.util.CommonBeanFactory;
import cn.cordys.common.util.LogUtils;
import cn.cordys.crm.clue.service.ClueService;
import cn.cordys.crm.contract.service.ContractService;
import cn.cordys.crm.customer.service.CustomerContactService;
import cn.cordys.crm.customer.service.CustomerService;
import cn.cordys.crm.opportunity.service.OpportunityQuotationService;
import cn.cordys.crm.opportunity.service.OpportunityService;
import cn.cordys.crm.product.service.ProductPriceService;
import cn.cordys.crm.product.service.ProductService;
import cn.cordys.crm.system.constants.FieldSourceType;

import java.util.HashMap;
import java.util.Map;

/**
 * @author song-cc-rock
 */
public class SourceServiceFactory {

	private static final Map<FieldSourceType, Object> SERVICE_MAP = new HashMap<>();

	static {
		SERVICE_MAP.put(FieldSourceType.CLUE, CommonBeanFactory.getBean(ClueService.class));
		SERVICE_MAP.put(FieldSourceType.CUSTOMER, CommonBeanFactory.getBean(CustomerService.class));
		SERVICE_MAP.put(FieldSourceType.OPPORTUNITY, CommonBeanFactory.getBean(OpportunityService.class));
		SERVICE_MAP.put(FieldSourceType.CONTACT, CommonBeanFactory.getBean(CustomerContactService.class));
		SERVICE_MAP.put(FieldSourceType.PRODUCT, CommonBeanFactory.getBean(ProductService.class));
		SERVICE_MAP.put(FieldSourceType.PRICE, CommonBeanFactory.getBean(ProductPriceService.class));
		SERVICE_MAP.put(FieldSourceType.QUOTATION, CommonBeanFactory.getBean(OpportunityQuotationService.class));
		SERVICE_MAP.put(FieldSourceType.CONTRACT, CommonBeanFactory.getBean(ContractService.class));
	}

	/**
	 * 根据来源类型获取对应的 Service
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getService(FieldSourceType type) {
		return (T) SERVICE_MAP.get(type);
	}

	/**
	 * 根据数据源类型和入参获取数据对象
	 */
	public static Object getById(FieldSourceType type, Object id) {
		Object service = getService(type);
		if (service == null || !(id instanceof String)) {
			LogUtils.error("数据源类型{}有误, 或参数值{}传递有误", new Object[]{type.name(), id});
			return null;
		}
		try {
			return service.getClass().getMethod("get", String.class).invoke(service, id.toString());
		} catch (Exception e) {
			LogUtils.error("获取数据源详情异常, {}", e);
			return null;
		}
	}
}