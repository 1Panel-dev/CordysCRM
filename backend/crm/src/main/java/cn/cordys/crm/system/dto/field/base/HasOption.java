package cn.cordys.crm.system.dto.field.base;

import java.util.List;

/**
 * 选项字段通用接口 (通用属性)
 *
 * @author song-cc-rock
 */
public interface HasOption {

  /**
   * 获取选项集合
   *
   * @return 选项集合
   */
  List<OptionProp> getOptions();

  /**
   * 设置选项
   *
   * @param options 选项
   */
  void setOptions(List<OptionProp> options);

  /**
   * 获取选项来源
   *
   * @return 选项来源
   */
  String getOptionSource();

  /**
   * 设置选项来源
   *
   * @param optionSource 选项来源
   */
  void setOptionSource(String optionSource);

  /**
   * 设置自定义选项
   *
   * @param customOptions 自定义选项
   */
  void setCustomOptions(List<OptionProp> customOptions);

  /**
   * 获取自定义选项集合
   *
   * @return 选项集合
   */
  List<OptionProp> getCustomOptions();

  /**
   * 获取选项引用ID
   *
   * @return 选项引用ID
   */
  String getRefId();
}
