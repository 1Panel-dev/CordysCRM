import { defineConfig } from '@rsbuild/core';
import { pluginLess } from '@rsbuild/plugin-less';
import { pluginVue } from '@rsbuild/plugin-vue';
import { VantResolver } from '@vant/auto-import-resolver';
import postcssPxtorem from 'postcss-pxtorem';
import AutoImport from 'unplugin-auto-import/rspack';
import Components from 'unplugin-vue-components/rspack';

export default defineConfig({
  plugins: [pluginVue(), pluginLess()],
  tools: {
    rspack: {
      plugins: [
        AutoImport({
          dts: 'src/auto-import.d.ts',
          include: [
            /\.[tj]sx?$/, // .ts, .tsx, .js, .jsx
            /\.vue$/,
            /\.vue\?vue/, // .vue
          ],
          imports: ['vue'],
          resolvers: [VantResolver()],
          eslintrc: {
            enabled: true,
          },
        }),
        Components({
          resolvers: [VantResolver()],
        }),
      ],
    },
    postcss: {
      postcssOptions: {
        plugins: [
          postcssPxtorem({
            rootValue: 16, // 基准值
            propList: ['*'], // 需要转换的属性列表
            selectorBlackList: [], // 忽略的选择器
            replace: true, // 替换而不是添加回退
            mediaQuery: false, // 允许在媒体查询中转换px
            minPixelValue: 0, // 最小的转换数值
          }),
        ],
      },
    },
  },
});
