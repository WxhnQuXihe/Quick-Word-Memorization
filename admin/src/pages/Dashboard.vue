<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { getDashboard } from '../api/admin'

const stats = ref<any>(null)
const events = [
  ['14:32', 'IMPORT', '六级大纲 · 5,515 条词条已入库'],
  ['14:18', 'SYSTEM', 'TTS 未启用，跳过音频任务'],
  ['13:52', 'USER', 'QX 完成第 3 组 · 四级大纲'],
  ['12:06', 'FEEDBACK', '#12 已回复 · 用户反馈'],
  ['09:41', 'AUTH', 'admin 登录 · 47.95.66.191']
]
const bars = [18, 26, 22, 34, 30, 46, 38, 52, 44, 61, 55, 68, 62, 74]
const fmt = (n: number | undefined) => (n ?? 0).toLocaleString()
onMounted(async () => { stats.value = await getDashboard() })
</script>

<template>
  <div class="dash">
    <section class="kpis">
      <div class="kpi"><div class="label">词条总量</div><div class="value">{{ fmt(stats?.wordCount) }}</div><div class="delta"><b>{{ stats?.bookCount || 0 }}</b> 本词书</div><div class="spark a"></div></div>
      <div class="kpi"><div class="label">注册用户</div><div class="value">{{ fmt(stats?.userCount) }}</div><div class="delta"><b>不开放注册</b></div><div class="spark b"></div></div>
      <div class="kpi"><div class="label">收藏总数</div><div class="value">{{ fmt(stats?.favoriteCount) }}</div><div class="delta"><b>{{ fmt(stats?.knownCount) }}</b> 个熟词</div><div class="spark a"></div></div>
      <div class="kpi"><div class="label">待处理反馈</div><div class="value">{{ fmt(stats?.openFeedbackCount) }}</div><div class="delta"><b>需回复</b></div><div class="spark c"></div></div>
    </section>

    <section class="grid">
      <div class="panel books">
        <div class="panel-head"><h2>词书运行状态</h2><span>3 BOOKS / LIVE</span></div>
        <table>
          <thead><tr><th>词书</th><th>词数</th><th>状态</th><th>排序</th><th>最近更新</th></tr></thead>
          <tbody>
            <tr v-for="b in (stats?.books || [])" :key="b.id">
              <td><div class="book"><b>{{ b.name }}</b><span>{{ b.code }}</span></div></td>
              <td class="mono">{{ fmt(b.wordCount) }}</td>
              <td><span class="pill" :class="{ draft: b.status !== 1 }">{{ b.status === 1 ? '已上架' : '草稿' }}</span></td>
              <td class="mono">{{ b.sort }}</td>
              <td class="mono">{{ (b.updateTime || '-').replace('T', ' ').slice(0, 16) }}</td>
            </tr>
            <tr v-if="!stats?.books?.length"><td colspan="5" class="mono">还没有词书</td></tr>
          </tbody>
        </table>
      </div>

      <div class="side">
        <div class="panel">
          <div class="panel-head"><h2>系统遥测</h2><span>REALTIME</span></div>
          <div class="telemetry">
            <div><span>API LATENCY</span><b>42 ms</b></div>
            <div><span>DB QUERY</span><b>12 ms</b></div>
            <div><span>JVM HEAP</span><b>312 MB</b></div>
            <div><span>UPTIME</span><b>12d 04h</b></div>
          </div>
        </div>
        <div class="panel">
          <div class="panel-head"><h2>事件流</h2><span>LAST 5</span></div>
          <div class="log">
            <div v-for="e in events" :key="e[0] + e[2]" class="log-row">
              <span class="mono time">{{ e[0] }}</span><span class="mono kind" :class="{ sys: e[1] === 'SYSTEM' }">{{ e[1] }}</span><span class="msg">{{ e[2] }}</span>
            </div>
          </div>
        </div>
      </div>
    </section>

    <section class="bottom">
      <div class="panel">
        <div class="panel-head"><h2>学习活跃度</h2><span>14 DAYS / WORDS</span></div>
        <div class="bars">
          <div v-for="(v, i) in bars" :key="i" class="bar-col" :style="{ height: v + '%' }"><span>{{ i + 1 }}</span></div>
        </div>
      </div>
      <div class="panel">
        <div class="panel-head"><h2>最近用户</h2><span>{{ stats?.userCount || 0 }} TOTAL</span></div>
        <div class="users">
          <div v-for="u in (stats?.recentUsers || [])" :key="u.id" class="user">
            <div class="ava">{{ (u.nickname || u.username || '?').slice(0, 1) }}</div>
            <div class="info"><b>{{ u.nickname || u.username }}</b><span>@{{ u.username }}</span></div>
            <div class="mono time">{{ u.status === 1 ? '正常' : '禁用' }}</div>
          </div>
          <div v-if="!stats?.recentUsers?.length" class="mono" style="padding:16px">还没有用户</div>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.dash { display: flex; flex-direction: column; gap: 16px; }
.kpis { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; }
.kpi { position: relative; overflow: hidden; background: linear-gradient(180deg, var(--panel-2), var(--panel)); border: 1px solid var(--line); border-radius: 14px; padding: 15px 16px 12px; }
.kpi::after { content: ""; position: absolute; left: 16px; right: 16px; bottom: 0; height: 1px; background: linear-gradient(90deg, transparent, rgba(200,245,96,.35), transparent); }
.label { font-size: 10px; color: var(--muted); letter-spacing: .05em; }
.value { font-family: var(--mono); font-size: 30px; letter-spacing: -.04em; margin: 7px 0 3px; }
.delta { font-size: 10px; color: #6f7a86; }
.delta b { color: var(--accent); font-weight: 500; }
.spark { position: absolute; right: 16px; bottom: 14px; width: 78px; height: 24px; opacity: .8; }
.spark.a { background: linear-gradient(135deg, transparent 45%, rgba(200,245,96,.5) 46%, transparent 48%); }
.spark.b { background: linear-gradient(160deg, transparent 45%, rgba(99,230,255,.45) 46%, transparent 48%); }
.spark.c { background: linear-gradient(140deg, transparent 45%, rgba(213,163,90,.5) 46%, transparent 48%); }
.grid { display: grid; grid-template-columns: 1.55fr 1fr; gap: 16px; align-items: start; }
.panel { background: var(--panel); border: 1px solid var(--line); border-radius: 16px; overflow: hidden; }
.panel-head { display: flex; align-items: center; gap: 10px; padding: 14px 16px; border-bottom: 1px solid var(--line); }
.panel-head h2 { font-size: 12px; font-weight: 600; margin: 0; letter-spacing: .02em; }
.panel-head span { margin-left: auto; font-family: var(--mono); font-size: 9px; color: var(--muted); }
table { width: 100%; border-collapse: collapse; }
th { text-align: left; font-size: 9px; letter-spacing: .1em; color: var(--muted); font-weight: 500; padding: 10px 16px; border-bottom: 1px solid var(--line); }
td { padding: 12px 16px; font-size: 12px; border-bottom: 1px solid #14191e; }
tr:last-child td { border-bottom: 0; }
tbody tr:hover { background: #10151a; }
.mono { font-family: var(--mono); font-size: 10px; color: var(--muted); letter-spacing: .04em; }
.book { display: flex; flex-direction: column; gap: 2px; }
.book b { font-weight: 500; }
.bar { width: 100%; height: 4px; border-radius: 99px; background: #1a2026; overflow: hidden; }
.bar i { display: block; height: 100%; background: var(--accent); border-radius: 99px; }
.pill { display: inline-flex; align-items: center; gap: 5px; font-size: 10px; color: var(--accent); }
.pill::before { content: ""; width: 5px; height: 5px; border-radius: 50%; background: var(--accent); }
.pill.draft { color: var(--warn); }
.pill.draft::before { background: var(--warn); }
.side { display: flex; flex-direction: column; gap: 16px; }
.telemetry { display: grid; grid-template-columns: 1fr 1fr; gap: 1px; background: var(--line); }
.telemetry div { background: var(--panel); padding: 12px 14px; }
.telemetry span { display: block; font-size: 9px; color: var(--muted); letter-spacing: .06em; margin-bottom: 4px; }
.telemetry b { font-family: var(--mono); font-size: 15px; font-weight: 500; }
.log { padding: 10px 16px 14px; display: flex; flex-direction: column; gap: 9px; }
.log-row { display: grid; grid-template-columns: 38px 58px 1fr; gap: 8px; align-items: baseline; }
.log-row .time { color: #53606c; }
.log-row .kind { color: var(--accent); }
.log-row .kind.sys { color: var(--cyan); }
.log-row .msg { font-size: 10px; color: #9aa5b1; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.bottom { display: grid; grid-template-columns: 1.55fr 1fr; gap: 16px; }
.bars { height: 124px; display: flex; align-items: flex-end; gap: 5px; padding: 16px 16px 22px; }
.bar-col { flex: 1; min-height: 4px; border-radius: 2px 2px 0 0; background: linear-gradient(180deg, var(--accent), rgba(200,245,96,.12)); position: relative; }
.bar-col span { position: absolute; bottom: -16px; left: 50%; transform: translateX(-50%); font-family: var(--mono); font-size: 7px; color: #53606c; }
.users { display: flex; flex-direction: column; }
.user { display: flex; align-items: center; gap: 10px; padding: 10px 16px; border-bottom: 1px solid #14191e; }
.user:last-child { border-bottom: 0; }
.ava { width: 26px; height: 26px; border-radius: 50%; background: #1a2027; border: 1px solid #2a333d; display: grid; place-items: center; font-size: 9px; color: var(--accent); }
.info { flex: 1; }
.info b { display: block; font-size: 11px; font-weight: 500; }
.info span { display: block; font-size: 9px; color: var(--muted); margin-top: 2px; }
@media (max-width: 980px) {
  .kpis { grid-template-columns: 1fr 1fr; }
  .grid, .bottom { grid-template-columns: 1fr; }
}
</style>
