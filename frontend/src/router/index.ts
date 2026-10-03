import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes: RouteRecordRaw[] = [
  // 认证区
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/auth/LoginView.vue'),
    meta: { public: true, layout: 'AuthLayout' },
  },
  {
    path: '/',
    component: () => import('@/views/shared/ProfileView.vue'),
    meta: { requiresAuth: true },
  },

  // 公共中心 (STA)
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('@/views/shared/ProfileView.vue'),
    meta: { requiresAuth: true, roles: ['STUDENT', 'TEACHER', 'ADMIN'], nav: true, title: '个人中心', icon: 'i-lucide-user-round' },
  },
  {
    path: '/profile/password',
    name: 'Password',
    component: () => import('@/views/shared/PasswordView.vue'),
    meta: { requiresAuth: true, roles: ['STUDENT', 'TEACHER', 'ADMIN'] },
  },

  // 学生区 (STUDENT)
  {
    path: '/student',
    redirect: '/student/courses',
  },
  {
    path: '/student/courses',
    name: 'StudentCourses',
    component: () => import('@/views/student/CourseSelectionView.vue'),
    meta: { requiresAuth: true, roles: ['STUDENT'], nav: true, icon: 'i-lucide-book-open', title: '课程选择' },
  },
  {
    path: '/student/timetable',
    name: 'StudentTimetable',
    component: () => import('@/views/student/TimetableView.vue'),
    meta: { requiresAuth: true, roles: ['STUDENT'], nav: true, icon: 'i-lucide-calendar-days', title: '我的课表' },
  },
  {
    path: '/student/grades',
    name: 'StudentGrades',
    component: () => import('@/views/student/MyGradesView.vue'),
    meta: { requiresAuth: true, roles: ['STUDENT'], nav: true, icon: 'i-lucide-chart-no-axes-column', title: '我的成绩' },
  },
  {
    path: '/student/summary',
    name: 'StudentSummary',
    component: () => import('@/views/student/CreditSummaryView.vue'),
    meta: { requiresAuth: true, roles: ['STUDENT'], nav: true, icon: 'i-lucide-chart-pie', title: '我的学分统计' },
  },

  // 教师区 (TEACHER)
  {
    path: '/teacher',
    redirect: '/teacher/classes',
  },
  {
    path: '/teacher/classes',
    name: 'TeacherClasses',
    component: () => import('@/views/teacher/MyClassesView.vue'),
    meta: { requiresAuth: true, roles: ['TEACHER'], nav: true, icon: 'i-lucide-chalkboard-teacher', title: '我的教学班' },
  },
  {
    path: '/teacher/classes/:id/roster',
    name: 'TeacherClassRoster',
    component: () => import('@/views/teacher/ClassRosterView.vue'),
    meta: { requiresAuth: true, roles: ['TEACHER'], title: '选课名单' },
  },
  {
    path: '/teacher/classes/:id/grades',
    name: 'TeacherClassGrades',
    component: () => import('@/views/teacher/ClassGradeView.vue'),
    meta: { requiresAuth: true, roles: ['TEACHER'], title: '成绩录入与发布' },
  },

  // 管理区 (ADMIN)
  {
    path: '/admin',
    redirect: '/admin/dashboard',
  },
  {
    path: '/admin/dashboard',
    name: 'AdminDashboard',
    component: () => import('@/views/admin/DashboardView.vue'),
    meta: { requiresAuth: true, roles: ['ADMIN'], layout: 'AdminLayout', nav: true, icon: 'i-lucide-layout-dashboard', title: '概览看板' },
  },
  {
    path: '/admin/users',
    name: 'AdminUsers',
    component: () => import('@/views/admin/UserListView.vue'),
    meta: { requiresAuth: true, roles: ['ADMIN'], layout: 'AdminLayout', nav: true, icon: 'i-lucide-users-round', title: '用户管理' },
  },
  {
    path: '/admin/depts',
    name: 'AdminDepts',
    component: () => import('@/views/admin/DeptMajorView.vue'),
    meta: { requiresAuth: true, roles: ['ADMIN'], layout: 'AdminLayout', nav: true, icon: 'i-lucide-building-2', title: '院系/专业' },
  },
  {
    path: '/admin/terms',
    name: 'AdminTerms',
    component: () => import('@/views/admin/TermListView.vue'),
    meta: { requiresAuth: true, roles: ['ADMIN'], layout: 'AdminLayout', nav: true, icon: 'i-lucide-calendar-range', title: '学期管理' },
  },
  {
    path: '/admin/courses',
    name: 'AdminCourses',
    component: () => import('@/views/admin/CourseListView.vue'),
    meta: { requiresAuth: true, roles: ['ADMIN'], layout: 'AdminLayout', nav: true, icon: 'i-lucide-library-big', title: '课程管理' },
  },
  {
    path: '/admin/teaching-classes',
    name: 'AdminTeachingClasses',
    component: () => import('@/views/admin/TeachingClassListView.vue'),
    meta: { requiresAuth: true, roles: ['ADMIN'], layout: 'AdminLayout', nav: true, icon: 'i-lucide-presentation', title: '教学班管理' },
  },
  {
    path: '/admin/stats',
    name: 'AdminStats',
    component: () => import('@/views/admin/StatsView.vue'),
    meta: { requiresAuth: true, roles: ['ADMIN'], layout: 'AdminLayout', nav: true, icon: 'i-lucide-chart-column', title: '统计报表' },
  },
  {
    path: '/admin/audit-logs',
    name: 'AdminAuditLogs',
    component: () => import('@/views/admin/AuditLogView.vue'),
    meta: { requiresAuth: true, roles: ['ADMIN'], layout: 'AdminLayout', nav: true, icon: 'i-lucide-shield-check', title: '审计日志' },
  },
  {
    path: '/admin/login-logs',
    name: 'AdminLoginLogs',
    component: () => import('@/views/admin/LoginLogView.vue'),
    meta: { requiresAuth: true, roles: ['ADMIN'], layout: 'AdminLayout', nav: true, icon: 'i-lucide-history', title: '登录日志' },
  },
  {
    path: '/admin/configs',
    name: 'AdminConfigs',
    component: () => import('@/views/admin/ConfigView.vue'),
    meta: { requiresAuth: true, roles: ['ADMIN'], layout: 'AdminLayout', nav: true, icon: 'i-lucide-settings-2', title: '系统参数' },
  },

  // 错误页
  {
    path: '/403',
    name: 'Forbidden',
    component: () => import('@/views/error/ForbiddenView.vue'),
    meta: { public: true, layout: 'AuthLayout' },
  },
  {
    path: '/404',
    name: 'NotFound',
    component: () => import('@/views/error/NotFoundView.vue'),
    meta: { public: true, layout: 'AuthLayout' },
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/404',
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// docs/04 §4.1: 全局路由守卫流程
router.beforeEach(async (to) => {
  const authStore = useAuthStore()

  const homeOf = () => authStore.isStudent ? '/student/courses'
    : authStore.isTeacher ? '/teacher/classes'
    : authStore.isAdmin ? '/admin/dashboard' : '/profile'

  // 1. 公开路由判断
  if (to.meta.public) {
    // 仅在确实持有有效用户信息时才从登录页跳走，
    // 否则 localStorage 里的过期 Token 会造成 登录页 ↔ 业务页 来回跳。
    if (authStore.isAuthenticated && to.path === '/login') {
      try {
        if (!authStore.loaded) await authStore.fetchMe()
        return authStore.mustChangePwd ? '/profile/password' : homeOf()
      } catch {
        return true
      }
    }
    return true
  }

  // 2. 无 Token 拦截到登录
  if (!authStore.isAuthenticated) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  // 3. 有 Token 但用户信息尚未加载：先校验一次，失效则清理并回登录页
  if (!authStore.loaded || !authStore.userType) {
    try {
      await authStore.fetchMe()
    } catch {
      return { path: '/login', query: { redirect: to.fullPath } }
    }
  }

  // 4. 强制改密规则守卫
  if (authStore.mustChangePwd && to.path !== '/profile/password') {
    return '/profile/password'
  }

  if (to.path === '/') return homeOf()

  // 5. 角色权限拦截 -> /403
  const requiredRoles = to.meta.roles as string[] | undefined
  if (requiredRoles && !requiredRoles.includes(authStore.userType)) {
    return '/403'
  }

  return true
})

export default router
