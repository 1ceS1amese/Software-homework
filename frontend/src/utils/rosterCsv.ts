import type { EnrollmentItem } from '@/api/types'

export function csvCell(value: unknown): string {
  let text = String(value ?? '')
  // 电子表格应将用户提供的公式前缀视为文本。
  if (/^\s*[=+\-@]/u.test(text)) text = "'" + text
  return '"' + text.replaceAll('"', '') + '"'
}

export function buildRosterCsv(students: EnrollmentItem[]): string {
  const rows: unknown[][] = [
    ['学号', '姓名', '教学班', '选课时间', '选课途径'],
    ...students.map(student => [
      student.studentUsername || student.studentId,
      student.studentName, student.className, student.enrolledAt,
      student.source === 'PORTAL' ? '自主选课' : '管理代选',
    ]),
  ]
  return '\uFEFF' + rows.map(row => row.map(csvCell).join(',')).join('\r\n') + '\r\n'
}
