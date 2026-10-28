<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <div class="page-title">用户管理</div>
        <div class="page-subtitle">账号、角色与联系方式明文调取统一在此维护，调取须两级审批</div>
      </div>
      <div class="page-actions">
        <el-button v-permission="['AUDIT_ADMIN']" type="primary" icon="el-icon-plus" @click="handleCreate">新增用户</el-button>
      </div>
    </div>

    <div class="kpi-row">
      <div class="kpi-card">
        <div class="kpi-label">用户总数</div>
        <div class="kpi-value">{{ total }}</div>
        <div class="kpi-hint">个系统账号</div>
      </div>
      <div class="kpi-card">
        <div class="kpi-label">本页账号</div>
        <div class="kpi-value">{{ list.length }}</div>
        <div class="kpi-hint">当前页展示条数</div>
      </div>
      <div class="kpi-card kpi-card--success">
        <div class="kpi-label">本页角色种类</div>
        <div class="kpi-value">{{ roleCount }}</div>
        <div class="kpi-hint">种角色分布</div>
      </div>
    </div>

    <div class="toolbar">
      <el-input v-model="listQuery.keyword" placeholder="用户名 / 姓名" clearable style="width:220px" @keyup.enter.native="handleFilter" />
      <el-button type="primary" icon="el-icon-search" @click="handleFilter">查询</el-button>
      <el-button icon="el-icon-refresh" @click="handleReset">重置</el-button>
    </div>

    <el-table v-loading="listLoading" :data="list" border stripe style="width:100%">
      <el-table-column type="index" label="序号" width="55" align="center" />
      <el-table-column prop="username" label="用户名" min-width="120" />
      <el-table-column prop="realName" label="姓名" min-width="100" />
      <el-table-column label="角色" min-width="110" align="center">
        <template slot-scope="{ row }">
          <el-tag :type="roleTagType(row.role)" :class="roleTagClass(row.role)" size="small">
            {{ roleText(row.role) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="phone" label="联系电话" min-width="120">
        <template slot-scope="{ row }">
          <span class="mono">{{ row.phone }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" min-width="150" />
      <el-table-column v-if="isAuditAdmin" label="操作" width="265" align="center" class-name="op-col">
        <template slot-scope="{ row }">
          <el-button type="text" class="link-secondary" icon="el-icon-key" @click="openPrivacy(row)">联系方式调取</el-button>
          <el-button type="text" icon="el-icon-edit" @click="handleUpdate(row)">编辑</el-button>
          <el-button type="text" class="danger-text" icon="el-icon-delete" :disabled="row.id === currentUserId" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
      <template slot="empty">
        <EmptyState type="folder" title="暂无用户" desc="新增用户后，可在这里统一管理账号角色与权限" />
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
          <el-input v-model="form.phone" :placeholder="dialogType === 'update' ? '打码值保持不变则不修改' : '请输入联系电话'" />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="submitForm">确 定</el-button>
      </div>
    </el-dialog>

    <!-- 联系方式明文调取弹窗 -->
    <el-dialog title="联系方式明文调取" :visible.sync="privacyVisible" width="560px" :close-on-click-modal="false">
      <el-descriptions :column="2" border size="small">
        <el-descriptions-item label="姓名">{{ privacyRow.realName }}</el-descriptions-item>
        <el-descriptions-item label="电话(脱敏)">{{ privacyRow.phone }}</el-descriptions-item>
      </el-descriptions>

      <el-form label-width="90px" style="margin-top:16px">
        <el-form-item label="调取理由" required>
          <el-input v-model="privacyReason" type="textarea" :rows="3" placeholder="请填写调取理由（必填）" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="applyLoading" @click="submitApply">提交申请</el-button>
          <span class="apply-tip">明文调取须经初审、终审双人两级审批</span>
        </el-form-item>
      </el-form>

      <el-divider />

      <div>
        <el-button type="warning" icon="el-icon-view" :loading="profileLoading" @click="viewProfile">查看明文</el-button>
        <div v-if="profile" style="margin-top:12px">
          <el-alert
            v-if="profile.masked"
            type="info"
            :closable="false"
            title="暂无有效审批单，请提交申请并等待两级审批"
            show-icon
          />
          <template v-else>
            <el-alert
              type="success"
              :closable="false"
              :title="'审批单 #' + profile.applyId + '，明文可见至 ' + profile.expireTime"
              show-icon
              style="margin-bottom:12px"
            />
            <el-descriptions :column="1" border size="small">
              <el-descriptions-item label="姓名">{{ profile.realName }}</el-descriptions-item>
              <el-descriptions-item label="联系电话">{{ profile.sponsorPhone }}</el-descriptions-item>
            </el-descriptions>
          </template>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listUsers, addUser, updateUser, deleteUser } from '@/api/user'
import { applyPrivacyAccess, getPrivacyProfile } from '@/api/privacy'
import EmptyState from '@/components/EmptyState.vue'

export default {
  name: 'User',
  components: { EmptyState },
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
      // 联系方式明文调取
      privacyVisible: false,
      privacyRow: {},
      privacyReason: '',
      applyLoading: false,
      profileLoading: false,
      profile: null,
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
    dialogTitle() { return this.dialogType === 'create' ? '新增用户' : '编辑用户' },
    // 仅用于顶部统计展示，不改变任何列表逻辑
    roleCount() {
      return new Set(this.list.map(item => item.role).filter(Boolean)).size
    }
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
    roleTagClass(role) {
      const map = {
        SUPERVISOR: 'role-tag--supervisor',
        DISBURSE_ADMIN: 'role-tag--disburse',
        SPONSOR: 'role-tag--sponsor',
        AUDIT_ADMIN: 'role-tag--audit'
      }
      return map[role] || ''
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
    },
    // 联系方式明文调取
    openPrivacy(row) {
      this.privacyRow = row
      this.privacyReason = ''
      this.profile = null
      this.privacyVisible = true
    },
    submitApply() {
      if (!this.privacyReason.trim()) {
        this.$message.warning('请填写调取理由')
        return
      }
      this.applyLoading = true
      applyPrivacyAccess({
        virtualId: 'USER-' + this.privacyRow.id,
        fieldScope: 'sponsorPhone',
        reason: this.privacyReason.trim()
      }).then(() => {
        this.$message.success('申请已提交，待两级审批')
        this.privacyReason = ''
      }).finally(() => { this.applyLoading = false })
    },
    viewProfile() {
      this.profileLoading = true
      getPrivacyProfile('USER-' + this.privacyRow.id).then(data => {
        this.profile = data
      }).finally(() => { this.profileLoading = false })
    }
  }
}
</script>

<style scoped>
.pagination-bar { margin-top:16px; text-align:right; }
.danger-text { color: var(--c-danger) !important; }
.danger-text:hover,
.danger-text:focus { color: var(--c-danger) !important; background: var(--c-danger-light) !important; }
/* 次级文字按钮：联系方式调取 */
.el-button--text.link-secondary { color: var(--c-text-3) !important; }
.el-button--text.link-secondary:hover,
.el-button--text.link-secondary:focus { color: var(--c-primary) !important; background: var(--c-primary-light); }
/* 角色标签配色：监事蓝 / 拨付橙 / 资助绿 / 审核红 */
.el-tag.role-tag--supervisor {
  background: #eaf2fb;
  border-color: #cfe0f3;
  color: #2b6cb0;
}
.el-tag.role-tag--disburse {
  background: #fcf4e8;
  border-color: #f2ddbe;
  color: #c07f2e;
}
.el-tag.role-tag--sponsor {
  background: #eaf6f0;
  border-color: #c8e8d8;
  color: #2f9e6e;
}
.el-tag.role-tag--audit {
  background: #fbedec;
  border-color: #f0cfcd;
  color: #c24a45;
}
/* 桌面端操作列按钮单行排列 */
@media (min-width: 769px) {
  ::v-deep .op-col .cell { white-space: nowrap; }
}
</style>
