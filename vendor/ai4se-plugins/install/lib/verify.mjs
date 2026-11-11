import fs from 'node:fs';
import { fileURLToPath } from 'node:url';
import { hostSkillEntries } from './catalog.mjs';
import path from 'node:path';
import {
  detectOpenspec,
  detectPackHub,
  detectProjectOpenspec,
  detectSuperpowers,
  nodeVersionOk,
} from './detect.mjs';

function ok(label, pass, detail) {
  return { label, pass: Boolean(pass), detail: detail || '' };
}

export function verifyState({
  cwd,
  packScope = 'project',
  openspecScope = 'project',
  hosts = [],
  packRoot,
  requireHosts = true,
  requireOpenspec = true,
  requireSuperpowers = true,
  requirePack = true,
}) {
  const checks = [];
  const node = nodeVersionOk();
  checks.push(ok('Node.js >= 20.19', node.ok, node.version));

  if (requireOpenspec) {
    const ospec = detectOpenspec();
    checks.push(ok('OpenSpec CLI', ospec.installed, ospec.version || 'not found'));

    if (openspecScope === 'project') {
      const proj = detectProjectOpenspec(cwd);
      checks.push(ok(
        'OpenSpec project tree (openspec/)',
        proj.dirExists,
        proj.dirExists ? proj.path : 'missing — run openspec init',
      ));
    }
  }

  if (requireSuperpowers) {
    const sp = detectSuperpowers(cwd);
    checks.push(ok(
      'Superpowers (brainstorming skill)',
      sp.installed,
      sp.brainstormingPath || 'not found',
    ));
  }

  if (requirePack) {
    const hub = detectPackHub(packScope, cwd);
    checks.push(ok(
      `Pack hub (${packScope})`,
      hub.exists && hub.skillCount >= 1,
      hub.exists ? `${hub.path} (${hub.skillCount} SKILL.md)` : `missing ${hub.path}`,
    ));
  }

  if (requireHosts && hosts.length) {
    for (const host of hosts) {
      const marker = findHostProjection(cwd, packScope, host, packRoot);
      checks.push(ok(
        `Host projection: ${host}`,
        marker.ok,
        marker.detail,
      ));
    }
  }

  const passed = checks.every((c) => c.pass);
  return { passed, checks };
}

export function findHostProjection(cwd, packScope, host, packRoot = fileURLToPath(new URL('../../', import.meta.url))) {
  const project = packScope === 'project';
  if (project && (host === 'codex' || host === 'qoder')) {
    const dir = path.join(cwd, host === 'codex' ? '.agents' : '.qoder', 'skills');
    const entries = hostSkillEntries(packRoot, host);
    for (const { name } of entries) {
      const file = path.join(dir, name, 'SKILL.md');
      const header = fs.existsSync(file) && fs.readFileSync(file, 'utf8').match(/^---\r?\n([\s\S]*?)\r?\n---/);
      if (!header || !header[1].split(/\r?\n/).includes(`name: ${name}`)) {
        return { ok: false, detail: `missing or misnamed: ${file}` };
      }
    }
    return { ok: true, detail: `${entries.length} project skills in ${dir}` };
  }
  switch (host) {
    case 'cursor': {
      const rule = path.join(cwd, '.cursor', 'rules', 'ai4se-sdd.mdc');
      if (fs.existsSync(rule)) return { ok: true, detail: rule };
      if (!project) {
        const cmds = path.join(process.env.HOME || '', '.cursor', 'commands');
        if (fs.existsSync(cmds)) return { ok: true, detail: cmds };
      }
      return { ok: false, detail: `missing ${rule}` };
    }
    case 'claude-code': {
      const cmds = path.join(
        project ? cwd : process.env.HOME || '',
        '.claude',
        'commands',
      );
      const sample = path.join(cmds, 'sdd-quality-checklist.md');
      if (fs.existsSync(sample)) return { ok: true, detail: sample };
      const link = path.join(
        project ? cwd : process.env.HOME || '',
        '.claude',
        'skills',
        'ai4se-sdd',
      );
      if (fs.existsSync(link)) return { ok: true, detail: link };
      return { ok: false, detail: `missing ${sample}` };
    }
    case 'codex': {
      const agents = path.join(cwd, 'AGENTS.md');
      if (fs.existsSync(agents)) {
        const text = fs.readFileSync(agents, 'utf8');
        if (text.includes('ai4se-plugins-install')) {
          return { ok: true, detail: `${agents} (marker)` };
        }
      }
      const link = path.join(
        project ? cwd : process.env.HOME || '',
        '.codex',
        'skills',
        'ai4se-sdd',
      );
      if (fs.existsSync(link)) return { ok: true, detail: link };
      return { ok: false, detail: 'missing AGENTS.md marker or .codex/skills/ai4se-sdd' };
    }
    case 'github-copilot': {
      const link = path.join(cwd, '.github', 'skills', 'ai4se-sdd');
      if (fs.existsSync(link)) return { ok: true, detail: link };
      const g = path.join(process.env.HOME || '', '.copilot', 'skills', 'ai4se-sdd');
      if (!project && fs.existsSync(g)) return { ok: true, detail: g };
      return { ok: false, detail: `missing ${link}` };
    }
    case 'qoder': {
      const base = project ? cwd : process.env.HOME || '';
      const link = project ? path.join(base, '.qoder', 'skills') : path.join(base, '.qoder', 'skills', 'ai4se-sdd');
      const sample = path.join(link, 'sdd-quality-checklist', 'SKILL.md');
      if (fs.existsSync(sample)) {
        const body = fs.readFileSync(sample, 'utf8');
        if (/^name:\s*sdd-quality-checklist\s*$/m.test(body)) {
          return { ok: true, detail: link };
        }
        return { ok: false, detail: `${sample} missing sdd- name prefix` };
      }
      return { ok: false, detail: `missing ${sample}` };
    }
    default:
      return { ok: false, detail: `unknown host ${host}` };
  }
}

export function printVerifyReport(result) {
  for (const c of result.checks) {
    const mark = c.pass ? 'OK' : 'FAIL';
    console.log(`[${mark}] ${c.label}: ${c.detail}`);
  }
  console.log(result.passed ? 'verify: PASSED' : 'verify: FAILED');
}
