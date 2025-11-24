package cn.cordys.common.constants;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author song-cc-rock
 */

@Getter
public enum FormKey {

    /**
     * 线索
     */
    CLUE("clue"),
    /**
     * 客户
     */
    CUSTOMER("customer"),
    /**
     * 联系人
     */
    CONTACT("contact"),
    /**
     * 跟进记录
     */
    FOLLOW_RECORD("record"),
    /**
     * 跟进计划
     */
    FOLLOW_PLAN("plan"),
    /**
     * 商机
     */
    OPPORTUNITY("opportunity"),
    /**
     * 产品
     */
    PRODUCT("product"),
	/**
	 * 价格
	 */
	PRICE("price"),
	/**
	 * 报价单
	 */
	QUOTATION("quotation"),
    /**
     * 合同
     */
    CONTRACT("contract");

    private final String key;

    FormKey(String key) {
        this.key = key;
    }

	public static List<String> allKeys() {
		return Arrays.stream(FormKey.values()).map(FormKey::getKey).collect(Collectors.toList());
	}
}
