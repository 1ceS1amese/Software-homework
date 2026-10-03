// Enums as const matching docs/02 §7 & docs/04 §8.3
export const USER_TYPES = {
  STUDENT: 'STUDENT',
  TEACHER: 'TEACHER',
  ADMIN: 'ADMIN',
} as const
export type UserType = (typeof USER_TYPES)[keyof typeof USER_TYPES]

export const USER_STATUSES = {
  ACTIVE: 'ACTIVE',
  DISABLED: 'DISABLED',
} as const
export type UserStatus = (typeof USER_STATUSES)[keyof typeof USER_STATUSES]

export const CLASS_STATUSES = {
  DRAFT: 'DRAFT',
  PUBLISHED: 'PUBLISHED',
  CLOSED: 'CLOSED',
  CANCELLED: 'CANCELLED',
} as const
export type TeachingClassStatus = (typeof CLASS_STATUSES)[keyof typeof CLASS_STATUSES]

export const TERM_STATUSES = {
  PLANNED: 'PLANNED',
  ENROLLING: 'ENROLLING',
  RUNNING: 'RUNNING',
  CLOSED: 'CLOSED',
} as const
export type TermStatus = (typeof TERM_STATUSES)[keyof typeof TERM_STATUSES]

export const ENROLL_STATUSES = {
  ENROLLED: 'ENROLLED',
  WITHDRAWN: 'WITHDRAWN',
  COMPLETED: 'COMPLETED',
} as const
export type EnrollmentStatus = (typeof ENROLL_STATUSES)[keyof typeof ENROLL_STATUSES]

export const GRADE_STATUSES = {
  DRAFT: 'DRAFT',
  PUBLISHED: 'PUBLISHED',
} as const
export type GradeStatus = (typeof GRADE_STATUSES)[keyof typeof GRADE_STATUSES]

export const COURSE_TYPES = {
  REQUIRED: 'REQUIRED',
  ELECTIVE: 'ELECTIVE',
  RESTRICTED: 'RESTRICTED',
} as const
export type CourseType = (typeof COURSE_TYPES)[keyof typeof COURSE_TYPES]

// Generic Envelope
export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
  traceId?: string
}

export interface PageQuery {
  page?: number
  size?: number
  sortBy?: string
  sortOrder?: 'ASC' | 'DESC'
}

export interface PageResult<T> {
  records: T[]
  total: number
  page?: number
  size?: number
  pages?: number
}

// User & Profile
export interface UserProfile {
  id: number
  username: string
  realName: string
  userType: UserType
  deptId?: number
  deptName?: string
  majorId?: number
  majorName?: string
  gender?: 'MALE' | 'FEMALE' | 'UNKNOWN'
  phone?: string
  email?: string
  status: UserStatus
  mustChangePwd?: boolean
  roles?: string[]
  permissions?: string[]
}

export interface UserItem extends UserProfile {
  failCount?: number
  lockUntil?: string
  lastLoginAt?: string
  createdAt?: string
}

export interface LoginPayload {
  username: string
  password: string
}

export interface PasswordChangePayload {
  oldPassword?: string
  newPassword: string
  confirmPassword?: string
}

// Base Data
export interface DeptItem {
  id: number
  code: string
  name: string
  leader?: string
  status: 'ACTIVE' | 'DISABLED'
  sortNo: number
}

export interface MajorItem {
  id: number
  deptId: number
  deptName?: string
  code: string
  name: string
  degreeYears: number
  status: 'ACTIVE' | 'DISABLED'
}

export interface TermItem {
  id: number
  code: string
  name: string
  startDate: string
  endDate: string
  enrollStart: string
  enrollEnd: string
  withdrawEnd: string
  status: TermStatus
}

export interface CourseItem {
  id: number
  courseCode: string
  name: string
  credit: number
  creditHours: number
  courseType: CourseType
  deptId: number
  deptName?: string
  majorId?: number
  majorName?: string
  description?: string
  status?: 'ACTIVE' | 'DISABLED'
  prereqCourseIds?: number[]
  prereqNames?: string
}

// Teaching Class & Schedule
export interface TeachingClassItem {
  id: number
  courseId: number
  courseCode: string
  courseName: string
  termId: number
  termName?: string
  teacherId: number
  teacherName: string
  className: string
  capacity: number
  enrolledCount: number
  credit: number
  location?: string
  startWeek: number
  endWeek: number
  openClassDate?: string
  status: TeachingClassStatus
  schedules?: ClassScheduleItem[]
}

export interface ClassScheduleItem {
  id?: number
  teachingClassId?: number
  dayOfWeek: number // 1..7 (周一..周日)
  startSection: number
  endSection: number
  startMinute: number
  endMinute: number
  location?: string
  weekDesc: string
  startWeek: number
  endWeek: number
}

// Enrollment
export interface EnrollmentItem {
  id: number
  studentId: number
  studentName?: string
  studentUsername?: string
  teachingClassId: number
  className: string
  courseId: number
  courseCode?: string
  courseName: string
  termId: number
  termName?: string
  credit: number
  status: EnrollmentStatus
  enrolledAt: string
  withdrawnAt?: string
  source: 'PORTAL' | 'ADMIN'
  schedules?: ClassScheduleItem[]
}

// Grade
export interface GradeItem {
  id: number
  enrollmentId: number
  teachingClassId: number
  studentId: number
  studentNumber?: string
  studentName?: string
  courseId: number
  courseName?: string
  credit?: number
  termId: number
  regularScore?: number
  midtermScore?: number
  finalScore?: number
  totalScore?: number
  gradePoint?: number
  isPass?: number
  status: GradeStatus
  publishedAt?: string
  unlockCount?: number
  lastUnlockReason?: string
}

// Stats
export interface StatsOverview {
  termId: number
  totalEnrollments: number
  totalCapacity: number
  avgFillRate: number
  classCount: number
  studentCount: number
}

export interface StudentCreditSummary {
  studentId: number
  termId: number
  totalEnrolledCredits: number
  earnedCredits: number
  gpa: number
  courseCount: number
  passCount: number
  failCount: number
}

// Audit & Config
export interface AuditLogItem {
  id: number
  userId?: number
  username?: string
  roleCode?: string
  module: string
  action: string
  targetType?: string
  targetId?: number
  beforeJson?: string
  afterJson?: string
  result: 'SUCCESS' | 'FAIL'
  ip?: string
  traceId?: string
  createdAt: string
}

export interface LoginLogItem {
  id: number
  username: string
  userId?: number
  ip?: string
  userAgent?: string
  result: string
  loginAt: string
}

export interface SysConfigItem {
  id: number
  configKey: string
  configValue: string
  valueType: 'STRING' | 'NUMBER' | 'BOOL' | 'JSON'
  description?: string
  editable: number
}
