// Full class names (not built dynamically) so Tailwind can see and generate them.
const GRADIENTS = [
  'from-indigo-500 to-violet-500',
  'from-sky-500 to-indigo-500',
  'from-emerald-500 to-teal-500',
  'from-rose-500 to-orange-500',
  'from-amber-500 to-pink-500',
  'from-fuchsia-500 to-purple-500',
];

/** A stable gradient per category name, used when an event has no image. */
export function gradientFor(name: string): string {
  let hash = 0;
  for (const char of name) hash = (hash * 31 + char.charCodeAt(0)) >>> 0;
  return GRADIENTS[hash % GRADIENTS.length];
}
