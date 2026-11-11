import { useCallback, useEffect, useRef, useState } from 'react';
import { ApiError, getJson, sendForm } from '../lib/api';
import type { SessionView } from '../lib/confirmation';
import { moment } from '../lib/confirmation';
import type { ImportBatch } from '../lib/imports';
import { canImport, errorLine, outcomeLine, refusalLine, statusLabel } from '../lib/imports';

/**
 * The coordinator's import screen: one upload, the batch history and the rows each rejected batch
 * kept. The workbook is the only channel for person data and one upload is one unit of work, so
 * the two failures read differently on purpose — a file whose shape is wrong is refused and leaves
 * nothing behind, while a file with bad rows exists as a rejected batch that wrote nothing and
 * lists every row to fix.
 *
 * The history answers counts only, so each rejected batch is read again by id for its error rows.
 */
function withErrors(batch: ImportBatch): Promise<ImportBatch> {
  if (batch.status !== 'REJECTED') {
    return Promise.resolve(batch);
  }
  return getJson<ImportBatch>(`/api/imports/${batch.id}`).then((detail) => ({
    ...batch,
    errors: detail.errors,
  }));
}

export default function ImportScreen() {
  const [batches, setBatches] = useState<ImportBatch[] | null>(null);
  const [viewer, setViewer] = useState<SessionView | null>(null);
  const [file, setFile] = useState<File | null>(null);
  const [refused, setRefused] = useState<string | null>(null);
  const [uploaded, setUploaded] = useState<ImportBatch | null>(null);
  const [busy, setBusy] = useState(false);
  const picker = useRef<HTMLInputElement>(null);

  const load = useCallback(() => {
    let cancelled = false;
    Promise.all([getJson<ImportBatch[]>('/api/imports'), getJson<SessionView>('/api/session')])
      .then(([rows, session]) => Promise.all(rows.map(withErrors)).then((full) => [full, session] as const))
      .then(([full, session]) => {
        if (!cancelled) {
          setBatches(full);
          setViewer(session);
        }
      })
      .catch((caught: unknown) => {
        if (!cancelled) {
          setRefused(caught instanceof ApiError ? refusalLine(caught) : String(caught));
        }
      });
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => load(), [load]);

  const upload = () => {
    if (file === null) {
      return;
    }
    const form = new FormData();
    form.append('file', file);
    setBusy(true);
    setRefused(null);
    setUploaded(null);
    sendForm<ImportBatch>('/api/imports', form)
      .then((batch) => {
        setUploaded(batch);
        setFile(null);
        if (picker.current !== null) {
          picker.current.value = '';
        }
        load();
      })
      .catch((caught: unknown) => {
        setRefused(caught instanceof ApiError ? refusalLine(caught) : String(caught));
      })
      .finally(() => setBusy(false));
  };

  const writable = viewer !== null && canImport(viewer.role);

  return (
    <main className="mx-auto max-w-4xl p-8">
      <h1 className="text-2xl font-semibold tracking-tight text-slate-900">People import</h1>
      <p className="mt-1 text-sm text-slate-600">
        One workbook, two sheets — <code>people</code> and <code>skills</code>. A file whose shape
        is wrong is refused and leaves no batch behind; a file with bad rows is recorded as a
        rejected batch, writes nothing and lists every row to fix.
      </p>

      <section className="mt-6 rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
        <h2 className="text-base font-semibold text-slate-900">Upload</h2>
        <div className="mt-2 flex items-center gap-2">
          <input
            ref={picker}
            type="file"
            accept=".xlsx"
            disabled={!writable || busy}
            className="text-sm text-slate-700"
            onChange={(event) => setFile(event.target.files?.[0] ?? null)}
          />
          <button
            type="button"
            className="rounded border border-slate-300 px-2 py-1 text-sm text-slate-700 disabled:text-slate-400"
            disabled={!writable || busy || file === null}
            onClick={upload}
          >
            Upload
          </button>
        </div>
        {uploaded !== null ? (
          <p className="mt-3 text-sm text-slate-800">
            {uploaded.filename} · {outcomeLine(uploaded)}
          </p>
        ) : null}
        {refused !== null ? (
          <p className="mt-3 rounded border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-800">
            {refused}
          </p>
        ) : null}
        {viewer !== null && !writable ? (
          <p className="mt-3 text-sm text-slate-600">
            Signed in as {viewer.displayName}; only the coordinator imports a workbook.
          </p>
        ) : null}
      </section>

      <section className="mt-6 rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
        <h2 className="text-base font-semibold text-slate-900">Batch history</h2>
        {batches === null ? (
          <p className="mt-2 text-sm text-slate-600">Reading the batches…</p>
        ) : batches.length === 0 ? (
          <p className="mt-2 text-sm text-slate-600">No workbook has been uploaded yet.</p>
        ) : (
          <ul className="mt-2 space-y-3">
            {batches.map((batch) => (
              <li key={batch.id} className="rounded border border-slate-200 p-3">
                <p className="text-sm text-slate-900">
                  {batch.filename} · {statusLabel(batch)}
                </p>
                <p className="text-xs text-slate-500">
                  {batch.uploader} · {moment(batch.uploadedAt)} · batch {batch.id}
                </p>
                <p className="mt-1 text-sm text-slate-700">{outcomeLine(batch)}</p>
                {batch.errors.length > 0 ? (
                  <ul className="mt-2 space-y-1 text-sm text-red-800">
                    {batch.errors.map((error) => (
                      <li key={`${error.sheet}-${error.rowNo}-${error.key ?? ''}`}>
                        {errorLine(error)}
                      </li>
                    ))}
                  </ul>
                ) : null}
              </li>
            ))}
          </ul>
        )}
      </section>
    </main>
  );
}
