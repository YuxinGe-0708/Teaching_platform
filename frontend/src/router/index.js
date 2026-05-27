import { createRouter, createWebHistory } from 'vue-router'
import { authApi } from '../api'

const routes = [
  { path: '/', name: 'home', component: () => import('../views/StudentHome.vue'), meta: { requiresAuth: true, role: 'student' } },
  { path: '/courses', name: 'allCourses', component: () => import('../views/AllCourses.vue'), meta: { requiresAuth: true, role: 'student' } },
  { path: '/courses/:id', name: 'courseDetail', component: () => import('../views/CourseDetail.vue'), meta: { requiresAuth: true, role: 'student' } },
  { path: '/tasks/:id', name: 'taskSubmit', component: () => import('../views/TaskSubmit.vue'), meta: { requiresAuth: true, role: 'student' } },
  { path: '/profile', name: 'profile', component: () => import('../views/Profile.vue'), meta: { requiresAuth: true } },
  { path: '/experiment/:id', name: 'experiment', component: () => import('../views/Experiment.vue'), meta: { requiresAuth: true, role: 'student' } },
  { path: '/teacher', name: 'teacherHome', component: () => import('../views/TeacherHome.vue'), meta: { requiresAuth: true, role: 'teacher' } },
  { path: '/teacher/course/:id', name: 'teacherTasks', component: () => import('../views/TeacherTasks.vue'), meta: { requiresAuth: true, role: 'teacher' } },
  { path: '/teacher/grading/:taskId', name: 'teacherGrading', component: () => import('../views/TeacherGrading.vue'), meta: { requiresAuth: true, role: 'teacher' } },
  { path: '/teacher/stats/:id', name: 'teacherStats', component: () => import('../views/TeacherStats.vue'), meta: { requiresAuth: true, role: 'teacher' } },
  { path: '/login', name: 'login', component: () => import('../views/Login.vue') },
  { path: '/register', name: 'register', component: () => import('../views/Register.vue') }
]

const router = createRouter({ history: createWebHistory('/app/'), routes })

// Track whether we have tried syncing session
let sessionSynced = false

router.beforeEach(async (to, from, next) => {
  let user = JSON.parse(localStorage.getItem('user') || 'null')

  // On first navigation, try to sync user from backend session
  if (!sessionSynced) {
    sessionSynced = true
    if (!user) {
      try {
        const res = await authApi.me()
        if (res.code === 200 && res.data && res.data.id) {
          user = res.data
          localStorage.setItem('user', JSON.stringify(user))
        }
      } catch (e) { /* session not logged in, that is ok */ }
    }
  }

  if (to.meta.requiresAuth && !user) {
    // Not logged in → redirect to Thymeleaf login page
    window.location.href = '/login'
    return
  }
  if (to.meta.role && user && user.role !== to.meta.role && to.meta.role !== undefined) {
    next(user.role === 'teacher' ? '/teacher' : '/')
  } else {
    next()
  }
})

export default router
