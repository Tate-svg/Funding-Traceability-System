<template>
  <div class="page-container">
    <!-- 顶部统计卡片区 -->
    <div class="stat-row">
      <el-card shadow="hover" class="stat-card">
        <div class="stat-inner">
          <div class="stat-icon pending"><i class="el-icon-s-check"></i></div>
          <div class="stat-info">
            <div class="stat-label">待审核申请</div>
            <el-badge :value="pendingCount" :max="99" type="danger" class="stat-badge">
              <span class="stat-num">{{ pendingCount }}</span>
            </el-badge>
          </div>
        </div>
      </el-card>
      <el-card shadow="hover" class="stat-card">
        <div class="stat-inner">
          <div class="stat-icon big"><i class="el-icon-warning-outline"></i></div>
          <div class="stat-info">
            <div class="stat-label">本页大额待审（≥5000元）</div>
            <span class="stat-num">{{ bigCount }}</span>
          </div>
        </div>
      </el-card>
    </div>

    <el-tabs v-model="activeTab" class="workbench-tabs">
      <!-- 待审核卡片列表 -->
      <el-tab-pane label="待审核申请" name="pending">
        <div v-loading="listLoading">
          <el-empty v-if="!listLoading && list.length === 0" description="暂无待审核申请" />
          <el-row :gutter="16">
            <el-col v-for="item in list" :key="item.id" :xs="24" :sm="12" :md="8">
              <el-card shadow="hover" class="apply-card">
                <div slot="header" class="card-header">
                  <span class="student-name">{{ item.studentName }}</span>
                  <el-tag v-if="isBig(item)" type="danger" size="mini">大额</el-tag>
                </div>
                <div class="card-body">
                  <div class="amount-line" :class="{ 'big-amount': isBig(item) }">¥ {{ formatAmount(item.amount) }}</div>
                  <div class="info-line"><span class="label">资助人：</span>{{ item.sponsorName }}</div>
                  <div class="info-line"><span class="label">资助日期：</span>{{ item.fundDate }}</div>
                  <div class="info-line"><span class="label">用途：</span>{{ item.purpose || '—' }}</div>
                  <div class="info-line"><span class="label">备注：</span>{{ item.remark || '—' }}</div>
                  <div class="info-line muted"><span class="label">提交时间：</span>{{ item.createTime }}</div>
                </div>
                <div class="card-footer">
                  <el-button type="success" size="mini" icon="el-icon-check" @click="handleAudit(item, 'PASS')">通过</el-button>
                  <el-button type="danger" size="mini" plain icon="el-icon-close" @click="handleAudit(item, 'REJECT')">驳回</el-button>
                  <el-button type="text" size="mini" icon="el-icon-view" @click="handleDetail(item)">详情</el-button>
                </div>
              </el-card>
            </el-col>
          </el-row>
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
            <template slot-scope="{ row }">{{ formatAmount(row.amount) }}</template>
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
      <el-alert v-if="auditTarget && isBig(auditTarget) && auditResult === 'PASS'" type="warning" :closable="false" show-icon
        title="该申请金额 ≥ 5000 元，审核通过后将自动进入复核抽检池" class="big-tip" />
      <el-form label-width="80px">
        <el-form-item label="学生">{{ auditTarget ? auditTarget.studentName : '' }}</el-form-item>
        <el-form-item label="金额">¥ {{ auditTarget ? formatAmount(auditTarget.amount) : '' }}</el-form-item>
        <el-form-item :label="auditResult === 'PASS' ? '审核意见' : '驳回原因'" required>
          <el-input v-model="auditRemark" type="textarea" :rows="3" :placeholder="auditResult === 'PASS' ? '请填写审核意见（选填）' : '请填写驳回原因（必填）'" />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="auditDialogVisible = false">取 消</el-button>
        <el-button :type="auditResult === 'PASS' ? 'success' : 'danger'" :loading="auditLoading" @click="submitAudit">
          {{ auditResult === 'PASS' ? '确认通过' : '确认驳回' }}
        </el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listPendingAudit, countPendingAudit, auditFund } from '@/api/audit'
import { listFunds } from '@/api/fund'

const BIG_THRESHOLD = 5000

export default {
  name: 'AuditWorkbench',
  data() {
    return {
      activeTab: 'pending',
      pendingCount: 0,
      list: [],
      total: 0,
      listLoading: false,
      listQuery: { page: 1, size: 9 },
      auditedList: [],
      auditedTotal: 0,
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
      listFunds(this.auditedQuery).then(data => {
        this.auditedList = ((data && data.records) || []).filter(r => r.status !== 'SUBMITTED')
        this.auditedTotal = (data && data.total) || 0
      }).finally(() => { this.auditedLoading = false })
    },
    handleCurrentChange(page) { this.listQuery.page = page; this.getList() },
    handleAuditedFilter() { this.auditedQuery.page = 1; this.getAuditedList() },
    handleAuditedCurrentChange(page) { this.auditedQuery.page = page; this.getAuditedList() },
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
.page-container { background:#fff; padding:16px; border-radius:4px; }
.stat-row { display:flex; gap:16px; margin-bottom:16px; flex-wrap:wrap; }
.stat-card { flex:1; min-width:240px; }
.stat-inner { display:flex; align-items:center; gap:14px; }
.stat-icon { width:48px; height:48px; border-radius:8px; display:flex; align-items:center; justify-content:center; font-size:24px; color:#fff; }
.stat-icon.pending { background:#F56C6C; }
.stat-icon.big { background:#E6A23C; }
.stat-label { font-size:13px; color:#909399; margin-bottom:4px; }
.stat-num { font-size:26px; font-weight:700; color:#303133; line-height:1.2; }
.workbench-tabs { margin-top:4px; }
.apply-card { margin-bottom:16px; }
.card-header { display:flex; align-items:center; justify-content:space-between; }
.student-name { font-weight:600; font-size:15px; }
.card-body { padding:4px 0; }
.amount-line { font-size:20px; font-weight:700; color:#303133; margin-bottom:8px; }
.amount-line.big-amount { color:#F56C6C; }
.info-line { font-size:13px; color:#606266; line-height:1.9; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; }
.info-line .label { color:#909399; }
.info-line.muted { color:#909399; font-size:12px; }
.card-footer { border-top:1px solid #EBEEF5; padding-top:10px; margin-top:10px; display:flex; align-items:center; }
.pagination-bar { margin-top:8px; text-align:right; }
.filter-bar { display:flex; align-items:center; gap:8px; margin-bottom:12px; }
.big-tip { margin-bottom:12px; }
</style>
