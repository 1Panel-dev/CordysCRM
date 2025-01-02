import type { GlobalThemeOverrides } from 'naive-ui';

export const themeOverrides: GlobalThemeOverrides = {
  common: {
    borderRadiusSmall: '4px',
    primaryColor: 'var(--primary-8)', // 主题品牌色
    primaryColorHover: 'var(--primary-1)', // 主题hover颜色
    primaryColorPressed: 'var(--primary-0)', // 主题按下颜色
    primaryColorSuppl: 'var(--primary-8)', // 表单输入激活状态下展示主色扩展
    // info颜色
    infoColor: 'var(--info-blue)',
    infoColorHover: 'var(--info-1)', // info颜色悬浮
    infoColorPressed: 'var(--info-0)', // info颜色激活
    infoColorSuppl: 'var(--info-blue)', // info颜色表单输入激活状态下展示主色扩展
    //  success颜色
    successColor: 'var(--success-green)',
    successColorHover: 'var(--success-1)',
    successColorPressed: 'var(--success-0)',
    successColorSuppl: 'var(--success-green)',
    //   warning颜色
    warningColor: 'var(--warning-yellow)',
    warningColorHover: 'var(--warning-1)',
    warningColorPressed: 'var(--warning-0)',
    warningColorSuppl: 'var(--warning-yellow)',
    //   error颜色
    errorColor: 'var(--error-red)',
    errorColorHover: 'var(--error-1)',
    errorColorPressed: 'var(--error-0)',
    errorColorSuppl: 'var(--error-red)',
    // 文本的颜色
    textColorBase: 'var(--text-n1)',
    textColorDisabled: 'var(--text-n6)',
    placeholderColor: 'var(--text-n4)',
    placeholderColorDisabled: 'var(--text-n6)',
    // icon颜色
    iconColor: 'var(--text-n4)',
    iconColorHover: 'var(--text-n1)',
    iconColorDisabled: 'var(--text-n6)',
    dividerColor: 'var(--text-n8)',
    closeIconColor: 'var(--text-n2)',
    closeIconColorHover: 'var(--text-n2)',
    closeColorHover: 'var(--text-n2)',
    closeColorPressed: 'var(--text-n2)',
    scrollbarColor: 'rgba(0, 0, 0, 0.25)',
    scrollbarColorHover: 'rgba(0, 0, 0, 0.4)',
    popoverColor: 'var(--text-n10)',
    tableColor: 'var(--text-n10)',
    cardColor: 'var(--text-n10)',
    modalColor: 'var(--text-n10)',
    bodyColor: 'var(--text-n10)',
    invertedColor: 'rgb(0, 20, 40)',
    inputColor: 'var(--text-n10)',
  },
  Button: {
    // 因只能配置一种颜色，禁用默认border边框
    border: 'none',
    borderHover: 'var(--text-n7)',
    borderPressed: 'var(--text-n8)',
    borderFocus: 'var(--text-n8)',
    borderDisabled: 'var(--text-n9)',
    textColor: 'var(--text-n1)',
    textColorDisabled: 'var(--text-n6)',
    // 主题主要按钮
    borderPrimary: '1px solid var(--primary-8)',
    borderHoverPrimary: '1px solid var(--primary-1)',
    borderPressedPrimary: '1px solid var(--primary-0)',
    borderFocusPrimary: '1px solid var(--primary-0)',
    borderDisabledPrimary: '1px solid var(--primary-4)',
    textColorFocusPrimary: 'var(--text-n9)',
    colorPrimary: 'var(--primary-8)',
    colorHoverPrimary: 'var(--primary-1)',
    colorPressedPrimary: 'var(--primary-0)',
    colorFocusPrimary: 'var(--primary-0)',
    colorDisabledPrimary: 'var(--primary-4)',
    textColorPrimary: 'var(--text-n9)',
    textColorHoverPrimary: 'var(--text-n9)',
    textColorPressedPrimary: 'var(--text-n9)',
    textColorDisabledPrimary: 'var(--text-n9)',
    // 次要按钮
    colorSecondary: 'var(--text-n8)',
    colorSecondaryHover: 'var(--text-n9)',
    colorSecondaryPressed: 'var(--text-n7)',
    textColorHover: 'var(--text-n9)',
    textColorSuccess: 'var(--text-n9)',
    textColorHoverSuccess: 'var(--text-n9)',
    textColorPressedSuccess: 'var(--text-n9)',
    textColorFocusSuccess: 'var(--text-n9)',
    textColorDisabledSuccess: 'var(--text-n9)',
    textColorTextDisabledSuccess: 'var(--success-3)',
    textColorWarning: 'var(--text-n9)',
    textColorHoverWarning: 'var(--text-n9)',
    textColorPressedWarning: 'var(--text-n9)',
    textColorFocusWarning: 'var(--text-n9)',
    textColorDisabledWarning: 'var(--text-n9)',
    textColorTextDisabledWarning: 'var(--warning-3)',
    textColorError: 'var(--text-n9)',
    textColorHoverError: 'var(--text-n9)',
    textColorPressedError: 'var(--text-n9)',
    textColorFocusError: 'var(--text-n9)',
    textColorDisabledError: 'var(--text-n9)',
    textColorTextDisabledError: 'var(--error-3)',
    // ghost主题按钮无背景
    textColorGhostPrimary: 'var(--primary-8)',
    textColorGhostHoverPrimary: 'var(--primary-1)',
  },
};

export default {};
