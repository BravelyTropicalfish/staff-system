package com.example.staffing.domain;

import java.util.List;

/** The one sheet of an upload, read but not yet judged. Rows are in sheet order, blank rows gone. */
record ParsedWorkbook(ColumnMap peopleColumns, List<SheetRow> people) {}
