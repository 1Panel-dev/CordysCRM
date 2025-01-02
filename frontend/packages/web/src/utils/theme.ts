import Color from 'color';

/**
 * 获取颜色对象的 rgb 色值
 * @param color Color对象
 * @returns 颜色值
 */
export function getRGBinnerVal(color: Color) {
  return color
    .rgb()
    .toString()
    .replace(/rgba?\(|\)/g, '');
}

/**
 * 设置自定义颜色的主题色
 * @param primaryColor 主题色
 */
export function setCustomTheme(primaryColor: string) {
  const styleTag = document.createElement('style');
  styleTag.id = 'CRM-CUSTOM-THEME';
  const primary = new Color(primaryColor);
  const white = Color('#fff');
  const P = primary.toString().replace(/rgba?\(|\)/g, '');
  const P0 = getRGBinnerVal(primary.mix(Color('#000'), 0.15));
  const P1 = getRGBinnerVal(primary.mix(white, 0.15));
  const P2 = getRGBinnerVal(primary.mix(white, 0.3));
  const P3 = getRGBinnerVal(primary.mix(white, 0.4));
  const P4 = getRGBinnerVal(primary.mix(white, 0.7));
  const P5 = getRGBinnerVal(primary.mix(white, 0.8));
  const P6 = getRGBinnerVal(primary.mix(white, 0.9));
  const P7 = getRGBinnerVal(primary.mix(white, 0.95));
  styleTag.innerHTML = `
    :root {
      --primary-0: ${P0};
      --primary-1: ${P1};
      --primary-2: ${P2};
      --primary-3: ${P3};
      --primary-4: ${P4};
      --primary-5: ${P5};
      --primary-6: ${P6};
      --primary-7: ${P7};
      --primary-8: ${P};
    }
  `;
  // 移除之前的 style 标签（如果有）
  const prevStyleTag = document.getElementById('CRM-CUSTOM-THEME');
  if (prevStyleTag) {
    prevStyleTag.remove();
  }
  document.body.appendChild(styleTag);
}

/**
 * 主题重置为默认主题
 */
export function resetTheme() {
  const prevStyleTag = document.getElementById('CRM-CUSTOM-THEME');
  if (prevStyleTag) {
    prevStyleTag.remove();
  }
}

export default {};
