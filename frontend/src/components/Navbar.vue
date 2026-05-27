<template>
  <nav class="navbar">
    <router-link :to="auth.isTeacher ? '/teacher' : '/'" class="navbar-brand">
      <span class="icon">📚</span>
      教务平台
    </router-link>
    <div class="navbar-nav" v-if="auth.isLoggedIn">
      <!-- Teacher nav -->
      <template v-if="auth.isTeacher">
        <router-link to="/teacher" class="nav-link" :class="{ active: $route.path === '/teacher' }">工作台</router-link>
      </template>
      <!-- Student nav -->
      <template v-else>
        <router-link to="/" class="nav-link" :class="{ active: $route.path === '/' }">我的首页</router-link>
        <router-link to="/courses" class="nav-link" :class="{ active: $route.path === '/courses' }">课程广场</router-link>
      </template>

      <router-link to="/profile" class="user-info-link" style="font-size:13px;color:var(--gray-500);margin-left:8px;display:flex;align-items:center;gap:8px;text-decoration:none;cursor:pointer">
        {{ auth.user?.name || auth.user?.username }}
        <span class="badge" :class="auth.isTeacher ? 'badge-blue' : 'badge-green'">
          {{ auth.isTeacher ? '教师' : '学生' }}
        </span>
      </router-link>
      <button class="btn btn-ghost btn-sm" @click="handleLogout">退出</button>
    </div>
    <div class="navbar-nav" v-else>
      <router-link to="/login" class="btn btn-primary btn-sm">登录 / 注册</router-link>
    </div>
  </nav>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const router = useRouter()

async function handleLogout() {
  await auth.logout()
  router.push('/login')
}
</script>