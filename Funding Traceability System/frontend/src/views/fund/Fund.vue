<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <div class="page-title">资金管理</div>
        <div class="page-subtitle">资助申请全生命周期：提交 → 审核 → 复核 → 拨付，关键节点自动存证</div>
      </div>
      <div class="page-actions">
        <el-button v-permission="['AUDIT_ADMIN']" type="warning" icon="el-icon-download" :loading="exportLoading" @click="handleExport">导出</el-button>
        <el-button v-permission="['AUDIT_ADMIN']" type="primary" icon="el-icon-plus" @click="handleCreate">新增资金</el-button>
      </div>
    </div>

    <PrivacyTipBar />

    <div class="kpi-row">
      <div class="kpi-card">
        <div class="kpi-label">资金记录总数</div>
        <div class="kpi-value">{{ total }}</div>
        <div class="kpi-hint">条资助流水</div>
      </div>
      <div class="kpi-card">
        <div class="kpi-label">本页金额合计（元）</div>
        <div class="kpi-value kpi-value--amount">{{ formatAmount(pageAmount) }}</div>
        <div class="kpi-hint">当前页 {{ list.length }} 笔合计</div>
      </div>
      <div class="kpi-card kpi-card--warn">
        <div class="kpi-label">本页待审核</div>
        <div class="kpi-value">{{ pendingInPage }}</div>
        <div class="kpi-hint">条尚未审核的申请</div>
      </div>
    </div>

    <div class="toolbar">
      <el-select v-model="listQuery.studentId" placeholder="选择学生筛选" clearable filterable style="width:180px" @change="handleFilter">
        <el-option v-for="item in studentOptions" :key="item.id" :label="item.name" :value="item.id" />
      </el-select>
      <el-select v-model="listQuery.status" placeholder="状态筛选" clearable style="width:150px" @change="handleFilter">
        <el-option label="待审核" value="SUBMITTED" />
        <el-option label="审核通过" value="AUDITED" />
        <el-option label="已驳回" value="REJECTED" />
        <el-option label="复核通过" value="REVIEWED" />
        <el-option label="已拨付" value="DISBURSED" />
      </el-select>
      <el-input v-model="listQuery.keyword" placeholder="用途 / 备注" clearable style="width:220px" @keyup.enter.native="handleFilter" />
      <el-button type="primary" icon="el-icon-search" @click="handleFilter">查询</el-button>
      <el-button icon="el-icon-refresh" @click="handleReset">重置</el-button>
    </div>

    <el-table v-loading="listLoading" :data="list" border stripe style="width:100%">
      <el-table-column type="index" label="序号" width="55" align="center" />
      <el-table-column prop="studentName" label="学生" min-width="90" />
      <el-table-column prop="sponsorName" label="资助人" min-width="90" />
      <el-table-column prop="amount" label="金额(元)" min-width="120" align="right">
        <template slot-scope="{ row }">
          <span class="amount amount-cell">{{ formatAmount(row.amount) }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="fundDate" label="资助日期" min-width="110" />
      <el-table-column label="状态" width="95" align="center">
        <template slot-scope="{ row }">
          <el-tag :type="statusTagType(row.status)" :class="statusTagClass(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="凭证号" min-width="165">
        <template slot-scope="{ row }">
          <span v-if="row.voucherNo" class="voucher-text"><i class="el-icon-lock"></i> {{ row.voucherNo }}</span>
          <span v-else class="empty-text">—</span>
        </template>
      </el-table-column>
      <el-table-column prop="purpose" label="用途" min-width="95" show-overflow-tooltip>
        <template slot-scope="{ row }">
          <span v-if="row.purpose">{{ row.purpose }}</span>
          <span v-else class="locked-text">已锁定</span>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="160" />
      <el-table-column label="操作" width="175" align="center" fixed="right">
        <template slot-scope="{ row }">
          <el-button type="text" icon="el-icon-view" @click="handleDetail(row)">详情</el-button>
          <template v-if="isAuditAdmin">
            <el-button v-if="row.status === 'SUBMITTED' || row.status === 'REJECTED'" type="text" icon="el-icon-edit" @click="handleUpdate(row)">编辑</el-button>
            <el-button v-if="row.status === 'SUBMITTED'" type="text" class="danger-text" icon="el-icon-delete" @click="handleDelete(row)">删除</el-button>
          </template>
        </template>
      </el-table-column>
      <template slot="empty">
        <EmptyState type="ledger" title="暂无资金记录" desc="调整筛选条件，或新增一笔资金记录" />
      </template>
    </el-table>

    <div class="pagination-bar">
      <el-pagination
        background
        :current-page="listQuery.page"
        :page-size="listQuery.size"
        :page-sizes="[10, 20, 50]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>

    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="560px" :close-on-click-modal="false">
      <el-form ref="dataForm" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="学生" prop="studentId">
          <el-select v-model="form.studentId" placeholder="请选择学生" filterable style="width:100%" @change="onStudentChange">
            <el-option v-for="item in studentOptions" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="资助人" prop="sponsorId">
          <el-select v-model="form.sponsorId" placeholder="请选择资助人" filterable style="width:100%" @change="onSponsorChange">
            <el-option v-for="item in sponsorOptions" :key="item.id" :label="item.realName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="金额(元)" prop="amount">
          <el-input-number v-model="form.amount" :min="0" :precision="2" :step="100" controls-position="right" style="width:100%" />
        </el-form-item>
        <el-form-item label="资助日期" prop="fundDate">
          <el-date-picker v-model="form.fundDate" type="date" value-format="yyyy-MM-dd" placeholder="选择日期" style="width:100%" />
        </el-form-item>
        <el-form-item label="用途" prop="purpose">
          <div class="purpose-quick">
            <el-tooltip v-for="item in purposeCategories" :key="item.name" :content="item.desc" placement="top">
              <span class="purpose-chip" :class="{ 'is-active': form.purpose === item.name }" @click="pickPurpose(item)">{{ item.name }}</span>
            </el-tooltip>
          </div>
          <SensitiveTextarea v-model="form.purpose" field-name="用途" :rows="2" placeholder="请输入用途，请勿填写未打码的敏感信息" @block="purposeBlock = $event" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <SensitiveTextarea v-model="form.remark" field-name="备注" :rows="3" placeholder="备注，请勿填写未打码的敏感信息" @block="remarkBlock = $event" />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" :loading="submitLoading" :disabled="purposeBlock || remarkBlock" @click="submitForm">确 定</el-button>
      </div>
    </el-dialog>

    <ComplianceFooter />
  </div>
</template>

<script>
import { listFunds, getFund, addFund, updateFund, deleteFund, exportFunds } from '@/api/fund'
import { studentOptions } from '@/api/student'
import { userOptions } from '@/api/user'
import { downloadFile } from '@/utils/download'
import PrivacyTipBar from '@/components/PrivacyTipBar.vue'
import ComplianceFooter from '@/components/ComplianceFooter.vue'
import SensitiveTextarea from '@/components/SensitiveTextarea.vue'
import EmptyState from '@/components/EmptyState.vue'

export default {
  name: 'Fund',
  components: { PrivacyTipBar, ComplianceFooter, SensitiveTextarea, EmptyState },
  data() {
    return {
      list: [],
      total: 0,
      listLoading: false,
      listQuery: { page: 1, size: 10, studentId: undefined, status: undefined, keyword: '' },
      dialogVisible: false,
      submitLoading: false,
      exportLoading: false,
      dialogType: 'create',
      studentOptions: [],
      sponsorOptions: [],
      purposeBlock: false,
      remarkBlock: false,
      // 标准用途分类：与仪表盘「资金用途分布」的统计口径保持一致，点击可快速填入用途
      purposeCategories: [
        { name: '学费', desc: '学年学费（统一归类，不再拆分春秋学期）' },
        { name: '住宿费', desc: '在校宿舍住宿费用' },
        { name: '生活费', desc: '餐饮、日常基础生活开支' },
        { name: '学习资料费', desc: '教材、书本、习题、线上学习资源' },
        { name: '实验耗材费', desc: '实验课程材料、实训配件、专业工具' },
        { name: '医保体检费', desc: '大学生医保、入学/年度体检' },
        { name: '其他专项资助', desc: '临时困难补助、综合助学补贴、交通补助等' }
      ],
      form: this.getEmptyForm(),
      rules: {
        studentId: [{ required: true, message: '请选择学生', trigger: 'change' }],
        sponsorId: [{ required: true, message: '请选择资助人', trigger: 'change' }],
        amount: [{ required: true, message: '请输入金额', trigger: 'blur' }],
        fundDate: [{ required: true, message: '请选择日期', trigger: 'change' }],
        purpose: [{ required: true, message: '请输入用途', trigger: 'blur' }]
      }
    }
  },
  computed: {
    isAuditAdmin() { return this.$store.getters.role === 'AUDIT_ADMIN' },
    dialogTitle() { return this.dialogType === 'create' ? '新增资金' : '编辑资金' },
    // 仅用于顶部统计展示，不改变任何列表/筛选逻辑
    pageAmount() {
      return this.list.reduce((sum, item) => sum + Number(item.amount || 0), 0)
    },
    pendingInPage() {
      return this.list.filter(item => item.status === 'SUBMITTED').length
    }
  },
  created() {
    this.getList()
    this.loadOptions()
  },
  methods: {
    getEmptyForm() {
      return { id: null, studentId: undefined, studentName: '', sponsorId: undefined, sponsorName: '', amount: 0, fundDate: '', purpose: '', remark: '' }
    },
    getList() {
      this.listLoading = true
      listFunds(this.listQuery).then(data => {
        this.list = (data && data.records) || []
        this.total = (data && data.total) || 0
      }).finally(() => { this.listLoading = false })
    },
    loadOptions() {
      studentOptions().then(data => { this.studentOptions = data || [] }).catch(() => {})
      userOptions('SPONSOR').then(data => { this.sponsorOptions = data || [] }).catch(() => {})
    },
    handleFilter() { this.listQuery.page = 1; this.getList() },
    handleReset() { this.listQuery.studentId = undefined; this.listQuery.status = undefined; this.listQuery.keyword = ''; this.listQuery.page = 1; this.getList() },
    statusText(status) {
      const map = { SUBMITTED: '待审核', AUDITED: '审核通过', REJECTED: '已驳回', REVIEWED: '复核通过', DISBURSED: '已拨付' }
      return map[status] || status || '-'
    },
    statusTagType(status) {
      const map = { SUBMITTED: 'info', AUDITED: 'primary', REJECTED: 'danger', REVIEWED: 'warning', DISBURSED: 'success' }
      return map[status] || 'info'
    },
    statusTagClass(status) {
      // 仅视觉区分：审核通过=蓝色描边，已拨付=绿色填充，其余沿用语义色
      if (status === 'AUDITED') return 'tag-outline'
      if (status === 'DISBURSED') return 'tag-solid'
      return ''
    },
    handleDetail(row) {
      this.$router.push('/fund/detail/' + row.id)
    },
    handleSizeChange(size) { this.listQuery.size = size; this.listQuery.page = 1; this.getList() },
    handleCurrentChange(page) { this.listQuery.page = page; this.getList() },
    onStudentChange(val) {
      const t = this.studentOptions.find(s => s.id === val)
      this.form.studentName = t ? t.name : ''
    },
    onSponsorChange(val) {
      const t = this.sponsorOptions.find(s => s.id === val)
      this.form.sponsorName = t ? t.realName : ''
    },
    handleCreate() {
      this.dialogType = 'create'
      this.form = this.getEmptyForm()
      this.purposeBlock = false
      this.remarkBlock = false
      this.dialogVisible = true
      this.$nextTick(() => { this.$refs.dataForm && this.$refs.dataForm.clearValidate() })
    },
    handleUpdate(row) {
      this.dialogType = 'update'
      getFund(row.id).then(data => {
        this.form = Object.assign(this.getEmptyForm(), data || {})
        this.purposeBlock = false
        this.remarkBlock = false
        this.dialogVisible = true
        this.$nextTick(() => { this.$refs.dataForm && this.$refs.dataForm.clearValidate() })
      })
    },
    submitForm() {
      this.$refs.dataForm.validate(valid => {
        if (!valid) return
        this.submitLoading = true
        const payload = { studentId: this.form.studentId, sponsorId: this.form.sponsorId, amount: this.form.amount, fundDate: this.form.fundDate, purpose: this.form.purpose, remark: this.form.remark }
        const action = this.dialogType === 'create' ? addFund(payload) : updateFund(this.form.id, payload)
        action.then(() => {
          this.$message.success(this.dialogType === 'create' ? '新增成功' : '修改成功')
          this.dialogVisible = false
          this.getList()
        }).finally(() => { this.submitLoading = false })
      })
    },
    handleDelete(row) {
      this.$confirm(`确认删除该资金记录吗？`, '提示', { type: 'warning' }).then(() => {
        deleteFund(row.id).then(() => {
          this.$message.success('删除成功')
          if (this.list.length === 1 && this.listQuery.page > 1) this.listQuery.page--
          this.getList()
        })
      }).catch(() => {})
    },
    handleExport() {
      this.exportLoading = true
      exportFunds().then(blob => downloadFile(blob, '资金记录.xlsx')).then(() => {
        this.$message.success('导出成功')
      }).catch(err => {
        if (err && err.message) this.$message.error(err.message)
      }).finally(() => { this.exportLoading = false })
    },
    formatAmount(val) {
      if (val === undefined || val === null) return '0.00'
      return Number(val).toFixed(2)
    },
    // 快速填入标准用途，仍可继续编辑为更具体的说明
    pickPurpose(item) {
      this.form.purpose = item.name
      this.$nextTick(() => {
        if (this.$refs.dataForm) this.$refs.dataForm.validateField('purpose', () => {})
      })
    }
  }
}
</script>

<style scoped>
.pagination-bar { margin-top:16px; text-align:right; }
.purpose-quick { display:flex; flex-wrap:wrap; gap:6px; margin-bottom:8px; }
.purpose-chip {
  display:inline-flex; align-items:center; height:24px; padding:0 10px;
  border-radius:var(--r-pill); border:1px solid var(--c-border); background:var(--c-bg-head);
  color:var(--c-text-2); font-size:12px; cursor:pointer;
  transition:color var(--t-fast), border-color var(--t-fast), background-color var(--t-fast);
}
.purpose-chip:hover { border-color:var(--c-primary-border); color:var(--c-primary); background:var(--c-primary-light); }
.purpose-chip.is-active { border-color:var(--c-primary); background:var(--c-primary); color:#fff; }
.danger-text { color:var(--c-danger); }
.kpi-value--amount { font-size:20px; letter-spacing:0.3px; }
.amount-cell { display:inline-block; font-size:15px; font-weight:700; color:var(--c-text-1); letter-spacing:0.2px; font-variant-numeric:tabular-nums; }
.voucher-text {
  font-size:12px;
  color:var(--c-primary);
  text-decoration:none;
  text-underline-offset:2px;
  transition:color var(--t-fast);
}
.voucher-text:hover { text-decoration:underline; color:var(--c-primary-hover); }
.empty-text { color:var(--c-text-4); }
.locked-text { color:var(--c-text-4); font-style:italic; }
</style>
