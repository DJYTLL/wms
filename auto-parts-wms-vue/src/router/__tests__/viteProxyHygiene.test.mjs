import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'
import { fileURLToPath } from 'node:url'
import { test } from 'node:test'

const root = fileURLToPath(new URL('../../../', import.meta.url))
const viteConfigSource = readFileSync(join(root, 'vite.config.ts'), 'utf8')

test('dev proxy only matches the api prefix segment', () => {
  assert.match(viteConfigSource, /'\^\/api\(\?:\/\|\$\)'/)
  assert.doesNotMatch(viteConfigSource, /proxy:\s*\{[\s\S]*?['"]\/api['"]\s*:/)
})
