export default function DAYS(end: number, start: number): number {
  if (typeof end !== 'number' || typeof start !== 'number') return 0;
  return Math.max(0, Math.floor(end - start));
}
