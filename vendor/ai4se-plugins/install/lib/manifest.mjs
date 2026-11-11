import fs from 'node:fs';
import path from 'node:path';
import { writeClientPackVersion } from './version.mjs';

/** Schema version for this JSON file (not the pack semver). */
const MANIFEST_SCHEMA = 2;

export function manifestPath(cwd) {
  return path.join(path.resolve(cwd), '.agents', 'ai4se-plugins-install.json');
}

export function readInstallManifest(cwd) {
  const file = manifestPath(cwd);
  if (!fs.existsSync(file)) return null;
  try {
    const data = JSON.parse(fs.readFileSync(file, 'utf8'));
    if (!data || typeof data !== 'object') return null;
    // Normalize legacy field: version → manifestSchema; packVersion may be absent
    if (data.packVersion == null && typeof data.version === 'string' && /^\d+\.\d+\.\d+/.test(data.version)) {
      data.packVersion = data.version;
    }
    if (data.manifestSchema == null && typeof data.version === 'number') {
      data.manifestSchema = data.version;
    }
    return data;
  } catch {
    return null;
  }
}

export function writeInstallManifest(cwd, opts) {
  const file = manifestPath(cwd);
  fs.mkdirSync(path.dirname(file), { recursive: true });
  const packVersion = opts.packVersion;
  const payload = {
    manifestSchema: MANIFEST_SCHEMA,
    packVersion,
    installedAt: new Date().toISOString(),
    packRoot: opts.packRoot,
    packScope: opts.packScope,
    openspecScope: opts.openspecScope,
    superpowersScope: opts.superpowersScope,
    hosts: [...opts.hosts].sort(),
    mode: opts.mode,
    skipOpenspec: Boolean(opts.skipOpenspec),
    skipSuperpowers: Boolean(opts.skipSuperpowers),
    skipPack: Boolean(opts.skipPack),
  };
  fs.writeFileSync(file, `${JSON.stringify(payload, null, 2)}\n`, 'utf8');
  let versionFile = null;
  if (packVersion) {
    versionFile = writeClientPackVersion(cwd, packVersion);
  }
  return { file, payload, versionFile };
}

export function optionsSnapshot(opts) {
  return {
    packRoot: path.resolve(opts.packRoot),
    packScope: opts.packScope,
    openspecScope: opts.openspecScope,
    superpowersScope: opts.superpowersScope,
    hosts: [...opts.hosts].map(String).sort().join(','),
    mode: opts.mode || 'symlink',
    skipOpenspec: Boolean(opts.skipOpenspec),
    skipSuperpowers: Boolean(opts.skipSuperpowers),
    skipPack: Boolean(opts.skipPack),
  };
}

export function optionsEqual(a, b) {
  if (!a || !b) return false;
  const aa = optionsSnapshot(a);
  const bb = optionsSnapshot(b);
  return Object.keys(aa).every((k) => aa[k] === bb[k]);
}

export function optionsDiff(previous, next) {
  if (!previous) return ['(no previous manifest — first install record)'];
  const a = optionsSnapshot(previous);
  const b = optionsSnapshot(next);
  const changes = [];
  for (const k of Object.keys(b)) {
    if (a[k] !== b[k]) {
      changes.push(`${k}: ${a[k] ?? '(none)'} → ${b[k]}`);
    }
  }
  return changes;
}
