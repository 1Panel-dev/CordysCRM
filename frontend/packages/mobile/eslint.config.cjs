const { defineConfig, globalIgnores } = require('eslint/config');

const pluginVue = require('eslint-plugin-vue');
const parser = require('vue-eslint-parser');
const globals = require('globals');
const simpleImportSort = require('eslint-plugin-simple-import-sort');

const { fixupConfigRules } = require('@eslint/compat');

const js = require('@eslint/js');

const { FlatCompat } = require('@eslint/eslintrc');

const compat = new FlatCompat({
  baseDirectory: __dirname,
  recommendedConfig: js.configs.recommended,
  allConfig: js.configs.all,
});
const path = require('path');

module.exports = defineConfig([
  {
    languageOptions: {
      parser,
      sourceType: 'module',
      ecmaVersion: 2020,

      parserOptions: {
        parser: '@typescript-eslint/parser',

        ecmaFeatures: {
          jsx: true,
        },
      },

      globals: {
        ...globals.browser,
        ...globals.node,
        NodeJS: 'readonly',
      },
    },

    plugins: {
      'vue': pluginVue,
      'simple-import-sort': simpleImportSort,
    },

    extends: fixupConfigRules(
      compat.extends(
        'airbnb-base',
        'plugin:@typescript-eslint/recommended',
        'plugin:import/recommended',
        'plugin:import/typescript',
        'plugin:prettier/recommended',
        './.eslintrc-auto-import.json'
      )
    ),

    settings: {
      'import/resolver': {
        typescript: {
          project: path.resolve(__dirname, './tsconfig.json'),
        },
      },
    },

    rules: {
      'prettier/prettier': 1,
      'vue/require-default-prop': 0,
      'vue/singleline-html-element-content-newline': 0,
      'vue/max-attributes-per-line': 0,
      'vue/custom-event-name-casing': [2, 'camelCase'],
      'vue/no-v-text': 1,
      'vue/padding-line-between-blocks': 1,
      'vue/require-direct-export': 1,
      'vue/multi-word-component-names': 0,
      '@typescript-eslint/ban-ts-comment': 0,
      '@typescript-eslint/no-unused-vars': 1,
      '@typescript-eslint/no-empty-function': 1,
      '@typescript-eslint/no-explicit-any': 0,
      '@typescript-eslint/no-duplicate-enum-values': 0,
      'consistent-return': 'off',
      'vue/return-in-computed-property': ['off'],
      'vue/no-side-effects-in-computed-properties': 'off',

      'import/no-unresolved': [
        'error',
        {
          ignore: ['^@lib/shared'],
        },
      ],

      'import/extensions': [
        2,
        'ignorePackages',
        {
          js: 'never',
          jsx: 'never',
          ts: 'never',
          tsx: 'never',
        },
      ],

      'no-debugger': 2,
      'no-param-reassign': 0,
      'prefer-regex-literals': 0,
      'import/no-extraneous-dependencies': 0,
      'import/no-cycle': 'off',
      'import/order': 'off',
      'class-methods-use-this': 'off',
      'global-require': 0,
      'no-plusplus': 'off',
      'no-underscore-dangle': 'off',
      'vue/attributes-order': 1,
      'simple-import-sort/exports': 'error',
      'no-case-declarations': 'off',

      'simple-import-sort/imports': [
        'error',
        {
          groups: [
            [
              '^vue$',
              '^vue-router$',
              '^vue-i18n$',
              '^pinia$',
              '^@vueuse/core$',
              '^vant$',
              '^@vicons/ionicons5$',
              '^lodash-es$',
              '^axios$',
              '^dayjs$',
              '^jsencrypt$',
              '^echarts$',
              '^localforage$',
            ],
            ['.*/assets/.*', '^@/assets$'],
            ['^@lib/shared/.*'],
            ['^@/components/pure/.*', '^@/components/business/.*', '.*\\.vue$'],
            [
              '^@/api($|/.*)',
              '^@/config($|/.*)',
              '^@/directive($|/.*)',
              '^@/hooks($|/.*)',
              '^@/locale($|/.*)',
              '^@/router($|/.*)',
              '^@/store($|/.*)',
              '^@/utils($|/.*)',
            ],
            ['^@/models($|/.*)', '^@/enums($|/.*)'],
            ['^type'],
          ],
        },
      ],
    },
  },
  {
    files: ['src/enums/**/*.ts'],

    rules: {
      'no-shadow': 'off',
    },
  },
  globalIgnores([
    '*.json',
    'src/**/*.json',
    '**/dist',
    '**/postcss.config.js',
    '**/*.md',
    'src/assets/icon-font/iconfont.js',
    'src/assets/fonts/AlibabaPuHuiTi-3-55-Regular-normal.js',
  ]),
]);
