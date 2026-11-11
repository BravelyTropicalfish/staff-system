package com.example.staffing.domain;

import com.example.staffing.data.ImportBatchRepository;
import com.example.staffing.data.PersonRepository;
import com.example.staffing.data.RoleRepository;
import com.example.staffing.data.SkillRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Uploading the workbook: the one place person data enters the system. The service reads the file,
 * validates every row, and only then writes — so a batch is either applied whole or recorded as
 * rejected whole, and a file that cannot be read at all leaves no batch behind.
 */
@Service
public class ImportService {

  private final ImportValidator validator = new ImportValidator();
  private final PersonRepository people;
  private final RoleRepository roles;
  private final SkillRepository skills;
  private final ImportBatchRepository batches;

  public ImportService(
      PersonRepository people,
      RoleRepository roles,
      SkillRepository skills,
      ImportBatchRepository batches) {
    this.people = people;
    this.roles = roles;
    this.skills = skills;
    this.batches = batches;
  }

  /** One upload, one unit of work: the batch row and every write it stands for commit together. */
  @Transactional
  public ImportBatchView upload(String filename, byte[] content, long uploaderId) {
    ParsedWorkbook workbook = WorkbookParser.parse(content);
    ImportResult result = validator.validate(workbook, reference());
    long batchId =
        batches.insert(
            filename,
            result.isRejected() ? "REJECTED" : "APPLIED",
            uploaderId,
            result.people().size(),
            result.skills().size(),
            result.errors().size());
    if (result.isRejected()) {
      batches.insertErrors(batchId, errorsOf(result));
    } else {
      write(result);
    }
    return batch(batchId);
  }

  public List<ImportBatchView> history() {
    return batches.findAll().stream().map(row -> view(row, List.of())).toList();
  }

  public ImportBatchView batch(long id) {
    ImportBatchRepository.BatchRow row =
        batches
            .findById(id)
            .orElseThrow(() -> new NoSuchElementException("没有批次 " + id));
    return view(row, batches.findErrors(id));
  }

  private void write(ImportResult result) {
    for (PersonImportRow person : result.people()) {
      people.upsert(
          person.employeeNo(),
          person.name(),
          person.email(),
          person.roleId(),
          person.jobLevel(),
          person.travel(),
          person.active());
    }
    for (SkillImportRow skill : result.skills()) {
      people.upsertBand(skill.employeeNo(), skill.skillId(), skill.band());
    }
  }

  /** The catalog as it stands now: only active items are accepted by a new file. */
  private ImportReference reference() {
    Map<String, ImportReference.RoleRef> rolesByName = new LinkedHashMap<>();
    for (RoleRepository.RoleRow role : roles.findAll()) {
      if (role.active()) {
        rolesByName.put(
            ImportReference.key(role.name()), new ImportReference.RoleRef(role.id(), role.name()));
      }
    }
    Map<String, ImportReference.SkillRef> skillsByName = new LinkedHashMap<>();
    for (SkillRepository.SkillRow skill : skills.findAll()) {
      if (skill.active()) {
        skillsByName.put(
            ImportReference.key(skill.name()),
            new ImportReference.SkillRef(skill.id(), skill.name()));
      }
    }
    return new ImportReference(rolesByName, skillsByName, people.employeeNumbers());
  }

  private static List<ImportBatchRepository.ErrorRow> errorsOf(ImportResult result) {
    return result.errors().stream()
        .map(
            error ->
                new ImportBatchRepository.ErrorRow(
                    error.sheet(), error.rowNo(), error.key(), error.message()))
        .toList();
  }

  private static ImportBatchView view(
      ImportBatchRepository.BatchRow row, List<ImportBatchRepository.ErrorRow> errors) {
    return new ImportBatchView(
        row.id(),
        row.filename(),
        row.uploader(),
        row.status(),
        row.uploadedAt() == null ? null : row.uploadedAt().toString(),
        row.personRows(),
        row.skillRows(),
        row.errorCount(),
        errors.stream()
            .map(error -> new RowError(error.sheet(), error.rowNo(), error.key(), error.message()))
            .toList());
  }
}
