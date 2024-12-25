package io.cordys.aspectj.builder;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OperationLogBuilder {
    private String successLogTemplate;
    private String failLogTemplate;
    private String operatorId;
    private String type;
    private String resourceId;
    private String subType;
    private String extra;
}
