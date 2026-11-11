import fs from 'node:fs';
import path from 'node:path';

/**
 * Split a markdown table data row into cells (handles \| inside cells).
 */
function splitTableRow(line) {
  const raw = line.trim();
  if (!raw.startsWith('|') || !raw.endsWith('|')) return null;
  const inner = raw.slice(1, -1);
  const cells = [];
  let cur = '';
  for (let i = 0; i < inner.length; i += 1) {
    const ch = inner[i];
    if (ch === '\\' && i + 1 < inner.length) {
      cur += ch + inner[i + 1];
      i += 1;
      continue;
    }
    if (ch === '|') {
      cells.push(cur.trim());
      cur = '';
      continue;
    }
    cur += ch;
  }
  cells.push(cur.trim());
  return cells;
}

function stripTicks(s) {
  const t = (s || '').trim();
  if (t.startsWith('`') && t.endsWith('`') && t.length >= 2) {
    return t.slice(1, -1);
  }
  return t;
}

/**
 * Parse commands/catalog.md into host projection entries.
 * Returns [{ slash, gate, status, target, skillPath, impl }].
 */
export function parseCatalogCommands(packRoot) {
  const catalogPath = path.join(packRoot, 'commands', 'catalog.md');
  if (!fs.existsSync(catalogPath)) {
    throw new Error(`Catalog missing: ${catalogPath}`);
  }

  const text = fs.readFileSync(catalogPath, 'utf8');
  const rows = [];

  for (const line of text.split('\n')) {
    if (!line.includes('`/sdd:')) continue;
    const cells = splitTableRow(line);
    if (!cells || cells.length < 7) continue;

    const slashCell = stripTicks(cells[0]);
    if (!slashCell.startsWith('/sdd:')) continue;

    const slash = slashCell.slice('/sdd:'.length);
    const target = stripTicks(cells[1]);
    const skillPath = stripTicks(cells[2]);
    const impl = stripTicks(cells[3]);
    const status = stripTicks(cells[5]);
    const gate = stripTicks(cells[6]) === 'true';

    rows.push({ slash, target, skillPath, impl, status, gate });
  }

  if (rows.length === 0) {
    throw new Error(`No /sdd rows parsed from ${catalogPath}`);
  }

  // Stable order: facades first (p0–p5), then remaining by catalog order.
  const facade = [];
  const rest = [];
  for (const row of rows) {
    if (/^p[0-5]$/.test(row.slash)) facade.push(row);
    else rest.push(row);
  }
  facade.sort((a, b) => a.slash.localeCompare(b.slash));
  return [...facade, ...rest];
}

/** Qoder keeps its published leaf names; Codex uses the catalog command names. */
export function hostSkillEntries(packRoot, host) {
  return parseCatalogCommands(packRoot)
    .filter(row => row.target.startsWith('skill.'))
    .map(row => ({
      id: row.target,
      name: 'sdd-' + (host === 'qoder' ? path.basename(row.skillPath) : row.slash),
      src: path.join(packRoot, row.skillPath),
    }));
}
