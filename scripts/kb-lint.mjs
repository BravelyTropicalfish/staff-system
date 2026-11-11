/**
 * Knowledge-base consistency check for `docs/knowledge/`.
 *
 * It is the compile step `docs/knowledge/_SCHEMA.md` names: every entry carries a title, and every
 * relative link in `_INDEX.md` resolves. It is deliberately read-only — the curated index table is
 * hand-maintained, so regenerating it here would discard the descriptions the table exists for.
 */
import { readdirSync, readFileSync, statSync, existsSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const knowledge = join(root, 'docs', 'knowledge');
const skipped = new Set(['_INDEX.md', '_LOG.md', '_SCHEMA.md']);

function markdown(dir) {
  const files = [];
  for (const entry of readdirSync(dir)) {
    const path = join(dir, entry);
    if (statSync(path).isDirectory()) {
      files.push(...markdown(path));
    } else if (entry.endsWith('.md') && !skipped.has(entry)) {
      files.push(path);
    }
  }
  return files;
}

const problems = [];
const entries = markdown(knowledge);
for (const file of entries) {
  const head = readFileSync(file, 'utf8').split(/\r?\n/).slice(0, 10);
  if (!head.some((line) => line.startsWith('# '))) {
    problems.push(`${file.slice(root.length + 1)}: no H1 title in the first ten lines`);
  }
}

const index = join(knowledge, '_INDEX.md');
if (!existsSync(index)) {
  problems.push('docs/knowledge/_INDEX.md is missing');
} else {
  const text = readFileSync(index, 'utf8');
  for (const match of text.matchAll(/\]\(([^)]+)\)/g)) {
    const target = match[1];
    if (/^[a-z]+:/i.test(target) || target.startsWith('#')) {
      continue;
    }
    const path = resolve(dirname(index), target.split('#')[0]);
    if (!existsSync(path)) {
      problems.push(`docs/knowledge/_INDEX.md: broken link ${target}`);
    }
  }
}

if (problems.length > 0) {
  for (const problem of problems) {
    console.error(`kb-lint: ${problem}`);
  }
  process.exit(1);
}
console.log(`kb-lint: ${entries.length} entries, every _INDEX.md link resolves`);
