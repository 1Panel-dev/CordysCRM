package cn.cordys.crm.system.excel.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

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
}
