<template>
  <div>
    <div class="page-header">
      <h1>🌐 课程广场</h1>
      <p>浏览所有开课课程，复制邀请码即可加入</p>
    </div>
    <div v-if="loading" class="loading"><div class="loading-spinner"></div></div>
    <div class="card-grid" v-else-if="courses.length > 0">
      <div class="card course-card" v-for="c in courses" :key="c.id"
           @click="$router.push(`/courses/${c.id}`)">
        <h3>{{ c.name }}</h3>
        <div class="meta">👨‍🏫 {{ c.teacherName }} · {{ c.credits || 0 }} 学分</div>
        <div class="desc" v-if="c.description">{{ c.description }}</div>
        <div style="margin-top:12px;display:flex;align-items:center;gap:8px">
          <span class="badge badge-blue">{{ c.studentCount || 0 }} 人选修</span>
          <code>{{ c.inviteCode }}</code>
        </div>
      </div>
    </div>
    <div class="empty" v-else>
      <div class="empty-icon">📚</div>
      <p>暂时没有开课的课程</p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { studentApi } from '../api'

const courses = ref([])
const loading = ref(true)

onMounted(async () => {
  try {
    const res = await studentApi.getAllCourses()
    courses.value = res.data || []
  } finally {
    loading.value = false
  }
})
</script>
