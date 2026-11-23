const { defineConfig, globalIgnores } = require('eslint/config');
const { FlatCompat } = require('@eslint/eslintrc');
const js = require('@eslint/js');
const pluginVue = require('eslint-plugin-vue');

const compat = new FlatCompat({
  baseDirectory: __dirname,
  recommendedConfig: js.configs.recommended,
  allConfig: js.configs.all,
});
const legacyConfig = require('./.eslintrc.cjs');
const compatibleConfig = {
  ...legacyConfig,
  extends: legacyConfig.extends.filter((config) => config !== 'plugin:vue/vue3-recommended'),
};

module.exports = defineConfig([
  ...compat.config(compatibleConfig),
  ...pluginVue.configs['flat/recommended'],
  { rules: legacyConfig.rules },
  globalIgnores(['*.json', 'src/**/*.json', '**/dist', '**/postcss.config.js', '**/*.md']),
]);
