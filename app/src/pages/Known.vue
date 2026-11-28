<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { listKnown, removeKnown } from '../api/app'
import PhoneticText from '../components/PhoneticText.vue'

const PAGE_SIZE = 20
const router = useRouter()
const rows = ref<any[]>([])
const loading = ref(true)
const pageNo = ref(1)
const total = ref(0)
const pageCount = computed(() => Math.max(1, Math.ceil(total.value / PAGE_SIZE)))
const expanded = ref<Set<number>>(new Set())

function toggle(id: number) {
  const next = new Set(expanded.value)
  if (next.has(id)) next.delete(id)
  else next.add(id)
  expanded.value = next
}

async function load(page = pageNo.value) {
  loading.value = true
  try {
    let target = Math.max(1, page)
    let res = await listKnown(target, PAGE_SIZE)
    total.value = Number(res?.total || 0)
    // 移出本页最后一条后服务端会返回空页，此时退到新的最后一页。
    if (!(res?.records || []).length && target > 1) {
      target = Math.max(1, Math.ceil(total.value / PAGE_SIZE))
      res = await listKnown(target, PAGE_SIZE)
      total.value = Number(res?.total || 0)
    }
    rows.value = res?.records || []
    pageNo.value = target
  } finally {
    loading.value = false
  }
}
function go(page: number) {
  if (loading.value || page < 1 || page > pageCount.value || page === pageNo.value) return
  window.scrollTo({ top: 0, behavior: 'smooth' })
  load(page)
}
async function remove(id: number) {
  await removeKnown(id)
  showToast('已移出熟词本')
  await load(pageNo.value)
}
onMounted(load)
</script>

<template>
  <div class="page list-page">
    <header class="topbar">
      <button class="back" @click="router.back()">‹</button>
      <div class="title"><b>熟词本</b><span>标熟是为了以后不再见</span></div>
      <span v-if="total" class="count">{{ total }} 词</span>
    </header>
    <div v-if="loading && !rows.length" class="card loading">正在翻熟词本…</div>
    <div v-else-if="!rows.length" class="card empty-card"><div class="empty-mark">✓</div><h2 class="serif">还没有熟词</h2><p>背单词时点「标熟」，词就会来这里。</p><van-button type="primary" round @click="router.replace('/')">去背单词</van-button></div>
    <div v-else class="list" :class="{ dim: loading }">
      <article v-for="r in rows" :key="r.id" class="item">
        <div class="item-main">
          <div class="word-head" role="button" tabindex="0" :aria-expanded="expanded.has(r.id)" @click="toggle(r.id)" @keydown.enter.prevent="toggle(r.id)" @keydown.space.prevent="toggle(r.id)">
            <b class="serif">{{ r.word }}</b>
            <PhoneticText class="phonetic" small :uk="r.phoneticUk" :us="r.phoneticUs" />
            <span class="meaning-toggle">{{ expanded.has(r.id) ? '收起' : '释义' }}</span>
          </div>
          <div v-if="expanded.has(r.id)" class="senses">
            <div v-for="(s, i) in r.senses" :key="i" class="sense"><span class="pos">{{ s.pos }}</span><span>{{ s.meaning }}</span></div>
            <div v-if="!r.senses?.length" class="sense-empty">暂无释义</div>
          </div>
        </div>
        <button class="check" @click.stop="remove(r.id)">✓</button>
      </article>
    </div>
    <nav v-if="pageCount > 1" class="pager" aria-label="熟词本翻页">
      <button class="pager-btn" :disabled="loading || pageNo <= 1" @click="go(pageNo - 1)">‹ 上一页</button>
      <span class="pager-info"><b>{{ pageNo }}</b><i>/</i>{{ pageCount }}<em>共 {{ total }} 词</em></span>
      <button class="pager-btn" :disabled="loading || pageNo >= pageCount" @click="go(pageNo + 1)">下一页 ›</button>
    </nav>
  </div>
</template>

<style scoped>
.list-page { padding-bottom: 120px; }
.count { font-size: 12px; color: var(--muted); white-space: nowrap; }
.loading { color: var(--muted); font-size: 13px; }
.list { transition: opacity .15s ease; }
.list.dim { opacity: .5; pointer-events: none; }
.empty-card { text-align: center; padding: 40px 24px; }
.empty-mark { font-size: 28px; color: var(--ok); margin-bottom: 10px; }
.empty-card h2 { font-size: 24px; margin: 0 0 6px; font-weight: 500; }
.empty-card p { color: var(--muted); font-size: 12px; margin: 0 0 18px; }
.item { display: flex; align-items: flex-start; gap: 10px; background: var(--surface); border: 1px solid var(--line); border-radius: 18px; padding: 16px 18px; margin-bottom: 10px; }
.item-main { flex: 1; min-width: 0; }
.item b { font-size: 24px; font-weight: 500; }
.word-head { display: flex; align-items: baseline; gap: 8px; cursor: pointer; }
.phonetic { font-size: 11px; color: var(--muted); }
.meaning-toggle { margin-left: auto; flex: none; font-size: 10px; color: var(--muted); border: 1px solid var(--line); border-radius: 999px; padding: 3px 7px; line-height: 1.2; }
.senses { margin-top: 8px; display: flex; flex-direction: column; gap: 3px; }
.sense-empty { color: var(--muted); font-size: 12px; }
.sense { display: grid; grid-template-columns: 30px 1fr; gap: 6px; font-size: 12px; line-height: 1.6; }
.pos { color: var(--accent); font-family: "Iowan Old Style", Georgia, serif; font-style: italic; }
.check { border: 0; background: transparent; color: var(--ok); font-size: 18px; padding: 2px; }
.pager { position: fixed; left: 50%; bottom: calc(14px + env(safe-area-inset-bottom)); transform: translateX(-50%); z-index: 5; width: min(calc(100% - 36px), 560px); display: flex; align-items: center; gap: 6px; padding: 7px 8px; background: var(--surface); border: 1px solid var(--line); border-radius: 18px; box-shadow: 0 10px 30px rgba(26, 23, 19, .10); }
.pager-btn { flex: 1; border: 0; background: transparent; color: var(--ink); font-size: 12px; padding: 9px 6px; border-radius: 12px; }
.pager-btn:not(:disabled):active { background: rgba(26, 23, 19, .06); }
.pager-btn:disabled { color: var(--muted); opacity: .45; }
.pager-info { flex: 0 0 auto; min-width: 68px; text-align: center; font-size: 11px; color: var(--muted); line-height: 1.25; }
.pager-info b { font-size: 14px; font-weight: 500; color: var(--ink); }
.pager-info i { font-style: normal; margin: 0 3px; }
.pager-info em { display: block; font-style: normal; font-size: 10px; }
</style>
