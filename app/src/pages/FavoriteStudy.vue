<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { listFavorites, removeFavorite } from '../api/app'
import PhoneticText from '../components/PhoneticText.vue'

const router = useRouter()
const rows = ref<any[]>([])
const loading = ref(true)
const page = ref(1)
const nextArmed = ref(false)
const showExit = ref(false)
let timer: number | undefined

const pageSize = 10
const pages = computed(() => Math.max(1, Math.ceil(rows.value.length / pageSize)))
const cards = computed(() => rows.value.slice((page.value - 1) * pageSize, page.value * pageSize))
const allWordsHidden = computed(() => cards.value.length > 0 && cards.value.every((c) => c.wordHidden))
const allMeaningsHidden = computed(() => cards.value.length > 0 && cards.value.every((c) => c.meaningHidden))

function decorate(row: any) {
  return { ...row, wordHidden: false, meaningHidden: false }
}

async function load() {
  loading.value = true
  try {
    const res = await listFavorites(1, 500)
    rows.value = (res.records || []).map(decorate)
  } finally {
    loading.value = false
  }
}

async function unstar(row: any) {
  await removeFavorite(row.id)
  rows.value = rows.value.filter((r) => r.id !== row.id)
  if (page.value > pages.value) page.value = pages.value
  showToast('已移出收藏夹')
}

function toggleWords() {
  const hide = !allWordsHidden.value
  cards.value.forEach((c) => { c.wordHidden = hide })
}
function toggleMeanings() {
  const hide = !allMeaningsHidden.value
  cards.value.forEach((c) => { c.meaningHidden = hide })
}
function next() {
  if (page.value >= pages.value) return
  if (!nextArmed.value) {
    nextArmed.value = true
    timer = window.setTimeout(() => { nextArmed.value = false }, 3000)
    return
  }
  clearTimeout(timer)
  nextArmed.value = false
  page.value += 1
  window.scrollTo({ top: 0, behavior: 'smooth' })
}
function exit() {
  showExit.value = false
  router.back()
}
onMounted(load)
</script>

<template>
  <div class="page fav-study">
    <header class="topbar">
      <button class="back" @click="showExit = true">‹</button>
      <div class="title"><b>背收藏夹</b><span v-if="rows.length">第 {{ page }} / {{ pages }} 组 · 本组 {{ cards.length }} 词</span></div>
      <div class="meta">收藏夹</div>
    </header>
    <div class="progress-line"><i :style="{ width: `${(page / pages) * 100}%` }"></i></div>

    <div v-if="loading" class="card loading">正在翻收藏夹…</div>
    <div v-else-if="!rows.length" class="card empty-card">
      <div class="empty-mark">☆</div><h2 class="serif">收藏夹是空的</h2><p>先在背单词时点星标，收藏的词会自动汇到这里。</p>
      <van-button type="primary" round @click="router.replace('/')">去背单词</van-button>
    </div>

    <main v-else class="grid">
      <article v-for="(card, index) in cards" :key="card.id" class="word-card">
        <div class="card-top">
          <span class="index">{{ String((page - 1) * pageSize + index + 1).padStart(2, '0') }}</span>
          <button class="star" @click="unstar(card)">★ <span>移出</span></button>
        </div>
        <div class="word-zone" @click="card.wordHidden = !card.wordHidden">
          <template v-if="!card.wordHidden"><div class="word serif">{{ card.word }}</div><PhoneticText class="phonetic" :uk="card.phoneticUk" :us="card.phoneticUs" /></template>
          <div v-else class="hidden-slot">点击显示单词</div>
        </div>
        <div class="rule"></div>
        <div class="meaning-zone" @click="card.meaningHidden = !card.meaningHidden">
          <div v-if="!card.meaningHidden" class="senses">
            <div v-for="(s, si) in card.senses" :key="si" class="sense"><span class="pos">{{ s.pos }}</span><span class="def">{{ s.meaning }}</span></div>
          </div>
          <div v-else class="hidden-slot">点击显示释义 · {{ card.senses.length }} 个词性</div>
        </div>
      </article>
    </main>

    <footer v-if="rows.length" class="bottom">
      <div class="hide-actions"><button @click="toggleWords">{{ allWordsHidden ? '显示单词' : '隐藏单词' }}</button><button @click="toggleMeanings">{{ allMeaningsHidden ? '显示释义' : '隐藏释义' }}</button></div>
      <button class="next" :class="{ armed: nextArmed }" @click="next">{{ nextArmed ? '再点一次进入下一组' : (page >= pages ? '最后一组' : '下一组') }}</button>
    </footer>

    <div v-if="showExit" class="overlay">
      <div class="modal">
        <div class="kicker">先别走</div><h2 class="serif">要耍起了吗？</h2><p>这组还没背完，确定现在就走？</p>
        <div class="modal-actions"><button class="ghost" @click="showExit = false">我才没有想耍起</button><button class="danger" @click="exit">不说了先耍起</button></div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.fav-study { padding-bottom: 150px; }
.loading { color: var(--muted); font-size: 13px; }
.grid { display: grid; grid-template-columns: 1fr; gap: 12px; }
.word-card { background: var(--surface); border: 1px solid var(--line); border-radius: 20px; padding: 18px 20px 16px; }
.card-top { display: flex; align-items: center; justify-content: space-between; margin-bottom: 6px; }
.index { font-size: 11px; color: var(--muted); letter-spacing: .12em; }
.star { border: 0; background: transparent; color: var(--star); font-size: 17px; }
.star span { font-size: 10px; color: var(--muted); margin-left: 3px; }
.word-zone, .meaning-zone { cursor: pointer; }
.word { font-size: 32px; line-height: 1.08; letter-spacing: -.02em; font-weight: 500; margin: 2px 0 4px; }
.phonetic { font-size: 12px; color: var(--muted); }
.rule { height: 1px; background: var(--line); margin: 14px 0 10px; }
.senses { display: flex; flex-direction: column; gap: 5px; }
.sense { display: grid; grid-template-columns: 34px 1fr; gap: 8px; align-items: start; }
.pos { font-family: "Iowan Old Style", "Palatino Linotype", Georgia, serif; font-style: italic; font-size: 13px; color: var(--accent); line-height: 1.75; }
.def { font-size: 14px; line-height: 1.7; }
.hidden-slot { display: flex; align-items: center; justify-content: center; min-height: 44px; border: 1px dashed var(--line); border-radius: 12px; color: var(--muted); font-size: 12px; }
.meaning-zone .hidden-slot { min-height: 38px; }
.bottom { position: fixed; left: 50%; bottom: calc(16px + env(safe-area-inset-bottom)); transform: translateX(-50%); width: min(720px, calc(100% - 28px)); display: flex; align-items: center; gap: 8px; background: rgba(255, 253, 248, .9); backdrop-filter: blur(18px); border: 1px solid var(--line); border-radius: 999px; padding: 7px; box-shadow: 0 12px 40px rgba(50, 35, 20, .12); z-index: 60; }
.hide-actions { display: flex; gap: 2px; flex: 1; }
.hide-actions button { border: 0; background: transparent; border-radius: 999px; padding: 10px 12px; font-size: 12px; color: var(--ink); }
.next { border: 0; border-radius: 999px; background: var(--accent); color: #fff; font-weight: 600; padding: 11px 18px; min-width: 118px; font-size: 13px; }
.next.armed { background: #7b2d21; }
.empty-card { text-align: center; padding: 40px 24px; }
.empty-mark { font-size: 28px; color: var(--star); margin-bottom: 10px; }
.empty-card h2 { font-size: 24px; margin: 0 0 6px; font-weight: 500; }
.empty-card p { color: var(--muted); font-size: 12px; margin: 0 0 18px; }
.overlay { position: fixed; inset: 0; background: rgba(15, 12, 8, .5); backdrop-filter: blur(4px); display: grid; place-items: center; padding: 20px; z-index: 200; }
.modal { width: min(420px, 100%); background: var(--surface); border: 1px solid var(--line); border-radius: 24px; padding: 26px; }
.kicker { font-size: 11px; letter-spacing: .18em; color: var(--accent); font-weight: 700; }
.modal h2 { font-size: 30px; margin: 8px 0 6px; font-weight: 500; }
.modal p { margin: 0; font-size: 13px; line-height: 1.7; color: var(--muted); }
.modal-actions { display: flex; gap: 8px; margin-top: 22px; }
.modal-actions button { flex: 1; padding: 12px 10px; border-radius: 12px; font-size: 12px; }
.ghost { border: 1px solid var(--line); background: transparent; color: var(--ink); }
.danger { border: 0; background: var(--accent); color: #fff; }
@media (min-width: 900px) { .grid { grid-template-columns: 1fr 1fr; gap: 16px; } }
</style>
