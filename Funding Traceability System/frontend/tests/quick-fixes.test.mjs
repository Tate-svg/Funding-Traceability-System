import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
const load = path => import('data:text/javascript;base64,' + Buffer.from(readFileSync(new URL(path, import.meta.url), 'utf8')).toString('base64'))
const { scorePreview, scoreValidationError } = await load('../src/utils/scoreValidation.js')
const { createSensitiveIdleSession } = await load('../src/utils/sensitiveIdleSession.js')
const items = () => [{ itemId: 1, weight: 30, itemScore: 0 }, { itemId: 2, weight: 70, itemScore: 80 }]
test('缺项无总分预览，但真实0分有效', () => {
  const input = items()
  assert.equal(scorePreview(input), '56.00')
  input[0].itemScore = undefined
  assert.equal(scorePreview(input), null)
  assert.match(scoreValidationError(input), /全部指标/)
})
test('重复指标、非法分数与权重配置不能预览', () => {
  for (const change of [x => { x[1].itemId = 1 }, x => { x[1].weight = 60 }, x => { x[1].itemScore = NaN }, x => { x[0].weight = -1 }, x => { x[1].itemScore = 101 }]) {
    const input = items(); change(input); assert.equal(scorePreview(input), null)
  }
})
function fixture(renew = () => Promise.resolve({ remainingSeconds: 900, absoluteSecondsRemaining: 7200 }), options = {}) {
  let time = 0; let expired = 0; let lastTick
  const timers = new Map(); let id = 0
  const guard = createSensitiveIdleSession({ idleSeconds: 900, remainingSeconds: 900, absoluteSecondsRemaining: 7200, ...options,
    renew, now: () => time, onExpire: () => expired++, onTick: n => { lastTick = n },
    setTimer: (fn, delay) => { timers.set(++id, { fn, at: time + delay }); return id }, clearTimer: k => timers.delete(k)
  })
  return { guard, timers, setTime: n => { time = n }, expired: () => expired, tick: () => lastTick,
    fire: () => { for (const [key, t] of [...timers]) if (t.at <= time) { timers.delete(key); t.fn() } } }
}
const flush = async () => { for (let i = 0; i < 8; i++) await Promise.resolve() }
test('无交互不续期，后台标签页恢复后按真实经过时间到期', () => {
  let calls = 0; const f = fixture(() => { calls++ })
  f.guard.check(); assert.equal(f.tick(), 900)
  f.setTime(900000); f.guard.check()
  assert.equal(f.expired(), 1); assert.equal(calls, 0)
  f.guard.activity(); assert.equal(f.timers.size, 0)
})
test('首次慢响应扣除传输时间，不延长授权截止', () => {
  const f = fixture(undefined, { initialRequestAt: -30000, absoluteSecondsRemaining: 60 })
  f.guard.check(); assert.equal(f.tick(), 30)
  f.setTime(30000); f.guard.check(); assert.equal(f.expired(), 1)
  const expired = fixture(undefined, { initialRequestAt: -60000, absoluteSecondsRemaining: 60 })
  expired.guard.check(); assert.equal(expired.expired(), 1)
})
test('交互续期受节流且绝对期限不会随活动延长', async () => {
  let calls = 0
  const f = fixture(() => { calls++; return Promise.resolve({ remainingSeconds: 900, absoluteSecondsRemaining: 6600 }) })
  f.setTime(600000)
  for (let i = 0; i < 100; i++) f.guard.activity()
  assert.equal(f.timers.size, 1); f.fire(); await flush()
  assert.equal(calls, 1); f.guard.check(); assert.equal(f.tick(), 900)
  f.setTime(7200000); f.guard.check(); assert.equal(f.expired(), 1)
})
test('用户关闭后，晚到续期响应不能恢复会话或敏感视图', async () => {
  let resolve
  const f = fixture(() => new Promise(r => { resolve = r }))
  f.setTime(30000); f.guard.activity(); f.fire(); await flush()
  f.guard.stop(); resolve({ remainingSeconds: 900, absoluteSecondsRemaining: 7100 }); await flush()
  assert.equal(f.guard.check(), false); assert.equal(f.expired(), 0)
})
test('续期失败时立即让展示层清空，而不是继续保留明文', async () => {
  const f = fixture(() => Promise.reject(new Error('expired')))
  f.setTime(30000); f.guard.activity(); f.fire(); await flush()
  assert.equal(f.expired(), 1); assert.equal(f.guard.check(), false)
})

test('续期响应不得突破最初已知的最长会话期限', async () => {
  const f = fixture(() => Promise.resolve({ remainingSeconds: 900, absoluteSecondsRemaining: 7200 }), { absoluteSecondsRemaining: 60 })
  f.setTime(30000); f.guard.activity(); f.fire(); await flush()
  assert.equal(f.tick(), 30)
  f.setTime(60000); f.guard.check(); assert.equal(f.expired(), 1)
})
