package com.example.staffing.api;

import com.example.staffing.domain.ImportBatchView;
import com.example.staffing.domain.ImportService;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * The workbook upload and its history. Only the coordinator sends people data in, and only the
 * coordinator reads what the system made of each file.
 *
 * <p>A file with the wrong shape is refused with 422 and leaves no batch; a file with the right
 * shape but bad rows is created as a batch with status {@code REJECTED}, so the answer is still a
 * 201 — the resource that exists is the history entry that lists what to fix.
 */
@RestController
@RequestMapping("/api/imports")
public class ImportController {

  private final ImportService imports;

  public ImportController(ImportService imports) {
    this.imports = imports;
  }

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  ImportBatchView upload(@RequestPart("file") MultipartFile file, Authentication authentication)
      throws IOException {
    return imports.upload(
        file.getOriginalFilename(), file.getBytes(), Auth.coordinator(authentication).id());
  }

  @GetMapping
  List<ImportBatchView> history(Authentication authentication) {
    Auth.coordinator(authentication);
    return imports.history();
  }

  @GetMapping("/{id}")
  ImportBatchView batch(@PathVariable long id, Authentication authentication) {
    Auth.coordinator(authentication);
    return imports.batch(id);
  }
}
