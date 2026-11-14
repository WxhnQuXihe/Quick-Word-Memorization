<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createUser, deleteUser, listUsers, resetPassword, updateUserStatus } from '../api/admin'

const rows = ref<any[]>([])
const total = ref(0)
const loading = ref(false)
const keyword = ref('')
const dialog = ref(false)
const form = reactive({ username: '', password: '', nickname: '' })

async function load() {
  loading.value = true
  try {
    const res = await listUsers(1, 50, keyword.value)
    rows.value = res.records || []
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

async function submitCreate() {
  if (!form.username || form.password.length < 6) {
    ElMessage.warning('用户名必填，密码至少 6 位')
    return
  }
  await createUser({ ...form })
  ElMessage.success('用户已创建')
  dialog.value = false
  form.username = ''
  form.password = ''
  form.nickname = ''
  await load()
}

async function reset(row: any) {
  const { value } = await ElMessageBox.prompt(`给 ${row.username} 设置新密码`, '重置密码', {
    inputType: 'password',
    inputPattern: /^.{6,32}$/,
    inputErrorMessage: '密码长度 6~32 位'
  })
  await resetPassword(row.id, value)
  ElMessage.success('密码已重置')
}

async function toggle(row: any) {
  const next = row.status === 1 ? 0 : 1
  await updateUserStatus(row.id, next)
  row.status = next
  ElMessage.success(next === 1 ? '已启用' : '已禁用')
}

async function remove(row: any) {
  await ElMessageBox.confirm(
    `删除「${row.username}」后，学习记录、收藏、熟词本和反馈都会清空，且不可恢复。`,
    '确认删除用户',
    { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消' }
  )
  await deleteUser(row.id)
  ElMessage.success('删除成功')
  await load()
}

onMounted(load)
</script>

<template>
  <div class="page">
    <div class="toolbar">
      <el-input v-model="keyword" placeholder="搜索用户名 / 昵称" style="width: 260px" clearable @keyup.enter="load" />
      <el-button @click="load">搜索</el-button>
      <span class="hint">共 {{ total }} 个用户 · 不开放注册</span>
      <el-button type="primary" @click="dialog = true">新建用户</el-button>
    </div>

    <div class="panel">
      <table v-loading="loading">
        <thead><tr><th>用户名</th><th>昵称</th><th>状态</th><th>创建时间</th><th>最近登录</th><th class="right">操作</th></tr></thead>
        <tbody>
          <tr v-for="u in rows" :key="u.id">
            <td><b>{{ u.username }}</b></td>
            <td>{{ u.nickname || '-' }}</td>
            <td><span class="pill" :class="{ off: u.status !== 1 }">{{ u.status === 1 ? '正常' : '禁用' }}</span></td>
            <td class="mono">{{ (u.createTime || '').replace('T', ' ').slice(0, 16) }}</td>
            <td class="mono">{{ (u.lastLoginTime || '-').replace('T', ' ').slice(0, 16) }}</td>
            <td class="right">
              <el-button link @click="reset(u)">重置密码</el-button>
              <el-button link @click="toggle(u)">{{ u.status === 1 ? '禁用' : '启用' }}</el-button>
              <el-button link type="danger" @click="remove(u)">删除</el-button>
            </td>
          </tr>
          <tr v-if="!rows.length"><td colspan="6" class="empty">还没有用户，点右上角新建一个。</td></tr>
        </tbody>
      </table>
    </div>

    <el-dialog v-model="dialog" title="新建用户" width="420px">
      <el-form label-position="top">
        <el-form-item label="用户名"><el-input v-model="form.username" placeholder="登录账号" /></el-form-item>
        <el-form-item label="昵称"><el-input v-model="form.nickname" placeholder="显示名称，可空" /></el-form-item>
        <el-form-item label="初始密码"><el-input v-model="form.password" type="password" show-password placeholder="至少 6 位" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="submitCreate">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page { display: flex; flex-direction: column; gap: 14px; }
.toolbar { display: flex; align-items: center; gap: 10px; }
.toolbar .el-button:last-child { margin-left: auto; }
.hint { font-size: 11px; color: var(--muted); }
.panel { background: var(--panel); border: 1px solid var(--line); border-radius: 16px; overflow: hidden; min-height: 120px; }
table { width: 100%; border-collapse: collapse; }
th { text-align: left; font-size: 9px; letter-spacing: .1em; color: var(--muted); font-weight: 500; padding: 10px 16px; border-bottom: 1px solid var(--line); }
td { padding: 13px 16px; font-size: 12px; border-bottom: 1px solid #14191e; }
tr:last-child td { border-bottom: 0; }
tbody tr:hover { background: #10151a; }
.mono { font-family: var(--mono); font-size: 10px; color: var(--muted); }
.pill { font-size: 10px; color: var(--accent); }
.pill.off { color: #e07a5f; }
.right { text-align: right; }
.empty { text-align: center; color: var(--muted); font-size: 12px; padding: 40px; }
</style>
