/**
 * The payloads of `GET /api/roles` and `GET /api/skills` and the readings the catalog screen makes
 * of them. The server owns what a rename or a deactivation may do: the screen only names an item,
 * offers the one toggle its state allows, and shows the refusal the server sends back.
 */

/** A skill with the single role whose domain holds it, as `/api/skills` answers it. */
export type Skill = {
  id: number;
  name: string;
  roleDomainId: number;
  roleDomainName: string;
  active: boolean;
};

/** A role with the skills of its domain, as `/api/roles` answers it. */
export type Role = {
  id: number;
  name: string;
  active: boolean;
  skills: Skill[];
};

/** A role's domain read as names; a role added later starts empty and says so. */
export function domainLabel(role: Pick<Role, 'skills'>): string {
  return role.skills.length === 0 ? 'no domain skills' : role.skills.map((skill) => skill.name).join(', ');
}

/** The one toggle an item's state allows. */
export function toggleLabel(item: Pick<Role, 'active'>): string {
  return item.active ? 'Deactivate' : 'Activate';
}

/** The patch body the toggle sends: the state flipped, never a state guessed by the screen. */
export function toggleBody(item: Pick<Role, 'active'>): { active: boolean } {
  return { active: !item.active };
}

/** A rename sends the trimmed name and nothing else, so the item's state is left where it was. */
export function renameBody(name: string): { name: string } {
  return { name: name.trim() };
}

/** Only the coordinator writes a catalog; a lead reads it. */
export function canEditCatalogs(role: string): boolean {
  return role === 'COORDINATOR';
}
