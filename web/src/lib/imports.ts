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

/** 批次状态，用屏幕上的说法。 */
export function statusLabel(batch: Pick<ImportBatch, 'status'>): string {
  return batch.status === 'APPLIED' ? '已应用' : '已拒绝';
}

function rows(count: number, what: string): string {
  return `${what} ${count} 行`;
}

/**
 * 一个批次对人员池做了什么，一句话说完：已应用的写明写了什么，已拒绝的说明一字未写、还有多少
 * 行待修。
 */
export function outcomeLine(
  batch: Pick<ImportBatch, 'status' | 'personRows' | 'skillRows' | 'errorCount'>,
): string {
  const counts = `${rows(batch.personRows, '人员')} · ${rows(batch.skillRows, '技能')}`;
  return batch.status === 'APPLIED'
    ? `已应用 · 写入 ${counts}`
    : `已拒绝 · 未写入任何数据 · ${batch.errorCount} 行待修`;
}

/** 一行被拒的记录，按它在工作簿里的位置与它的键值定位。 */
export function errorLine(error: RowError): string {
  const where = `${error.sheet} 第 ${error.rowNo} 行`;
  return `${error.key ? `${where} · ${error.key}` : where} — ${error.message}`;
}

/**
 * 失败中“被拒”的那一半：上传直接返回失败码，和“已拒绝的批次”不同，它在历史里什么都没留下。
 */
export function refusalLine(error: { code: string; message: string }): string {
  return `上传被拒 · ${error.code}: ${error.message} · 未记录任何批次`;
}

/** Only the coordinator sends people data in. */
export function canImport(role: string): boolean {
  return role === 'COORDINATOR';
}
