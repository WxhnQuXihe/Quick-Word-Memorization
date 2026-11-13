<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast } from 'vant'
import { currentGroup, markKnown, nextGroup, toggleFavorite } from '../api/app'

const route = useRoute()
const router = useRouter()
const bookId = Number(route.params.bookId)
const bookName = (route.query.name as string) || '词书'

const group = ref<any>(null)
const cards = ref<any[]>([])
const loading = ref(true)
const nextArmed = ref(false)
const showExit = ref(false)
let nextTimer: number | undefined

const allWordsHidden = computed(() => cards.value.length > 0 && cards.value.every((c) => c.wordHidden))
const allMeaningsHidden = computed(() => cards.value.length > 0 && cards.value.every((c) => c.meaningHidden))
const percent = computed(() => group.value ? Math.min(100, Math.round((group.value.groupNo / 443) * 100)) : 0)

function decorate(word: any) {
  return {
    ...word,
    key: `${word.id}-${Date.now()}-${Math.random()}`,
    wordHidden: false,
    meaningHidden: false,
    favorite: false,
    flash: false
  }
}

async function load() {
  loading.value = true
  try {
    group.value = await currentGroup(bookId)
    cards.value = (group.value.words || []).map(decorate)
  } finally {
    loading.value = false
  }
}

async function mark(card: any, index: number) {
  const res = await markKnown(group.value.groupId, card.id)
  if (res) {
    const fresh = decorate(res)
    fresh.flash = true
    cards.value[index] = fresh
    setTimeout(() => {
      if (cards.value[index]) cards.value[index].flash = false
    }, 550)
    showToast('已标熟，换了一个新词')
  } else {
    cards.value.splice(index, 1)
    showToast('本书已全部标熟')
  }
}

async function fav(card: any) {
  const on = await toggleFavorite(group.value.groupId, card.id)
  card.favorite = on
}

function toggleAllWords() {
  const hide = !allWordsHidden.value
  cards.value.forEach((c) => { c.wordHidden = hide })
}

function toggleAllMeanings() {
  const hide = !allMeaningsHidden.value
  cards.value.forEach((c) => { c.meaningHidden = hide })
}

async function handleNext() {
  if (!nextArmed.value) {
    nextArmed.value = true
    nextTimer = window.setTimeout(() => { nextArmed.value = false }, 3000)
    return
  }
  clearTimeout(nextTimer)
  nextArmed.value = false
  const res = await nextGroup(group.value.groupId)
  group.value = res
  cards.value = (res.words || []).map(decorate)
  if (!res.finished) showToast(`第 ${res.groupNo} 组`)
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

function confirmExit() {
  showExit.value = false
  router.replace('/')
}

onMounted(load)
</script>

<template>
  <div class="page study">
    <header class="topbar">
      <button class="back" @click="showExit = true">‹</button>
      <div class="title"><b>{{ bookName }}</b><span v-if="group">第 {{ group.groupNo }} 组 · 本组 {{ group.wordCount }} 词</span></div>
      <div class="meta">{{ group?.roundNo ? `第 ${group.roundNo} 轮` : '' }}</div>
    </header>
    <div class="progress-line"><i :style="{ width: percent + '%' }"></i></div>

    <div v-if="loading" class="card loading">正在取词…</div>

    <div v-else-if="group?.finished || !cards.length" class="card done">
      <div class="done-mark">✓</div>
      <h2 class="serif">这本书已经全部标熟了</h2>
      <p>去换一本，或者回主页看看。</p>
      <van-button type="primary" round @click="router.replace('/')">回主页</van-button>
    </div>

    <main v-else class="grid">
      <article v-for="(card, index) in cards" :key="card.key" class="word-card" :class="{ flash: card.flash }">
        <div class="card-top">
          <span class="index">{{ String(index + 1).padStart(2, '0') }}</span>
          <div class="actions">
            <button @click.stop="mark(card, index)">✓ <span>标熟</span></button>
            <button :class="{ on: card.favorite }" @click.stop="fav(card)">☆ <span>{{ card.favorite ? '已收藏' : '收藏' }}</span></button>
          </div>
        </div>
        <div class="word-zone" @click="card.wordHidden = !card.wordHidden">
          <template v-if="!card.wordHidden">
            <div class="word serif">{{ card.word }}</div>
            <div class="phonetic">{{ card.phoneticUk || card.phoneticUs }}</div>
          </template>
          <div v-else class="hidden-slot">点击显示单词</div>
        </div>
        <div class="rule"></div>
        <div class="meaning-zone" @click="card.meaningHidden = !card.meaningHidden">
          <div v-if="!card.meaningHidden" class="senses">
            <div v-for="(s, si) in card.senses" :key="si" class="sense">
              <span class="pos">{{ s.pos }}</span><span class="def">{{ s.meaning }}</span>
            </div>
          </div>
          <div v-else class="hidden-slot">点击显示释义 · {{ card.senses.length }} 个词性</div>
        </div>
      </article>
    </main>

    <footer v-if="cards.length" class="bottom">
      <div class="hide-actions">
        <button @click="toggleAllWords">{{ allWordsHidden ? '显示单词' : '隐藏单词' }}</button>
        <button @click="toggleAllMeanings">{{ allMeaningsHidden ? '显示释义' : '隐藏释义' }}</button>
      </div>
      <button class="next" :class="{ armed: nextArmed }" @click="handleNext">{{ nextArmed ? '再点一次进入下一组' : '下一组' }}</button>
    </footer>

    <div v-if="showExit" class="overlay">
      <div class="modal">
        <div class="kicker">先别走</div>
        <h2 class="serif">要耍起了吗？</h2>
        <p>这一组还没背完，确定现在就走？</p>
        <div class="modal-actions">
          <button class="ghost" @click="showExit = false">我才没有想耍起</button>
          <button class="danger" @click="confirmExit">不说了先耍起</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.study { padding-bottom: 150px; }
.loading { color: var(--muted); font-size: 13px; }
.grid { display: grid; grid-template-columns: 1fr; gap: 12px; }
.word-card { background: var(--surface); border: 1px solid var(--line); border-radius: 20px; padding: 18px 20px 16px; transition: background .25s; }
.word-card.flash { animation: flash .55s ease; }
@keyframes flash { 0% { background: #efe2cf; } 100% { background: var(--surface); } }
.card-top { display: flex; align-items: center; justify-content: space-between; gap: 10px; margin-bottom: 6px; }
.index { font-size: 11px; color: var(--muted); letter-spacing: .12em; font-variant-numeric: tabular-nums; }
.actions { display: flex; gap: 4px; }
.actions button { border: 0; background: transparent; color: var(--muted); font-size: 11px; padding: 5px 7px; border-radius: 999px; }
.actions button.on { color: var(--star); background: rgba(201, 138, 46, .1); }
.word-zone, .meaning-zone { cursor: pointer; }
.word { font-size: 32px; line-height: 1.08; letter-spacing: -.02em; font-weight: 500; margin: 2px 0 4px; }
.phonetic { font-size: 12px; color: var(--muted); }
.rule { height: 1px; background: var(--line); margin: 14px 0 10px; }
.senses { display: flex; flex-direction: column; gap: 5px; }
.sense { display: grid; grid-template-columns: 34px 1fr; column-gap: 8px; align-items: start; }
.pos { font-family: "Iowan Old Style", "Palatino Linotype", Georgia, serif; font-style: italic; font-size: 13px; color: var(--accent); line-height: 1.75; white-space: nowrap; }
.def { font-size: 14px; line-height: 1.7; }
.hidden-slot { display: flex; align-items: center; justify-content: center; min-height: 44px; border: 1px dashed var(--line); border-radius: 12px; color: var(--muted); font-size: 12px; }
.meaning-zone .hidden-slot { min-height: 38px; }
.done { text-align: center; padding: 40px 24px; }
.done-mark { width: 52px; height: 52px; border-radius: 50%; background: var(--ok); color: #fff; display: grid; place-items: center; margin: 0 auto 14px; font-size: 22px; }
.done h2 { font-size: 24px; margin: 0 0 6px; font-weight: 500; }
.done p { font-size: 12px; color: var(--muted); margin: 0 0 18px; }
.bottom { position: fixed; left: 50%; bottom: calc(16px + env(safe-area-inset-bottom)); transform: translateX(-50%); width: min(720px, calc(100% - 28px)); display: flex; align-items: center; gap: 8px; background: rgba(255, 253, 248, .9); backdrop-filter: blur(18px); border: 1px solid var(--line); border-radius: 999px; padding: 7px; box-shadow: 0 12px 40px rgba(50, 35, 20, .12); z-index: 60; }
.hide-actions { display: flex; gap: 2px; flex: 1; }
.hide-actions button { border: 0; background: transparent; border-radius: 999px; padding: 10px 12px; font-size: 12px; color: var(--ink); white-space: nowrap; }
.next { border: 0; border-radius: 999px; background: var(--accent); color: #fff; font-weight: 600; padding: 11px 18px; min-width: 118px; font-size: 13px; }
.next.armed { background: #7b2d21; animation: pulse .7s infinite alternate; }
@keyframes pulse { from { opacity: .75; } to { opacity: 1; } }
.overlay { position: fixed; inset: 0; background: rgba(15, 12, 8, .5); backdrop-filter: blur(4px); display: grid; place-items: center; padding: 20px; z-index: 200; }
.modal { width: min(420px, 100%); background: var(--surface); border: 1px solid var(--line); border-radius: 24px; padding: 26px; box-shadow: 0 30px 80px rgba(0, 0, 0, .35); }
.kicker { font-size: 11px; letter-spacing: .18em; color: var(--accent); font-weight: 700; }
.modal h2 { font-size: 30px; margin: 8px 0 6px; font-weight: 500; }
.modal p { margin: 0; font-size: 13px; line-height: 1.7; color: var(--muted); }
.modal-actions { display: flex; gap: 8px; margin-top: 22px; }
.modal-actions button { flex: 1; padding: 12px 10px; border-radius: 12px; font-size: 12px; }
.ghost { border: 1px solid var(--line); background: transparent; color: var(--ink); }
.danger { border: 0; background: var(--accent); color: #fff; }
@media (min-width: 900px) {
  .grid { grid-template-columns: 1fr 1fr; gap: 16px; }
}
</style>
