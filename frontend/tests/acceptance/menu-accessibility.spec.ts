import { expect, test } from '@playwright/test'
import { login } from './helpers'

test('AC-06 手机侧栏收起后隐藏导航和键盘控件，展开与选中后状态正确', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 900 })
  await login(page, 'teacher1')
  const navigation = page.getByRole('navigation', { name: '主导航' })
  const opener = page.getByRole('button', { name: '打开菜单' })
  await expect(navigation).not.toBeVisible()
  await expect(opener).toHaveAttribute('aria-expanded', 'false')
  await opener.click()
  await expect(navigation).toBeVisible()
  await expect(opener).toHaveAttribute('aria-expanded', 'true')
  await navigation.getByRole('link', { name: '我的教学班', exact: true }).click()
  await expect(navigation).not.toBeVisible()
  await expect(opener).toHaveAttribute('aria-expanded', 'false')
})
