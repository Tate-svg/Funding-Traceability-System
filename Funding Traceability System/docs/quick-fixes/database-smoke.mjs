// 仅用于独立测试数据库。需要Node18+和mysql客户端，或本次临时Docker容器。
// 示例：QUICKFIX_BASE_URL=http://127.0.0.1:18090 QUICKFIX_DB=funding_test node docs/quick-fixes/database-smoke.mjs
import assert from 'node:assert/strict'
import { execFileSync } from 'node:child_process'
import { writeFileSync } from 'node:fs'
const base = process.env.QUICKFIX_BASE_URL || 'http://127.0.0.1:8090'
if (!['127.0.0.1', 'localhost'].includes(new URL(base).hostname)) throw new Error('仅允许本机隔离测试服务')
const db = process.env.QUICKFIX_DB || 'funding_test'
if (!/^funding_(test|quickfix_[a-z0-9_]+)$/.test(db)) throw new Error('数据库必须是独立测试库')
const container = process.env.QUICKFIX_DOCKER_CONTAINER
if (container && !/^funding-quickfix-mysql-[a-z0-9]+$/.test(container)) throw new Error('仅允许专用临时容器')
const quote = value => "'" + String(value).replaceAll("'", "''") + "'"
function sql(query) {
  // 只修改本次客户端连接的时区，与测试后端一致，不修改MySQL服务器全局设置。
  query = "SET time_zone = '+08:00';\n" + query
  try {
    if (container) {
      const env = { ...process.env }
      for (const name of ['DOCKER_HOST', 'DOCKER_CONTEXT', 'DOCKER_TLS', 'DOCKER_TLS_VERIFY', 'DOCKER_CERT_PATH']) delete env[name]
      return execFileSync('docker', ['--host=unix:///var/run/docker.sock', 'exec', '-i', container, 'sh', '-c',
        'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysql -uroot --default-character-set=utf8mb4 --batch --skip-column-names ' + db], { input: query, encoding: 'utf8', env, stdio: ['pipe', 'pipe', 'pipe'] }).trim()
    }
    return execFileSync(process.env.QUICKFIX_MYSQL_EXE || 'mysql', ['--host=' + (process.env.QUICKFIX_DB_HOST || '127.0.0.1'), '--port=' + (process.env.QUICKFIX_DB_PORT || '3306'), '--user=' + (process.env.DB_USERNAME || 'root'), '--default-character-set=utf8mb4', '--batch', '--skip-column-names', db],
      { input: query, encoding: 'utf8', env: { ...process.env, MYSQL_PWD: process.env.DB_PASSWORD || '' }, stdio: ['pipe', 'pipe', 'pipe'] }).trim()
  } catch { throw new Error('独立测试库SQL执行失败；未输出SQL、凭据或会话key') }
}
const marker = 'quickfix-' + Date.now()
const results = []
function check(name, fn) { fn(); results.push({ name, passed: true }); console.log('PASS ' + name) }
async function api(path, token, method = 'GET', body) {
  const response = await fetch(base + '/api' + path, { method, headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: 'Bearer ' + token } : {}) }, body: body === undefined ? undefined : JSON.stringify(body) })
  return response.json()
}
async function login(username) {
  const r = await api('/auth/login', null, 'POST', { username, password: '123456' })
  assert.equal(r.code, 200, '测试账号登录失败'); return r.data.token
}
const admin = await login('audit_admin'); const sponsor = await login('sponsor'); const supervisor = await login('supervisor')
const count = () => Number(sql('SELECT COUNT(*) FROM score;'))
const initial = count()
const items = (await api('/score-items', admin)).data
const details = items.map((i, n) => ({ itemId: i.id, itemScore: n === 0 ? 0 : 80 }))
const payload = { fundId: 1, studentId: 1, sponsorId: 2, scoreDate: '2026-11-04', remark: marker, totalScore: 999, details }
let r = await api('/scores', admin, 'POST', { ...payload, details: details.slice(1) })
check('漏项返回400且没有新增评分', () => { assert.equal(r.code, 400); assert.equal(count(), initial) })
r = await api('/scores', admin, 'POST', { ...payload, details: details.map((i, n) => n === 0 ? { ...i, itemScore: null } : i) })
check('空分返回400且没有新增评分', () => { assert.equal(r.code, 400); assert.equal(count(), initial) })
r = await api('/scores', admin, 'POST', { ...payload, details: details.map((i, n) => n === 1 ? { ...i, itemId: details[0].itemId } : i) })
check('重复指标返回400', () => assert.equal(r.code, 400))
r = await api('/scores', admin, 'POST', { ...payload, details: details.map((i, n) => n === 0 ? { ...i, itemScore: 101 } : i) })
check('越界得分返回400', () => assert.equal(r.code, 400))
const originalWeight = items[0].weight
try {
  sql(`UPDATE score_item SET weight=weight-1 WHERE id=${Number(items[0].id)};`)
  r = await api('/scores', admin, 'POST', payload)
  check('权重合计不为100时拒绝评分', () => { assert.equal(r.code, 400); assert.equal(count(), initial) })
} finally { sql(`UPDATE score_item SET weight=${Number(originalWeight)} WHERE id=${Number(items[0].id)};`) }
r = await api('/scores', admin, 'POST', payload)
check('真实0分可保存，忽略客户端999总分', () => { assert.equal(r.code, 200); assert.equal(count(), initial + 1) })
const id = Number(sql(`SELECT id FROM score WHERE remark=${quote(marker)} ORDER BY id DESC LIMIT 1;`))
r = await api('/scores/' + id, admin)
const expected = details.reduce((s, d) => s + d.itemScore * Number(items.find(i => i.id === d.itemId).weight) / 100, 0)
check('数据库回读总分和全部明细正确', () => { assert.equal(r.code, 200); assert.equal(Number(r.data.totalScore), expected); assert.equal(r.data.details.length, details.length) })
r = await api('/scores/' + id, admin, 'PUT', { ...payload, details: details.slice(1) })
check('编辑漏项被拒绝，已有总分不变', () => { assert.equal(r.code, 400); assert.equal(Number(sql(`SELECT total_score FROM score WHERE id=${id};`)), expected) })
async function createApply(suffix) {
  const reason = marker + suffix
  const a = await api('/privacy/access/apply', sponsor, 'POST', { virtualId: 'STU-a1b2c3d4e5f6', fieldScope: 'phone', reason })
  assert.equal(a.code, 200)
  const aId = Number(sql(`SELECT id FROM privacy_access_apply WHERE reason=${quote(reason)} ORDER BY id DESC LIMIT 1;`))
  assert.ok(aId > 0); return aId
}
const target = await createApply('-apply'); const otherTarget = await createApply('-other')
async function session() {
  const a = await api('/privacy/mask/createViewSession?targetId=' + target, sponsor, 'POST')
  assert.equal(a.code, 200); return a.data.sessionKey
}
const path = (action, key, t = target) => '/privacy/mask/' + action + '?sessionKey=' + encodeURIComponent(key) + '&targetId=' + t
let key = await session()
r = await api(path('getSensitiveDetail', key), sponsor)
check('真实数据库中待审批资料保持脱敏', () => { assert.equal(r.code, 200); assert.equal(r.data.masked, true); assert.equal(r.data.approved, false) })
const expiry = sql(`SELECT expire_time FROM sensitive_view_session WHERE session_id=${quote(key)};`)
await api(path('getSensitiveDetail', key), sponsor)
check('读取详情不会自动续期', () => assert.equal(sql(`SELECT expire_time FROM sensitive_view_session WHERE session_id=${quote(key)};`), expiry))
r = await api(path('getSensitiveDetail', key), admin)
check('有审批权限的其他用户也不能借用此会话', () => assert.equal(r.code, 403))
r = await api(path('getSensitiveDetail', key, otherTarget), sponsor)
check('同一用户不能跨申请单借用会话', () => assert.equal(r.code, 403))
r = await api(path('closeSession', key), admin, 'POST')
check('他人关闭被拒绝且本人会话仍有效', () => { assert.equal(r.code, 403); assert.equal(Number(sql(`SELECT is_valid FROM sensitive_view_session WHERE session_id=${quote(key)};`)), 1) })
sql(`UPDATE sensitive_view_session SET expire_time=DATE_ADD(NOW(),INTERVAL 10 SECOND) WHERE session_id=${quote(key)};`)
r = await api(path('touchSession', key), sponsor, 'POST')
check('活动上报真实更新数据库空闲期限', () => { assert.equal(r.code, 200); assert.ok(r.data.remainingSeconds >= 850); assert.ok(Number(sql(`SELECT TIMESTAMPDIFF(SECOND,NOW(),expire_time) FROM sensitive_view_session WHERE session_id=${quote(key)};`)) >= 850) })
r = await api(path('closeSession', key), sponsor, 'POST')
check('本人关闭成功并持久化失效标记', () => { assert.equal(r.code, 200); assert.equal(Number(sql(`SELECT is_valid FROM sensitive_view_session WHERE session_id=${quote(key)};`)), 0) })
r = await api(path('touchSession', key), sponsor, 'POST')
check('关闭后不能续期复活', () => assert.equal(r.code, 410))
key = await session()
sql(`UPDATE sensitive_view_session SET expire_time=DATE_SUB(NOW(),INTERVAL 1 MINUTE) WHERE session_id=${quote(key)};`)
r = await api(path('touchSession', key), sponsor, 'POST')
check('过期后不能续期且持久化失效', () => { assert.equal(r.code, 410); assert.equal(Number(sql(`SELECT is_valid FROM sensitive_view_session WHERE session_id=${quote(key)};`)), 0) })
key = await session()
sql(`UPDATE sensitive_view_session SET start_time=DATE_SUB(DATE_SUB(NOW(),INTERVAL 119 MINUTE),INTERVAL 50 SECOND),expire_time=DATE_ADD(NOW(),INTERVAL 5 MINUTE) WHERE session_id=${quote(key)};`)
r = await api(path('touchSession', key), sponsor, 'POST')
check('最长2小时截止在数据库和接口中生效', () => { assert.equal(r.code, 200); assert.ok(r.data.remainingSeconds >= 0 && r.data.remainingSeconds <= 10); assert.ok(Number(sql(`SELECT TIMESTAMPDIFF(SECOND,NOW(),expire_time) FROM sensitive_view_session WHERE session_id=${quote(key)};`)) <= 10) })
await api('/privacy/access/' + target + '/approve', admin, 'POST', { result: 'APPROVE', remark: marker })
r = await api('/privacy/access/' + target + '/approve', supervisor, 'POST', { result: 'APPROVE', remark: marker })
check('测试申请完成真实两级审批', () => assert.equal(r.code, 200))
key = await session()
sql(`UPDATE privacy_access_apply SET expire_time=DATE_ADD(NOW(),INTERVAL 10 SECOND) WHERE id=${target};`)
r = await api(path('touchSession', key), sponsor, 'POST')
check('审批授权截止优先于空闲续期', () => { assert.equal(r.code, 200); assert.ok(r.data.remainingSeconds >= 0 && r.data.remainingSeconds <= 10); assert.ok(r.data.absoluteSecondsRemaining <= 10) })
sql(`UPDATE privacy_access_apply SET expire_time=DATE_SUB(NOW(),INTERVAL 1 MINUTE) WHERE id=${target};`)
r = await api(path('touchSession', key), sponsor, 'POST')
check('授权过期后拒绝继续续期', () => assert.equal(r.code, 410))
const output = process.env.QUICKFIX_RESULTS || 'quickfix-database-results.json'
writeFileSync(output, JSON.stringify({ passed: results.length, failed: 0, database: 'isolated MySQL8', marker, checks: results }, null, 2))
console.log(`完成：${results.length}项真实MySQL/API检查通过。未进行性能测试或完整浏览器回归。`)
