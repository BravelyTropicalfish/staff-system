import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { projectPackAgents } from './agents.mjs';
import { hostSkillEntries, parseCatalogCommands } from './catalog.mjs';
import { agentsSkillsDir } from './detect.mjs';
import { linkOrCopy, rmIfExists } from './hub.mjs';

const MARKER_START = '<!-- ai4se-plugins-install:start -->';
const MARKER_END = '<!-- ai4se-plugins-install:end -->';

/** All catalog `/sdd:*` rows for host command projection. */
export function listProjectCommands(packRoot) {
  return parseCatalogCommands(packRoot);
}

function ensureDir(d) {
  fs.mkdirSync(d, { recursive: true });
}

function writeFile(file, content) {
  ensureDir(path.dirname(file));
  fs.writeFileSync(file, content, 'utf8');
}

function commandBody(entry, packRoot) {
  const { slash, gate, status, skillPath, target } = entry;
  const catalog = path.join(packRoot, 'commands', 'catalog.md');
  const packRel = skillPath || target;
  const gateNote = gate
    ? '\n\n**Gate:** human explicit invoke only. Do not auto-run.\n'
    : '';
  const statusNote =
    status && status !== 'ready'
      ? `\n\n**Status:** \`${status}\` — follow the SKILL/recipe stub behavior; do not invent a full pipeline.\n`
      : '';
  return `---
description: /sdd:${slash} — ai4se-plugins
${gate ? 'disable-model-invocation: true\n' : ''}---

# /sdd:${slash}

Resolve this command via the ai4se-plugins catalog and follow the linked SKILL.md or recipe.

- Catalog: \`${catalog}\`
- Target: \`${packRel}\`
- Pack skills hub: \`.agents/skills/ai4se-sdd/\` (or \`~/.agents/skills/ai4se-sdd/\`)
${gateNote}${statusNote}
Steps:
1. Open the catalog row for \`/sdd:${slash}\`.
2. Load the target path (skill or recipe) from the pack / hub.
3. Execute Instructions; do not invent a parallel flow for OpenSpec/Superpowers wraps.
`;
}

function cursorRule(packRoot) {
  return `---
description: ai4se-plugins /sdd command resolution
globs:
alwaysApply: true
---

# ai4se-plugins

When the user invokes \`/sdd:*\` (or asks to run an SDD stage):

1. Resolve the command via \`${path.join(packRoot, 'commands', 'catalog.md')}\`.
2. Follow the linked \`SKILL.md\` or recipe under the pack / \`.agents/skills/ai4se-sdd/\`.
3. Do **not** auto-invoke commands marked \`gate: true\` unless the user typed that exact gate command.
4. Assume OpenSpec + Superpowers when wraps require them; if missing, tell the human to run the ai4se-plugins installer.
`;
}

function upsertAgentsMarker(file, packRoot) {
  const block = [
    MARKER_START,
    '## ai4se-plugins',
    '',
    'When the user invokes `/sdd:*`, resolve via:',
    '',
    `- Catalog: \`${path.join(packRoot, 'commands', 'catalog.md')}\``,
    '- Skills hub: `.agents/skills/ai4se-sdd/` (project) or `~/.agents/skills/ai4se-sdd/` (global)',
    '- Stage agents: `.cursor/agents/`, `.claude/agents/`, `.codex/agents/`, `.qoder/agents/`, `.github/agents/*.agent.md` (`sdd-agent-spec-align`, `sdd-agent-apply-execute`)',
    '- Do not auto-invoke `gate: true` catalog rows.',
    '',
    MARKER_END,
    '',
  ].join('\n');

  let text = '';
  if (fs.existsSync(file)) {
    text = fs.readFileSync(file, 'utf8');
    if (text.includes(MARKER_START) && text.includes(MARKER_END)) {
      text = text.replace(
        new RegExp(`${MARKER_START}[\\s\\S]*?${MARKER_END}\\n?`),
        block,
      );
    } else {
      text = `${text.trimEnd()}\n\n${block}`;
    }
  } else {
    text = `# AGENTS.md\n\n${block}`;
  }
  writeFile(file, text);
}

function hubTarget(packScope, cwd) {
  return path.join(agentsSkillsDir(packScope, cwd), 'ai4se-sdd');
}

/**
 * Write host-specific thin projections. Returns { ok, files, errors }.
 */
export function projectHosts({
  packRoot,
  cwd,
  packScope,
  hosts,
  mode = 'symlink',
}) {
  const files = [];
  const errors = [];
  const hub = hubTarget(packScope, cwd);

  if (!fs.existsSync(hub)) {
    errors.push(`Pack hub missing before host project: ${hub}`);
    return { ok: false, files, errors };
  }

  for (const host of hosts) {
    try {
      switch (host) {
        case 'cursor':
          files.push(...projectCursor({ packRoot, cwd, packScope, hub, mode }));
          break;
        case 'claude-code':
          files.push(...projectClaude({ packRoot, cwd, packScope, hub, mode }));
          break;
        case 'codex':
          files.push(...projectCodex({ packRoot, cwd, packScope, hub, mode }));
          break;
        case 'github-copilot':
          files.push(...projectCopilot({ packRoot, cwd, packScope, hub, mode }));
          break;
        case 'qoder':
          files.push(...projectQoder({ packRoot, cwd, packScope, hub, mode }));
          break;
        default:
          errors.push(`Unknown host: ${host}`);
      }
    } catch (e) {
      errors.push(`${host}: ${e.message}`);
    }
  }

  return { ok: errors.length === 0, files, errors };
}

function commandsDir(base, hostFolder) {
  return path.join(base, hostFolder, 'commands');
}

function writeCommandSet(dir, packRoot) {
  const out = [];
  ensureDir(dir);
  const commands = listProjectCommands(packRoot);
  for (const entry of commands) {
    const file = path.join(dir, `sdd-${entry.slash}.md`);
    writeFile(file, commandBody(entry, packRoot));
    out.push(file);
  }
  return out;
}

function projectHostAgents(packRoot, agentsDir, mode, fileName) {
  return projectPackAgents({
    packRoot,
    agentsDir,
    mode,
    readyOnly: true,
    ...(fileName ? { fileName } : {}),
  });
}

/** Copilot CLI / IDE custom agents use `<name>.agent.md`. */
function copilotAgentFileName(agent) {
  return `${agent.name}.agent.md`;
}

function projectCursor({ packRoot, cwd, packScope, hub, mode }) {
  const files = [];
  if (packScope === 'project') {
    const rule = path.join(cwd, '.cursor', 'rules', 'ai4se-sdd.mdc');
    writeFile(rule, cursorRule(packRoot));
    files.push(rule);
    files.push(...writeCommandSet(path.join(cwd, '.cursor', 'commands'), packRoot));
    files.push(
      ...projectHostAgents(packRoot, path.join(cwd, '.cursor', 'agents'), mode),
    );
  } else {
    const home = os.homedir();
    // Cursor discovers ~/.agents/skills; also drop commands under ~/.cursor/commands
    files.push(...writeCommandSet(path.join(home, '.cursor', 'commands'), packRoot));
    const native = path.join(home, '.cursor', 'skills', 'ai4se-sdd');
    linkOrCopy(hub, native, mode);
    files.push(native);
    files.push(
      ...projectHostAgents(packRoot, path.join(home, '.cursor', 'agents'), mode),
    );
  }
  return files;
}

function projectClaude({ packRoot, cwd, packScope, hub, mode }) {
  const files = [];
  const base = packScope === 'project' ? cwd : os.homedir();
  const skillsLink = path.join(base, '.claude', 'skills', 'ai4se-sdd');
  linkOrCopy(hub, skillsLink, mode);
  files.push(skillsLink);
  files.push(...writeCommandSet(commandsDir(base, '.claude'), packRoot));
  files.push(
    ...projectHostAgents(packRoot, path.join(base, '.claude', 'agents'), mode),
  );
  return files;
}

function projectCodex({ packRoot, cwd, packScope, hub, mode }) {
  const files = [];
  const base = packScope === 'project' ? cwd : os.homedir();
  const skillsLink = path.join(base, '.codex', 'skills', 'ai4se-sdd');
  linkOrCopy(hub, skillsLink, mode);
  files.push(skillsLink);
  files.push(
    ...projectHostAgents(packRoot, path.join(base, '.codex', 'agents'), mode),
  );
  if (packScope === 'project') {
    for (const entry of hostSkillEntries(packRoot, 'codex')) {
      files.push(projectPrefixedSkill(entry, path.join(cwd, '.agents', 'skills'), mode));
    }
    const agents = path.join(cwd, 'AGENTS.md');
    upsertAgentsMarker(agents, packRoot);
    files.push(agents);
  } else {
    const note = path.join(os.homedir(), '.codex', 'ai4se-sdd-NOTE.md');
    writeFile(
      note,
      [
        '# ai4se-plugins (global)',
        '',
        `Skills: ${path.join(os.homedir(), '.agents', 'skills', 'ai4se-sdd')}`,
        `Catalog: ${path.join(packRoot, 'commands', 'catalog.md')}`,
        `Agents: ${path.join(os.homedir(), '.codex', 'agents')}`,
        '',
        'Restart Codex after install so skills are rediscovered.',
        '',
      ].join('\n'),
    );
    files.push(note);
  }
  return files;
}

function projectCopilot({ packRoot, cwd, packScope, hub, mode }) {
  const files = [];
  if (packScope === 'project') {
    const link = path.join(cwd, '.github', 'skills', 'ai4se-sdd');
    linkOrCopy(hub, link, mode);
    files.push(link);
    const readme = path.join(cwd, '.github', 'skills', 'ai4se-sdd-README.md');
    writeFile(
      readme,
      [
        '# ai4se-plugins for GitHub Copilot',
        '',
        `Catalog: ${path.join(packRoot, 'commands', 'catalog.md')}`,
        'Skills are linked as `ai4se-sdd/` (recursive SKILL.md tree).',
        'Ready agents: `.github/agents/<name>.agent.md` (`sdd-agent-spec-align`, `sdd-agent-apply-execute`).',
        'Invoke via skill name, custom agent picker, or ask the agent to follow `/sdd:*` from the catalog.',
        '',
      ].join('\n'),
    );
    files.push(readme);
    files.push(
      ...projectHostAgents(
        packRoot,
        path.join(cwd, '.github', 'agents'),
        mode,
        copilotAgentFileName,
      ),
    );
  } else {
    const link = path.join(os.homedir(), '.copilot', 'skills', 'ai4se-sdd');
    linkOrCopy(hub, link, mode);
    files.push(link);
    files.push(
      ...projectHostAgents(
        packRoot,
        path.join(os.homedir(), '.copilot', 'agents'),
        mode,
        copilotAgentFileName,
      ),
    );
  }
  return files;
}

function projectPrefixedSkill({ id, src, name }, root, mode) {
  const dest = path.join(root, name);
  const existing = path.join(dest, 'SKILL.md');
  if (fs.lstatSync(dest, { throwIfNoEntry: false })?.isSymbolicLink() ||
      (fs.existsSync(existing) && !fs.readFileSync(existing, 'utf8').split(/\r?\n/).includes(`id: ${id}`))) {
    throw new Error(`Unmanaged skill directory: ${dest}`);
  }
  ensureDir(dest);
  const skillMd = fs.readFileSync(path.join(src, 'SKILL.md'), 'utf8');
  const rewritten = skillMd.replace(/^name:\s*.+$/m, `name: ${name}`);
  writeFile(path.join(dest, 'SKILL.md'), rewritten);

  for (const ent of fs.readdirSync(src, { withFileTypes: true })) {
    if (ent.name === 'SKILL.md') continue;
    const s = path.join(src, ent.name);
    const d = path.join(dest, ent.name);
    if (ent.isDirectory()) {
      linkOrCopy(s, d, mode);
    } else if (mode === 'symlink') {
      rmIfExists(d);
      fs.symlinkSync(s, d);
    } else {
      fs.copyFileSync(s, d);
    }
  }
  return dest;
}

function projectQoder({ packRoot, cwd, packScope, hub: _hub, mode }) {
  const files = [];
  const base = packScope === 'project' ? cwd : os.homedir();
  const skillsDir = path.join(base, '.qoder', 'skills');
  const groupedRoot = path.join(skillsDir, 'ai4se-sdd');
  const root = packScope === 'project' ? skillsDir : groupedRoot;
  ensureDir(skillsDir);

  const skills = hostSkillEntries(packRoot, 'qoder');
  for (const entry of skills) {
    files.push(projectPrefixedSkill(entry, root, mode));
  }
  if (packScope === 'project' && fs.existsSync(groupedRoot) && !fs.lstatSync(groupedRoot).isSymbolicLink()) {
    // Remove only the previous installer’s entries, preserving unrelated content.
    for (const { name, id } of skills) {
      const legacy = path.join(groupedRoot, name);
      const file = path.join(legacy, 'SKILL.md');
      if (fs.existsSync(file) && fs.readFileSync(file, 'utf8').split(/\r?\n/).includes(`id: ${id}`)) {
        rmIfExists(legacy);
      }
    }
    if (!fs.readdirSync(groupedRoot).length) fs.rmdirSync(groupedRoot);
  }

  files.push(
    ...projectHostAgents(packRoot, path.join(base, '.qoder', 'agents'), mode),
  );

  const note = path.join(skillsDir, 'ai4se-sdd-README.md');
  writeFile(
    note,
    [
      '# ai4se-plugins for Qoder',
      '',
      `Catalog: ${path.join(packRoot, 'commands', 'catalog.md')}`,
      packScope === 'project' ? 'Skills live under `.qoder/skills/sdd-<name>/`.' : 'Skills live under `.qoder/skills/ai4se-sdd/sdd-<name>/` (no domain nesting).',
      'Qoder-only: folder and frontmatter `name:` both use the `sdd-` prefix.',
      'Ready agents: `.qoder/agents/<name>.md` (`sdd-agent-spec-align`, `sdd-agent-apply-execute`).',
      'If Qoder is already running: `/skills reload` and `/agents reload`',
      '',
    ].join('\n'),
  );
  files.push(note);
  files.push(root);
  return files;
}
