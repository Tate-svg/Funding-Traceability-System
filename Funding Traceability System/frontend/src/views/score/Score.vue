<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <div class="page-title">评分管理</div>
        <div class="page-subtitle">按指标权重加权计分，记录资助对象阶段表现，用于复核与抽检参考</div>
      </div>
      <div class="page-actions">
        <el-button v-permission="['AUDIT_ADMIN']" type="warning" icon="el-icon-download" :loading="exportLoading" @click="handleExport">导出</el-button>
        <el-button v-permission="['AUDIT_ADMIN']" type="primary" icon="el-icon-plus" @click="handleCreate">新增评分</el-button>
      </div>
    </div>

    <div class="kpi-row">
      <div class="kpi-card">
        <div class="kpi-label">评分记录总数</div>
        <div class="kpi-value">{{ total }}</div>
        <div class="kpi-hint">条评分记录</div>
      </div>
      <div class="kpi-card kpi-card--success">
        <div class="kpi-label">本页平均总分</div>
        <div class="kpi-value">{{ pageAvgScore }}</div>
        <div class="kpi-hint">百分制加权均分</div>
      </div>
      <div class="kpi-card">
        <div class="kpi-label">本页优秀记录</div>
        <div class="kpi-value">{{ excellentCount }}</div>
        <div class="kpi-hint">总分 ≥ 90 分</div>
      </div>
    </div>

    <div class="toolbar">
      <el-select v-model="listQuery.studentId" placeholder="选择学生筛选" clearable filterable style="width:180px" @change="handleFilter">
        <el-option v-for="item in studentOptions" :key="item.id" :label="item.name" :value="item.id" />
      </el-select>
      <el-input-number v-model="listQuery.fundId" :min="1" :precision="0" :controls="false" placeholder="按资助单ID筛选" style="width:170px" />
      <el-button type="primary" icon="el-icon-search" @click="handleFilter">查询</el-button>
      <el-button icon="el-icon-refresh" @click="handleReset">重置</el-button>
    </div>

    <el-table v-loading="listLoading" :data="list" border stripe style="width:100%">
      <el-table-column type="index" label="序号" width="55" align="center" />
      <el-table-column prop="studentName" label="学生" min-width="100" />
      <el-table-column prop="sponsorName" label="资助人" min-width="100" />
      <el-table-column label="关联资助单" min-width="160"><template slot-scope="{ row }">{{ row.fundNo || '历史未关联' }}</template></el-table-column>
      <el-table-column label="规则快照" min-width="130"><template slot-scope="{ row }">{{ row.ruleVersion ? row.ruleVersion.slice(0, 12) : '历史未留存' }}</template></el-table-column>
      <el-table-column prop="scoreDate" label="评分日期" min-width="110" />
      <el-table-column prop="totalScore" label="总分" min-width="120" align="center">
        <template slot-scope="{ row }">
          <span :class="['score-total', scoreLevelClass(row.totalScore)]">{{ formatScore(row.totalScore) }}</span>
          <el-progress
            :percentage="scorePercent(row.totalScore)"
            :stroke-width="6"
            :show-text="false"
            :color="scoreLevelColor(row.totalScore)"
            class="score-progress"
          />
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
      <el-table-column prop="createTime" label="创建时间" min-width="150" />
      <el-table-column label="操作" width="215" align="center" fixed="right" class-name="op-col">
        <template slot-scope="{ row }">
          <el-button type="text" icon="el-icon-view" @click="handleDetail(row)">详情</el-button>
          <el-button v-if="isAdmin" type="text" icon="el-icon-edit" @click="handleUpdate(row)">编辑</el-button>
          <el-button v-if="isAdmin" type="text" class="danger-text" icon="el-icon-delete" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
      <template slot="empty">
        <EmptyState type="chart" title="暂无评分记录" desc="调整筛选条件，或新增一条评分记录" />
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

    <!-- 新增/编辑弹窗 -->
    <el-dialog append-to-body :title="dialogTitle" :visible.sync="dialogVisible" width="720px" :close-on-click-modal="false">
      <el-form ref="dataForm" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="10">
          <el-col :span="12">
            <el-form-item label="学生" prop="studentId">
              <el-select :disabled="dialogType === 'update'" v-model="form.studentId" placeholder="请选择学生" filterable style="width:100%" @change="onStudentChange">
                <el-option v-for="item in studentOptions" :key="item.id" :label="item.name" :value="item.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="资助人" prop="sponsorId">
              <el-select :disabled="dialogType === 'update'" v-model="form.sponsorId" placeholder="请选择资助人" filterable style="width:100%" @change="onSponsorChange">
                <el-option v-for="item in sponsorOptions" :key="item.id" :label="item.realName" :value="item.id" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="10">
          <el-col :span="12">
            <el-form-item label="评分日期" prop="scoreDate">
              <el-date-picker :disabled="legacyEditing" v-model="form.scoreDate" type="date" value-format="yyyy-MM-dd" placeholder="选择日期" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="总分预览">
              <el-tag type="success" size="medium">{{ liveTotal }}</el-tag>
              <span class="tip-text">（所有指标必填，最终以服务端为准）</span>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="关联资助单" prop="fundId">
          <el-select v-model="form.fundId" filterable remote :remote-method="loadFundOptions" :loading="fundLoading" placeholder="先选学生及资助人；可搜凭证号或完整ID" style="width:100%">
            <el-option v-for="item in fundOptions" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-alert v-if="legacyEditing" title="历史规则未留存：保留原得分和日期，仅可补充资助单关联及备注。调整得分请新增评分。" type="warning" :closable="false" />
        <el-alert v-else-if="dialogType === 'update'" title="编辑使用原评分规则快照，不套用当前指标及权重。" type="info" :closable="false" />
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="备注" />
        </el-form-item>

        <div class="score-items-title">评分指标</div>
        <el-table :data="form.details" border size="mini" style="width:100%">
          <el-table-column prop="itemName" label="指标名称" min-width="120" />
          <el-table-column label="权重(%)" width="100" align="center"><template slot-scope="{ row }">{{ row.weight == null ? '未留存' : row.weight }}</template></el-table-column>
          <el-table-column label="得分(0-100)" width="180">
            <template slot-scope="{ row }">
              <el-input-number :disabled="legacyEditing" v-model="row.itemScore" :min="0" :max="100" :step="1" :precision="2" controls-position="right" size="mini" placeholder="请填写" style="width:100%" />
            </template>
          </el-table-column>
          <el-table-column label="加权得分" width="120" align="center">
            <template slot-scope="{ row }">{{ weighted(row) }}</template>
          </el-table-column>
        </el-table>
        <div v-if="!form.details.length" class="empty-tip">暂无评分指标，请先在「评分指标」页配置</div>
      </el-form>
      <div slot="footer">
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="submitForm">确 定</el-button>
      </div>
    </el-dialog>

    <!-- 详情弹窗（只读） -->
    <el-dialog append-to-body title="评分详情" :visible.sync="detailVisible" width="680px">
      <el-descriptions :column="2" border size="medium">
        <el-descriptions-item label="学生">{{ detail.studentName }}</el-descriptions-item>
        <el-descriptions-item label="资助人">{{ detail.sponsorName }}</el-descriptions-item>
        <el-descriptions-item label="评分日期">{{ detail.scoreDate }}</el-descriptions-item>
        <el-descriptions-item label="总分">{{ formatScore(detail.totalScore) }}</el-descriptions-item>
        <el-descriptions-item label="关联资助单">{{ detail.fundNo || '历史未关联' }}</el-descriptions-item>
        <el-descriptions-item label="规则留存时间">{{ detail.ruleCapturedAt || '未留存' }}</el-descriptions-item>
        <el-descriptions-item label="规则标识" :span="2">{{ detail.ruleVersion || '历史规则未留存，权重无法还原' }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ detail.remark }}</el-descriptions-item>
      </el-descriptions>
      <el-table :data="detail.details" border size="small" style="width:100%; margin-top:12px">
        <el-table-column prop="itemName" label="指标名称" min-width="120" />
        <el-table-column label="权重(%)" width="100" align="center"><template slot-scope="{ row }">{{ row.weight == null ? '未留存' : row.weight }}</template></el-table-column>
        <el-table-column prop="itemScore" label="得分" width="100" align="center" />
        <el-table-column label="加权得分" align="center">
          <template slot-scope="{ row }">{{ weighted(row) }}</template>
        </el-table-column>
      </el-table>
      <div slot="footer">
        <el-button @click="detailVisible = false">关 闭</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listScores, getScore, addScore, updateScore, deleteScore, exportScores, scoreFundOptions } from '@/api/score'
import { listScoreItems } from '@/api/scoreItem'
import { studentOptions } from '@/api/student'
import { userOptions } from '@/api/user'
import { downloadFile } from '@/utils/download'
import EmptyState from '@/components/EmptyState.vue'
import { scoreValidationError, scorePreview } from '@/utils/scoreValidation'

export default {
  name: 'Score',
  components: { EmptyState },
  data() {
    return {
      list: [],
      total: 0,
      listLoading: false,
      listQuery: { page: 1, size: 10, studentId: undefined, fundId: Number(this.$route.query.fundId) || undefined },
      dialogVisible: false,
      detailVisible: false,
      submitLoading: false,
      exportLoading: false,
      dialogType: 'create',
      studentOptions: [],
      sponsorOptions: [],
      scoreItems: [],
      fundOptions: [],
      fundLoading: false,
      form: this.getEmptyForm(),
      detail: { details: [] },
      rules: {
        fundId: [{ required: true, message: '请选择关联资助单', trigger: 'change' }],
        studentId: [{ required: true, message: '请选择学生', trigger: 'change' }],
        sponsorId: [{ required: true, message: '请选择资助人', trigger: 'change' }],
        scoreDate: [{ required: true, message: '请选择评分日期', trigger: 'change' }]
      }
    }
  },
  computed: {
    isAdmin() { return this.$store.getters.role === 'AUDIT_ADMIN' },
    legacyEditing() { return this.dialogType === 'update' && !this.form.ruleVersion },
    dialogTitle() { return this.dialogType === 'create' ? '新增评分' : '编辑评分' },
    liveTotal() {
      if (this.legacyEditing) return this.formatScore(this.form.totalScore)
      const total = scorePreview(this.form.details)
      return total === null ? '待完整填写' : total
    },
    // 以下三项仅用于顶部统计展示，不改变任何评分逻辑
    pageAvgScore() {
      const valid = this.list.filter(item => item.totalScore !== null && item.totalScore !== undefined && item.totalScore !== '')
      if (!valid.length) return '-'
      const sum = valid.reduce((s, item) => s + Number(item.totalScore || 0), 0)
      return (sum / valid.length).toFixed(2)
    },
    excellentCount() {
      return this.list.filter(item => Number(item.totalScore) >= 90).length
    }
  },
  created() {
    this.getList()
    this.loadOptions()
    this.loadScoreItems()
  },
  methods: {
    getEmptyForm() {
      return { fundId: undefined, ruleVersion: null, ruleCapturedAt: null, id: null, studentId: undefined, studentName: '', sponsorId: undefined, sponsorName: '', scoreDate: '', remark: '', details: [] }
    },
    getList() {
      this.listLoading = true
      listScores(this.listQuery).then(data => {
        this.list = (data && data.records) || []
        this.total = (data && data.total) || 0
      }).finally(() => { this.listLoading = false })
    },
    loadOptions() {
      studentOptions().then(data => { this.studentOptions = data || [] }).catch(() => {})
      userOptions('SPONSOR').then(data => { this.sponsorOptions = data || [] }).catch(() => {})
    },
    loadScoreItems() {
      listScoreItems().then(data => { this.scoreItems = data || [] }).catch(() => {})
    },
    loadFundOptions(keyword = '') {
      const studentId = this.form.studentId
      const sponsorId = this.form.sponsorId
      if (!studentId || !sponsorId) { this.fundOptions = []; return }
      this.fundLoading = true
      scoreFundOptions({ studentId, sponsorId, keyword }).then(data => {
        if (studentId !== this.form.studentId || sponsorId !== this.form.sponsorId) return
        this.fundOptions = data || []
        if (this.form.fundId && !this.fundOptions.some(f => f.id === this.form.fundId)) {
          this.fundOptions.unshift({ id: this.form.fundId, name: this.form.fundNo || ('资助单 #' + this.form.fundId) })
        }
      }).finally(() => { this.fundLoading = false })
    },
    // 新增按当前规则初始化；编辑直接使用服务端返回的历史快照。
    buildDetails(savedDetails) {
      const map = {}
      ;(savedDetails || []).forEach(d => { map[d.itemId] = d })
      return this.scoreItems.map(it => {
        const saved = map[it.id]
        return {
          itemId: it.id,
          itemName: it.name,
          weight: it.weight,
          itemScore: saved && saved.itemScore !== null && saved.itemScore !== undefined ? Number(saved.itemScore) : undefined
        }
      })
    },
    handleFilter() { this.listQuery.page = 1; this.getList() },
    handleReset() { this.listQuery.fundId = undefined; this.listQuery.studentId = undefined; this.listQuery.page = 1; this.getList() },
    handleSizeChange(size) { this.listQuery.size = size; this.listQuery.page = 1; this.getList() },
    handleCurrentChange(page) { this.listQuery.page = page; this.getList() },
    onStudentChange(val) {
      const t = this.studentOptions.find(s => s.id === val)
      this.form.studentName = t ? t.name : ''
      this.form.fundId = undefined
      this.loadFundOptions()
    },
    onSponsorChange(val) {
      const t = this.sponsorOptions.find(s => s.id === val)
      this.form.sponsorName = t ? t.name : ''
      this.form.fundId = undefined
      this.loadFundOptions()
    },
    handleCreate() {
      if (!this.scoreItems.length) {
        this.$message.warning('暂无评分指标，无法新增评分')
        return
      }
      this.dialogType = 'create'
      this.form = this.getEmptyForm()
      this.fundOptions = []
      this.form.details = this.buildDetails([])
      this.dialogVisible = true
      this.$nextTick(() => { this.$refs.dataForm && this.$refs.dataForm.clearValidate() })
    },
    handleUpdate(row) {
      this.dialogType = 'update'
      getScore(row.id).then(data => {
        this.form = Object.assign(this.getEmptyForm(), data || {})
        this.form.details = ((data && data.details) || []).map(d => ({ ...d, itemScore: d.itemScore == null ? undefined : Number(d.itemScore) }))
        this.loadFundOptions()
        this.dialogVisible = true
        this.$nextTick(() => { this.$refs.dataForm && this.$refs.dataForm.clearValidate() })
      })
    },
    handleDetail(row) {
      getScore(row.id).then(data => {
        this.detail = data || {}
        if (!this.detail.details) this.detail.details = []
        this.detailVisible = true
      })
    },
    submitForm() {
      this.$refs.dataForm.validate(valid => {
        if (!valid) return
        // 校验每项得分
        const error = this.legacyEditing ? '' : scoreValidationError(this.form.details)
        if (error) {
          this.$message.warning(error)
          return
        }
        this.submitLoading = true
        const payload = {
          fundId: this.form.fundId,
          studentId: this.form.studentId,
          sponsorId: this.form.sponsorId,
          scoreDate: this.form.scoreDate,
          remark: this.form.remark,
          details: this.legacyEditing ? undefined : this.form.details.map(d => ({ itemId: d.itemId, itemScore: d.itemScore }))
        }
        const action = this.dialogType === 'create' ? addScore(payload) : updateScore(this.form.id, payload)
        action.then(() => {
          this.$message.success(this.dialogType === 'create' ? '新增成功' : '修改成功')
          this.dialogVisible = false
          this.getList()
        }).finally(() => { this.submitLoading = false })
      })
    },
    handleDelete(row) {
      this.$confirm('确认删除该评分记录吗？', '提示', { type: 'warning' }).then(() => {
        deleteScore(row.id).then(() => {
          this.$message.success('删除成功')
          if (this.list.length === 1 && this.listQuery.page > 1) this.listQuery.page--
          this.getList()
        })
      }).catch(() => {})
    },
    handleExport() {
      this.exportLoading = true
      exportScores().then(blob => downloadFile(blob, '评分记录.xlsx')).then(() => {
        this.$message.success('导出成功')
      }).catch(err => {
        if (err && err.message) this.$message.error(err.message)
      }).finally(() => { this.exportLoading = false })
    },
    weighted(row) {
      if (row.weight == null) return '未留存'
      if (row.itemScore === null || row.itemScore === undefined || row.itemScore === '') return '待填写'
      const v = Number(row.itemScore) * Number(row.weight || 0) / 100
      return v.toFixed(2)
    },
    formatScore(val) {
      if (val === undefined || val === null || val === '') return '-'
      return Number(val).toFixed(2)
    },
    // 展示用等级阈值：优秀≥90、良好≥75、合格≥60、不合格<60（后端未返回评分等级字段）
    scoreLevelClass(val) {
      if (val === undefined || val === null || val === '') return 'score-muted'
      const n = Number(val)
      if (n >= 90) return 'score-excellent'
      if (n >= 75) return 'score-good'
      if (n >= 60) return 'score-qualified'
      return 'score-fail'
    },
    scoreLevelColor(val) {
      if (val === undefined || val === null || val === '') return '#c3cedb'
      const n = Number(val)
      if (n >= 90) return '#1f7d52'
      if (n >= 75) return '#2b6cb0'
      if (n >= 60) return '#8a5a10'
      return '#a83a36'
    },
    // 领域内总分为加权百分制，后端未返回满分字段，此处固定按满分 100 计算进度
    scorePercent(val) {
      if (val === undefined || val === null || val === '') return 0
      const n = Number(val)
      if (isNaN(n)) return 0
      return Math.max(0, Math.min(100, n))
    }
  }
}
</script>

<style scoped>
.pagination-bar { margin-top:16px; text-align:right; }
.danger-text { color:var(--c-danger-text); }
.score-total { display:inline-block; font-size:22px; font-weight:700; line-height:1.2; font-variant-numeric:tabular-nums; }
.score-excellent { color:var(--c-success-text); }
.score-good { color:var(--c-primary); }
.score-qualified { color:var(--c-warning-text); }
.score-fail { color:var(--c-danger-text); }
.score-muted { color:var(--c-text-3); }
.score-progress { width:100px; margin:12px auto 0; }
.score-progress ::v-deep .el-progress-bar__outer { background-color:var(--c-border-light); border-radius:5px; }
.score-progress ::v-deep .el-progress-bar__inner { border-radius:5px; }
.score-items-title { font-size:15px; font-weight:600; color:var(--c-text-1); margin:10px 0 8px; border-left:3px solid var(--c-primary); padding-left:8px; }
.tip-text { font-size:12px; color:var(--c-text-3); margin-left:8px; }
.empty-tip { color:var(--c-text-3); font-size:13px; text-align:center; padding:12px 0; }
/* 桌面端操作列按钮单行排列 */
@media (min-width: 769px) {
  ::v-deep .op-col .cell { white-space: nowrap; }
}
</style>
