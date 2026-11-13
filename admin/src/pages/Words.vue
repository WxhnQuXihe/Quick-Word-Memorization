<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'

const keyword = ref('')
const words = [
  { id: 1, word: 'abandon', phonetic: '/əˈbændən/', pos: 'v. / n.', meaning: '放弃；遗弃 / 放任；狂热' },
  { id: 2, word: 'evaluate', phonetic: '/ɪˈvæljueɪt/', pos: 'v.', meaning: '评估；估价；评价' },
  { id: 3, word: 'integrity', phonetic: '/ɪnˈteɡrəti/', pos: 'n.', meaning: '正直；诚实；完整' }
]
function todo() { ElMessage.info('词条编辑抽屉下一步接入') }
</script>

<template>
  <div class="page">
    <div class="toolbar">
      <el-input v-model="keyword" placeholder="搜索单词 / 释义 / 词性" style="width: 320px" />
      <span class="hint">共 12,271 条词条 · 多词性一行一个</span>
      <el-button type="primary" @click="todo">新增词条</el-button>
    </div>
    <div class="panel">
      <table>
        <thead><tr><th>单词</th><th>音标</th><th>词性</th><th>释义</th><th class="right">操作</th></tr></thead>
        <tbody>
          <tr v-for="w in words" :key="w.id">
            <td><b>{{ w.word }}</b></td>
            <td class="mono">{{ w.phonetic }}</td>
            <td><span class="pos">{{ w.pos }}</span></td>
            <td class="meaning">{{ w.meaning }}</td>
            <td class="right"><el-button link @click="todo">编辑</el-button></td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<style scoped>
.page { display: flex; flex-direction: column; gap: 14px; }
.toolbar { display: flex; align-items: center; gap: 12px; }
.toolbar .el-button { margin-left: auto; }
.hint { font-size: 11px; color: var(--muted); }
.panel { background: var(--panel); border: 1px solid var(--line); border-radius: 16px; overflow: hidden; }
table { width: 100%; border-collapse: collapse; }
th { text-align: left; font-size: 9px; letter-spacing: .1em; color: var(--muted); font-weight: 500; padding: 10px 16px; border-bottom: 1px solid var(--line); }
td { padding: 13px 16px; font-size: 12px; border-bottom: 1px solid #14191e; }
tr:last-child td { border-bottom: 0; }
tbody tr:hover { background: #10151a; }
.mono { font-family: var(--mono); font-size: 10px; color: var(--muted); }
.pos { font-family: var(--mono); font-size: 10px; color: var(--accent); }
.meaning { color: #b9c2cc; }
.right { text-align: right; }
</style>
