<script setup lang="ts">
import { ElMessage } from 'element-plus'

const books = [
  { id: 1, code: 'CET4', name: '四级大纲', words: 4427, progress: 72, status: 1, updated: '2h ago' },
  { id: 2, code: 'CET6', name: '六级大纲', words: 5515, progress: 48, status: 1, updated: '1d ago' },
  { id: 3, code: 'PHRASE', name: '四六级词组', words: 2329, progress: 31, status: 0, updated: '3d ago' }
]

function todo(action: string) {
  ElMessage.info(`${action}：接口下一步接入`)
}
</script>

<template>
  <div class="page">
    <div class="toolbar">
      <div class="hint">3 本词书 · 词条由 Excel 导入，不在后台逐条新建</div>
      <el-button type="primary" @click="todo('新建词书')">新建词书</el-button>
    </div>
    <div class="panel">
      <table>
        <thead><tr><th>词书</th><th>词数</th><th>覆盖率</th><th>状态</th><th>更新</th><th class="right">操作</th></tr></thead>
        <tbody>
          <tr v-for="b in books" :key="b.id">
            <td><div class="book"><b>{{ b.name }}</b><span>{{ b.code }}</span></div></td>
            <td class="mono">{{ b.words.toLocaleString() }}</td>
            <td><div class="bar"><i :style="{ width: b.progress + '%' }"></i></div><span class="mono">{{ b.progress }}%</span></td>
            <td><span class="pill" :class="{ draft: !b.status }">{{ b.status ? '已上架' : '草稿' }}</span></td>
            <td class="mono">{{ b.updated }}</td>
            <td class="right">
              <el-button link @click="todo('编辑')">编辑</el-button>
              <el-button link @click="todo('导入')">导入</el-button>
              <el-button link @click="todo(b.status ? '下架' : '上架')">{{ b.status ? '下架' : '上架' }}</el-button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<style scoped>
.page { display: flex; flex-direction: column; gap: 14px; }
.toolbar { display: flex; align-items: center; gap: 12px; }
.hint { font-size: 11px; color: var(--muted); }
.toolbar .el-button { margin-left: auto; }
.panel { background: var(--panel); border: 1px solid var(--line); border-radius: 16px; overflow: hidden; }
table { width: 100%; border-collapse: collapse; }
th { text-align: left; font-size: 9px; letter-spacing: .1em; color: var(--muted); font-weight: 500; padding: 10px 16px; border-bottom: 1px solid var(--line); }
td { padding: 13px 16px; font-size: 12px; border-bottom: 1px solid #14191e; }
tr:last-child td { border-bottom: 0; }
tbody tr:hover { background: #10151a; }
.mono { font-family: var(--mono); font-size: 10px; color: var(--muted); }
.book { display: flex; flex-direction: column; gap: 2px; }
.book b { font-weight: 500; }
.book span { font-family: var(--mono); font-size: 9px; color: var(--muted); }
.bar { width: 100%; height: 4px; background: #1a2026; border-radius: 99px; overflow: hidden; }
.bar i { display: block; height: 100%; background: var(--accent); }
.pill { display: inline-flex; align-items: center; gap: 5px; font-size: 10px; color: var(--accent); }
.pill::before { content: ""; width: 5px; height: 5px; border-radius: 50%; background: var(--accent); }
.pill.draft { color: var(--warn); }
.pill.draft::before { background: var(--warn); }
.right { text-align: right; }
</style>
