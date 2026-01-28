package cn.cordys.crm.system.mapper;

import cn.cordys.common.dto.OptionDTO;
import cn.cordys.crm.system.domain.ModuleField;
import java.util.List;
import org.apache.ibatis.annotations.Param;

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

  List<OptionDTO> getSourceOptionsByIds(
      @Param("tableName") String table, @Param("ids") List<String> ids);

  List<OptionDTO> getSourceOptionsByKeywords(
      @Param("tableName") String table, @Param("keywords") List<String> keywords);

  List<OptionDTO> getSourceOptionsByName(
      @Param("tableName") String table,
      @Param("keyword") String keyword,
      @Param("orgId") String orgId);

  List<ModuleField> getModuleField(
      @Param("orgId") String orgId, @Param("formKeys") List<String> formKeys);

  /**
   * 批量更新移动端显示
   *
   * @param ids ID集合
   * @param mobile 移动端显示
   */
  void batchUpdateMobile(@Param("ids") List<String> ids, @Param("mobile") Boolean mobile);

  /**
   * 获取表单字段最大位次
   *
   * @param formId 表单ID
   * @return 位置下标
   */
  Long getMaxFieldPosByFormId(String formId);
}
