import assert from 'node:assert/strict'
import test from 'node:test'
import { createInFlightRequestDeduper } from '../src/project/inFlightRequestDeduper.js'

test('two overlapping project-detail loads perform only one request', async () => {
  const loadOnce = createInFlightRequestDeduper()
  let calls = 0
  let finish
  const response = new Promise((resolve) => { finish = resolve })

  const first = loadOnce('17', () => { calls += 1; return response })
  const second = loadOnce('17', () => { calls += 1; return response })
  assert.strictEqual(first, second)

  await Promise.resolve()
  assert.equal(calls, 1)
  finish({ id: 17 })
  assert.deepEqual(await first, { id: 17 })
  assert.deepEqual(await second, { id: 17 })
})

test('a later visit can load again after the pending request finishes', async () => {
  const loadOnce = createInFlightRequestDeduper()
  let calls = 0
  const load = () => { calls += 1; return Promise.resolve('ok') }

  assert.equal(await loadOnce('17', load), 'ok')
  assert.equal(await loadOnce('17', load), 'ok')
  assert.equal(calls, 2)
})

test('loads for different project IDs are independent', async () => {
  const loadOnce = createInFlightRequestDeduper()
  const ids = []

  await Promise.all([
    loadOnce('17', () => { ids.push('17'); return 17 }),
    loadOnce('22', () => { ids.push('22'); return 22 }),
  ])
  assert.deepEqual(ids, ['17', '22'])
})

test('failed requests are cleared so retry remains possible', async () => {
  const loadOnce = createInFlightRequestDeduper()
  let attempts = 0
  await assert.rejects(loadOnce('17', () => { attempts += 1; throw new Error('offline') }), /offline/)
  assert.equal(await loadOnce('17', () => { attempts += 1; return 'recovered' }), 'recovered')
  assert.equal(attempts, 2)
})
