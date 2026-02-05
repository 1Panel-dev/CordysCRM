export default function DAYS(end: number, start: number): number {
  if (typeof end !== 'number' || typeof start !== 'number') return 0;
  return Math.floor(end - start);
}
