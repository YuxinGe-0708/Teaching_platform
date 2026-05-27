<template>
  <div>
    <div class="page-header">
      <button class="btn btn-ghost btn-sm" @click="$router.back()" style="margin-bottom:12px">← 返回</button>
      <h1 v-if="task">📝 {{ task.title }}</h1>
      <p v-if="task">
        {{ task.courseName }}
        · 满分{{ task.maxScore }}分
        · 截止{{ formatDate(task.endTime) }}
        <span class="badge" :class="getStatusClass(task)" style="margin-left:8px">{{ getStatusText(task) }}</span>
      </p>
    </div>

    <div v-if="loading" class="loading"><div class="loading-spinner"></div></div>
    <template v-else-if="task">
      <!-- Programming task: redirect hint -->
      <div v-if="task.type === 'programming'" class="card" style="padding:24px;text-align:center;margin-bottom:24px">
        <div style="font-size:40px;margin-bottom:12px">💻</div>
        <h3>这是一个编程实训题</h3>
        <p style="font-size:14px;color:var(--gray-500);margin:10px 0 20px">
          建议使用在线实训环境编写和评测代码
        </p>
        <button class="btn btn-primary btn-lg" @click="$router.push(`/experiment/${task.id}`)">
          进入实训环境
        </button>
        <p style="font-size:12px;color:var(--gray-400);margin-top:12px">
          或在下方直接粘贴代码提交
        </p>
      </div>

      <div class="card" style="padding:20px;margin-bottom:24px">
        <h3 style="font-size:16px;font-weight:600;margin-bottom:8px;color:var(--gray-800)">📖 作业描述</h3>
        <div style="font-size:14px;color:var(--gray-600);line-height:1.7;white-space:pre-wrap">
          {{ task.description || '暂无详细描述' }}
        </div>
      </div>

      <div class="card" style="padding:20px;margin-bottom:24px">
        <h3 style="font-size:16px;font-weight:600;margin-bottom:12px;color:var(--gray-800)">✏️ 提交作业</h3>
        <div class="form-error" v-if="submitError">{{ submitError }}</div>
        <div class="form-success" v-if="submitOk">{{ submitOk }}</div>
        <div class="form-group">
          <textarea v-model="content" class="form-input" rows="10"
                    :placeholder="task.type === 'programming' ? '请粘贴你的代码...' : '请输入你的答案...'"
                    style="resize:vertical;font-family:'JetBrains Mono','Consolas',monospace;font-size:14px"></textarea>
        </div>
        <button class="btn btn-primary" @click="handleSubmit" :disabled="submitting">
          {{ submitting ? '提交中...' : '提交答案' }}
        </button>
        <span v-if="submission && !submitOk" style="margin-left:12px;font-size:13px;color:var(--gray-500)">
          上次提交于 {{ formatDate(submission.submittedAt) }}
        </span>
      </div>

      <div class="card" style="padding:20px" v-if="submission">
        <h3 style="font-size:16px;font-weight:600;margin-bottom:12px;color:var(--gray-800)">📋 提交记录</h3>
        <div v-if="submission.score !== null && submission.score !== undefined" style="display:flex;align-items:center;gap:12px">
          <div style="font-size:24px;font-weight:700;color:var(--accent)">{{ submission.score }}</div>
          <div style="font-size:14px;color:var(--gray-500)">/ {{ task.maxScore }} 分</div>
          <span class="badge" :class="getJudgeClass(submission.judgeResult)">
            {{ submission.judgeResult || '已评分' }}
          </span>
        </div>
        <div v-else style="color:var(--gray-500);font-size:14px;display:flex;align-items:center;gap:8px">
          <span class="status status-active"><span class="status-dot"></span></span>
          已提交，等待批改...
        </div>
        <div v-if="submission.feedback" style="margin-top:12px;padding:12px;background:var(--warm);border-radius:8px;font-size:14px;border:1px solid #fde68a">
          <strong>💬 教师评语：</strong>{{ submission.feedback }}
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { studentApi } from '../api'

const route = useRoute()
const router = useRouter()
const task = ref(null)
const submission = ref(null)
const content = ref('')
const loading = ref(true)
const submitting = ref(false)
const submitError = ref('')
const submitOk = ref('')

onMounted(async () => {
  const id = route.params.id
  const [t, s] = await Promise.all([
    studentApi.getTaskDetail(id),
    studentApi.getSubmission(id)
  ])
  task.value = t.data
  if (s.data) {
    submission.value = s.data
    content.value = s.data.content || ''
  }
  loading.value = false
})

async function handleSubmit() {
  if (!content.value.trim()) {
    submitError.value = '提交内容不能为空'
    submitOk.value = ''
    return
  }
  submitError.value = ''
  submitOk.value = ''
  submitting.value = true
  try {
    const res = await studentApi.submitTask(task.value.id, content.value)
    submission.value = res.data
    submitOk.value = '提交成功！'
    setTimeout(() => { submitOk.value = '' }, 3000)
  } catch (e) {
    submitError.value = e.response?.data?.message || '提交失败，请重试'
  } finally {
    submitting.value = false
  }
}

function formatDate(ts) {
  if (!ts) return '-'
  return new Date(ts).toLocaleDateString('zh-CN', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })
}
function getStatusClass(t) {
  if (!t || !t.endTime) return 'badge-green'
  return new Date(t.endTime) < new Date() ? 'badge-danger' : 'badge-green'
}
function getStatusText(t) {
  if (!t || !t.endTime) return '进行中'
  return new Date(t.endTime) < new Date() ? '已截止' : '进行中'
}
function getJudgeClass(result) {
  if (result === 'AC') return 'badge-green'
  if (result === 'WA') return 'badge-danger'
  if (result === 'CE' || result === 'RE' || result === 'TLE') return 'badge-warm'
  return 'badge-blue'
}
</script>
