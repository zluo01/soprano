import js from '@eslint/js';
import pluginQuery from '@tanstack/eslint-plugin-query';
import pluginRouter from '@tanstack/eslint-plugin-router';
import eslintConfigPrettier from 'eslint-config-prettier';
import pluginPromise from 'eslint-plugin-promise';
import pluginReact from 'eslint-plugin-react';
import pluginReactHooks from 'eslint-plugin-react-hooks';
import pluginReactRefresh from 'eslint-plugin-react-refresh';
// import tailwind from 'eslint-plugin-tailwindcss';
import globals from 'globals';
import ts from 'typescript-eslint';

export default [
  { files: ['**/*.{ts,tsx}'] },
  {
    languageOptions: {
      ecmaVersion: 2020,
      globals: globals.browser,
    },
  },
  js.configs.recommended,
  ...ts.configs.recommended,
  // ...tailwind.configs['flat/recommended'],
  pluginReact.configs.flat.recommended,
  pluginPromise.configs['flat/recommended'],
  ...pluginRouter.configs['flat/recommended'],
  ...pluginQuery.configs['flat/recommended'],
  eslintConfigPrettier,
  {
    plugins: {
      'react-hooks': pluginReactHooks,
      'react-refresh': pluginReactRefresh,
    },
    rules: {
      'react/prop-types': 'off',
      'react/react-in-jsx-scope': 'off',
      '@typescript-eslint/no-explicit-any': 'off',
      '@typescript-eslint/no-array-constructor': 'error',
      '@typescript-eslint/no-empty-function': ['error'],
      '@typescript-eslint/no-unused-vars': [
        2,
        {
          argsIgnorePattern: '^_',
        },
      ],
      'no-console': 'error',
      ...pluginReactHooks.configs.recommended.rules,
      'react-refresh/only-export-components': [
        'warn',
        { allowConstantExport: true },
      ],
    },
  },
  {
    ignores: ['dist/*', '*.config.*js'],
  },
];
