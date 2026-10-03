export type NoticeColor = 'success' | 'error' | 'warning' | 'info'
type Notice = { title: string; color: NoticeColor }

let handler: ((notice: Notice) => void) | null = null

export function setNoticeHandler(next: (notice: Notice) => void) {
  handler = next
}

export function notify(title: string, color: NoticeColor = 'info') {
  handler?.({ title, color })
}
