// 日期差天数统计:日历天数差,同一天内，不管相差 1 分钟还是 23 小时，都算 0 天
export default function DAYS(end: number, start: number): number {
  const DAY_MS = 24 * 60 * 60 * 1000;

  const d1 = new Date(end);
  const d2 = new Date(start);
  // 统一到当天 00:00:00
  d1.setHours(0, 0, 0, 0);
  d2.setHours(0, 0, 0, 0);

  return Math.abs(Math.round((d1.getTime() - d2.getTime()) / DAY_MS));
}
