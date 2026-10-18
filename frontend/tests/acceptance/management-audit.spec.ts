import { expect, test } from '@playwright/test'
import { api, database, login } from './helpers'

test('REL-08 登录失败持久化与锁定、操作审计、检索详情和敏感字段排除', async ({ page }, info) => {
  await login(page, 'admin')
  const username = `audit-${info.project.name}-${Date.now() % 1000000}`
  const created = await api(page, '/users', 'POST', { username, realName: '审计验收用户' })
  expect(created.status).toBe(200)
  const id = created.body.data.id
  const password = created.body.data.initialPassword
  const attempt = (pw: string) => page.request.post('/api/auth/login', { data: { username, password: pw } })
  for (let i = 1; i <= 5; i++) {
    expect((await attempt('WrongPassword9')).status()).toBe(401)
    expect(database(`SELECT fail_count FROM sys_user WHERE id=${id}`)).toBe(String(i))
  }
  expect((await (await attempt(password)).json()).code).toBe(20002)
  expect(database(`SELECT COUNT(*) FROM sys_login_log WHERE username='${username}' AND result='BAD_PASSWORD'`)).toBe('5')
  expect(database(`SELECT COUNT(*) FROM sys_login_log WHERE username='${username}' AND result='LOCKED'`)).toBe('1')
  // 验证锁定过期后从新一轮计数开始，只修改本次新建的隔离验收账户。
  database(`UPDATE sys_user SET lock_until=DATE_SUB(NOW(),INTERVAL 1 SECOND) WHERE id=${id}`)
  expect((await attempt('WrongPassword9')).status()).toBe(401)
  expect(database(`SELECT CONCAT(fail_count,'|',lock_until IS NULL OR lock_until<NOW()) FROM sys_user WHERE id=${id}`)).toBe('1|1')
  expect((await attempt('WrongPassword9')).status()).toBe(401)
  expect(database(`SELECT fail_count FROM sys_user WHERE id=${id}`)).toBe('2')
  const reset = await api(page, `/users/${id}/password`, 'PUT')
  expect(reset.status).toBe(200)
  const signed = await attempt(reset.body.data.initialPassword)
  expect(signed.status()).toBe(200)
  const token = (await signed.json()).data
  const headers = { Authorization: `Bearer ${token}` }
  const denied = await page.request.post('/api/depts', { headers, data: { code: 'DENIED-AUDIT', name: '越权' } })
  expect(denied.status()).toBe(403)
  const trace = denied.headers()['x-trace-id']
  await expect.poll(() => database(`SELECT COUNT(*) FROM sys_audit_log WHERE trace_id='${trace}' AND result='FAIL'`)).toBe('1')
  const read = await page.request.get('/api/auth/me', { headers })
  expect(read.status()).toBe(200)
  await expect.poll(() => database(`SELECT COUNT(*) FROM sys_audit_log WHERE trace_id='${read.headers()['x-trace-id']}' AND action='HTTP_GET'`)).toBe('1')
  expect((await page.request.post('/api/auth/logout', { headers })).status()).toBe(200)
  expect(database(`SELECT COUNT(*) FROM sys_audit_log WHERE user_id=${id} AND action='LOGOUT'`)).toBe('1')
  const newPassword = 'AuditChanged938!'
  expect((await page.request.put('/api/auth/password', { headers, data: { oldPassword: reset.body.data.initialPassword, newPassword: 'short' } })).status()).toBe(400)
  expect((await page.request.put('/api/auth/password', { headers, data: { oldPassword: reset.body.data.initialPassword, newPassword } })).status()).toBe(200)
  expect((await page.request.get('/api/auth/me', { headers })).status()).toBe(401)
  expect((await attempt(newPassword)).status()).toBe(200)
  const json = database(`SELECT CONCAT(COALESCE(before_json,''),COALESCE(after_json,'')) FROM sys_audit_log WHERE user_id=${id} OR target_type='sys_user' AND target_id=${id}`)
  for (const secret of [password, reset.body.data.initialPassword, newPassword, token, '$2a$', '$2b$']) expect(json).not.toContain(secret)
  expect(database(`SELECT COUNT(*) FROM sys_audit_log WHERE user_id=${id} AND action='CHANGE_PASSWORD' AND result='SUCCESS'`)).toBe('1')
  expect((await api(page, `/login-logs?username=${username}&result=FAIL`)).body.data.total).toBe(8)
  expect((await api(page, '/login-logs?startTime=bad')).status).toBe(400)
  expect((await api(page, '/audit-logs?startTime=2030-01-01T00:00&endTime=2020-01-01T00:00')).status).toBe(400)
  const filtered = await api(page, `/audit-logs?targetType=sys_user&targetId=${id}&action=CHANGE_PASSWORD`)
  expect(filtered.body.data.total).toBe(1)
  expect((await api(page, `/audit-logs/${filtered.body.data.records[0].id}`)).body.data.action).toBe('CHANGE_PASSWORD')
  await page.goto('/admin/login-logs')
  await page.getByPlaceholder('输入用户名').fill(username)
  await page.getByRole('button', { name: '查询', exact: true }).click()
  await expect(page.getByRole('row').filter({ hasText: 'BAD_PASSWORD' }).first()).toBeVisible()
  await page.goto('/admin/audit-logs')
  await page.getByPlaceholder('用户名', { exact: true }).fill(username)
  await page.getByPlaceholder('例如 LOGIN').fill('CHANGE_PASSWORD')
  await page.getByRole('button', { name: '查询', exact: true }).click()
  const detailResponse = page.waitForResponse(r => /\/audit-logs\/\d+$/.test(r.url()))
  await page.getByRole('button', { name: '详情', exact: true }).first().click()
  expect((await detailResponse).status()).toBe(200)
  await expect(page.getByRole('dialog')).toContainText('credentialsChanged')
  await expect(page.getByRole('dialog')).toContainText('追踪号')
  expect((await page.request.get('/api/audit-logs')).status()).toBe(401)
  expect((await api(page, `/users/${id}`, 'DELETE')).status).toBe(200)
})

test('REL-08 认证动态配置校验与新令牌有效期', async ({ page }, info) => {
  await login(page, 'admin')
  const settings = ['auth.max_fail_count', 'auth.lock_minutes', 'auth.jwt_expire_seconds']
  const previous = (await api(page, '/configs')).body.data.filter((c: any) => settings.includes(c.configKey))
  const username = `policy-${info.project.name}-${Date.now() % 1000000}`
  const created = await api(page, '/users', 'POST', { username, realName: '认证配置验收' })
  const id = created.body.data.id
  try {
    for (const [key, value] of [['auth.max_fail_count', '2'], ['auth.lock_minutes', '1'], ['auth.jwt_expire_seconds', '600']])
      expect((await api(page, '/configs/' + key, 'PUT', { configValue: value })).status).toBe(200)
    for (const value of ['0', '1.5', '90000']) expect((await api(page, '/configs/auth.jwt_expire_seconds', 'PUT', { configValue: value })).status).toBe(400)
    const r = await page.request.post('/api/v1/auth/login', { data: { username, password: created.body.data.initialPassword } })
    const data = (await r.json()).data
    expect(data.expiresIn).toBe(600)
    const claims = JSON.parse(Buffer.from(data.accessToken.split('.')[1], 'base64url').toString())
    expect(claims.exp - claims.iat).toBe(600)
    for (let i = 0; i < 2; i++) expect((await page.request.post('/api/auth/login', { data: { username, password: 'WrongPolicy123' } })).status()).toBe(401)
    expect(database(`SELECT CONCAT(fail_count,'|',TIMESTAMPDIFF(SECOND,NOW(),lock_until) BETWEEN 50 AND 60) FROM sys_user WHERE id=${id}`)).toBe('2|1')
  } finally {
    for (const c of previous) await api(page, '/configs/' + c.configKey, 'PUT', { configValue: c.configValue })
    await api(page, `/users/${id}`, 'DELETE')
  }
})
