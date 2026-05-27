<template>
  <div>
    <div class="page-header" style="display:flex;justify-content:space-between;align-items:start">
      <div>
        <h1>📖 我的学习</h1>
        <p>{{ auth.user?.name }}，欢迎回来！看看今天有什么新内容吧。</p>
      </div>
      <button class="btn btn-accent" @click="showEnroll = true">+ 加入课程</button>
    </div>

    <!-- Stats -->
    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-value">{{ courses.length }}</div>
        <div class="stat-label">已选课程</div>
      </div>
      <div class="stat-card accent">
        <div class="stat-value">{{ activeTasks }}</div>
        <div class="stat-label">进行中作业</div>
      </div>
      <div class="stat-card warm" v-if="submissions.length > 0">
        <div class="stat-value">{{ submissions.length }}</div>
        <div class="stat-label">提交记录</div>
      </div>
    </div>

    <!-- Enroll Modal -->
    <div class="modal-overlay" v-if="showEnroll" @click.self="showEnroll = false">
      <div class="modal-content">
        <h3>🔑 加入课程</h3>
        <p style="font-size:13px;color:var(--gray-500);margin-bottom:16px">输入教师提供的课程邀请码即可加入</p>
        <div class="form-error" v-if="enrollError">{{ enrollError }}</div>
        <div class="form-group">
          <label>邀请码</label>
          <input v-model="inviteCode" class="form-input" placeholder="例如：A1B2C3D4" @keyup.enter="handleEnroll" autofocus>
        </div>
        <div class="actions">
          <button class="btn btn-ghost" @click="showEnroll = false; enrollError = ''">取消</button>
          <button class="btn btn-accent" @click="handleEnroll" :disabled="enrolling">
            {{ enrolling ? '加入中...' : '确认加入' }}
          </button>
        </div>
      </div>
    </div>

    <div v-if="loading" class="loading">
      <div class="loading-spinner"></div>
      <div>加载中...</div>
    </div>

    <template v-else>
      <!-- My Courses -->
      <div class="page-toolbar">
        <h2 style="font-size:18px;font-weight:600;display:flex;align-items:center;gap:8px">
          <span style="color:var(--accent)">📚</span> 我的课程
        </h2>
      </div>
      <div class="card-grid" v-if="courses.length > 0">
        <div class="card course-card" v-for="c in courses" :key="c.id" @click="$router.push(`/courses/${c.id}`)">
          <h3>{{ c.name }}</h3>
          <div class="meta">👨‍🏫 {{ c.teacherName }} · {{ c.studentCount || 0 }} 名同学</div>
          <div class="desc" v-if="c.description">{{ c.description }}</div>
          <div style="margin-top:10px">
            <span class="badge badge-blue" v-if="c.credits">{{ c.credits }}学分</span>
          </div>
        </div>
      </div>
      <div class="empty" v-else>
        <div class="empty-icon">📖</div>
        <p>还没有加入任何课程，快去探索吧！</p>
        <button class="btn btn-accent" @click="showEnroll = true">加入课程</button>
      </div>

      <hr class="section-divider">

      <!-- Recent Tasks -->
      <div class="page-toolbar">
        <h2 style="font-size:18px;font-weight:600;display:flex;align-items:center;gap:8px">
          <span style="color:var(--warning)">📝</span> 近期作业
        </h2>
      </div>
      <div v-if="tasks.length > 0">
        <div class="card task-item" v-for="t in tasks" :key="t.id"
             @click="goToTask(t)">
          <div>
            <div class="task-title">{{ t.title }}</div>
            <div class="task-meta">
              {{ t.courseName }} · {{ t.type === 'programming' ? '💻编程实训' : t.type === 'exam' ? '📋考试' : '📄作业' }}
              · 截止 {{ formatDate(t.endTime) }}
            </div>
          </div>
          <span class="badge" :class="getStatusClass(t)">{{ getStatusText(t) }}</span>
        </div>
      </div>
      <div class="empty" v-else style="padding:30px">
        <p>暂无作业，轻松一刻~</p>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { studentApi } from '../api'

const auth = useAuthStore()
const router = useRouter()
const courses = ref([])
const tasks = ref([])
const submissions = ref([])
const loading = ref(true)
const showEnroll = ref(false)
const inviteCode = ref('')
const enrollError = ref('')
const enrolling = ref(false)

const activeTasks = computed(() => tasks.value.filter(t => {
  if (!t.endTime) return true
  return new Date(t.endTime) >= new Date()
}).length)

onMounted(async () => {
  try {
    const [c, t, s] = await Promise.all([
      studentApi.getCourses(),
      studentApi.getTasks(),
      studentApi.getSubmissions()
    ])
    courses.value = c.data || []
    tasks.value = t.data || []
    submissions.value = s.data || []
  } finally {
    loading.value = false
  }
})

function goToTask(task) {
  if (task.type === 'programming') {
    router.push(`/experiment/${task.id}`)
  } else {
    router.push(`/tasks/${task.id}`)
  }
}

async function handleEnroll() {
  if (!inviteCode.value.trim()) {
    enrollError.value = '请输入邀请码'
    return
  }
  enrollError.value = ''
  enrolling.value = true
  try {
    await studentApi.enroll(inviteCode.value.trim().toUpperCase())
    const c = await studentApi.getCourses()
    courses.value = c.data || []
    showEnroll.value = false
    inviteCode.value = ''
  } catch (e) {
    enrollError.value = e.response?.data?.message || '加入失败，请检查邀请码'
  } finally {
    enrolling.value = false
  }
}

function formatDate(ts) {
  if (!ts) return '不限'
  const d = new Date(ts)
  return d.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })
}

function getStatusClass(task) {
  if (!task.endTime) return 'badge-green'
  return new Date(task.endTime) < new Date() ? 'badge-danger' : 'badge-green'
}

function getStatusText(task) {
  if (!task.endTime) return '进行中'
  return new Date(task.endTime) < new Date() ? '已截止' : '进行中'
}
</script>
