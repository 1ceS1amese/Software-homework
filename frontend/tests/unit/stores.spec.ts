import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useEnrollStore } from '../../src/stores/enroll'
import { useTermStore } from '../../src/stores/term'
import { getMyEnrollments } from '../../src/api/enrollments'
import { getTerms } from '../../src/api/base'

vi.mock('../../src/api/enrollments', () => ({
  getMyEnrollments: vi.fn(),
  enrollClass: vi.fn(),
  withdrawClass: vi.fn(),
}))
vi.mock('../../src/api/base', () => ({ getTerms: vi.fn() }))

beforeEach(() => {
  setActivePinia(createPinia())
  vi.resetAllMocks()
})

describe('选课状态', () => {
  it('接口失败时清除旧的已选课程且向页面抛错', async () => {
    vi.mocked(getMyEnrollments).mockResolvedValueOnce([{
      id: 1, studentId: 1, teachingClassId: 7, className: '01',
      courseId: 2, courseName: '课程', termId: 1, credit: 3,
      status: 'ENROLLED', enrolledAt: '2026-09-01', source: 'PORTAL',
    }]).mockRejectedValueOnce(new Error('offline'))
    const store = useEnrollStore()
    await store.fetchMine(1)
    expect(store.enrolledClassIds.has(7)).toBe(true)
    await expect(store.fetchMine(1)).rejects.toThrow('offline')
    expect(store.enrolledClassIds.size).toBe(0)
    expect(store.totalCredits).toBe(0)
  })
})

describe('学期状态', () => {
  it('学期接口失败时不允许选课，也不显示假学期', async () => {
    vi.mocked(getTerms).mockRejectedValueOnce(new Error('offline'))
    const store = useTermStore()
    await store.fetchTerms()
    expect(store.terms).toEqual([])
    expect(store.currentTermId).toBeNull()
    expect(store.isEnrollingNow).toBe(false)
    expect(store.isWithdrawAllowedNow).toBe(false)
  })

  it('即使学期标记为选课中，超过选课结束时间也不能继续选课，但在退课截止前仍可退课', async () => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-10-03T12:00:00+08:00'))
    vi.mocked(getTerms).mockResolvedValueOnce([{
      id: 1, code: '2026-1', name: '测试学期',
      startDate: '2026-09-01', endDate: '2027-01-01',
      enrollStart: '2026-09-01 08:00:00', enrollEnd: '2026-10-02 18:00:00',
      withdrawEnd: '2026-10-05 18:00:00', status: 'ENROLLING',
    }])
    try {
      const store = useTermStore()
      await store.fetchTerms()
      expect(store.isEnrollingNow).toBe(false)
      expect(store.isWithdrawAllowedNow).toBe(true)
    } finally { vi.useRealTimers() }
  })
})
