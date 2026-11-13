<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Grid, Collection, Document, Upload, User, ChatDotRound } from '@element-plus/icons-vue'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const menus = [
  { name: 'dashboard', label: '总览', icon: Grid, path: '/' },
  { name: 'wordbooks', label: '词书', icon: Collection, path: '/wordbooks' },
  { name: 'words', label: '词条', icon: Document, path: '/words' },
  { name: 'imports', label: '导入', icon: Upload, path: '/imports' },
  { name: 'users', label: '用户', icon: User, path: '/users' },
  { name: 'feedback', label: '反馈', icon: ChatDotRound, path: '/feedback' }
]

const title = computed(() => (route.meta.title as string) || '总览')

function go(path: string) {
  router.push(path)
}
function logout() {
  auth.logout()
  router.push('/login')
}
</script>

<template>
  <div class="shell">
    <aside class="rail">
      <div class="logo">S</div>
      <nav class="nav">
        <button
          v-for="m in menus"
          :key="m.name"
          :class="{ active: route.name === m.name }"
          :title="m.label"
          @click="go(m.path)"
        >
          <el-icon :size="18"><component :is="m.icon" /></el-icon>
        </button>
      </nav>
      <div class="rail-bottom">
        <span class="dot"></span>
        <span class="ver">v0.1</span>
      </div>
    </aside>

    <main class="main">
      <header class="top">
        <div>
          <div class="eyebrow">SHORTHAND / CONTROL</div>
          <h1>{{ title }}</h1>
        </div>
        <div class="status"><span class="dot"></span> ALL SYSTEMS NOMINAL</div>
        <el-dropdown trigger="click">
          <span class="avatar">{{ auth.name.slice(0, 2).toUpperCase() }}</span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </header>
      <router-view />
    </main>
  </div>
</template>

<style scoped>
.shell { display: grid; grid-template-columns: 76px 1fr; min-height: 100vh; }
.rail {
  position: sticky; top: 0; height: 100vh;
  border-right: 1px solid var(--line);
  display: flex; flex-direction: column; align-items: center;
  padding: 18px 0; background: rgba(8, 9, 11, .86);
}
.logo {
  width: 40px; height: 40px; border-radius: 12px;
  background: var(--accent); color: #060806;
  display: grid; place-items: center; font-weight: 800; font-size: 18px;
  box-shadow: 0 0 24px rgba(200, 245, 96, .16);
}
.nav { display: flex; flex-direction: column; gap: 6px; margin-top: 34px; }
.nav button {
  width: 46px; height: 44px; border: 0; border-radius: 12px;
  background: transparent; color: var(--muted); cursor: pointer;
  display: grid; place-items: center; transition: background .18s, color .18s;
}
.nav button:hover { background: #14191f; color: var(--ink); }
.nav button.active { background: rgba(200, 245, 96, .1); color: var(--accent); }
.rail-bottom { margin-top: auto; display: flex; flex-direction: column; align-items: center; gap: 10px; }
.rail-bottom .dot { width: 7px; height: 7px; border-radius: 50%; background: var(--accent); box-shadow: 0 0 10px var(--accent); }
.rail-bottom .ver { writing-mode: vertical-rl; font-family: var(--mono); font-size: 9px; color: var(--muted); letter-spacing: .16em; }
.main { padding: 24px 34px 40px; min-width: 0; }
.top { display: flex; align-items: center; gap: 20px; margin-bottom: 24px; }
.eyebrow { font-family: var(--mono); font-size: 9px; letter-spacing: .2em; color: var(--muted); margin-bottom: 4px; }
h1 { margin: 0; font-size: 24px; line-height: 1; font-weight: 600; letter-spacing: -.03em; }
.status {
  margin-left: auto; display: flex; align-items: center; gap: 7px;
  border: 1px solid var(--line); border-radius: 999px; padding: 8px 11px;
  font-family: var(--mono); font-size: 9px; letter-spacing: .12em; color: var(--muted);
}
.status .dot { width: 6px; height: 6px; border-radius: 50%; background: var(--accent); box-shadow: 0 0 9px var(--accent); }
.avatar {
  width: 34px; height: 34px; border-radius: 50%; background: #1a2027;
  border: 1px solid #2a333d; display: grid; place-items: center;
  font-size: 10px; font-weight: 600; color: var(--accent); cursor: pointer;
}
</style>
