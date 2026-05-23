import test from 'node:test'
import assert from 'node:assert/strict'

import { createInflightRequestDeduper } from '../inflightRequestDeduperCore.ts'

test('inflight request deduper merges concurrent calls with the same key', async () => {
  let loadCount = 0
  const deduper = createInflightRequestDeduper()
  const loader = async () => {
    loadCount += 1
    await new Promise((resolve) => setTimeout(resolve, 10))
    return { value: loadCount }
  }

  const [first, second] = await Promise.all([
    deduper.run('sale-order-page::page=1&size=10&status=DRAFT', loader),
    deduper.run('sale-order-page::page=1&size=10&status=DRAFT', loader)
  ])

  assert.equal(loadCount, 1)
  assert.deepEqual(first, { value: 1 })
  assert.deepEqual(second, { value: 1 })
})

test('inflight request deduper allows a new call after the previous one settles', async () => {
  let loadCount = 0
  const deduper = createInflightRequestDeduper()
  const loader = async () => {
    loadCount += 1
    return { value: loadCount }
  }

  const first = await deduper.run('sale-order-page::page=1&size=10&status=DRAFT', loader)
  const second = await deduper.run('sale-order-page::page=1&size=10&status=DRAFT', loader)

  assert.deepEqual(first, { value: 1 })
  assert.deepEqual(second, { value: 2 })
  assert.equal(loadCount, 2)
})
