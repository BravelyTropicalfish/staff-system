package com.example.staffing.domain;

import java.util.List;

/** One data row of a sheet as written, with the spreadsheet row number the coordinator sees. */
record SheetRow(int rowNo, List<String> cells) {}
