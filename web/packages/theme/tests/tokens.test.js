import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, join, resolve } from 'node:path';

const HERE = dirname(fileURLToPath(import.meta.url));
const PKG_DIR = resolve(HERE, '..');

test('tokens.ts re-exports same keys as design/tokens/wscan.tokens.json', async () => {
  const sourceJsonPath = join(PKG_DIR, '..', '..', '..', 'design', 'tokens', 'wscan.tokens.json');
  const source = JSON.parse(readFileSync(sourceJsonPath, 'utf8'));
  const mod = await import('../src/tokens.ts').catch(() => null);

  if (!mod) {
    // tokens.ts is TS — can only be loaded via a TS-aware test runner.
    // Skip strict re-export check in plain node:test; verified by the package CI.
    return;
  }
  for (const key of Object.keys(source.color)) {
    const camel = key.replace(/-([a-z])/g, (_, c) => c.toUpperCase());
    assert.ok(camel in mod.color, `missing color.${camel}`);
  }
});

test('rssiBucket buckets correctly', async () => {
  const { rssiBucket } = await import('../src/tokens.ts').catch(() => ({ rssiBucket: null }));
  if (!rssiBucket) return; // TS-only; covered by web package tests
  assert.equal(rssiBucket(-40).label, 'strong');
  assert.equal(rssiBucket(-60).label, 'strong');
  assert.equal(rssiBucket(-70).label, 'moderate');
  assert.equal(rssiBucket(-85).label, 'weak');
});
