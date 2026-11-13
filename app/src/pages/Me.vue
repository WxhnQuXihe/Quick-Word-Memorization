<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { getHome, myFeedback, submitFeedback } from '../api/app'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const auth = useAuthStore()
const home = ref<any>(null)
const reminder = ref(true)
const showFeedback = ref(false)
const feedbackText = ref('')
const feedbackContact = ref('')
const submitting = ref(false)
const myFeedbacks = ref<any[]>([])
async function openFeedback() {
  showFeedback.value = true
  const res = await myFeedback()
  myFeedbacks.value = res.records || []
}
async function sendFeedback() {
  if (!feedbackText.value.trim()) return showToast('先写点反馈内容')
  submitting.value = true
  try {
    await submitFeedback(feedbackText.value.trim(), feedbackContact.value.trim())
    showToast('反馈已提交')
    feedbackText.value = ''
    feedbackContact.value = ''
    const res = await myFeedback()
    myFeedbacks.value = res.records || []
  } finally {
    submitting.value = false
  }
}

onMounted(async () => { home.value = await getHome() })

function logout() {
  auth.logout()
  router.replace('/login')
}
</script>

<template>
  <div class="page me">
    <header class="profile">
      <div class="avatar">{{ (auth.name || '神').slice(0, 1) }}</div>
      <div><h1 class="serif">{{ auth.name || '神也' }}</h1><p>@{{ auth.name || 'shorthand' }}</p></div>
    </header>

    <section class="card fav-card">
      <div>
        <div class="tag">收藏夹</div>
        <h2 class="serif">{{ home?.favoriteCount || 0 }} 词</h2>
        <p>你收藏的单词会自动汇入这里，可当作专属词书。</p>
      </div>
      <div class="row-actions">
        <van-button size="small" round plain @click="router.push('/favorites')">查看</van-button>
        <van-button size="small" round type="primary" @click="router.push('/favorites/study')">背收藏夹</van-button>
      </div>
    </section>

    <van-cell-group inset class="cells">
      <van-cell title="熟词本" :value="`${home?.knownCount || 0} 词`" is-link @click="router.push('/known')" />
      <van-cell title="学习提醒" center>
        <template #right-icon>
          <van-switch v-model="reminder" size="20" @change="showToast(reminder ? '已开启提醒' : '已关闭提醒')" />
        </template>
      </van-cell>
      <van-cell title="意见反馈" is-link @click="openFeedback" />
      <van-cell title="退出登录" is-link @click="logout" />
    </van-cell-group>

    <van-popup v-model:show="showFeedback" position="bottom" round safe-area-inset-bottom>
      <div class="feedback-sheet">
        <h3 class="serif">意见反馈</h3>
        <p class="feedback-hint">遇到问题、想要的功能，都可以写在这里。</p>
        <van-field v-model="feedbackText" type="textarea" rows="4" maxlength="1000" show-word-limit placeholder="写点反馈…" />
        <van-field v-model="feedbackContact" placeholder="联系方式（可选）" />
        <van-button type="primary" block round :loading="submitting" class="feedback-submit" @click="sendFeedback">提交反馈</van-button>
        <div v-if="myFeedbacks.length" class="feedback-history">
          <div v-for="f in myFeedbacks" :key="f.id" class="feedback-item">
            <p>{{ f.content }}</p>
            <span>{{ (f.createTime || '').replace('T', ' ').slice(0, 16) }} · {{ f.status === 1 ? '已回复' : '待处理' }}</span>
            <div v-if="f.reply" class="reply">回复：{{ f.reply }}</div>
          </div>
        </div>
      </div>
    </van-popup>
  </div>
</template>

<style scoped>
.me { padding-top: 24px; }
.profile { display: flex; align-items: center; gap: 14px; padding: 8px 2px 28px; }
.avatar { width: 54px; height: 54px; border-radius: 18px; background: var(--accent); color: #fff; display: grid; place-items: center; font-family: "Iowan Old Style", Georgia, serif; font-size: 24px; }
.profile h1 { margin: 0 0 3px; font-size: 25px; font-weight: 500; }
.profile p { margin: 0; font-size: 11px; color: var(--muted); }
.fav-card { display: flex; flex-direction: column; gap: 16px; padding: 20px; }
.tag { font-size: 10px; letter-spacing: .12em; color: var(--accent); }
.fav-card h2 { margin: 8px 0 5px; font-size: 26px; font-weight: 500; }
.fav-card p { margin: 0; font-size: 11px; line-height: 1.6; color: var(--muted); }
.row-actions { display: flex; gap: 8px; justify-content: flex-end; }
.cells { margin: 14px 0 0; background: var(--surface); border: 1px solid var(--line); border-radius: 18px; overflow: hidden; }
.cells :deep(.van-cell) { background: transparent; }
.cells :deep(.van-cell__title) { font-size: 13px; }
.feedback-sheet { padding: 22px 20px 30px; }
.feedback-sheet h3 { margin: 0 0 4px; font-size: 24px; font-weight: 500; }
.feedback-hint { margin: 0 0 14px; font-size: 11px; color: var(--muted); }
.feedback-sheet :deep(.van-field) { background: var(--paper); border-radius: 12px; margin-bottom: 10px; }
.feedback-submit { margin-top: 4px; }
.feedback-history { margin-top: 22px; border-top: 1px solid var(--line); padding-top: 14px; max-height: 240px; overflow: auto; }
.feedback-item { padding: 10px 0; border-bottom: 1px solid var(--line); }
.feedback-item:last-child { border-bottom: 0; }
.feedback-item p { margin: 0 0 4px; font-size: 13px; line-height: 1.6; }
.feedback-item span { font-size: 10px; color: var(--muted); }
.reply { margin-top: 6px; font-size: 11px; color: var(--accent); }
</style>
