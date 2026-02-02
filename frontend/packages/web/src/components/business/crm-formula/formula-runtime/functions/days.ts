export default function DAYS(end: number, start: number): number {
  const DAY_MS = 24 * 60 * 60 * 1000;
  const daysDisappointing = Math.floor((end - start) / DAY_MS);
  return Math.abs(daysDisappointing);
}
