/**
 * The shape of the chrome: which links a role sees, what the sidebar calls them, and what the
 * breadcrumb says on each route. Kept out of the component so the rules can be tested without a
 * browser, the way every other screen keeps its readings.
 */

export type Role = 'COORDINATOR' | 'LEAD';

export type NavItem = { to: string; label: string; roles: Role[] };

export type NavGroup = { group: string; items: NavItem[] };

/** The prototype's three groups (`prototype/app.js` NAV), with the routes this app actually serves. */
export const NAV: NavGroup[] = [
  {
    group: 'Delivery',
    items: [{ to: '/projects', label: 'Projects and their posts', roles: ['COORDINATOR', 'LEAD'] }],
  },
  {
    group: 'People',
    items: [{ to: '/people', label: 'The people pool', roles: ['COORDINATOR'] }],
  },
  {
    group: 'Data',
    items: [
      { to: '/imports', label: 'Import the workbook', roles: ['COORDINATOR'] },
      { to: '/catalogs', label: 'Roles and skills', roles: ['COORDINATOR'] },
    ],
  },
  {
    group: 'Inbox',
    items: [{ to: '/notices', label: 'Notices', roles: ['COORDINATOR'] }],
  },
];

/** What a signed-out visitor sees: the whole map, so the app does not look empty at the door. */
export function visibleNav(groups: NavGroup[], role: Role | null): NavGroup[] {
  return groups
    .map((group) => ({
      group: group.group,
      items: group.items.filter((item) => role === null || item.roles.includes(role)),
    }))
    .filter((group) => group.items.length > 0);
}

/**
 * The section a path belongs to and the word the breadcrumb ends on. A post and its candidate search
 * live under the project they belong to, so they read as delivery work, not as a separate area.
 */
export function breadcrumb(pathname: string): { section: string; page: string } {
  if (pathname.startsWith('/people')) {
    return { section: 'People pool', page: pathname === '/people' ? 'Everyone' : 'Profile' };
  }
  if (pathname.startsWith('/projects/') && pathname.endsWith('/posts/new')) {
    return { section: 'Delivery', page: 'New post' };
  }
  if (pathname.startsWith('/projects/')) {
    return { section: 'Delivery', page: 'Project' };
  }
  if (pathname.startsWith('/projects')) {
    return { section: 'Delivery', page: 'Projects and their posts' };
  }
  if (pathname.endsWith('/candidates')) {
    return { section: 'Delivery', page: 'Candidate search' };
  }
  if (pathname.startsWith('/posts/')) {
    return { section: 'Delivery', page: 'Post and gap' };
  }
  if (pathname.startsWith('/imports')) {
    return { section: 'Data', page: 'Import the workbook' };
  }
  if (pathname.startsWith('/catalogs')) {
    return { section: 'Data', page: 'Roles and skills' };
  }
  if (pathname.startsWith('/notices')) {
    return { section: 'Inbox', page: 'Notices' };
  }
  if (pathname.startsWith('/login')) {
    return { section: 'Account', page: 'Sign in' };
  }
  return { section: 'Workspace', page: 'Overview' };
}

/** The nav entry that should read as current, so a detail screen keeps its parent lit. */
export function activeLink(pathname: string): string | null {
  if (pathname.startsWith('/projects') || pathname.startsWith('/posts')) return '/projects';
  if (pathname.startsWith('/people')) return '/people';
  if (pathname.startsWith('/imports')) return '/imports';
  if (pathname.startsWith('/catalogs')) return '/catalogs';
  if (pathname.startsWith('/notices')) return '/notices';
  return null;
}

/** The account chip's second line: what the server called the role, in words. */
export function roleWord(role: string): string {
  if (role === 'COORDINATOR') return 'Resource coordinator';
  if (role === 'LEAD') return 'Project lead';
  return role;
}

/** The avatar glyph: the first letter of the account, upper-cased. */
export function initial(name: string): string {
  return (name.trim()[0] ?? '?').toUpperCase();
}
