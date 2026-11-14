<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showConfirmDialog, showToast } from 'vant'
import { getAnnouncements, getAppVersion, getHome, listBooks, selectBook } from '../api/app'
import { APP_VERSION } from '../version'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const auth = useAuthStore()
const home = ref<any>(null)
const books = ref<any[]>([])
const showBooks = ref(false)
const loading = ref(true)
const announcements = ref<any[]>([])

const todayLabel = new Date().toLocaleDateString('zh-CN', { month: 'long', day: 'numeric', weekday: 'long' })
const percent = computed(() => {
  const b = home.value?.currentBook
  if (!b || !b.wordCount) return 0
  return Math.min(100, Math.round(((home.value.learnedCount || 0) / b.wordCount) * 100))
})

async function load() {
  loading.value = true
  try {
    home.value = await getHome()
    books.value = await listBooks()
    announcements.value = await getAnnouncements()
  } finally {
    loading.value = false
  }
}

async function pick(book: any) {
  await selectBook(book.id)
  showBooks.value = false
  await load()
  showToast(`已选择 ${book.name}`)
}

function study() {
  const b = home.value?.currentBook
  if (!b) return showBooks.value = true
  router.push({ name: 'study', params: { bookId: b.id }, query: { name: b.name } })
}

async function checkUpdate() {
  const info = await getAppVersion()
  if (info?.latestVersion && info.latestVersion !== APP_VERSION) {
    showConfirmDialog({
      title: '发现新版本',
      message: `最新版本 ${info.latestVersion}\n${info.notes || ''}`,
      confirmButtonText: '去下载',
      cancelButtonText: '稍后'
    }).then(() => {
      window.open(info.downloadUrl, '_blank')
    }).catch(() => {})
  }
}
onMounted(async () => { await load(); checkUpdate() })
</script>

<template>
  <div class="page home">
    <header class="hero">
      <div class="hero-copy">
        <div class="eyebrow">SHORTHAND · DAILY</div>
        <h1 class="serif">Hi，{{ auth.name || '神也' }}</h1>
        <p class="tagline">即将成神</p>
      </div>
      <div class="hero-side">
        <div class="date">{{ todayLabel }}</div>
        <div class="streak"><b>{{ home?.streakDays || 0 }}</b><span>天连续</span></div>
      </div>
    </header>

    <div v-if="loading" class="card skeleton">正在翻开你的单词册…</div>

    <template v-else>
      <section v-if="announcements.length" class="notice">
        <div class="notice-head"><span class="notice-dot"></span><span>公告</span><span class="notice-time">{{ (announcements[0].publishTime || announcements[0].createTime || '').replace('T', ' ').slice(0, 16) }}</span></div>
        <h3 class="serif">{{ announcements[0].title }}</h3>
        <p>{{ announcements[0].content }}</p>
      </section>

      <template v-if="home?.currentBook">
        <section class="card current">
          <div class="current-top">
            <div class="current-copy">
              <div class="tag">正在背 · CURRENT BOOK</div>
              <h2 class="serif">{{ home.currentBook.name }}</h2>
              <p>{{ home.learnedCount }} / {{ home.currentBook.wordCount }} 已学 · 第 {{ home.currentBook.currentRoundNo || 1 }} 轮</p>
            </div>
            <div class="ring" :style="{ background: `conic-gradient(var(--accent) ${percent}%, #eee5d6 0)` }">
              <div class="ring-in"><b class="serif">{{ percent }}<small>%</small></b></div>
            </div>
          </div>
          <div class="bar"><i :style="{ width: percent + '%' }"></i></div>
          <div class="current-foot">
            <button class="switch" @click="showBooks = true">换词书 <span>↗</span></button>
            <van-button type="primary" round class="continue" @click="study">继续背</van-button>
          </div>
        </section>

        <section class="quick">
          <button class="mini" @click="router.push('/favorites')">
            <span class="mini-icon">☆</span>
            <span class="mini-label">收藏夹</span>
            <b>{{ home.favoriteCount }}</b>
          </button>
          <button class="mini" @click="router.push('/known')">
            <span class="mini-icon">✓</span>
            <span class="mini-label">熟词本</span>
            <b>{{ home.knownCount }}</b>
          </button>
        </section>
      </template>

      <section v-else class="card empty-card">
        <div class="empty-mark">词</div>
        <h2 class="serif">先选一本词书</h2>
        <p>选一本开始，今天就把「即将成神」兑现一点。</p>
        <van-button type="primary" round @click="showBooks = true">选择词书</van-button>
      </section>
    </template>

    <van-popup v-model:show="showBooks" position="bottom" round safe-area-inset-bottom>
      <div class="drawer">
        <div class="drawer-head"><h3 class="serif">选择词书</h3><button @click="showBooks = false">关闭</button></div>
        <p class="drawer-hint">换书不会清空学习记录。</p>
        <button v-for="b in books" :key="b.id" class="book-row" @click="pick(b)">
          <div><b>{{ b.name }}</b><span>{{ b.wordCount.toLocaleString() }} 词 · 难度 {{ b.difficulty }}/5</span></div>
          <span class="arrow">›</span>
        </button>
      </div>
    </van-popup>
  </div>
</template>

<style scoped>
.home { padding-top: 14px; }
.hero { display: flex; align-items: flex-start; justify-content: space-between; gap: 18px; padding: 14px 2px 24px; }
.eyebrow { font-family: var(--mono); font-size: 9px; letter-spacing: .18em; color: var(--muted); margin-bottom: 8px; }
.hero h1 { font-size: 33px; line-height: 1.05; font-weight: 500; margin: 0 0 5px; letter-spacing: -.04em; }
.tagline { margin: 0; color: var(--accent); font-size: 12px; letter-spacing: .08em; }
.hero-side { text-align: right; flex: none; }
.date { color: var(--muted); font-size: 11px; margin: 4px 0 9px; white-space: nowrap; }
.streak { display: inline-flex; align-items: baseline; gap: 4px; border: 1px solid var(--line); border-radius: 999px; padding: 7px 11px; background: var(--surface); color: var(--muted); }
.streak b { font-family: var(--mono); font-size: 16px; color: var(--accent); font-weight: 600; }
.streak span { font-size: 10px; }
.skeleton { color: var(--muted); font-size: 13px; }
.notice { margin-bottom: 14px; padding: 15px 17px 14px; background: rgba(180, 67, 47, .045); border: 1px solid rgba(180, 67, 47, .18); border-radius: 16px; }
.notice-head { display: flex; align-items: center; gap: 6px; font-size: 10px; color: var(--accent); letter-spacing: .1em; }
.notice-dot { width: 5px; height: 5px; border-radius: 50%; background: var(--accent); box-shadow: 0 0 0 3px rgba(180,67,47,.08); }
.notice-time { margin-left: auto; font-family: var(--mono); font-size: 8px; color: #a1988a; letter-spacing: 0; }
.notice h3 { font-size: 19px; font-weight: 500; margin: 7px 0 4px; }
.notice p { font-size: 12px; line-height: 1.7; color: var(--muted); margin: 0; white-space: pre-wrap; }
.current { padding: 22px; }
.current-top { display: flex; justify-content: space-between; gap: 16px; }
.current-copy { min-width: 0; }
.tag { font-size: 9px; color: var(--accent); letter-spacing: .12em; margin-bottom: 8px; }
.current h2 { font-size: 27px; font-weight: 500; margin: 0 0 5px; letter-spacing: -.03em; }
.current p { margin: 0; font-size: 11px; color: var(--muted); }
.ring { width: 82px; height: 82px; border-radius: 50%; display: grid; place-items: center; flex: none; }
.ring-in { width: 66px; height: 66px; border-radius: 50%; background: var(--surface); display: grid; place-items: center; }
.ring-in b { font-size: 18px; color: var(--accent); font-weight: 600; }
.ring-in small { font-size: 10px; font-weight: 400; }
.bar { height: 3px; background: #eee5d6; border-radius: 99px; margin: 20px 0 14px; overflow: hidden; }
.bar i { display: block; height: 100%; background: var(--accent); }
.current-foot { display: flex; align-items: center; justify-content: space-between; }
.switch { border: 0; background: transparent; color: var(--muted); font-size: 12px; padding: 6px 0; }
.switch span { color: var(--accent); margin-left: 2px; }
.continue { min-width: 142px; }
.quick { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; margin-top: 12px; }
.mini { border: 1px solid var(--line); background: var(--surface); border-radius: 16px; padding: 15px 16px; text-align: left; display: grid; grid-template-columns: 24px 1fr auto; align-items: center; column-gap: 7px; row-gap: 2px; }
.mini-icon { grid-row: span 2; color: var(--star); font-size: 17px; }
.mini-label { font-size: 11px; color: var(--muted); }
.mini b { grid-row: span 2; grid-column: 3; font-family: var(--mono); font-size: 22px; font-weight: 400; }
.empty-card { text-align: center; padding: 38px 24px; }
.empty-mark { width: 52px; height: 52px; border: 1px solid var(--line); border-radius: 50%; display: grid; place-items: center; margin: 0 auto 14px; color: var(--accent); font-family: "Iowan Old Style", Georgia, serif; font-size: 24px; }
.empty-card h2 { font-size: 24px; margin: 0 0 6px; font-weight: 500; }
.empty-card p { color: var(--muted); font-size: 12px; margin: 0 0 18px; }
.drawer { padding: 22px 20px 26px; }
.drawer-head { display: flex; align-items: center; margin-bottom: 4px; }
.drawer-head h3 { margin: 0; font-size: 24px; font-weight: 500; }
.drawer-head button { margin-left: auto; border: 0; background: transparent; color: var(--muted); font-size: 12px; }
.drawer-hint { margin: 0 0 10px; color: var(--muted); font-size: 11px; }
.book-row { width: 100%; display: flex; align-items: center; justify-content: space-between; border: 1px solid var(--line); background: var(--surface); border-radius: 14px; padding: 14px 15px; margin-top: 10px; text-align: left; }
.book-row b { display: block; font-size: 14px; font-weight: 600; }
.book-row span { display: block; font-size: 11px; color: var(--muted); margin-top: 3px; }
.book-row .arrow { font-size: 20px; color: var(--muted); }
@media (min-width: 900px) {
  .home { max-width: 860px; }
}
</style>
