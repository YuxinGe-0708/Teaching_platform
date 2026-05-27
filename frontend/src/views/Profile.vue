<template>
  <div>
    <div class="page-header" style="display:flex;justify-content:space-between;align-items:start">
      <div>
        <h1>👤 个人资料</h1>
        <p>管理你的账户信息和学习概况</p>
      </div>
      <button v-if="!editing" class="btn btn-primary btn-sm" @click="startEdit">✏️ 编辑资料</button>
    </div>

    <div v-if="loading" class="loading">
      <div class="loading-spinner"></div>
      <div>加载中...</div>
    </div>

    <template v-else>
      <!-- Stats Row -->
      <div class="stats-row">
        <div class="stat-card">
          <div class="stat-value">{{ stats?.courseCount || 0 }}</div>
          <div class="stat-label">已选课程</div>
        </div>
        <div class="stat-card accent">
          <div class="stat-value">{{ stats?.activeTaskCount || 0 }}</div>
          <div class="stat-label">进行中作业</div>
        </div>
        <div class="stat-card warm">
          <div class="stat-value">{{ stats?.submissionCount || 0 }}</div>
          <div class="stat-label">提交记录</div>
        </div>
      </div>

      <div class="profile-grid">
        <!-- Left: Personal Info -->
        <div class="profile-section">
          <h2 class="section-title">📋 基本信息</h2>

          <!-- View mode -->
          <div class="info-card" v-if="!editing">
            <div class="info-avatar">
              {{ (editForm.name || userInfo?.username || '?')[0].toUpperCase() }}
            </div>
            <div class="info-body">
              <div class="info-row">
                <span class="info-label">用户名</span>
                <span class="info-value">{{ userInfo?.username }}</span>
              </div>
              <div class="info-row">
                <span class="info-label">姓名</span>
                <span class="info-value">{{ userInfo?.name || '未设置' }}</span>
              </div>
              <div class="info-row">
                <span class="info-label">邮箱</span>
                <span class="info-value">{{ userInfo?.email || '未设置' }}</span>
              </div>
              <div class="info-row">
                <span class="info-label">身份</span>
                <span class="badge badge-green">{{ userInfo?.role === 'student' ? '学生' : '教师' }}</span>
              </div>
              <div class="info-row" v-if="userInfo?.createdAt">
                <span class="info-label">注册时间</span>
                <span class="info-value">{{ formatDate(userInfo.createdAt) }}</span>
              </div>
            </div>
          </div>

          <!-- Edit mode -->
          <div class="info-card edit-card" v-else>
            <div class="info-avatar">
              {{ (editForm.name || userInfo?.username || '?')[0].toUpperCase() }}
            </div>
            <div class="info-body">
              <div class="form-error" v-if="saveError">{{ saveError }}</div>
              <div class="form-success" v-if="saveOk">{{ saveOk }}</div>
              <div class="form-group">
                <label>用户名</label>
                <input class="form-input" :value="userInfo?.username" disabled>
                <span style="font-size:11px;color:var(--gray-400);margin-top:2px;display:block">用户名不可修改</span>
              </div>
              <div class="form-group">
                <label>姓名</label>
                <input v-model="editForm.name" class="form-input" placeholder="输入你的姓名">
              </div>
              <div class="form-group">
                <label>邮箱</label>
                <input v-model="editForm.email" class="form-input" placeholder="输入你的邮箱">
              </div>
              <div class="edit-actions">
                <button class="btn btn-accent btn-sm" @click="saveProfile" :disabled="saving">
                  {{ saving ? '保存中...' : '💾 保存' }}
                </button>
                <button class="btn btn-ghost btn-sm" @click="cancelEdit">取消</button>
              </div>
            </div>
          </div>
        </div>

        <!-- Right: Quick Actions -->
        <div class="profile-section">
          <h2 class="section-title">⚡ 快捷操作</h2>
          <div class="action-list">
            <div class="action-item" @click="$router.push('/')">
              <span class="action-icon">🏠</span>
              <span>我的首页</span>
              <span class="action-arrow">→</span>
            </div>
            <div class="action-item" @click="$router.push('/courses')">
              <span class="action-icon">📚</span>
              <span>课程广场</span>
              <span class="action-arrow">→</span>
            </div>
            <div class="action-item" @click="handleLogout">
              <span class="action-icon">🚪</span>
              <span style="color:var(--danger)">退出登录</span>
              <span class="action-arrow">→</span>
            </div>
          </div>
        </div>
      </div>

      <hr class="section-divider">

      <!-- Enrolled Courses -->
      <div class="section-block">
        <h2 class="section-title" style="display:flex;align-items:center;gap:8px">
          <span style="color:var(--accent)">📚</span> 选课信息
          <span class="badge badge-blue">{{ courses.length }}门</span>
        </h2>
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
          <p>暂未加入任何课程</p>
        </div>
      </div>

      <hr class="section-divider">

      <!-- Submissions -->
      <div class="section-block">
        <h2 class="section-title" style="display:flex;align-items:center;gap:8px">
          <span style="color:var(--warning)">📝</span> 作业提交记录
          <span class="badge badge-blue">{{ submissions.length }}条</span>
        </h2>
        <div v-if="submissions.length > 0">
          <div class="card submission-item" v-for="s in submissions" :key="s.id">
            <div class="submission-left">
              <div class="submission-title">{{ s.taskTitle || '未命名作业' }}</div>
              <div class="submission-meta">
                提交于 {{ formatDate(s.submittedAt) }}
              </div>
            </div>
            <div class="submission-right">
              <span v-if="s.score != null" class="badge" :class="s.score >= 60 ? 'badge-green' : 'badge-danger'">
                {{ s.score }}分
              </span>
              <span v-else class="badge badge-gray">{{ statusMap[s.status] || s.status || '待批阅' }}</span>
            </div>
          </div>
        </div>
        <div class="empty" v-else>
          <p>暂无提交记录</p>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { studentApi, authApi } from '../api'

const auth = useAuthStore()
const router = useRouter()
const loading = ref(true)
const userInfo = ref(null)
const courses = ref([])
const submissions = ref([])
const stats = ref(null)
const editing = ref(false)
const saving = ref(false)
const saveError = ref('')
const saveOk = ref('')
const editForm = reactive({ name: '', email: '' })

const statusMap = {
  'submitted': '已提交',
  'judging': '评判中',
  'graded': '已批阅'
}

onMounted(async () => {
  try {
    const res = await studentApi.getProfile()
    if (res.code === 200 && res.data) {
      userInfo.value = res.data.user
      courses.value = res.data.courses || []
      submissions.value = res.data.submissions || []
      stats.value = res.data.stats
    }
  } finally {
    loading.value = false
  }
})

function startEdit() {
  editForm.name = userInfo.value?.name || ''
  editForm.email = userInfo.value?.email || ''
  saveError.value = ''
  saveOk.value = ''
  editing.value = true
}

function cancelEdit() {
  editing.value = false
  saveError.value = ''
}

async function saveProfile() {
  saving.value = true
  saveError.value = ''
  saveOk.value = ''
  try {
    const res = await authApi.updateProfile({
      name: editForm.name.trim() || null,
      email: editForm.email.trim() || null
    })
    if (res.code === 200) {
      userInfo.value = { ...userInfo.value, ...res.data }
      auth.user = { ...auth.user, ...res.data }
      localStorage.setItem('user', JSON.stringify(auth.user))
      saveOk.value = '保存成功！'
      setTimeout(() => {
        editing.value = false
        saveOk.value = ''
      }, 1200)
    }
  } catch (e) {
    saveError.value = e.response?.data?.message || '保存失败'
  } finally {
    saving.value = false
  }
}

function formatDate(ts) {
  if (!ts) return '-'
  const d = new Date(ts)
  return d.toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric', hour: '2-digit', minute: '2-digit' })
}

async function handleLogout() {
  await auth.logout()
  router.push('/login')
}
</script>

<style scoped>
.profile-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
  margin-bottom: 8px;
}

.profile-section {
  min-width: 0;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 16px;
  color: var(--gray-800);
}

.section-block {
  margin-bottom: 8px;
}

.info-card {
  background: var(--gray-50);
  border-radius: 12px;
  padding: 24px;
  display: flex;
  gap: 20px;
  align-items: flex-start;
}

.edit-card {
  background: #fff;
  border: 1.5px solid var(--gray-200);
}

.info-avatar {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--accent), #818cf8);
  color: #fff;
  font-size: 28px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.info-body {
  flex: 1;
  min-width: 0;
}

.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid var(--gray-100);
}

.info-row:last-child {
  border-bottom: none;
}

.info-label {
  font-size: 13px;
  color: var(--gray-500);
}

.info-value {
  font-size: 14px;
  font-weight: 500;
  color: var(--gray-800);
  text-align: right;
  word-break: break-all;
}

.edit-actions {
  display: flex;
  gap: 8px;
  margin-top: 16px;
}

.action-list {
  background: var(--gray-50);
  border-radius: 12px;
  overflow: hidden;
}

.action-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 20px;
  cursor: pointer;
  transition: background 0.15s;
  font-size: 14px;
  color: var(--gray-700);
  border-bottom: 1px solid var(--gray-100);
}

.action-item:last-child {
  border-bottom: none;
}

.action-item:hover {
  background: var(--gray-100);
}

.action-icon {
  font-size: 18px;
}

.action-arrow {
  margin-left: auto;
  color: var(--gray-400);
}

.submission-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 20px;
}

.submission-item:hover {
  background: var(--gray-50);
}

.submission-left {
  min-width: 0;
}

.submission-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--gray-800);
}

.submission-meta {
  font-size: 12px;
  color: var(--gray-500);
  margin-top: 4px;
}

.submission-right {
  flex-shrink: 0;
  margin-left: 16px;
}

.badge-gray {
  background: var(--gray-100);
  color: var(--gray-600);
}

@media (max-width: 640px) {
  .profile-grid {
    grid-template-columns: 1fr;
  }
}
</style>