<template>
  <div class="page-container" v-loading="loading">
    <div class="header-bar">
      <el-button icon="el-icon-arrow-left" size="small" @click="$router.back()">返回</el-button>
      <span class="page-title">资助单详情 #{{ $route.params.id }}</span>
      <el-tag v-if="detail" :type="statusTagType(detail.status)" size="small">{{ statusText(detail.status) }}</el-tag>
    </div>

    <template v-if="detail">
      <!-- 顶部彩色进度条 -->
      <el-card shadow="never" class="steps-card">
        <el-steps :active="activeStep" align-center finish-status="success" process-status="process">
          <el-step title="提交" :description="detail.createTime" />
          <el-step
            title="审核"
            :status="detail.status === 'REJECTED' ? 'error' : undefined"
            :description="detail.status === 'REJECTED' ? ('已驳回 ' + (detail.auditTime || '')) : (detail.auditTime || '待审核')"
          />
          <el-step title="复核" :description="reviewStepDesc" />
          <el-step title="拨付" :description="detail.disburseTime || '待拨付'" />
        </el-steps>
      </el-card>

      <!-- 单据信息 -->
      <el-card shadow="never" class="info-card">
        <div slot="header" class="card-title">单据信息</div>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="学生">{{ detail.studentName }}</el-descriptions-item>
          <el-descriptions-item label="资助人">{{ detail.sponsorName }}</el-descriptions-item>
          <el-descriptions-item label="金额(元)">
            <span class="amount-text">{{ formatAmount(detail.amount) }}</span>
            <el-tag v-if="isBig" type="danger" size="mini" class="big-tag">大额</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="资助日期">{{ detail.fundDate }}</el-descriptions-item>
          <el-descriptions-item label="用途">
            <span v-if="detail.purpose">{{ detail.purpose }}</span>
            <span v-else class="masked-text">—</span>
          </el-descriptions-item>
          <el-descriptions-item label="备注">
            <span v-if="detail.remark">{{ detail.remark }}</span>
            <span v-else class="masked-text">—</span>
          </el-descriptions-item>
          <el-descriptions-item label="资助凭证号">
            <span v-if="detail.voucherNo" class="voucher-text"><i class="el-icon-lock"></i> {{ detail.voucherNo }}</span>
            <span v-else class="masked-text">审核通过后生成</span>
          </el-descriptions-item>
          <el-descriptions-item label="存证哈希(SHA-256)">
            <el-tooltip v-if="detail.voucherHash" :content="detail.voucherHash" placement="top">
              <span class="hash-text"><i class="el-icon-lock"></i> {{ shortHash(detail.voucherHash) }}</span>
            </el-tooltip>
            <span v-else class="masked-text">—</span>
          </el-descriptions-item>
          <el-descriptions-item label="审核人">{{ detail.auditorName || '—' }}</el-descriptions-item>
          <el-descriptions-item label="审核时间">{{ detail.auditTime || '—' }}</el-descriptions-item>
          <el-descriptions-item label="审核意见" :span="2">{{ detail.auditRemark || '—' }}</el-descriptions-item>
          <el-descriptions-item label="拨付人">{{ detail.disburserName || '—' }}</el-descriptions-item>
          <el-descriptions-item label="拨付时间">{{ detail.disburseTime || '—' }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ detail.createTime }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ detail.updateTime || '—' }}</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- 关联操作日志（仅审核管理员/监事可见） -->
      <el-card v-if="canViewLogs" shadow="never" class="log-card-wrap">
        <div slot="header" class="card-title">
          关联操作日志
          <span class="card-subtitle"><i class="el-icon-lock"></i> 只读存证，不可篡改</span>
        </div>
        <el-empty v-if="logs.length === 0" description="暂无关联日志" :image-size="60" />
        <el-timeline v-else class="mini-timeline">
          <el-timeline-item
            v-for="item in logs"
            :key="item.id"
            :timestamp="item.createTime"
            placement="top"
            :color="item.targetType === 'fund' ? '#409EFF' : '#E6A23C'"
          >
            <div class="mini-log">
              <span class="operation">{{ item.operation }}</span>
              <i v-if="item.targetType === 'fund'" class="el-icon-lock lock-icon" title="已存证"></i>
              <div class="mini-meta">{{ item.username }}（{{ roleText(item.role) }}）</div>
              <div v-if="item.content" class="mini-content">{{ item.content }}</div>
            </div>
          </el-timeline-item>
        </el-timeline>
      </el-card>
    </template>
  </div>
</template>

<script>
import { getFund } from '@/api/fund'
import { listOperationLogs } from '@/api/log'

const BIG_THRESHOLD = 5000

export default {
  name: 'FundDetail',
  data() {
    return {
      detail: null,
      logs: [],
      loading: false
    }
  },
  computed: {
    isBig() {
      return this.detail && Number(this.detail.amount) >= BIG_THRESHOLD
    },
    activeStep() {
      if (!this.detail) return 0
      const map = { SUBMITTED: 1, AUDITED: 2, REJECTED: 1, REVIEWED: 3, DISBURSED: 4 }
      return map[this.detail.status] || 0
    },
    reviewStepDesc() {
      if (!this.detail) return ''
      if (!this.isBig) return '小额免复核'
      const map = {
        REVIEWED: '复核通过',
        DISBURSED: '复核通过'
      }
      return map[this.detail.status] || '待抽检复核'
    },
    canViewLogs() {
      return ['AUDIT_ADMIN', 'SUPERVISOR'].includes(this.$store.getters.role)
    }
  },
  created() {
    this.getDetail()
    if (this.canViewLogs) {
      this.getLogs()
    }
  },
  methods: {
    formatAmount(val) {
      if (val === undefined || val === null) return '0.00'
      return Number(val).toFixed(2)
    },
    shortHash(hash) {
      return hash ? hash.substring(0, 16) + '…' : '—'
    },
    statusText(status) {
      const map = { SUBMITTED: '待审核', AUDITED: '审核通过', REJECTED: '已驳回', REVIEWED: '复核通过', DISBURSED: '已拨付' }
      return map[status] || status || '-'
    },
    statusTagType(status) {
      const map = { SUBMITTED: 'info', AUDITED: 'primary', REJECTED: 'danger', REVIEWED: 'warning', DISBURSED: 'success' }
      return map[status] || 'info'
    },
    roleText(role) {
      const map = {
        AUDIT_ADMIN: '审核管理员',
        DISBURSE_ADMIN: '拨付管理员',
        SUPERVISOR: '监事/审计',
        SPONSOR: '资助人'
      }
      return map[role] || role || '-'
    },
    getDetail() {
      this.loading = true
      getFund(this.$route.params.id).then(data => {
        this.detail = data
      }).finally(() => { this.loading = false })
    },
    getLogs() {
      listOperationLogs({ page: 1, size: 20, targetType: 'fund', targetId: this.$route.params.id }).then(data => {
        this.logs = (data && data.records) || []
      }).catch(() => {})
    }
  }
}
</script>

<style scoped>
.page-container { background:#fff; padding:16px; border-radius:4px; }
.header-bar { display:flex; align-items:center; gap:12px; margin-bottom:16px; }
.page-title { font-size:16px; font-weight:600; }
.steps-card { margin-bottom:16px; }
.info-card { margin-bottom:16px; }
.card-title { font-weight:600; display:flex; align-items:center; gap:10px; }
.card-subtitle { font-weight:400; font-size:12px; color:#E6A23C; }
.amount-text { font-weight:700; color:#303133; }
.big-tag { margin-left:8px; }
.voucher-text { font-size:13px; color:#606266; }
.hash-text { font-family:Consolas, monospace; font-size:12px; color:#909399; cursor:default; }
.masked-text { color:#C0C4CC; }
.log-card-wrap { margin-bottom:16px; }
.mini-timeline { padding-left:4px; }
.mini-log .operation { font-weight:600; color:#303133; font-size:13px; }
.lock-icon { color:#E6A23C; margin-left:6px; }
.mini-meta { font-size:12px; color:#909399; margin-top:4px; }
.mini-content { font-size:12px; color:#606266; margin-top:4px; word-break:break-all; }
</style>
