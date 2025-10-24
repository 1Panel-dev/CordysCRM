package cn.cordys.crm.system.mapper;

import java.util.List;

public interface ExtAttachmentMapper {

    /**
     * 通过附件名称获取附件ID列表
     *
     * @param names 名称列表
     *
     * @return 附件ID列表
     */
    List<String> getAttachmentIdsByNames(List<String> names);
}
