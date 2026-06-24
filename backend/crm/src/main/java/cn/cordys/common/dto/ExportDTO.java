package cn.cordys.common.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Data
@Builder
public class ExportDTO {
    private String userId;
    private String orgId;
    /**
     * {@link cn.cordys.crm.system.constants.ExportConstants.ExportType}
     */
    private String exportType;
    private String logModule;
    private Locale locale;
    private String fileName;
    private List<ExportHeadDTO> headList;
    private DeptDataPermissionDTO deptDataPermission;
    private BasePageRequest pageRequest;
    private List<String> selectIds;
    private ExportSelectRequest selectRequest;
	private String formKey;
	/**
	 * 导出字段参数 (通用参数无需设置)
	 */
	private ExportFieldParam exportFieldParam;
	private List<String> mergeHeads;
	private List<FieldExportMeta> exportMetas;
	/**
	 * 扩展参数，用于存储导出过程中需要缓存的数据（如阶段配置等）
	 */
	@Builder.Default
	private Map<String, Object> extraParams = new java.util.HashMap<>();
}
