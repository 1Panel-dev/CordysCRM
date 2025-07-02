package io.cordys.crm.clue.constants;

import org.apache.commons.lang3.StringUtils;

/**
 * @Author: jianxing
 * @CreateTime: 2025-03-11  15:53
 */
public enum ClueStatus {

    /**
     * 新建
     */
    NEW("NEW", "新建"),
    /**
     * 跟进中
     */
    FOLLOWING("FOLLOWING", "跟进中"),
    /**
     * 感兴趣
     */
    INTERESTED("INTERESTED", "感兴趣"),
    /**
     * 成功
     */
    SUCCESS("SUCCESS", "成功"),
    /**
     * 失败
     */
    FAIL("FAIL", "失败");

    private final String key;
	private final String name;

	ClueStatus(String key, String name) {
		this.key = key;
        this.name = name;
	}

    public static String getByKey(String key) {
        for (ClueStatus status : ClueStatus.values()) {
            if (StringUtils.equals(key, status.key)) {
                return status.name;
            }
        }
        return null;
    }
}
