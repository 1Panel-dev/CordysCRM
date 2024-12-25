import { defineConfig } from '@rsbuild/core';
import { pluginLess } from '@rsbuild/plugin-less';
import { pluginVue } from '@rsbuild/plugin-vue';
import { VantResolver } from '@vant/auto-import-resolver';
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
  },
});
