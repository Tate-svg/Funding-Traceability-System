<template>
  <div class="page-container">
    <div class="filter-bar">
      <el-input v-model="listQuery.keyword" placeholder="用户名 / 姓名" clearable style="width:220px" @keyup.enter.native="handleFilter" />
      <el-button type="primary" icon="el-icon-search" @click="handleFilter">查询</el-button>
      <el-button icon="el-icon-refresh" @click="handleReset">重置</el-button>
      <div class="filter-actions">
        <el-button v-permission="['AUDIT_ADMIN']" type="success" icon="el-icon-plus" @click="handleCreate">新增用户</el-button>
      </div>
    </div>

    <el-table v-loading="listLoading" :data="list" border stripe style="width:100%">
      <el-table-column type="index" label="序号" width="55" align="center" />
      <el-table-column prop="username" label="用户名" min-width="120" />
      <el-table-column prop="realName" label="姓名" min-width="100" />
      <el-table-column label="角色" min-width="110" align="center">
        <template slot-scope="{ row }">
          <el-tag :type="roleTagType(row.role)" size="small">
            {{ roleText(row.role) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="phone" label="联系电话" min-width="120" />
      <el-table-column prop="createTime" label="创建时间" min-width="150" />
      <el-table-column v-if="isAuditAdmin" label="操作" width="150" align="center">
        <template slot-scope="{ row }">
          <el-button type="text" icon="el-icon-edit" @click="handleUpdate(row)">编辑</el-button>
          <el-button type="text" class="danger-text" icon="el-icon-delete" :disabled="row.id === currentUserId" @click="handleDelete(row)">删除</el-button>
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

    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="520px" :close-on-click-modal="false">
      <el-form ref="dataForm" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" :disabled="dialogType === 'update'" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password :placeholder="dialogType === 'update' ? '留空则不修改密码' : '请输入密码'" />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="form.realName" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="form.role" placeholder="请选择角色" style="width:100%">
            <el-option label="审核管理员" value="AUDIT_ADMIN" />
            <el-option label="拨付管理员" value="DISBURSE_ADMIN" />
            <el-option label="监事/审计" value="SUPERVISOR" />
            <el-option label="资助人" value="SPONSOR" />
          </el-select>
        </el-form-item>
        <el-form-item label="联系电话" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入联系电话" />
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
import { listUsers, addUser, updateUser, deleteUser } from '@/api/user'

export default {
  name: 'User',
  data() {
    const validatePassword = (rule, value, callback) => {
      if (this.dialogType === 'create' && !value) {
        callback(new Error('请输入密码'))
      } else {
        callback()
      }
    }
    return {
      list: [],
      total: 0,
      listLoading: false,
      listQuery: { page: 1, size: 10, keyword: '' },
      dialogVisible: false,
      submitLoading: false,
      dialogType: 'create',
      form: this.getEmptyForm(),
      rules: {
        username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
        password: [{ validator: validatePassword, trigger: 'blur' }],
        realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
        role: [{ required: true, message: '请选择角色', trigger: 'change' }]
      }
    }
  },
  computed: {
    isAuditAdmin() { return this.$store.getters.role === 'AUDIT_ADMIN' },
    currentUserId() { return this.$store.getters.userId },
    dialogTitle() { return this.dialogType === 'create' ? '新增用户' : '编辑用户' }
  },
  created() {
    this.getList()
  },
  methods: {
    getEmptyForm() {
      return { id: null, username: '', password: '', realName: '', role: '', phone: '' }
    },
    roleText(role) {
      const map = {
        AUDIT_ADMIN: '审核管理员',
        DISBURSE_ADMIN: '拨付管理员',
        SUPERVISOR: '监事/审计',
        SPONSOR: '资助人'
      }
      return map[role] || role || '-'
    },
    roleTagType(role) {
      const map = {
        AUDIT_ADMIN: 'danger',
        DISBURSE_ADMIN: 'warning',
        SUPERVISOR: 'primary',
        SPONSOR: 'success'
      }
      return map[role] || 'info'
    },
    getList() {
      this.listLoading = true
      listUsers(this.listQuery).then(data => {
        this.list = (data && data.records) || []
        this.total = (data && data.total) || 0
      }).finally(() => { this.listLoading = false })
    },
    handleFilter() { this.listQuery.page = 1; this.getList() },
    handleReset() { this.listQuery.keyword = ''; this.listQuery.page = 1; this.getList() },
    handleSizeChange(size) { this.listQuery.size = size; this.listQuery.page = 1; this.getList() },
    handleCurrentChange(page) { this.listQuery.page = page; this.getList() },
    handleCreate() {
      this.dialogType = 'create'
      this.form = this.getEmptyForm()
      this.dialogVisible = true
      this.$nextTick(() => { this.$refs.dataForm && this.$refs.dataForm.clearValidate() })
    },
    handleUpdate(row) {
      this.dialogType = 'update'
      this.form = Object.assign(this.getEmptyForm(), row, { password: '' })
      this.dialogVisible = true
      this.$nextTick(() => { this.$refs.dataForm && this.$refs.dataForm.clearValidate() })
    },
    submitForm() {
      this.$refs.dataForm.validate(valid => {
        if (!valid) return
        this.submitLoading = true
        const payload = {
          username: this.form.username,
          realName: this.form.realName,
          role: this.form.role,
          phone: this.form.phone
        }
        // 密码为空时不传（编辑场景保持原密码）
        if (this.form.password) {
          payload.password = this.form.password
        }
        const action = this.dialogType === 'create' ? addUser(payload) : updateUser(this.form.id, payload)
        action.then(() => {
          this.$message.success(this.dialogType === 'create' ? '新增成功' : '修改成功')
          this.dialogVisible = false
          this.getList()
        }).finally(() => { this.submitLoading = false })
      })
    },
    handleDelete(row) {
      if (row.id === this.currentUserId) {
        this.$message.warning('不能删除当前登录用户')
        return
      }
      this.$confirm(`确认删除用户「${row.username}」吗？`, '提示', { type: 'warning' }).then(() => {
        deleteUser(row.id).then(() => {
          this.$message.success('删除成功')
          if (this.list.length === 1 && this.listQuery.page > 1) this.listQuery.page--
          this.getList()
        })
      }).catch(() => {})
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
