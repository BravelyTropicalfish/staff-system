import { useEffect, useState } from 'react';
import { ApiError, getJson } from '../lib/api';
import type { HeadcountReport } from '../lib/headcount';
import ChartCard from './ChartCard';

function failure(error: unknown): string {
  return error instanceof ApiError ? `${error.code}: ${error.message}` : String(error);
}

/**
 * 人员池页面顶上的两张图。它自己读一次报表，但只在页面已经确认调用者是协调员之后才挂载 ——
 * 否则项目经理这一页会先发出报表请求再把它藏起来，而规格要求的是“不请求”。
 *
 * <p>次序与顺序都照服务端答案渲染：柱子按收到的顺序排，占比按收到的数字打印。
 */
export default function HeadcountSection() {
  const [report, setReport] = useState<HeadcountReport | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    getJson<HeadcountReport>('/api/headcount')
      .then((row) => {
        if (!cancelled) {
          setReport(row);
        }
      })
      .catch((caught: unknown) => {
        if (!cancelled) {
          setError(failure(caught));
        }
      });
    return () => {
      cancelled = true;
    };
  }, []);

  if (error !== null) {
    return <div className="banner danger">{error}</div>;
  }
  if (report === null) {
    return <div className="banner">正在读取人数报表…</div>;
  }

  return (
    <div className="chart-grid">
      <ChartCard
        title="Headcount by Role"
        total={report.total}
        bars={report.byRole.map((bar) => ({
          key: String(bar.roleId),
          label: bar.role,
          count: bar.count,
          share: bar.share,
        }))}
      />
      <ChartCard
        title="Headcount by Grade"
        total={report.total}
        bars={report.byGrade.map((bar) => ({
          key: bar.grade,
          label: bar.grade,
          count: bar.count,
          share: bar.share,
        }))}
      />
    </div>
  );
}
