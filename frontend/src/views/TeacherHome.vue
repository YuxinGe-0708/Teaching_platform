<template>
  <div>
    <div class="page-header" style="display:flex;justify-content:space-between;align-items:start">
      <div>
        <h1>👨‍🏫 教师工作台</h1>
        <p>{{ auth.user?.name }}，管理你的课程和教学任务</p>
      </div>
      <button class="btn btn-accent" @click="showCreate = true">+ 创建新课程</button>
    </div>

    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-value">{{ courses.length }}</div>
        <div class="stat-label">已创建课程</div>
      </div>
      <div class="stat-card accent">
        <div class="stat-value">{{ totalStudents }}</div>
        <div class="stat-label">选课学生</div>
      </div>
      <div class="stat-card warm">
        <div class="stat-value">{{ totalTasks }}</div>
        <div class="stat-label">已发布作业</div>
      </div>
    </div>

    <!-- Create Course Modal -->
    <div class="modal-overlay" v-if="showCreate" @click.self="showCreate = false">
      <div class="modal-content">
        <h3>📘 创建新课程</h3>
        <div class="form-error" v-if="createError">{{ createError }}</div>
        <div class="form-group">
          <label>课程名称 *</label>
          <input v-model="newCourse.name" class="form-input" placeholder="例如：高等数学">
        </div>
        <div class="form-group">
          <label>课程代码</label>
          <input v-model="newCourse.code" class="form-input" placeholder="例如：MATH101">
        </div>
        <div class="form-group">
          <label>学分</label>
          <input v-model.number="newCourse.credits" type="number" class="form-input" placeholder="4" min="0">
        </div>
        <div class="form-group">
          <label>课程描述</label>
          <textarea v-model="newCourse.description" class="form-input" rows="3" placeholder="简要描述课程内容..."></textarea>
        </div>
        <div class="actions">
          <button class="btn btn-ghost" @click="showCreate = false; createError = ''">取消</button>
          <button class="btn btn-accent" @click="handleCreate" :disabled="creating">
            {{ creating ? '创建中...' : '确认创建' }}
          </button>
        </div>
      </div>
    </div>

    <!-- Edit Course Modal -->
    <div class="modal-overlay" v-if="showEdit" @click.self="showEdit = false">
      <div class="modal-content">
        <h3>✏️ 编辑课程</h3>
        <div class="form-error" v-if="editError">{{ editError }}</div>
        <div class="form-group">
          <label>课程名称</label>
          <input v-model="editForm.name" class="form-input">
        </div>
        <div class="form-group">
          <label>课程代码</label>
          <input v-model="editForm.code" class="form-input">
        </div>
        <div class="form-group">
          <label>学分</label>
          <input v-model.number="editForm.credits" type="number" class="form-input" min="0">
        </div>
        <div class="form-group">
          <label>课程描述</label>
          <textarea v-model="editForm.description" class="form-input" rows="3"></textarea>
        </div>
        <div class="actions">
          <button class="btn btn-ghost" @click="showEdit = false">取消</button>
          <button class="btn btn-primary" @click="handleEdit" :disabled="editing">保存</button>
        </div>
      </div>
    </div>

    <div v-if="loading" class="loading"><div class="loading-spinner"></div></div>

    <template v-else>
      <div v-if="courses.length === 0" class="empty">
        <div class="empty-icon">📚</div>
        <p>还没有创建任何课程</p>
        <button class="btn btn-accent" @click="showCreate = true">创建第一门课程</button>
      </div>

      <div class="card-grid" v-else>
        <div class="card" style="padding:20px" v-for="c in courses" :key="c.id">
          <div style="display:flex;justify-content:space-between;align-items:start;margin-bottom:12px">
            <div>
              <h3 style="font-size:16px;font-weight:600;color:var(--gray-900)">{{ c.name }}</h3>
              <div style="font-size:13px;color:var(--gray-500);margin-top:2px">
                {{ c.code || '无代码' }} · {{ c.credits }}学分 · {{ c.studentCount || 0 }}名学生
              </div>
            </div>
            <span class="badge" :class="c.status === 'active' ? 'badge-green' : 'badge-gray'">
              {{ c.status === 'active' ? '进行中' : '已归档' }}
            </span>
          </div>
          <div style="font-size:13px;color:var(--gray-600);margin-bottom:12px;line-height:1.5">
            {{ c.description || '暂无描述' }}
          </div>
          <div style="font-size:12px;color:var(--gray-400);margin-bottom:12px">
            邀请码：<code>{{ c.inviteCode }}</code>
          </div>
          <div style="display:flex;gap:8px;flex-wrap:wrap">
            <button class="btn btn-primary btn-sm" @click="$router.push(`/teacher/course/${c.id}`)">📝 管理作业</button>
            <button class="btn btn-outline btn-sm" @click="openEdit(c)">✏️ 编辑</button>
            <button class="btn btn-danger btn-sm" @click="handleDelete(c)">🗑 删除</button>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useAuthStore } from '../stores/auth'
import api from '../api'

const auth = useAuthStore()
const courses = ref([])
const loading = ref(true)
const showCreate = ref(false)
const creating = ref(false)
const createError = ref('')
const newCourse = reactive({ name: '', code: '', credits: 0, description: '' })

const showEdit = ref(false)
const editing = ref(false)
const editError = ref('')
const editForm = reactive({ id: null, name: '', code: '', credits: 0, description: '' })

const totalStudents = computed(() => courses.value.reduce((s, c) => s + (c.studentCount || 0), 0))
const totalTasks = ref(0)

onMounted(async () => {
  try {
    const res = await api.get('/teacher/courses')
    courses.value = res.data || []
    let t = 0
    for (const c of courses.value) {
      try {
        const tr = await api.get(`/teacher/course/${c.id}/tasks`)
        t += (tr.data || []).length
      } catch (e) { /* ignore */ }
    }
    totalTasks.value = t
  } finally {
    loading.value = false
  }
})

async function handleCreate() {
  if (!newCourse.name.trim()) { createError.value = '请输入课程名称'; return }
  creating.value = true; createError.value = ''
  try {
    await api.post('/teacher/course/create', {
      name: newCourse.name.trim(),
      code: newCourse.code.trim(),
      credits: newCourse.credits,
      description: newCourse.description.trim()
    })
    const res = await api.get('/teacher/courses')
    courses.value = res.data || []
    showCreate.value = false
    Object.assign(newCourse, { name: '', code: '', credits: 0, description: '' })
  } catch (e) {
    createError.value = e.response?.data?.message || '创建失败'
  } finally { creating.value = false }
}

function openEdit(c) {
  editForm.id = c.id; editForm.name = c.name; editForm.code = c.code || ''
  editForm.credits = c.credits || 0; editForm.description = c.description || ''
  editError.value = ''; showEdit.value = true
}

async function handleEdit() {
  editing.value = true
  try {
    await api.put(`/teacher/course/${editForm.id}`, {
      name: editForm.name, code: editForm.code,
      credits: editForm.credits, description: editForm.description
    })
    const res = await api.get('/teacher/courses')
    courses.value = res.data || []
    showEdit.value = false
  } catch (e) {
    editError.value = e.response?.data?.message || '保存失败'
  } finally { editing.value = false }
}

async function handleDelete(c) {
  if (!confirm(`确定删除课程「${c.name}」？\n此操作不可恢复！`)) return
  await api.delete(`/teacher/course/${c.id}`)
  courses.value = courses.value.filter(x => x.id !== c.id)
}
</script>
