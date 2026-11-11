import ExcelJS from 'exceljs';

/**
 * T26, the workbook fixture. The import reads two sheets by column name, so the builder writes the
 * headers `WorkbookParser` requires and one row per person or band; the bands are the seeded
 * vocabularies, because the validator rejects anything outside them.
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
    ]);
  }

  const skillsSheet = book.addWorksheet('skills');
  skillsSheet.addRow(['employee_no', 'skill', 'proficiency']);
  for (const row of skills) {
    skillsSheet.addRow([row.employeeNo, row.skill, row.proficiency]);
  }

  return Buffer.from(await book.xlsx.writeBuffer());
}
