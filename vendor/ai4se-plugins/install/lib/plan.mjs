import fs from 'node:fs';
import path from 'node:path';
import {
  agentsSkillsDir,
  detectOpenspec,
  detectPackHub,
  detectProjectOpenspec,
  detectSuperpowers,
  HOSTS,
} from './detect.mjs';
import { findHostProjection } from './verify.mjs';
import {
  optionsDiff,
  optionsEqual,
  readInstallManifest,
} from './manifest.mjs';
import {
  compareSemver,
  readClientPackVersion,
} from './version.mjs';

function resolveLinkTarget(linkPath) {
  try {
    const st = fs.lstatSync(linkPath);
    if (!st.isSymbolicLink()) return null;
    return path.resolve(path.dirname(linkPath), fs.readlinkSync(linkPath));
  } catch {
    return null;
  }
}

export function hubAlreadyMatches({ packRoot, cwd, packScope, mode }) {
  const hub = path.join(agentsSkillsDir(packScope, cwd), 'ai4se-sdd');
  if (!fs.existsSync(hub)) return false;
  const skillsSrc = path.resolve(packRoot, 'skills');
  let st;
  try {
    st = fs.lstatSync(hub);
  } catch {
    return false;
  }
  if (mode === 'symlink') {
    if (!st.isSymbolicLink()) return false;
    const target = resolveLinkTarget(hub);
    return target === skillsSrc;
  }
  // copy mode: hub should be a real directory, not a symlink to another pack
  return st.isDirectory() && !st.isSymbolicLink();
}

export function superpowersDestReady(scope, cwd) {
  const dest = path.join(agentsSkillsDir(scope, cwd), 'superpowers');
  const direct = path.join(dest, 'brainstorming', 'SKILL.md');
  if (fs.existsSync(direct)) return { ready: true, path: dest };
  // nested skills/ layout
  const nested = path.join(dest, 'skills', 'brainstorming', 'SKILL.md');
  if (fs.existsSync(nested)) return { ready: true, path: dest };
  return { ready: false, path: dest };
}

export function detectExistingInstall(cwd, packScopeHint = 'project', packRoot) {
  const previous = readInstallManifest(cwd);
  const packScope = previous?.packScope || packScopeHint;
  const packHub = detectPackHub(packScope, cwd);
  const packHubAlt =
    packScope === 'project'
      ? detectPackHub('global', cwd)
      : detectPackHub('project', cwd);
  const openspecCli = detectOpenspec();
  const openspecProject = detectProjectOpenspec(cwd);
  const superpowers = detectSuperpowers(cwd);
  const projectedHosts = [];
  for (const h of HOSTS) {
    const marker = findHostProjection(cwd, packScope, h, packRoot);
    if (marker.ok) projectedHosts.push(h);
  }

  const present =
    Boolean(previous) ||
    packHub.exists ||
    openspecProject.dirExists ||
    projectedHosts.length > 0 ||
    superpowers.installed;

  const installedPackVersion =
    previous?.packVersion || readClientPackVersion(cwd) || null;

  return {
    present,
    previous,
    packScope,
    packHub,
    packHubAlt,
    openspecCli,
    openspecProject,
    superpowers,
    projectedHosts,
    installedPackVersion,
  };
}

export function printExistingSummary(existing, packVersion = null) {
  if (packVersion) {
    console.log(`Pack version (source): ${packVersion}`);
  }
  if (!existing.present) {
    console.log('No previous ai4se-plugins install detected in this project.');
    return;
  }
  console.log('Detected existing ai4se-plugins state:');
  if (existing.installedPackVersion) {
    const cmp =
      packVersion != null
        ? compareSemver(existing.installedPackVersion, packVersion)
        : 0;
    let note = '';
    if (packVersion != null) {
      if (cmp < 0) note = ` (upgrade available → ${packVersion})`;
      else if (cmp > 0) note = ` (newer than pack source ${packVersion})`;
      else note = ' (up to date)';
    }
    console.log(`  Installed pack version: ${existing.installedPackVersion}${note}`);
  } else {
    console.log('  Installed pack version: (unknown — pre-version install)');
  }
  if (existing.previous) {
    console.log(
      `  Last install: ${existing.previous.installedAt || '(unknown time)'}`,
    );
    console.log(
      `  Last options: pack=${existing.previous.packScope} openspec=${existing.previous.openspecScope} superpowers=${existing.previous.superpowersScope}`,
    );
    console.log(
      `  Last hosts: ${(existing.previous.hosts || []).join(', ') || '(none)'}`,
    );
    if (existing.previous.packRoot) {
      console.log(`  Last packRoot: ${existing.previous.packRoot}`);
    }
  } else {
    console.log('  (no install manifest yet — artifacts found on disk)');
  }
  console.log(
    `  Pack hub (${existing.packScope}): ${
      existing.packHub.exists
        ? `${existing.packHub.path} (${existing.packHub.skillCount} SKILL.md)`
        : 'missing'
    }`,
  );
  if (existing.packHubAlt.exists) {
    console.log(
      `  Pack hub (other scope): ${existing.packHubAlt.path} (${existing.packHubAlt.skillCount} SKILL.md)`,
    );
  }
  console.log(
    `  OpenSpec CLI: ${
      existing.openspecCli.installed
        ? existing.openspecCli.version
        : 'not found'
    }`,
  );
  console.log(
    `  OpenSpec project tree: ${
      existing.openspecProject.dirExists
        ? existing.openspecProject.path
        : 'missing'
    }`,
  );
  console.log(
    `  Superpowers: ${
      existing.superpowers.installed
        ? existing.superpowers.brainstormingPath
        : 'not found'
    }`,
  );
  console.log(
    `  Host projections: ${
      existing.projectedHosts.length
        ? existing.projectedHosts.join(', ')
        : 'none detected'
    }`,
  );
}

/**
 * Decide which install steps to run.
 * Unchanged options + healthy artifacts → skip.
 * --force → apply all non-skipped components.
 */
export function buildInstallPlan({
  opts,
  existing,
  force = false,
}) {
  const previous = existing.previous;
  const sameOptions = previous ? optionsEqual(previous, opts) : false;
  const changes = optionsDiff(previous, opts);
  const installedVersion =
    existing.installedPackVersion || previous?.packVersion || null;
  const packVersion = opts.packVersion || null;
  const versionChanged =
    Boolean(packVersion) &&
    (installedVersion == null ||
      compareSemver(installedVersion, packVersion) !== 0);
  if (versionChanged) {
    changes.push(
      `packVersion: ${installedVersion ?? '(none)'} → ${packVersion}`,
    );
  }
  const reasons = {};
  const actions = {
    openspec: 'apply',
    superpowers: 'apply',
    pack: 'apply',
    hosts: 'apply',
  };

  const openspecReady =
    existing.openspecCli.installed &&
    (opts.openspecScope === 'global' || existing.openspecProject.dirExists);
  const openspecOptsSame =
    !previous ||
    (previous.openspecScope === opts.openspecScope &&
      Boolean(previous.skipOpenspec) === Boolean(opts.skipOpenspec) &&
      sameHosts(previous.hosts, opts.hosts));

  if (opts.skipOpenspec) {
    actions.openspec = 'skip';
    reasons.openspec = '--skip-openspec';
  } else if (!force && openspecReady && (sameOptions || openspecOptsSame || !previous)) {
    actions.openspec = 'skip';
    reasons.openspec = previous
      ? 'OpenSpec already ready (options unchanged for this step)'
      : 'OpenSpec already ready on disk';
  }

  const spDest = superpowersDestReady(opts.superpowersScope, opts.cwd);
  const spOptsSame =
    !previous || previous.superpowersScope === opts.superpowersScope;
  if (opts.skipSuperpowers) {
    actions.superpowers = 'skip';
    reasons.superpowers = '--skip-superpowers';
  } else if (!force && spDest.ready && (sameOptions || spOptsSame || !previous)) {
    actions.superpowers = 'skip';
    reasons.superpowers = `already linked for ${opts.superpowersScope} scope (${spDest.path})`;
  }

  if (opts.skipPack) {
    actions.pack = 'skip';
    actions.hosts = 'skip';
    reasons.pack = '--skip-pack';
    reasons.hosts = '--skip-pack';
  } else if (versionChanged && !force) {
    actions.pack = 'apply';
    actions.hosts = 'apply';
    reasons.pack = `pack version change (${installedVersion ?? 'none'} → ${packVersion})`;
    reasons.hosts = `pack version change (${installedVersion ?? 'none'} → ${packVersion})`;
  } else {
    const hubOk = hubAlreadyMatches({
      packRoot: opts.packRoot,
      cwd: opts.cwd,
      packScope: opts.packScope,
      mode: opts.mode,
    });
    const packOptsSame =
      !previous ||
      (previous.packScope === opts.packScope &&
        (previous.mode || 'symlink') === opts.mode &&
        path.resolve(previous.packRoot || '') === path.resolve(opts.packRoot));

    if (!force && hubOk && (sameOptions || packOptsSame || !previous) && !versionChanged) {
      actions.pack = 'skip';
      reasons.pack = 'hub already matches packRoot/scope/mode';
    }

    const hostsSame = !previous || sameHosts(previous.hosts, opts.hosts);
    const allProjected = opts.hosts.every((h) => {
      const m = findHostProjection(opts.cwd, opts.packScope, h, opts.packRoot);
      return m.ok;
    });
    const packScopeSame = !previous || previous.packScope === opts.packScope;

    if (
      !force &&
      actions.pack === 'skip' &&
      allProjected &&
      (sameOptions || (hostsSame && packScopeSame) || !previous) &&
      !versionChanged
    ) {
      actions.hosts = 'skip';
      reasons.hosts = 'hosts projections already present';
    } else if (!opts.skipPack && actions.pack !== 'skip') {
      // pack applying — hosts follow
      if (!reasons.hosts) {
        if (force) reasons.hosts = '--force';
        else if (!allProjected) reasons.hosts = 'missing host projection(s)';
        else if (previous && !hostsSame) reasons.hosts = 'host list changed';
        else reasons.hosts = 'pack hub changing';
      }
    } else if (!opts.skipPack) {
      actions.hosts = 'apply';
      if (force) reasons.hosts = '--force';
      else if (!allProjected) reasons.hosts = 'missing host projection(s)';
      else if (previous && !hostsSame) reasons.hosts = 'host list changed';
      else if (actions.pack === 'apply') reasons.hosts = 'pack hub changing';
      else reasons.hosts = 'refresh host projections';
    }
  }

  if (force) {
    if (!opts.skipOpenspec) {
      actions.openspec = 'apply';
      reasons.openspec = '--force';
    }
    if (!opts.skipSuperpowers) {
      actions.superpowers = 'apply';
      reasons.superpowers = '--force';
    }
    if (!opts.skipPack) {
      actions.pack = 'apply';
      actions.hosts = 'apply';
      reasons.pack = '--force';
      reasons.hosts = '--force';
    }
  }

  const anyApply = Object.values(actions).some((a) => a === 'apply');

  return {
    isReinstall: existing.present,
    sameOptions: sameOptions && !versionChanged,
    versionChanged,
    packVersion,
    installedVersion,
    changes,
    actions,
    reasons,
    anyApply,
  };
}

function sameHosts(a, b) {
  const aa = [...(a || [])].map(String).sort().join(',');
  const bb = [...(b || [])].map(String).sort().join(',');
  return aa === bb;
}

export function printInstallPlan(plan) {
  if (plan.packVersion) {
    console.log(
      `Pack version: ${plan.packVersion}` +
        (plan.installedVersion
          ? ` (client had ${plan.installedVersion})`
          : ' (fresh client)'),
    );
  }
  console.log('Install plan:');
  for (const key of ['openspec', 'superpowers', 'pack', 'hosts']) {
    const action = plan.actions[key];
    const reason = plan.reasons[key] || '';
    console.log(
      `  ${key}: ${action}${reason ? ` — ${reason}` : ''}`,
    );
  }
  if (plan.changes.length && !plan.sameOptions) {
    console.log('Option changes vs last install:');
    for (const line of plan.changes) console.log(`  - ${line}`);
  } else if (plan.sameOptions) {
    console.log('Options and pack version match last install.');
  }
}
