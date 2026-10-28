<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <div class="page-title">拨付工作台</div>
        <div class="page-subtitle">仅显示已通过审核的单据，确认拨付后自动写入存证台账</div>
      </div>
      <div class="page-actions">
        <span class="lock-pill"><i class="el-icon-lock"></i> 申请信息已锁定</span>
      </div>
    </div>

    <div class="note-bar">
      <i class="el-icon-info"></i>
      <span>申请资料已置灰锁定：拨付管理员仅可查看已审核通过的单据并执行发放，不可修改申请信息</span>
    </div>

    <div class="kpi-row">
      <div class="kpi-card kpi-card--warn">
        <div class="kpi-label">待拨付单据</div>
        <div class="kpi-value">{{ total }}</div>
        <div class="kpi-hint">笔待执行发放</div>
      </div>
      <div class="kpi-card">
        <div class="kpi-label">本页金额合计（元）</div>
        <div class="kpi-value kpi-value--amount">{{ formatAmount(pageAmount) }}</div>
        <div class="kpi-hint">当前页 {{ list.length }} 笔合计</div>
      </div>
      <div class="kpi-card kpi-card--success">
        <div class="kpi-label">本页含凭证</div>
        <div class="kpi-value">{{ voucherCount }}</div>
        <div class="kpi-hint">笔已生成凭证号</div>
      </div>
    </div>

    <el-table v-loading="listLoading" :data="list" border stripe style="width:100%">
      <el-table-column type="index" label="序号" width="55" align="center" />
      <el-table-column label="凭证号" min-width="175">
        <template slot-scope="{ row }">
          <span v-if="row.voucherNo" class="voucher-text"><i class="el-icon-lock"></i> {{ row.voucherNo }}</span>
          <span v-else class="empty-text">—</span>
        </template>
      </el-table-column>
      <el-table-column prop="studentName" label="学生" min-width="90" />
      <el-table-column prop="sponsorName" label="资助人" min-width="90" />
      <el-table-column label="金额(元)" min-width="110" align="right">
        <template slot-scope="{ row }">
          <span class="amount-text">{{ formatAmount(row.amount) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100" align="center">
        <template slot-scope="{ row }">
          <el-tag :type="statusTagType(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="存证哈希" min-width="130">
        <template slot-scope="{ row }">
          <el-tooltip v-if="row.voucherHash" :content="row.voucherHash" placement="top">
            <span class="hash-text">{{ shortHash(row.voucherHash) }}</span>
          </el-tooltip>
          <span v-else class="empty-text">—</span>
        </template>
      </el-table-column>
      <el-table-column label="用途" min-width="120" align="center">
        <template>
          <span class="locked-text"><i class="el-icon-lock"></i> 已锁定</span>
        </template>
      </el-table-column>
      <el-table-column label="备注" min-width="120" align="center">
        <template>
          <span class="locked-text"><i class="el-icon-lock"></i> 已锁定</span>
        </template>
      </el-table-column>
      <el-table-column prop="auditTime" label="审核时间" min-width="150" />
      <el-table-column label="操作" width="130" align="center" fixed="right">
        <template slot-scope="{ row }">
          <el-button type="success" size="mini" icon="el-icon-bank-card" @click="handleDisburse(row)">确认拨付</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-bar">
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
</template>

<script>
import { listPendingDisburse, disburseFund } from '@/api/disburse'

export default {
  name: 'DisburseWorkbench',
  data() {
    return {
      list: [],
      total: 0,
      listLoading: false,
      listQuery: { page: 1, size: 10 }
    }
  },
  created() {
    this.getList()
  },
  computed: {
    // 仅用于顶部统计展示，不改变任何拨付逻辑
    pageAmount() {
      return this.list.reduce((sum, item) => sum + Number(item.amount || 0), 0)
    },
    voucherCount() {
      return this.list.filter(item => !!item.voucherNo).length
    }
  },
  methods: {
    formatAmount(val) {
      if (val === undefined || val === null) return '0.00'
      return Number(val).toFixed(2)
    },
    shortHash(hash) {
      return hash ? hash.substring(0, 12) + '…' : '—'
    },
    statusText(status) {
      const map = { AUDITED: '审核通过', REVIEWED: '复核通过' }
      return map[status] || status || '-'
    },
    statusTagType(status) {
      const map = { AUDITED: 'primary', REVIEWED: 'warning' }
      return map[status] || 'info'
    },
    getList() {
      this.listLoading = true
      listPendingDisburse(this.listQuery).then(data => {
        this.list = (data && data.records) || []
        this.total = (data && data.total) || 0
      }).finally(() => { this.listLoading = false })
    },
    handleCurrentChange(page) { this.listQuery.page = page; this.getList() },
    handleDisburse(row) {
      this.$confirm(
        `确认向学生「${row.studentName}」拨付 ¥${this.formatAmount(row.amount)} 吗？凭证号：${row.voucherNo || '—'}`,
        '确认拨付',
        { confirmButtonText: '确认拨付', cancelButtonText: '取消', type: 'warning' }
      ).then(() => {
        disburseFund(row.id).then(() => {
          this.$message.success('拨付成功')
          this.getList()
        })
      }).catch(() => {})
    }
  }
}
</script>

<style scoped>
.lock-pill {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  height: 26px;
  padding: 0 12px;
  border-radius: var(--r-pill);
  background: var(--c-warning-light);
  border: 1px solid #f2ddbe;
  color: var(--c-warning);
  font-size: 12px;
  font-weight: 600;
}
.kpi-value--amount { font-size:20px; letter-spacing:0.3px; }
.amount-text { font-weight:600; color:var(--c-text-1); font-variant-numeric:tabular-nums; }
.voucher-text { font-size:12px; color:var(--c-text-2); }
.hash-text { font-family:Consolas, monospace; font-size:12px; color:var(--c-text-3); cursor:default; }
.locked-text { color:var(--c-text-4); font-style:italic; font-size:12px; }
.empty-text { color:var(--c-text-4); }
.pagination-bar { margin-top:16px; text-align:right; }
</style>
