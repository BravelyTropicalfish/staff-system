import fs from 'node:fs';
import path from 'node:path';
import { linkOrCopyFile, rmIfExists } from './hub.mjs';

/**
 * Ready pack agents under agents/<name>/AGENT.md (skips _TEMPLATE, README).
 * Returns [{ name, id, status, src }].
 */
export function listPackAgents(packRoot, { readyOnly = true } = {}) {
  const agentsRoot = path.join(packRoot, 'agents');
  const out = [];
  if (!fs.existsSync(agentsRoot)) return out;

  for (const ent of fs.readdirSync(agentsRoot, { withFileTypes: true })) {
    if (!ent.isDirectory()) continue;
    if (ent.name.startsWith('_') || ent.name.startsWith('.')) continue;
    const src = path.join(agentsRoot, ent.name, 'AGENT.md');
    if (!fs.existsSync(src)) continue;
    const meta = parseAgentFrontmatter(src);
    if (readyOnly && meta.status !== 'ready') continue;
    out.push({
      name: meta.name || ent.name,
      id: meta.id || `agent.${ent.name}`,
      status: meta.status || 'draft',
      src,
      dirName: ent.name,
    });
  }
  return out.sort((a, b) => a.name.localeCompare(b.name));
}

export function parseAgentFrontmatter(file) {
  const text = fs.readFileSync(file, 'utf8');
  const m = text.match(/^---\r?\n([\s\S]*?)\r?\n---/);
  if (!m) return {};
  const fields = {};
  for (const line of m[1].split(/\r?\n/)) {
    const kv = line.match(/^([A-Za-z0-9_-]+):\s*(.*)$/);
    if (kv) fields[kv[1]] = kv[2].trim();
  }
  return fields;
}

/**
 * Write flattened host agent files: <agentsDir>/<fileName> ← AGENT.md.
 * Only replaces files we own (matching pack agent names); does not wipe the dir.
 *
 * @param {object} opts
 * @param {(agent: {name: string}) => string} [opts.fileName]
 *   Default: `${name}.md`. Copilot uses `${name}.agent.md`.
 */
export function projectPackAgents({
  packRoot,
  agentsDir,
  mode = 'symlink',
  readyOnly = true,
  fileName = (agent) => `${agent.name}.md`,
}) {
  const agents = listPackAgents(packRoot, { readyOnly });
  const files = [];
  if (!agents.length) return files;

  fs.mkdirSync(agentsDir, { recursive: true });
  for (const agent of agents) {
    const dest = path.join(agentsDir, fileName(agent));
    linkOrCopyFile(agent.src, dest, mode);
    files.push(dest);
  }
  return files;
}

/**
 * Remove previously projected pack agent files (by name) from a host agents dir.
 */
export function clearProjectedPackAgents(
  packRoot,
  agentsDir,
  fileName = (agent) => `${agent.name}.md`,
) {
  if (!fs.existsSync(agentsDir)) return;
  // Include non-ready so upgrades that drop status still clean old projections
  const agents = listPackAgents(packRoot, { readyOnly: false });
  for (const agent of agents) {
    rmIfExists(path.join(agentsDir, fileName(agent)));
  }
}
