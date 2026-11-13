<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { importWordbook, listImports, listWordbooks } from '../api/admin'

const books = ref<any[]>([])
const bookId = ref<number | null>(null)
const logs = ref<any[]>([])
const loading = ref(false)
const importing = ref(false)
const result = ref<any>(null)
const fileInput = ref<HTMLInputElement | null>(null)

async function load() {
  loading.value = true
  try {
    books.value = await listWordbooks()
    if (!bookId.value && books.value.length) bookId.value = books.value[0].id
    const res = await listImports()
    logs.value = res.records || []
  } finally {
    loading.value = false
  }
}

function pick() {
  fileInput.value?.click()
}

async function onFile(e: Event) {
  const file = (e.target as HTMLInputElement).files?.[0]
  if (!file || !bookId.value) return
  importing.value = true
  try {
    result.value = await importWordbook(bookId.value, file)
    ElMessage.success(`导入完成：成功 ${result.value.successRows}，失败 ${result.value.failRows}`)
    await load()
  } finally {
    importing.value = false
    if (fileInput.value) fileInput.value.value = ''
  }
}

onMounted(load)
</script>

<template>
  <div class="page">
    <div class="toolbar">
      <span class="hint">导入到</span>
      <el-select v-model="bookId" placeholder="选择词书" style="width: 220px">
        <el-option v-for="b in books" :key="b.id" :label="b.name" :value="b.id" />
      </el-select>
      <span class="hint">一行一个词条 · 释义一行一个词性</span>
    </div>

    <div class="drop" :class="{ busy: importing }" @click="pick">
      <div class="drop-icon">↑</div>
      <b>{{ importing ? '正在导入…' : '拖入词书文件' }}</b>
      <span>.xlsx / .csv · 最大 12MB</span>
      <el-button type="primary" :loading="importing">选择文件</el-button>
      <input ref="fileInput" type="file" accept=".xlsx,.xls,.csv" hidden @change="onFile" />
    </div>

    <div v-if="result" class="result">
      <div><b>总行数</b><span>{{ result.totalRows }}</span></div>
      <div><b>成功</b><span class="ok">{{ result.successRows }}</span></div>
      <div><b>失败</b><span class="bad">{{ result.failRows }}</span></div>
      <div v-if="result.errors?.length" class="errors">
        <div v-for="(e, i) in result.errors" :key="i">{{ e }}</div>
      </div>
    </div>

    <div class="panel" v-loading="loading">
      <div class="head"><h2>导入记录</h2><span>IMPORT_LOG</span></div>
      <table>
        <thead><tr><th>文件</th><th>词书</th><th>总行数</th><th>成功</th><th>失败</th><th>状态</th><th>时间</th></tr></thead>
        <tbody>
          <tr v-for="l in logs" :key="l.id">
            <td class="mono">{{ l.fileName }}</td>
            <td>{{ books.find((b) => b.id === l.wordbookId)?.name || l.wordbookId }}</td>
            <td class="mono">{{ l.totalRows }}</td>
            <td class="mono ok">{{ l.successRows }}</td>
            <td class="mono" :class="{ bad: l.failRows }">{{ l.failRows }}</td>
            <td><span class="pill" :class="{ bad: l.status !== 1 }">{{ l.status === 1 ? '成功' : l.status === 2 ? '部分失败' : '失败' }}</span></td>
            <td class="mono">{{ (l.createTime || '').replace('T', ' ').slice(0, 16) }}</td>
          </tr>
          <tr v-if="!logs.length"><td colspan="7" class="empty">还没有导入记录</td></tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<style scoped>
.page { display: flex; flex-direction: column; gap: 16px; }
.toolbar { display: flex; align-items: center; gap: 10px; }
.hint { font-size: 11px; color: var(--muted); }
.drop { border: 1px dashed #39434d; background: var(--panel); padding: 34px; text-align: center; cursor: pointer; }
.drop:hover { border-color: var(--accent); }
.drop.busy { opacity: .7; pointer-events: none; }
.drop-icon { width: 38px; height: 38px; border: 1px solid var(--line); display: grid; place-items: center; margin: 0 auto 12px; color: var(--accent); font-size: 18px; }
.drop b { display: block; font-size: 13px; margin-bottom: 5px; }
.drop span { display: block; font-size: 10px; color: var(--muted); margin-bottom: 12px; }
.result { display: flex; gap: 24px; align-items: flex-start; padding: 14px 18px; border: 1px solid var(--line); background: var(--panel); font-size: 12px; }
.result b { display: block; font-size: 10px; color: var(--muted); font-weight: 400; margin-bottom: 4px; }
.result span { font-family: var(--mono); font-size: 16px; }
.ok { color: var(--accent); }
.bad { color: #e07a5f; }
.errors { margin-left: auto; max-width: 50%; max-height: 80px; overflow: auto; font-size: 10px; color: #e07a5f; line-height: 1.6; }
.panel { background: var(--panel); border: 1px solid var(--line); border-radius: 16px; overflow: hidden; }
.head { display: flex; align-items: center; padding: 14px 16px; border-bottom: 1px solid var(--line); }
.head h2 { font-size: 12px; margin: 0; font-weight: 600; }
.head span { margin-left: auto; font-family: var(--mono); font-size: 9px; color: var(--muted); }
table { width: 100%; border-collapse: collapse; }
th { text-align: left; font-size: 9px; letter-spacing: .1em; color: var(--muted); font-weight: 500; padding: 10px 16px; border-bottom: 1px solid var(--line); }
td { padding: 12px 16px; font-size: 11px; border-bottom: 1px solid #14191e; }
tr:last-child td { border-bottom: 0; }
.mono { font-family: var(--mono); font-size: 10px; color: var(--muted); }
.pill { font-size: 10px; color: var(--accent); }
.pill.bad { color: #e07a5f; }
.empty { text-align: center; color: var(--muted); padding: 34px; }
</style>
