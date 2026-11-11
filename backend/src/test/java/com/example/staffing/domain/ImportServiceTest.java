package com.example.staffing.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.staffing.data.ImportBatchRepository;
import com.example.staffing.data.PersonRepository;
import com.example.staffing.data.RoleRepository;
import com.example.staffing.data.SkillRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * One branch of the import history: a batch row the database always stamps still reads as a batch
 * without a time rather than failing the list.
 */
class ImportServiceTest {

  private final ImportBatchRepository batches = mock(ImportBatchRepository.class);
  private final ImportService service =
      new ImportService(
          mock(PersonRepository.class),
          mock(RoleRepository.class),
          mock(SkillRepository.class),
          batches);

  @Test
  void aBatchWithoutAnUploadTimeAnswersNoTime() {
    when(batches.findById(7))
        .thenReturn(
            Optional.of(
                new ImportBatchRepository.BatchRow(
                    7, "people.xlsx", "APPLIED", "coordinator", null, 2, 1, 0)));
    when(batches.findErrors(7)).thenReturn(List.of());

    ImportBatchView view = service.batch(7);

    assertThat(view.uploadedAt()).isNull();
    assertThat(view).extracting(ImportBatchView::status, ImportBatchView::personRows)
        .containsExactly("APPLIED", 2);
  }
}
