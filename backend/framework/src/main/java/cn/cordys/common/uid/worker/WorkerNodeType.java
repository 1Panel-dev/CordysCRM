package cn.cordys.common.uid.worker;

import cn.cordys.common.uid.utils.ValuedEnum;

/**
 * Worker node types.
 * <ul>
 * <li>CONTAINER: Such as Docker</li>
 * <li>ACTUAL: Actual machine</li>
 * </ul>
 */
public enum WorkerNodeType implements ValuedEnum<Integer> {

    CONTAINER(1), ACTUAL(2);

    /**
     * Worker node type value
     */
    private final Integer type;

    /**
     * Constructor with field of type
     */
    WorkerNodeType(Integer type) {
        this.type = type;
    }

    @Override
    public Integer value() {
        return type;
    }

}
