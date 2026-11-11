import fs from 'node:fs';
import path from 'node:path';
import readline from 'node:readline';
import { HOSTS } from './detect.mjs';

export function parseArgs(argv) {
  const out = {
    command: 'help',
    hosts: null,
    packScope: null,
    openspecScope: null,
    superpowersScope: null,
    packRoot: null,
    cwd: process.cwd(),
    yes: false,
    force: false,
    mode: 'symlink',
    skipOpenspec: false,
    skipSuperpowers: false,
    skipPack: false,
    versionAction: null,
    versionValue: null,
  };
  const args = [...argv];
  if (args.length && !args[0].startsWith('-')) {
    out.command = args.shift();
  }
  // `version bump|set|show …` and `--version`
  if (out.command === 'version') {
    out.versionAction = 'show';
    if (args.length && !args[0].startsWith('-')) {
      out.versionAction = args.shift();
      if (
        (out.versionAction === 'bump' || out.versionAction === 'set') &&
        args.length &&
        !args[0].startsWith('-')
      ) {
        out.versionValue = args.shift();
      }
    }
  }
  while (args.length) {
    const a = args.shift();
    const next = () => {
      if (!args.length) throw new Error(`Missing value for ${a}`);
      return args.shift();
    };
    switch (a) {
      case '--hosts':
        out.hosts = next().split(',').map((s) => s.trim()).filter(Boolean);
        break;
      case '--pack-scope':
        out.packScope = next();
        break;
      case '--openspec-scope':
        out.openspecScope = next();
        break;
      case '--superpowers-scope':
        out.superpowersScope = next();
        break;
      case '--pack-root':
        out.packRoot = next();
        break;
      case '--cwd':
        out.cwd = path.resolve(next());
        break;
      case '--yes':
      case '-y':
        out.yes = true;
        break;
      case '--force':
      case '-f':
        out.force = true;
        break;
      case '--mode':
        out.mode = next();
        break;
      case '--skip-openspec':
        out.skipOpenspec = true;
        break;
      case '--skip-superpowers':
        out.skipSuperpowers = true;
        break;
      case '--skip-pack':
        out.skipPack = true;
        break;
      case '--version':
      case '-V':
        out.command = 'version';
        out.versionAction = out.versionAction || 'show';
        break;
      case '--help':
      case '-h':
        out.command = 'help';
        break;
      default:
        throw new Error(`Unknown argument: ${a}`);
    }
  }
  return out;
}

function assertScope(name, value) {
  if (value !== 'project' && value !== 'global') {
    throw new Error(`${name} must be project|global, got ${value}`);
  }
}

export function normalizeHosts(hosts) {
  if (!hosts || !hosts.length) {
    throw new Error('At least one --hosts value is required (or answer the interactive prompt)');
  }
  for (const h of hosts) {
    if (!HOSTS.includes(h)) {
      throw new Error(`Unknown host "${h}". Allowed: ${HOSTS.join(', ')}`);
    }
  }
  return [...new Set(hosts)];
}

async function ask(rl, question, defaultValue) {
  const suffix = defaultValue != null ? ` [${defaultValue}]` : '';
  const answer = await new Promise((resolve) => {
    rl.question(`${question}${suffix}: `, resolve);
  });
  const t = answer.trim();
  return t || defaultValue;
}

export async function resolveInstallOptions(flags, { interactive, defaults = {} }) {
  const opts = { ...flags };
  let rl;
  const needAsk = interactive && !flags.yes && process.stdin.isTTY;

  if (needAsk) {
    rl = readline.createInterface({ input: process.stdin, output: process.stdout });
  }

  try {
    const defHosts = defaults.hosts?.length
      ? defaults.hosts.join(',')
      : 'cursor,claude-code';
    const defPack = defaults.packScope || 'project';
    const defOpenspec = defaults.openspecScope || 'project';
    const defSuperpowers = defaults.superpowersScope || 'project';

    if (!opts.hosts) {
      if (needAsk) {
        const ans = await ask(
          rl,
          `Hosts (comma-separated: ${HOSTS.join(',')})`,
          defHosts,
        );
        opts.hosts = ans.split(',').map((s) => s.trim()).filter(Boolean);
      } else {
        opts.hosts = defaults.hosts?.length
          ? [...defaults.hosts]
          : ['cursor', 'claude-code'];
      }
    }
    opts.hosts = normalizeHosts(opts.hosts);

    if (!opts.packScope) {
      opts.packScope = needAsk
        ? await ask(rl, 'ai4se-plugins scope (project|global)', defPack)
        : defPack;
    }
    if (!opts.openspecScope) {
      opts.openspecScope = needAsk
        ? await ask(rl, 'OpenSpec scope (project|global)', defOpenspec)
        : defOpenspec;
    }
    if (!opts.superpowersScope) {
      opts.superpowersScope = needAsk
        ? await ask(rl, 'Superpowers scope (project|global)', defSuperpowers)
        : defSuperpowers;
    }

    assertScope('pack-scope', opts.packScope);
    assertScope('openspec-scope', opts.openspecScope);
    assertScope('superpowers-scope', opts.superpowersScope);

    if (opts.mode !== 'symlink' && opts.mode !== 'copy') {
      throw new Error('--mode must be symlink|copy');
    }

    return opts;
  } finally {
    if (rl) rl.close();
  }
}

export async function confirm(message, { yes }) {
  if (yes) return true;
  if (!process.stdin.isTTY) {
    throw new Error(`Refusing privileged step without --yes: ${message}`);
  }
  const rl = readline.createInterface({ input: process.stdin, output: process.stdout });
  try {
    const ans = await new Promise((resolve) => {
      rl.question(`${message} [y/N]: `, resolve);
    });
    return /^y(es)?$/i.test(ans.trim());
  } finally {
    rl.close();
  }
}

export function printHelp() {
  console.log(`ai4se-plugins installer

Usage:
  node install/bin/ai4se-plugins.mjs check [--cwd DIR] [--pack-root DIR] [--pack-scope project|global]
  node install/bin/ai4se-plugins.mjs install [options]
  node install/bin/ai4se-plugins.mjs verify [options]
  node install/bin/ai4se-plugins.mjs version [--pack-root DIR]
  node install/bin/ai4se-plugins.mjs version bump [major|minor|patch] [--pack-root DIR]
  node install/bin/ai4se-plugins.mjs version set <X.Y.Z> [--pack-root DIR]
  node install/bin/ai4se-plugins.mjs --version

Options:
  --hosts cursor,claude-code,codex,github-copilot,qoder
  --pack-scope project|global
  --openspec-scope project|global
  --superpowers-scope project|global
  --pack-root DIR
  --cwd DIR
  --mode symlink|copy
  --yes                 skip confirmation prompts (CI/agent)
  --force               re-apply steps even when options/artifacts unchanged
  --skip-openspec | --skip-superpowers | --skip-pack

Version:
  Pack semver lives in pack-root/VERSION. Install writes it to the client as
  .agents/ai4se-plugins-VERSION (+ install manifest packVersion).
  version bump updates VERSION in this pack (maintainers). A client reinstall
  with a newer pack version refreshes hub + host projections.

Reinstall:
  Existing project state is printed first. Steps whose options and artifacts
  are unchanged are skipped; change an option, bump pack version, or pass
  --force to re-run.
`);
}
