package cn.cordys.common.dto;

/**
 * 客户详情关联列表中按负责人判断读取来源的记录。
 */
public interface CustomerTabRow {

    String getOwner();

    void setCollaborationOnly(Boolean collaborationOnly);
}
