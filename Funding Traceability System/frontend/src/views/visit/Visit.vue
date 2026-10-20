<template>
  <div class="page-container">
    <PrivacyTipBar />
    <div class="filter-bar">
      <el-select v-model="listQuery.studentId" placeholder="选择学生筛选" clearable filterable style="width:180px" @change="handleFilter">
        <el-option v-for="item in studentOptions" :key="item.id" :label="item.name" :value="item.id" />
      </el-select>
      <el-input v-model="listQuery.keyword" placeholder="回访内容 / 问题" clearable style="width:220px" @keyup.enter.native="handleFilter" />
      <el-button type="primary" icon="el-icon-search" @click="handleFilter">查询</el-button>
      <el-button icon="el-icon-refresh" @click="handleReset">重置</el-button>
      <div class="filter-actions">
        <el-button v-permission="['AUDIT_ADMIN']" type="success" icon="el-icon-plus" @click="handleCreate">新增回访</el-button>
        <el-button v-permission="['AUDIT_ADMIN']" type="warning" icon="el-icon-download" :loading="exportLoading" @click="handleExport">导出</el-button>
      </div>
    </div>

    <el-table v-loading="listLoading" :data="list" border stripe style="width:100%">
      <el-table-column type="index" label="序号" width="55" align="center" />
      <el-table-column prop="studentName" label="学生" min-width="90" />
      <el-table-column prop="sponsorName" label="资助人" min-width="90" />
      <el-table-column prop="visitDate" label="回访日期" min-width="110" />
      <el-table-column prop="visitor" label="回访人" min-width="90" />
      <el-table-column prop="content" label="回访内容" min-width="160" show-overflow-tooltip />
      <el-table-column prop="situation" label="情况" min-width="140" show-overflow-tooltip />
      <el-table-column prop="problem" label="存在问题" min-width="140" show-overflow-tooltip />
      <el-table-column prop="createTime" label="创建时间" min-width="150" />
      <el-table-column v-if="isAdmin" label="操作" width="150" align="center" fixed="right">
        <template slot-scope="{ row }">
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
        <el-form-item label="回访内容" prop="content">
          <SensitiveTextarea v-model="form.content" field-name="回访内容" :rows="2" placeholder="请输入回访内容，请勿填写未打码的敏感信息" @block="contentBlock = $event" />
        </el-form-item>
        <el-form-item label="情况说明" prop="situation">
          <SensitiveTextarea v-model="form.situation" field-name="情况说明" :rows="2" placeholder="请输入情况说明，请勿填写未打码的敏感信息" @block="situationBlock = $event" />
        </el-form-item>
        <el-form-item label="存在问题" prop="problem">
          <SensitiveTextarea v-model="form.problem" field-name="存在问题" :rows="2" placeholder="请输入存在问题，请勿填写未打码的敏感信息" @block="problemBlock = $event" />
        </el-form-item>
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
import { listVisits, getVisit, addVisit, updateVisit, deleteVisit, exportVisits } from '@/api/visit'
import { studentOptions } from '@/api/student'
import { userOptions } from '@/api/user'
import { downloadFile } from '@/utils/download'
import PrivacyTipBar from '@/components/PrivacyTipBar.vue'
import ComplianceFooter from '@/components/ComplianceFooter.vue'
import SensitiveTextarea from '@/components/SensitiveTextarea.vue'

export default {
  name: 'Visit',
  components: { PrivacyTipBar, ComplianceFooter, SensitiveTextarea },
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
      contentBlock: false,
      situationBlock: false,
      problemBlock: false,
      form: this.getEmptyForm(),
      rules: {
        studentId: [{ required: true, message: '请选择学生', trigger: 'change' }],
        sponsorId: [{ required: true, message: '请选择资助人', trigger: 'change' }],
        visitDate: [{ required: true, message: '请选择回访日期', trigger: 'change' }],
        visitor: [{ required: true, message: '请输入回访人', trigger: 'blur' }],
        content: [{ required: true, message: '请输入回访内容', trigger: 'blur' }]
      }
    }
  },
  computed: {
    isAdmin() { return this.$store.getters.role === 'AUDIT_ADMIN' },
    dialogTitle() { return this.dialogType === 'create' ? '新增回访' : '编辑回访' }
  },
  created() {
    this.getList()
    this.loadOptions()
  },
  methods: {
    getEmptyForm() {
      return { id: null, studentId: undefined, studentName: '', sponsorId: undefined, sponsorName: '', visitDate: '', content: '', situation: '', problem: '', visitor: '' }
    },
    getList() {
      this.listLoading = true
      listVisits(this.listQuery).then(data => {
        this.list = (data && data.records) || []
        this.total = (data && data.total) || 0
      }).finally(() => { this.listLoading = false })
    },
    loadOptions() {
      studentOptions().then(data => { this.studentOptions = data || [] }).catch(() => {})
      userOptions('SPONSOR').then(data => { this.sponsorOptions = data || [] }).catch(() => {})
    },
    handleFilter() { this.listQuery.page = 1; this.getList() },
    handleReset() { this.listQuery.studentId = undefined; this.listQuery.keyword = ''; this.listQuery.page = 1; this.getList() },
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
      this.contentBlock = false
      this.situationBlock = false
      this.problemBlock = false
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
        this.dialogVisible = true
        this.$nextTick(() => { this.$refs.dataForm && this.$refs.dataForm.clearValidate() })
      })
    },
    submitForm() {
      this.$refs.dataForm.validate(valid => {
        if (!valid) return
        this.submitLoading = true
        const payload = {
          studentId: this.form.studentId, sponsorId: this.form.sponsorId,
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
.page-container { background:#fff; padding:16px; border-radius:4px; }
.filter-bar { display:flex; align-items:center; gap:8px; margin-bottom:16px; }
.filter-actions { margin-left:auto; display:flex; gap:8px; }
.pagination-bar { margin-top:16px; text-align:right; }
.danger-text { color:#F56C6C; }
</style>
