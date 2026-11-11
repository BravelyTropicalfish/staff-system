import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { spawnSync } from 'node:child_process';

const __dirname = path.dirname(fileURLToPath(import.meta.url));

export const HOSTS = [
  'cursor',
  'claude-code',
  'codex',
  'github-copilot',
  'qoder',
];

export const OPENSPEC_TOOL_IDS = {
  cursor: 'cursor',
  'claude-code': 'claude',
  codex: 'codex',
  'github-copilot': 'github-copilot',
  qoder: 'qoder',
};

export function packRootFromModule() {
  return path.resolve(__dirname, '../..');
}

export function resolvePackRoot(explicit) {
  if (explicit) {
    const abs = path.resolve(explicit);
    if (!fs.existsSync(path.join(abs, 'commands', 'catalog.md'))) {
      throw new Error(`--pack-root does not look like ai4se-plugins: ${abs}`);
    }
    return abs;
  }
  let dir = process.cwd();
  for (;;) {
    if (fs.existsSync(path.join(dir, 'commands', 'catalog.md')) &&
        fs.existsSync(path.join(dir, 'skills'))) {
      return dir;
    }
    const parent = path.dirname(dir);
    if (parent === dir) break;
    dir = parent;
  }
  const fallback = packRootFromModule();
  if (fs.existsSync(path.join(fallback, 'commands', 'catalog.md'))) {
    return fallback;
  }
  throw new Error('Could not locate ai4se-plugins pack root (need commands/catalog.md)');
}

export function homeDir() {
  return os.homedir();
}

export function scopeRoot(scope, cwd) {
  if (scope === 'global') return homeDir();
  return path.resolve(cwd);
}

export function agentsSkillsDir(scope, cwd) {
  return path.join(scopeRoot(scope, cwd), '.agents', 'skills');
}

export function runCmd(cmd, args, opts = {}) {
  const res = spawnSync(cmd, args, {
    encoding: 'utf8',
    shell: false,
    ...opts,
  });
  return {
    status: res.status ?? 1,
    stdout: (res.stdout || '').trim(),
    stderr: (res.stderr || '').trim(),
    error: res.error || null,
  };
}

export function which(bin) {
  const res = runCmd(process.platform === 'win32' ? 'where' : 'which', [bin]);
  return res.status === 0 ? res.stdout.split(/\r?\n/)[0] : null;
}

export function nodeVersionOk() {
  const m = /^v(\d+)\.(\d+)\.(\d+)/.exec(process.version);
  if (!m) return { ok: false, version: process.version };
  const major = Number(m[1]);
  const minor = Number(m[2]);
  const ok = major > 20 || (major === 20 && minor >= 19);
  return { ok, version: process.version };
}

export function detectOpenspec() {
  const bin = which('openspec');
  if (!bin) return { installed: false, version: null, bin: null };
  const v = runCmd('openspec', ['--version']);
  return {
    installed: v.status === 0,
    version: v.status === 0 ? v.stdout : null,
    bin,
  };
}

export function findSkillNamed(roots, name) {
  for (const root of roots) {
    if (!root || !fs.existsSync(root)) continue;
    const direct = path.join(root, name, 'SKILL.md');
    if (fs.existsSync(direct)) return path.dirname(direct);
    // recurse one/two levels (superpowers layout)
    try {
      const entries = fs.readdirSync(root, { withFileTypes: true });
      for (const e of entries) {
        if (!e.isDirectory() && !e.isSymbolicLink()) continue;
        const candidate = path.join(root, e.name, name, 'SKILL.md');
        if (fs.existsSync(candidate)) return path.dirname(candidate);
        const nested = path.join(root, e.name, 'skills', name, 'SKILL.md');
        if (fs.existsSync(nested)) return path.dirname(nested);
      }
    } catch {
      // ignore
    }
  }
  return null;
}

export function superpowersSearchRoots(cwd) {
  const home = homeDir();
  return [
    path.join(cwd, '.agents', 'skills'),
    path.join(cwd, '.agents', 'skills', 'superpowers'),
    path.join(cwd, '.claude', 'skills'),
    path.join(cwd, '.cursor', 'skills'),
    path.join(home, '.agents', 'skills'),
    path.join(home, '.agents', 'skills', 'superpowers'),
    path.join(home, '.claude', 'skills'),
    path.join(home, '.cursor', 'skills'),
    path.join(home, '.codex', 'superpowers', 'skills'),
    path.join(home, '.copilot', 'skills'),
    path.join(home, '.qoder', 'skills'),
  ];
}

export function detectSuperpowers(cwd = process.cwd()) {
  const brainstorming = findSkillNamed(superpowersSearchRoots(cwd), 'brainstorming');
  return {
    installed: Boolean(brainstorming),
    brainstormingPath: brainstorming,
  };
}

export function countSkillMd(dir) {
  if (!fs.existsSync(dir)) return 0;
  let n = 0;
  const walk = (d) => {
    let entries;
    try {
      entries = fs.readdirSync(d, { withFileTypes: true });
    } catch {
      return;
    }
    for (const e of entries) {
      const p = path.join(d, e.name);
      if (e.isDirectory() || e.isSymbolicLink()) {
        if (e.name === 'node_modules' || e.name === '.git') continue;
        // follow symlink dirs
        try {
          const st = fs.statSync(p);
          if (st.isDirectory()) walk(p);
        } catch {
          // broken link
        }
      } else if (e.isFile() && e.name === 'SKILL.md') {
        n += 1;
      }
    }
  };
  walk(dir);
  return n;
}

export function detectPackHub(scope, cwd) {
  const hub = path.join(agentsSkillsDir(scope, cwd), 'ai4se-sdd');
  const exists = fs.existsSync(hub);
  return {
    path: hub,
    exists,
    skillCount: exists ? countSkillMd(hub) : 0,
  };
}

export function detectProjectOpenspec(cwd) {
  const dir = path.join(cwd, 'openspec');
  return {
    dirExists: fs.existsSync(dir),
    path: dir,
  };
}

export function detectAll({ packRoot, cwd, packScope = 'project' }) {
  let packVersion = null;
  try {
    // lazy import avoided — version is sync and light
    packVersion = readPackVersionSafe(packRoot);
  } catch {
    packVersion = null;
  }
  return {
    node: nodeVersionOk(),
    packRoot,
    packVersion,
    cwd,
    openspec: detectOpenspec(),
    openspecProject: detectProjectOpenspec(cwd),
    superpowers: detectSuperpowers(cwd),
    packHub: detectPackHub(packScope, cwd),
    packHubGlobal: detectPackHub('global', cwd),
    clientPackVersion: readClientPackVersionSafe(cwd),
  };
}

function readPackVersionSafe(packRoot) {
  const file = path.join(packRoot, 'VERSION');
  if (!fs.existsSync(file)) return null;
  return fs.readFileSync(file, 'utf8').trim() || null;
}

function readClientPackVersionSafe(cwd) {
  const file = path.join(path.resolve(cwd), '.agents', 'ai4se-plugins-VERSION');
  if (!fs.existsSync(file)) return null;
  return fs.readFileSync(file, 'utf8').trim() || null;
}
