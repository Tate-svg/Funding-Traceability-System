<template>
  <div class="page-container">
    <!-- 顶部统计区：不对称栅格 + 差异化渐变 -->
    <div class="stat-row">
      <div class="stat-card stat-card--main">
        <div class="stat-inner">
          <div class="stat-icon"><i class="el-icon-s-check"></i></div>
          <div class="stat-info">
            <div class="stat-label">待审核申请</div>
            <div class="stat-value-row">
              <span class="stat-num">{{ pendingCount }}</span>
              <span class="stat-unit">笔</span>
            </div>
            <div class="stat-hint">资助申请待处理</div>
          </div>
        </div>
        <div class="stat-progress">
          <div class="stat-progress-head">
            <span>本页大额占比</span>
            <span class="stat-progress-val">{{ bigRatio }}%</span>
          </div>
          <div class="stat-progress-track">
            <div class="stat-progress-fill" :style="{ width: bigRatio + '%' }"></div>
          </div>
        </div>
      </div>

      <div class="stat-card stat-card--side">
        <div class="stat-inner">
          <div class="stat-icon"><i class="el-icon-warning-outline"></i></div>
          <div class="stat-info">
            <div class="stat-label">本页大额待审（≥5000元）</div>
            <div class="stat-value-row">
              <span class="stat-num">{{ bigCount }}</span>
              <span class="stat-unit">笔</span>
            </div>
            <div class="stat-hint">需重点关注的资助款项</div>
          </div>
        </div>
        <div class="stat-legend">
          <div class="legend-item">
            <span class="legend-dot legend-dot--normal"></span>
            <span>本页待审</span>
            <b>{{ list.length }}</b>
          </div>
          <div class="legend-item">
            <span class="legend-dot legend-dot--big"></span>
            <span>大额待审</span>
            <b>{{ bigCount }}</b>
          </div>
        </div>
      </div>
    </div>

    <el-tabs v-model="activeTab" class="workbench-tabs">
      <!-- 待审核：双列分流队列 -->
      <el-tab-pane label="待审核申请" name="pending">
        <div v-loading="listLoading">
          <EmptyState
            v-if="!listLoading && list.length === 0"
            type="todo"
            title="当前没有待审核事项"
            desc="审核队列已清空，暂无待办"
          />

          <div v-else class="queue-grid">
            <section class="queue-col">
              <header class="queue-head">
                <span class="queue-title">待初审</span>
                <span class="queue-count">{{ normalList.length }}</span>
                <span class="queue-sub">常规金额</span>
              </header>

              <article v-for="item in normalList" :key="item.id" class="queue-card">
                <div class="qc-head">
                  <span class="qc-name">{{ item.studentName }}</span>
                </div>
                <div class="qc-amount">¥ {{ formatAmount(item.amount) }}</div>
                <div class="qc-meta">
                  <span class="qc-label">资助人</span>
                  <span class="qc-value" :title="item.sponsorName">{{ item.sponsorName }}</span>
                </div>
                <div class="qc-meta">
                  <span class="qc-label">资助日期</span>
                  <span class="qc-value">{{ item.fundDate }}</span>
                </div>
                <div class="qc-meta">
                  <span class="qc-label">用途</span>
                  <span class="qc-value" :title="item.purpose">{{ item.purpose || '—' }}</span>
                </div>
                <div class="qc-meta">
                  <span class="qc-label">备注</span>
                  <span class="qc-value" :title="item.remark">{{ item.remark || '—' }}</span>
                </div>
                <div class="qc-meta qc-meta--muted">
                  <span class="qc-label">提交时间</span>
                  <span class="qc-value">{{ item.createTime }}</span>
                </div>
                <div class="qc-foot">
                  <el-button type="primary" size="mini" icon="el-icon-check" @click="handleAudit(item, 'PASS')">通过</el-button>
                  <el-button size="mini" plain icon="el-icon-close" class="btn-reject" @click="handleAudit(item, 'REJECT')">驳回</el-button>
                  <el-button type="text" size="mini" icon="el-icon-view" @click="handleDetail(item)">详情</el-button>
                </div>
              </article>

              <div v-if="normalList.length === 0" class="queue-empty">本页暂无常规金额申请</div>
            </section>

            <section class="queue-col queue-col--alert">
              <header class="queue-head queue-head--alert">
                <span class="queue-title">大额待审</span>
                <span class="queue-count queue-count--alert">{{ bigList.length }}</span>
                <span class="queue-sub">≥ 5000 元</span>
              </header>

              <article v-for="item in bigList" :key="item.id" class="queue-card queue-card--alert">
                <span class="amount-badge">¥ {{ formatAmount(item.amount) }}</span>
                <div class="qc-head">
                  <span class="qc-name">{{ item.studentName }}</span>
                </div>
                <div class="qc-meta">
                  <span class="qc-label">资助人</span>
                  <span class="qc-value" :title="item.sponsorName">{{ item.sponsorName }}</span>
                </div>
                <div class="qc-meta">
                  <span class="qc-label">资助日期</span>
                  <span class="qc-value">{{ item.fundDate }}</span>
                </div>
                <div class="qc-meta">
                  <span class="qc-label">用途</span>
                  <span class="qc-value" :title="item.purpose">{{ item.purpose || '—' }}</span>
                </div>
                <div class="qc-meta">
                  <span class="qc-label">备注</span>
                  <span class="qc-value" :title="item.remark">{{ item.remark || '—' }}</span>
                </div>
                <div class="qc-meta qc-meta--muted">
                  <span class="qc-label">提交时间</span>
                  <span class="qc-value">{{ item.createTime }}</span>
                </div>
                <div class="qc-foot">
                  <el-button type="primary" size="mini" icon="el-icon-check" @click="handleAudit(item, 'PASS')">通过</el-button>
                  <el-button size="mini" plain icon="el-icon-close" class="btn-reject" @click="handleAudit(item, 'REJECT')">驳回</el-button>
                  <el-button type="text" size="mini" icon="el-icon-view" @click="handleDetail(item)">详情</el-button>
                </div>
              </article>

              <div v-if="bigList.length === 0" class="queue-empty">本页暂无大额申请</div>
            </section>
          </div>

          <div v-if="list.length > 0" class="pagination-bar">
            <el-pagination
              background
              :current-page="listQuery.page"
              :page-size="listQuery.size"
              :total="total"
              layout="total, prev, pager, next"
              @current-change="handleCurrentChange"
            />
          </div>
        </div>
      </el-tab-pane>

      <!-- 已审记录 -->
      <el-tab-pane label="已审记录" name="audited">
        <div class="filter-bar">
          <el-select v-model="auditedQuery.status" placeholder="状态筛选" clearable style="width:160px" @change="handleAuditedFilter">
            <el-option label="审核通过" value="AUDITED" />
            <el-option label="已驳回" value="REJECTED" />
            <el-option label="复核通过" value="REVIEWED" />
            <el-option label="已拨付" value="DISBURSED" />
          </el-select>
        </div>
        <el-table v-loading="auditedLoading" :data="auditedList" border stripe style="width:100%">
          <el-table-column type="index" label="序号" width="55" align="center" />
          <el-table-column prop="studentName" label="学生" min-width="90" />
          <el-table-column prop="sponsorName" label="资助人" min-width="90" />
          <el-table-column label="金额(元)" min-width="110" align="right">
            <template slot-scope="{ row }">
              <span class="amount">{{ formatAmount(row.amount) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100" align="center">
            <template slot-scope="{ row }">
              <el-tag :type="statusTagType(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="voucherNo" label="凭证号" min-width="170">
            <template slot-scope="{ row }">{{ row.voucherNo || '—' }}</template>
          </el-table-column>
          <el-table-column prop="auditTime" label="审核时间" min-width="150" />
          <el-table-column prop="auditRemark" label="审核意见" min-width="140" show-overflow-tooltip>
            <template slot-scope="{ row }">{{ row.auditRemark || '—' }}</template>
          </el-table-column>
          <el-table-column label="操作" width="90" align="center">
            <template slot-scope="{ row }">
              <el-button type="text" icon="el-icon-view" @click="handleDetail(row)">详情</el-button>
            </template>
          </el-table-column>
          <template slot="empty">
            <EmptyState type="ledger" title="暂无已审记录" desc="完成审核后，记录会在这里按时间归档" />
          </template>
        </el-table>
        <div class="pagination-bar">
          <el-pagination
            background
            :current-page="auditedQuery.page"
            :page-size="auditedQuery.size"
            :total="auditedTotal"
            layout="total, prev, pager, next"
            @current-change="handleAuditedCurrentChange"
          />
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 审核意见弹窗 -->
    <el-dialog :title="auditDialogTitle" :visible.sync="auditDialogVisible" width="480px" :close-on-click-modal="false">
      <el-alert
        v-if="auditTarget && isBig(auditTarget) && auditResult === 'PASS'"
        type="warning"
        :closable="false"
        show-icon
        title="该申请金额 ≥ 5000 元，审核通过后将自动进入复核抽检池"
        class="big-tip"
      />
      <el-form label-width="80px">
        <el-form-item label="学生">{{ auditTarget ? auditTarget.studentName : '' }}</el-form-item>
        <el-form-item label="金额">¥ {{ auditTarget ? formatAmount(auditTarget.amount) : '' }}</el-form-item>
        <el-form-item :label="auditResult === 'PASS' ? '审核意见' : '驳回原因'" required>
          <el-input
            v-model="auditRemark"
            type="textarea"
            :rows="3"
            :placeholder="auditResult === 'PASS' ? '请填写审核意见（选填）' : '请填写驳回原因（必填）'"
          />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="auditDialogVisible = false">取 消</el-button>
        <el-button :type="auditResult === 'PASS' ? 'primary' : 'danger'" :loading="auditLoading" @click="submitAudit">
          {{ auditResult === 'PASS' ? '确认通过' : '确认驳回' }}
        </el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listPendingAudit, countPendingAudit, auditFund } from '@/api/audit'
import { listFunds } from '@/api/fund'
import EmptyState from '@/components/EmptyState'

const BIG_THRESHOLD = 5000

export default {
  name: 'AuditWorkbench',
  components: { EmptyState },
  data() {
    return {
      activeTab: 'pending',
      pendingCount: 0,
      list: [],
      total: 0,
      listLoading: false,
      listQuery: { page: 1, size: 9 },
      // 已审记录：后端 /funds 仅支持单一 status 过滤，且分页 total 会含待审单据
      // 故一次性取回后在本地按非 SUBMITTED 过滤 + 分页，保证 total 与列表一致
      auditedAll: [],
      auditedLoading: false,
      auditedQuery: { page: 1, size: 10, status: undefined },
      auditDialogVisible: false,
      auditLoading: false,
      auditTarget: null,
      auditResult: 'PASS',
      auditRemark: ''
    }
  },
  computed: {
    bigCount() {
      return this.list.filter(item => this.isBig(item)).length
    },
    // 仅用于双列分流展示，不改变任何审核逻辑
    bigList() {
      return this.list.filter(item => this.isBig(item))
    },
    normalList() {
      return this.list.filter(item => !this.isBig(item))
    },
    bigRatio() {
      if (!this.list.length) return 0
      return Math.round((this.bigCount / this.list.length) * 100)
    },
    auditedTotal() {
      return this.auditedAll.length
    },
    auditedList() {
      const start = (this.auditedQuery.page - 1) * this.auditedQuery.size
      return this.auditedAll.slice(start, start + this.auditedQuery.size)
    },
    auditDialogTitle() {
      return this.auditResult === 'PASS' ? '审核通过' : '驳回申请'
    }
  },
  created() {
    this.getList()
    this.getCount()
    this.getAuditedList()
  },
  methods: {
    isBig(item) { return Number(item.amount) >= BIG_THRESHOLD },
    formatAmount(val) {
      if (val === undefined || val === null) return '0.00'
      return Number(val).toFixed(2)
    },
    statusText(status) {
      const map = { SUBMITTED: '待审核', AUDITED: '审核通过', REJECTED: '已驳回', REVIEWED: '复核通过', DISBURSED: '已拨付' }
      return map[status] || status || '-'
    },
    statusTagType(status) {
      const map = { SUBMITTED: 'info', AUDITED: 'primary', REJECTED: 'danger', REVIEWED: 'warning', DISBURSED: 'success' }
      return map[status] || 'info'
    },
    getList() {
      this.listLoading = true
      listPendingAudit(this.listQuery).then(data => {
        this.list = (data && data.records) || []
        this.total = (data && data.total) || 0
      }).finally(() => { this.listLoading = false })
    },
    getCount() {
      countPendingAudit().then(data => { this.pendingCount = data || 0 }).catch(() => {})
    },
    getAuditedList() {
      this.auditedLoading = true
      listFunds({ page: 1, size: 1000, status: this.auditedQuery.status || undefined }).then(data => {
        this.auditedAll = ((data && data.records) || []).filter(r => r.status !== 'SUBMITTED')
        // 过滤后总数变少时，回退到仍有效的页码
        const maxPage = Math.max(1, Math.ceil(this.auditedAll.length / this.auditedQuery.size))
        if (this.auditedQuery.page > maxPage) this.auditedQuery.page = maxPage
      }).finally(() => { this.auditedLoading = false })
    },
    handleCurrentChange(page) { this.listQuery.page = page; this.getList() },
    handleAuditedFilter() { this.auditedQuery.page = 1; this.getAuditedList() },
    // 已审记录已全量取回，翻页只需切换本地页码，无需再次请求
    handleAuditedCurrentChange(page) { this.auditedQuery.page = page },
    handleDetail(row) {
      this.$router.push('/fund/detail/' + row.id)
    },
    handleAudit(row, result) {
      this.auditTarget = row
      this.auditResult = result
      this.auditRemark = ''
      this.auditDialogVisible = true
    },
    submitAudit() {
      if (this.auditResult === 'REJECT' && !this.auditRemark.trim()) {
        this.$message.warning('请填写驳回原因')
        return
      }
      this.auditLoading = true
      auditFund(this.auditTarget.id, { result: this.auditResult, remark: this.auditRemark.trim() }).then(() => {
        this.$message.success(this.auditResult === 'PASS' ? '审核通过，资助凭证已生成' : '已驳回')
        this.auditDialogVisible = false
        this.getList()
        this.getCount()
        this.getAuditedList()
      }).finally(() => { this.auditLoading = false })
    }
  }
}
</script>

<style scoped>
/* ---------- 统计区：不对称栅格 ---------- */
.stat-row {
  display: grid;
  grid-template-columns: 1.62fr 1fr;
  gap: 16px;
  margin-bottom: 16px;
}

.stat-card {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 18px 20px;
  border-radius: var(--r-card);
  color: #fff;
  overflow: hidden;
  box-shadow: var(--sh-2);
  transition: transform var(--t-fast), box-shadow var(--t-fast);
}

.stat-card::after {
  content: '';
  position: absolute;
  top: -46px;
  right: -30px;
  width: 150px;
  height: 150px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.16), transparent 68%);
  pointer-events: none;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--sh-3);
}

.stat-card--main {
  background: linear-gradient(135deg, #1e4c7e 0%, #2b6cb0 55%, #4d8fcf 100%);
}

.stat-card--side {
  background: linear-gradient(135deg, #17614c 0%, #2f9e6e 55%, #52bb92 100%);
}

.stat-inner {
  display: flex;
  align-items: center;
  gap: 14px;
  position: relative;
  z-index: 1;
}

.stat-icon {
  flex: 0 0 46px;
  width: 46px;
  height: 46px;
  border-radius: 11px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 21px;
  color: #fff;
  background: rgba(255, 255, 255, 0.15);
  border: 1px solid rgba(255, 255, 255, 0.24);
}

.stat-info {
  min-width: 0;
}

.stat-label {
  font-size: 12.5px;
  color: rgba(255, 255, 255, 0.82);
  letter-spacing: 0.2px;
  margin-bottom: 2px;
}

.stat-value-row {
  display: flex;
  align-items: baseline;
  gap: 5px;
}

.stat-num {
  font-size: 30px;
  font-weight: 700;
  line-height: 1.1;
  letter-spacing: 0.5px;
  font-variant-numeric: tabular-nums;
  font-feature-settings: 'tnum';
}

.stat-unit {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.68);
}

.stat-hint {
  margin-top: 2px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
}

/* 主卡：占比进度 */
.stat-progress {
  position: relative;
  z-index: 1;
  padding-top: 12px;
  border-top: 1px solid rgba(255, 255, 255, 0.18);
}

.stat-progress-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.72);
  margin-bottom: 7px;
}

.stat-progress-val {
  font-weight: 600;
  color: #fff;
  font-variant-numeric: tabular-nums;
}

.stat-progress-track {
  height: 4px;
  border-radius: var(--r-pill);
  background: rgba(255, 255, 255, 0.22);
  overflow: hidden;
}

.stat-progress-fill {
  height: 100%;
  border-radius: var(--r-pill);
  background: #fff;
  transition: width 0.3s ease;
}

/* 副卡：图例 */
.stat-legend {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 18px;
  padding-top: 11px;
  border-top: 1px solid rgba(255, 255, 255, 0.18);
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 7px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.75);
}

.legend-item b {
  font-weight: 600;
  color: #fff;
  font-variant-numeric: tabular-nums;
}

.legend-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
}

.legend-dot--normal {
  background: rgba(255, 255, 255, 0.55);
}

.legend-dot--big {
  background: #ffd9a8;
}

/* ---------- 双列分流队列 ---------- */
.workbench-tabs {
  margin-top: 4px;
}

.queue-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  align-items: start;
}

.queue-col {
  background: var(--c-bg-card);
  border: 1px solid var(--c-border);
  border-radius: var(--r-card);
  padding: 14px 14px 4px;
}

.queue-head {
  display: flex;
  align-items: center;
  gap: 8px;
  padding-bottom: 10px;
  margin-bottom: 10px;
  border-bottom: 1px solid var(--c-border-light);
}

.queue-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--c-text-1);
}

.queue-count {
  min-width: 20px;
  height: 20px;
  padding: 0 6px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--r-pill);
  background: var(--c-primary-light);
  color: var(--c-primary);
  font-size: 12px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}

.queue-count--alert {
  background: var(--c-warning-light);
  color: var(--c-warning);
}

.queue-sub {
  margin-left: auto;
  font-size: 12px;
  color: var(--c-text-4);
}

.queue-card {
  position: relative;
  background: var(--c-bg-card);
  border: 1px solid var(--c-border-light);
  border-radius: var(--r-panel);
  padding: 12px 13px;
  margin-bottom: 10px;
  transition: border-color var(--t-fast), box-shadow var(--t-fast), transform var(--t-fast);
}

.queue-card:hover {
  border-color: var(--c-primary-border);
  box-shadow: var(--sh-2);
  transform: translateY(-1px);
}

.queue-card--alert::before {
  content: '';
  position: absolute;
  left: 0;
  top: 10px;
  bottom: 10px;
  width: 3px;
  border-radius: 0 3px 3px 0;
  background: var(--c-warning);
}

.amount-badge {
  position: absolute;
  top: 10px;
  right: 12px;
  padding: 2px 10px;
  border-radius: var(--r-pill);
  background: var(--c-warning-light);
  border: 1px solid #f2ddbe;
  color: var(--c-warning);
  font-size: 13px;
  font-weight: 700;
  line-height: 1.5;
  font-variant-numeric: tabular-nums;
}

.qc-head {
  display: flex;
  align-items: center;
  gap: 6px;
  padding-right: 104px;
}

.qc-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--c-text-1);
}

.qc-amount {
  margin: 5px 0 7px;
  font-size: 17px;
  font-weight: 700;
  color: var(--c-text-1);
  font-variant-numeric: tabular-nums;
}

.qc-meta {
  display: flex;
  align-items: baseline;
  gap: 8px;
  font-size: 12.5px;
  line-height: 1.95;
}

.qc-label {
  flex: 0 0 58px;
  color: var(--c-text-4);
}

.qc-value {
  min-width: 0;
  color: var(--c-text-2);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.qc-meta--muted .qc-value {
  color: var(--c-text-3);
  font-size: 12px;
}

.qc-foot {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid var(--c-border-light);
}

.btn-reject:hover {
  color: var(--c-danger);
  border-color: #e6b4b1;
  background: var(--c-danger-light);
}

.queue-empty {
  padding: 26px 0;
  margin-bottom: 10px;
  text-align: center;
  font-size: 12.5px;
  color: var(--c-text-4);
  background: var(--c-bg-page);
  border-radius: var(--r-table);
}

.pagination-bar {
  margin-top: 12px;
  text-align: right;
}

.filter-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.big-tip {
  margin-bottom: 12px;
}

@media (max-width: 1024px) {
  .stat-row {
    grid-template-columns: 1fr;
  }

  .queue-grid {
    grid-template-columns: 1fr;
  }
}
</style>
