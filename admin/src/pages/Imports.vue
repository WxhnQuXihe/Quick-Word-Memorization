<script setup lang="ts">
import { ElMessage } from 'element-plus'

const logs = [
  { id: 1, file: 'cet6.xlsx', book: '六级大纲', total: 5515, success: 5515, fail: 0, status: '成功', time: '14:32' },
  { id: 2, file: 'phrase.xlsx', book: '四六级词组', total: 2329, success: 2301, fail: 28, status: '部分失败', time: '昨天' }
]
function pick() { ElMessage.info('选择文件：接口下一步接入') }
</script>

<template>
  <div class="page">
    <div class="drop" @click="pick">
      <div class="drop-icon">↑</div>
      <b>拖入词书文件</b>
      <span>.xlsx / .csv · 最大 12MB · 一行一个词条，释义一行一个词性</span>
      <el-button type="primary">选择文件</el-button>
    </div>
    <div class="panel">
      <div class="head"><h2>导入记录</h2><span>IMPORT_LOG</span></div>
      <table>
        <thead><tr><th>文件</th><th>词书</th><th>总行数</th><th>成功</th><th>失败</th><th>状态</th><th>时间</th></tr></thead>
        <tbody>
          <tr v-for="l in logs" :key="l.id">
            <td class="mono">{{ l.file }}</td><td>{{ l.book }}</td>
            <td class="mono">{{ l.total }}</td><td class="mono ok">{{ l.success }}</td>
            <td class="mono" :class="{ bad: l.fail }">{{ l.fail }}</td>
            <td><span class="pill" :class="{ bad: l.fail }">{{ l.status }}</span></td>
            <td class="mono">{{ l.time }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<style scoped>
.page { display: flex; flex-direction: column; gap: 16px; }
.drop { border: 1px dashed #39434d; background: var(--panel); padding: 34px; text-align: center; cursor: pointer; }
.drop:hover { border-color: var(--accent); }
.drop-icon { width: 38px; height: 38px; border: 1px solid var(--line); display: grid; place-items: center; margin: 0 auto 12px; color: var(--accent); font-size: 18px; }
.drop b { display: block; font-size: 13px; margin-bottom: 5px; }
.drop span { display: block; font-size: 10px; color: var(--muted); margin-bottom: 12px; }
.panel { background: var(--panel); border: 1px solid var(--line); border-radius: 16px; overflow: hidden; }
.head { display: flex; align-items: center; padding: 14px 16px; border-bottom: 1px solid var(--line); }
.head h2 { font-size: 12px; margin: 0; font-weight: 600; }
.head span { margin-left: auto; font-family: var(--mono); font-size: 9px; color: var(--muted); }
table { width: 100%; border-collapse: collapse; }
th { text-align: left; font-size: 9px; letter-spacing: .1em; color: var(--muted); font-weight: 500; padding: 10px 16px; border-bottom: 1px solid var(--line); }
td { padding: 12px 16px; font-size: 11px; border-bottom: 1px solid #14191e; }
tr:last-child td { border-bottom: 0; }
.mono { font-family: var(--mono); font-size: 10px; color: var(--muted); }
.ok { color: var(--accent); }
.bad { color: #e07a5f; }
.pill { font-size: 10px; color: var(--accent); }
.pill.bad { color: #e07a5f; }
</style>
