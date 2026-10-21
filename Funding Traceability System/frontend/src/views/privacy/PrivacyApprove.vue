<template>
  <div class="page-container">
    <el-tabs v-model="activeTab" @tab-click="handleTabClick">
      <el-tab-pane label="我的申请" name="mine">
        <el-table v-loading="mineLoading" :data="mineList" border stripe style="width:100%">
          <el-table-column prop="id" label="申请单号" width="90" align="center" />
          <el-table-column prop="virtualId" label="虚拟ID" min-width="110" />
          <el-table-column prop="fieldScope" label="字段范围" min-width="160" show-overflow-tooltip>
            <template slot-scope="{ row }">{{ scopeText(row.fieldScope) }}</template>
          </el-table-column>
          <el-table-column prop="reason" label="理由" min-width="160" show-overflow-tooltip />
          <el-table-column label="状态" width="100" align="center">
            <template slot-scope="{ row }">
              <el-tag :type="statusTagType(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="expireTime" label="有效截止时间" min-width="150">
            <template slot-scope="{ row }">{{ row.expireTime || '—' }}</template>
          </el-table-column>
          <el-table-column prop="createTime" label="申请时间" min-width="150" />
        </el-table>
      </el-tab-pane>

      <el-tab-pane v-if="role === 'AUDIT_ADMIN'" name="todoL1">
        <span slot="label">
          初审待办
          <el-badge v-if="todoList.length" :value="todoList.length" :max="99" class="tab-badge" />
        </span>
        <el-table v-loading="todoLoading" :data="todoList" border stripe style="width:100%">
          <el-table-column prop="id" label="申请单号" width="90" align="center" />
          <el-table-column prop="applicantName" label="申请人" min-width="100" />
          <el-table-column prop="virtualId" label="虚拟ID" min-width="110" />
          <el-table-column prop="fieldScope" label="字段范围" min-width="150" show-overflow-tooltip>
            <template slot-scope="{ row }">{{ scopeText(row.fieldScope) }}</template>
          </el-table-column>
          <el-table-column prop="reason" label="理由" min-width="150" show-overflow-tooltip />
          <el-table-column prop="createTime" label="申请时间" min-width="150" />
          <el-table-column label="操作" width="140" align="center" fixed="right">
            <template slot-scope="{ row }">
              <div class="op-btns">
                <el-button type="success" size="mini" @click="openApprove(row, 'APPROVE')">通过</el-button>
                <el-button type="danger" size="mini" @click="openApprove(row, 'REJECT')">驳回</el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane v-if="role === 'SUPERVISOR'" name="todoL2">
        <span slot="label">
          终审待办
          <el-badge v-if="todoList.length" :value="todoList.length" :max="99" class="tab-badge" />
        </span>
        <el-table v-loading="todoLoading" :data="todoList" border stripe style="width:100%">
          <el-table-column prop="id" label="申请单号" width="90" align="center" />
          <el-table-column prop="applicantName" label="申请人" min-width="100" />
          <el-table-column prop="virtualId" label="虚拟ID" min-width="110" />
          <el-table-column prop="fieldScope" label="字段范围" min-width="150" show-overflow-tooltip>
            <template slot-scope="{ row }">{{ scopeText(row.fieldScope) }}</template>
          </el-table-column>
          <el-table-column prop="reason" label="理由" min-width="150" show-overflow-tooltip />
          <el-table-column prop="l1Remark" label="初审意见" min-width="120" show-overflow-tooltip>
            <template slot-scope="{ row }">{{ row.l1Remark || '—' }}</template>
          </el-table-column>
          <el-table-column prop="createTime" label="申请时间" min-width="150" />
          <el-table-column label="操作" width="140" align="center" fixed="right">
            <template slot-scope="{ row }">
              <div class="op-btns">
                <el-button type="success" size="mini" @click="openApprove(row, 'APPROVE')">通过</el-button>
                <el-button type="danger" size="mini" @click="openApprove(row, 'REJECT')">驳回</el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <!-- 审批意见弹窗 -->
    <el-dialog :title="approveTitle" :visible.sync="approveVisible" width="480px" :close-on-click-modal="false">
      <el-form label-width="90px">
        <el-form-item label="审批意见">
          <el-input v-model="approveRemark" type="textarea" :rows="3" placeholder="请填写审批意见" />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="approveVisible = false">取 消</el-button>
        <el-button :type="approveResult === 'APPROVE' ? 'success' : 'danger'" :loading="approveLoading" @click="submitApprove">确 定</el-button>
      </div>
    </el-dialog>

    <ComplianceFooter />
  </div>
</template>

<script>
import { listMyApplies, listTodoApplies, approvePrivacyAccess } from '@/api/privacy'
import ComplianceFooter from '@/components/ComplianceFooter.vue'

const FIELD_NAMES = {
  name: '姓名',
  idCard: '身份证号',
  phone: '电话',
  familyAddress: '家庭住址',
  familySituation: '家庭情况'
}

export default {
  name: 'PrivacyApprove',
  components: { ComplianceFooter },
  data() {
    return {
      activeTab: 'mine',
      mineList: [],
      mineLoading: false,
      todoList: [],
      todoLoading: false,
      approveVisible: false,
      approveLoading: false,
      approveResult: 'APPROVE',
      approveRemark: '',
      currentRow: null
    }
  },
  computed: {
    role() {
      return this.$store.getters.role
    },
    approveTitle() {
      return this.approveResult === 'APPROVE' ? '通过申请' : '驳回申请'
    }
  },
  created() {
    this.loadMine()
    // 预载待办数量用于 Tab 红色未读角标
    if (this.role === 'AUDIT_ADMIN' || this.role === 'SUPERVISOR') {
      this.loadTodo()
    }
  },
  methods: {
    scopeText(scope) {
      if (!scope) return '—'
      return scope.split(',').map(s => FIELD_NAMES[s.trim()] || s).join('、')
    },
    statusText(status) {
      const map = { PENDING_L1: '待初审', PENDING_L2: '待终审', APPROVED: '已通过', REJECTED: '已驳回' }
      return map[status] || status || '-'
    },
    statusTagType(status) {
      const map = { PENDING_L1: 'info', PENDING_L2: 'warning', APPROVED: 'success', REJECTED: 'danger' }
      return map[status] || 'info'
    },
    loadMine() {
      this.mineLoading = true
      listMyApplies().then(data => {
        this.mineList = data || []
      }).finally(() => {
        this.mineLoading = false
      })
    },
    loadTodo() {
      this.todoLoading = true
      listTodoApplies().then(data => {
        this.todoList = data || []
      }).finally(() => {
        this.todoLoading = false
      })
    },
    handleTabClick(tab) {
      if (tab.name === 'mine') {
        this.loadMine()
      } else {
        this.loadTodo()
      }
    },
    openApprove(row, result) {
      this.currentRow = row
      this.approveResult = result
      this.approveRemark = ''
      this.approveVisible = true
    },
    submitApprove() {
      this.approveLoading = true
      approvePrivacyAccess(this.currentRow.id, { result: this.approveResult, remark: this.approveRemark }).then(() => {
        this.$message.success(this.approveResult === 'APPROVE' ? '已通过' : '已驳回')
        this.approveVisible = false
        this.loadTodo()
      }).finally(() => {
        this.approveLoading = false
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

/* 操作列按钮：水平居中、等距、不换行 */
.op-btns {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 8px;
  white-space: nowrap;
}
.op-btns .el-button + .el-button {
  margin-left: 0;
}

/* Tab 未读角标位置微调 */
.tab-badge {
  margin-left: 4px;
  vertical-align: 2px;
}
.tab-badge ::v-deep .el-badge__content {
  position: static;
  transform: none;
  border: none;
}
</style>
