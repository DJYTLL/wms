import test from 'node:test'
import assert from 'node:assert/strict'

import { createRequestStateCache } from '../requestStateCacheCore.ts'

test('request state cache deduplicates concurrent loads by key', async () => {
  let loadCount = 0
  const cache = createRequestStateCache()
  const loader = async () => {
    loadCount += 1
    return { value: loadCount }
  }

  const [first, second] = await Promise.all([
    cache.getOrLoad('tenant-columns::draft::T1', loader),
    cache.getOrLoad('tenant-columns::draft::T1', loader)
  ])

  assert.equal(loadCount, 1)
  assert.deepEqual(first, { value: 1 })
  assert.deepEqual(second, { value: 1 })
})

test('request state cache supports overriding and invalidation', async () => {
  let loadCount = 0
  const cache = createRequestStateCache()
  const loader = async () => {
    loadCount += 1
    return { value: loadCount }
  }

  cache.set('user-table::erp-sale-draft::U1', { value: 9 })
  const cached = await cache.getOrLoad('user-table::erp-sale-draft::U1', loader)
  cache.invalidate('user-table::erp-sale-draft::U1')
  const reloaded = await cache.getOrLoad('user-table::erp-sale-draft::U1', loader)

  assert.deepEqual(cached, { value: 9 })
  assert.deepEqual(reloaded, { value: 1 })
  assert.equal(loadCount, 1)
})
