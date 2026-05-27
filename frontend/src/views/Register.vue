<template>
  <div class="auth-page">
    <div class="auth-card">
      <h2>注册</h2>
      <p class="sub">创建您的教学平台账号</p>
      <div class="form-error" v-if="error">{{ error }}</div>
      <form @submit.prevent="handleRegister">
        <div class="form-group">
          <label>用户名</label>
          <input v-model="form.username" class="form-input" placeholder="3-20个字符" required>
        </div>
        <div class="form-group">
          <label>密码</label>
          <input v-model="form.password" type="password" class="form-input" placeholder="6-32个字符" required>
        </div>
        <div class="form-group">
          <label>昵称</label>
          <input v-model="form.name" class="form-input" placeholder="可选，默认使用用户名">
        </div>
        <div class="form-group">
          <label>身份</label>
          <select v-model="form.role" class="form-select" required>
            <option value="student">学生</option>
            <option value="teacher">教师</option>
          </select>
        </div>
        <button type="submit" class="btn btn-primary btn-lg" style="width:100%;margin-top:8px" :disabled="loading">
          {{ loading ? '注册中...' : '立即注册' }}
        </button>
      </form>
      <p style="text-align:center;margin-top:20px;font-size:13px;color:var(--gray-500)">
        已有账号？<router-link to="/login">前往登录</router-link>
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const auth = useAuthStore()
const error = ref('')
const loading = ref(false)
const form = reactive({ username: '', password: '', name: '', role: 'student' })

async function handleRegister() {
  error.value = ''
  loading.value = true
  try {
    await auth.register(form.username, form.password, form.role, form.name || form.username)
    await auth.login(form.username, form.password)
    router.push('/')
  } catch (e) {
    error.value = e.response?.data?.message || '注册失败，请重试'
  } finally {
    loading.value = false
  }
}
</script>
