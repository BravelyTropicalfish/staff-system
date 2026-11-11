import assert from 'node:assert/strict';
import { test } from 'node:test';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { execFileSync } from 'node:child_process';
import { createHash } from 'node:crypto';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
function projectFiles(dir) {
  // Ask Git so tracked files remain checked even when an ignore rule matches.
  const names = execFileSync('git', ['ls-files', '-z', '--cached', '--others', '--exclude-standard'], { cwd: dir, encoding: 'utf8' });
  return [...new Set(names.split('\0').filter(Boolean))].sort().map(name => {
    const full = path.join(dir, name);
    assert.ok(fs.lstatSync(full).isFile(), `Non-portable project entry: ${name}`);
    return full;
  });
}

function files(dir) {
  return fs.readdirSync(dir, { withFileTypes: true }).flatMap(entry => {
    if (entry.name === '.git') return [];
    const full = path.join(dir, entry.name);
    assert.ok(!entry.isSymbolicLink(), `Non-portable symlink: ${full}`);
    return entry.isDirectory() ? files(full) : [full];
  });
}
function fingerprint(dir) {
  return Object.fromEntries(files(dir).sort().map(file => [
    path.relative(dir, file), createHash('sha256').update(fs.readFileSync(file)).digest('hex'),
  ]));
}

test('all assets are ordinary files without author-machine paths', () => {
  for (const file of projectFiles(root)) {
    const content = fs.readFileSync(file, 'utf8');
    assert.doesNotMatch(content, /\/(?:Users|home)\/[A-Za-z0-9_-]+\//, path.relative(root, file));
  }
});

test('local Markdown resource links exist inside the clone', () => {
  const failures = [];
  for (const file of projectFiles(root).filter(file => file.endsWith('.md'))) {
    const text = fs.readFileSync(file, 'utf8').replace(/^```.*?^```[^\n]*/gms, '');
    for (const match of text.matchAll(/\]\(([^\s)]+)\)/g)) {
      const href = match[1];
      if (/^(?:[a-z][a-z0-9+.-]*:|#)/i.test(href)) continue;
      const target = path.resolve(path.dirname(file), href.split('#')[0]);
      if (!target.startsWith(root + path.sep) || !fs.existsSync(target)) {
        failures.push(`${path.relative(root, file)} -> ${href}`);
      }
    }
  }
  assert.deepEqual(failures, []);
});

test('Git-ignored IDE files are excluded while force-tracked files remain checked', () => {
  const temporary = fs.mkdtempSync(path.join(os.tmpdir(), 'ai4se-ignored-'));
  try {
    execFileSync('git', ['init', '--quiet', '--template='], { cwd: temporary });
    fs.writeFileSync(path.join(temporary, '.gitignore'), '.idea/\n');
    fs.mkdirSync(path.join(temporary, '.idea'));
    fs.writeFileSync(path.join(temporary, '.idea/workspace.xml'), ['', 'Users', 'example', 'project'].join('/'));
    fs.writeFileSync(path.join(temporary, 'SKILL.md'), 'Local learner draft\n');
    assert.deepEqual(projectFiles(temporary).map(file => path.relative(temporary, file)), ['.gitignore', 'SKILL.md']);
    execFileSync('git', ['add', '--force', '.idea/workspace.xml'], { cwd: temporary });
    assert.ok(projectFiles(temporary).includes(path.join(temporary, '.idea/workspace.xml')));
  } finally {
    fs.rmSync(temporary, { recursive: true, force: true });
  }
});

test('both hosts retain working helpers and project skills after relocation', () => {
  const temporary = fs.mkdtempSync(path.join(os.tmpdir(), 'ai4se-relocation-'));
  const clone = path.join(temporary, 'clone with spaces');
  try {
    fs.mkdirSync(clone);
    for (const source of projectFiles(root)) {
      const target = path.join(clone, path.relative(root, source));
      fs.mkdirSync(path.dirname(target), { recursive: true });
      fs.copyFileSync(source, target);
      fs.chmodSync(target, fs.statSync(source).mode);
    }
    for (const relative of [
      '.qoder/skills/sdd-bootstrap/scripts/init-checkpoint-log.sh',
      '.github/skills/ai4se-sdd/harness/bootstrap/scripts/init-checkpoint-log.sh',
    ]) {
      const project = fs.mkdtempSync(path.join(temporary, 'project-'));
      execFileSync('bash', [path.join(clone, relative), '--root', project], { encoding: 'utf8' });
      const logs = fs.readdirSync(path.join(project, 'docs/ai4se-plugins'));
      assert.equal(logs.length, 1);
      assert.match(fs.readFileSync(path.join(project, 'docs/ai4se-plugins', logs[0]), 'utf8'), /skill\.harness\.bootstrap/);
    }
    const before = fingerprint(clone);
    execFileSync(process.execPath, [path.join(clone, 'scripts/refresh-agent-assets.mjs')]);
    assert.deepEqual(fingerprint(clone), before, 'Regeneration must be independent of checkout location');

    const learnerSkill = path.join(clone, 'skills/test-results-summary');
    fs.mkdirSync(path.join(learnerSkill, 'references'), { recursive: true });
    fs.writeFileSync(path.join(learnerSkill, 'SKILL.md'), '---\nname: test-results-summary\ndescription: Summarize supplied test results.\n---\n\nRead [the example](references/example.md).\n');
    fs.writeFileSync(path.join(learnerSkill, 'references/example.md'), 'A supplied test run passed.\n');
    const learnerScript = 'PACK_ROOT="$(cd "$SCRIPT_DIR/../../../.." && pwd)"\n';
    fs.writeFileSync(path.join(learnerSkill, 'helper.sh'), learnerScript);
    const hostOnly = path.join(clone, '.qoder/skills/my-existing-skill/SKILL.md');
    fs.mkdirSync(path.dirname(hostOnly));
    fs.writeFileSync(hostOnly, 'Keep this learner-authored host skill.\n');
    execFileSync(process.execPath, [path.join(clone, 'scripts/refresh-agent-assets.mjs')]);
    assert.equal(fs.readFileSync(hostOnly, 'utf8'), 'Keep this learner-authored host skill.\n');
    for (const host of ['.qoder', '.github']) {
      const target = path.join(clone, host, 'skills/test-results-summary');
      const content = fs.readFileSync(path.join(target, 'SKILL.md'), 'utf8');
      const link = content.match(/\]\(([^)]+)\)/)[1];
      assert.equal(fs.readFileSync(path.resolve(target, link), 'utf8'), 'A supplied test run passed.\n');
      assert.equal(fs.readFileSync(path.join(target, 'references/example.md'), 'utf8'), 'A supplied test run passed.\n');
      assert.equal(fs.readFileSync(path.join(target, 'helper.sh'), 'utf8'), learnerScript);
    }
    const preserved = fingerprint(path.join(clone, '.qoder'));
    fs.mkdirSync(path.join(clone, 'skills/superpowers'));
    fs.writeFileSync(path.join(clone, 'skills/superpowers/SKILL.md'), 'Conflicting skill\n');
    assert.throws(() => execFileSync(process.execPath, [path.join(clone, 'scripts/refresh-agent-assets.mjs')], { stdio: 'pipe' }), /Project skill would replace a bundled skill/);
    assert.deepEqual(fingerprint(path.join(clone, '.qoder')), preserved);
  } finally {
    fs.rmSync(temporary, { recursive: true, force: true });
  }
});
