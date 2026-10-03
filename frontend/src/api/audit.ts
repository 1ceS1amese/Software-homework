import { request } from './request'
import type { AuditLogItem, LoginLogItem, PageResult } from './types'

// L-01: 审计日志查询
export function getAuditLogs(params?: {
  userId?: number
  username?: string
  module?: string
  action?: string
  result?: string
  startTime?: string
  endTime?: string
  page?: number
  size?: number
}) {
  return request<PageResult<AuditLogItem>>({
    url: '/audit-logs',
    method: 'GET',
    params,
  })
}

// L-02: 登录日志查询
export function getLoginLogs(params?: {
  username?: string
  result?: string
  startTime?: string
  endTime?: string
  page?: number
  size?: number
}) {
  return request<PageResult<LoginLogItem>>({
    url: '/login-logs',
    method: 'GET',
    params,
  })
}

// L-03: 审计日志详情
export function getAuditLogDetail(id: number) {
  return request<AuditLogItem>({
    url: `/audit-logs/${id}`,
    method: 'GET',
  })
}
