import type { App, DirectiveBinding } from 'vue'
import { useAuthStore } from '@/stores/auth'

export const permDirective = {
  mounted(el: HTMLElement, binding: DirectiveBinding<string | string[]>) {
    const authStore = useAuthStore()
    const { value } = binding

    if (!value) return

    const hasPermission = Array.isArray(value)
      ? value.some(code => authStore.hasPerm(code))
      : authStore.hasPerm(value)

    if (!hasPermission) {
      el.parentNode?.removeChild(el)
    }
  },
}

export function setupDirectives(app: App) {
  app.directive('perm', permDirective)
}
