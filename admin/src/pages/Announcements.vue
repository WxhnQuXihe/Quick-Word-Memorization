<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createAnnouncement, deleteAnnouncement, listAnnouncements, updateAnnouncement, updateAnnouncementStatus } from '../api/admin'

const rows = ref<any[]>([])
const loading = ref(false)
const dialog = ref(false)
const editing = ref<number | null>(null)
const form = reactive({ title: '', content: '', status: 0 })

async function load() {
  loading.value = true
  try {
    const res = await listAnnouncements()
    rows.value = res.records || []
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editing.value = null
  Object.assign(form, { title: '', content: '', status: 0 })
  dialog.value = true
}

function openEdit(row: any) {
  editing.value = row.id
  Object.assign(form, { title: row.title, content: row.content, status: row.status })
  dialog.value = true
}

async function save() {
  if (!form.title || !form.content) return ElMessage.warning('标题和内容必填')
  if (editing.value) await updateAnnouncement(editing.value, { ...form })
  else await createAnnouncement({ ...form })
  dialog.value = false
  ElMessage.success('发布成功')
  await load()
}

async function toggle(row: any) {
  await updateAnnouncementStatus(row.id, row.status === 1 ? 0 : 1)
  ElMessage.success(row.status === 1 ? '已下线' : '已发布')
  await load()
}

async function remove(row: any) {
  await ElMessageBox.confirm(`删除公告「${row.title}」？`, '确认删除', { type: 'warning' })
  await deleteAnnouncement(row.id)
  ElMessage.success('删除成功')
  await load()
}

onMounted(load)
</script>

<template>
  <div class="page">
    <div class="toolbar">
      <span class="hint">用户端主页会展示最新一条已发布公告</span>
      <el-button type="primary" @click="openCreate">发布公告</el-button>
    </div>
    <div class="panel" v-loading="loading">
      <table>
        <thead><tr><th>标题</th><th>状态</th><th>发布时间</th><th>更新时间</th><th class="right">操作</th></tr></thead>
        <tbody>
          <tr v-for="a in rows" :key="a.id">
            <td><b>{{ a.title }}</b></td>
            <td><span class="pill" :class="{ draft: a.status !== 1 }">{{ a.status === 1 ? '已发布' : '草稿' }}</span></td>
            <td class="mono">{{ (a.publishTime || '-').replace('T', ' ').slice(0, 16) }}</td>
            <td class="mono">{{ (a.updateTime || '-').replace('T', ' ').slice(0, 16) }}</td>
            <td class="right">
              <el-button link @click="openEdit(a)">编辑</el-button>
              <el-button link @click="toggle(a)">{{ a.status === 1 ? '下线' : '发布' }}</el-button>
              <el-button link type="danger" @click="remove(a)">删除</el-button>
            </td>
          </tr>
          <tr v-if="!rows.length"><td colspan="5" class="empty">还没有公告</td></tr>
        </tbody>
      </table>
    </div>
    <el-dialog v-model="dialog" :title="editing ? '编辑公告' : '发布公告'" width="500px">
      <el-form label-position="top">
        <el-form-item label="标题"><el-input v-model="form.title" maxlength="200" show-word-limit /></el-form-item>
        <el-form-item label="内容"><el-input v-model="form.content" type="textarea" :rows="7" maxlength="2000" show-word-limit /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialog = false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page { display: flex; flex-direction: column; gap: 14px; }
.toolbar { display: flex; align-items: center; gap: 10px; }
.toolbar .el-button { margin-left: auto; }
.hint { font-size: 11px; color: var(--muted); }
.panel { background: var(--panel); border: 1px solid var(--line); border-radius: 16px; overflow: hidden; min-height: 100px; }
table { width: 100%; border-collapse: collapse; }
th { text-align: left; font-size: 9px; letter-spacing: .1em; color: var(--muted); font-weight: 500; padding: 10px 16px; border-bottom: 1px solid var(--line); }
td { padding: 13px 16px; font-size: 12px; border-bottom: 1px solid #14191e; }
tr:last-child td { border-bottom: 0; }
tbody tr:hover { background: #10151a; }
.mono { font-family: var(--mono); font-size: 10px; color: var(--muted); }
.pill { font-size: 10px; color: var(--accent); }
.pill.draft { color: var(--warn); }
.right { text-align: right; }
.empty { text-align: center; color: var(--muted); padding: 34px; }
</style>
