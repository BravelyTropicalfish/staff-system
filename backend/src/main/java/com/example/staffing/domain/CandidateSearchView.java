package com.example.staffing.domain;

import java.util.List;
import java.util.Map;

/**
 * The answer to one search of the pool for one post: the suitable list, the excluded people and the
 * count of exclusions per gate, so an empty list explains itself instead of failing. The counts
 * carry every gate, zero included, plus X1 and X2 — the X counts stay zero while the fallback did
 * not open.
 *
 * <p>The fallback is its own array below the suitable list, never interleaved with it, and carries
 * the reason the section exists (or null when it was not evaluated at all).
 */
public record CandidateSearchView(
    List<SuitabilityRow> suitable,
    List<ExclusionRow> excluded,
    Map<String, Integer> exclusionCounts,
    List<FallbackRow> fallback,
    String fallbackReason) {}
