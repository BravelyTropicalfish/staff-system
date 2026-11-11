import assert from 'node:assert/strict';
import test from 'node:test';
import {
  canImport,
  errorLine,
  outcomeLine,
  refusalLine,
  statusLabel,
} from '../src/lib/imports.ts';
import type { ImportBatch } from '../src/lib/imports.ts';

/**
 * T10, the import screen's own readings. One upload is one unit of work, so the two failures must
 * never read alike: a refusal leaves no batch behind, while a rejected batch exists, wrote nothing
 * and lists the rows to fix by sheet and row number.
 */

const applied: ImportBatch = {
  id: 2,
  filename: 'people.xlsx',
  uploader: 'Resource coordinator',
  status: 'APPLIED',
  uploadedAt: '2026-11-11T09:12:33.123',
  personRows: 2,
  skillRows: 1,
  errorCount: 0,
  errors: [],
};

const rejected: ImportBatch = {
  ...applied,
  id: 1,
  status: 'REJECTED',
  personRows: 1,
  skillRows: 0,
  errorCount: 2,
  errors: [
    { sheet: 'people', rowNo: 2, key: 'E200', message: 'role Product is not one of the active roles' },
    { sheet: 'skills', rowNo: 4, key: 'E999', message: 'person not found' },
  ],
};

test('an applied batch names what it wrote, a rejected one names what is left to fix', () => {
  assert.equal(statusLabel(applied), 'applied');
  assert.equal(statusLabel(rejected), 'rejected');
  assert.equal(outcomeLine(applied), 'applied · 2 person rows · 1 skill row written');
  assert.equal(outcomeLine(rejected), 'rejected · nothing was written · 2 rows to fix');
});

test('the counts keep their singular where one row is meant', () => {
  assert.equal(
    outcomeLine({ status: 'APPLIED', personRows: 0, skillRows: 1, errorCount: 0 }),
    'applied · 0 person rows · 1 skill row written',
  );
  assert.equal(
    outcomeLine({ status: 'REJECTED', personRows: 1, skillRows: 0, errorCount: 1 }),
    'rejected · nothing was written · 1 row to fix',
  );
});

test('a refused row names its sheet, its row number and its key', () => {
  assert.equal(
    errorLine(rejected.errors[0]),
    'people row 2 · E200 — role Product is not one of the active roles',
  );
  assert.equal(errorLine(rejected.errors[1]), 'skills row 4 · E999 — person not found');
});

test('a row that could not name a person is located without a key', () => {
  assert.equal(
    errorLine({ sheet: 'skills', rowNo: 7, key: null, message: 'skill is blank' }),
    'skills row 7 — skill is blank',
  );
});

test('a refusal says it left no batch, which a rejection never does', () => {
  assert.equal(
    refusalLine({ code: 'unreadable workbook', message: 'the workbook holds only a people sheet' }),
    'refused · unreadable workbook: the workbook holds only a people sheet · no batch was recorded',
  );
  assert.ok(!outcomeLine(rejected).includes('no batch was recorded'));
});

test('only the coordinator imports a workbook', () => {
  assert.equal(canImport('COORDINATOR'), true);
  assert.equal(canImport('LEAD'), false);
});
