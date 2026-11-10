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
// 只在run-local-mysql.ps1创建的独立库，或本次专用临时容器运行。
const marker = 'round2-' + Date.now()
const results = []
function check(name, fn) { fn(); results.push({ name, passed: true }); console.log('PASS ' + name) }
async function api(path, token, method = 'GET', body) {
  const response = await fetch(base + '/api' + path, { method, headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: 'Bearer ' + token } : {}) }, body: body === undefined ? undefined : JSON.stringify(body) })
  return response.json()
}
async function login(username) {
  const r = await api('/auth/login', null, 'POST', { username, password: '123456' })
  assert.equal(r.code, 200, '隔离测试账号登录失败'); return r.data.token
}
const admin = await login('audit_admin')
const original = (await api('/score-items', admin)).data
const details = original.map((i, n) => ({ itemId: i.id, itemScore: n === 0 ? 0 : 80, weight: 999, itemName: '客户端伪造名称' }))
const payload = { fundId: 1, studentId: 1, sponsorId: 2, scoreDate: '2026-11-04', remark: marker, ruleVersion: 'fake', totalScore: 999, details }
const legacy = (await api('/scores/1', admin)).data
check('旧评分不伪造历史规则或权重', () => { assert.equal(legacy.ruleVersion, null); assert.ok(legacy.details.every(d => d.weight === null)); assert.equal(Number(legacy.totalScore), 86.25) })
for (const [name, change] of [['新评分必须关联资助单', { fundId: null }], ['不存在的资助单被拒绝', { fundId: 9999999 }], ['资助单须匹配学生', { fundId: 3 }], ['资助单须匹配资助人', { sponsorId: 9999999 }]]) {
  const r = await api('/scores', admin, 'POST', { ...payload, ...change })
  check(name, () => assert.equal(r.code, 400))
}
let r = await api('/scores', admin, 'POST', payload)
assert.equal(r.code, 200)
const id = Number(sql(`SELECT id FROM score WHERE remark=${quote(marker)} ORDER BY id DESC LIMIT 1;`))
let frozen = (await api('/scores/' + id, admin)).data
check('新评分保存真实规则快照及资助单', () => { assert.equal(frozen.fundId, 1); assert.equal(frozen.ruleVersion.length, 64); assert.notEqual(frozen.ruleVersion, 'fake'); assert.ok(frozen.ruleCapturedAt); assert.equal(frozen.details[0].itemName, original[0].name); assert.equal(Number(frozen.details[0].weight), Number(original[0].weight)) })
const frozenVersion = frozen.ruleVersion
const frozenTime = frozen.ruleCapturedAt
const expected = details.reduce((s, d) => s + d.itemScore * Number(original.find(i => i.id === d.itemId).weight) / 100, 0)
check('客户端假权重、假指标名和假总分不起作用', () => assert.equal(Number(frozen.totalScore), expected))
try {
  sql(`UPDATE score_item SET name='修改后的当前规则',weight=weight-1 WHERE id=${Number(original[0].id)}; UPDATE score_item SET weight=weight+1 WHERE id=${Number(original[1].id)};`)
  frozen = (await api('/scores/' + id, admin)).data
  check('改当前规则后历史详情和总分保持原解释', () => { assert.equal(frozen.ruleVersion, frozenVersion); assert.equal(frozen.details[0].itemName, original[0].name); assert.equal(Number(frozen.details[0].weight), Number(original[0].weight)); assert.equal(Number(frozen.totalScore), expected) })
  r = await api('/scores', admin, 'POST', { ...payload, remark: marker + '-new-rule' })
  assert.equal(r.code, 200)
  const newId = Number(sql(`SELECT id FROM score WHERE remark=${quote(marker + '-new-rule')} LIMIT 1;`))
  const newRuleScore = (await api('/scores/' + newId, admin)).data
  check('规则修改后的新评分生成不同规则标识', () => { assert.notEqual(newRuleScore.ruleVersion, frozenVersion); assert.equal(newRuleScore.details[0].itemName, '修改后的当前规则') })
  sql(`DELETE FROM score_item WHERE id=${Number(original[0].id)};`)
  r = await api('/scores/' + id, admin, 'PUT', payload)
  check('当前指标删除后历史评分仍按快照编辑', () => assert.equal(r.code, 200))
  frozen = (await api('/scores/' + id, admin)).data
  check('编辑不改写原规则标识和留存时间', () => { assert.equal(frozen.ruleVersion, frozenVersion); assert.equal(frozen.ruleCapturedAt, frozenTime); assert.equal(Number(frozen.totalScore), expected); assert.equal(frozen.details.length, original.length) })
} finally {
  for (const i of original) sql(`INSERT INTO score_item(id,name,weight,description) VALUES(${Number(i.id)},${quote(i.name)},${Number(i.weight)},${i.description == null ? 'NULL' : quote(i.description)}) ON DUPLICATE KEY UPDATE name=VALUES(name),weight=VALUES(weight),description=VALUES(description);`)
}
r = await api('/scores/' + id, admin, 'PUT', { ...payload, fundId: 3, studentId: 2 })
check('不能将历史评分改到另一名学生', () => assert.equal(r.code, 400))
try {
  sql(`UPDATE score SET rule_version='corrupt' WHERE id=${id};`)
  r = await api('/scores/' + id, admin, 'PUT', payload)
  check('快照摘要异常时拒绝重新计算', () => assert.equal(r.code, 409))
} finally { sql(`UPDATE score SET rule_version=${quote(frozenVersion)} WHERE id=${id};`) }
r = await api('/scores/1', admin, 'PUT', { fundId: 1, studentId: legacy.studentId, sponsorId: legacy.sponsorId, scoreDate: legacy.scoreDate, remark: marker + '-legacy' })
check('旧评分可补资助单关联和备注', () => assert.equal(r.code, 200))
const boundLegacy = (await api('/scores/1', admin)).data
check('补关联不修改旧总分和历史规则状态', () => { assert.equal(boundLegacy.fundId, 1); assert.equal(boundLegacy.ruleVersion, null); assert.equal(Number(boundLegacy.totalScore), Number(legacy.totalScore)); assert.deepEqual(boundLegacy.details, legacy.details) })
r = await api('/scores/1', admin, 'PUT', { ...payload, details: legacy.details.map((d, n) => ({ itemId: d.itemId, itemScore: n === 0 ? 1 : d.itemScore })) })
check('缺历史规则时不能修改旧分数', () => assert.equal(r.code, 400))
r = await api('/scores?fundId=1&page=1&size=100', admin)
check('评分可按资助单精确查询', () => { assert.equal(r.code, 200); assert.ok(r.data.records.some(s => s.id === id)); assert.ok(r.data.records.every(s => s.fundId === 1)) })
const otherName = marker + '-sponsor'
r = await api('/users', admin, 'POST', { username: otherName, password: '123456', realName: '独立测试资助人', role: 'SPONSOR' })
assert.equal(r.code, 200)
const otherSponsor = await login(otherName)
r = await api('/scores?fundId=1', otherSponsor)
check('其他资助人无法通过单据筛选读取评分', () => { assert.equal(r.code, 200); assert.equal(r.data.total, 0) })
r = await api('/scores/' + id, otherSponsor)
check('其他资助人直接请求评分详情也被拒绝', () => assert.equal(r.code, 403))
const editableFund = marker.replace(/[0-9]/g, d => String.fromCharCode(97 + Number(d))) + '-fund'
r = await api('/funds', admin, 'POST', { studentId: 1, sponsorId: 2, amount: 123, fundDate: '2026-11-04', purpose: '学习支持', remark: editableFund })
assert.equal(r.code, 200)
const fundId = Number(sql(`SELECT id FROM fund WHERE remark=${quote(editableFund)} LIMIT 1;`))
assert.ok(fundId)
r = await api('/scores', admin, 'POST', { ...payload, fundId, remark: marker + '-linked-pending' }); assert.equal(r.code, 200)
r = await api('/funds/' + fundId, admin, 'DELETE')
check('已关联评分的资助单不能删除', () => assert.equal(r.code, 400))
r = await api('/funds/' + fundId, admin, 'PUT', { studentId: 2, sponsorId: 2, amount: 456, purpose: '学习支持' })
check('已关联评分的资助单不能改写申请关系', () => assert.equal(r.code, 400))
const auditorName = marker + '-auditor'
r = await api('/users', admin, 'POST', { username: auditorName, password: '123456', realName: '只读测试', role: 'AUDITOR' }); assert.equal(r.code, 200)
const auditor = await login(auditorName)
for (const path of ['/audit-readonly/funds', '/audit-readonly/funds/1', '/audit-readonly/scores?fundId=1', '/audit-readonly/scores/' + id, '/audit-readonly/timestamps', '/audit-readonly/logs', '/audit-readonly/verify?fundId=1']) {
  r = await api(path, auditor)
  check('只读账号可查阅 ' + path.split('?')[0], () => { assert.equal(r.code, 200); const raw = JSON.stringify(r.data); assert.ok(!raw.includes('客户端伪造名称')); if (r.data.records) for (const row of r.data.records) { assert.equal(row.content, undefined); assert.equal(row.remark, undefined); assert.equal(row.phone, undefined); assert.equal(row.familyAddress, undefined) } })
}
const countsBefore = sql('SELECT (SELECT COUNT(*) FROM score),(SELECT COUNT(*) FROM fund),(SELECT COUNT(*) FROM sys_user);')
for (const [method, path] of [
  ['POST', '/funds'], ['PUT', '/funds/1'], ['DELETE', '/funds/1'],
  ['POST', '/scores'], ['PUT', '/scores/' + id], ['DELETE', '/scores/' + id],
  ['POST', '/score-items'], ['PUT', '/score-items/1'], ['DELETE', '/score-items/1'],
  ['POST', '/review-tasks/draw'], ['POST', '/review-tasks/1/finish'],
  ['POST', '/privacy/access/apply'], ['POST', '/privacy/access/1/approve'],
  ['POST', '/privacy/mask/createViewSession?targetId=1'], ['POST', '/privacy/mask/switchMask?isSecondMask=false'],
  ['POST', '/users'], ['PUT', '/users/1'], ['DELETE', '/users/1'],
  ['POST', '/visits'], ['POST', '/students']
]) {
  r = await api(path, auditor, method, payload)
  check('只读账号拒绝 ' + method + ' ' + path.split('?')[0], () => assert.equal(r.code, 403))
}
r = await api('/audit-readonly/funds', auditor, 'POST', payload)
check('只读查询接口不支持写方法', () => assert.equal(r.code, 405))
check('拒绝写请求后业务表数量不变', () => assert.equal(sql('SELECT (SELECT COUNT(*) FROM score),(SELECT COUNT(*) FROM fund),(SELECT COUNT(*) FROM sys_user);'), countsBefore))
for (const path of ['/privacy/profile/STU-a1b2c3d4e5f6', '/privacy/mask/getSensitiveDetail?sessionKey=fake&targetId=1', '/students/1/attachments', '/visits', '/users/options', '/operation-logs', '/funds', '/scores']) {
  r = await api(path, auditor)
  check('只读账号不能绕过专用接口 ' + path.split('?')[0], () => assert.equal(r.code, 403))
}
const demotedName = marker + '-demoted'
r = await api('/users', admin, 'POST', { username: demotedName, password: '123456', realName: '角色变更测试', role: 'AUDIT_ADMIN' }); assert.equal(r.code, 200)
const oldAdminToken = await login(demotedName)
const demotedId = Number(sql(`SELECT id FROM sys_user WHERE username=${quote(demotedName)};`))
r = await api('/users/' + demotedId, admin, 'PUT', { role: 'AUDITOR' }); assert.equal(r.code, 200)
r = await api('/funds', oldAdminToken, 'POST', payload)
check('角色改为只读后旧管理员JWT立即失去写权限', () => assert.equal(r.code, 403))
r = await api('/audit-readonly/funds', oldAdminToken)
check('旧JWT的后续请求按数据库当前角色授权', () => assert.equal(r.code, 200))
r = await api('/users/' + demotedId, admin, 'DELETE'); assert.equal(r.code, 200)
r = await api('/audit-readonly/funds', oldAdminToken)
check('删除账号后旧JWT失效', () => assert.equal(r.code, 401))
r = await api('/privacy/access/todo', await login('supervisor'))
check('原监事审批入口保持可用', () => assert.equal(r.code, 200))
writeFileSync(process.env.QUICKFIX_RESULTS || 'round2-results.json', JSON.stringify({ passed: results.length, failed: 0, database: 'isolated MySQL8', checks: results }, null, 2))
console.log(`完成：${results.length}项新增功能MySQL/API检查通过。`)
