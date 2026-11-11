import ExcelJS from 'exceljs';

/**
 * The workbook fixture. The import reads one sheet by column name, so the builder writes the
 * headers `WorkbookParser` requires and one row per person; that person's skills are folded into the
 * `skills` cell as `skill:band;skill:band`, because one row is one person plus their skills. The
 * bands are the seeded vocabularies, because the validator rejects anything outside them.
 */

export type PersonRow = {
  employeeNo: string;
  name: string;
  email?: string | null;
  role: string;
  jobLevel: string;
  travel: string;
  active?: 'active' | 'inactive';
};

export type SkillRow = { employeeNo: string; skill: string; proficiency: string };

export async function workbook(people: PersonRow[], skills: SkillRow[]): Promise<Buffer> {
  const byEmployee = new Map<string, string[]>();
  for (const row of skills) {
    const held = byEmployee.get(row.employeeNo) ?? [];
    held.push(`${row.skill}:${row.proficiency}`);
    byEmployee.set(row.employeeNo, held);
  }

  const book = new ExcelJS.Workbook();
  const peopleSheet = book.addWorksheet('people');
  peopleSheet.addRow([
    'employee_no',
    'name',
    'email',
    'role',
    'job_level',
    'travel_willingness',
    'active',
    'skills',
  ]);
  for (const person of people) {
    peopleSheet.addRow([
      person.employeeNo,
      person.name,
      person.email ?? '',
      person.role,
      person.jobLevel,
      person.travel,
      person.active ?? 'active',
      (byEmployee.get(person.employeeNo) ?? []).join(';'),
    ]);
  }

  return Buffer.from(await book.xlsx.writeBuffer());
}
