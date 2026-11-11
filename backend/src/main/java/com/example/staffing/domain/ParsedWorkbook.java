package com.example.staffing.domain;

import java.util.List;

/** The two sheets of an upload, read but not yet judged. Rows are in sheet order, blank rows gone. */
record ParsedWorkbook(
    ColumnMap peopleColumns,
    List<SheetRow> people,
    ColumnMap skillColumns,
    List<SheetRow> skills) {}
