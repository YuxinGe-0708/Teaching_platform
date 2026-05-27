<template>
  <div>
    <div class="page-header">
      <button class="btn btn-ghost btn-sm" @click="$router.back()" style="margin-bottom:12px">← 返回</button>
      <h1 v-if="course">📘 {{ course.name }}</h1>
      <p v-if="course">👨‍🏫 {{ course.teacherName }} · {{ course.credits }}学分 · {{ course.studentCount }}名同学</p>
    </div>

    <div v-if="loading" class="loading"><div class="loading-spinner"></div></div>
    <template v-else-if="course">
      <!-- Tabs -->
      <div class="tabs">
        <button class="tab" :class="{ active: activeTab === 'info' }" @click="activeTab = 'info'">📖 课程信息</button>
        <button class="tab" :class="{ active: activeTab === 'tasks' }" @click="activeTab = 'tasks'">📝 课程作业 <span class="badge badge-blue" style="margin-left:4px">{{ tasks.length }}</span></button>
        <button class="tab" :class="{ active: activeTab === 'ai' }" @click="activeTab = 'ai'">🤖 AI 助手</button>
      </div>

      <!-- Tab: Course Info -->
      <div v-if="activeTab === 'info'">
        <div class="card" style="padding:20px;margin-bottom:24px">
          <h3 style="font-size:16px;font-weight:600;margin-bottom:8px;color:var(--gray-800)">课程简介</h3>
          <p style="font-size:14px;color:var(--gray-600);line-height:1.7">
            {{ course.description || '暂无简介' }}
          </p>
        </div>

        <div class="card" style="padding:20px;margin-bottom:24px">
          <h3 style="font-size:16px;font-weight:600;margin-bottom:8px;color:var(--gray-800)">课程信息</h3>
          <div style="display:grid;grid-template-columns:1fr 1fr;gap:12px;font-size:14px">
            <div><span style="color:var(--gray-500)">课程代码：</span>{{ course.code || '-' }}</div>
            <div><span style="color:var(--gray-500)">学分：</span>{{ course.credits || '-' }}</div>
            <div><span style="color:var(--gray-500)">授课教师：</span>{{ course.teacherName }}</div>
            <div><span style="color:var(--gray-500)">邀请码：</span><code>{{ course.inviteCode }}</code></div>
            <div><span style="color:var(--gray-500)">选课人数：</span>{{ course.studentCount || 0 }} 人</div>
          </div>
        </div>

        <button class="btn btn-outline btn-sm" @click="confirmUnenroll = true" v-if="!confirmUnenroll">
          退出课程
        </button>
        <div v-if="confirmUnenroll" class="card" style="padding:16px 20px;margin-top:12px;background:var(--warm);border-color:#fde68a">
          <span style="font-size:14px">确定要退出这门课程吗？退出后需重新使用邀请码加入。</span>
          <button class="btn btn-danger btn-sm" style="margin-left:12px" @click="handleUnenroll" :disabled="unenrolling">确认退出</button>
          <button class="btn btn-ghost btn-sm" style="margin-left:8px" @click="confirmUnenroll = false">取消</button>
        </div>
      </div>

      <!-- Tab: Tasks -->
      <div v-if="activeTab === 'tasks'">
        <div v-if="tasks.length > 0">
          <div class="card task-item" v-for="t in tasks" :key="t.id"
               @click="goToTask(t)">
            <div>
              <div class="task-title">{{ t.title }}</div>
              <div class="task-meta">
                {{ t.type === 'programming' ? '💻 编程实训' : t.type === 'exam' ? '📋 考试' : '📄 作业' }}
                · 满分{{ t.maxScore }}分
                · 截止{{ formatDate(t.endTime) }}
              </div>
            </div>
            <span class="badge" :class="getStatusClass(t)">{{ getStatusText(t) }}</span>
          </div>
        </div>
        <div class="empty" v-else style="padding:40px"><p>暂无作业</p></div>
      </div>

      <!-- Tab: AI -->
      <div v-if="activeTab === 'ai'">
        <AiChat :courseId="course.id" :courseName="course.name" />
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { studentApi } from '../api'
import AiChat from '../components/AiChat.vue'

const route = useRoute()
const router = useRouter()
const course = ref(null)
const tasks = ref([])
const loading = ref(true)
const activeTab = ref('info')
const confirmUnenroll = ref(false)
const unenrolling = ref(false)

onMounted(async () => {
  try {
    const id = route.params.id
    const [c, t] = await Promise.all([
      studentApi.getCourseDetail(id),
      studentApi.getTasks()
    ])
    course.value = c.data
    tasks.value = (t.data || []).filter(tk => tk.courseId == id)
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

async function handleUnenroll() {
  unenrolling.value = true
  try {
    await studentApi.unenroll(course.value.id)
    router.push('/')
  } catch (e) {
    alert('退出失败: ' + (e.response?.data?.message || '请重试'))
  } finally {
    unenrolling.value = false
  }
}

function formatDate(ts) {
  if (!ts) return '不限'
  return new Date(ts).toLocaleDateString('zh-CN', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })
}
function getStatusClass(t) {
  if (!t.endTime) return 'badge-green'
  return new Date(t.endTime) < new Date() ? 'badge-danger' : 'badge-green'
}
function getStatusText(t) {
  if (!t.endTime) return '进行中'
  return new Date(t.endTime) < new Date() ? '已截止' : '进行中'
}
</script>

<style scoped>
.tabs {
  display: flex;
  gap: 4px;
  margin-bottom: 20px;
  background: #fff;
  border-radius: var(--radius);
  padding: 4px;
  border: 1px solid #f0ebe3;
  display: inline-flex;
}
.tab {
  padding: 8px 18px;
  border: none;
  background: transparent;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  color: var(--gray-500);
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
}
.tab:hover { color: var(--gray-700); background: var(--gray-50); }
.tab.active {
  background: var(--primary);
  color: #fff;
}
</style>
