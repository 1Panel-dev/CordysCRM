package cn.cordys.crm.system.excel.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 合并结果
 * @author song-cc-rock
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MergeResult {

	/**
	 * 数据列表
	 */
	private List<List<Object>> dataList;
	/**
	 * 合并的行区域
	 */
	private List<int[]> mergeRegions;

	/**
	 * 实际合并处理的行数
	 */
	private int handleCount;

	/**
	 * 查询到的原始记录数（过滤前），用于判断是否还有下一页
	 * 当此值小于每页大小时，才表示没有更多数据
	 */
	@Builder.Default
	private int queryCount = 0;
}
