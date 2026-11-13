<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createWord, deleteWord, listWords, updateWord } from '../api/admin'

const rows = ref<any[]>([])
const total = ref(0)
const pageNo = ref(1)
const pageSize = ref(20)
const keyword = ref('')
const loading = ref(false)
const drawer = ref(false)
const editing = ref<number | null>(null)
const form = reactive({ word: '', phoneticUk: '', phoneticUs: '', pos: '', meaning: '', exampleEn: '', exampleCn: '' })

async function load() {
  loading.value = true
  try {
    const res = await listWords(pageNo.value, pageSize.value, keyword.value)
    rows.value = res.records || []
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editing.value = null
  Object.assign(form, { word: '', phoneticUk: '', phoneticUs: '', pos: '', meaning: '', exampleEn: '', exampleCn: '' })
  drawer.value = true
}

function openEdit(row: any) {
  editing.value = row.id
  Object.assign(form, {
    word: row.word,
    phoneticUk: row.phoneticUk || '',
    phoneticUs: row.phoneticUs || '',
    pos: row.pos || '',
    meaning: row.meaning || '',
    exampleEn: row.exampleEn || '',
    exampleCn: row.exampleCn || ''
  })
  drawer.value = true
}

async function save() {
  if (!form.word || !form.meaning) return ElMessage.warning('单词和释义必填')
  if (editing.value) await updateWord(editing.value, { ...form })
  else await createWord({ ...form })
  drawer.value = false
  ElMessage.success('已保存')
  await load()
}

async function remove(row: any) {
  await ElMessageBox.confirm(`删除「${row.word}」？`, '确认删除', { type: 'warning' })
  await deleteWord(row.id)
  ElMessage.success('已删除')
  await load()
}

function search() {
  pageNo.value = 1
  load()
}

onMounted(load)
</script>

<template>
  <div class="page">
    <div class="toolbar">
      <el-input v-model="keyword" placeholder="搜索单词 / 释义" style="width: 300px" clearable @keyup.enter="search" />
      <el-button @click="search">搜索</el-button>
      <span class="hint">共 {{ total }} 条 · 释义一行一个词性</span>
      <el-button type="primary" @click="openCreate">新增词条</el-button>
    </div>
    <div class="panel" v-loading="loading">
      <table>
        <thead><tr><th>单词</th><th>音标</th><th>释义</th><th>更新</th><th class="right">操作</th></tr></thead>
        <tbody>
          <tr v-for="w in rows" :key="w.id">
            <td><b>{{ w.word }}</b></td>
            <td class="mono">{{ w.phoneticUk || w.phoneticUs || '-' }}</td>
            <td class="meaning"><span v-for="(line, i) in (w.meaning || '').split('\n')" :key="i" class="line">{{ line }}</span></td>
            <td class="mono">{{ (w.updateTime || '').replace('T', ' ').slice(0, 16) }}</td>
            <td class="right"><el-button link @click="openEdit(w)">编辑</el-button><el-button link @click="remove(w)">删除</el-button></td>
          </tr>
          <tr v-if="!rows.length"><td colspan="5" class="empty">没有词条</td></tr>
        </tbody>
      </table>
    </div>
    <div class="pager">
      <el-button :disabled="pageNo <= 1" @click="pageNo--; load()">上一页</el-button>
      <span class="mono">{{ pageNo }} / {{ Math.max(1, Math.ceil(total / pageSize)) }}</span>
      <el-button :disabled="pageNo * pageSize >= total" @click="pageNo++; load()">下一页</el-button>
    </div>

    <el-drawer v-model="drawer" :title="editing ? '编辑词条' : '新增词条'" size="480px">
      <el-form label-position="top">
        <el-form-item label="单词"><el-input v-model="form.word" placeholder="abandon" /></el-form-item>
        <div class="two">
          <el-form-item label="英式音标"><el-input v-model="form.phoneticUk" placeholder="/əˈbændən/" /></el-form-item>
          <el-form-item label="美式音标"><el-input v-model="form.phoneticUs" placeholder="/əˈbændən/" /></el-form-item>
        </div>
        <el-form-item label="首个词性"><el-input v-model="form.pos" placeholder="v." /></el-form-item>
        <el-form-item label="释义（一行一个词性）">
          <el-input v-model="form.meaning" type="textarea" :rows="6" placeholder="v. 放弃；遗弃&#10;n. 放任；狂热" />
        </el-form-item>
        <el-form-item label="英文例句"><el-input v-model="form.exampleEn" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="例句翻译"><el-input v-model="form.exampleCn" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="drawer = false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template>
    </el-drawer>
  </div>
</template>

<style scoped>
.page { display: flex; flex-direction: column; gap: 14px; }
.toolbar { display: flex; align-items: center; gap: 10px; }
.toolbar .el-button:last-child { margin-left: auto; }
.hint { font-size: 11px; color: var(--muted); }
.panel { background: var(--panel); border: 1px solid var(--line); border-radius: 16px; overflow: hidden; min-height: 100px; }
table { width: 100%; border-collapse: collapse; }
th { text-align: left; font-size: 9px; letter-spacing: .1em; color: var(--muted); font-weight: 500; padding: 10px 16px; border-bottom: 1px solid var(--line); }
td { padding: 12px 16px; font-size: 12px; border-bottom: 1px solid #14191e; vertical-align: top; }
tr:last-child td { border-bottom: 0; }
tbody tr:hover { background: #10151a; }
.mono { font-family: var(--mono); font-size: 10px; color: var(--muted); }
.meaning { color: #b9c2cc; max-width: 520px; }
.line { display: block; line-height: 1.6; }
.right { text-align: right; white-space: nowrap; }
.empty { text-align: center; color: var(--muted); padding: 34px; }
.pager { display: flex; align-items: center; gap: 10px; justify-content: flex-end; }
.two { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
</style>
