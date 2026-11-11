package com.example.staffing.domain;

import com.example.staffing.data.HeadcountRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The headcount report's read side. It owns the axis, the counts and the one rounding rule; nothing
 * is stored, so every call answers the pool as it stands.
 */
@Service
public class HeadcountService {

  private final HeadcountRepository counts;

  public HeadcountService(HeadcountRepository counts) {
    this.counts = counts;
  }

  /** The report: the active pool, one bar per role on the axis, one bar per job-level band. */
  @Transactional(readOnly = true)
  public HeadcountView report() {
    List<HeadcountRepository.RoleCount> roles = counts.roleCounts();
    // Every active person holds exactly one role, so the role counts also give the total.
    int total = roles.stream().mapToInt(HeadcountRepository.RoleCount::count).sum();

    List<RoleBar> byRole =
        roles.stream()
            .map(row -> new RoleBar(row.roleId(), row.role(), row.count(), share(row.count(), total)))
            .toList();

    Map<String, Integer> held = new HashMap<>();
    for (HeadcountRepository.GradeCount row : counts.gradeCounts()) {
      held.put(row.grade(), row.count());
    }
    List<GradeBar> byGrade = new ArrayList<>(Bands.JOB_LEVELS.size());
    for (String band : Bands.JOB_LEVELS) {
      int count = held.getOrDefault(band, 0);
      byGrade.add(new GradeBar(band, count, share(count, total)));
    }

    return new HeadcountView(total, byRole, List.copyOf(byGrade));
  }

  /**
   * The share of the total as a percentage with one decimal, rounded half-up. A pool with no active
   * person has no divisor, so it answers zero rather than dividing.
   */
  static double share(int count, int total) {
    if (total == 0) {
      return 0.0;
    }
    return Math.round(count * 1000.0 / total) / 10.0;
  }
}
