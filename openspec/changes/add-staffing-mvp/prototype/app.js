/* Staffing MVP prototype — static data + rendering. No backend, no build step. */

const state = {
  account: null,
  role: 'coordinator',
  view: 'posts',
  post: 'P-1042',
  person: 'E1002',
  catalogTab: 'skills',
  recFilter: 'proposed',
  batch: '#127'
};

const LEAD_NAME = '陈立';
const MY_PROJECTS = ['Atlas'];

/* Sign-in accounts. One account = one person = exactly one role. */
const ACCOUNTS = [
  {
    id: 'liu', name: '刘敏', initial: '刘', role: 'coordinator', title: '资源协调员',
    scope: '全部项目 · 维护人员池与目录、导入、搜索与推荐'
  },
  {
    id: 'chen', name: '陈立', initial: '陈', role: 'lead', title: '项目经理',
    scope: '仅 Atlas 交易中台 · 建岗位、确认或驳回推荐、解除指派'
  }
];

const data = {
  projects: [
    { id: 'Atlas', name: 'Atlas 交易中台', lead: '陈立', start: '2027-01-01', end: '2027-12-31', status: 'running' },
    { id: 'Borealis', name: 'Borealis 报表平台', lead: '李娜', start: '2026-10-01', end: '2027-09-30', status: 'running' }
  ],
  posts: [
    {
      id: 'P-1042', project: 'Atlas', title: '后端开发（两名）', role: 'DEV', level: 'Senior',
      headcount: 2, start: '2027-02-01', end: '2027-06-30', travel: 'limited',
      skills: [
        { name: '后端开发', kind: 'required', band: 'Practitioner' },
        { name: '前端开发', kind: 'required', band: 'Advanced beginner' },
        { name: '测试', kind: 'bonus', band: 'Practitioner' }
      ],
      confirmed: [{ no: 'E1001', name: '王宇', level: 'Lead', at: '2026-11-06' }]
    },
    {
      id: 'P-1043', project: 'Atlas', title: '测试工程师', role: 'QA', level: 'Senior',
      headcount: 1, start: '2027-03-01', end: '2027-08-31', travel: 'full',
      skills: [{ name: '测试', kind: 'required', band: 'Practitioner' },
        { name: '后端开发', kind: 'bonus', band: 'Practitioner' }],
      confirmed: []
    },
    {
      id: 'P-1044', project: 'Atlas', title: '业务分析师', role: 'BA', level: 'Senior',
      headcount: 2, start: '2027-01-15', end: '2027-05-31', travel: 'none',
      skills: [{ name: '业务分析', kind: 'required', band: 'Practitioner' }],
      confirmed: [
        { no: 'E1033', name: '孙悦', level: 'Senior', at: '2026-10-28' },
        { no: 'E1036', name: '李默', level: 'Lead', at: '2026-10-30' }
      ]
    },
    {
      id: 'P-1051', project: 'Borealis', title: '后端开发', role: 'DEV', level: 'Senior',
      headcount: 1, start: '2027-03-15', end: '2027-09-30', travel: 'limited',
      skills: [{ name: '后端开发', kind: 'required', band: 'Proficient' },
        { name: '测试', kind: 'bonus', band: 'Practitioner' }],
      confirmed: []
    },
    {
      id: 'P-1039', project: 'Atlas', title: '测试工程师（已取消）', role: 'QA', level: 'Junior',
      headcount: 1, start: '2027-02-01', end: '2027-04-30', travel: 'none',
      cancelled: true,
      skills: [{ name: '测试', kind: 'required', band: 'Advanced beginner' }],
      confirmed: []
    }
  ],
  // contract: exactly ONE role per person, stored as a single value (never an array).
  // Skills are capability items and may look role-shaped (测试, 项目管理) — they are not roles.
  people: [
    { no: 'E1001', name: '王宇', role: 'DEV', level: 'Lead', travel: 'limited', active: true,
      skills: { '后端开发': 'Proficient', '前端开发': 'Proficient', '测试': 'Practitioner' },
      alloc: [{ project: 'Atlas', start: '2027-02-01', end: '2027-06-30' }] },
    { no: 'E1002', name: '李婷', role: 'DEV', level: 'Senior', travel: 'limited', active: true,
      skills: { '后端开发': 'Proficient', '前端开发': 'Practitioner', '测试': 'Practitioner' }, alloc: [] },
    { no: 'E1005', name: '张伟', role: 'DEV', level: 'Senior', travel: 'full', active: true,
      skills: { '后端开发': 'Practitioner', '前端开发': 'Advanced beginner', '测试': 'Novice' }, alloc: [] },
    { no: 'E1011', name: '赵敏', role: 'DEV', level: 'Lead', travel: 'limited', active: true,
      skills: { '后端开发': 'Proficient', '前端开发': 'Practitioner', '测试': 'Proficient' },
      alloc: [{ project: 'Atlas', start: '2026-12-01', end: '2027-01-22' }] },
    { no: 'E1015', name: '孙浩', role: 'PM', level: 'Senior', travel: 'full', active: true,
      skills: { '项目管理': 'Proficient' }, alloc: [] },
    { no: 'E1018', name: '周雪', role: 'DEV', level: 'Junior', travel: 'limited', active: true,
      skills: { '后端开发': 'Advanced beginner' }, alloc: [] },
    { no: 'E1021', name: '吴澄', role: 'DEV', level: 'Senior', travel: 'full', active: true,
      skills: { '后端开发': 'Novice', '前端开发': 'Practitioner' }, alloc: [] },
    { no: 'E1024', name: '郑楠', role: 'DEV', level: 'Senior', travel: 'none', active: true,
      skills: { '后端开发': 'Proficient', '前端开发': 'Practitioner' }, alloc: [] },
    { no: 'E1027', name: '何蕾', role: 'DEV', level: 'Senior', travel: 'limited', active: false,
      skills: { '后端开发': 'Proficient' }, alloc: [] },
    { no: 'E1030', name: '钱进', role: 'QA', level: 'Senior', travel: 'limited', active: true,
      skills: { '测试': 'Proficient' }, alloc: [] },
    { no: 'E1034', name: '冯磊', role: 'QA', level: 'Junior', travel: 'limited', active: true,
      skills: { '测试': 'Advanced beginner' }, alloc: [] },
    { no: 'E1033', name: '孙悦', role: 'BA', level: 'Senior', travel: 'limited', active: true,
      skills: { '业务分析': 'Proficient' },
      alloc: [{ project: 'Atlas', start: '2027-01-15', end: '2027-05-31' }] },
    { no: 'E1036', name: '李默', role: 'BA', level: 'Lead', travel: 'none', active: true,
      skills: { '业务分析': 'Proficient' },
      alloc: [{ project: 'Atlas', start: '2027-01-15', end: '2027-05-31' }] },
    { no: 'E1042', name: '顾然', role: 'DEV', level: 'Lead', travel: 'full', active: true,
      skills: { '后端开发': 'Practitioner', '测试': 'Proficient' }, alloc: [] }
  ],
  ranked: {
    'P-1042': [
      { no: 'E1002', name: '李婷', level: 'Senior', total: 94.8,
        c: { fit: 29.75, bonus: 15, level: 20, head: 20, travel: 10 },
        why: '必备技能各高出 1 档；加分技能达标；排期前后 30 天无占用' },
      { no: 'E1011', name: '赵敏', level: 'Lead', total: 79.8,
        c: { fit: 29.75, bonus: 15, level: 15, head: 10, travel: 10 },
        why: '职级高出 1 档；上一项目在开始前 10 天结束，排期余量减半' },
      { no: 'E1005', name: '张伟', level: 'Senior', total: 71.5,
        c: { fit: 24.5, bonus: 0, level: 20, head: 20, travel: 7 },
        why: '必备技能刚好达标；加分技能未达目标；差旅接受度高于要求' }
    ],
    'P-1043': [],
    'P-1044': [],
    'P-1051': [
      { no: 'E1002', name: '李婷', level: 'Senior', total: 89.5,
        c: { fit: 24.5, bonus: 15, level: 20, head: 20, travel: 10 },
        why: '必备技能刚好达标；加分技能达标；排期无占用' }
    ]
  },
  excluded: {
    'P-1042': [
      { no: 'E1027', name: '何蕾', gate: 'G1 停用', why: '人员已停用' },
      { no: 'E1015', name: '孙浩', gate: 'G2 角色', why: '岗位要求 DEV，本人为 PM' },
      { no: 'E1018', name: '周雪', gate: 'G3 职级', why: '岗位要求 Senior，本人为 Junior' },
      { no: 'E1021', name: '吴澄', gate: 'G4 必备技能', why: '后端开发为 Novice，低于 Practitioner' },
      { no: 'E1042', name: '顾然', gate: 'G4 必备技能', why: '未持有 前端开发（required ≥ Advanced beginner）' },
      { no: 'E1024', name: '郑楠', gate: 'G5 差旅', why: '岗位要求 limited，本人仅接受 none' },
      { no: 'E1001', name: '王宇', gate: 'G6 排期', why: '2027-02-01 ~ 2027-06-30 已被本项目占用' }
    ],
    /* ranked list is empty, so the same people are explained by the fallback gates */
    'P-1043': [
      { no: 'E1030', name: '钱进', gate: 'G5 差旅', why: '本角色 QA；岗位要求 full，本人仅接受 limited' },
      { no: 'E1034', name: '冯磊', gate: 'G5 差旅', why: '本角色 QA；岗位要求 full，本人仅接受 limited' },
      { no: 'E1002', name: '李婷', gate: 'X1 职级未高一级', why: 'DEV · Senior，与要求同级；测试已达 Practitioner 仍不算兜底' },
      { no: 'E1015', name: '孙浩', gate: 'X1 职级未高一级', why: 'PM · Senior，与要求同级，兜底要求至少 Lead' },
      { no: 'E1036', name: '李默', gate: 'X2 域内无 3 分技能', why: 'BA · Lead，但 QA 技能域（测试）内没有 Practitioner 及以上' },
      { no: 'E1011', name: '赵敏', gate: 'G5 差旅', why: 'DEV · Lead 且 测试 Proficient，但仅接受 limited；差旅门槛兜底同样生效' },
      { no: 'E1001', name: '王宇', gate: 'G6 排期', why: 'DEV · Lead 且 测试 Practitioner，但 2027-02-01 ~ 06-30 已占用' }
    ],
    'P-1044': [],
    'P-1051': []
  },
  /* cross-role fallback: filled only when the post role has no ranked candidate */
  fallback: {
    'P-1042': [],
    'P-1043': [
      { no: 'E1042', name: '顾然', role: 'DEV', level: 'Lead', gap: 1, skill: '测试', band: 'Proficient',
        total: 89.8, c: { fit: 29.75, bonus: 15, level: 15, head: 20, travel: 10 },
        why: 'QA 无人通过门槛；职级高 1 档，QA 技能域内 测试 = Proficient' }
    ],
    'P-1044': [],
    'P-1051': []
  },
  recs: [
    { id: 'R-301', post: 'P-1042', no: 'E1002', name: '李婷', state: 'proposed', score: 94.8,
      at: '2026-11-08 10:12', by: '刘敏（协调员）', why: '必备技能各高出 1 档；加分技能达标' },
    { id: 'R-302', post: 'P-1042', no: 'E1011', name: '赵敏', state: 'proposed', score: 79.8,
      at: '2026-11-08 10:14', by: '刘敏（协调员）', why: '职级高出 1 档；排期余量减半' },
    { id: 'R-303', post: 'P-1051', no: 'E1002', name: '李婷', state: 'proposed', score: 89.5,
      at: '2026-11-08 11:03', by: '刘敏（协调员）', why: '必备技能刚好达标；加分技能达标' },
    { id: 'R-304', post: 'P-1043', no: 'E1042', name: '顾然', state: 'proposed', score: 89.8,
      cross: { role: 'DEV', gap: 1, skill: '测试', band: 'Proficient' },
      at: '2026-11-08 11:20', by: '刘敏（协调员）', why: 'QA 无人通过门槛；跨角色兜底：职级高 1 档，QA 域内 测试 = Proficient' },
    { id: 'R-298', post: 'P-1042', no: 'E1005', name: '张伟', state: 'rejected', score: 71.5,
      at: '2026-11-04 15:40', by: '刘敏（协调员）', why: '职级与加分技能未达，项目经理决定先不面' },
    { id: 'R-295', post: 'P-1042', no: 'E1002', name: '李婷', state: 'voided', score: 94.8,
      at: '2026-11-06 09:05', by: '刘敏（协调员）', why: '岗位门槛字段被修改，原推荐作废；修改后已重新推荐' }
  ],
  notices: [
    { post: 'P-1042', name: '李婷', reason: 'post changed', at: '2026-11-06 09:05', detail: '岗位名称由“后端开发”改为“后端开发（两名）”，原推荐作废' },
    { post: 'P-1051', name: '王宇', reason: 'person taken', at: '2026-11-05 16:40', detail: '该员工已被 P-1042 确认' },
    { post: 'P-1039', name: '冯磊', reason: 'post cancelled', at: '2026-11-02 11:20', detail: '岗位已取消' }
  ],
  batches: [
    { id: '#128', at: '2026-11-08 09:31', by: '刘敏', state: 'applied', people: 42, skills: 156, errors: [] },
    { id: '#127', at: '2026-11-07 17:02', by: '刘敏', state: 'rejected', people: 41, skills: 155, errors: [
      { sheet: 'people', row: 17, key: 'E1032', why: 'job_level = Mid，不在四档职级内（Junior / Senior / Lead / Principal）' },
      { sheet: 'people', row: 23, key: 'E1041', why: 'role = Product，不在角色目录（PM / BA / DEV / QA / UX）' },
      { sheet: 'skills', row: 88, key: 'E9999', why: '人员不存在：工号既不在系统中，也不在本文件的 people 表' },
      { sheet: 'skills', row: 92, key: 'E1032 · 测试', why: '同一工号同一技能重复出现（第 88 行之后再次出现）' }
    ] },
    { id: '#126', at: '2026-11-01 10:15', by: '刘敏', state: 'applied', people: 40, skills: 150, errors: [] }
  ],
  /* refs are derived from people + posts, never stored twice */
  roles: [
    { name: 'PM', active: true }, { name: 'BA', active: true },
    { name: 'DEV', active: true }, { name: 'QA', active: true },
    { name: 'UX', active: true }, { name: 'SRE', active: true }
  ],
  skills: [
    { name: '后端开发', domain: 'DEV', active: true }, { name: '前端开发', domain: 'DEV', active: true },
    { name: '项目管理', domain: 'PM', active: false }, { name: '业务分析', domain: 'BA', active: true },
    { name: '测试', domain: 'QA', active: true }, { name: 'UI设计', domain: 'UX', active: true },
    { name: '数据分析', domain: 'BA', active: true }
  ]
};

/* ---------- helpers ---------- */
const esc = (s) => String(s).replace(/[&<>"]/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c]));
const post = (id) => data.posts.find((p) => p.id === id);
const person = (no) => data.people.find((p) => p.no === no);
const project = (id) => data.projects.find((p) => p.id === id);
/* a role's skill domain = every skill that declares that role as its domain */
const domainOf = (role) => data.skills.filter((s) => s.domain === role).map((s) => s.name);
const remaining = (p) => p.headcount - p.confirmed.length;
const statusOf = (p) => (p.cancelled ? 'cancelled' : p.confirmed.length === 0 ? 'open' : remaining(p) === 0 ? 'filled' : 'partially filled');
const statusBadge = (p) => {
  const s = statusOf(p);
  const map = {
    open: ['badge-muted', 'open 空缺'],
    'partially filled': ['badge-warning', 'partially filled 部分到位'],
    filled: ['badge-success', 'filled 已满'],
    cancelled: ['badge-muted', 'cancelled 已取消']
  };
  return `<span class="badge ${map[s][0]}">${map[s][1]}</span>`;
};
const visibleProjects = () => (state.role === 'lead' ? MY_PROJECTS : data.projects.map((p) => p.id));
const visiblePosts = () => data.posts.filter((p) => visibleProjects().includes(p.project));
const visibleRecs = () =>
  state.role === 'lead'
    ? data.recs.filter((r) => post(r.post) && post(r.post).project === 'Atlas')
    : data.recs;

const NAV = [
  { group: '项目交付', items: [
    { id: 'posts', label: '岗位与缺口', roles: ['coordinator', 'lead'] },
    { id: 'recs', label: '推荐与确认', roles: ['coordinator', 'lead'] }
  ] },
  { group: '人员', items: [
    { id: 'match', label: '候选人搜索', roles: ['coordinator'] },
    { id: 'people', label: '人员池', roles: ['coordinator'] }
  ] },
  { group: '数据', items: [
    { id: 'import', label: '报表导入', roles: ['coordinator'] },
    { id: 'catalog', label: '目录管理', roles: ['coordinator'] }
  ] }
];

function toast(msg) {
  const el = document.createElement('div');
  el.textContent = msg;
  el.style.cssText = 'position:fixed;bottom:22px;left:50%;transform:translateX(-50%);background:hsl(240 5.9% 10%);color:#fff;padding:9px 16px;border-radius:9px;font-size:12.5px;z-index:50;box-shadow:0 8px 24px rgba(0,0,0,.18)';
  document.body.appendChild(el);
  setTimeout(() => el.remove(), 2200);
}

/* ---------- nav / chrome ---------- */
function signIn(id) {
  const acc = ACCOUNTS.find((a) => a.id === id) || null;
  state.account = acc ? acc.id : null;
  state.role = acc ? acc.role : 'coordinator';
  state.view = 'posts';
  render();
}

function renderLogin() {
  document.getElementById('login').innerHTML = `
    <div class="login-card">
      <div class="login-head">
        <span class="brand-mark">S</span>
        <div>
          <div class="login-title">登录 Staffing</div>
          <div class="note">MVP 原型 · 选择账号进入</div>
        </div>
      </div>
      <p class="login-sub">原型不校验密码，点选账号即进入。一个账号对应一个人、一个角色；进入后可从顶栏右上方「切换账号」回到这里。</p>
      <div class="account-list">
        ${ACCOUNTS.map((a) => `
          <button class="account-opt" data-account="${a.id}">
            <span class="avatar">${esc(a.initial)}</span>
            <span class="who">
              <strong>${esc(a.name)} · ${esc(a.title)}</strong>
              <span>${esc(a.scope)}</span>
            </span>
          </button>`).join('')}
      </div>
      <div class="login-foot">角色与账号一一对应：刘敏只有协调员这一个角色，陈立只有项目经理这一个角色；同一个人不会同时持有两个角色。</div>
    </div>`;
}

function renderChrome() {
  const pending = visibleRecs().filter((r) => r.state === 'proposed').length;
  document.getElementById('nav').innerHTML = NAV.map((g) => {
    const items = g.items
      .filter((i) => i.roles.includes(state.role))
      .map((i) => {
        const active = state.view === i.id ? ' active' : '';
        const count = i.id === 'recs' && pending ? `<span class="count">${pending}</span>` : '';
        return `<button class="nav-item${active}" data-act="go" data-view="${i.id}">
          <span class="dot"></span>${i.label}${count}</button>`;
      })
      .join('');
    return items ? `<div class="nav-group">${g.group}</div>${items}` : '';
  }).join('');

  const labels = { posts: '岗位与缺口', recs: '推荐与确认', match: '候选人搜索', people: '人员池', import: '报表导入', catalog: '目录管理' };
  const proj = state.view === 'match' ? post(state.post).project : 'Atlas';
  document.getElementById('crumbs').innerHTML =
    `<span>Staffing</span><span>/</span><span>${esc(project(proj) ? project(proj).name : '全部项目')}</span>` +
    `<span>/</span><strong>${labels[state.view]}</strong>`;

  const me = ACCOUNTS.find((a) => a.id === state.account);
  document.getElementById('account').innerHTML = `
    <span class="avatar">${esc(me.initial)}</span>
    <span class="who"><strong>${esc(me.name)}</strong><span>${esc(me.title)}</span></span>
    <button class="switch" data-act="signout">切换账号</button>`;
}

/* ---------- shared bits ---------- */
function specTag(text) { return `<span class="spec-tag">${esc(text)}</span>`; }

function pageHead(title, lede, tag, actions = '') {
  return `<div class="page-head">
    <div><h1>${title}</h1><div class="lede">${lede} ${tag ? specTag(tag) : ''}</div></div>
    <div class="btn-row">${actions}</div>
  </div>`;
}

function scoreBars(c) {
  const rows = [
    ['必备技能', c.fit, 35], ['加分技能', c.bonus, 15], ['职级', c.level, 20],
    ['排期余量', c.head, 20], ['差旅', c.travel, 10]
  ];
  return `<div class="bars">${rows.map(([label, v, max]) => {
    const pct = Math.round((v / max) * 100);
    return `<div class="bar-line"><span class="muted">${label}</span>
      <span class="bar-track"><span class="bar-fill${pct < 50 ? ' low' : ''}" style="width:${pct}%"></span></span>
      <span class="val">${v}/${max}</span></div>`;
  }).join('')}</div>`;
}

/* ---------- view: posts + gap ---------- */
function viewPosts() {
  const posts = visiblePosts();
  const short = posts.filter((p) => remaining(p) > 0 && statusOf(p) !== 'cancelled');
  const confirmed = posts.reduce((n, p) => n + p.confirmed.length, 0);

  const table = `<table>
    <thead><tr>
      <th>项目</th><th>岗位</th><th>角色 / 最低职级</th><th class="num">名额</th>
      <th class="num">已到位</th><th class="num">剩余</th><th>状态</th>
    </tr></thead>
    <tbody>${posts.map((p) => `
      <tr class="clickable${state.post === p.id ? ' selected' : ''}" data-act="post" data-id="${p.id}">
        <td>${esc(project(p.project).name)}</td>
        <td><strong>${esc(p.title)}</strong><div class="note mono">${p.id} · ${p.start} ~ ${p.end}</div></td>
        <td><span class="badge badge-outline-primary">${p.role}</span> ${p.level}</td>
        <td class="num">${p.headcount}</td>
        <td class="num">${p.confirmed.length}</td>
        <td class="num">${p.cancelled ? '—' : remaining(p)}</td>
        <td>${statusBadge(p)}</td>
      </tr>`).join('')}
    </tbody></table>`;

  const p = post(state.post);
  const locked = p.confirmed.length > 0;
  /* one compact banner instead of two stacked ones: keeps the detail card short enough to fit on screen */
  const notice = locked
    ? `<div class="banner warn"><span class="ico">!</span><div><strong>门槛字段已锁定。</strong>
        已有 ${p.confirmed.length} 名确认人员（${p.confirmed.map((c) => esc(c.name)).join('、')}），角色、职级、<code>required</code> 技能、日期与差旅不可改；先<strong>解除</strong>才能改期改要求，名额只能上调。
        ${state.role === 'coordinator' ? '岗位由项目经理维护，协调员只读。' : ''}</div></div>`
    : `<div class="banner info"><span class="ico">i</span><div>岗位由项目经理维护，协调员只读。</div></div>`;
  const detail = `<div class="card">
    <div class="card-head">
      <div><h2>${esc(p.title)}</h2><div class="note mono">${p.id} · ${esc(project(p.project).name)}</div></div>
      ${statusBadge(p)}
    </div>
    <div class="card-body stack">
      ${notice}

      <dl class="kv">
        <dt>角色</dt><dd><span class="badge badge-outline-primary">${p.role}</span>
          <span class="note">技能域 ${esc(domainOf(p.role).join('、') || '空域')}</span></dd>
        <dt>最低职级</dt><dd>${p.level}</dd>
        <dt>起止日期</dt><dd class="mono">${p.start} ~ ${p.end}</dd>
        <dt>招聘名额</dt><dd>${p.headcount} · 已到位 ${p.confirmed.length} · 剩余 ${remaining(p)}</dd>
        <dt>差旅要求</dt><dd>${p.travel}</dd>
      </dl>

      <div>
        <h3 style="margin-bottom:6px">技能要求</h3>
        <table>
          <thead><tr><th>技能</th><th>类型</th><th>最低 / 目标档位</th></tr></thead>
          <tbody>${p.skills.map((s) => `<tr>
            <td>${esc(s.name)}</td>
            <td>${s.kind === 'required' ? '<span class="badge badge-solid">required 必备</span>' : '<span class="badge">bonus 加分 · 只影响排序</span>'}</td>
            <td>${s.band}</td></tr>`).join('')}
          </tbody></table>
      </div>

      <div>
        <h3 style="margin-bottom:6px">已确认人员</h3>
        ${p.confirmed.length ? `<table><tbody>${p.confirmed.map((c) => `<tr>
          <td>${esc(c.name)} <span class="note mono">${c.no}</span></td><td>${c.level}</td>
          <td class="note">确认于 ${c.at}</td>
          <td><button class="btn btn-sm btn-danger" data-act="release" data-post="${p.id}" data-no="${c.no}">解除</button></td>
        </tr>`).join('')}</tbody></table>`
        : `<div class="empty">尚无确认人员</div>`}
      </div>

      <div class="btn-row">
        <button class="btn btn-primary" data-act="goto-match" ${state.role === 'lead' ? 'disabled title="排名只对协调员开放"' : ''}>搜索候选人</button>
        <button class="btn" data-act="${state.role === 'lead' ? 'edit-post' : 'noop'}" ${state.role === 'coordinator' ? 'disabled' : ''}>编辑岗位</button>
        <button class="btn btn-danger" data-act="cancel-post" ${state.role === 'coordinator' ? 'disabled' : ''}>取消岗位</button>
      </div>
    </div>
  </div>`;

  return pageHead(
    state.role === 'lead' ? '我的岗位' : '岗位与缺口',
    '按岗位显示已选定人员、已到位数量与剩余名额；剩余为 0 的岗位离开缺口列表。',
    'post-requirement',
    `<button class="btn btn-primary" data-act="new-post" ${state.role === 'coordinator' ? 'disabled title="岗位由项目经理创建"' : ''}>新建岗位</button>`
  ) + `<div class="stack">
    <div class="grid-3">
      <div class="card kpi"><div class="card-body"><div class="muted small">仍缺人岗位</div><div class="score mono">${short.length}</div></div></div>
      <div class="card kpi"><div class="card-body"><div class="muted small">已确认人员</div><div class="score mono">${confirmed}</div></div></div>
      <div class="card kpi"><div class="card-body"><div class="muted small">待决策推荐</div><div class="score mono">${visibleRecs().filter((r) => r.state === 'proposed').length}</div></div></div>
    </div>
    <div class="grid-kpi">
      <div class="card"><div class="card-body tight">${table}</div></div>
      ${detail}
    </div>
  </div>`;
}

/* ---------- view: candidate matching ---------- */
function viewMatch() {
  if (state.role === 'lead') return `<div class="banner danger"><span class="ico">×</span><div>项目经理看不到完整候选池与排名，只能看到推荐给你的人。（S23）</div></div>`;

  const p = post(state.post);
  const ranked = data.ranked[p.id] || [];
  const excluded = data.excluded[p.id] || [];
  const counts = excluded.reduce((m, e) => { m[e.gate] = (m[e.gate] || 0) + 1; return m; }, {});
  const fallback = data.fallback[p.id] || [];
  /* the fallback opens only when the post role has nobody: empty ranked list */
  const fallbackOpen = ranked.length === 0;
  const domain = domainOf(p.role);
  const domainText = domain.length ? domain.join('、') : '空域';

  const chips = data.posts.filter((x) => remaining(x) > 0 && !x.cancelled).map((x) =>
    `<button class="chip${state.post === x.id ? ' active' : ''}" data-act="chip-post" data-id="${x.id}">${x.id} · ${esc(x.title)}</button>`
  ).join('');

  const rankedTable = ranked.length ? `<table>
    <thead><tr><th>#</th><th>候选人</th><th>职级</th><th class="num">契合度</th><th>分项得分</th><th>理由</th><th></th></tr></thead>
    <tbody>${ranked.map((r, i) => `<tr>
      <td class="mono">${i + 1}</td>
      <td><strong>${esc(r.name)}</strong><div class="note mono">${r.no}</div></td>
      <td>${r.level}</td>
      <td class="num"><span class="score mono">${r.total}</span></td>
      <td>${scoreBars(r.c)}</td>
      <td class="note" style="max-width:220px">${esc(r.why)}</td>
      <td><button class="btn btn-sm btn-primary" data-act="recommend" data-no="${r.no}" data-post="${p.id}">推荐</button></td>
    </tr>`).join('')}</tbody></table>`
    : `<div class="empty">没有同角色候选人通过全部门槛。<br>排除原因见下方统计；兜底名单见下方的“跨角色兜底”。</div>`;

  const crossRole = fallbackOpen ? `<div class="card">
    <div class="card-head">
      <div><h2>跨角色兜底</h2>
        <div class="note">本角色（${p.role}）无人可用时的第二层级：单独一栏，永不排在同角色之前</div></div>
      <span class="badge badge-warning">X1 + X2</span>
    </div>
    <div class="card-body stack">
      <div class="banner info"><span class="ico">i</span><div>
        <strong>本角色没有任何人通过门槛。</strong>下面的候选人来自其他角色：职级至少高 1 档（X1），
        且在 ${p.role} 的技能域 <code>${esc(domainText)}</code> 内至少有一项达到 <code>Practitioner</code>
        及以上（X2）。G1 在职、G5 差旅、G6 排期仍然生效；岗位的 required 技能不是他们的门槛，缺少的那项只计 0 分。
      </div></div>
      ${fallback.length ? `<table>
        <thead><tr><th>#</th><th>候选人</th><th>职级差</th><th class="num">契合度</th><th>分项得分</th><th>达标技能与理由</th><th></th></tr></thead>
        <tbody>${fallback.map((r, i) => `<tr>
          <td class="mono">${i + 1}</td>
          <td><strong>${esc(r.name)}</strong> <span class="badge badge-outline-primary">${esc(r.role)}</span>
            <div class="note mono">${r.no}</div></td>
          <td class="mono">+${r.gap} 档<div class="note">${esc(r.level)}</div></td>
          <td class="num"><span class="score mono">${r.total}</span></td>
          <td>${scoreBars(r.c)}</td>
          <td class="note" style="max-width:230px"><strong>${esc(r.skill)} · ${esc(r.band)}</strong><br>${esc(r.why)}</td>
          <td><button class="btn btn-sm btn-primary" data-act="recommend-cross" data-no="${r.no}" data-post="${p.id}">推荐</button></td>
        </tr>`).join('')}</tbody></table>`
        : `<div class="empty">没有跨角色候选人：没有人职级高于要求，或技能域内没有 Practitioner 及以上的人。<br>驳回数量见下方 X1 / X2 统计。</div>`}
      <div class="note">两栏分数同为 0–100，但永不跨栏比较；确认、占用与作废规则与同角色推荐完全一致，推荐快照会带上职级差与达标技能。</div>
    </div></div>` : '';

  return pageHead('候选人搜索',
    '六道硬门槛按顺序判定，第一个不满足的门槛就是排除理由；通过者按五维加权 0–100 排序。本角色无人时，另给一份跨角色兜底名单。',
    'candidate-matching') +
  `<div class="stack">
    <div class="chips">${chips}</div>
    <div class="card">
      <div class="card-head"><div><h2>${esc(p.title)}</h2>
        <div class="note mono">${p.id} · ${p.role} ≥ ${p.level} · 技能域 ${esc(domainText)} · 差旅 ${p.travel} · 剩余 ${remaining(p)}</div></div>
        <div class="row">
          ${p.skills.map((s) => `<span class="badge ${s.kind === 'required' ? '' : 'badge-muted'}">${esc(s.name)} ≥ ${s.band}${s.kind === 'bonus' ? ' · bonus' : ''}</span>`).join('')}
        </div></div>
      <div class="card-body tight">${rankedTable}</div>
    </div>

    ${crossRole}

    <div class="card">
      <div class="card-head"><h2>被排除的人</h2><span class="note">不计入排名，只解释原因</span></div>
      <div class="card-body stack">
        <div class="gate-grid">${Object.entries(counts).map(([g, n]) =>
          `<div class="gate-cell"><div class="n mono">${n}</div><div class="l">${esc(g)}</div></div>`).join('')}</div>
        <table><tbody>${excluded.map((e) => `<tr>
          <td style="width:150px">${esc(e.name)} <span class="note mono">${e.no}</span></td>
          <td style="width:120px"><span class="badge badge-danger">${esc(e.gate)}</span></td>
          <td class="note">${esc(e.why)}</td></tr>`).join('')}</tbody></table>
      </div>
    </div>
    <div class="note">排序并列时先比必备技能得分，再按姓名；同分同富余只是展示顺序不同。</div>
  </div>`;
}

/* ---------- view: recommendations ---------- */
function viewRecs() {
  const recs = visibleRecs();
  const states = [['proposed', '待决策'], ['rejected', '已驳回'], ['voided', '已作废']];
  const filtered = recs.filter((r) => r.state === state.recFilter);
  const badge = (s) => {
    const m = { proposed: ['badge-warning', 'proposed 待决策'], rejected: ['badge-muted', 'rejected 已驳回'], voided: ['badge-danger', 'voided 已作废'] };
    return `<span class="badge ${m[s][0]}">${m[s][1]}</span>`;
  };

  const cards = filtered.length ? filtered.map((r) => {
    const p = post(r.post) || {};
    return `<div class="card"><div class="card-body stack">
      <div class="row" style="justify-content:space-between">
        <div><strong>${esc(r.name)}</strong> <span class="note mono">${r.no}</span>
          ${r.cross ? `<span class="badge badge-warning">跨角色 ${esc(r.cross.role)} · +${r.cross.gap} 档</span>` : ''}
          <div class="note">${esc(p.title || '')} · ${r.post} · 推荐人 ${esc(r.by)} · ${r.at}</div></div>
        ${badge(r.state)}
      </div>
      <div class="row" style="gap:16px">
        <div><div class="note">推荐时快照</div><div class="score mono">${r.score}</div></div>
        <div class="note" style="max-width:520px">${esc(r.why)}${r.cross ? `<br>兜底证据：${esc(r.cross.role)} · 职级 +${r.cross.gap} 档 · ${esc(r.cross.skill)} = ${esc(r.cross.band)}（快照随行，确认规则不变）` : ''}<br>快照是推荐那一刻的分数与理由，之后数据变化不会改写它。</div>
      </div>
      <div class="btn-row">
        ${state.role === 'lead' && r.state === 'proposed' ? `
          <button class="btn btn-primary btn-sm" data-act="confirm" data-id="${r.id}">确认</button>
          <button class="btn btn-sm" data-act="reject" data-id="${r.id}">驳回</button>` : ''}
        ${state.role === 'coordinator' && r.state === 'proposed'
          ? `<button class="btn btn-sm" data-act="withdraw" data-id="${r.id}">撤回推荐</button>` : ''}
        ${r.state === 'voided' ? `<span class="note">作废原因：${esc(r.why)}</span>` : ''}
      </div>
    </div></div>`;
  }).join('') : `<div class="card"><div class="empty">没有该状态的推荐</div></div>`;

  const notices = state.role === 'coordinator' ? `<div class="card">
    <div class="card-head"><h2>作废通知</h2><span class="note">保留到手动清除，不自动过期</span></div>
    <div class="list">${data.notices.map((n) => `<div class="list-item">
      <span class="badge badge-danger">${esc(n.reason)}</span>
      <div><strong>${esc(n.name)}</strong> · ${n.post}
        <div class="note">${esc(n.detail)} · ${n.at}</div></div></div>`).join('')}</div>
  </div>` : '';

  return pageHead(state.role === 'lead' ? '推荐给我的人' : '推荐与确认',
    state.role === 'lead'
      ? '只有推荐到本项目的候选人才可见，带分数与理由；确认前系统会重跑「在职」与「排期」两条门槛。'
      : '推荐由协调员发起，确认或驳回由项目经理执行；确认后写入指派与占用。',
    'staffing-confirmation') +
  `<div class="stack">
    ${state.role === 'coordinator' ? `<div class="banner info"><span class="ico">i</span><div>协调员不能确认推荐，只能撤回；确认动作只属于岗位所属项目的项目经理。</div></div>` : ''}
    <div class="chips">${states.map(([s, l]) =>
      `<button class="chip${state.recFilter === s ? ' active' : ''}" data-act="rec-filter" data-state="${s}">${l} (${recs.filter((r) => r.state === s).length})</button>`).join('')}</div>
    <div class="rec-grid">${cards}</div>
    ${notices}
  </div>`;
}

/* ---------- view: people ---------- */
function viewPeople() {
  const p = person(state.person) || data.people[0];
  const table = `<table>
    <thead><tr><th>工号</th><th>姓名</th><th>角色（唯一）</th><th>职级</th><th>差旅</th><th class="num">技能项</th><th>状态</th></tr></thead>
    <tbody>${data.people.map((x) => `<tr class="clickable${p.no === x.no ? ' selected' : ''}" data-act="person" data-no="${x.no}">
      <td class="mono">${x.no}</td><td>${esc(x.name)}</td>
      <td><span class="badge badge-outline-primary">${x.role}</span></td>
      <td>${x.level}</td><td>${x.travel}</td>
      <td class="num">${Object.keys(x.skills).length}</td>
      <td>${x.active ? '<span class="badge badge-success">active</span>' : '<span class="badge badge-muted">inactive</span>'}</td>
    </tr>`).join('')}</tbody></table>`;

  return pageHead('人员池', '每个人员只有一个角色；角色由报表写入，界面不能新增或修改。技能是能力项，不是角色。', 'people-pool') +
  `<div class="grid-2">
    <div class="card"><div class="card-body tight">${table}</div></div>
    <div class="card">
      <div class="card-head"><div><h2>${esc(p.name)}</h2><div class="note mono">${p.no}</div></div>
        ${p.active ? '<span class="badge badge-success">active</span>' : '<span class="badge badge-muted">inactive</span>'}</div>
      <div class="card-body stack">
        <div class="banner info"><span class="ico">i</span><div>主数据只读。姓名、角色、职级、差旅、在职状态只能通过报表导入修改；<strong>一个人只能有一个角色</strong>，不能兼任。</div></div>
        <dl class="kv">
          <dt>角色</dt><dd><span class="badge badge-outline-primary">${p.role}</span> <span class="note">唯一角色</span></dd>
          <dt>职级</dt><dd>${p.level}</dd>
          <dt>差旅接受度</dt><dd>${p.travel}</dd>
        </dl>
        <div><h3 style="margin-bottom:4px">技能项与档位</h3>
          <div class="note" style="margin-bottom:6px">技能是能力项，可以跨角色（例如 DEV 持有“测试”技能），但它不会让人拥有第二个角色。</div>
          <table><tbody>${Object.entries(p.skills).map(([s, b]) => `<tr>
            <td>${esc(s)}</td><td class="note">${b}</td></tr>`).join('')}</tbody></table></div>
        <div><h3 style="margin-bottom:6px">占用（含未来）</h3>
          ${p.alloc.length ? `<table><tbody>${p.alloc.map((a) => `<tr>
            <td>${esc(a.project)}</td><td class="mono note">${a.start} ~ ${a.end}</td></tr>`).join('')}</tbody></table>`
            : '<div class="empty">当前无占用</div>'}
        </div>
        <div class="btn-row"><button class="btn" disabled title="没有手工编辑入口">编辑主数据</button></div>
      </div>
    </div>
  </div>`;
}

/* ---------- view: import ---------- */
function viewImport() {
  const b = data.batches.find((x) => x.id === state.batch) || data.batches[0];
  const history = `<table>
    <thead><tr><th>批次</th><th>上传时间</th><th>上传人</th><th class="num">people</th><th class="num">skills</th><th>状态</th></tr></thead>
    <tbody>${data.batches.map((x) => `<tr class="clickable${b.id === x.id ? ' selected' : ''}" data-act="batch" data-id="${x.id}">
      <td class="mono">${x.id}</td><td class="mono">${x.at}</td><td>${esc(x.by)}</td>
      <td class="num">${x.people}</td><td class="num">${x.skills}</td>
      <td>${x.state === 'applied' ? '<span class="badge badge-success">applied</span>' : '<span class="badge badge-danger">rejected</span>'}</td>
    </tr>`).join('')}</tbody></table>`;

  const detail = b.state === 'rejected' ? `
    <div class="banner danger"><span class="ico">×</span><div><strong>整份文件未通过校验，本批次没有写入任何数据。</strong>
      人员池保持 ${b.id} 之前的状态：没有新建人员、没有改动熟练度、没有切换在职标记。</div></div>
    <table>
      <thead><tr><th>工作表</th><th class="num">行号</th><th>键值</th><th>原因</th></tr></thead>
      <tbody>${b.errors.map((e) => `<tr>
        <td class="mono">${e.sheet}</td><td class="num mono">${e.row}</td>
        <td class="mono">${esc(e.key)}</td><td class="note">${esc(e.why)}</td></tr>`).join('')}</tbody></table>
    <div class="note">一次列全，修好后整份重传；旧批次与它的错误清单保持可读。</div>`
    : `<div class="banner info"><span class="ico">✓</span><div>全部行通过校验，已写入：people ${b.people} 行、skills ${b.skills} 行。</div></div>`;

  return pageHead('报表导入', '一个工作簿 = 一个工作单元。任何一行不合法，整批 rejected，一个字都不写。', 'people-import') +
  `<div class="stack">
    <div class="grid-2">
      <div class="card"><div class="card-body stack">
        <div class="drop"><div class="big">把 .xlsx 拖到这里，或点击选择文件</div>
          <div class="note">必须同时包含 <code>people</code> 与 <code>skills</code> 两张表</div></div>
        <div class="note">
          <strong>people</strong>：employee_no, name, email, role, job_level, travel_willingness, active<br>
          <strong>skills</strong>：employee_no, skill, proficiency<br>
          单元格 trim；ASCII 值大小写不敏感（<code>senior</code> 视为 <code>Senior</code>）；<code>people</code> 可为空（只刷技能），两张表都无数据则拒绝。
        </div>
        <div class="btn-row"><button class="btn btn-primary" data-act="upload">上传并校验</button>
          <button class="btn" data-act="noop">下载模板</button></div>
      </div></div>
      <div class="card">
        <div class="card-head"><h2>批次历史</h2><span class="note">不可修改，可回看错误</span></div>
        <div class="card-body tight">${history}</div>
      </div>
    </div>
    <div class="card">
      <div class="card-head"><div><h2>批次 ${b.id}</h2>
        <div class="note mono">${b.at} · ${esc(b.by)}</div></div>
        ${b.state === 'rejected' ? '<span class="badge badge-danger">rejected</span>' : '<span class="badge badge-success">applied</span>'}</div>
      <div class="card-body stack">${detail}</div>
    </div>
  </div>`;
}

/* ---------- view: catalog ---------- */
function viewCatalog() {
  const rows = state.catalogTab === 'roles' ? data.roles : data.skills;
  const refsOf = (r) => state.catalogTab === 'roles'
    ? data.people.filter((p) => p.role === r.name).length + data.posts.filter((p) => p.role === r.name).length
    : data.people.filter((p) => p.skills[r.name]).length
      + data.posts.filter((p) => (p.skills || []).some((s) => s.name === r.name)).length;
  const head = state.catalogTab === 'roles'
    ? '<tr><th>角色</th><th>技能域</th><th class="num">引用数</th><th>状态</th><th>操作</th></tr>'
    : '<tr><th>技能</th><th>技能域</th><th class="num">引用数</th><th>状态</th><th>操作</th></tr>';
  const domainCell = (r) => state.catalogTab === 'roles'
    ? `<span class="note">${domainOf(r.name).length ? esc(domainOf(r.name).join('、')) : '空域'}</span>`
    : `<span class="badge badge-outline-primary">${esc(r.domain)}</span>`;

  const table = `<table><thead>${head}</thead><tbody>${rows.map((r) => `<tr>
    <td><strong>${esc(r.name)}</strong></td>
    <td>${domainCell(r)}</td>
    <td class="num">${refsOf(r)}</td>
    <td>${r.active ? '<span class="badge badge-success">active</span>' : '<span class="badge badge-muted">inactive</span>'}</td>
    <td class="ops"><div class="btn-row">
      <button class="btn btn-sm" data-act="toggle-catalog" data-name="${esc(r.name)}">${r.active ? '停用' : '启用'}</button>
      ${state.catalogTab === 'skills' ? `<button class="btn btn-sm" data-act="change-domain" data-name="${esc(r.name)}">改域</button>` : ''}
      <button class="btn btn-sm" ${refsOf(r) ? 'disabled title="已被引用：名称冻结"' : ''}>改名</button>
      <button class="btn btn-sm btn-danger" disabled title="目录项不允许删除">删除</button>
    </div></td></tr>`).join('')}</tbody></table>`;

  const roleSnapshot = `<div class="card">
    <div class="card-head"><h2>角色目录快照</h2><span class="note">一人一角色</span></div>
    <div class="card-body stack">
      ${data.roles.map((r) => {
        const holders = data.people.filter((p) => p.role === r.name);
        const names = holders.slice(0, 3).map((h) => esc(h.name)).join('、');
        return `<div class="role-row">
          <span class="badge badge-outline-primary">${esc(r.name)}</span>
          <span class="note">${holders.length ? `${holders.length} 人 · ${names}${holders.length > 3 ? ' 等' : ''}` : '暂无人员'}
            <div>技能域：${domainOf(r.name).length ? esc(domainOf(r.name).join('、')) : '空域 → 该角色岗位无兜底候选人'}</div></span>
        </div>`;
      }).join('')}
      <div class="note">每个人员在上面只出现一次；技能目录单独维护，持有某个技能不会让人员多出一个角色。技能域只服务于兜底，不放宽岗位的 required 门槛。</div>
    </div></div>`;

  return pageHead('目录管理', '被引用的目录项只可停用，不可改名或删除；停用不影响任何已存数据。', 'people-pool') +
  `<div class="stack">
    <div class="banner info"><span class="ico">i</span><div>角色目录预置 <code>PM</code> <code>BA</code> <code>DEV</code> <code>QA</code> <code>UX</code>；技能目录预置六项。两者都可新增，新增后同样可用于岗位、人员与导入。<br>角色是人员的唯一属性，一个人只能有一个角色；岗位只接受与人员角色相同的候选人。<strong>每项技能声明一个角色技能域</strong>（DEV→后端开发/前端开发、QA→测试、BA→业务分析、PM→项目管理、UX→UI设计）；改域只影响之后的搜索，不改写已存人员、岗位或推荐快照。</div></div>
    <div class="grid-2">
      <div class="card">
        <div class="tabs">
          <button class="tab${state.catalogTab === 'roles' ? ' active' : ''}" data-act="catalog-tab" data-tab="roles">角色</button>
          <button class="tab${state.catalogTab === 'skills' ? ' active' : ''}" data-act="catalog-tab" data-tab="skills">技能</button>
        </div>
        <div class="card-body tight">${table}</div>
      </div>
      ${roleSnapshot}
    </div>
    <div class="note">停用只会让新建岗位与新导入不再提供该项；已有人员、岗位与历史推荐理由继续显示原名。</div>
  </div>`;
}

/* ---------- render / events ---------- */
function render() {
  if (!state.account) {
    document.body.classList.add('signed-out');
    renderLogin();
    return;
  }
  document.body.classList.remove('signed-out');
  if (state.view === 'match' && state.role === 'lead') state.view = 'posts';
  if (!visiblePosts().some((p) => p.id === state.post)) state.post = visiblePosts()[0].id;
  renderChrome();
  const views = { posts: viewPosts, match: viewMatch, recs: viewRecs, people: viewPeople, import: viewImport, catalog: viewCatalog };
  document.getElementById('view').innerHTML = (views[state.view] || viewPosts)();
}

document.addEventListener('click', (e) => {
  const accBtn = e.target.closest('[data-account]');
  if (accBtn) { signIn(accBtn.dataset.account); return; }

  const el = e.target.closest('[data-act]');
  if (!el || el.disabled) return;
  const a = el.dataset.act;

  switch (a) {
    case 'signout': signIn(null); return;
    case 'go': state.view = el.dataset.view; break;
    case 'post': state.post = el.dataset.id; break;
    case 'chip-post': state.post = el.dataset.id; state.view = 'match'; break;
    case 'goto-match': state.view = 'match'; break;
    case 'rec-filter': state.recFilter = el.dataset.state; break;
    case 'person': state.person = el.dataset.no; break;
    case 'catalog-tab': state.catalogTab = el.dataset.tab; break;
    case 'batch': state.batch = el.dataset.id; break;
    case 'recommend': toast('已生成推荐：分数快照与理由快照已记录（原型不落库）'); break;
    case 'recommend-cross': toast('已生成跨角色推荐：快照带上角色、职级差与达标技能（原型不落库）'); break;
    case 'change-domain': toast('已改技能域：只影响之后的搜索，不改写已存人员、岗位与推荐快照（原型不落库）'); break;
    case 'confirm': toast('已确认：写入指派与占用，剩余名额 −1（原型不落库）'); break;
    case 'reject': toast('已驳回：人员恢复可用（原型不落库）'); break;
    case 'withdraw': toast('已撤回推荐（原型不落库）'); break;
    case 'release': toast('已解除：指派与占用删除，名额重新开放（原型不落库）'); break;
    case 'toggle-catalog': toast('已切换启用状态：不影响已存数据（原型不落库）'); break;
    case 'cancel-post': toast('无法取消：请先解除已确认人员（原型）'); break;
    case 'new-post': state.view = 'posts'; toast('新建岗位（原型仅示意）'); break;
    case 'edit-post': toast('改期被拒：请先解除已确认人员（原型）'); break;
    case 'upload': state.batch = '#127'; toast('校验完成：4 行错误，整批 rejected，未写入任何数据'); break;
    default: toast('原型：该操作仅作示意');
  }
  render();
});

render();
