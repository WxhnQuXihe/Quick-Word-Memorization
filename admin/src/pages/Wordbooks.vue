<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { createWordbook, listWordbooks, updateWordbook, updateWordbookStatus } from '../api/admin'

const rows = ref<any[]>([])
const loading = ref(false)
const dialog = ref(false)
const editing = ref<number | null>(null)
const form = reactive({ code: '', name: '', description: '', difficulty: 3, sort: 100 })

async function load() {
  loading.value = true
  try {
    rows.value = await listWordbooks()
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editing.value = null
  Object.assign(form, { code: '', name: '', description: '', difficulty: 3, sort: 100 })
  dialog.value = true
}

function openEdit(row: any) {
  editing.value = row.id
  Object.assign(form, {
    code: row.code,
    name: row.name,
    description: row.description || '',
    difficulty: row.difficulty || 3,
    sort: row.sort || 100
  })
  dialog.value = true
}

async function save() {
  if (!form.code || !form.name) return ElMessage.warning('编码和名称必填')
  if (editing.value) await updateWordbook(editing.value, { ...form })
  else await createWordbook({ ...form })
  dialog.value = false
  ElMessage.success('已保存')
  await load()
}

async function toggle(row: any) {
  await updateWordbookStatus(row.id, row.status === 1 ? 0 : 1)
  ElMessage.success(row.status === 1 ? '已下架' : '已上架')
  await load()
}

onMounted(load)
</script>

<template>
  <div class="page">
    <div class="toolbar">
      <div class="hint">{{ rows.length }} 本词书 · 词条用 Excel 导入</div>
      <el-button type="primary" @click="openCreate">新建词书</el-button>
    </div>
    <div class="panel" v-loading="loading">
      <table>
        <thead><tr><th>词书</th><th>词数</th><th>状态</th><th>排序</th><th>最近更新</th><th class="right">操作</th></tr></thead>
        <tbody>
          <tr v-for="b in rows" :key="b.id">
            <td><div class="book"><b>{{ b.name }}</b><span>{{ b.code }}</span></div></td>
            <td class="mono">{{ (b.wordCount || 0).toLocaleString() }}</td>
            <td><span class="pill" :class="{ draft: b.status !== 1 }">{{ b.status === 1 ? '已上架' : '草稿' }}</span></td>
            <td class="mono">{{ b.sort }}</td>
            <td class="mono">{{ b.updateTime ? b.updateTime.replace('T', ' ').slice(0, 16) : '-' }}</td>
            <td class="right">
              <el-button link @click="openEdit(b)">编辑</el-button>
              <el-button link @click="toggle(b)">{{ b.status === 1 ? '下架' : '上架' }}</el-button>
            </td>
          </tr>
          <tr v-if="!rows.length"><td colspan="6" class="empty">还没有词书，点右上角新建。</td></tr>
        </tbody>
      </table>
    </div>

    <el-dialog v-model="dialog" :title="editing ? '编辑词书' : '新建词书'" width="460px">
      <el-form label-position="top">
        <el-form-item label="编码"><el-input v-model="form.code" placeholder="cet4 / cet6 / phrase" :disabled="!!editing" /></el-form-item>
        <el-form-item label="名称"><el-input v-model="form.name" placeholder="四级大纲" /></el-form-item>
        <el-form-item label="简介"><el-input v-model="form.description" type="textarea" :rows="2" /></el-form-item>
        <div class="two">
          <el-form-item label="难度 1~5"><el-input-number v-model="form.difficulty" :min="1" :max="5" /></el-form-item>
          <el-form-item label="排序"><el-input-number v-model="form.sort" :min="0" :max="9999" /></el-form-item>
        </div>
      </el-form>
      <template #footer><el-button @click="dialog = false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page { display: flex; flex-direction: column; gap: 14px; }
.toolbar { display: flex; align-items: center; gap: 12px; }
.hint { font-size: 11px; color: var(--muted); }
.toolbar .el-button { margin-left: auto; }
.panel { background: var(--panel); border: 1px solid var(--line); border-radius: 16px; overflow: hidden; min-height: 100px; }
table { width: 100%; border-collapse: collapse; }
th { text-align: left; font-size: 9px; letter-spacing: .1em; color: var(--muted); font-weight: 500; padding: 10px 16px; border-bottom: 1px solid var(--line); }
td { padding: 13px 16px; font-size: 12px; border-bottom: 1px solid #14191e; }
tr:last-child td { border-bottom: 0; }
tbody tr:hover { background: #10151a; }
.mono { font-family: var(--mono); font-size: 10px; color: var(--muted); }
.book { display: flex; flex-direction: column; gap: 2px; }
.book b { font-weight: 500; }
.book span { font-family: var(--mono); font-size: 9px; color: var(--muted); }
.pill { font-size: 10px; color: var(--accent); }
.pill.draft { color: var(--warn); }
.right { text-align: right; }
.empty { text-align: center; color: var(--muted); padding: 34px; }
.two { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
</style>
