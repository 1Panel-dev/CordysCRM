import { VantResolver } from '@vant/auto-import-resolver';
import vue from '@vitejs/plugin-vue';
import vueJsx from '@vitejs/plugin-vue-jsx';
import { resolve } from 'path';
import postcssPxtorem from 'postcss-pxtorem';
import AutoImport from 'unplugin-auto-import/vite';
import Components from 'unplugin-vue-components/vite';
import { defineConfig } from 'vite';
import { createSvgIconsPlugin } from 'vite-plugin-svg-icons';
import vueSetupExtend from 'vite-plugin-vue-setup-extend';
import svgLoader from 'vite-svg-loader';

export default defineConfig({
  base: './',
  plugins: [
    vue(),
    vueJsx(),
    vueSetupExtend(),
    svgLoader({ svgoConfig: {} }),
    createSvgIconsPlugin({
      // 指定需要缓存的图标文件夹
      iconDirs: [resolve(process.cwd(), 'src/assets/svg'), resolve(process.cwd(), 'public/images')], // 与本地储存地址一致
      // 指定symbolId格式
      symbolId: 'icon-[name]',
    }),
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
  resolve: {
    alias: [
      {
        find: '@',
        replacement: resolve(__dirname, '../src'),
      },
      {
        find: 'vue-i18n',
        replacement: 'vue-i18n/dist/vue-i18n.esm-bundler.js', // 解决 vue-i18n 依赖包报错
      },
      {
        find: 'vue',
        replacement: 'vue/dist/vue.esm-bundler.js', // compile template
      },
    ],
    extensions: ['.ts', '.js', '.jsx', '.tsx', '.json', '.vue', '.mjs'],
  },
  define: {
    'process.env': {},
    // 定义特性标志
    '__VUE_OPTIONS_API__': true,
    '__VUE_PROD_DEVTOOLS__': false,
    // 设置hydration不匹配详细信息的标志
    '__VUE_PROD_HYDRATION_MISMATCH_DETAILS__': true,
  },
  css: {
    postcss: {
      plugins: [
        postcssPxtorem({
          rootValue: 16, // 基准值，1rem
          propList: ['*'], // 需要转换的属性，`*` 表示所有属性都转换
          unitPrecision: 5, // 转换后保留的小数位数
          selectorBlackList: ['.no-rem'], // 忽略的选择器，例如 `.no-rem` 类名的样式不会被转换
          replace: true, // 替换规则，而不是添加回退
          mediaQuery: false, // 是否允许在媒体查询中转换
          minPixelValue: 2, // 小于或等于 2px 的值不转换为 rem
        }),
      ],
    },
    preprocessorOptions: {
      less: {
        modifyVars: {
          hack: `true; @import (reference) "${resolve('src/assets/style/var.less')}";`,
        },
        javascriptEnabled: true,
      },
    },
  },
});
