/**
 * The payloads of `POST /api/imports` and the batch history, plus the readings the import screen
 * makes of them. One upload is one unit of work, and it can fail in two ways that must never read
 * alike: a file whose shape is wrong is refused and leaves no batch behind, while a file with bad
 * rows exists as a `REJECTED` batch that lists every row to fix and writes nothing.
 */

export type RowError = {
  /** `people` or `skills`, as the workbook names its sheets. */
  sheet: string;
  /** The spreadsheet's own row number, so the fix is a cell to open. */
  rowNo: number;
  /** The employee number the row was about; null when the row could not name one. */
  key: string | null;
  message: string;
};

export type ImportBatch = {
  id: number;
  filename: string;
  uploader: string;
  status: 'APPLIED' | 'REJECTED';
  uploadedAt: string;
  personRows: number;
  skillRows: number;
  errorCount: number;
  /** The history list answers counts only; `GET /api/imports/{id}` answers the rows. */
  errors: RowError[];
};

/** The status in the words the screen uses. */
export function statusLabel(batch: Pick<ImportBatch, 'status'>): string {
  return batch.status === 'APPLIED' ? 'applied' : 'rejected';
}

function rows(count: number, what = ''): string {
  return `${count} ${what ? `${what} ` : ''}${count === 1 ? 'row' : 'rows'}`;
}

/**
 * What the batch did to the pool, said in one line: an applied file names what it wrote, a rejected
 * one says that nothing was written and how much is left to fix.
 */
export function outcomeLine(
  batch: Pick<ImportBatch, 'status' | 'personRows' | 'skillRows' | 'errorCount'>,
): string {
  const counts = `${rows(batch.personRows, 'person')} · ${rows(batch.skillRows, 'skill')}`;
  return batch.status === 'APPLIED'
    ? `applied · ${counts} written`
    : `rejected · nothing was written · ${rows(batch.errorCount)} to fix`;
}

/** One refused row, located by its sheet and its spreadsheet row and named by its key. */
export function errorLine(error: RowError): string {
  const where = `${error.sheet} row ${error.rowNo}`;
  return `${error.key ? `${where} · ${error.key}` : where} — ${error.message}`;
}

/**
 * The refused half of a failure: the upload answered with a failure code and, unlike a rejected
 * batch, left nothing in the history at all.
 */
export function refusalLine(error: { code: string; message: string }): string {
  return `refused · ${error.code}: ${error.message} · no batch was recorded`;
}

/** Only the coordinator sends people data in. */
export function canImport(role: string): boolean {
  return role === 'COORDINATOR';
}
