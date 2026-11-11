import fs from 'node:fs';
import path from 'node:path';

const SEMVER_RE = /^(\d+)\.(\d+)\.(\d+)(?:-([0-9A-Za-z.-]+))?(?:\+([0-9A-Za-z.-]+))?$/;

export function versionFilePath(packRoot) {
  return path.join(packRoot, 'VERSION');
}

export function clientVersionPath(cwd) {
  return path.join(path.resolve(cwd), '.agents', 'ai4se-plugins-VERSION');
}

export function parseSemver(raw) {
  const text = String(raw || '').trim();
  const m = SEMVER_RE.exec(text);
  if (!m) return null;
  return {
    raw: text,
    major: Number(m[1]),
    minor: Number(m[2]),
    patch: Number(m[3]),
    prerelease: m[4] || null,
    build: m[5] || null,
  };
}

export function formatSemver({ major, minor, patch, prerelease, build }) {
  let s = `${major}.${minor}.${patch}`;
  if (prerelease) s += `-${prerelease}`;
  if (build) s += `+${build}`;
  return s;
}

/**
 * Compare two semver strings. Returns -1 / 0 / 1.
 * Invalid versions sort as older than valid ones.
 */
export function compareSemver(a, b) {
  const aa = parseSemver(a);
  const bb = parseSemver(b);
  if (!aa && !bb) return 0;
  if (!aa) return -1;
  if (!bb) return 1;
  if (aa.major !== bb.major) return aa.major < bb.major ? -1 : 1;
  if (aa.minor !== bb.minor) return aa.minor < bb.minor ? -1 : 1;
  if (aa.patch !== bb.patch) return aa.patch < bb.patch ? -1 : 1;
  // prerelease: absent > present (1.0.0 > 1.0.0-rc.1)
  if (aa.prerelease && !bb.prerelease) return -1;
  if (!aa.prerelease && bb.prerelease) return 1;
  if (aa.prerelease && bb.prerelease) {
    if (aa.prerelease < bb.prerelease) return -1;
    if (aa.prerelease > bb.prerelease) return 1;
  }
  return 0;
}

export function readPackVersion(packRoot) {
  const file = versionFilePath(packRoot);
  if (!fs.existsSync(file)) {
    throw new Error(`Pack VERSION file missing: ${file}`);
  }
  const raw = fs.readFileSync(file, 'utf8').trim();
  if (!parseSemver(raw)) {
    throw new Error(`Invalid pack VERSION "${raw}" in ${file} (want X.Y.Z)`);
  }
  return raw;
}

export function writePackVersion(packRoot, version) {
  const parsed = parseSemver(version);
  if (!parsed) {
    throw new Error(`Invalid version "${version}" (want X.Y.Z)`);
  }
  const file = versionFilePath(packRoot);
  fs.writeFileSync(file, `${parsed.raw}\n`, 'utf8');
  return parsed.raw;
}

export function bumpPackVersion(packRoot, level = 'patch') {
  const current = readPackVersion(packRoot);
  const parsed = parseSemver(current);
  const next = { ...parsed, prerelease: null, build: null };
  switch (level) {
    case 'major':
      next.major += 1;
      next.minor = 0;
      next.patch = 0;
      break;
    case 'minor':
      next.minor += 1;
      next.patch = 0;
      break;
    case 'patch':
      next.patch += 1;
      break;
    default:
      throw new Error(`Unknown bump level "${level}" (want major|minor|patch)`);
  }
  const formatted = formatSemver(next);
  writePackVersion(packRoot, formatted);
  return { previous: current, next: formatted, level };
}

export function readClientPackVersion(cwd) {
  const file = clientVersionPath(cwd);
  if (fs.existsSync(file)) {
    const raw = fs.readFileSync(file, 'utf8').trim();
    if (parseSemver(raw)) return raw;
  }
  return null;
}

export function writeClientPackVersion(cwd, version) {
  const parsed = parseSemver(version);
  if (!parsed) {
    throw new Error(`Invalid version "${version}"`);
  }
  const file = clientVersionPath(cwd);
  fs.mkdirSync(path.dirname(file), { recursive: true });
  fs.writeFileSync(file, `${parsed.raw}\n`, 'utf8');
  return file;
}
