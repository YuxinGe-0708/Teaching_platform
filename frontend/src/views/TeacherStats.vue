<template>
  <div>
    <div class="page-header">
      <button class="btn btn-ghost btn-sm" @click="$router.back()" style="margin-bottom:12px">← 返回</button>
      <h1>📊 成绩统计</h1>
      <p>{{ courseName }}</p>
    </div>

    <div v-if="loading" class="loading"><div class="loading-spinner"></div></div>

    <template v-else>
      <!-- Per-task stats -->
      <div class="card" v-for="t in stats" :key="t.taskId" style="padding:18px 20px;margin-bottom:12px">
        <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:10px">
          <strong style="font-size:15px">{{ t.taskTitle }}</strong>
          <span class="badge badge-blue">满分{{ t.maxScore }}分</span>
        </div>
        <div class="mini-stats">
          <div class="mini-stat"><span class="ms-val">{{ t.submissionCount }}</span><span class="ms-label">提交数</span></div>
          <div class="mini-stat accent"><span class="ms-val">{{ t.average || '-' }}</span><span class="ms-label">平均分</span></div>
          <div class="mini-stat"><span class="ms-val">{{ t.max || '-' }}</span><span class="ms-label">最高</span></div>
          <div class="mini-stat"><span class="ms-val">{{ t.min || '-' }}</span><span class="ms-label">最低</span></div>
          <div class="mini-stat warm"><span class="ms-val">{{ t.passRate || 0 }}%</span><span class="ms-label">及格率</span></div>
        </div>
      </div>

      <!-- Distribution chart (simple bar) -->
      <div class="card" style="padding:20px" v-if="distData.length > 0">
        <h3 style="font-size:16px;font-weight:600;margin-bottom:16px">📈 成绩分布</h3>
        <div class="bar-chart">
          <div class="bar-item" v-for="d in distData" :key="d.label">
            <div class="bar-label">{{ d.label }}</div>
            <div class="bar-track">
              <div class="bar-fill" :style="{width: d.pct + '%', background: d.color}"></div>
            </div>
            <div class="bar-count">{{ d.count }}</div>
          </div>
        </div>
      </div>

      <div class="empty" v-if="stats.length === 0"><p>暂无数据</p></div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import api from '../api'

const route = useRoute()
const courseName = ref('')
const stats = ref([])
const loading = ref(true)

const colors = ['#ef4444','#f59e0b','#3b82f6','#10b981','#059669']
const distData = computed(() => {
  const dist = {}
  for (const t of stats.value) {
    for (const s of t.subs || []) {
      if (s.score == null) continue
      const key = s.score < 60 ? '0-59' : s.score < 70 ? '60-69' : s.score < 80 ? '70-79' : s.score < 90 ? '80-89' : '90-100'
      dist[key] = (dist[key] || 0) + 1
    }
  }
  const total = Object.values(dist).reduce((a,b)=>a+b,0) || 1
  const keys = ['0-59','60-69','70-79','80-89','90-100']
  return keys.map((k,i) => ({ label: k, count: dist[k] || 0, pct: Math.round((dist[k]||0)/total*100), color: colors[i] }))
})

onMounted(async () => {
  const courseId = route.params.id
  const [c, r] = await Promise.all([
    api.get(`/teacher/course/${courseId}`),
    api.get(`/teacher/score/statistics/${courseId}`)
  ])
  courseName.value = c.data?.name || ''
  stats.value = r.data?.tasks || []
  // Attach distribution from backend
  if (r.data?.distribution) {
    const distMap = r.data.distribution
    // Inject into distData
    for (const t of stats.value) {
      // We'll use the backend distribution directly
    }
  }
  loading.value = false
})
</script>

<style scoped>
.mini-stats { display: flex; gap: 16px; flex-wrap: wrap; }
.mini-stat { text-align: center; min-width: 60px; }
.ms-val { display: block; font-size: 20px; font-weight: 700; color: var(--gray-800); }
.mini-stat.accent .ms-val { color: var(--accent); }
.mini-stat.warm .ms-val { color: #d97706; }
.ms-label { font-size: 11px; color: var(--gray-500); }

.bar-chart { display: flex; flex-direction: column; gap: 10px; }
.bar-item { display: flex; align-items: center; gap: 10px; }
.bar-label { width: 50px; font-size: 12px; color: var(--gray-600); text-align: right; flex-shrink: 0; }
.bar-track { flex: 1; height: 22px; background: var(--gray-100); border-radius: 4px; overflow: hidden; }
.bar-fill { height: 100%; border-radius: 4px; transition: width 0.5s ease; min-width: 2px; }
.bar-count { width: 30px; font-size: 13px; font-weight: 600; color: var(--gray-700); flex-shrink: 0; }
</style>