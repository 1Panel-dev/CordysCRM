package cn.cordys.aspectj.builder;

import cn.cordys.aspectj.dto.LogDTO;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>用于构建 {@link LogDTO} 的建造者类。</p>
 * <p>通过 {@link Builder} 模式构建 {@link LogDTO} 对象，确保日志数据的完整性和一致性。</p>
 */
@Getter
@Setter
@Builder
public class LogDTOBuilder {

    /**
     * 组织ID
     */
    private String organizationId;

    /**
     * 来源ID
     */
    private String sourceId;

    /**
     * 创建用户
     */
    private String createUser;

    /**
     * 日志类型
     */
    private String type;

    /**
     * 方法名
     */
    private String method;

    /**
     * 模块名
     */
    private String module;

    /**
     * 日志内容
     */
    private String content;

    /**
     * 请求路径
     */
    private String path;

    /**
     * 构建 {@link LogDTO} 对象
     *
     * @return {@link LogDTO} 对象
     */
    public LogDTO getLogDTO() {
        LogDTO logDTO = new LogDTO(organizationId, sourceId, createUser, type, module, content);
        logDTO.setPath(path);
        logDTO.setMethod(method);
        return logDTO;
    }
}
