<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { listFeedback, replyFeedback } from '../api/admin'

const rows = ref<any[]>([])
const loading = ref(false)
const dialog = ref(false)
const current = ref<any>(null)
const reply = ref('')

async function load() {
  loading.value = true
  try {
    const res = await listFeedback()
    rows.value = res.records || []
  } finally {
    loading.value = false
  }
}

function open(row: any) {
  current.value = row
  reply.value = row.reply || ''
  dialog.value = true
}

async function send() {
  if (!reply.value.trim()) return ElMessage.warning('请输入回复')
  await replyFeedback(current.value.id, reply.value.trim())
  ElMessage.success('已回复')
  dialog.value = false
  await load()
}

onMounted(load)
</script>

<template>
  <div class="page">
    <div class="panel" v-loading="loading">
      <div class="head"><h2>用户反馈</h2><span>{{ rows.filter(r => r.status === 0).length }} OPEN</span></div>
      <div v-for="r in rows" :key="r.id" class="row">
        <div class="avatar">{{ (r.nickname || r.username || '?').slice(0, 1) }}</div>
        <div class="body">
          <b>{{ r.nickname || r.username || ('用户 #' + r.userId) }}</b>
          <span class="uid">@{{ r.username || r.userId }}</span>
          <p>{{ r.content }}</p>
          <span>{{ (r.createTime || '').replace('T', ' ').slice(0, 16) }} · {{ r.status === 1 ? '已回复' : '待处理' }}</span>
          <div v-if="r.reply" class="reply">回复：{{ r.reply }}</div>
        </div>
        <el-button link @click="open(r)">{{ r.status === 1 ? '查看' : '回复' }}</el-button>
      </div>
      <div v-if="!rows.length" class="empty">还没有反馈</div>
    </div>

    <el-dialog v-model="dialog" title="回复反馈" width="460px">
      <p class="content">{{ current?.content }}</p>
      <el-input v-model="reply" type="textarea" :rows="4" placeholder="写点回复…" />
      <template #footer><el-button @click="dialog = false">取消</el-button><el-button type="primary" @click="send">发送</el-button></template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page { display: flex; flex-direction: column; gap: 14px; }
.panel { background: var(--panel); border: 1px solid var(--line); border-radius: 16px; overflow: hidden; min-height: 120px; }
.head { display: flex; padding: 14px 16px; border-bottom: 1px solid var(--line); }
.head h2 { font-size: 12px; margin: 0; font-weight: 600; }
.head span { margin-left: auto; font-family: var(--mono); font-size: 9px; color: var(--muted); }
.row { display: flex; align-items: flex-start; gap: 12px; padding: 16px; border-bottom: 1px solid #14191e; }
.row:last-child { border-bottom: 0; }
.avatar { width: 30px; height: 30px; border-radius: 50%; background: #1a2027; border: 1px solid #2a333d; display: grid; place-items: center; font-size: 10px; color: var(--accent); }
.body { flex: 1; }
.body b { font-size: 11px; font-weight: 500; }
.uid { display: inline-block; margin-left: 6px; font-family: var(--mono); font-size: 9px; color: var(--muted); }
.body p { margin: 5px 0; font-size: 12px; color: #b9c2cc; }
.body span { font-family: var(--mono); font-size: 9px; color: var(--muted); }
.reply { margin-top: 7px; font-size: 11px; color: var(--accent); }
.content { font-size: 12px; color: var(--muted); line-height: 1.7; margin: 0 0 12px; }
.empty { text-align: center; color: var(--muted); padding: 34px; }
</style>
