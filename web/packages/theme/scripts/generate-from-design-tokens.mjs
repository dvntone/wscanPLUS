#!/usr/bin/env node
/**
 * Regenerate src/tokens.ts and src/tokens.css from design/tokens/wscan.tokens.json.
 * Idempotent — safe to run on every save.
 *
 * Usage: node scripts/generate-from-design-tokens.mjs
 */

import { readFileSync, writeFileSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const HERE = dirname(fileURLToPath(import.meta.url));
const PKG_DIR = resolve(HERE, '..');
const REPO_ROOT = resolve(PKG_DIR, '..', '..', '..');
const SOURCE_JSON = join(REPO_ROOT, 'design', 'tokens', 'wscan.tokens.json');

const tokens = JSON.parse(readFileSync(SOURCE_JSON, 'utf8'));

const camel = (s) => s.replace(/-([a-z])/g, (_, c) => c.toUpperCase());

function flatten(group) {
  return Object.fromEntries(
    Object.entries(group).map(([k, v]) => [camel(k), v.value]),
  );
}

const color = flatten(tokens.color);
const radius = flatten(tokens.radius);
const space = flatten(tokens.space);
const motion = flatten(tokens.motion);

let ts = `/**\n * Generated from design/tokens/wscan.tokens.json — do not hand-edit.\n */\n\n`;

const stringify = (obj) =>
  Object.entries(obj)
    .map(([k, v]) => `  ${/^\d/.test(k) ? `'${k}'` : k}: '${v}',`)
    .join('\n');

ts += `export const color = {\n${stringify(color)}\n} as const;\n\n`;
ts += `export const radius = {\n${stringify(radius)}\n} as const;\n\n`;
ts += `export const space = {\n${stringify(space)}\n} as const;\n\n`;
ts += `export const motion = {\n${stringify(motion)}\n} as const;\n`;

writeFileSync(join(PKG_DIR, 'src', 'tokens.generated.ts'), ts);
console.log('Wrote src/tokens.generated.ts');

// CSS vars
const cssLines = [
  '/* Generated from design/tokens/wscan.tokens.json — do not hand-edit. */',
  ':root {',
  ...Object.entries(color).map(([k, v]) => `  --ws-${k}: ${v};`),
  ...Object.entries(radius).map(([k, v]) => `  --ws-r-${k}: ${v};`),
  ...Object.entries(space).map(([k, v]) => `  --ws-s-${k}: ${v};`),
  ...Object.entries(motion).map(([k, v]) => `  --ws-m-${k}: ${v};`),
  '}',
  '',
];
writeFileSync(join(PKG_DIR, 'src', 'tokens.generated.css'), cssLines.join('\n'));
console.log('Wrote src/tokens.generated.css');
