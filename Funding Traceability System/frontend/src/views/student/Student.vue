<template>
  <div class="page-container">
    <!-- 查询区 -->
    <div class="filter-bar">
      <el-input
        v-model="listQuery.keyword"
        placeholder="姓名 / 身份证 / 学校"
        clearable
        style="width: 240px"
        @keyup.enter.native="handleFilter"
      />
      <el-button type="primary" icon="el-icon-search" @click="handleFilter">查询</el-button>
      <el-button icon="el-icon-refresh" @click="handleReset">重置</el-button>
      <div class="filter-actions">
        <el-button v-permission="['AUDIT_ADMIN']" type="success" icon="el-icon-plus" @click="handleCreate">新增学生</el-button>
        <el-button v-permission="['AUDIT_ADMIN']" type="warning" icon="el-icon-download" :loading="exportLoading" @click="handleExport">导出</el-button>
      </div>
    </div>

    <!-- 表格 -->
    <el-table
      v-loading="listLoading"
      :data="list"
      border
      stripe
      style="width: 100%"
    >
      <el-table-column type="index" label="序号" width="55" align="center" />
      <el-table-column prop="name" label="姓名" min-width="90" />
      <el-table-column prop="gender" label="性别" width="60" align="center" />
      <el-table-column prop="idCard" label="身份证号" min-width="160" />
      <el-table-column prop="school" label="学校" min-width="120" show-overflow-tooltip />
      <el-table-column prop="grade" label="年级" min-width="100" show-overflow-tooltip />
      <el-table-column prop="phone" label="联系电话" min-width="110" />
      <el-table-column prop="sponsorName" label="资助人" min-width="90" />
      <el-table-column prop="createTime" label="创建时间" min-width="150" />
      <el-table-column v-if="isAdmin" label="操作" width="150" align="center" fixed="right">
        <template slot-scope="{ row }">
          <el-button type="text" icon="el-icon-edit" @click="handleUpdate(row)">编辑</el-button>
          <el-button type="text" class="danger-text" icon="el-icon-delete" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
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
    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="640px" :close-on-click-modal="false">
      <el-form ref="dataForm" :model="form" :rules="rules" label-width="100px" class="form-grid">
        <el-row :gutter="10">
          <el-col :span="12">
            <el-form-item label="姓名" prop="name">
              <el-input v-model="form.name" placeholder="请输入姓名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="性别" prop="gender">
              <el-select v-model="form.gender" placeholder="请选择性别" style="width:100%">
                <el-option label="男" value="男" />
                <el-option label="女" value="女" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="10">
          <el-col :span="12">
            <el-form-item label="身份证号" prop="idCard">
              <el-input v-model="form.idCard" placeholder="请输入身份证号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系电话" prop="phone">
              <el-input v-model="form.phone" placeholder="请输入联系电话" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="10">
          <el-col :span="12">
            <el-form-item label="学校" prop="school">
              <el-input v-model="form.school" placeholder="请输入学校" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="年级" prop="grade">
              <el-input v-model="form.grade" placeholder="请输入年级" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="资助人" prop="sponsorId">
          <el-select v-model="form.sponsorId" placeholder="请选择资助人" filterable style="width:100%" @change="onSponsorChange">
            <el-option v-for="item in sponsorOptions" :key="item.id" :label="item.realName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="家庭住址" prop="familyAddress">
          <el-input v-model="form.familyAddress" placeholder="请输入家庭住址" />
        </el-form-item>
        <el-form-item label="家庭情况" prop="familySituation">
          <el-input v-model="form.familySituation" type="textarea" :rows="3" placeholder="请描述家庭情况" />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="submitForm">确 定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listStudents, getStudent, addStudent, updateStudent, deleteStudent, exportStudents } from '@/api/student'
import { userOptions } from '@/api/user'
import { downloadFile } from '@/utils/download'

export default {
  name: 'Student',
  data() {
    return {
      list: [],
      total: 0,
      listLoading: false,
      listQuery: { page: 1, size: 10, keyword: '' },
      dialogVisible: false,
      submitLoading: false,
      exportLoading: false,
      dialogType: 'create',
      sponsorOptions: [],
      form: this.getEmptyForm(),
      rules: {
        name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
        gender: [{ required: true, message: '请选择性别', trigger: 'change' }],
        idCard: [{ required: true, message: '请输入身份证号', trigger: 'blur' }],
        school: [{ required: true, message: '请输入学校', trigger: 'blur' }],
        grade: [{ required: true, message: '请输入年级', trigger: 'blur' }],
        sponsorId: [{ required: true, message: '请选择资助人', trigger: 'change' }]
      }
    }
  },
  computed: {
    isAdmin() {
      return this.$store.getters.role === 'AUDIT_ADMIN'
    },
    dialogTitle() {
      return this.dialogType === 'create' ? '新增学生' : '编辑学生'
    }
  },
  created() {
    this.getList()
    this.loadSponsors()
  },
  methods: {
    getEmptyForm() {
      return {
        id: null,
        name: '',
        gender: '',
        idCard: '',
        school: '',
        grade: '',
        familyAddress: '',
        familySituation: '',
        phone: '',
        sponsorId: undefined,
        sponsorName: ''
      }
    },
    getList() {
      this.listLoading = true
      listStudents(this.listQuery).then(data => {
        this.list = (data && data.records) || []
        this.total = (data && data.total) || 0
      }).finally(() => {
        this.listLoading = false
      })
    },
    loadSponsors() {
      userOptions('SPONSOR').then(data => {
        this.sponsorOptions = data || []
      }).catch(() => {})
    },
    handleFilter() {
      this.listQuery.page = 1
      this.getList()
    },
    handleReset() {
      this.listQuery.keyword = ''
      this.listQuery.page = 1
      this.getList()
    },
    handleSizeChange(size) {
      this.listQuery.size = size
      this.listQuery.page = 1
      this.getList()
    },
    handleCurrentChange(page) {
      this.listQuery.page = page
      this.getList()
    },
    handleCreate() {
      this.dialogType = 'create'
      this.form = this.getEmptyForm()
      this.dialogVisible = true
      this.$nextTick(() => {
        this.$refs.dataForm && this.$refs.dataForm.clearValidate()
      })
    },
    handleUpdate(row) {
      this.dialogType = 'update'
      getStudent(row.id).then(data => {
        this.form = Object.assign(this.getEmptyForm(), data || {})
        this.dialogVisible = true
        this.$nextTick(() => {
          this.$refs.dataForm && this.$refs.dataForm.clearValidate()
        })
      })
    },
    onSponsorChange(val) {
      const target = this.sponsorOptions.find(s => s.id === val)
      this.form.sponsorName = target ? target.realName : ''
    },
    submitForm() {
      this.$refs.dataForm.validate(valid => {
        if (!valid) return
        this.submitLoading = true
        const payload = Object.assign({}, this.form)
        const action = this.dialogType === 'create'
          ? addStudent(payload)
          : updateStudent(payload.id, payload)
        action.then(() => {
          this.$message.success(this.dialogType === 'create' ? '新增成功' : '修改成功')
          this.dialogVisible = false
          this.getList()
        }).finally(() => {
          this.submitLoading = false
        })
      })
    },
    handleDelete(row) {
      this.$confirm(`确认删除学生「${row.name}」吗？`, '提示', {
        type: 'warning'
      }).then(() => {
        deleteStudent(row.id).then(() => {
          this.$message.success('删除成功')
          if (this.list.length === 1 && this.listQuery.page > 1) {
            this.listQuery.page--
          }
          this.getList()
        })
      }).catch(() => {})
    },
    handleExport() {
      this.exportLoading = true
      exportStudents().then(blob => {
        return downloadFile(blob, '学生列表.xlsx')
      }).then(() => {
        this.$message.success('导出成功')
      }).catch(err => {
        if (err && err.message) this.$message.error(err.message)
      }).finally(() => {
        this.exportLoading = false
      })
    }
  }
}
</script>

<style scoped>
.page-container {
  background: #fff;
  padding: 16px;
  border-radius: 4px;
}
.filter-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
}
.filter-actions {
  margin-left: auto;
  display: flex;
  gap: 8px;
}
.pagination-bar {
  margin-top: 16px;
  text-align: right;
}
.danger-text {
  color: #F56C6C;
}
</style>
