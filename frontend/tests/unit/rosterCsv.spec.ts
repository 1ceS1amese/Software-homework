import { describe, expect, it } from 'vitest'
import { buildRosterCsv, csvCell } from '../../src/utils/rosterCsv'
import type { EnrollmentItem } from '../../src/api/types'

const student: EnrollmentItem = {
  id: 1, studentId: 10, studentUsername: '00010', studentName: '李"华',
  teachingClassId: 1, className: '数据结构,01班', courseId: 1,
  courseName: '数据结构', termId: 1, credit: 4, status: 'ENROLLED',
  enrolledAt: '2026-10-09', source: 'PORTAL',
}

describe('教师花名册 CSV', () => {
  it('姓名中的引号、逗号与换行不会破坏列结构', () => {
    expect(csvCell('李"华,同学')).toBe('"李""华,同学"')
    expect(csvCell('第一行\n第二行')).toBe('"第一行\n第二行"')
    expect(csvCell(undefined)).toBe('""')
  })

  it('可能被电子表格执行的内容按文本导出', () => {
    for (const value of ['=1+1', '+1', '-1', '@name', '  =1']) {
      expect(csvCell(value)).toBe('"' + "'" + value + '"')
    }
  })

  it('生成包含中文 BOM、原始学号与可读选课途径的花名册', () => {
    const csv = buildRosterCsv([student])
    expect(csv.startsWith('\uFEFF')).toBe(true)
    expect(csv).toContain('"00010","李""华","数据结构,01班","2026-10-09","自主选课"\r\n')
    expect(buildRosterCsv([{ ...student, source: 'ADMIN' }])).toContain('"管理代选"')
  })

  it('没有学生时仅输出表头', () => {
    expect(buildRosterCsv([])).toBe('\uFEFF"学号","姓名","教学班","选课时间","选课途径"\r\n')
  })
})
