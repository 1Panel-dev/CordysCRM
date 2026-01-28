package cn.cordys.crm.system.excel.domain;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 合并结果
 *
 * @author song-cc-rock
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MergeResult {

  /** 数据列表 */
  private List<List<Object>> dataList;

  /** 合并的行区域 */
  private List<int[]> mergeRegions;
}
