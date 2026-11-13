<script setup lang="ts">
import { ElMessage } from 'element-plus'
const rows = [
  { id: 1, user: 'QX', content: '希望能加一个「只看单词」的开关', time: '今天 12:06', status: 0 },
  { id: 2, user: 'wang', content: '词组那本有的释义重复了', time: '昨天 18:20', status: 0 }
]
function reply() { ElMessage.info('回复抽屉下一步接入') }
</script>

<template>
  <div class="page">
    <div class="panel">
      <div class="head"><h2>用户反馈</h2><span>2 OPEN</span></div>
      <div v-for="r in rows" :key="r.id" class="row">
        <div class="avatar">{{ r.user.slice(0, 1) }}</div>
        <div class="body"><b>{{ r.user }}</b><p>{{ r.content }}</p><span>{{ r.time }}</span></div>
        <el-button link @click="reply">回复</el-button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page { display: flex; flex-direction: column; gap: 14px; }
.panel { background: var(--panel); border: 1px solid var(--line); border-radius: 16px; overflow: hidden; }
.head { display: flex; padding: 14px 16px; border-bottom: 1px solid var(--line); }
.head h2 { font-size: 12px; margin: 0; font-weight: 600; }
.head span { margin-left: auto; font-family: var(--mono); font-size: 9px; color: var(--muted); }
.row { display: flex; align-items: flex-start; gap: 12px; padding: 16px; border-bottom: 1px solid #14191e; }
.row:last-child { border-bottom: 0; }
.avatar { width: 30px; height: 30px; border-radius: 50%; background: #1a2027; border: 1px solid #2a333d; display: grid; place-items: center; font-size: 10px; color: var(--accent); }
.body { flex: 1; }
.body b { font-size: 11px; font-weight: 500; }
.body p { margin: 5px 0; font-size: 12px; color: #b9c2cc; }
.body span { font-family: var(--mono); font-size: 9px; color: var(--muted); }
</style>
