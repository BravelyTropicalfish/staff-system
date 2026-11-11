/**
 * 壳层的规则：角色能看到哪些链接、侧栏怎么称呼它们、每个路由的面包屑写什么。放在组件之外，
 * 这样这些规则不用浏览器就能测，和其他屏幕的做法一致。
 *
 * <p>三组链接就是原型的（`prototype/app.js` 的 NAV）：项目交付装岗位与缺口、推荐与确认；
 * 人员装候选人搜索与人员池；数据装报表导入与目录管理。作废通知不再是独立目的地 —— 它读在
 * 决策收件箱里，就在它所属的推荐旁边。
 */

export type Role = 'COORDINATOR' | 'LEAD';

/** `badge` 指明侧栏某个链接旁要显示哪个计数，由壳层填值。 */
export type NavItem = { to: string; label: string; roles: Role[]; badge?: 'pending' };

export type NavGroup = { group: string; items: NavItem[] };

/** 原型的三组六项，路由用本应用实际提供的那些。 */
export const NAV: NavGroup[] = [
  {
    group: '项目交付',
    items: [
      { to: '/posts', label: '岗位与缺口', roles: ['COORDINATOR', 'LEAD'] },
      { to: '/recommendations', label: '推荐与确认', roles: ['COORDINATOR', 'LEAD'], badge: 'pending' },
    ],
  },
  {
    group: '人员',
    items: [
      { to: '/candidates', label: '候选人搜索', roles: ['COORDINATOR'] },
      { to: '/people', label: '人员池', roles: ['COORDINATOR'] },
    ],
  },
  {
    group: '数据',
    items: [
      { to: '/imports', label: '报表导入', roles: ['COORDINATOR'] },
      { to: '/catalogs', label: '目录管理', roles: ['COORDINATOR'] },
    ],
  },
];

/** 未登录的访客看到整张地图，这样门口不会显得空。 */
export function visibleNav(groups: NavGroup[], role: Role | null): NavGroup[] {
  return groups
    .map((group) => ({
      group: group.group,
      items: group.items.filter((item) => role === null || item.roles.includes(role)),
    }))
    .filter((group) => group.items.length > 0);
}

/** 一条路径属于哪个区、面包屑最后落在哪个词。岗位、岗位编辑与候选人搜索都读作项目交付。 */
export function breadcrumb(pathname: string): { section: string; page: string } {
  if (pathname.startsWith('/people')) {
    return { section: '人员池', page: pathname === '/people' ? '全部人员' : '人员档案' };
  }
  if (pathname.startsWith('/candidates') || pathname.endsWith('/candidates')) {
    return { section: '项目交付', page: '候选人搜索' };
  }
  if (pathname === '/posts') {
    return { section: '项目交付', page: '岗位与缺口' };
  }
  if (pathname.startsWith('/posts/')) {
    return { section: '项目交付', page: '岗位' };
  }
  if (pathname.startsWith('/recommendations')) {
    return { section: '项目交付', page: '推荐与确认' };
  }
  if (pathname.startsWith('/projects/') && pathname.endsWith('/posts/new')) {
    return { section: '项目交付', page: '新建岗位' };
  }
  if (pathname.startsWith('/projects')) {
    return { section: '项目交付', page: '项目' };
  }
  if (pathname.startsWith('/imports')) {
    return { section: '数据', page: '报表导入' };
  }
  if (pathname.startsWith('/catalogs')) {
    return { section: '数据', page: '目录管理' };
  }
  if (pathname.startsWith('/notices')) {
    return { section: '项目交付', page: '作废通知' };
  }
  if (pathname.startsWith('/login')) {
    return { section: '账号', page: '登录' };
  }
  return { section: '工作台', page: '概览' };
}

/** 当前应高亮的导航项，让详情页保持父项点亮。 */
export function activeLink(pathname: string): string | null {
  if (pathname.startsWith('/recommendations')) return '/recommendations';
  if (pathname.startsWith('/candidates') || pathname.endsWith('/candidates')) return '/candidates';
  if (pathname.startsWith('/posts') || pathname.startsWith('/projects')) return '/posts';
  if (pathname.startsWith('/people')) return '/people';
  if (pathname.startsWith('/imports')) return '/imports';
  if (pathname.startsWith('/catalogs')) return '/catalogs';
  return null;
}

/** 账号胶囊的第二行：把服务端说的角色写成中文。 */
export function roleWord(role: string): string {
  if (role === 'COORDINATOR') return '资源协调员';
  if (role === 'LEAD') return '项目经理';
  return role;
}

/** 头像字形：账号首字母大写。 */
export function initial(name: string): string {
  return (name.trim()[0] ?? '?').toUpperCase();
}
