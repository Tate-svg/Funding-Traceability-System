<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <div class="page-title">溯源台账</div>
        <div class="page-subtitle">每个业务节点自动生成时间戳存证，哈希成链，逐条可核验</div>
      </div>
      <div class="page-actions">
        <el-button icon="el-icon-download" :loading="exporting === 'pdf'" @click="exportAudit('pdf')">导出 PDF</el-button>
        <el-button icon="el-icon-picture-outline" :loading="exporting === 'png'" @click="exportAudit('png')">导出图片</el-button>
        <el-button type="primary" icon="el-icon-check" @click="verifyAll">核验全链</el-button>
      </div>
    </div>

    <div class="kpi-row">
      <div class="kpi-card">
        <div class="kpi-label">台账总数</div>
        <div class="kpi-value">{{ total }}</div>
        <div class="kpi-hint">条时间戳存证记录</div>
      </div>
      <div class="kpi-card kpi-card--success">
        <div class="kpi-label">本页可信存证</div>
        <div class="kpi-value">{{ list.length - anomalyInPage }}</div>
        <div class="kpi-hint">本页 {{ list.length }} 条中的存证数</div>
      </div>
      <div class="kpi-card" :class="anomalyInPage > 0 ? 'kpi-card--danger' : ''">
        <div class="kpi-label">本页时序异常</div>
        <div class="kpi-value">{{ anomalyInPage }}</div>
        <div class="kpi-hint">条时间戳顺序异常</div>
      </div>
    </div>

    <div class="toolbar">
      <el-select v-model="listQuery.bizType" placeholder="业务类型" clearable style="width:140px" @change="handleFilter">
        <el-option label="资助单" value="fund" />
        <el-option label="学生资料" value="student" />
        <el-option label="回访" value="visit" />
        <el-option label="评分" value="score" />
        <el-option label="复核任务" value="review_task" />
        <el-option label="用户" value="sys_user" />
      </el-select>
      <el-select v-model="listQuery.node" placeholder="业务节点" clearable style="width:140px" @change="handleFilter">
        <el-option v-for="n in nodeOptions" :key="n.value" :label="n.label" :value="n.value" />
      </el-select>
      <el-input v-model="listQuery.keyword" placeholder="摘要 / 操作人关键词" clearable style="width:220px" @keyup.enter.native="handleFilter" />
      <el-button type="primary" icon="el-icon-search" @click="handleFilter">查询</el-button>
      <el-button icon="el-icon-refresh" @click="handleReset">重置</el-button>
    </div>

    <div v-loading="listLoading" class="ledger-wrap">
      <EmptyState v-if="!listLoading && list.length === 0" type="ledger" title="暂无时间戳台账记录" desc="业务操作发生后，系统会自动生成不可篡改的时间戳存证" />
      <div v-else class="ledger-list">
        <div
          v-for="item in list"
          :key="item.id"
          class="ledger-card"
          :class="[nodeClass(item.node), 'op-' + opType(item), { 'is-anomaly': item.anomaly === 1 }]"
        >
          <div class="ts-badge" :style="{ background: nodeColor(item.node) }">
            <div class="ts-serial">#{{ item.serialNo }}</div>
            <div class="ts-time">{{ item.tsTime }}</div>
          </div>
          <div class="card-body">
            <div class="card-head">
              <span class="node-tag" :style="{ background: nodeColor(item.node) }">{{ item.nodeName }}</span>
              <span class="biz-text">{{ bizTypeText(item.bizType) }}<template v-if="item.bizId"> #{{ item.bizId }}</template></span>
              <el-tag v-if="item.anomaly === 1" type="danger" size="mini" effect="dark">时序异常</el-tag>
              <span class="flex-fill"></span>
              <el-button v-if="item.bizType === 'fund' && item.bizId" type="text" size="mini" @click="goDetail(item)">
                查看单据 <i class="el-icon-arrow-right"></i>
              </el-button>
            </div>
            <div class="card-summary">{{ item.summary }}</div>
            <div class="card-meta">
              <span>操作人：{{ item.operatorName || '—' }}</span>
              <el-tag :type="roleTagType(item.role)" size="mini">{{ roleText(item.role) }}</el-tag>
              <span class="op-flag" :class="'op-' + opType(item)">
                <i :class="opIcon(item)"></i> {{ opLabel(item) }}
              </span>
              <el-tooltip :content="item.tsHash || '暂无哈希'" placement="top">
                <span class="hash-text"><i class="el-icon-lock"></i> {{ shortHash(item.tsHash) }}</span>
              </el-tooltip>
              <span class="stamp-flag"><i class="el-icon-circle-check"></i> 可信存证</span>
            </div>
            <div v-if="item.anomaly === 1" class="anomaly-reason">
              <i class="el-icon-warning-outline"></i> {{ item.anomalyReason }}
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="pagination-bar">
      <el-pagination
        background
        :current-page="listQuery.page"
        :page-size="listQuery.size"
        :page-sizes="[10, 20, 50]"
        :total="total"
        layout="total, sizes, prev, pager, next"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>

    <!-- 全链核验结果弹窗 -->
    <el-dialog title="时间戳哈希链核验" :visible.sync="verifyVisible" width="520px">
      <template v-if="verifyResult">
        <el-alert
          :type="verifyResult.valid ? 'success' : 'error'"
          :closable="false"
          show-icon
          :title="verifyResult.valid ? '哈希链核验通过：全部记录与链上哈希一致，未发现篡改' : ('哈希链核验失败：序列 #' + verifyResult.chainBrokenAt + ' 起与链上哈希不一致，疑似篡改')"
        />
        <div class="verify-stats">
          <span>台账总数：<b>{{ verifyTotal }}</b> 条</span>
          <span>时序异常：<b :class="{ 'danger-text': verifyResult.anomalyCount > 0 }">{{ verifyResult.anomalyCount }}</b> 条</span>
        </div>
      </template>
      <div slot="footer">
        <el-button type="primary" @click="verifyVisible = false">关 闭</el-button>
      </div>
    </el-dialog>

    <!-- 审计专用页面水印：账号 + 访问时间，离开本页自动销毁 -->
    <audit-watermark />
  </div>
</template>

<script>
import { listTimestamps, verifyTimestamps, exportAuditTrail } from '@/api/trace'
import { downloadFile } from '@/utils/download'
import EmptyState from '@/components/EmptyState.vue'
import AuditWatermark from '@/components/AuditWatermark.vue'

// 业务节点差异化色卡（统一到全站设计令牌色系，降低饱和度）
const NODE_COLORS = {
  SUBMIT: '#2b6cb0',        // 提交申请 蓝
  AUDIT_PASS: '#2f9e6e',    // 审核通过 绿
  AUDIT_REJECT: '#c24a45',  // 审核驳回 红
  REVIEW: '#c07f2e',        // 复核确认 橙
  REVIEW_DRAW: '#9e6420',   // 大额抽检 深橙
  DISBURSE: '#a88523',      // 资金拨付 金
  VOUCHER_MODIFY: '#6b5bb5',// 凭证修改 紫
  VISIT_RECORD: '#2f8f9e',  // 回访录入 青
  STUDENT_UPLOAD: '#22998c',// 资料上传 蓝绿
  OTHER: '#7a8798'          // 其他 灰
}

export default {
  name: 'TraceLedger',
  components: { EmptyState, AuditWatermark },
  data() {
    return {
      list: [],
      total: 0,
      listLoading: false,
      exporting: '',
      listQuery: { page: 1, size: 10, bizType: undefined, node: undefined, keyword: '' },
      verifyVisible: false,
      verifyResult: null,
      nodeOptions: [
        { value: 'SUBMIT', label: '提交申请' },
        { value: 'AUDIT_PASS', label: '审核通过' },
        { value: 'AUDIT_REJECT', label: '审核驳回' },
        { value: 'REVIEW_DRAW', label: '大额抽检' },
        { value: 'REVIEW', label: '复核确认' },
        { value: 'DISBURSE', label: '资金拨付' },
        { value: 'VOUCHER_MODIFY', label: '凭证修改' },
        { value: 'VISIT_RECORD', label: '回访录入' },
        { value: 'STUDENT_UPLOAD', label: '资料上传' },
        { value: 'OTHER', label: '其他操作' }
      ]
    }
  },
  created() {
    this.getList()
  },
  computed: {
    verifyTotal() {
      return (this.verifyResult && this.verifyResult.total != null) ? this.verifyResult.total : 0
    },
    // 仅用于顶部统计展示，不改变任何台账逻辑
    anomalyInPage() {
      return this.list.filter(item => item.anomaly === 1).length
    }
  },
  methods: {
    nodeColor(node) {
      return NODE_COLORS[node] || NODE_COLORS.OTHER
    },
    nodeClass(node) {
      return 'node-' + String(node || 'OTHER').toLowerCase()
    },
    bizTypeText(bizType) {
      const map = { fund: '资助单', student: '学生资料', visit: '回访', score: '评分', score_item: '评分指标', review_task: '复核任务', sys_user: '用户', system: '系统' }
      return map[bizType] || bizType || '-'
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
    roleTagType(role) {
      const map = { AUDIT_ADMIN: 'danger', DISBURSE_ADMIN: 'warning', SUPERVISOR: 'primary', SPONSOR: 'success' }
      return map[role] || 'info'
    },
    // 归纳操作类型：新增 / 修改 / 删除（仅用于视觉标注，不改动字段本身）
    // 优先依据摘要语义判定，摘要无明确动作词时回落到业务节点
    opType(item) {
      const summary = (item && item.summary) || ''
      if (summary.indexOf('删除') > -1) return 'del'
      if (summary.indexOf('修改') > -1 || summary.indexOf('更新') > -1) return 'edit'
      if (summary.indexOf('新增') > -1 || summary.indexOf('录入') > -1 || summary.indexOf('上传') > -1 || summary.indexOf('提交') > -1) return 'add'
      const node = item && item.node
      if (node === 'SUBMIT' || node === 'VISIT_RECORD' || node === 'STUDENT_UPLOAD' || node === 'DISBURSE') return 'add'
      return 'edit'
    },
    opLabel(item) {
      const map = { add: '新增', edit: '修改', del: '删除' }
      return map[this.opType(item)]
    },
    opIcon(item) {
      const map = { add: 'el-icon-plus', edit: 'el-icon-edit', del: 'el-icon-delete' }
      return map[this.opType(item)]
    },
    shortHash(hash) {
      if (!hash) return '—'
      if (hash.length <= 16) return hash
      return hash.substring(0, 8) + '…' + hash.substring(hash.length - 6)
    },
    getList() {
      this.listLoading = true
      listTimestamps(this.listQuery).then(data => {
        this.list = (data && data.records) || []
        this.total = (data && data.total) || 0
      }).finally(() => { this.listLoading = false })
    },
    handleFilter() { this.listQuery.page = 1; this.getList() },
    handleReset() {
      this.listQuery.bizType = undefined
      this.listQuery.node = undefined
      this.listQuery.keyword = ''
      this.listQuery.page = 1
      this.getList()
    },
    handleSizeChange(size) { this.listQuery.size = size; this.listQuery.page = 1; this.getList() },
    handleCurrentChange(page) { this.listQuery.page = page; this.getList() },
    goDetail(item) {
      this.$router.push('/fund/detail/' + item.bizId)
    },
    verifyAll() {
      verifyTimestamps({}).then(data => {
        this.verifyResult = data
        this.verifyVisible = true
      }).catch(() => {})
    },
    // 审计专用导出：文件由服务端渲染并自带「账号 + 导出时间」水印
    exportAudit(format) {
      if (this.exporting) return
      this.exporting = format
      const params = {
        bizType: this.listQuery.bizType || undefined,
        node: this.listQuery.node || undefined,
        keyword: this.listQuery.keyword || undefined
      }
      exportAuditTrail(format, params).then(blob => {
        const now = new Date()
        const pad = n => (n < 10 ? '0' + n : '' + n)
        const stamp = '' + now.getFullYear() + pad(now.getMonth() + 1) + pad(now.getDate()) +
          '_' + pad(now.getHours()) + pad(now.getMinutes()) + pad(now.getSeconds())
        return downloadFile(blob, '审计溯源单_' + stamp + '.' + format)
      }).catch(err => {
        this.$message.error((err && err.message) || '导出失败，请稍后重试')
      }).finally(() => {
        this.exporting = ''
      })
    }
  }
}
</script>

<style scoped>
.ledger-wrap { min-height:200px; }
.ledger-list { display:flex; flex-direction:column; gap:12px; }

/* 台账卡片：左侧时间戳徽章 + 节点色卡 + 可信存证水印 */
.ledger-card {
  position:relative; overflow:hidden;
  display:flex; align-items:stretch;
  border:1px solid var(--c-border-light); border-left-width:4px;
  border-radius:var(--r-panel);
  background:var(--c-bg-card);
  box-shadow:var(--sh-1);
  transition:box-shadow var(--t-fast), transform var(--t-fast);
}
.ledger-card:hover { box-shadow:var(--sh-2); transform:translateY(-1px); }
.ledger-card::before {
  content:'可信存证 · 不可篡改';
  position:absolute; right:-20px; top:50%;
  transform:translateY(-50%) rotate(-24deg);
  font-size:26px; font-weight:700; letter-spacing:6px;
  color:rgba(43,108,176,.06); white-space:nowrap; pointer-events:none;
}
.ledger-card.is-anomaly { border-color:var(--c-danger); background:var(--c-danger-light); }
.ledger-card.is-anomaly::before { color:rgba(194,74,69,.08); }

.ts-badge {
  flex:0 0 118px; display:flex; flex-direction:column; align-items:center; justify-content:center;
  color:#fff; padding:10px 6px; gap:4px;
}
.ts-serial { font-size:18px; font-weight:700; font-family:Consolas, monospace; }
.ts-time { font-size:11px; opacity:.92; text-align:center; line-height:1.5; }

.card-body { flex:1; padding:10px 16px; min-width:0; }
.card-head { display:flex; align-items:center; gap:8px; flex-wrap:wrap; }
.node-tag { color:#fff; font-size:12px; padding:2px 10px; border-radius:var(--r-pill); line-height:18px; }
.biz-text { font-weight:600; color:var(--c-text-1); font-size:13px; }
.card-summary { margin-top:6px; font-size:13px; color:var(--c-text-2); word-break:break-all; }
.card-meta { margin-top:6px; font-size:12px; color:var(--c-text-3); display:flex; align-items:center; gap:12px; flex-wrap:wrap; }
.card-meta .hash-text { display:inline-flex; align-items:center; gap:4px; }
.stamp-flag {
  display:inline-flex; align-items:center; gap:4px;
  color:var(--c-success); background:var(--c-success-light); border:1px solid #cbe9dc;
  font-size:11px; line-height:18px; padding:0 8px; border-radius:var(--r-tag);
}
.anomaly-reason { margin-top:6px; font-size:12px; color:var(--c-danger); }
.danger-text { color:var(--c-danger); }
.verify-stats { margin-top:12px; display:flex; gap:24px; font-size:13px; color:var(--c-text-2); }

/* 节点色卡左边框与徽章同色（仅作兜底，实际由下方操作类型色条覆盖） */
.node-submit { border-left-color:#2b6cb0; }
.node-audit_pass { border-left-color:#2f9e6e; }
.node-audit_reject { border-left-color:#c24a45; }
.node-review { border-left-color:#c07f2e; }
.node-review_draw { border-left-color:#9e6420; }
.node-disburse { border-left-color:#a88523; }
.node-voucher_modify { border-left-color:#6b5bb5; }
.node-visit_record { border-left-color:#2f8f9e; }
.node-student_upload { border-left-color:#22998c; }
.node-other { border-left-color:#7a8798; }

/* 左侧色条按操作类型区分：新增绿 / 修改蓝 / 删除红 */
.ledger-card.op-add { border-left-color:var(--c-success); --op-color:var(--c-success); }
.ledger-card.op-edit { border-left-color:var(--c-primary); --op-color:var(--c-primary); }
.ledger-card.op-del { border-left-color:var(--c-danger); --op-color:var(--c-danger); }
/* 操作类型色条置于卡片最左侧，避免被时间戳徽章底色覆盖 */
.ledger-card::after {
  content:''; position:absolute; left:0; top:0; bottom:0; width:4px;
  background:var(--op-color,#cfd8e3); z-index:2; pointer-events:none;
}
.op-flag {
  display:inline-flex; align-items:center; gap:3px;
  font-size:11px; line-height:18px; padding:0 8px;
  border-radius:var(--r-tag); border:1px solid transparent;
}
.op-flag.op-add { color:var(--c-success); background:var(--c-success-light); border-color:#cbe9dc; }
.op-flag.op-edit { color:var(--c-primary); background:var(--c-primary-light); border-color:var(--c-primary-border); }
.op-flag.op-del { color:var(--c-danger); background:var(--c-danger-light); border-color:#f2d3d1; }
/* 时序异常保留红色边框与浅红底，覆盖操作色条 */
.ledger-card.is-anomaly { border-left-color:var(--c-danger); }

.pagination-bar { margin-top:16px; text-align:right; }

@media (max-width: 768px) {
  .ledger-card { flex-direction:column; }
  .ts-badge { flex:0 0 auto; flex-direction:row; justify-content:flex-start; gap:10px; padding:8px 12px; }
  .ts-badge .ts-time { text-align:left; }
}
</style>
