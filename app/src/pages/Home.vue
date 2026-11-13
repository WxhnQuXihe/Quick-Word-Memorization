<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { getHome, listBooks, selectBook } from '../api/app'

const router = useRouter()
const home = ref<any>(null)
const books = ref<any[]>([])
const showBooks = ref(false)
const loading = ref(true)

const percent = computed(() => {
  const b = home.value?.currentBook
  if (!b || !b.wordCount) return 0
  return Math.min(100, Math.round(((home.value.learnedCount || 0) / b.wordCount) * 100))
})

async function load() {
  loading.value = true
  try {
    home.value = await getHome()
    if (!home.value?.currentBook) {
      books.value = await listBooks()
    }
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

onMounted(load)
</script>

<template>
  <div class="page home">
    <header class="hello">
      <div>
        <h1 class="serif">Hi，神也</h1>
        <p>{{ new Date().toLocaleDateString('zh-CN', { month: 'long', day: 'numeric', weekday: 'long' }) }}</p>
      </div>
      <div class="streak"><span>连续</span><b>{{ home?.streakDays || 0 }}</b><span>天</span></div>
    </header>

    <div v-if="loading" class="card skeleton">正在翻开你的单词册…</div>

    <template v-else-if="home?.currentBook">
      <section class="card current">
        <div class="current-top">
          <div>
            <div class="tag">正在背</div>
            <h2 class="serif">{{ home.currentBook.name }}</h2>
            <p>{{ home.learnedCount }} / {{ home.currentBook.wordCount }} 已学</p>
          </div>
          <div class="ring" :style="{ background: `conic-gradient(var(--accent) ${percent}%, #eee5d6 0)` }">
            <div class="ring-in"><b>{{ percent }}%</b></div>
          </div>
        </div>
        <div class="bar"><i :style="{ width: percent + '%' }"></i></div>
        <div class="current-foot">
          <button class="switch" @click="showBooks = true">换词书</button>
          <van-button type="primary" round class="continue" @click="study">继续背</van-button>
        </div>
      </section>

      <section class="quick">
        <button class="mini" @click="router.push('/favorites')"><span>收藏夹</span><b>{{ home.favoriteCount }}</b></button>
        <button class="mini" @click="router.push('/known')"><span>熟词本</span><b>{{ home.knownCount }}</b></button>
      </section>
    </template>

    <section v-else class="card empty-card">
      <div class="empty-mark">词</div>
      <h2 class="serif">先选一本词书</h2>
      <p>四级、六级、词组，三本都在这里。</p>
      <van-button type="primary" round @click="showBooks = true">选择词书</van-button>
    </section>

    <van-popup v-model:show="showBooks" position="bottom" round safe-area-inset-bottom>
      <div class="drawer">
        <div class="drawer-head"><h3 class="serif">选择词书</h3><button @click="showBooks = false">关闭</button></div>
        <button v-for="b in books" :key="b.id" class="book-row" @click="pick(b)">
          <div><b>{{ b.name }}</b><span>{{ b.wordCount.toLocaleString() }} 词 · 难度 {{ b.difficulty }}/5</span></div>
          <span class="arrow">›</span>
        </button>
      </div>
    </van-popup>
  </div>
</template>

<style scoped>
.home { padding-top: 10px; }
.hello { display: flex; align-items: flex-start; justify-content: space-between; padding: 22px 2px 26px; }
.hello h1 { font-size: 30px; font-weight: 500; margin: 0 0 5px; letter-spacing: -.03em; }
.hello p { margin: 0; color: var(--muted); font-size: 12px; }
.streak { display: flex; align-items: baseline; gap: 4px; font-size: 11px; color: var(--muted); border: 1px solid var(--line); border-radius: 999px; padding: 8px 12px; background: var(--surface); }
.streak b { font-size: 17px; color: var(--accent); font-weight: 600; }
.skeleton { color: var(--muted); font-size: 13px; }
.current { padding: 22px; }
.current-top { display: flex; justify-content: space-between; gap: 16px; }
.tag { font-size: 10px; color: var(--accent); letter-spacing: .12em; margin-bottom: 8px; }
.current h2 { font-size: 27px; font-weight: 500; margin: 0 0 4px; letter-spacing: -.03em; }
.current p { margin: 0; font-size: 12px; color: var(--muted); }
.ring { width: 78px; height: 78px; border-radius: 50%; display: grid; place-items: center; flex: none; }
.ring-in { width: 62px; height: 62px; border-radius: 50%; background: var(--surface); display: grid; place-items: center; }
.ring-in b { font-size: 15px; color: var(--accent); font-weight: 600; }
.bar { height: 3px; background: #eee5d6; border-radius: 99px; margin: 20px 0 14px; overflow: hidden; }
.bar i { display: block; height: 100%; background: var(--accent); }
.current-foot { display: flex; align-items: center; justify-content: space-between; }
.switch { border: 0; background: transparent; color: var(--muted); font-size: 12px; padding: 6px 0; }
.continue { min-width: 140px; }
.quick { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; margin-top: 12px; }
.mini { border: 1px solid var(--line); background: var(--surface); border-radius: 16px; padding: 16px; text-align: left; display: flex; flex-direction: column; gap: 6px; }
.mini span { font-size: 11px; color: var(--muted); }
.mini b { font-size: 22px; font-weight: 500; }
.empty-card { text-align: center; padding: 38px 24px; }
.empty-mark { width: 52px; height: 52px; border: 1px solid var(--line); border-radius: 50%; display: grid; place-items: center; margin: 0 auto 14px; color: var(--accent); font-family: "Iowan Old Style", Georgia, serif; font-size: 24px; }
.empty-card h2 { font-size: 24px; margin: 0 0 6px; font-weight: 500; }
.empty-card p { color: var(--muted); font-size: 12px; margin: 0 0 18px; }
.drawer { padding: 22px 20px 26px; }
.drawer-head { display: flex; align-items: center; margin-bottom: 10px; }
.drawer-head h3 { margin: 0; font-size: 24px; font-weight: 500; }
.drawer-head button { margin-left: auto; border: 0; background: transparent; color: var(--muted); font-size: 12px; }
.book-row { width: 100%; display: flex; align-items: center; justify-content: space-between; border: 1px solid var(--line); background: var(--surface); border-radius: 14px; padding: 14px 15px; margin-top: 10px; text-align: left; }
.book-row b { display: block; font-size: 14px; font-weight: 600; }
.book-row span { display: block; font-size: 11px; color: var(--muted); margin-top: 3px; }
.book-row .arrow { font-size: 20px; color: var(--muted); }
@media (min-width: 900px) {
  .home { max-width: 860px; }
}
</style>
