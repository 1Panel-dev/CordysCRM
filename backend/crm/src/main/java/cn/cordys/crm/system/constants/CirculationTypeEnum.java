package cn.cordys.crm.system.constants;

public enum CirculationTypeEnum {
    NORMAL("NORMAL"),
    ADVANCED("ADVANCED");

    private final String name;

    CirculationTypeEnum(String name) {
        this.name = name;
    }
}