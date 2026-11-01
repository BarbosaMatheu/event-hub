import { describe, expect, it } from 'vitest';
import { pageWindow } from './pagination';

describe('pageWindow', () => {
  it('shows every page when there are few', () => {
    expect(pageWindow(0, 3)).toEqual([0, 1, 2]);
  });

  it('keeps first and last page and skips the middle with an ellipsis', () => {
    expect(pageWindow(0, 10)).toEqual([0, 1, -1, 9]);
    expect(pageWindow(5, 10)).toEqual([0, -1, 4, 5, 6, -1, 9]);
    expect(pageWindow(9, 10)).toEqual([0, -1, 8, 9]);
  });

  it('handles empty and single page results', () => {
    expect(pageWindow(0, 0)).toEqual([]);
    expect(pageWindow(0, 1)).toEqual([0]);
  });
});
