/**
 * Headcount 图表原型（add-headcount-charts rev 2）。
 *
 * 纯静态：没有后端，数据是评审用样例，但每个数字都按 spec 的规则算出来 ——
 * 占比一位小数四舍五入、轴上的空分类保留 0 值柱、两张图的总数由同一个池子得出。
 */

const ROLE_AXIS = [
  { name: 'PM', count: 40 },
  { name: 'BA', count: 25 },
  { name: 'DEV', count: 14 },
  { name: 'QA', count: 34 },
  { name: 'UX', count: 0 }, // 目录仍提供、但没有在职人员 → 保留 0 值柱
];

const GRADE_AXIS = [
  { name: 'Junior', count: 38 },
  { name: 'Senior', count: 42 },
  { name: 'Lead', count: 28 },
  { name: 'Principal', count: 5 },
];

const PEOPLE = [
  { no: 'E100', name: '刘敏', role: 'PM', level: 'Senior', travel: 'limited', active: true },
  { no: 'E102', name: '王晨', role: 'BA', level: 'Junior', travel: 'none', active: true },
  { no: 'E104', name: '陈立', role: 'DEV', level: 'Lead', travel: 'full', active: true },
  { no: 'E107', name: '赵倩', role: 'QA', level: 'Senior', travel: 'limited', active: true },
  { no: 'E111', name: '孙浩', role: 'DEV', level: 'Principal', travel: 'full', active: true },
  { no: 'E190', name: '周雨', role: 'QA', level: 'Junior', travel: 'none', active: false },
];

const ACCOUNT = {
  coordinator: { name: '刘敏', role: '资源协调员' },
  empty: { name: '刘敏', role: '资源协调员' },
  lead: { name: '陈立', role: '项目经理' },
};

const BAR_MAX_PX = 170;

/** 占比：一位小数、四舍五入；总数为 0 时不做除法。 */
function share(count, total) {
  if (total === 0) return 0;
  return Math.round((count / total) * 1000) / 10;
}

function sum(rows) {
  return rows.reduce((acc, row) => acc + row.count, 0);
}

/** 纵轴刻度：把最高柱抬到一个整十的刻度上。 */
function ticks(maxCount) {
  const step = maxCount <= 10 ? 2 : Math.ceil(maxCount / 5 / 10) * 10;
  const top = Math.ceil(maxCount / step) * step;
  const out = [];
  for (let value = 0; value <= top; value += step) out.push(value);
  return out.reverse();
}

function chart(labelTitle, rows, total) {
  const maxCount = Math.max(...rows.map((row) => row.count));
  const scale = ticks(maxCount);
  const top = scale[0] || 1;

  const axis = scale.map((value) => `<span>${value}</span>`).join('');
  const bars = rows
    .map((row) => {
      const height = row.count === 0 ? 0 : Math.max(4, Math.round((row.count / top) * BAR_MAX_PX));
      const zero = row.count === 0 ? ' is-zero' : '';
      const style = zero ? '' : ` style="height:${height}px"`;
      return `<div class="slot">
        <div class="slot-value">${row.count}</div>
        <div class="bar${zero}"${style}></div>
      </div>`;
    })
    .join('');

  const labels = rows
    .map(
      (row) => `<div class="label-cell${row.count === 0 ? ' is-zero' : ''}">
        <b>${row.name}</b>
        <span>${share(row.count, total).toFixed(1)}%</span>
      </div>`,
    )
    .join('');

  return `<section class="card">
    <div class="card-head">
      <h2>${labelTitle}</h2>
      <div class="total">Total <b>${total}</b></div>
    </div>
    <div class="card-body">
      <div class="chart-plot">
        <div class="axis">${axis}</div>
        <div class="plot">${bars}</div>
      </div>
      <div class="chart-labels">
        <div class="axis-spacer"></div>
        <div class="labels">${labels}</div>
      </div>
    </div>
  </section>`;
}

function emptyChart(labelTitle) {
  return `<section class="card">
    <div class="card-head">
      <h2>${labelTitle}</h2>
      <div class="total">Total <b>0</b></div>
    </div>
    <div class="card-body">
      <div class="chart-empty">无启用人员</div>
    </div>
  </section>`;
}

/** 人员表：列出所有导入的人员，含已停用。空池状态下全部行都标为已停用，与图表口径一致。 */
function peopleTable(forceInactive) {
  const rows = PEOPLE.map(
    (person) => {
      const active = forceInactive ? false : person.active;
      return `<tr>
      <td class="mono">${person.no}</td>
      <td><strong>${person.name}</strong></td>
      <td><span class="badge">${person.role}</span></td>
      <td>${person.level}</td>
      <td>${person.travel}</td>
      <td>${
        active
          ? '<span class="badge badge-success">在职</span>'
          : '<span class="badge badge-muted">已停用</span>'
      }</td>
    </tr>`;
    },
  ).join('');

  const inactive = forceInactive ? PEOPLE.length : PEOPLE.filter((person) => !person.active).length;

  return `<section class="card">
    <div class="card-head">
      <h2>人员池</h2>
      <div class="total">共 <b>${PEOPLE.length}</b> 行，其中 <b>${inactive}</b> 行已停用</div>
    </div>
    <table>
      <thead>
        <tr><th>工号</th><th>姓名</th><th>角色</th><th>职级</th><th>差旅</th><th>状态</th></tr>
      </thead>
      <tbody>${rows}</tbody>
    </table>
  </section>`;
}

function legend() {
  return `<section class="card">
    <div class="card-head"><h2>图上元素与规格条款</h2></div>
    <div class="card-body">
      <dl class="legend">
        <dt>两个总数相等</dt>
        <dd>两张图描述同一个启用池 → <code>S1</code>；本例两个总数都是 113</dd>
        <dt>空分类保留柱子</dt>
        <dd><code>UX</code> 为 0 仍占一个位置、读 <code>0</code> / <code>0.0%</code> → <code>S5</code>、<code>B-3</code></dd>
        <dt>柱上两个数字</dt>
        <dd>人数 + 占总数的比例，一位小数四舍五入：40/113 读 35.4% → <code>S3</code>、<code>S4</code></dd>
        <dt>顺序不按人数</dt>
        <dd>角色按目录序 PM → BA → DEV → QA → UX；等级按 Junior → Senior → Lead → Principal → <code>S6</code>、<code>S7</code></dd>
        <dt>表格与图表刻意不同</dt>
        <dd>表里列出已停用的 <code>E190</code>，图表只数在职 → <code>S12</code>、<code>B-14</code></dd>
        <dt>仅协调员可见</dt>
        <dd>项目经理进入本页时图表区整块不渲染、也不请求报表 → <code>S9</code>、<code>B-13</code></dd>
      </dl>
    </div>
  </section>`;
}

function renderCoordinator(totalOverride) {
  const roleTotal = totalOverride ?? sum(ROLE_AXIS);
  const gradeTotal = totalOverride ?? sum(GRADE_AXIS);
  const empty = roleTotal === 0;

  const charts = empty
    ? `<div class="chart-grid">
        ${emptyChart('Headcount by Role')}
        ${emptyChart('Headcount by Grade')}
      </div>`
    : `<div class="chart-grid">
        ${chart('Headcount by Role', ROLE_AXIS, roleTotal)}
        ${chart('Headcount by Grade', GRADE_AXIS, gradeTotal)}
      </div>`;

  const note = empty
    ? `<div class="banner">池中没有任何启用人员：两张图都被同一句空态替换，不画柱、不算占比、不做除法 → <code>S10</code>、<code>B-7</code>。</div>`
    : `<div class="banner">两张图是人员池页面的一个区块，不新增路由；表里的停用人员不进图表，也不进总数 → <code>B-14</code>、<code>S12</code>。</div>`;

  return `<div class="page-head">
      <div>
        <h1>人员池</h1>
        <div class="lede">每个人员只有一个角色；角色由报表写入，界面不能新增或修改。技能是能力项，不是角色。</div>
      </div>
    </div>
    ${charts}
    ${note}
    ${peopleTable(empty)}
    ${legend()}`;
}

function renderLead() {
  return `<div class="page-head">
      <div>
        <h1>人员池</h1>
        <div class="lede">这一页只对资源协调员开放。</div>
      </div>
    </div>
    <div class="banner danger">403 · 人员池只对资源协调员开放。</div>
    <div class="banner">
      导航栏不为项目经理绘制「人员池」入口；即使直接打开地址，<b>图表区也不渲染、报表也不请求</b>
      → <code>S9</code>、<code>B-13</code>。
      这里既没有占位、也没有拒绝文案，更没有数字。
    </div>`;
}

function render(state) {
  const account = ACCOUNT[state];
  document.getElementById('account').innerHTML =
    `<span class="avatar">${account.name.slice(0, 1)}</span><span>${account.name} · ${account.role}</span>`;

  // 壳层：人员池只对协调员出现在导航里（对应 lib/nav.ts 的 NAV）
  document.getElementById('nav-people').style.display = state === 'lead' ? 'none' : '';

  const view = document.getElementById('view');
  if (state === 'lead') view.innerHTML = renderLead();
  else if (state === 'empty') view.innerHTML = renderCoordinator(0);
  else view.innerHTML = renderCoordinator();

  document.querySelectorAll('.state-btn').forEach((button) => {
    button.classList.toggle('is-active', button.dataset.state === state);
  });
}

document.querySelectorAll('.state-btn').forEach((button) => {
  button.addEventListener('click', () => render(button.dataset.state));
});

render('coordinator');
