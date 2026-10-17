import { expect, test } from '@playwright/test'
import { api, database, login } from './helpers'

test('REL-02 学期页面新增编辑与顺序状态迁移，非法日期和跳转不落库', async ({ page }, info) => {
  await login(page, 'admin'); await page.goto('/admin/terms')
  const code = `TERM-${info.project.name}-${Date.now() % 1000000}`
  const form = { code, name: '新学期验收', startDate: '2030-09-01', endDate: '2031-01-31', enrollStart: '2030-08-01T08:00', enrollEnd: '2030-08-31T18:00', withdrawEnd: '2030-09-15T18:00' }
  await page.getByRole('button', { name: '新增学期' }).click()
  const dialog = page.getByRole('dialog')
  for (const [key, label] of Object.entries({ code: '学期代码', name: '学期名称', startDate: '开始日期', endDate: '结束日期', enrollStart: '选课开始', enrollEnd: '选课结束', withdrawEnd: '退课截止' }))
    await dialog.getByLabel(label, { exact: true }).fill(form[key as keyof typeof form])
  await dialog.getByRole('button', { name: '保存', exact: true }).click(); await expect(dialog).not.toBeVisible()
  const id = Number(database(`SELECT id FROM term WHERE code='${code}'`))
  const row = page.getByRole('row').filter({ hasText: code })
  await row.getByRole('button', { name: '编辑', exact: true }).click()
  await dialog.getByLabel('学期名称', { exact: true }).fill('已修改学期')
  await dialog.getByRole('button', { name: '保存', exact: true }).click(); await expect(dialog).not.toBeVisible()
  expect((await api(page, '/terms', 'POST', form)).body.code).toBe(30301)
  for (const bad of [{ startDate: form.endDate }, { enrollEnd: '2030-07-01T08:00' }, { withdrawEnd: '2032-01-01T08:00' }, { status: 'CLOSED' }])
    expect((await api(page, `/terms/${id}`, 'PUT', { ...form, ...bad })).body.code).not.toBe(0)
  expect(database(`SELECT name FROM term WHERE id=${id}`)).toBe('已修改学期')
  expect((await api(page, `/terms/${id}/status`, 'PUT', { status: 'CLOSED' })).body.code).toBe(40103)
  for (const label of ['开放选课', '开始授课', '结束学期']) {
    await row.getByRole('button', { name: label, exact: true }).click()
    await dialog.getByRole('button', { name: '确认变更', exact: true }).click(); await expect(dialog).not.toBeVisible()
  }
  await page.reload(); await expect(row).toContainText('已修改学期')
  expect(database(`SELECT status FROM term WHERE id=${id}`)).toBe('CLOSED')
  expect((await api(page, `/terms/${id}/status`, 'PUT', { status: 'ENROLLING' })).body.code).toBe(40103)
  expect(database(`SELECT COUNT(*) FROM sys_audit_log WHERE target_type='term' AND target_id=${id} AND action='TERM_STATUS'`)).toBe('3')
})
