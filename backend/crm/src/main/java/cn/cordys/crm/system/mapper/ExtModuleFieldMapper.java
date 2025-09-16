package cn.cordys.crm.system.mapper;

import cn.cordys.common.dto.OptionDTO;
import cn.cordys.crm.system.domain.ModuleField;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ExtModuleFieldMapper {

    /**
     * 根据ID集合批量删除字段
     *
     * @param ids ID集合
     */
    void deleteByIds(@Param("ids") List<String> ids);

    /**
     * 根据ID集合批量删除字段属性
     *
     * @param ids ID集合
     */
    void deletePropByIds(@Param("ids") List<String> ids);

    List<OptionDTO> getSourceOptionsByIds(@Param("tableName") String table, @Param("ids") List<String> ids);

    List<String> resolveIdsByName(@Param("tableName") String table, @Param("names") List<String> names);

    List<OptionDTO> getSourceOptionsByName(@Param("tableName") String table, @Param("keyword") String keyword, @Param("orgId") String orgId);

    List<ModuleField> getModuleField(@Param("orgId") String orgId, @Param("formKeys") List<String> formKeys);
}
