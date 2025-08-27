package cn.cordys.crm.system.excel.handler;

import cn.idev.excel.metadata.Head;
import cn.idev.excel.write.style.column.AbstractHeadColumnWidthStyleStrategy;

public class CustomHeadColWidthStyleStrategy extends AbstractHeadColumnWidthStyleStrategy {

	@Override
	protected Integer columnWidth(Head head, Integer columnIndex) {
		String headName = head.getHeadNameList().getFirst();
		int length = getTextWidth(headName);
		return Math.min(length + 4, 50);
	}

	/**
	 * 处理表头宽度
	 * @param text 文本
	 * @return 宽度
	 */
	private int getTextWidth(String text) {
		int width = 0;
		for (char c : text.toCharArray()) {
			width += (c >= 0x4e00 && c <= 0x9fa5) ? 2 : 1;
		}
		return width;
	}
}
