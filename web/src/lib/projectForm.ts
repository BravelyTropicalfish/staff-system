/**
 * The create-project form the two roles share. A lead opens a project of their own, so the lead
 * field is prefilled with the signed-in account and locked; the coordinator opens one for any lead.
 * The server enforces the same rule, so this module only spares a round trip, it does not decide.
 */

export type ProjectFormValue = {
  name: string;
  lead: string;
  startDate: string;
  endDate: string;
};

/** The signed-in account as `GET /api/session` answers it. */
export type Viewer = { username: string; role: string };

/** A blank form: the coordinator picks the lead, a lead is their own lead already. */
export function emptyProject(viewer: Viewer): ProjectFormValue {
  return {
    name: '',
    lead: viewer.role === 'LEAD' ? viewer.username : '',
    startDate: '',
    endDate: '',
  };
}

/** Only the coordinator chooses who leads the project. */
export function leadLocked(viewer: Viewer): boolean {
  return viewer.role === 'LEAD';
}

/** What is sent: the two names lose their padding, the dates are already `yyyy-mm-dd`. */
export function projectDraft(value: ProjectFormValue): ProjectFormValue {
  return {
    name: value.name.trim(),
    lead: value.lead.trim(),
    startDate: value.startDate,
    endDate: value.endDate,
  };
}

/** 表单自己能拦下的拒绝，措辞与服务端同义。 */
export function projectProblem(value: ProjectFormValue): string | null {
  const draft = projectDraft(value);
  if (draft.name === '') {
    return '请填写项目名称。';
  }
  if (draft.lead === '') {
    return '请指定项目经理。';
  }
  if (draft.startDate === '' || draft.endDate === '') {
    return '请填写起止日期。';
  }
  if (draft.endDate < draft.startDate) {
    return '结束日期不能早于开始日期。';
  }
  return null;
}
