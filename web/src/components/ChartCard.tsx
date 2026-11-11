import { axisTop, barHeight, chartTicks, shareText, totalText } from '../lib/headcount';

export type ChartBar = {
  key: string;
  label: string;
  count: number;
  share: number;
};

/**
 * 一张柱状图：标题、总数、纵轴刻度、按人数成比例的柱，柱下是分类名与占比。池里没有启用人员
 * 时整块换成那句空态 —— 最高柱为 0 的图没有可画的刻度，所以空态优先于 0 值柱。
 */
export default function ChartCard({
  title,
  total,
  bars,
}: {
  title: string;
  total: number;
  bars: ChartBar[];
}) {
  const top = axisTop(bars);

  return (
    <section className="card">
      <div className="card-head">
        <h2>{title}</h2>
        <div className="total">{totalText(total)}</div>
      </div>
      <div className="card-body">
        {total === 0 ? (
          <div className="chart-empty">无启用人员</div>
        ) : (
          <>
            <div className="chart-plot">
              <div className="axis">
                {chartTicks(top).map((tick) => (
                  <span key={tick}>{tick}</span>
                ))}
              </div>
              <div className="plot">
                {bars.map((bar) => (
                  <div className="bar-slot" key={bar.key}>
                    <div className="bar-value">{bar.count}</div>
                    {bar.count === 0 ? (
                      <div className="bar is-zero" />
                    ) : (
                      <div className="bar" style={{ height: barHeight(bar.count, top) }} />
                    )}
                  </div>
                ))}
              </div>
            </div>
            <div className="chart-labels">
              <div className="axis-spacer" />
              <div className="labels">
                {bars.map((bar) => (
                  <div className={bar.count === 0 ? 'label-cell is-zero' : 'label-cell'} key={bar.key}>
                    <b>{bar.label}</b>
                    <span>{shareText(bar.share)}</span>
                  </div>
                ))}
              </div>
            </div>
          </>
        )}
      </div>
    </section>
  );
}
