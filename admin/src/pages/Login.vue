<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { adminLogin } from '../api/admin'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const auth = useAuthStore()
const username = ref('admin')
const password = ref('')
const loading = ref(false)

async function submit() {
  if (!username.value || !password.value) {
    ElMessage.warning('请输入账号和密码')
    return
  }
  loading.value = true
  try {
    const res = await adminLogin(username.value, password.value)
    auth.setLogin(res.token, res.nickname || res.username)
    router.push('/')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <div class="grid"></div>
    <div class="login-card">
      <div class="mark">S</div>
      <div class="eyebrow">SHORTAND / ADMIN</div>
      <h1>控制台登录</h1>
      <p>单管理员入口 · 仅限本人使用</p>
      <form @submit.prevent="submit">
        <label>账号</label>
        <el-input v-model="username" placeholder="admin" size="large" />
        <label>密码</label>
        <el-input v-model="password" type="password" show-password placeholder="请输入密码" size="large" @keyup.enter="submit" />
        <el-button type="primary" size="large" :loading="loading" class="submit" @click="submit">进入控制台</el-button>
      </form>
      <div class="foot"><span class="dot"></span> SYSTEM ONLINE · 47.95.66.191</div>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh; display: grid; place-items: center;
  background: radial-gradient(circle at 50% 35%, #11161b 0%, #08090b 62%);
  position: relative; overflow: hidden;
}
.grid {
  position: absolute; inset: 0;
  background-image: linear-gradient(to right, rgba(255,255,255,.035) 1px, transparent 1px),
                    linear-gradient(to bottom, rgba(255,255,255,.035) 1px, transparent 1px);
  background-size: 48px 48px;
  mask-image: radial-gradient(circle at 50% 40%, #000 0%, transparent 70%);
}
.login-card {
  position: relative; width: min(380px, calc(100vw - 40px));
  border: 1px solid var(--line); background: rgba(13, 16, 19, .88);
  padding: 34px 32px 26px; backdrop-filter: blur(16px);
  box-shadow: 0 30px 80px rgba(0,0,0,.45);
}
.mark {
  width: 46px; height: 46px; border-radius: 13px; background: var(--accent);
  color: #060806; display: grid; place-items: center;
  font-weight: 800; font-size: 20px; margin-bottom: 24px;
  box-shadow: 0 0 30px rgba(200,245,96,.18);
}
.eyebrow { font-family: var(--mono); font-size: 9px; letter-spacing: .2em; color: var(--muted); }
h1 { font-size: 26px; margin: 8px 0 6px; letter-spacing: -.04em; font-weight: 600; }
p { font-size: 12px; color: var(--muted); margin: 0 0 26px; }
label { display: block; font-size: 10px; color: var(--muted); margin: 14px 0 7px; letter-spacing: .05em; }
.submit { width: 100%; margin-top: 22px; }
.foot { margin-top: 24px; padding-top: 14px; border-top: 1px solid var(--line); font-family: var(--mono); font-size: 9px; color: var(--muted); display: flex; align-items: center; gap: 7px; }
.dot { width: 5px; height: 5px; border-radius: 50%; background: var(--accent); box-shadow: 0 0 8px var(--accent); }
</style>
