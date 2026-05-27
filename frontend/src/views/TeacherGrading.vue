<template>
  <div>
    <div class="page-header">
      <button class="btn btn-ghost btn-sm" @click="$router.back()" style="margin-bottom:12px">← 返回</button>
      <h1>📋 学生提交列表</h1>
      <p>{{ courseName }} · {{ taskTitle }}</p>
    </div>

    <div v-if="loading" class="loading"><div class="loading-spinner"></div></div>

    <template v-else>
      <div v-if="submissions.length === 0" class="empty"><p>暂无学生提交</p></div>

      <div class="card" v-for="s in submissions" :key="s.id" style="padding:16px 20px;margin-bottom:10px">
        <div style="display:flex;justify-content:space-between;align-items:center">
          <div style="flex:1">
            <div style="font-weight:600;font-size:15px">{{ s.studentName || 'Student #' + s.studentId }}</div>
            <div style="font-size:13px;color:var(--gray-500);margin-top:2px">
              提交于 {{ formatDate(s.submittedAt) }}
              <span v-if="s.filePath" style="margin-left:8px">📎 有附件</span>
            </div>
          </div>
          <div style="display:flex;align-items:center;gap:10px">
            <span v-if="s.score != null" class="badge badge-green">{{ s.score }} 分</span>
            <span v-else class="badge badge-gray">未批改</span>
            <button class="btn btn-primary btn-sm" @click="openGrade(s)">✏️ 批改</button>
          </div>
        </div>
        <!-- Expand: content preview -->
        <div v-if="s.content" style="margin-top:10px;padding:10px;background:var(--gray-50);border-radius:6px;font-size:13px;max-height:120px;overflow:auto">
          <pre style="margin:0;white-space:pre-wrap;font-family:monospace">{{ s.content }}</pre>
        </div>
        <div v-if="s.feedback" style="margin-top:8px;font-size:13px;color:var(--gray-600)">
          💬 评语：{{ s.feedback }}
        </div>
      </div>

      <!-- Grade Modal -->
      <div class="modal-overlay" v-if="grading" @click.self="grading = null">
        <div class="modal-content" style="max-width:500px">
          <h3>✏️ 批改作业</h3>
          <p style="font-size:13px;color:var(--gray-500);margin-bottom:16px">
            学生：{{ grading.studentName || grading.studentId }}
          </p>
          <div class="form-error" v-if="gradeError">{{ gradeError }}</div>
          <div class="form-group">
            <label>分数</label>
            <input v-model.number="gradeForm.score" type="number" class="form-input" min="0" :max="maxScore" step="0.5">
          </div>
          <div class="form-group">
            <label>评语</label>
            <textarea v-model="gradeForm.feedback" class="form-input" rows="3" placeholder="给学生的反馈..."></textarea>
          </div>
          <div class="actions">
            <button class="btn btn-ghost" @click="grading = null">取消</button>
            <button class="btn btn-primary" @click="submitGrade" :disabled="saving">
              {{ saving ? '保存中...' : '确认批改' }}
            </button>
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
const taskId = route.params.taskId
const courseName = ref('')
const taskTitle = ref('')
const maxScore = ref(100)
const submissions = ref([])
const loading = ref(true)
const grading = ref(null)
const saving = ref(false)
const gradeError = ref('')
const gradeForm = reactive({ score: null, feedback: '' })

onMounted(async () => {
  const [s, t] = await Promise.all([
    api.get(`/teacher/task/${taskId}/submissions`),
    api.get(`/student/tasks/${taskId}`).catch(() => ({ data: {} }))
  ])
  submissions.value = s.data || []
  taskTitle.value = t.data?.title || ''
  maxScore.value = t.data?.maxScore || 100
  if (t.data?.courseId) {
    const c = await api.get(`/teacher/course/${t.data.courseId}`).catch(() => ({ data: {} }))
    courseName.value = c.data?.name || ''
  }
  loading.value = false
})

function openGrade(s) {
  grading.value = s
  gradeForm.score = s.score
  gradeForm.feedback = s.feedback || ''
  gradeError.value = ''
}

async function submitGrade() {
  if (gradeForm.score == null) { gradeError.value = '请输入分数'; return }
  saving.value = true
  try {
    await api.put(`/teacher/submission/${grading.value.id}/grade`, {
      score: gradeForm.score,
      feedback: gradeForm.feedback
    })
    grading.value.score = gradeForm.score
    grading.value.feedback = gradeForm.feedback
    grading.value.status = 'graded'
    grading.value = null
  } catch (e) {
    gradeError.value = e.response?.data?.message || '保存失败'
  } finally { saving.value = false }
}

function formatDate(ts) {
  if (!ts) return '-'
  return new Date(ts).toLocaleDateString('zh-CN', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })
}
</script>