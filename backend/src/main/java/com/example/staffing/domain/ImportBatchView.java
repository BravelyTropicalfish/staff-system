package com.example.staffing.domain;

import java.util.List;

/**
 * One upload as the history shows it: who sent which file, what became of it, how many rows it
 * carried, and — for a rejected batch — every row that stopped it.
 */
public record ImportBatchView(
    long id,
    String filename,
    String uploader,
    String status,
    String uploadedAt,
    int personRows,
    int skillRows,
    int errorCount,
    List<RowError> errors) {}
