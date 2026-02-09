export default function DAYS(end: number, start: number): number {
  if (!Number.isFinite(end) || !Number.isFinite(start)) return 0;

  return Math.floor(end) - Math.floor(start);
}
