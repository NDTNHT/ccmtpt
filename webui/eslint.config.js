import js from '@eslint/js';
import vue from 'eslint-plugin-vue';
import globals from 'globals';
export default [
  { ignores: ['dist/**', 'node_modules/**'] },
  js.configs.recommended,
  ...vue.configs['flat/essential'],
  { files: ['**/*.{js,vue}'], languageOptions: { globals: { ...globals.browser, ...globals.node } },
    rules: { 'no-unused-vars': ['error', { argsIgnorePattern: '^_', varsIgnorePattern: '^_' }], 'no-empty': ['error', { allowEmptyCatch: true }], 'vue/multi-word-component-names': 'off' } }
];
