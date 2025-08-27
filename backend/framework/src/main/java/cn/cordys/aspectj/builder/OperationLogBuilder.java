package cn.cordys.aspectj.builder;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OperationLogBuilder {
    private String resourceId;
    private String resourceName;
    private String operatorId;
    private String type;
    private String subType;
    private String extra;
}
