<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { login } from '../api/app'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const auth = useAuthStore()
const username = ref('')
const password = ref('')
const loading = ref(false)

async function submit() {
  if (!username.value) return showToast('请输入账号')
  if (password.value.length < 6) return showToast('密码至少 6 位')
  loading.value = true
  try {
    const res = await login(username.value, password.value)
    auth.setLogin(res.token, res.nickname || res.username)
    router.replace('/')
  } finally {
    loading.value = false
  }
}

function soon(text: string) {
  showToast(text)
}
</script>

<template>
  <div class="login">
    <div class="brand">
      <img src="/icon-192.png" alt="神也背单词" />
      <h1 class="serif">神也背单词</h1>
      <p>每天背一点，记得住</p>
    </div>
    <div class="form">
      <label>账号</label>
      <van-field v-model="username" placeholder="请输入账号" :border="false" />
      <label>密码</label>
      <van-field v-model="password" type="password" placeholder="请输入密码" :border="false" @keyup.enter="submit" />
      <van-button type="primary" block round :loading="loading" class="submit" @click="submit">登录</van-button>
      <div class="links">
        <button @click="soon('暂未开放注册，请联系管理员')">注册暂未开放</button>
        <button @click="soon('请联系管理员重置密码')">忘记密码？</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login { min-height: 100vh; max-width: 420px; margin: 0 auto; padding: 88px 28px 40px; }
.brand { text-align: center; margin-bottom: 46px; }
.brand img { width: 92px; height: 92px; border-radius: 22px; box-shadow: 0 10px 30px rgba(60, 40, 20, .12); }
.brand h1 { font-size: 34px; margin: 18px 0 6px; letter-spacing: -.03em; font-weight: 500; }
.brand p { margin: 0; font-size: 12px; color: var(--muted); letter-spacing: .08em; }
.form { background: var(--surface); border: 1px solid var(--line); border-radius: 24px; padding: 22px 20px; }
label { display: block; font-size: 11px; color: var(--muted); margin: 0 0 6px 2px; }
.form :deep(.van-field) { background: var(--paper); border-radius: 12px; margin-bottom: 16px; }
.form :deep(.van-field__control) { font-size: 14px; }
.submit { margin-top: 6px; }
.links { display: flex; justify-content: space-between; margin-top: 18px; }
.links button { border: 0; background: transparent; font-size: 11px; color: var(--muted); padding: 4px; }
</style>
