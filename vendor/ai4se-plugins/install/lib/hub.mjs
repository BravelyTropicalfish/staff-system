import fs from 'node:fs';
import path from 'node:path';
import { agentsSkillsDir } from './detect.mjs';
import { readPackVersion } from './version.mjs';

function ensureDir(dir) {
  fs.mkdirSync(dir, { recursive: true });
}

export function rmIfExists(target) {
  let st;
  try {
    st = fs.lstatSync(target);
  } catch {
    return;
  }
  if (st.isSymbolicLink() || st.isFile()) {
    fs.unlinkSync(target);
  } else if (st.isDirectory()) {
    fs.rmSync(target, { recursive: true, force: true });
  }
}

function copyDir(src, dest) {
  ensureDir(dest);
  for (const ent of fs.readdirSync(src, { withFileTypes: true })) {
    const s = path.join(src, ent.name);
    const d = path.join(dest, ent.name);
    if (ent.isDirectory()) copyDir(s, d);
    else fs.copyFileSync(s, d);
  }
}

/**
 * Place pack skills into agents hub and write meta index.
 *
 * Client surface is skills/ (+ meta) and host-projected agents/.
 * Pack-root maintainer tools such as scripts/update-version.sh and
 * scripts/check-package.sh are NOT installed.
 */
export function installPackHub({ packRoot, cwd, packScope, mode = 'symlink' }) {
  const skillsSrc = path.join(packRoot, 'skills');
  const recipesSrc = path.join(packRoot, 'recipes');
  const agentsSrc = path.join(packRoot, 'agents');
  if (!fs.existsSync(skillsSrc)) {
    throw new Error(`Pack skills missing: ${skillsSrc}`);
  }

  const packVersion = readPackVersion(packRoot);
  const hubParent = agentsSkillsDir(packScope, cwd);
  ensureDir(hubParent);
  const hub = path.join(hubParent, 'ai4se-sdd');
  const meta = path.join(hubParent, 'ai4se-sdd-meta');

  rmIfExists(hub);
  if (mode === 'symlink') {
    fs.symlinkSync(skillsSrc, hub, 'dir');
  } else {
    copyDir(skillsSrc, hub);
  }

  ensureDir(meta);
  const index = [
    '# ai4se-sdd meta',
    '',
    `- packVersion: ${packVersion}`,
    `- packRoot: ${packRoot}`,
    `- skillsHub: ${hub}`,
    `- recipes: ${recipesSrc}`,
    `- agents: ${agentsSrc}`,
    `- catalog: ${path.join(packRoot, 'commands', 'catalog.md')}`,
    '',
    'Invoke via host `/sdd:*` projections or by reading catalog + SKILL.md.',
    'Stage agents: host agent dirs (cursor/claude/codex/qoder `*.md`; copilot `*.agent.md`; ready only).',
    '',
  ].join('\n');
  fs.writeFileSync(path.join(meta, 'INDEX.md'), index, 'utf8');
  fs.writeFileSync(path.join(meta, 'VERSION'), `${packVersion}\n`, 'utf8');

  const recipeList = fs.existsSync(recipesSrc)
    ? fs.readdirSync(recipesSrc).filter((f) => f.endsWith('.md')).sort()
    : [];
  fs.writeFileSync(
    path.join(meta, 'recipes.md'),
    ['# Recipes', '', ...recipeList.map((f) => `- \`${path.join(recipesSrc, f)}\``), ''].join('\n'),
    'utf8',
  );

  writeAgentsMeta(meta, packRoot);

  return { hub, meta, mode, packVersion };
}

function writeAgentsMeta(meta, packRoot) {
  // Lazy import avoided — keep hub free of circular deps by inline scan
  const agentsRoot = path.join(packRoot, 'agents');
  const rows = [];
  if (fs.existsSync(agentsRoot)) {
    for (const ent of fs.readdirSync(agentsRoot, { withFileTypes: true })) {
      if (!ent.isDirectory()) continue;
      if (ent.name.startsWith('_') || ent.name.startsWith('.')) continue;
      const src = path.join(agentsRoot, ent.name, 'AGENT.md');
      if (!fs.existsSync(src)) continue;
      const text = fs.readFileSync(src, 'utf8');
      const fm = text.match(/^---\r?\n([\s\S]*?)\r?\n---/);
      let name = ent.name;
      let status = 'draft';
      let id = `agent.${ent.name}`;
      if (fm) {
        for (const line of fm[1].split(/\r?\n/)) {
          const kv = line.match(/^([A-Za-z0-9_-]+):\s*(.*)$/);
          if (!kv) continue;
          if (kv[1] === 'name') name = kv[2].trim();
          if (kv[1] === 'status') status = kv[2].trim();
          if (kv[1] === 'id') id = kv[2].trim();
        }
      }
      rows.push({ name, id, status, src });
    }
  }
  rows.sort((a, b) => a.name.localeCompare(b.name));
  const lines = [
    '# Agents',
    '',
    'Ready agents are projected to host agent dirs (all five hosts; Copilot uses `.agent.md`).',
    '',
    ...rows.map((r) => `- \`${r.id}\` (${r.status}) — \`${r.src}\``),
    '',
  ];
  fs.writeFileSync(path.join(meta, 'agents.md'), lines.join('\n'), 'utf8');
}

/**
 * Symlink or copy an arbitrary source dir to dest path.
 */
export function linkOrCopy(src, dest, mode = 'symlink') {
  ensureDir(path.dirname(dest));
  rmIfExists(dest);
  if (mode === 'symlink') {
    fs.symlinkSync(src, dest, 'dir');
  } else {
    copyDir(src, dest);
  }
  return dest;
}

/**
 * Symlink or copy a single file to dest path.
 */
export function linkOrCopyFile(src, dest, mode = 'symlink') {
  ensureDir(path.dirname(dest));
  rmIfExists(dest);
  if (mode === 'symlink') {
    fs.symlinkSync(src, dest);
  } else {
    fs.copyFileSync(src, dest);
  }
  return dest;
}
