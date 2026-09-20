package cn.cordys.crm.system.constants;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum SyncCycleCron {
    HOUR("0 0 * * * ?"),
    SIX_HOUR("0 0 0/6 * * ?"),
    TWELVE_HOUR("0 0 0/12 * * ?"),
    DAY("0 0 1 * * ?"),
    MONDAY("0 0 1 ? * MON"),
    TUESDAY("0 0 1 ? * TUE"),
    WEDNESDAY("0 0 1 ? * WED"),
    THURSDAY("0 0 1 ? * THU"),
    FRIDAY("0 0 1 ? * FRI"),
    SATURDAY("0 0 1 ? * SAT"),
    SUNDAY("0 0 1 ? * SUN");

    private final String cron;

    SyncCycleCron(String cron) {
        this.cron = cron;
    }

    public static String getCron(String type) {
        return Arrays.stream(values())
                .filter(item -> item.name().equalsIgnoreCase(type))
                .map(SyncCycleCron::getCron)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("不支持的同步周期类型：" + type));
    }

    /**
     * 根据 Cron 获取枚举
     */
    public static SyncCycleCron getByCron(String cron) {
        return Arrays.stream(values())
                .filter(item -> item.cron.equals(cron))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "不支持的 Cron 表达式：" + cron));
    }
}
