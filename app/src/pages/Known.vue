<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { listKnown, removeKnown } from '../api/app'

const router = useRouter()
const rows = ref<any[]>([])
const loading = ref(true)

async function load() {
  loading.value = true
  try {
    const res = await listKnown()
    rows.value = res.records || []
  } finally {
    loading.value = false
  }
}
async function remove(id: number) {
  await removeKnown(id)
  rows.value = rows.value.filter((r) => r.id !== id)
  showToast('已移出熟词本')
}
onMounted(load)
</script>

<template>
  <div class="page list-page">
    <header class="topbar">
      <button class="back" @click="router.back()">‹</button>
      <div class="title"><b>熟词本</b><span>标熟是为了以后不再见</span></div>
    </header>
    <div v-if="loading" class="card loading">正在翻熟词本…</div>
    <div v-else-if="!rows.length" class="card empty-card"><div class="empty-mark">✓</div><h2 class="serif">还没有熟词</h2><p>背单词时点「标熟」，词就会来这里。</p><van-button type="primary" round @click="router.replace('/')">去背单词</van-button></div>
    <div v-else class="list">
      <article v-for="r in rows" :key="r.id" class="item">
        <div class="item-main"><b class="serif">{{ r.word }}</b><span class="phonetic">{{ r.phoneticUk || r.phoneticUs }}</span><div class="senses"><div v-for="(s, i) in r.senses" :key="i" class="sense"><span class="pos">{{ s.pos }}</span><span>{{ s.meaning }}</span></div></div></div>
        <button class="check" @click="remove(r.id)">✓</button>
      </article>
    </div>
  </div>
</template>

<style scoped>
.list-page { padding-bottom: 40px; }
.loading { color: var(--muted); font-size: 13px; }
.empty-card { text-align: center; padding: 40px 24px; }
.empty-mark { font-size: 28px; color: var(--ok); margin-bottom: 10px; }
.empty-card h2 { font-size: 24px; margin: 0 0 6px; font-weight: 500; }
.empty-card p { color: var(--muted); font-size: 12px; margin: 0 0 18px; }
.item { display: flex; align-items: flex-start; gap: 10px; background: var(--surface); border: 1px solid var(--line); border-radius: 18px; padding: 16px 18px; margin-bottom: 10px; }
.item-main { flex: 1; min-width: 0; }
.item b { font-size: 24px; font-weight: 500; }
.phonetic { display: block; font-size: 11px; color: var(--muted); margin-top: 2px; }
.senses { margin-top: 8px; display: flex; flex-direction: column; gap: 3px; }
.sense { display: grid; grid-template-columns: 30px 1fr; gap: 6px; font-size: 12px; line-height: 1.6; }
.pos { color: var(--accent); font-family: "Iowan Old Style", Georgia, serif; font-style: italic; }
.check { border: 0; background: transparent; color: var(--ok); font-size: 18px; padding: 2px; }
</style>
