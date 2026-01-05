package cn.cordys.crm.system.constants;

public class ExportConstants {

    public enum ExportType {
        CUSTOMER, CLUE, OPPORTUNITY, CUSTOMER_POOL, CLUE_POOL, CUSTOMER_CONTACT, CONTRACT, CONTRACT_PAYMENT_PLAN, CONTRACT_INVOICE, PRODUCT_PRICE, BUSINESS_TITLE
    }

    public enum ExportStatus {
        PREPARED, STOP, SUCCESS, ERROR
    }
}
