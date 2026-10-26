<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <div class="page-title">回访管理</div>
        <div class="page-subtitle">回访内容与凭证材料自动脱敏留存，与资助流水绑定后可追溯到人</div>
      </div>
      <div class="page-actions">
        <el-button v-permission="['AUDIT_ADMIN']" type="warning" icon="el-icon-download" :loading="exportLoading" @click="handleExport">导出</el-button>
        <el-button v-permission="['AUDIT_ADMIN']" type="primary" icon="el-icon-plus" @click="handleCreate">新增回访</el-button>
      </div>
    </div>

    <PrivacyTipBar />

    <div class="kpi-row">
      <div class="kpi-card">
        <div class="kpi-label">回访记录总数</div>
        <div class="kpi-value">{{ total }}</div>
        <div class="kpi-hint">条回访记录</div>
      </div>
      <div class="kpi-card kpi-card--success">
        <div class="kpi-label">本页已绑凭证</div>
        <div class="kpi-value">{{ boundInPage }}</div>
        <div class="kpi-hint">条已生成资助凭证</div>
      </div>
      <div class="kpi-card">
        <div class="kpi-label">本页凭证材料</div>
        <div class="kpi-value">{{ materialInPage }}</div>
        <div class="kpi-hint">条已上传脱敏材料</div>
      </div>
    </div>

    <div class="toolbar">
      <el-select v-model="listQuery.studentId" placeholder="选择学生筛选" clearable filterable style="width:180px" @change="handleFilter">
        <el-option v-for="item in studentOptions" :key="item.id" :label="item.name" :value="item.id" />
      </el-select>
      <el-input v-model="listQuery.keyword" placeholder="回访内容 / 问题" clearable style="width:220px" @keyup.enter.native="handleFilter" />
      <el-button type="primary" icon="el-icon-search" @click="handleFilter">查询</el-button>
      <el-button icon="el-icon-refresh" @click="handleReset">重置</el-button>
    </div>

    <el-table v-loading="listLoading" :data="list" border stripe style="width:100%">
      <el-table-column type="index" label="序号" width="55" align="center" />
      <el-table-column prop="studentName" label="学生" min-width="90" />
      <el-table-column prop="sponsorName" label="资助人" min-width="90" />
      <el-table-column prop="visitDate" label="回访日期" min-width="110" />
      <el-table-column prop="visitor" label="回访人" min-width="90" />
      <el-table-column label="绑定凭证号" min-width="205">
        <template slot-scope="{ row }">
          <el-tag v-if="row.voucherNo" size="small" type="info" class="voucher-tag">
            <i class="el-icon-lock"></i><span>{{ row.voucherNo }}</span>
          </el-tag>
          <span v-else class="muted-text">—</span>
        </template>
      </el-table-column>
      <el-table-column prop="content" label="回访内容" min-width="180">
        <template slot-scope="{ row }">
          <div class="clamp-cell">
            <div
              :data-expand-key="expandKey(row, 'content')"
              :class="['clamp-text', { 'is-expanded': expanded(row, 'content') }]"
            >{{ row.content }}</div>
            <el-button v-if="overflow(row, 'content')" type="text" class="expand-btn" @click="toggleExpand(row, 'content')">{{ expanded(row, 'content') ? '收起' : '展开' }}</el-button>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="situation" label="情况" min-width="160">
        <template slot-scope="{ row }">
          <div class="clamp-cell">
            <div
              :data-expand-key="expandKey(row, 'situation')"
              :class="['clamp-text', { 'is-expanded': expanded(row, 'situation') }]"
            >{{ row.situation }}</div>
            <el-button v-if="overflow(row, 'situation')" type="text" class="expand-btn" @click="toggleExpand(row, 'situation')">{{ expanded(row, 'situation') ? '收起' : '展开' }}</el-button>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="problem" label="存在问题" min-width="160">
        <template slot-scope="{ row }">
          <div class="clamp-cell">
            <div
              :data-expand-key="expandKey(row, 'problem')"
              :class="['clamp-text', { 'is-expanded': expanded(row, 'problem') }]"
            >{{ row.problem }}</div>
            <el-button v-if="overflow(row, 'problem')" type="text" class="expand-btn" @click="toggleExpand(row, 'problem')">{{ expanded(row, 'problem') ? '收起' : '展开' }}</el-button>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="maskedRecord" label="脱敏回访记录" min-width="200" show-overflow-tooltip>
        <template slot-scope="{ row }">
          <span v-if="row.maskedRecord">{{ row.maskedRecord }}</span>
          <span v-else class="muted-text">—</span>
        </template>
      </el-table-column>
      <el-table-column label="脱敏凭证" width="90" align="center">
        <template slot-scope="{ row }">
          <el-button v-if="row.maskedMaterial" type="text" icon="el-icon-view" @click="handleDownloadMaterial(row)">查看</el-button>
          <span v-else class="muted-text">—</span>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" min-width="150" />
      <el-table-column v-if="isAdmin" label="操作" width="220" align="center" fixed="right">
        <template slot-scope="{ row }">
          <el-button type="text" class="action-primary" icon="el-icon-edit" @click="handleUpdate(row)">编辑</el-button>
          <el-upload v-if="row.fundId" :show-file-list="false" action="#" :http-request="opt => handleUpload(row, opt)" style="display:inline-block">
            <el-button type="text" class="action-secondary" icon="el-icon-upload2" title="图片/文档请先手动脱敏">传凭证</el-button>
          </el-upload>
          <span v-else class="muted-text" title="该回访未绑定资助流水，请先编辑绑定">未绑定流水</span>
          <el-button type="text" class="danger-text" icon="el-icon-delete" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
      <template slot="empty">
        <EmptyState type="folder" title="暂无回访记录" desc="调整筛选条件，或新增一条回访记录" />
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

    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="600px" :close-on-click-modal="false">
      <el-form ref="dataForm" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="10">
          <el-col :span="12">
            <el-form-item label="学生" prop="studentId">
              <el-select v-model="form.studentId" placeholder="请选择学生" filterable style="width:100%" @change="onStudentChange">
                <el-option v-for="item in studentOptions" :key="item.id" :label="item.name" :value="item.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="资助人" prop="sponsorId">
              <el-select v-model="form.sponsorId" placeholder="请选择资助人" filterable style="width:100%" @change="onSponsorChange">
                <el-option v-for="item in sponsorOptions" :key="item.id" :label="item.realName" :value="item.id" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="10">
          <el-col :span="12">
            <el-form-item label="回访日期" prop="visitDate">
              <el-date-picker v-model="form.visitDate" type="date" value-format="yyyy-MM-dd" placeholder="选择日期" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="回访人" prop="visitor">
              <el-input v-model="form.visitor" placeholder="请输入回访人" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="10">
          <el-col :span="12">
            <el-form-item label="资助流水" prop="fundId">
              <el-select v-model="form.fundId" placeholder="请选择资助流水（必选）" filterable style="width:100%">
                <el-option v-for="item in fundOptions" :key="item.id" :label="fundLabel(item)" :value="item.id" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="回访内容" prop="content">
          <SensitiveTextarea v-model="form.content" field-name="回访内容" :rows="2" placeholder="请输入回访内容，请勿填写未打码的敏感信息" @block="contentBlock = $event" />
        </el-form-item>
        <el-form-item label="情况说明" prop="situation">
          <SensitiveTextarea v-model="form.situation" field-name="情况说明" :rows="2" placeholder="请输入情况说明，请勿填写未打码的敏感信息" @block="situationBlock = $event" />
        </el-form-item>
        <el-form-item label="存在问题" prop="problem">
          <SensitiveTextarea v-model="form.problem" field-name="存在问题" :rows="2" placeholder="请输入存在问题，请勿填写未打码的敏感信息" @block="problemBlock = $event" />
        </el-form-item>
        <div class="mask-tip">
          <i class="el-icon-lock"></i> 数据最小采集原则：请勿填写与核验无关的隐私信息；保存后系统将自动脱敏（身份证/手机号/银行卡/邮箱）生成脱敏回访记录。<br>
          <i class="el-icon-warning-outline"></i> 文本类凭证由系统自动脱敏；图片/PDF/Word 无法自动识别画面内容，上传前请手动遮挡敏感信息，系统会自动剥离图片元数据。
        </div>
      </el-form>
      <div slot="footer">
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" :loading="submitLoading" :disabled="contentBlock || situationBlock || problemBlock" @click="submitForm">确 定</el-button>
      </div>
    </el-dialog>

    <ComplianceFooter />
  </div>
</template>

<script>
import { listVisits, getVisit, addVisit, updateVisit, deleteVisit, exportVisits, uploadVisitMaterial, downloadVisitMaterial } from '@/api/visit'
import { studentOptions } from '@/api/student'
import { userOptions } from '@/api/user'
import { listFunds } from '@/api/fund'
import { downloadFile } from '@/utils/download'
import PrivacyTipBar from '@/components/PrivacyTipBar.vue'
import ComplianceFooter from '@/components/ComplianceFooter.vue'
import SensitiveTextarea from '@/components/SensitiveTextarea.vue'
import EmptyState from '@/components/EmptyState.vue'

export default {
  name: 'Visit',
  components: { PrivacyTipBar, ComplianceFooter, SensitiveTextarea, EmptyState },
  data() {
    return {
      list: [],
      total: 0,
      listLoading: false,
      listQuery: { page: 1, size: 10, studentId: undefined, keyword: '' },
      dialogVisible: false,
      submitLoading: false,
      exportLoading: false,
      dialogType: 'create',
      studentOptions: [],
      sponsorOptions: [],
      fundOptions: [],
      contentBlock: false,
      situationBlock: false,
      problemBlock: false,
      expandedMap: {},
      overflowMap: {},
      form: this.getEmptyForm(),
      rules: {
        studentId: [{ required: true, message: '请选择学生', trigger: 'change' }],
        sponsorId: [{ required: true, message: '请选择资助人', trigger: 'change' }],
        fundId: [{ required: true, message: '请选择资助流水', trigger: 'change' }],
        visitDate: [{ required: true, message: '请选择回访日期', trigger: 'change' }],
        visitor: [{ required: true, message: '请输入回访人', trigger: 'blur' }],
        content: [{ required: true, message: '请输入回访内容', trigger: 'blur' }]
      }
    }
  },
  computed: {
    isAdmin() { return this.$store.getters.role === 'AUDIT_ADMIN' },
    dialogTitle() { return this.dialogType === 'create' ? '新增回访' : '编辑回访' },
    // 仅用于顶部统计展示，不改变任何列表/回访逻辑
    boundInPage() {
      return this.list.filter(item => !!item.voucherNo).length
    },
    materialInPage() {
      return this.list.filter(item => !!item.maskedMaterial).length
    }
  },
  created() {
    this.getList()
    this.loadOptions()
  },
  mounted() {
    window.addEventListener('resize', this.measureOverflow)
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.measureOverflow)
  },
  methods: {
    getEmptyForm() {
      return { id: null, studentId: undefined, studentName: '', sponsorId: undefined, sponsorName: '', fundId: null, visitDate: '', content: '', situation: '', problem: '', visitor: '' }
    },
    getList() {
      this.listLoading = true
      listVisits(this.listQuery).then(data => {
        this.list = (data && data.records) || []
        this.total = (data && data.total) || 0
        this.measureOverflow()
      }).finally(() => { this.listLoading = false })
    },
    loadOptions() {
      studentOptions().then(data => { this.studentOptions = data || [] }).catch(() => {})
      userOptions('SPONSOR').then(data => { this.sponsorOptions = data || [] }).catch(() => {})
    },
    // 长文本展开状态（仅展示用，不影响数据）
    expandKey(row, field) {
      return row.id + ':' + field
    },
    expanded(row, field) {
      return !!this.expandedMap[this.expandKey(row, field)]
    },
    overflow(row, field) {
      return !!this.overflowMap[this.expandKey(row, field)]
    },
    toggleExpand(row, field) {
      const key = this.expandKey(row, field)
      const next = !this.expandedMap[key]
      this.$set(this.expandedMap, key, next)
      if (!next) this.measureOverflow()
    },
    // 根据 3 行截断后的实际高度判断是否需要「展开」
    measureOverflow() {
      this.$nextTick(() => {
        if (!this.$el || !this.$el.querySelectorAll) return
        const cells = this.$el.querySelectorAll('[data-expand-key]')
        const map = {}
        Array.prototype.forEach.call(cells, el => {
          const key = el.getAttribute('data-expand-key')
          if (!key) return
          map[key] = this.expandedMap[key] ? true : el.scrollHeight > el.clientHeight + 1
        })
        this.overflowMap = map
      })
    },
    handleFilter() { this.listQuery.page = 1; this.getList() },
    handleReset() { this.listQuery.studentId = undefined; this.listQuery.keyword = ''; this.listQuery.page = 1; this.getList() },
    handleSizeChange(size) { this.listQuery.size = size; this.listQuery.page = 1; this.getList() },
    handleCurrentChange(page) { this.listQuery.page = page; this.getList() },
    onStudentChange(val) {
      const t = this.studentOptions.find(s => s.id === val)
      this.form.studentName = t ? t.name : ''
      this.form.fundId = null
      this.loadFundOptions(val)
    },
    loadFundOptions(studentId) {
      if (!studentId) { this.fundOptions = []; return }
      listFunds({ page: 1, size: 100, studentId }).then(data => {
        this.fundOptions = (data && data.records) || []
      }).catch(() => { this.fundOptions = [] })
    },
    fundLabel(item) {
      const statusMap = { SUBMITTED: '待审核', AUDITED: '审核通过', REJECTED: '已驳回', REVIEWED: '复核通过', DISBURSED: '已拨付' }
      const no = item.voucherNo || ('申请#' + item.id)
      return no + ' ￥' + item.amount + '（' + (statusMap[item.status] || item.status) + '）'
    },
    handleUpload(row, opt) {
      const name = (opt.file && opt.file.name) || ''
      const ext = name.indexOf('.') >= 0 ? name.split('.').pop().toLowerCase() : ''
      const textExt = ['txt', 'md', 'csv', 'log']
      const doUpload = manuallyMasked => {
        uploadVisitMaterial(row.id, opt.file, manuallyMasked).then(() => {
          this.$message.success(manuallyMasked ? '凭证已上传（已确认人工脱敏）' : '凭证已上传并自动脱敏')
          this.getList()
        }).catch(() => {})
      }
      if (textExt.indexOf(ext) >= 0) {
        doUpload(false)
        return
      }
      this.$confirm(
        '图片/PDF/Word 类凭证无法由系统自动识别画面内容。请确认已手动遮挡身份证号、手机号、住址等敏感信息；系统仍会自动剥离图片元数据。',
        '上传前确认',
        { type: 'warning', confirmButtonText: '已手动脱敏，继续上传', cancelButtonText: '取消' }
      ).then(() => doUpload(true)).catch(() => {})
    },
    handleDownloadMaterial(row) {
      downloadVisitMaterial(row.id).then(blob => {
        const ext = (row.maskedMaterial || '').split('.').pop() || 'dat'
        return downloadFile(blob, '回访脱敏凭证_' + row.id + '.' + ext)
      }).catch(err => {
        if (err && err.message) this.$message.error(err.message)
      })
    },
    onSponsorChange(val) {
      const t = this.sponsorOptions.find(s => s.id === val)
      this.form.sponsorName = t ? t.realName : ''
    },
    handleCreate() {
      this.dialogType = 'create'
      this.form = this.getEmptyForm()
      this.contentBlock = false
      this.situationBlock = false
      this.problemBlock = false
      this.fundOptions = []
      this.dialogVisible = true
      this.$nextTick(() => { this.$refs.dataForm && this.$refs.dataForm.clearValidate() })
    },
    handleUpdate(row) {
      this.dialogType = 'update'
      getVisit(row.id).then(data => {
        this.form = Object.assign(this.getEmptyForm(), data || {})
        this.contentBlock = false
        this.situationBlock = false
        this.problemBlock = false
        this.loadFundOptions(this.form.studentId)
        this.dialogVisible = true
        this.$nextTick(() => { this.$refs.dataForm && this.$refs.dataForm.clearValidate() })
      })
    },
    submitForm() {
      this.$refs.dataForm.validate(valid => {
        if (!valid) return
        this.submitLoading = true
        const payload = {
          studentId: this.form.studentId, sponsorId: this.form.sponsorId, fundId: this.form.fundId,
          visitDate: this.form.visitDate, content: this.form.content,
          situation: this.form.situation, problem: this.form.problem, visitor: this.form.visitor
        }
        const action = this.dialogType === 'create' ? addVisit(payload) : updateVisit(this.form.id, payload)
        action.then(() => {
          this.$message.success(this.dialogType === 'create' ? '新增成功' : '修改成功')
          this.dialogVisible = false
          this.getList()
        }).finally(() => { this.submitLoading = false })
      })
    },
    handleDelete(row) {
      this.$confirm('确认删除该回访记录吗？', '提示', { type: 'warning' }).then(() => {
        deleteVisit(row.id).then(() => {
          this.$message.success('删除成功')
          if (this.list.length === 1 && this.listQuery.page > 1) this.listQuery.page--
          this.getList()
        })
      }).catch(() => {})
    },
    handleExport() {
      this.exportLoading = true
      exportVisits().then(blob => downloadFile(blob, '回访记录.xlsx')).then(() => {
        this.$message.success('导出成功')
      }).catch(err => {
        if (err && err.message) this.$message.error(err.message)
      }).finally(() => { this.exportLoading = false })
    }
  }
}
</script>

<style scoped>
.pagination-bar { margin-top:16px; text-align:right; }
.danger-text { color:var(--c-danger); }
.muted-text { color:var(--c-text-4); }
.voucher-tag { display:inline-flex; align-items:center; max-width:100%; }
.voucher-tag i { margin-right:4px; }
.voucher-tag span { overflow:hidden; text-overflow:ellipsis; white-space:nowrap; font-size:12px; font-family:'SFMono-Regular', Consolas, Menlo, monospace; }
.clamp-cell { line-height:1.5; }
.clamp-text {
  display:-webkit-box;
  -webkit-box-orient:vertical;
  -webkit-line-clamp:3;
  overflow:hidden;
  white-space:normal;
  word-break:break-word;
  color:var(--c-text-2);
}
.clamp-text.is-expanded { display:block; overflow:visible; -webkit-line-clamp:unset; }
.expand-btn { padding:0; margin-top:2px; font-size:12px; line-height:1.4; }
.el-table .action-primary { font-weight:600; }
.el-table .el-button--text.action-secondary { color:var(--c-text-3); }
.el-table .el-button--text.action-secondary:hover { color:var(--c-text-2); background-color:var(--c-bg-head); }
.mask-tip { margin:0 0 8px 100px; font-size:12px; color:var(--c-warning); line-height:1.5; }
</style>
