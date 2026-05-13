package cn.cordys.crm.approval.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddSignSortInfo {

	/** 根任务ID（同一加签链的根节点） */
	private String rootTaskId;

	/** 排序（越小越靠前） */
	private Long sort;
}
