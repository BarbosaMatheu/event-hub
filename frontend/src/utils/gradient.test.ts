import { describe, expect, it } from 'vitest';
import { gradientFor } from './gradient';

describe('gradientFor', () => {
  it('is deterministic', () => {
    expect(gradientFor('Music')).toBe(gradientFor('Music'));
  });

  it('always returns a gradient class pair', () => {
    for (const name of ['Music', 'Technology', 'Sports', '', 'Ünïcode']) {
      expect(gradientFor(name)).toMatch(/^from-\w+-\d+ to-\w+-\d+$/);
    }
  });
});
