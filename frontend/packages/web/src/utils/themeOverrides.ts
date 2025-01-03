import type { GlobalThemeOverrides } from 'naive-ui';

function getLessVariableValue(variableName: string): string {
  return getComputedStyle(document.documentElement).getPropertyValue(variableName).trim();
}
/**
 * @returns GlobalThemeOverrides 主题全局配置
 * 注 如果需要定义相关组件的颜色配置，需要使用getLessVariableValue包裹
 *    否则部分组件的内部源码不识别less变量，使用组件可能会导致源码报错
 */
export function getThemeOverrides(): GlobalThemeOverrides {
  return {
    common: {
      borderRadiusSmall: '4px',
      primaryColor: getLessVariableValue('--primary-8'), // 主题品牌色
      primaryColorHover: getLessVariableValue('--primary-1'), // 主题hover颜色
      primaryColorPressed: getLessVariableValue('--primary-0'), // 主题按下颜色
      primaryColorSuppl: getLessVariableValue('--primary-8'), // 表单输入激活状态下展示主色扩展
      // info颜色
      infoColor: getLessVariableValue('--info-blue'),
      infoColorHover: getLessVariableValue('--info-1'), // info颜色悬浮
      infoColorPressed: getLessVariableValue('--info-0'), // info颜色激活
      infoColorSuppl: getLessVariableValue('--info-blue'), // info颜色表单输入激活状态下展示主色扩展
      // success颜色
      successColor: getLessVariableValue('--success-green'),
      successColorHover: getLessVariableValue('--success-1'),
      successColorPressed: getLessVariableValue('--success-0'),
      successColorSuppl: getLessVariableValue('--success-green'),
      // warning颜色
      warningColor: getLessVariableValue('--warning-yellow'),
      warningColorHover: getLessVariableValue('--warning-1'),
      warningColorPressed: getLessVariableValue('--warning-0'),
      warningColorSuppl: getLessVariableValue('--warning-yellow'),
      // error颜色
      errorColor: getLessVariableValue('--error-red'),
      errorColorHover: getLessVariableValue('--error-1'),
      errorColorPressed: getLessVariableValue('--error-0'),
      errorColorSuppl: getLessVariableValue('--error-red'),
      // 文本的颜色
      textColorBase: getLessVariableValue('--text-n1'),
      textColorDisabled: getLessVariableValue('--text-n6'),
      placeholderColor: getLessVariableValue('--text-n4'),
      placeholderColorDisabled: getLessVariableValue('--text-n6'),
      // icon颜色
      iconColor: getLessVariableValue('--text-n4'),
      iconColorHover: getLessVariableValue('--text-n1'),
      iconColorDisabled: getLessVariableValue('--text-n6'),
      dividerColor: getLessVariableValue('--text-n8'),
      closeIconColor: getLessVariableValue('--text-n2'),
      closeIconColorHover: getLessVariableValue('--text-n2'),
      closeColorHover: getLessVariableValue('--text-n2'),
      closeColorPressed: getLessVariableValue('--text-n2'),
      scrollbarColor: 'rgba(0, 0, 0, 0.25)',
      scrollbarColorHover: 'rgba(0, 0, 0, 0.4)',
      popoverColor: getLessVariableValue('--text-n10'),
      tableColor: getLessVariableValue('--text-n10'),
      cardColor: getLessVariableValue('--text-n10'),
      modalColor: getLessVariableValue('--text-n10'),
      bodyColor: getLessVariableValue('--text-n10'),
      invertedColor: 'rgb(0, 20, 40)',
      inputColor: getLessVariableValue('--text-n10'),
      // 盒子投影设置
      boxShadow1: '0 4px 10px -1px rgba(100, 103, 103, .15)', // 基础投影
      boxShadow2: '0 4px 15px 2px rgba(100, 103, 103, .1)', // 中层投影
      boxShadow3: '0 6px 35px 6px rgba(100, 103, 103, .1)', // 上层投影
    },
    Button: {
      // 因只能配置一种颜色，禁用默认border边框
      border: 'none',
      borderHover: getLessVariableValue('--text-n7'),
      borderPressed: getLessVariableValue('--text-n8'),
      borderFocus: getLessVariableValue('--text-n8'),
      borderDisabled: getLessVariableValue('--text-n9'),
      textColor: getLessVariableValue('--text-n1'),
      textColorDisabled: getLessVariableValue('--text-n6'),
      // 主题主要按钮
      borderPrimary: `1px solid ${getLessVariableValue('--primary-8')}`,
      borderHoverPrimary: `1px solid ${getLessVariableValue('--primary-1')}`,
      borderPressedPrimary: `1px solid ${getLessVariableValue('--primary-0')}`,
      borderFocusPrimary: `1px solid ${getLessVariableValue('--primary-0')}`,
      borderDisabledPrimary: `1px solid ${getLessVariableValue('--primary-4')}`,
      textColorFocusPrimary: getLessVariableValue('--text-n9'),
      colorPrimary: getLessVariableValue('--primary-8'),
      colorHoverPrimary: getLessVariableValue('--primary-1'),
      colorPressedPrimary: getLessVariableValue('--primary-0'),
      colorFocusPrimary: getLessVariableValue('--primary-0'),
      colorDisabledPrimary: getLessVariableValue('--primary-4'),
      textColorPrimary: getLessVariableValue('--text-n9'),
      textColorHoverPrimary: getLessVariableValue('--text-n9'),
      textColorPressedPrimary: getLessVariableValue('--text-n9'),
      textColorDisabledPrimary: getLessVariableValue('--text-n9'),
      // 次要按钮
      colorSecondary: getLessVariableValue('--text-n8'),
      colorSecondaryHover: getLessVariableValue('--text-n9'),
      colorSecondaryPressed: getLessVariableValue('--text-n7'),
      textColorHover: getLessVariableValue('--text-n9'),
      textColorSuccess: getLessVariableValue('--text-n9'),
      textColorHoverSuccess: getLessVariableValue('--text-n9'),
      textColorPressedSuccess: getLessVariableValue('--text-n9'),
      textColorFocusSuccess: getLessVariableValue('--text-n9'),
      textColorDisabledSuccess: getLessVariableValue('--text-n9'),
      textColorTextDisabledSuccess: getLessVariableValue('--success-3'),
      textColorWarning: getLessVariableValue('--text-n9'),
      textColorHoverWarning: getLessVariableValue('--text-n9'),
      textColorPressedWarning: getLessVariableValue('--text-n9'),
      textColorFocusWarning: getLessVariableValue('--text-n9'),
      textColorDisabledWarning: getLessVariableValue('--text-n9'),
      textColorTextDisabledWarning: getLessVariableValue('--warning-3'),
      textColorError: getLessVariableValue('--text-n9'),
      textColorHoverError: getLessVariableValue('--text-n9'),
      textColorPressedError: getLessVariableValue('--text-n9'),
      textColorFocusError: getLessVariableValue('--text-n9'),
      textColorDisabledError: getLessVariableValue('--text-n9'),
      textColorTextDisabledError: getLessVariableValue('--error-3'),
      // ghost主题按钮无背景
      textColorGhostPrimary: getLessVariableValue('--primary-8'),
      textColorGhostHoverPrimary: getLessVariableValue('--primary-1'),
      // 按钮尺寸大小字体设置
      fontSizeSmall: '12px',
      fontSizeMedium: '14px',
      fontSizeLarge: '16px',
    },
  };
}

export default {};
