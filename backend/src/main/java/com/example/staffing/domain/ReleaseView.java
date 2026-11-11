package com.example.staffing.domain;

/**
 * What a release leaves behind: the post's numbers recomputed from the remaining seats, and the
 * person who just became free for a post whose dates do not intersect the released range.
 */
public record ReleaseView(long postId, String personNo, String postStatus, int remaining) {}
