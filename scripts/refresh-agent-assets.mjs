#!/usr/bin/env node
// Reuse the pack's copy projections, then make their references relocatable.
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { installPackHub } from '../vendor/ai4se-plugins/install/lib/hub.mjs';
import { projectHosts } from '../vendor/ai4se-plugins/install/lib/project-hosts.mjs';
import { hostSkillEntries } from '../vendor/ai4se-plugins/install/lib/catalog.mjs';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const pack = path.join(root, 'vendor/ai4se-plugins');
const localSkills = path.join(root, 'skills');
const posix = value => value.split(path.sep).join('/');
const relative = (from, to) => posix(path.relative(from, to));
const projectSkills = fs.existsSync(localSkills)
  ? fs.readdirSync(localSkills, { withFileTypes: true }).filter(entry =>
    entry.isDirectory() && fs.existsSync(path.join(localSkills, entry.name, 'SKILL.md')))
  : [];
const bundledNames = new Set(['ai4se-sdd', 'superpowers', ...hostSkillEntries(pack, 'qoder').map(entry => entry.name)]);
for (const { name } of projectSkills) {
  if (!/^[a-z0-9]+(?:-[a-z0-9]+)*$/.test(name) || name.length > 64) {
    throw new Error(`Invalid project skill name: ${name}`);
  }
  if (bundledNames.has(name) || name.startsWith('openspec-')) {
    throw new Error(`Project skill would replace a bundled skill: ${name}`);
  }
}

function portableCopy(source, target) {
  fs.rmSync(target, { recursive: true, force: true });
  fs.mkdirSync(path.dirname(target), { recursive: true });
  fs.cpSync(source, target, { recursive: true });
}

function rebaseTree(source, target) {
  for (const entry of fs.readdirSync(source, { withFileTypes: true })) {
    const src = path.join(source, entry.name);
    const dst = path.join(target, entry.name);
    if (entry.isDirectory()) { rebaseTree(src, dst); continue; }
    if (entry.name.endsWith('.md')) {
      // Project docs retain their host-specific frontmatter. Resource links point
      // to the bundled canonical implementation, including shared pack resources.
      const content = fs.readFileSync(dst, 'utf8').replace(
        /(\]\()([^\s)]+)(\))/g,
        (match, before, href, after) => {
          if (/^(?:[a-z][a-z0-9+.-]*:|#|\/)/i.test(href)) return match;
          const [file, fragment] = href.split('#');
          const resolved = path.resolve(path.dirname(src), file);
          if (!fs.existsSync(resolved)) return match;
          return before + relative(path.dirname(dst), resolved) + (fragment ? '#' + fragment : '') + after;
        },
      );
      fs.writeFileSync(dst, content);
    } else if (entry.name.endsWith('.sh') && src.startsWith(pack + path.sep)) {
      const content = fs.readFileSync(dst, 'utf8');
      fs.writeFileSync(dst, content.replace(
        'PACK_ROOT="$(cd "$SCRIPT_DIR/../../../.." && pwd)"',
        'PACK_ROOT="$(cd "$SCRIPT_DIR/' + relative(path.dirname(dst), pack) + '" && pwd)"',
      ));
    }
  }
}

const scratch = fs.mkdtempSync(path.join(os.tmpdir(), 'ai4se-assets-'));
try {
  installPackHub({ packRoot: pack, cwd: scratch, packScope: 'project', mode: 'copy' });
  const result = projectHosts({ packRoot: pack, cwd: scratch, packScope: 'project', hosts: ['qoder', 'github-copilot'], mode: 'copy' });
  if (result.errors.length) throw new Error(result.errors.join('\n'));

  for (const entry of hostSkillEntries(pack, 'qoder')) {
    const target = path.join(root, '.qoder/skills', entry.name);
    portableCopy(path.join(scratch, '.qoder/skills', entry.name), target);
    rebaseTree(entry.src, target);
  }
  const copilot = path.join(root, '.github/skills/ai4se-sdd');
  portableCopy(path.join(scratch, '.github/skills/ai4se-sdd'), copilot);
  rebaseTree(path.join(pack, 'skills'), copilot);

  for (const host of ['.qoder', '.github']) {
    // Only the two pack-owned agent files are replaced; unrelated agents remain.
    for (const name of fs.readdirSync(path.join(scratch, host, 'agents'))) {
      portableCopy(path.join(scratch, host, 'agents', name), path.join(root, host, 'agents', name));
    }
    portableCopy(path.join(root, 'vendor/superpowers/skills'), path.join(root, host, 'skills/superpowers'));
    for (const { name } of projectSkills) {
      const source = path.join(localSkills, name);
      const target = path.join(root, host, 'skills', name);
      portableCopy(source, target);
      rebaseTree(source, target);
    }
    fs.writeFileSync(path.join(root, host, 'skills/ai4se-sdd-README.md'),
      '# AI4SE SDD skills\n\n' +
      'These are repository-local copies. No external checkout is required.\n\n' +
      '- [Command catalog](../../vendor/ai4se-plugins/commands/catalog.md)\n' +
      '- [Dependency versions and maintenance](../../vendor/README.md)\n' +
      '- [Create a project skill](skills-creator/SKILL.md)\n' +
      '\nRefresh after changing bundled sources: `node scripts/refresh-agent-assets.mjs` from the repository root.\n' +
      (host === '.qoder' ? '\nIn an existing Qoder session, use `/skills reload` and `/agents reload`.\n' : ''));
  }
  console.log('Refreshed portable Qoder and GitHub Copilot assets.');
} finally {
  fs.rmSync(scratch, { recursive: true, force: true });
}
