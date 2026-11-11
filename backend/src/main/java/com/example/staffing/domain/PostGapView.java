package com.example.staffing.domain;

import java.util.List;

/**
 * One post in the gap view: the numbers both roles read identically, and the people behind them.
 * A filled post stays in the list with `stillShort` false, so its seats and their flags stay
 * visible; a cancelled post has no gap and is left out.
 */
public record PostGapView(
    long id,
    String title,
    String role,
    String status,
    int headcount,
    int confirmed,
    int remaining,
    boolean stillShort,
    List<GapPersonView> people) {}
