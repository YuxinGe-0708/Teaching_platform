<template>
  <div class="auth-page">
    <div class="auth-card" style="text-align:center">
      <h2>登录</h2>
      <p class="sub" style="margin-bottom:20px">正在跳转到统一登录页面...</p>
      <button class="btn btn-primary btn-lg" @click="goLogin" style="width:100%">前往登录</button>
      <p style="text-align:center;margin-top:20px;font-size:13px;color:var(--gray-500)">
        还没有账号？<a href="/register">立即注册</a>
      </p>
    </div>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'

function goLogin() {
  window.location.href = '/login'
}

onMounted(() => {
  goLogin()
})
</script>
