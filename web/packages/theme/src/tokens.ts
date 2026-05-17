/**
 * Shared design tokens for wscan+.
 *
 * Source of truth: design/tokens/wscan.tokens.json
 * Regenerate with: pnpm --filter @wscanplus/theme generate
 *
 * Do NOT hand-edit color/space/radius values here. Edit the JSON, then run generate.
 * Token names follow {category}.{role} casing from the JSON, flattened.
 */

export const color = {
  canvas: '#0B1220',
  panel: '#121A2B',
  panelElevated: '#1A2336',
  panelHover: '#24324A',
  border: '#2A3850',
  borderStrong: '#42526B',
  accentScan: '#2AC3FF',
  accentFocus: '#7C5CFF',
  threatLow: '#2FBF71',
  threatMedium: '#F2B94B',
  threatHigh: '#FF8A3D',
  threatCritical: '#FF5A5F',
  rssiWeak: '#FF8A3D',
  rssiModerate: '#F2B94B',
  rssiStrong: '#2FBF71',
  textStrong: '#F2F5F9',
  text: '#D6DCE7',
  textMuted: '#A4AFBF',
  textDisabled: '#6B7586',
  console: '#05070B',
} as const;

export const radius = {
  sm: '6px',
  md: '10px',
  lg: '14px',
  xl: '18px',
  pill: '999px',
} as const;

export const space = {
  1: '4px',
  2: '8px',
  3: '12px',
  4: '16px',
  5: '20px',
  6: '24px',
  8: '32px',
} as const;

export const motion = {
  fast: '120ms',
  medium: '200ms',
} as const;

export const font = {
  display: '"Space Grotesk", "Inter Tight", system-ui, sans-serif',
  body: '"Inter", "IBM Plex Sans", system-ui, sans-serif',
  mono: '"JetBrains Mono", "Geist Mono", ui-monospace, "SF Mono", Menlo, monospace',
} as const;

/** Threat level enum + colors. Maps to ANOMALY_LEVELS in the prototype. */
export type ThreatLevel = 'low' | 'medium' | 'high' | 'critical';

export const threatColor: Record<ThreatLevel, string> = {
  low: color.threatLow,
  medium: color.threatMedium,
  high: color.threatHigh,
  critical: color.threatCritical,
};

export const threatShort: Record<ThreatLevel, string> = {
  low: 'LOW',
  medium: 'MED',
  high: 'HIGH',
  critical: 'CRIT',
};

/** Map an RSSI value to its strength bucket and color. */
export function rssiBucket(rssi: number): {
  level: 1 | 2 | 3 | 4;
  color: string;
  label: 'weak' | 'moderate' | 'strong';
} {
  const level = rssi > -55 ? 4 : rssi > -65 ? 3 : rssi > -75 ? 2 : 1;
  if (level >= 3) return { level: level as 3 | 4, color: color.rssiStrong, label: 'strong' };
  if (level === 2) return { level, color: color.rssiModerate, label: 'moderate' };
  return { level: 1, color: color.rssiWeak, label: 'weak' };
}
