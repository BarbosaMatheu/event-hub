/**
 * Page numbers (0-based) to show in a pager: always the first and last page, a window around the
 * current one, and -1 where pages are skipped (rendered as an ellipsis).
 */
export function pageWindow(current: number, totalPages: number, radius = 1): number[] {
  if (totalPages <= 0) return [];
  const wanted = new Set<number>([0, totalPages - 1]);
  for (let p = current - radius; p <= current + radius; p++) {
    if (p >= 0 && p < totalPages) wanted.add(p);
  }
  const sorted = [...wanted].sort((a, b) => a - b);
  const result: number[] = [];
  for (let i = 0; i < sorted.length; i++) {
    if (i > 0 && sorted[i] - sorted[i - 1] > 1) result.push(-1);
    result.push(sorted[i]);
  }
  return result;
}
