/**
 * 人数报表的载荷与区块自己的读法。占比、顺序与总数都由服务端定，这里只把数字变成文字和像素 ——
 * 客户端绝不重算占比，否则同一个人数会出现第二种舍入。
 */

export type RoleBar = {
  roleId: number;
  role: string;
  count: number;
  share: number;
};

export type GradeBar = {
  grade: string;
  count: number;
  share: number;
};

export type HeadcountReport = {
  /** 启用人数；两条轴的柱子之和都应等于它。 */
  total: number;
  byRole: RoleBar[];
  byGrade: GradeBar[];
};

/** 柱子的最大像素高度；绘图区高度留出数值行。 */
export const BAR_MAX_PX = 170;

/** 一位小数的占比文字：0 读作 `0.0%`，40/113 读作 `35.4%`。 */
export function shareText(share: number): string {
  return `${share.toFixed(1)}%`;
}

/** 轴上的最高柱；空池时为 0。 */
export function axisTop(bars: { count: number }[]): number {
  return bars.reduce((top, bar) => Math.max(top, bar.count), 0);
}

/**
 * 纵轴刻度，由上到下：把最高柱抬到一个整十（小池子时整二）的刻度上，再均分五格。
 * 最高柱为 0 时只有一根 `0`，因为此图的空态已经把柱子换掉了。
 */
export function chartTicks(top: number): number[] {
  const step = top <= 10 ? 2 : Math.ceil(top / 5 / 10) * 10;
  const ceiling = Math.ceil(top / step) * step;
  const ticks: number[] = [];
  for (let value = ceiling; value >= 0; value -= step) {
    ticks.push(value);
  }
  return ticks;
}

/** 柱高：与人数成正比。0 值柱由样式画成刻度，所以这里返回 0。 */
export function barHeight(count: number, top: number): number {
  if (count === 0 || top <= 0) {
    return 0;
  }
  return Math.round((count / top) * BAR_MAX_PX);
}

/** 池里没有启用人员时，两张图都被同一句空态替换。 */
export function isEmpty(report: HeadcountReport): boolean {
  return report.total === 0;
}

/** 每张图头上的总数行。 */
export function totalText(total: number): string {
  return `Total ${total}`;
}
