import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import {
  agentsSkillsDir,
  detectSuperpowers,
  runCmd,
  which,
} from './detect.mjs';
import { linkOrCopy } from './hub.mjs';
import { superpowersDestReady } from './plan.mjs';
import { confirm } from './scope.mjs';

const SUPERPOWERS_REPO = 'https://github.com/obra/superpowers.git';

function ensureDir(d) {
  fs.mkdirSync(d, { recursive: true });
}

function cloneOrUpdate(dest) {
  if (fs.existsSync(path.join(dest, '.git'))) {
    const pull = runCmd('git', ['-C', dest, 'pull', '--ff-only']);
    if (pull.status !== 0) {
      console.warn(`git pull failed for ${dest}: ${pull.stderr || pull.stdout}`);
    }
    return dest;
  }
  ensureDir(path.dirname(dest));
  if (fs.existsSync(dest)) {
    fs.rmSync(dest, { recursive: true, force: true });
  }
  const clone = runCmd('git', ['clone', '--depth', '1', SUPERPOWERS_REPO, dest], {
    stdio: 'inherit',
  });
  if (clone.status !== 0) {
    throw new Error(`Failed to clone superpowers into ${dest}`);
  }
  return dest;
}

function skillsSrcFromClone(cloneDir) {
  const nested = path.join(cloneDir, 'skills');
  if (fs.existsSync(nested)) return nested;
  return cloneDir;
}

/**
 * Ensure Superpowers skills are discoverable.
 * Prefer clone+symlink into agents hub (works across hosts).
 * Also print official marketplace commands for Claude/Cursor when global.
 */
export async function ensureSuperpowers({
  cwd,
  superpowersScope,
  hosts,
  yes,
  mode = 'symlink',
  force = false,
  skip = false,
}) {
  if (skip) {
    return { skipped: true, warnings: [] };
  }

  const warnings = [];
  const scoped = superpowersDestReady(superpowersScope, cwd);
  if (scoped.ready && !force) {
    console.log(
      `Superpowers already present for ${superpowersScope} scope: ${scoped.path}`,
    );
    return {
      skipped: false,
      already: true,
      path: scoped.path,
      warnings,
    };
  }

  if (!which('git')) {
    throw new Error('git not on PATH — needed to install Superpowers from GitHub');
  }

  const cacheRoot = path.join(os.homedir(), '.cache', 'ai4se-plugins');
  const cloneDir = path.join(cacheRoot, 'superpowers');
  console.log(
    scoped.ready
      ? `Refreshing Superpowers link for ${superpowersScope} scope`
      : `Superpowers missing for ${superpowersScope} scope. Will clone ${SUPERPOWERS_REPO} → ${cloneDir}`,
  );
  const ok = await confirm('Clone Superpowers and link into agents skills hub?', { yes });
  if (!ok) {
    throw new Error('User declined Superpowers install');
  }

  cloneOrUpdate(cloneDir);
  const skillsSrc = skillsSrcFromClone(cloneDir);

  const hubParent = agentsSkillsDir(superpowersScope, cwd);
  ensureDir(hubParent);
  const dest = path.join(hubParent, 'superpowers');
  linkOrCopy(skillsSrc, dest, mode);

  for (const h of hosts) {
    if (h === 'qoder') {
      const base = superpowersScope === 'global' ? os.homedir() : cwd;
      const q = path.join(base, '.qoder', 'skills', 'superpowers');
      try {
        linkOrCopy(skillsSrc, q, mode);
      } catch (e) {
        warnings.push(`Qoder superpowers link failed: ${e.message}`);
      }
    }
    if (h === 'github-copilot' && superpowersScope === 'global') {
      const c = path.join(os.homedir(), '.copilot', 'skills', 'superpowers');
      try {
        linkOrCopy(skillsSrc, c, mode);
      } catch (e) {
        warnings.push(`Copilot superpowers link failed: ${e.message}`);
      }
    }
  }

  if (hosts.includes('claude-code') || hosts.includes('cursor')) {
    warnings.push(
      'Optional marketplace install (host UI): Claude `/plugin install superpowers@claude-plugins-official`; Cursor `/add-plugin superpowers`. Hub symlink is already in place for skill discovery.',
    );
  }

  const after = detectSuperpowers(cwd);
  if (!after.installed) {
    const direct = path.join(dest, 'brainstorming', 'SKILL.md');
    if (!fs.existsSync(direct)) {
      throw new Error('Superpowers linked but brainstorming skill not found');
    }
  }

  return {
    skipped: false,
    already: false,
    path: dest,
    warnings,
  };
}
