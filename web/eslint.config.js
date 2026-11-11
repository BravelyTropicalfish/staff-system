// The web app's static check: TypeScript and TSX parsed by typescript-eslint, with the
// JavaScript and TypeScript recommended rule sets. Build output and Playwright reports are
// generated, so they are not linted.
import js from '@eslint/js';
import tseslint from 'typescript-eslint';

export default tseslint.config(
  {
    ignores: ['dist/**', 'node_modules/**', 'test-results/**', 'playwright-report/**'],
  },
  js.configs.recommended,
  ...tseslint.configs.recommended,
  {
    files: ['**/*.{ts,tsx}'],
    languageOptions: {
      ecmaVersion: 2022,
      sourceType: 'module',
    },
    rules: {
      // A screen renders a value the API may not have sent; an explicit `any` there is the
      // honest type, so it is reported as a warning rather than failing the check.
      '@typescript-eslint/no-explicit-any': 'warn',
    },
  },
);
