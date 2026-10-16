import { expect, test } from '@playwright/test'

test('AC-06 匿名请求与外部来源被拒绝，正常来源和现有响应结构兼容', async ({ request }) => {
  const anonymous = await request.get('/api/enrollments/my', { headers: { 'X-Trace-Id': 'a'.repeat(128) } })
  expect(anonymous.status()).toBe(401)
  const anonymousBody = await anonymous.json()
  expect(anonymousBody.code).toBe(20006)
  expect(anonymousBody.message).toMatch(/[\u4e00-\u9fff]/)
  expect(anonymousBody.traceId).toHaveLength(32)
  expect(anonymousBody.traceId).toBe(anonymous.headers()['x-trace-id'])
  const rejected = await request.post('/api/auth/login', {
    headers: { Origin: 'https://untrusted.invalid' }, data: { username: 'admin', password: '123456' },
  })
  expect(rejected.status()).toBe(403)
  const accepted = await request.post('/api/auth/login', {
    headers: { Origin: process.env.ACCEPTANCE_BASE_URL!, 'X-Trace-Id': 'acceptance-compat-trace-001' }, data: { username: 'admin', password: '123456' },
  })
  expect(accepted.status()).toBe(200)
  const body = await accepted.json()
  expect(body.code).toBe(0)
  expect(typeof body.data).toBe('string')
  expect(body.traceId).toBeTruthy()
  expect(body.traceId).toBe('acceptance-compat-trace-001')
  expect(body.traceId).toBe(accepted.headers()['x-trace-id'])
})
