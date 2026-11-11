import fs from 'node:fs';
import path from 'node:path';
import {
  detectOpenspec,
  nodeVersionOk,
  OPENSPEC_TOOL_IDS,
  runCmd,
  which,
} from './detect.mjs';
import { confirm } from './scope.mjs';

function preferredPkgManager() {
  if (which('npm')) return 'npm';
  if (which('pnpm')) return 'pnpm';
  if (which('bun')) return 'bun';
  if (which('yarn')) return 'yarn';
  return null;
}

function installGlobalCmd(pm) {
  switch (pm) {
    case 'npm':
      return ['npm', ['install', '-g', '@fission-ai/openspec@latest']];
    case 'pnpm':
      return ['pnpm', ['add', '-g', '@fission-ai/openspec@latest']];
    case 'bun':
      return ['bun', ['add', '-g', '@fission-ai/openspec@latest']];
    case 'yarn':
      return ['yarn', ['global', 'add', '@fission-ai/openspec@latest']];
    default:
      return null;
  }
}

/**
 * Ensure OpenSpec CLI; optionally init project for selected hosts.
 */
export async function ensureOpenspec({
  cwd,
  openspecScope,
  hosts,
  yes,
  force = false,
  skip = false,
}) {
  if (skip) {
    return { skipped: true, warnings: [] };
  }

  const warnings = [];
  const node = nodeVersionOk();
  if (!node.ok) {
    throw new Error(
      `OpenSpec needs Node.js >= 20.19.0 (found ${node.version}). Install/upgrade Node yourself — installer will not change version managers.`,
    );
  }

  let info = detectOpenspec();
  if (!info.installed) {
    const pm = preferredPkgManager();
    if (!pm) {
      throw new Error('No npm/pnpm/yarn/bun on PATH — cannot install OpenSpec CLI');
    }
    const [cmd, args] = installGlobalCmd(pm);
    const cmdline = `${cmd} ${args.join(' ')}`;
    console.log(`OpenSpec CLI missing. Proposed global install:\n  ${cmdline}`);
    const ok = await confirm('Run global OpenSpec install?', { yes });
    if (!ok) {
      throw new Error('User declined OpenSpec CLI install');
    }
    const res = runCmd(cmd, args, { stdio: 'inherit' });
    if (res.status !== 0) {
      throw new Error(`OpenSpec install failed (exit ${res.status}). ${res.stderr}`);
    }
    info = detectOpenspec();
    if (!info.installed) {
      throw new Error(
        'OpenSpec installed but `openspec` not on PATH. Add the package manager global bin to PATH yourself, then re-run verify.',
      );
    }
  } else {
    console.log(`OpenSpec CLI ready: ${info.version}`);
  }

  // project init when scope is project (also recommended when global CLI but user wants project tools)
  if (openspecScope === 'project') {
    const toolIds = [];
    for (const h of hosts) {
      const id = OPENSPEC_TOOL_IDS[h];
      if (id) toolIds.push(id);
      else warnings.push(`Host "${h}" has no OpenSpec tool id — skipped in openspec init --tools`);
    }
    const unique = [...new Set(toolIds)];
    if (!unique.length) {
      warnings.push('No OpenSpec tool ids for selected hosts; skipping openspec init');
      return { skipped: false, installed: true, version: info.version, inited: false, warnings };
    }

    const openspecDir = path.join(cwd, 'openspec');
    const already = fs.existsSync(openspecDir);
    if (already && !force) {
      console.log(
        `OpenSpec project tree already present (${openspecDir}) — skipping init (use --force to refresh)`,
      );
      return {
        skipped: false,
        installed: true,
        version: info.version,
        inited: false,
        already: true,
        tools: unique,
        warnings,
      };
    }

    const initArgs = ['init', '--tools', unique.join(',')];
    console.log(
      `${already ? 'Refreshing' : 'Initializing'} OpenSpec in ${cwd}:\n  openspec ${initArgs.join(' ')}`,
    );
    const ok = await confirm('Run openspec init for this project?', { yes });
    if (!ok) {
      warnings.push('User declined openspec init');
      return { skipped: false, installed: true, version: info.version, inited: false, warnings };
    }
    const res = runCmd('openspec', initArgs, { cwd, stdio: 'inherit' });
    if (res.status !== 0) {
      throw new Error(`openspec init failed (exit ${res.status})`);
    }
    return {
      skipped: false,
      installed: true,
      version: info.version,
      inited: true,
      tools: unique,
      warnings,
    };
  }

  warnings.push(
    'openspec-scope=global: CLI only. Run `openspec init --tools …` inside each project that needs /opsx.',
  );
  return {
    skipped: false,
    installed: true,
    version: info.version,
    inited: false,
    warnings,
  };
}
