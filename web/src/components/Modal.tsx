import { useEffect, type ReactNode } from 'react';

/**
 * 一个弹窗：标题、关闭按钮、内容。Esc 与点遮罩都能关，内容点击不冒泡。
 * 项目创建与人员档案都用它，页面本身不再因为一块表单或一张档案被撑长。
 */
export default function Modal({
  title,
  note,
  onClose,
  children,
}: {
  title: string;
  note?: string | null;
  onClose: () => void;
  children: ReactNode;
}) {
  useEffect(() => {
    const onKey = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        onClose();
      }
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [onClose]);

  return (
    <div className="modal-backdrop" role="presentation" onClick={onClose}>
      <div
        className="modal"
        role="dialog"
        aria-modal="true"
        aria-label={title}
        onClick={(event) => event.stopPropagation()}
      >
        <div className="modal-head">
          <div>
            <h2>{title}</h2>
            {note ? <div className="note">{note}</div> : null}
          </div>
          <button type="button" className="btn btn-sm" onClick={onClose}>
            关闭
          </button>
        </div>
        <div className="modal-body">{children}</div>
      </div>
    </div>
  );
}
