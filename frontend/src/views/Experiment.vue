<template>
  <div>
    <div class="page-header">
      <button class="btn btn-ghost btn-sm" @click="$router.back()" style="margin-bottom:12px">← 返回</button>
      <h1 v-if="task">💻 {{ task.title }}</h1>
      <p v-if="task">
        {{ task.courseName }}
        · 满分{{ task.maxScore }}分
        · 截止{{ formatDate(task.endTime) }}
      </p>
    </div>

    <div v-if="loading" class="loading"><div class="loading-spinner"></div></div>

    <template v-else-if="task">
      <div class="experiment-layout">
        <!-- Left: Problem Description -->
        <div class="experiment-left">
          <div class="card" style="padding:20px;margin-bottom:16px">
            <h3 style="font-size:15px;font-weight:600;margin-bottom:8px;color:var(--gray-800)">📖 题目描述</h3>
            <div style="font-size:14px;color:var(--gray-600);line-height:1.7;white-space:pre-wrap">
              {{ task.description || '暂无详细描述' }}
            </div>
          </div>

          <div class="card" style="padding:20px;margin-bottom:16px">
            <h3 style="font-size:15px;font-weight:600;margin-bottom:8px;color:var(--gray-800)">🧪 测试用例</h3>
            <div v-if="testCases.length === 0" style="font-size:13px;color:var(--gray-500)">
              暂无预设测试用例，评测将使用默认用例。
            </div>
            <div v-for="(tc, i) in testCases" :key="i" class="test-case-card">
              <div class="tc-header">用例 {{ i + 1 }}</div>
              <div class="tc-row"><span class="tc-label">输入：</span><code>{{ tc.input || '(空)' }}</code></div>
              <div class="tc-row"><span class="tc-label">期望输出：</span><code>{{ tc.expectedOutput || '(空)' }}</code></div>
            </div>
          </div>
        </div>

        <!-- Right: Code Editor + Results -->
        <div class="experiment-right">
          <CodeEditor
            v-model="code"
            v-model:language="language"
            :running="judging"
            :lastResult="lastJudgeResult"
            @run="handleJudge"
            @submit="handleSubmit"
          />

          <!-- Judge Results -->
          <div v-if="judgeResult" class="judge-results" style="margin-top:12px">
            <div class="judge-summary" :class="'judge-' + judgeResult.status.toLowerCase()">
              <div class="judge-status-icon">
                {{ judgeResult.status === 'AC' ? '✅' : judgeResult.status === 'WA' ? '❌' : '⚠️' }}
              </div>
              <div class="judge-status-info">
                <div class="judge-status-text">{{ getStatusText(judgeResult.status) }}</div>
                <div class="judge-status-meta">
                  通过 {{ judgeResult.passedCases }}/{{ judgeResult.totalCases }} 个用例
                  · 耗时 {{ judgeResult.timeUsedMs?.toFixed(0) || '0' }}ms
                  · 得分 {{ judgeResult.score?.toFixed(1) || '0' }}
                </div>
              </div>
            </div>

            <div class="case-results" v-if="judgeResult.caseResults">
              <div class="case-result-item" v-for="cr in judgeResult.caseResults" :key="cr.caseIndex"
                   :class="'case-' + cr.status.toLowerCase()">
                <div class="case-result-header">
                  <span>用例 {{ cr.caseIndex }}</span>
                  <span class="badge" :class="getCaseBadge(cr.status)">{{ cr.status }}</span>
                  <span style="font-size:12px;color:var(--gray-400);margin-left:auto">{{ cr.timeMs?.toFixed(0) || '0' }}ms</span>
                </div>
                <div class="case-result-body" v-if="cr.status !== 'AC'">
                  <div class="tc-row"><span class="tc-label">输入：</span><code>{{ cr.input || '(空)' }}</code></div>
                  <div class="tc-row"><span class="tc-label">期望：</span><code>{{ cr.expectedOutput || '(空)' }}</code></div>
                  <div class="tc-row"><span class="tc-label">实际：</span><code>{{ cr.actualOutput || '(空)' }}</code></div>
                </div>
              </div>
            </div>

            <div v-if="judgeResult.errorMessage" class="form-error" style="margin-top:12px">
              {{ judgeResult.errorMessage }}
            </div>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import api, { studentApi } from '../api'
import CodeEditor from '../components/CodeEditor.vue'

const route = useRoute()
const task = ref(null)
const loading = ref(true)
const code = ref('')
const language = ref('python')
const judging = ref(false)
const judgeResult = ref(null)
const lastJudgeResult = ref(null)

const defaultCode = {
  python: '# 请在此编写 Python 代码\n\ndef main():\n    print("Hello World")\n\nif __name__ == "__main__":\n    main()\n',
  java: '// 请在此编写 Java 代码\npublic class Main {\n    public static void main(String[] args) {\n        System.out.println("Hello World");\n    }\n}\n',
  cpp: '// 请在此编写 C++ 代码\n#include <iostream>\nusing namespace std;\n\nint main() {\n    cout << "Hello World" << endl;\n    return 0;\n}\n',
  javascript: '// 请在此编写 JavaScript 代码\nconsole.log("Hello World");\n'
}

const testCases = computed(() => {
  if (!task.value?.description) return []
  return parseTestCases(task.value.description)
})

onMounted(async () => {
  const id = route.params.id
  const [t, s] = await Promise.all([
    studentApi.getTaskDetail(id),
    studentApi.getSubmission(id)
  ])
  task.value = t.data
  if (s.data?.content) {
    code.value = s.data.content
  } else {
    code.value = defaultCode.python
  }
  loading.value = false
})

async function handleJudge() {
  judging.value = true
  judgeResult.value = null
  try {
    const res = await api.post('/judge/submit', {
      taskId: task.value.id,
      code: code.value,
      language: language.value
    })
    judgeResult.value = res.data
    lastJudgeResult.value = res.data
  } catch (e) {
    judgeResult.value = { status: 'IE', errorMessage: e.response?.data?.message || '评测失败' }
  } finally {
    judging.value = false
  }
}

async function handleSubmit() {
  await handleJudge()
  try {
    await studentApi.submitTask(task.value.id, code.value)
  } catch (e) { /* ignore */ }
}

function parseTestCases(desc) {
  const cases = []
  if (!desc || !desc.includes('输入:')) return cases
  const parts = desc.split('---')
  for (const part of parts) {
    const tc = {}
    for (const line of part.trim().split('\n')) {
      if (line.startsWith('输入:')) tc.input = line.substring(3).trim()
      if (line.startsWith('输出:')) tc.expectedOutput = line.substring(3).trim()
    }
    if (Object.keys(tc).length > 0) cases.push(tc)
  }
  return cases
}

function getStatusText(status) {
  const map = { AC: '通过 (Accepted)', WA: '答案错误 (Wrong Answer)', CE: '编译错误 (Compile Error)', RE: '运行错误 (Runtime Error)', TLE: '超时 (Time Limit Exceeded)', MLE: '内存超限 (Memory Limit Exceeded)', IE: '内部错误 (Internal Error)' }
  return map[status] || status
}
function getCaseBadge(status) {
  if (status === 'AC') return 'badge-green'
  if (status === 'WA') return 'badge-danger'
  return 'badge-warm'
}
function formatDate(ts) {
  if (!ts) return '-'
  return new Date(ts).toLocaleDateString('zh-CN', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })
}
</script>

<style scoped>
.experiment-layout {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  align-items: start;
}

@media (max-width: 1024px) {
  .experiment-layout { grid-template-columns: 1fr; }
}

.test-case-card {
  background: var(--gray-50);
  border: 1px solid var(--gray-200);
  border-radius: 8px;
  padding: 12px;
  margin-top: 8px;
}
.tc-header {
  font-size: 13px;
  font-weight: 600;
  color: var(--gray-700);
  margin-bottom: 6px;
}
.tc-row {
  font-size: 13px;
  margin-top: 4px;
  display: flex;
  gap: 6px;
}
.tc-label {
  color: var(--gray-500);
  flex-shrink: 0;
}
.tc-row code {
  font-size: 12px;
  word-break: break-all;
}

.judge-summary {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  border-radius: 10px;
  margin-bottom: 10px;
}
.judge-ac { background: #ecfdf5; border: 1px solid #a7f3d0; }
.judge-wa, .judge-ce, .judge-re, .judge-tle, .judge-mle, .judge-ie { background: #fef2f2; border: 1px solid #fecaca; }
.judge-status-icon { font-size: 24px; }
.judge-status-text { font-weight: 600; font-size: 15px; }
.judge-status-meta { font-size: 12px; color: var(--gray-500); margin-top: 2px; }

.case-result-item {
  border: 1px solid var(--gray-200);
  border-radius: 8px;
  margin-bottom: 6px;
  overflow: hidden;
}
.case-ac { border-left: 3px solid var(--accent); }
.case-wa { border-left: 3px solid var(--danger); }
.case-re, .case-tle, .case-mle, .case-ce { border-left: 3px solid var(--warning); }
.case-result-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  font-size: 13px;
  font-weight: 500;
}
.case-result-body {
  padding: 8px 12px;
  background: var(--gray-50);
  border-top: 1px solid var(--gray-100);
}
</style>
