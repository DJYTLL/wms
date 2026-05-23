import test from 'node:test'
import assert from 'node:assert/strict'

import { createRequestRefreshQueue } from '../requestRefreshQueueCore.ts'

test('request refresh queue resolves every pending entry and clears the queue', async () => {
  const queue = createRequestRefreshQueue()
  const resolved = []

  const first = new Promise((resolve, reject) => {
    queue.enqueue({
      resolve,
      reject,
      retry: async (token) => {
        resolved.push(`first:${token}`)
        return `first:${token}`
      }
    })
  })

  const second = new Promise((resolve, reject) => {
    queue.enqueue({
      resolve,
      reject,
      retry: async (token) => {
        resolved.push(`second:${token}`)
        return `second:${token}`
      }
    })
  })

  assert.equal(queue.size(), 2)

  queue.resolveAll('token-1')

  assert.equal(queue.size(), 0)
  assert.deepEqual(await Promise.all([first, second]), ['first:token-1', 'second:token-1'])
  assert.deepEqual(resolved, ['first:token-1', 'second:token-1'])
})

test('request refresh queue rejects every pending entry and clears the queue', async () => {
  const queue = createRequestRefreshQueue()
  const error = new Error('refresh failed')

  const first = new Promise((resolve, reject) => {
    queue.enqueue({
      resolve,
      reject,
      retry: async () => 'unused-first'
    })
  })

  const second = new Promise((resolve, reject) => {
    queue.enqueue({
      resolve,
      reject,
      retry: async () => 'unused-second'
    })
  })

  assert.equal(queue.size(), 2)

  queue.rejectAll(error)

  assert.equal(queue.size(), 0)
  await assert.rejects(first, error)
  await assert.rejects(second, error)
})
