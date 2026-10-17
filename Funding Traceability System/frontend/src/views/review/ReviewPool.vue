<template>
  <div class="page-container">
    <!-- 抽检设置卡片 -->
    <el-card shadow="never" class="draw-card">
      <div slot="header" class="draw-header">
        <span><i class="el-icon-aim"></i> 大额资助随机抽检</span>
        <span class="draw-tip">审核通过且金额 ≥ 5000 元的资助单自动进入复核候选池</span>
      </div>
      <el-form inline>
        <el-form-item label="金额区间">
          <el-input-number v-model="drawForm.minAmount" :min="5000" :precision="2" :step="1000" controls-position="right" style="width:150px" placeholder="最低" />
          <span class="range-sep">—</span>
          <el-input-number v-model="drawForm.maxAmount" :min="5000" :precision="2" :step="1000" controls-position="right" style="width:150px" placeholder="最高" />
        </el-form-item>
        <el-form-item label="抽检比例">
          <el-input-number v-model="drawForm.ratio" :min="1" :max="100" :precision="0" controls-position="right" style="width:110px" />
          <span class="range-sep">%</span>
        </el-form-item>
        <el-form-item>
          <el-button type="danger" icon="el-icon-aim" :loading="drawLoading" @click="handleDraw">开始抽取</el-button>
        </el-form-item>
      </el-form>
      <el-alert v-if="drawResult" type="success" show-icon :closable="false" class="draw-result">
        <template slot="title">
          批次号 <b>{{ drawResult.batchNo }}</b>：候选 {{ drawResult.total }} 单，抽中 {{ drawResult.drawn }} 单，未抽中单据已按抽检程序放行
          <el-button type="text" class="view-batch" @click="filterBatch(drawResult.batchNo)">查看本批任务</el-button>
        </template>
      </el-alert>
    </el-card>

    <!-- 复核任务表 -->
    <div class="filter-bar">
      <el-input v-model="listQuery.batchNo" placeholder="批次号" clearable style="width:200px" @keyup.enter.native="handleFilter" />
      <el-select v-model="listQuery.status" placeholder="任务状态" clearable style="width:140px" @change="handleFilter">
        <el-option label="待复核" value="PENDING" />
        <el-option label="复核通过" value="PASS" />
        <el-option label="复核不通过" value="FAIL" />
      </el-select>
      <el-button type="primary" icon="el-icon-search" @click="handleFilter">查询</el-button>
      <el-button icon="el-icon-refresh" @click="handleReset">重置</el-button>
    </div>

    <el-table v-loading="listLoading" :data="list" border stripe style="width:100%">
      <el-table-column type="index" label="序号" width="55" align="center" />
      <el-table-column prop="batchNo" label="批次号" min-width="150" />
      <el-table-column prop="studentName" label="学生" min-width="90" />
      <el-table-column prop="sponsorName" label="资助人" min-width="90" />
      <el-table-column label="金额(元)" min-width="110" align="right">
        <template slot-scope="{ row }">
          <span class="amount-text">{{ formatAmount(row.amount) }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="voucherNo" label="凭证号" min-width="165">
        <template slot-scope="{ row }">{{ row.voucherNo || '—' }}</template>
      </el-table-column>
      <el-table-column label="抽检比例" width="90" align="center">
        <template slot-scope="{ row }">{{ row.sampleRatio }}%</template>
      </el-table-column>
      <el-table-column label="状态" width="100" align="center">
        <template slot-scope="{ row }">
          <el-tag :type="taskTagType(row.status)" size="small">{{ taskText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="抽取时间" min-width="150" />
      <el-table-column label="操作" width="100" align="center" fixed="right">
        <template slot-scope="{ row }">
          <el-button v-if="row.status === 'PENDING'" type="primary" size="mini" icon="el-icon-edit-outline" @click="handleReview(row)">复核</el-button>
          <el-tooltip v-else :content="row.reviewRemark || '无备注'" placement="top">
            <span class="done-text"><i class="el-icon-circle-check"></i> 已完成</span>
          </el-tooltip>
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

    <!-- 复核弹窗 -->
    <el-dialog title="复核确认" :visible.sync="reviewDialogVisible" width="480px" :close-on-click-modal="false">
      <el-descriptions v-if="reviewTarget" :column="1" border size="small" class="review-desc">
        <el-descriptions-item label="学生">{{ reviewTarget.studentName }}</el-descriptions-item>
        <el-descriptions-item label="金额">¥ {{ formatAmount(reviewTarget.amount) }}</el-descriptions-item>
        <el-descriptions-item label="凭证号">{{ reviewTarget.voucherNo || '—' }}</el-descriptions-item>
        <el-descriptions-item label="批次号">{{ reviewTarget.batchNo }}</el-descriptions-item>
      </el-descriptions>
      <el-form label-width="80px" class="review-form">
        <el-form-item label="复核结果" required>
          <el-radio-group v-model="reviewResult">
            <el-radio-button label="PASS">通过</el-radio-button>
            <el-radio-button label="FAIL">不通过</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="复核备注">
          <el-input v-model="reviewRemark" type="textarea" :rows="3" placeholder="不通过时建议说明原因，单据将打回待审核" />
        </el-form-item>
      </el-form>
      <el-alert v-if="reviewResult === 'FAIL'" type="warning" :closable="false" show-icon title="复核不通过后，该资助单将被打回「待审核」状态，需重新审核" />
      <div slot="footer">
        <el-button @click="reviewDialogVisible = false">取 消</el-button>
        <el-button :type="reviewResult === 'PASS' ? 'success' : 'danger'" :loading="reviewLoading" @click="submitReview">确认提交</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { drawReviewTasks, listReviewTasks, finishReviewTask } from '@/api/review'

export default {
  name: 'ReviewPool',
  data() {
    return {
      drawForm: { minAmount: 5000, maxAmount: undefined, ratio: 50 },
      drawLoading: false,
      drawResult: null,
      list: [],
      total: 0,
      listLoading: false,
      listQuery: { page: 1, size: 10, batchNo: '', status: undefined },
      reviewDialogVisible: false,
      reviewLoading: false,
      reviewTarget: null,
      reviewResult: 'PASS',
      reviewRemark: ''
    }
  },
  created() {
    this.getList()
  },
  methods: {
    formatAmount(val) {
      if (val === undefined || val === null) return '0.00'
      return Number(val).toFixed(2)
    },
    taskText(status) {
      const map = { PENDING: '待复核', PASS: '复核通过', FAIL: '复核不通过' }
      return map[status] || status || '-'
    },
    taskTagType(status) {
      const map = { PENDING: 'info', PASS: 'success', FAIL: 'danger' }
      return map[status] || 'info'
    },
    getList() {
      this.listLoading = true
      listReviewTasks(this.listQuery).then(data => {
        this.list = (data && data.records) || []
        this.total = (data && data.total) || 0
      }).finally(() => { this.listLoading = false })
    },
    handleFilter() { this.listQuery.page = 1; this.getList() },
    handleReset() { this.listQuery.batchNo = ''; this.listQuery.status = undefined; this.listQuery.page = 1; this.getList() },
    handleCurrentChange(page) { this.listQuery.page = page; this.getList() },
    filterBatch(batchNo) {
      this.listQuery.batchNo = batchNo
      this.listQuery.status = undefined
      this.listQuery.page = 1
      this.getList()
    },
    handleDraw() {
      this.drawLoading = true
      drawReviewTasks({
        minAmount: this.drawForm.minAmount,
        maxAmount: this.drawForm.maxAmount,
        ratio: this.drawForm.ratio
      }).then(data => {
        this.drawResult = data
        this.getList()
      }).finally(() => { this.drawLoading = false })
    },
    handleReview(row) {
      this.reviewTarget = row
      this.reviewResult = 'PASS'
      this.reviewRemark = ''
      this.reviewDialogVisible = true
    },
    submitReview() {
      this.reviewLoading = true
      finishReviewTask(this.reviewTarget.id, { result: this.reviewResult, remark: this.reviewRemark.trim() }).then(() => {
        this.$message.success(this.reviewResult === 'PASS' ? '复核通过，单据可拨付' : '已打回待审核')
        this.reviewDialogVisible = false
        this.getList()
      }).finally(() => { this.reviewLoading = false })
    }
  }
}
</script>

<style scoped>
.page-container { background:#fff; padding:16px; border-radius:4px; }
.draw-card { margin-bottom:16px; }
.draw-header { display:flex; align-items:center; justify-content:space-between; font-weight:600; }
.draw-tip { font-weight:400; font-size:12px; color:#909399; }
.range-sep { margin:0 6px; color:#909399; }
.draw-result { margin-top:4px; }
.view-batch { padding:0 0 0 8px; }
.filter-bar { display:flex; align-items:center; gap:8px; margin-bottom:16px; flex-wrap:wrap; }
.amount-text { font-weight:600; color:#F56C6C; }
.done-text { color:#909399; font-size:12px; cursor:default; }
.pagination-bar { margin-top:16px; text-align:right; }
.review-desc { margin-bottom:16px; }
.review-form { margin-bottom:8px; }
</style>
