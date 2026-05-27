<template>
  <div class="code-editor-wrapper">
    <div class="editor-toolbar">
      <div class="lang-selector">
        <button v-for="lang in languages" :key="lang.value"
                class="btn btn-sm" :class="lang.value === language ? 'btn-primary' : 'btn-ghost'"
                @click="selectLanguage(lang.value)">
          {{ lang.label }}
        </button>
      </div>
      <div class="editor-actions">
        <span v-if="lastResult" class="badge" :class="getResultBadge(lastResult.status)" style="margin-right:8px">
          {{ lastResult.status }}
        </span>
        <button class="btn btn-accent btn-sm" @click="$emit('run')" :disabled="running">
          {{ running ? '⏳ 评测中...' : '▶ 运行评测' }}
        </button>
        <button class="btn btn-primary btn-sm" @click="$emit('submit')" :disabled="running" style="margin-left:6px">
          📤 提交
        </button>
      </div>
    </div>
    <div ref="editorContainer" class="editor-container"></div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import { EditorView, keymap, lineNumbers, highlightActiveLine, highlightActiveLineGutter } from '@codemirror/view'
import { EditorState } from '@codemirror/state'
import { defaultKeymap, history, historyKeymap } from '@codemirror/commands'
import { syntaxHighlighting, defaultHighlightStyle, bracketMatching, indentOnInput } from '@codemirror/language'
import { closeBrackets } from '@codemirror/autocomplete'
import { oneDark } from '@codemirror/theme-one-dark'

// Lazy load language support
const langModules = {
  python: () => import('@codemirror/lang-python').then(m => m.python()),
  java: () => import('@codemirror/lang-java').then(m => m.java()),
  cpp: () => import('@codemirror/lang-cpp').then(m => m.cpp()),
  javascript: () => import('@codemirror/lang-javascript').then(m => m.javascript())
}

const props = defineProps({
  modelValue: { type: String, default: '' },
  language: { type: String, default: 'python' },
  running: { type: Boolean, default: false },
  lastResult: { type: Object, default: null }
})

const emit = defineEmits(['update:modelValue', 'update:language', 'run', 'submit'])

const editorContainer = ref(null)
let editorView = null

const languages = [
  { label: '🐍 Python', value: 'python' },
  { label: '☕ Java', value: 'java' },
  { label: '⚡ C++', value: 'cpp' },
  { label: '📜 JavaScript', value: 'javascript' }
]

async function createEditor() {
  const lang = await langModules[props.language]()

  const state = EditorState.create({
    doc: props.modelValue,
    extensions: [
      lineNumbers(),
      highlightActiveLine(),
      highlightActiveLineGutter(),
      bracketMatching(),
      closeBrackets(),
      indentOnInput(),
      history(),
      keymap.of([...defaultKeymap, ...historyKeymap]),
      syntaxHighlighting(defaultHighlightStyle, { fallback: true }),
      oneDark,
      lang,
      EditorView.updateListener.of(update => {
        if (update.docChanged) {
          emit('update:modelValue', update.state.doc.toString())
        }
      })
    ]
  })

  editorView = new EditorView({
    state,
    parent: editorContainer.value
  })
}

async function selectLanguage(lang) {
  emit('update:language', lang)
  if (editorView) {
    const langExt = await langModules[lang]()
    editorView.dispatch({
      effects: EditorState.reconfigure.of([
        editorView.state.facet(EditorState.extensions).filter(e => {
          const name = e.constructor?.name || ''
          return !name.includes('Language') && !name.includes('Lang')
        }),
        langExt
      ].flat())
    })
  }
}

onMounted(() => {
  createEditor()
})

onBeforeUnmount(() => {
  if (editorView) editorView.destroy()
})

function getResultBadge(status) {
  if (status === 'AC') return 'badge-green'
  if (status === 'WA') return 'badge-danger'
  if (status === 'TLE' || status === 'MLE') return 'badge-warm'
  if (status === 'CE' || status === 'RE') return 'badge-warm'
  return 'badge-blue'
}
</script>

<style scoped>
.code-editor-wrapper {
  border: 1px solid #e5e7eb;
  border-radius: var(--radius);
  overflow: hidden;
  background: #282c34;
}

.editor-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background: #21252b;
  border-bottom: 1px solid #181a1f;
}

.lang-selector {
  display: flex;
  gap: 4px;
}

.lang-selector .btn-ghost {
  color: #abb2bf;
  font-size: 12px;
  padding: 4px 10px;
}
.lang-selector .btn-ghost:hover {
  background: #3a3f4b;
  color: #fff;
}
.lang-selector .btn-primary {
  font-size: 12px;
  padding: 4px 10px;
  background: #4f46e5;
}

.editor-actions {
  display: flex;
  align-items: center;
}

.editor-container {
  height: 400px;
  overflow: auto;
}

.editor-container :deep(.cm-editor) {
  height: 100%;
  font-size: 14px;
}
.editor-container :deep(.cm-scroller) {
  font-family: "JetBrains Mono", "Consolas", "Courier New", monospace;
  line-height: 1.6;
}
.editor-container :deep(.cm-gutters) {
  border-right: 1px solid #181a1f;
  background: #21252b;
  color: #636d83;
}
</style>
