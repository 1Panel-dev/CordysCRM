export function formatAiChatDuration(duration?: number): string {
  if (typeof duration !== 'number' || Number.isNaN(duration) || duration < 0) {
    return '';
  }

  const totalSeconds = duration / 1000;

  if (totalSeconds < 60) {
    return `${Number(totalSeconds.toFixed(totalSeconds < 10 ? 1 : 0))}s`;
  }

  const roundedSeconds = Math.round(totalSeconds);
  const hours = Math.floor(roundedSeconds / 3600);
  const minutes = Math.floor((roundedSeconds % 3600) / 60);
  const seconds = roundedSeconds % 60;
  const parts: string[] = [];

  if (hours > 0) {
    parts.push(`${hours}h`);
  }

  if (minutes > 0) {
    parts.push(`${minutes}m`);
  }

  if (seconds > 0) {
    parts.push(`${seconds}s`);
  }

  return parts.join(' ');
}
