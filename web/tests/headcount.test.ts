import assert from 'node:assert/strict';
import test from 'node:test';
import {
  axisTop,
  barHeight,
  chartTicks,
  isEmpty,
  shareText,
  totalText,
} from '../src/lib/headcount.ts';
import type { HeadcountReport } from '../src/lib/headcount.ts';

/**
 * T06, the chart section's own readings. The share is printed exactly as the server rounded it, the
 * axis is derived from the tallest bar, and a pool with nobody is the one case that replaces the
 * bars instead of drawing them.
 */

const report = (total: number): HeadcountReport => ({
  total,
  byRole: [{ roleId: 1, role: 'PM', count: total, share: total === 0 ? 0 : 100 }],
  byGrade: [{ grade: 'Junior', count: 0, share: 0 }],
});

test('a share is printed with one decimal, never recomputed', () => {
  assert.equal(shareText(35.4), '35.4%');
  assert.equal(shareText(0), '0.0%');
  assert.equal(shareText(100), '100.0%');
});

test('the axis is topped by the tallest bar and divides into five steps', () => {
  assert.deepEqual(chartTicks(40), [40, 30, 20, 10, 0]);
  assert.deepEqual(chartTicks(42), [50, 40, 30, 20, 10, 0]);
  assert.deepEqual(chartTicks(8), [8, 6, 4, 2, 0]);
  assert.deepEqual(chartTicks(0), [0]);
});

test('the tallest bar is zero when nobody holds a category', () => {
  assert.equal(axisTop([{ count: 0 }, { count: 0 }]), 0);
  assert.equal(axisTop([{ count: 3 }, { count: 40 }]), 40);
  assert.equal(axisTop([]), 0);
});

test('a bar is proportional to its count and a zero bar has no height', () => {
  assert.equal(barHeight(40, 50), 136);
  assert.equal(barHeight(1, 50), 3);
  assert.equal(barHeight(0, 40), 0);
  assert.equal(barHeight(5, 0), 0);
});

test('an empty pool is the state that replaces the bars', () => {
  assert.equal(isEmpty(report(0)), true);
  assert.equal(isEmpty(report(1)), false);
});

test('each chart header carries its total', () => {
  assert.equal(totalText(113), 'Total 113');
  assert.equal(totalText(0), 'Total 0');
});
