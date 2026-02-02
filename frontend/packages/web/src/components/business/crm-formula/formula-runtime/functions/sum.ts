export default function SUM(...args: any[]): number {
  let total = 0;

  args.forEach((v) => {
    if (Array.isArray(v)) {
      total += SUM(...v);
    } else if (typeof v === 'number') {
      total += v;
    }
  });

  return total;
}
