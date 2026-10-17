<template>
  <div class="page-container">
    <div class="filter-bar">
      <el-select v-model="listQuery.studentId" placeholder="选择学生筛选" clearable filterable style="width:180px" @change="handleFilter">
        <el-option v-for="item in studentOptions" :key="item.id" :label="item.name" :value="item.id" />
      </el-select>
      <el-button type="primary" icon="el-icon-search" @click="handleFilter">查询</el-button>
      <el-button icon="el-icon-refresh" @click="handleReset">重置</el-button>
      <div class="filter-actions">
        <el-button v-permission="['AUDIT_ADMIN']" type="success" icon="el-icon-plus" @click="handleCreate">新增评分</el-button>
        <el-button v-permission="['AUDIT_ADMIN']" type="warning" icon="el-icon-download" :loading="exportLoading" @click="handleExport">导出</el-button>
      </div>
    </div>

    <el-table v-loading="listLoading" :data="list" border stripe style="width:100%">
      <el-table-column type="index" label="序号" width="55" align="center" />
      <el-table-column prop="studentName" label="学生" min-width="100" />
      <el-table-column prop="sponsorName" label="资助人" min-width="100" />
      <el-table-column prop="scoreDate" label="评分日期" min-width="110" />
      <el-table-column prop="totalScore" label="总分" min-width="90" align="center">
        <template slot-scope="{ row }">
          <el-tag type="success" size="medium">{{ formatScore(row.totalScore) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
      <el-table-column prop="createTime" label="创建时间" min-width="150" />
      <el-table-column v-if="isAdmin" label="操作" width="190" align="center" fixed="right">
        <template slot-scope="{ row }">
          <el-button type="text" icon="el-icon-view" @click="handleDetail(row)">详情</el-button>
          <el-button type="text" icon="el-icon-edit" @click="handleUpdate(row)">编辑</el-button>
          <el-button type="text" class="danger-text" icon="el-icon-delete" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
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
    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="720px" :close-on-click-modal="false">
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
            <el-form-item label="评分日期" prop="scoreDate">
              <el-date-picker v-model="form.scoreDate" type="date" value-format="yyyy-MM-dd" placeholder="选择日期" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="总分预览">
              <el-tag type="success" size="medium">{{ liveTotal }}</el-tag>
              <span class="tip-text">（权重加权，最终以服务端为准）</span>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="备注" />
        </el-form-item>

        <div class="score-items-title">评分指标</div>
        <el-table :data="form.details" border size="mini" style="width:100%">
          <el-table-column prop="itemName" label="指标名称" min-width="120" />
          <el-table-column prop="weight" label="权重(%)" width="100" align="center" />
          <el-table-column label="得分(0-100)" width="180">
            <template slot-scope="{ row }">
              <el-input-number v-model="row.itemScore" :min="0" :max="100" :step="1" :precision="0" controls-position="right" size="mini" style="width:100%" />
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
    <el-dialog title="评分详情" :visible.sync="detailVisible" width="680px">
      <el-descriptions :column="2" border size="medium">
        <el-descriptions-item label="学生">{{ detail.studentName }}</el-descriptions-item>
        <el-descriptions-item label="资助人">{{ detail.sponsorName }}</el-descriptions-item>
        <el-descriptions-item label="评分日期">{{ detail.scoreDate }}</el-descriptions-item>
        <el-descriptions-item label="总分">{{ formatScore(detail.totalScore) }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ detail.remark }}</el-descriptions-item>
      </el-descriptions>
      <el-table :data="detail.details" border size="small" style="width:100%; margin-top:12px">
        <el-table-column prop="itemName" label="指标名称" min-width="120" />
        <el-table-column prop="weight" label="权重(%)" width="100" align="center" />
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
import { listScores, getScore, addScore, updateScore, deleteScore, exportScores } from '@/api/score'
import { listScoreItems } from '@/api/scoreItem'
import { studentOptions } from '@/api/student'
import { userOptions } from '@/api/user'
import { downloadFile } from '@/utils/download'

export default {
  name: 'Score',
  data() {
    return {
      list: [],
      total: 0,
      listLoading: false,
      listQuery: { page: 1, size: 10, studentId: undefined },
      dialogVisible: false,
      detailVisible: false,
      submitLoading: false,
      exportLoading: false,
      dialogType: 'create',
      studentOptions: [],
      sponsorOptions: [],
      scoreItems: [],
      form: this.getEmptyForm(),
      detail: { details: [] },
      rules: {
        studentId: [{ required: true, message: '请选择学生', trigger: 'change' }],
        sponsorId: [{ required: true, message: '请选择资助人', trigger: 'change' }],
        scoreDate: [{ required: true, message: '请选择评分日期', trigger: 'change' }]
      }
    }
  },
  computed: {
    isAdmin() { return this.$store.getters.role === 'AUDIT_ADMIN' },
    dialogTitle() { return this.dialogType === 'create' ? '新增评分' : '编辑评分' },
    liveTotal() {
      const sum = (this.form.details || []).reduce((s, d) => {
        return s + Number(d.itemScore || 0) * Number(d.weight || 0) / 100
      }, 0)
      return sum.toFixed(2)
    }
  },
  created() {
    this.getList()
    this.loadOptions()
    this.loadScoreItems()
  },
  methods: {
    getEmptyForm() {
      return { id: null, studentId: undefined, studentName: '', sponsorId: undefined, sponsorName: '', scoreDate: '', remark: '', details: [] }
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
    // 根据当前指标初始化 details
    buildDetails(savedDetails) {
      const map = {}
      ;(savedDetails || []).forEach(d => { map[d.itemId] = d })
      return this.scoreItems.map(it => {
        const saved = map[it.id]
        return {
          itemId: it.id,
          itemName: it.name,
          weight: it.weight,
          itemScore: saved ? Number(saved.itemScore || 0) : 0
        }
      })
    },
    handleFilter() { this.listQuery.page = 1; this.getList() },
    handleReset() { this.listQuery.studentId = undefined; this.listQuery.page = 1; this.getList() },
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
      if (!this.scoreItems.length) {
        this.$message.warning('暂无评分指标，无法新增评分')
        return
      }
      this.dialogType = 'create'
      this.form = this.getEmptyForm()
      this.form.details = this.buildDetails([])
      this.dialogVisible = true
      this.$nextTick(() => { this.$refs.dataForm && this.$refs.dataForm.clearValidate() })
    },
    handleUpdate(row) {
      if (!this.scoreItems.length) {
        this.$message.warning('暂无评分指标，无法编辑评分')
        return
      }
      this.dialogType = 'update'
      getScore(row.id).then(data => {
        this.form = Object.assign(this.getEmptyForm(), data || {})
        // 用当前指标重建 details，回填已保存的得分
        this.form.details = this.buildDetails(data && data.details)
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
        const invalid = this.form.details.some(d => d.itemScore === null || d.itemScore === undefined || d.itemScore < 0 || d.itemScore > 100)
        if (invalid) {
          this.$message.warning('请确保每项得分在 0-100 之间')
          return
        }
        this.submitLoading = true
        const payload = {
          studentId: this.form.studentId,
          sponsorId: this.form.sponsorId,
          scoreDate: this.form.scoreDate,
          remark: this.form.remark,
          details: this.form.details.map(d => ({ itemId: d.itemId, itemScore: d.itemScore }))
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
      const v = Number(row.itemScore || 0) * Number(row.weight || 0) / 100
      return v.toFixed(2)
    },
    formatScore(val) {
      if (val === undefined || val === null || val === '') return '-'
      return Number(val).toFixed(2)
    }
  }
}
</script>

<style scoped>
.page-container { background:#fff; padding:16px; border-radius:4px; }
.filter-bar { display:flex; align-items:center; gap:8px; margin-bottom:16px; }
.filter-actions { margin-left:auto; display:flex; gap:8px; }
.pagination-bar { margin-top:16px; text-align:right; }
.danger-text { color:#F56C6C; }
.score-items-title { font-size:14px; font-weight:600; color:#303133; margin:10px 0 8px; border-left:3px solid #409EFF; padding-left:8px; }
.tip-text { font-size:12px; color:#909399; margin-left:8px; }
.empty-tip { color:#909399; font-size:13px; text-align:center; padding:12px 0; }
</style>
