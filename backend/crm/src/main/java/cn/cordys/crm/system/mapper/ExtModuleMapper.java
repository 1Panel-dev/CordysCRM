package cn.cordys.crm.system.mapper;

import cn.cordys.crm.system.domain.Module;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ExtModuleMapper {

    /**
     * 模块区间上移
     *
     * @param start 开始
     * @param end   结束
     */
    void moveUpModule(@Param("start") Long start, @Param("end") Long end);

    /**
     * 模块区间下移
     *
     * @param start 开始
     * @param end   结束
     */
    void moveDownModule(@Param("start") Long start, @Param("end") Long end);

    /**
     * 根据模块key查询模块集合按照pos属性倒叙排序
     */
    List<Module> selectModuleListByKeyOrderPosDesc(@Param("orgId") String orgId, @Param("moduleKey") String moduleKey);

}
