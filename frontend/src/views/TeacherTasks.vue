<template>
  <div>
    <div class="page-header">
      <button class="btn btn-ghost btn-sm" @click="$router.push('/teacher')" style="margin-bottom:12px">← 返回工作台</button>
      <div style="display:flex;justify-content:space-between;align-items:start;flex-wrap:wrap;gap:10px">
        <div>
          <h1 v-if="course">📝 {{ course.name }}</h1>
          <p v-if="course">邀请码：<code>{{ course.inviteCode }}</code> · {{ course.studentCount || 0 }}名学生</p>
        </div>
        <div style="display:flex;gap:8px">
          <button class="btn btn-outline btn-sm" @click="$router.push(`/teacher/stats/${courseId}`)">📊 成绩统计</button>
          <button class="btn btn-accent" @click="showCreate = true">+ 发布作业</button>
        </div>
      </div>
    </div>

    <!-- Create Task Modal -->
    <div class="modal-overlay" v-if="showCreate" @click.self="showCreate = false">
      <div class="modal-content" style="max-width:560px">
        <h3>📝 发布新作业</h3>
        <div class="form-error" v-if="createError">{{ createError }}</div>
        <div class="form-group">
          <label>作业标题 *</label>
          <input v-model="newTask.title" class="form-input" placeholder="例如：第一章课后练习">
        </div>
        <div class="form-group">
          <label>类型</label>
          <select v-model="newTask.type" class="form-select">
            <option value="homework">📄 普通作业</option>
            <option value="exam">📋 考试</option>
            <option value="programming">💻 编程实训</option>
          </select>
        </div>
        <div class="form-group">
          <label>满分</label>
          <input v-model.number="newTask.maxScore" type="number" class="form-input" min="0" max="1000">
        </div>
        <div class="form-group">
          <label>截止时间（可选）</label>
          <input v-model="newTask.endTime" class="form-input" placeholder="留空不限时，格式：2026-06-01 23:59:59">
        </div>
        <div class="form-group">
          <label>描述（支持Markdown）</label>
          <textarea v-model="newTask.description" class="form-input" rows="4"
            placeholder="输入作业详细说明。支持Markdown格式。&#10;如果是编程实训题，可用：&#10;输入:xxx 输出:yyy --- 输入:aaa 输出:bbb"></textarea>
        </div>
        <div class="actions">
          <button class="btn btn-ghost" @click="showCreate = false; createError = ''">取消</button>
          <button class="btn btn-accent" @click="handleCreate" :disabled="creating">{{ creating ? '创建中...' : '发布' }}</button>
        </div>
      </div>
    </div>

    <div v-if="loading" class="loading"><div class="loading-spinner"></div></div>

    <template v-else>
      <div v-if="tasks.length === 0" class="empty">
        <div class="empty-icon">📝</div>
        <p>还没有发布任何作业</p>
        <button class="btn btn-accent" @click="showCreate = true">发布第一个作业</button>
      </div>

      <div v-else>
        <div class="card" v-for="t in tasks" :key="t.id" style="padding:16px 20px;margin-bottom:10px">
          <div style="display:flex;justify-content:space-between;align-items:start;flex-wrap:wrap;gap:8px">
            <div style="flex:1;min-width:200px">
              <div style="font-weight:600;font-size:15px;color:var(--gray-900)">{{ t.title }}</div>
              <div style="font-size:13px;color:var(--gray-500);margin-top:4px">
                {{ t.type === 'programming' ? '💻 编程实训' : t.type === 'exam' ? '📋 考试' : '📄 作业' }}
                · 满分{{ t.maxScore }}分
                · 截止{{ t.endTime ? new Date(t.endTime).toLocaleDateString('zh-CN') : '不限' }}
              </div>
            </div>
            <div style="display:flex;align-items:center;gap:8px" class="task-actions">
              <span class="badge" :class="getStatusClass(t)">{{ getStatusText(t) }}</span>
              <button class="btn btn-outline btn-sm" @click="$router.push(`/teacher/grading/${t.id}`)">📋 批改</button>
              <button class="btn btn-danger btn-sm" @click="handleDelete(t)">删除</button>
            </div>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import api from '../api'

const route = useRoute()
const courseId = route.params.id
const course = ref(null)
const tasks = ref([])
const loading = ref(true)
const showCreate = ref(false)
const creating = ref(false)
const createError = ref('')
const newTask = reactive({ title: '', type: 'homework', maxScore: 100, endTime: '', description: '' })

onMounted(async () => {
  const [c, t] = await Promise.all([
    api.get(`/teacher/course/${courseId}`),
    api.get(`/teacher/course/${courseId}/tasks`)
  ])
  course.value = c.data
  tasks.value = t.data || []
  loading.value = false
})

async function handleCreate() {
  if (!newTask.title.trim()) { createError.value = 'Please enter title'; return }
  creating.value = true; createError.value = ''
  try {
    const body = {
      courseId: course.value.id,
      title: newTask.title.trim(),
      type: newTask.type,
      maxScore: newTask.maxScore,
      description: newTask.description.trim()
    }
    if (newTask.endTime.trim()) body.endTime = newTask.endTime.trim()
    await api.post('/teacher/task/create', body)
    const res = await api.get(`/teacher/course/${courseId}/tasks`)
    tasks.value = res.data || []
    showCreate.value = false
    Object.assign(newTask, { title: '', type: 'homework', maxScore: 100, endTime: '', description: '' })
  } catch (e) {
    createError.value = e.response?.data?.message || 'Create failed'
  } finally { creating.value = false }
}

async function handleDelete(t) {
  if (!confirm(`Delete task "${t.title}"?`)) return
  await api.delete(`/teacher/task/${t.id}`)
  tasks.value = tasks.value.filter(x => x.id !== t.id)
}

function getStatusClass(t) {
  if (!t.endTime) return 'badge-green'
  return new Date(t.endTime) < new Date() ? 'badge-danger' : 'badge-green'
}
function getStatusText(t) {
  if (!t.endTime) return 'Active'
  return new Date(t.endTime) < new Date() ? 'Ended' : 'Active'
}
</script>