<template>
  <div class="page-container" v-loading="loading">
    <div class="page-header">
      <div class="detail-head">
        <el-button icon="el-icon-arrow-left" size="small" @click="$router.back()">返回</el-button>
        <div>
          <div class="page-title">资助单详情 #{{ $route.params.id }}</div>
          <div class="page-subtitle">全流程进度、单据信息与可信时间戳溯源</div>
        </div>
      </div>
      <div class="page-actions">
        <el-tag v-if="detail" :type="statusTagType(detail.status)" size="small">{{ statusText(detail.status) }}</el-tag>
      </div>
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

      <!-- 溯源核验二维码（对外只读公开核验，不含任何隐私字段） -->
      <el-card shadow="never" class="qr-card">
        <div slot="header" class="card-title">
          溯源核验二维码
          <span class="card-subtitle"><i class="el-icon-lock"></i> 监事 / 审计人员免登录扫码核验，仅展示单号、金额、节点时间与状态</span>
        </div>
        <div class="qr-body">
          <div class="qr-img-wrap">
            <img v-if="qrcode && qrcode.qrImage" :src="qrcode.qrImage" class="qr-img" alt="资助凭证溯源核验二维码">
            <div v-else class="qr-placeholder">
              <i class="el-icon-loading"></i>
              <span>二维码生成中…</span>
            </div>
          </div>
          <div class="qr-side">
            <div class="qr-row">
              <span class="qr-label">业务单号</span>
              <span class="qr-value mono">{{ qrcode ? qrcode.businessNo : '—' }}</span>
            </div>
            <div class="qr-row">
              <span class="qr-label">当前状态</span>
              <el-tag v-if="qrcode" size="mini" :type="statusTagType(qrcode.status)">{{ qrcode.statusText }}</el-tag>
              <span v-else class="masked-text">—</span>
            </div>
            <div class="qr-row">
              <span class="qr-label">核验地址</span>
              <span v-if="qrcode" class="qr-url" :title="qrcode.verifyUrl" @click="openVerify">{{ qrcode.verifyUrl }}</span>
              <span v-else class="masked-text">—</span>
            </div>
            <div class="qr-actions">
              <el-button type="primary" size="small" icon="el-icon-link" :disabled="!qrcode" @click="openVerify">打开核验页</el-button>
              <el-button size="small" icon="el-icon-copy-document" :disabled="!qrcode" @click="copyVerifyLink">复制链接</el-button>
              <el-button size="small" icon="el-icon-download" :disabled="!qrcode" @click="downloadQr">下载二维码</el-button>
            </div>
            <p class="qr-tip">
              核验页为只读公开入口，与内部业务页面隔离：仅展示业务单号、资助金额、各审批节点时间与业务状态，不含学生任何隐私信息。单据状态更新后，扫码展示内容实时同步。
            </p>
          </div>
        </div>
      </el-card>

      <!-- 可信时间戳溯源（仅审核管理员/监事可见） -->
      <el-card v-if="canViewLogs" shadow="never" class="log-card-wrap">
        <div slot="header" class="card-title">
          可信时间戳溯源
          <span class="card-subtitle"><i class="el-icon-lock"></i> 全流程盖章存证，不可篡改</span>
        </div>
        <trace-timeline biz-type="fund" :biz-id="$route.params.id" />
      </el-card>

      <!-- 关联回访凭证（脱敏后纳入全链路溯源台账） -->
      <el-card shadow="never" class="visit-card">
        <div slot="header" class="card-title">
          关联回访凭证
          <span class="card-subtitle"><i class="el-icon-lock"></i> 回访材料已脱敏，资助-拨付-回访业务闭环</span>
        </div>
        <EmptyState v-if="visits.length === 0" type="folder" title="暂无关联回访" desc="回访记录录入后，将在这里展示脱敏凭证" />
        <el-table v-else :data="visits" border size="mini">
          <el-table-column prop="visitDate" label="回访日期" width="110" />
          <el-table-column prop="visitor" label="回访人" width="100" />
          <el-table-column label="脱敏回访记录" min-width="240" show-overflow-tooltip>
            <template slot-scope="{ row }">
              <span v-if="row.maskedRecord">{{ row.maskedRecord }}</span>
              <span v-else class="masked-text">—</span>
            </template>
          </el-table-column>
          <el-table-column label="脱敏凭证" width="100" align="center">
            <template slot-scope="{ row }">
              <el-button v-if="row.maskedMaterial" type="text" icon="el-icon-view" @click="downloadMaterial(row)">查看</el-button>
              <span v-else class="masked-text">—</span>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </template>
  </div>
</template>

<script>
import { getFund } from '@/api/fund'
import { getFundQrCode } from '@/api/verify'
import { listVisits, downloadVisitMaterial } from '@/api/visit'
import { downloadFile } from '@/utils/download'
import TraceTimeline from '@/views/trace/TraceTimeline.vue'
import EmptyState from '@/components/EmptyState.vue'

const BIG_THRESHOLD = 5000

export default {
  name: 'FundDetail',
  components: { TraceTimeline, EmptyState },
  data() {
    return {
      detail: null,
      visits: [],
      qrcode: null,
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
    this.getVisits()
    this.getQrCode()
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
    getDetail() {
      this.loading = true
      getFund(this.$route.params.id).then(data => {
        this.detail = data
      }).finally(() => { this.loading = false })
    },
    getVisits() {
      listVisits({ page: 1, size: 50, fundId: this.$route.params.id }).then(data => {
        this.visits = (data && data.records) || []
      }).catch(() => {})
    },
    getQrCode() {
      getFundQrCode(this.$route.params.id).then(data => {
        this.qrcode = data
      }).catch(() => {
        this.qrcode = null
      })
    },
    openVerify() {
      if (!this.qrcode || !this.qrcode.verifyUrl) return
      window.open(this.qrcode.verifyUrl, '_blank')
    },
    copyVerifyLink() {
      if (!this.qrcode || !this.qrcode.verifyUrl) return
      this.copyText(this.qrcode.verifyUrl).then(() => {
        this.$message.success('核验链接已复制')
      }).catch(() => {
        this.$message.warning('复制失败，请手动选择核验地址复制')
      })
    },
    copyText(text) {
      if (navigator.clipboard && navigator.clipboard.writeText) {
        return navigator.clipboard.writeText(text)
      }
      return new Promise((resolve, reject) => {
        const textarea = document.createElement('textarea')
        textarea.value = text
        textarea.setAttribute('readonly', '')
        textarea.style.position = 'fixed'
        textarea.style.top = '-9999px'
        document.body.appendChild(textarea)
        textarea.select()
        try {
          const ok = document.execCommand('copy')
          document.body.removeChild(textarea)
          ok ? resolve() : reject(new Error('copy failed'))
        } catch (e) {
          document.body.removeChild(textarea)
          reject(e)
        }
      })
    },
    downloadQr() {
      if (!this.qrcode || !this.qrcode.qrImage) return
      const link = document.createElement('a')
      link.href = this.qrcode.qrImage
      link.download = '资助凭证核验二维码_' + this.qrcode.businessNo + '.png'
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
    },
    downloadMaterial(row) {
      downloadVisitMaterial(row.id).then(blob => {
        const ext = (row.maskedMaterial || '').split('.').pop() || 'dat'
        return downloadFile(blob, '回访脱敏凭证_' + row.id + '.' + ext)
      }).catch(err => {
        if (err && err.message) this.$message.error(err.message)
      })
    }
  }
}
</script>

<style scoped>
.detail-head { display:flex; align-items:center; gap:12px; }
.steps-card { margin-bottom:16px; }
.info-card { margin-bottom:16px; }
.card-title { font-weight:600; display:flex; align-items:center; gap:10px; color:var(--c-text-1); }
.card-subtitle { font-weight:400; font-size:12px; color:var(--c-warning); }
.amount-text { font-weight:700; color:var(--c-text-1); font-variant-numeric:tabular-nums; }
.big-tag { margin-left:8px; }
.voucher-text { font-size:13px; color:var(--c-text-2); }
.hash-text { font-family:Consolas, monospace; font-size:12px; color:var(--c-text-3); cursor:default; }
.masked-text { color:var(--c-text-4); }
.qr-card { margin-bottom:16px; }
.qr-body { display:flex; gap:20px; align-items:center; }
.qr-img-wrap {
  width:180px; height:180px; flex:none; padding:8px;
  display:flex; align-items:center; justify-content:center;
  border:1px solid var(--c-border-light); border-radius:var(--r-panel);
  background:var(--c-bg-head);
}
.qr-img { width:100%; height:100%; object-fit:contain; display:block; }
.qr-placeholder { display:flex; flex-direction:column; align-items:center; gap:6px; color:var(--c-text-4); font-size:12px; }
.qr-side { flex:1; min-width:0; display:flex; flex-direction:column; gap:10px; }
.qr-row { display:flex; align-items:center; gap:12px; font-size:13px; }
.qr-label { width:64px; flex:none; color:var(--c-text-3); font-size:12px; }
.qr-value { color:var(--c-text-1); font-weight:600; word-break:break-all; }
.mono { font-family:Consolas, monospace; }
.qr-url { font-size:12px; color:var(--c-primary); cursor:pointer; word-break:break-all; }
.qr-url:hover { text-decoration:underline; }
.qr-actions { display:flex; flex-wrap:wrap; gap:8px; margin-top:2px; }
.qr-actions .el-button + .el-button { margin-left:0; }
.qr-tip { margin:0; font-size:12px; line-height:1.6; color:var(--c-text-4); }
.log-card-wrap { margin-bottom:16px; }
.visit-card { margin-bottom:16px; }
@media (max-width: 768px) {
  .detail-head { align-items:flex-start; }
  .detail-head > div { min-width:0; }
  .qr-body { flex-direction:column; align-items:stretch; }
  .qr-img-wrap { width:160px; height:160px; align-self:center; }
  .qr-row { align-items:flex-start; }
}
</style>
