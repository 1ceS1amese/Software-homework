import { expect, test } from '@playwright/test'
import { api, login } from './helpers'

test('REL-09A 标准认证响应与大整数 ID、旧入口兼容和错误契约', async ({ page }) => {
  const response = await page.request.post('/api/v1/auth/login', { data: { username: 'student1', password: '123456' } })
  expect(response.status()).toBe(200)
  const body = await response.json()
  expect(body.traceId).toBe(response.headers()['x-trace-id'])
  expect(body.data.tokenType).toBe('Bearer')
  expect(body.data.expiresIn).toBe(7200)
  expect(body.data.user).toMatchObject({ id: '3', role: 'STUDENT', deptName: expect.any(String), majorName: expect.any(String) })
  const headers = { Authorization: `Bearer ${body.data.accessToken}` }
  const claims = JSON.parse(Buffer.from(body.data.accessToken.split('.')[1], 'base64url').toString())
  expect(claims.sub).toBe('3'); expect(claims.majorId).toBeTruthy(); expect(claims.jti).toBeTruthy()
  expect((await page.request.get('/api/v1/auth/me', { headers })).status()).toBe(200)
  expect((await page.request.get('/api/v1/users', { headers })).status()).toBe(403)
  expect((await page.request.get('/api/v1/enrollment/reconcile', { headers })).status()).toBe(403)
  const health = await page.request.get('/api/v1/actuator/health')
  expect((await health.json()).data.status).toBe('UP')
  for (const [path, status] of [['/courses/bad', 400], ['/not-a-route', 404]] as const) {
    const r = await page.request.get('/api/v1' + path, { headers }); expect(r.status()).toBe(status)
    expect((await r.json()).traceId).toBeTruthy()
  }
  const malformed = await page.request.post('/api/v1/auth/login', { data: '{broken', headers: { 'Content-Type': 'application/json' } })
  expect(malformed.status()).toBe(400)
  expect((await malformed.json()).code).toBe(10000)
  expect((await page.request.get('/api/v1/auth/me', { headers: { Authorization: 'Bearer invalid' } })).status()).toBe(401)
  await login(page, 'admin')
  expect(typeof await page.evaluate(() => localStorage.getItem('csms_token'))).toBe('string')
  expect((await api(page, '/enrollment/reconcile')).body.data).toEqual([])
  const token = await page.evaluate(() => localStorage.getItem('csms_token'))
  const depts = await page.request.get('/api/v1/depts', { headers: { Authorization: `Bearer ${token}` } })
  expect(typeof (await depts.json()).data[0].id).toBe('string')
})
