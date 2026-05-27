<template>
  <div class="ai-chat">
    <div class="chat-header">
      <div>
        <span style="font-size:20px;margin-right:8px">🤖</span>
        <strong>AI 助教</strong>
        <span style="font-size:12px;color:var(--gray-400);margin-left:8px">基于 GPT · 懂你的课程</span>
      </div>
      <button class="btn btn-ghost btn-sm" @click="$emit('clear')" title="清除对话">🔄 新对话</button>
    </div>

    <div class="chat-messages" ref="msgContainer">
      <div v-if="messages.length === 0" class="chat-welcome">
        <div style="font-size:40px;margin-bottom:12px">🤖</div>
        <h3>AI 助教为你服务</h3>
        <p>你可以问我课程相关的问题，比如：</p>
        <div class="suggestions">
          <button v-for="q in suggestions" :key="q" class="btn btn-outline btn-sm"
                  @click="sendMessage(q)">{{ q }}</button>
        </div>
      </div>

      <div v-for="(msg, i) in messages" :key="i" class="chat-bubble" :class="msg.role">
        <div class="bubble-avatar">{{ msg.role === 'user' ? '👤' : '🤖' }}</div>
        <div class="bubble-content" v-html="renderMarkdown(msg.content)"></div>
      </div>

      <div v-if="loading" class="chat-bubble assistant">
        <div class="bubble-avatar">🤖</div>
        <div class="bubble-content typing">
          <span></span><span></span><span></span>
        </div>
      </div>
    </div>

    <div class="chat-input-area">
      <textarea v-model="input" class="form-input chat-input"
                placeholder="输入你的问题，按 Enter 发送，Shift+Enter 换行..."
                rows="1"
                @keydown.enter.exact.prevent="sendMessage()"
                @input="autoResize"
                :disabled="loading"
                ref="inputEl"></textarea>
      <button class="btn btn-primary" @click="sendMessage()" :disabled="loading || !input.trim()">
        <span v-if="!loading">发送</span>
        <span v-else>思考中</span>
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick, watch } from 'vue'
import api from '../api'

const props = defineProps({
  courseId: { type: [String, Number], required: true },
  courseName: { type: String, default: '未知课程' }
})

defineEmits(['clear'])

const messages = ref([])
const input = ref('')
const loading = ref(false)
const msgContainer = ref(null)
const inputEl = ref(null)

const suggestions = [
  '这门课的核心知识点有哪些？',
  '帮我总结一下最近学的内容',
  '这道题我不会，可以给我一些思路吗？',
  '请解释一下什么是数据结构'
]

async function sendMessage(text) {
  const msg = text || input.value.trim()
  if (!msg || loading.value) return

  messages.value.push({ role: 'user', content: msg })
  input.value = ''
  loading.value = true

  await nextTick()
  scrollToBottom()

  try {
    const res = await api.post('/ai/chat', {
      message: msg,
      courseId: String(props.courseId),
      courseName: props.courseName
    })
    messages.value.push({ role: 'assistant', content: res.data.reply })
  } catch (e) {
    messages.value.push({ role: 'assistant', content: '抱歉，AI 助手暂时无法回复。' + (e.response?.data?.message || '') })
  } finally {
    loading.value = false
    await nextTick()
    scrollToBottom()
  }
}

function autoResize() {
  const el = inputEl.value
  if (el) {
    el.style.height = 'auto'
    el.style.height = Math.min(el.scrollHeight, 120) + 'px'
  }
}

function scrollToBottom() {
  if (msgContainer.value) {
    msgContainer.value.scrollTop = msgContainer.value.scrollHeight
  }
}

function renderMarkdown(text) {
  if (!text) return ''
  // Simple markdown: bold, italic, code blocks, inline code
  return text
    .replace(/```(\w*)\n([\s\S]*?)```/g, '<pre><code>$2</code></pre>')
    .replace(/`([^`]+)`/g, '<code>$1</code>')
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/\*(.+?)\*/g, '<em>$1</em>')
    .replace(/\n/g, '<br>')
}
</script>

<style scoped>
.ai-chat {
  display: flex;
  flex-direction: column;
  height: 520px;
  background: #fff;
  border-radius: var(--radius);
  border: 1px solid #f0ebe3;
  overflow: hidden;
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 20px;
  background: linear-gradient(135deg, var(--primary-bg), var(--accent-bg));
  border-bottom: 1px solid #d4e4f7;
  font-size: 15px;
  color: var(--gray-800);
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px 20px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.chat-welcome {
  text-align: center;
  padding: 20px;
  color: var(--gray-500);
}
.chat-welcome h3 {
  font-size: 17px;
  color: var(--gray-700);
  margin-bottom: 6px;
}
.chat-welcome p {
  font-size: 13px;
  margin-bottom: 14px;
}
.suggestions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: center;
}

.chat-bubble {
  display: flex;
  gap: 10px;
  max-width: 85%;
}
.chat-bubble.user {
  align-self: flex-end;
  flex-direction: row-reverse;
}
.chat-bubble.assistant {
  align-self: flex-start;
}

.bubble-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  flex-shrink: 0;
  background: var(--gray-100);
}
.chat-bubble.user .bubble-avatar {
  background: var(--primary-bg);
}

.bubble-content {
  padding: 10px 14px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.6;
  word-break: break-word;
}
.chat-bubble.user .bubble-content {
  background: var(--primary);
  color: #fff;
  border-bottom-right-radius: 4px;
}
.chat-bubble.assistant .bubble-content {
  background: var(--gray-50);
  color: var(--gray-800);
  border: 1px solid #f0ebe3;
  border-bottom-left-radius: 4px;
}

.bubble-content :deep(pre) {
  background: #1e293b;
  color: #e2e8f0;
  padding: 12px;
  border-radius: 8px;
  font-size: 13px;
  line-height: 1.5;
  overflow-x: auto;
  margin: 6px 0;
}
.bubble-content :deep(code) {
  font-family: "JetBrains Mono", "Consolas", monospace;
  font-size: 13px;
}
.chat-bubble.user .bubble-content :deep(code) {
  background: rgba(255,255,255,0.2);
  color: #fff;
}

.typing span {
  display: inline-block;
  width: 7px; height: 7px;
  border-radius: 50%;
  background: var(--gray-400);
  margin: 0 2px;
  animation: typing 1.4s infinite;
}
.typing span:nth-child(2) { animation-delay: 0.2s; }
.typing span:nth-child(3) { animation-delay: 0.4s; }
@keyframes typing {
  0%, 60%, 100% { transform: translateY(0); opacity: 0.4; }
  30% { transform: translateY(-8px); opacity: 1; }
}

.chat-input-area {
  display: flex;
  gap: 10px;
  padding: 12px 16px;
  border-top: 1px solid #f0ebe3;
  background: #fafaf9;
}
.chat-input {
  flex: 1;
  resize: none;
  min-height: 40px;
  max-height: 120px;
}
</style>
