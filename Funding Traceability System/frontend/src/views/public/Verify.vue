<template>
  <div class="verify-page">
    <!-- 公开核验页：独立于系统内部布局，无侧边栏、无菜单、不可编辑 -->
    <header class="verify-top">
      <div class="brand">
        <span class="brand-mark"><i class="el-icon-lock"></i></span>
        <div class="brand-text">
          <div class="brand-name">助学资助资金溯源系统</div>
          <div class="brand-sub">资助凭证公开核验</div>
        </div>
      </div>
      <span class="public-pill">免登录 · 只读</span>
    </header>

    <main class="verify-main">
      <!-- 加载态 -->
      <section v-if="loading && !data" class="panel loading-panel">
        <div class="bone bone-lg"></div>
        <div class="bone bone-md"></div>
        <div class="bone bone-sm"></div>
        <div class="bone bone-block"></div>
      </section>

      <!-- 失败态 -->
      <section v-else-if="error" class="panel error-panel">
        <div class="error-icon"><i class="el-icon-warning-outline"></i></div>
        <h2 class="error-title">核验未通过</h2>
        <p class="error-msg">{{ error }}</p>
        <el-button type="primary" icon="el-icon-refresh" @click="load(true)">重新核验</el-button>
        <p class="error-hint">请确认二维码来源有效；如有疑问，请联系资助管理部门核实。</p>
      </section>

      <!-- 核验结果 -->
      <template v-else-if="data">
        <section class="panel hero" :class="'tone-' + statusTone">
          <div class="hero-top">
            <span class="hero-badge"><i class="el-icon-document-checked"></i> 核验结果</span>
            <span class="hero-time">{{ data.verifyTime }}</span>
          </div>

          <div class="hero-status">{{ data.statusText }}</div>

          <div class="hero-checks">
            <span class="check-chip" :class="{ 'is-done': data.auditCompleted }">
              <i :class="data.auditCompleted ? 'el-icon-circle-check' : 'el-icon-time'"></i>
              审核{{ data.auditCompleted ? '已完成' : '未完成' }}
            </span>
            <span class="check-chip" :class="{ 'is-done': data.disburseCompleted }">
              <i :class="data.disburseCompleted ? 'el-icon-circle-check' : 'el-icon-time'"></i>
              拨付{{ data.disburseCompleted ? '已完成' : '未完成' }}
            </span>
          </div>

          <div class="hero-grid">
            <div class="hero-item">
              <span class="hero-label">业务单号</span>
              <span class="hero-value mono">{{ data.businessNo }}</span>
            </div>
            <div class="hero-item">
              <span class="hero-label">资助金额</span>
              <span class="hero-value amount">¥ {{ formatAmount(data.amount) }}</span>
            </div>
          </div>
        </section>

        <section class="panel">
          <div class="panel-head">
            <div class="panel-title">审批流程节点</div>
            <span class="panel-desc">节点时间取自可信时间戳台账</span>
          </div>
          <ul class="timeline">
            <li
              v-for="node in data.nodes"
              :key="node.node"
              class="tl-item"
              :class="'state-' + (node.state || '').toLowerCase()"
            >
              <span class="tl-dot">
                <i v-if="node.state === 'DONE'" class="el-icon-check"></i>
                <i v-else-if="node.state === 'SKIPPED'" class="el-icon-minus"></i>
              </span>
              <div class="tl-body">
                <div class="tl-line">
                  <span class="tl-name">{{ node.nodeName }}</span>
                  <span class="tl-state">{{ stateText(node.state) }}</span>
                </div>
                <div class="tl-time">{{ node.time || skippedHint(node) }}</div>
              </div>
            </li>
          </ul>
        </section>

        <section class="live-bar">
          <span class="live-dot"></span>
          <span class="live-text">数据与业务单据实时联动</span>
          <span class="live-refresh" :disabled="refreshing" @click="load(true)">
            <i class="el-icon-refresh" :class="{ spinning: refreshing }"></i> {{ refreshing ? '刷新中' : '刷新' }}
          </span>
        </section>

        <footer class="verify-foot">
          <div class="foot-title"><i class="el-icon-lock"></i> 隐私保护说明</div>
          <p>本页面仅展示业务单号、资助金额、各审批节点时间与业务状态，<strong>不含学生姓名、学校、年级、资助人、用途等任何个人信息</strong>。</p>
          <p class="foot-dim">页面为只读核验入口，每次访问实时读取主业务单据，单据状态更新后展示内容自动同步。</p>
        </footer>
      </template>
    </main>
  </div>
</template>

<script>
import { publicVerify } from '@/api/verify'

const REFRESH_INTERVAL = 30000
const TONE_MAP = {
  SUBMITTED: 'warning',
  AUDITED: 'primary',
  REJECTED: 'danger',
  REVIEWED: 'primary',
  DISBURSED: 'success'
}
const STATE_TEXT = {
  DONE: '已完成',
  PENDING: '待处理',
  SKIPPED: '免复核'
}

export default {
  name: 'PublicVerify',
  data() {
    return {
      code: this.$route.params.code || '',
      data: null,
      error: '',
      loading: false,
      refreshing: false,
      timer: null
    }
  },
  computed: {
    statusTone() {
      return (this.data && TONE_MAP[this.data.status]) || 'primary'
    }
  },
  created() {
    document.title = '资助凭证核验 · 助学资助资金溯源系统'
    this.load(false)
    this.timer = setInterval(() => {
      // 页面不可见时不发起请求，回到前台后由用户手动刷新或等待下一轮
      if (document.visibilityState === 'visible') {
        this.load(true)
      }
    }, REFRESH_INTERVAL)
  },
  beforeDestroy() {
    if (this.timer) {
      clearInterval(this.timer)
      this.timer = null
    }
  },
  methods: {
    load(silent) {
      if (!this.code) {
        this.error = '核验地址缺少核验码'
        return
      }
      this.loading = !silent
      if (silent) {
        this.refreshing = true
      }
      publicVerify(this.code)
        .then(data => {
          this.data = data
          this.error = ''
        })
        .catch(err => {
          // 静默刷新失败时保留上一次结果，避免监事扫码过程中页面突然空白
          if (!silent || !this.data) {
            this.error = (err && err.message) || '核验失败，请稍后重试'
            if (err && err.code === 404) {
              this.data = null
            }
          }
        })
        .finally(() => {
          this.loading = false
          this.refreshing = false
        })
    },
    formatAmount(value) {
      if (value === undefined || value === null || value === '') return '0.00'
      return Number(value).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
    },
    stateText(state) {
      return STATE_TEXT[state] || '待处理'
    },
    skippedHint(node) {
      return node.state === 'SKIPPED' ? '小额资助免复核' : '尚未进行'
    }
  }
}
</script>

<style scoped>
.verify-page {
  min-height: 100vh;
  padding: 0 16px 40px;
  background: linear-gradient(180deg, #eaf2fb 0%, var(--c-bg-page) 220px);
}
.verify-top {
  max-width: 720px;
  margin: 0 auto;
  padding: 18px 0 14px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}
.brand-mark {
  width: 34px;
  height: 34px;
  border-radius: 9px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: var(--c-primary);
  color: #fff;
  font-size: 17px;
  flex: none;
  box-shadow: 0 2px 6px rgba(43, 108, 176, 0.28);
}
.brand-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--c-text-1);
  line-height: 1.3;
}
.brand-sub {
  font-size: 12px;
  color: var(--c-text-3);
  margin-top: 1px;
}
.public-pill {
  flex: none;
  padding: 3px 10px;
  border-radius: var(--r-pill);
  background: rgba(43, 108, 176, 0.1);
  color: var(--c-primary);
  font-size: 11px;
  border: 1px solid var(--c-primary-border);
}
.verify-main {
  max-width: 720px;
  margin: 0 auto;
}
.panel {
  background: var(--c-bg-card);
  border: 1px solid var(--c-border-light);
  border-radius: var(--r-card);
  box-shadow: var(--sh-1);
  padding: 18px;
  margin-bottom: 12px;
}
/* 状态色由 tone-* 变量驱动 */
.tone-primary { --tone: var(--c-primary); --tone-light: var(--c-primary-light); }
.tone-success { --tone: var(--c-success); --tone-light: var(--c-success-light); }
.tone-warning { --tone: var(--c-warning); --tone-light: var(--c-warning-light); }
.tone-danger { --tone: var(--c-danger); --tone-light: var(--c-danger-light); }

.hero-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.hero-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 9px;
  border-radius: var(--r-pill);
  background: var(--tone-light);
  color: var(--tone);
  font-size: 11px;
  font-weight: 600;
}
.hero-time {
  font-size: 11px;
  color: var(--c-text-4);
  font-variant-numeric: tabular-nums;
}
.hero-status {
  margin: 12px 0 2px;
  font-size: 24px;
  line-height: 1.3;
  font-weight: 700;
  color: var(--tone);
  letter-spacing: 0.5px;
}
.hero-checks {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin: 10px 0 16px;
}
.check-chip {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 4px 10px;
  border-radius: var(--r-pill);
  border: 1px solid var(--c-border);
  background: var(--c-bg-head);
  color: var(--c-text-3);
  font-size: 12px;
}
.check-chip.is-done {
  border-color: rgba(47, 158, 110, 0.28);
  background: var(--c-success-light);
  color: var(--c-success);
}
.hero-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 10px;
  padding-top: 14px;
  border-top: 1px dashed var(--c-border);
}
.hero-item {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
}
.hero-label {
  font-size: 12px;
  color: var(--c-text-3);
  flex: none;
}
.hero-value {
  font-size: 14px;
  font-weight: 600;
  color: var(--c-text-1);
  text-align: right;
  word-break: break-all;
}
.mono {
  font-family: Consolas, 'SFMono-Regular', Menlo, monospace;
  letter-spacing: 0.2px;
}
.hero-value.amount {
  font-size: 20px;
  font-weight: 700;
  color: var(--tone);
  font-variant-numeric: tabular-nums;
}

.panel-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 6px;
}
.panel-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--c-text-1);
}
.panel-desc {
  font-size: 11px;
  color: var(--c-text-4);
}
.timeline {
  list-style: none;
  margin: 0;
  padding: 6px 0 0;
}
.tl-item {
  position: relative;
  display: flex;
  gap: 12px;
  padding: 10px 0 10px 0;
}
.tl-item:not(:last-child)::before {
  content: '';
  position: absolute;
  left: 10px;
  top: 30px;
  bottom: -2px;
  width: 2px;
  background: var(--c-border-light);
}
.tl-item.state-done:not(:last-child)::before {
  background: rgba(47, 158, 110, 0.24);
}
.tl-dot {
  position: relative;
  z-index: 1;
  width: 22px;
  height: 22px;
  flex: none;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  background: var(--c-bg-head);
  border: 2px solid var(--c-border);
  color: var(--c-text-4);
}
.state-done .tl-dot {
  background: var(--c-success);
  border-color: var(--c-success);
  color: #fff;
}
.state-skipped .tl-dot {
  border-style: dashed;
  background: #fff;
  color: var(--c-text-4);
}
.tl-body {
  min-width: 0;
  flex: 1;
}
.tl-line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}
.tl-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--c-text-1);
}
.tl-state {
  font-size: 11px;
  color: var(--c-text-4);
}
.state-done .tl-state {
  color: var(--c-success);
}
.tl-time {
  margin-top: 3px;
  font-size: 12px;
  color: var(--c-text-3);
  font-variant-numeric: tabular-nums;
  word-break: break-all;
}
.state-pending .tl-time,
.state-skipped .tl-time {
  color: var(--c-text-4);
}

.live-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  padding: 10px 14px;
  margin-bottom: 12px;
  border-radius: var(--r-panel);
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid var(--c-border-light);
  font-size: 12px;
  color: var(--c-text-3);
}
.live-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--c-success);
  box-shadow: 0 0 0 3px rgba(47, 158, 110, 0.16);
  flex: none;
}
.live-text {
  color: var(--c-text-2);
}
.live-refresh {
  margin-left: auto;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: var(--c-primary);
  cursor: pointer;
  user-select: none;
}
.live-refresh[disabled] {
  color: var(--c-text-4);
  cursor: default;
}
.spinning {
  animation: spin 0.9s linear infinite;
}
@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.verify-foot {
  padding: 0 4px;
  font-size: 12px;
  line-height: 1.7;
  color: var(--c-text-3);
}
.foot-title {
  display: flex;
  align-items: center;
  gap: 5px;
  font-weight: 600;
  color: var(--c-text-2);
  margin-bottom: 4px;
}
.verify-foot p {
  margin: 0 0 4px;
}
.foot-dim {
  color: var(--c-text-4);
}

.loading-panel .bone {
  height: 12px;
  border-radius: 6px;
  margin-bottom: 12px;
  background: linear-gradient(90deg, #eef2f7 25%, #f7f9fc 37%, #eef2f7 63%);
  background-size: 400% 100%;
  animation: shimmer 1.4s ease infinite;
}
.bone-lg { height: 26px !important; width: 45%; }
.bone-md { width: 70%; }
.bone-sm { width: 35%; }
.bone-block { height: 150px !important; margin-top: 18px; }
@keyframes shimmer {
  0% { background-position: 100% 50%; }
  100% { background-position: 0 50%; }
}

.error-panel {
  text-align: center;
  padding: 32px 20px;
}
.error-icon {
  width: 54px;
  height: 54px;
  margin: 0 auto 12px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--c-danger-light);
  color: var(--c-danger);
  font-size: 26px;
}
.error-title {
  margin: 0 0 6px;
  font-size: 17px;
  color: var(--c-text-1);
}
.error-msg {
  margin: 0 0 18px;
  font-size: 13px;
  color: var(--c-text-3);
}
.error-hint {
  margin: 16px 0 0;
  font-size: 12px;
  color: var(--c-text-4);
}

@media (min-width: 640px) {
  .verify-page { padding: 0 24px 56px; }
  .verify-top { padding: 26px 0 18px; }
  .panel { padding: 24px 26px; margin-bottom: 14px; }
  .hero-status { font-size: 28px; }
  .hero-grid { grid-template-columns: 1fr 1fr; gap: 16px; }
  .hero-item { flex-direction: column; align-items: flex-start; gap: 4px; }
  .hero-value { text-align: left; }
  .hero-value.amount { font-size: 24px; }
  .tl-item { padding: 12px 0; }
  .tl-name { font-size: 14px; }
  .tl-time { font-size: 12.5px; }
}
</style>
