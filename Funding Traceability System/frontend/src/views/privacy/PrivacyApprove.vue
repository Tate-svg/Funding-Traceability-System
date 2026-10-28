<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <div class="page-title">隐私调取审批</div>
        <div class="page-subtitle">敏感字段调取需分级审批，全程留痕，到期自动失效</div>
      </div>
      <div class="page-actions">
        <span class="grade-pill"><i class="el-icon-lock"></i> 分级审批 · 全程留痕</span>
      </div>
    </div>

    <div class="kpi-row">
      <div class="kpi-card">
        <div class="kpi-label">我的申请</div>
        <div class="kpi-value">{{ mineList.length }}</div>
        <div class="kpi-hint">条隐私调取申请</div>
      </div>
      <div class="kpi-card kpi-card--success">
        <div class="kpi-label">已通过</div>
        <div class="kpi-value">{{ approvedCount }}</div>
        <div class="kpi-hint">条申请已获授权</div>
      </div>
      <div v-if="role === 'AUDIT_ADMIN' || role === 'SUPERVISOR'" class="kpi-card kpi-card--warn">
        <div class="kpi-label">待我审批</div>
        <div class="kpi-value">{{ todoList.length }}</div>
        <div class="kpi-hint">{{ role === 'SUPERVISOR' ? '终审待办' : '初审待办' }}</div>
      </div>
    </div>

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
              <el-tag :type="statusTagType(row.status)" :class="statusTagClass(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="expireTime" label="有效截止时间" min-width="150">
            <template slot-scope="{ row }">{{ row.expireTime || '—' }}</template>
          </el-table-column>
          <el-table-column prop="createTime" label="申请时间" min-width="150" />
          <el-table-column label="操作" width="120" align="center">
            <template slot-scope="scope">
              <div class="op-btns">
                <el-button type="primary" size="small" @click="openMaskDetail($store.state.user.adminId, scope.row.id)">查看敏感详情</el-button>
              </div>
            </template>
          </el-table-column>
          <template slot="empty">
            <EmptyState type="todo" title="暂无隐私调取申请" desc="提交隐私调取申请后，审批进度会在这里展示" />
          </template>
          <el-dialog v-model="maskDialogVisible" title="敏感详情" width="600px" :close-on-click-modal="false">
            <div>
              <el-switch v-model="maskToggle" @change="handleMaskToggle" active-text="开启二次遮挡" inactive-text="关闭二次遮挡"/>
              <p v-if="countdown > 0">会话剩余 {{countdown}} 分钟，超时自动关闭</p >
              <p>敏感数据：{{sensitiveData}}</p >
            </div>
            <template #footer>
              <el-button @click="resetCountdown">延长会话</el-button>
              <el-button @click="maskDialogVisible = false">关闭</el-button>
            </template>
          </el-dialog>
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
                <el-button type="primary" size="mini" @click="openApprove(row, 'APPROVE')">通过</el-button>
                <el-button type="danger" size="mini" @click="openApprove(row, 'REJECT')">驳回</el-button>
              </div>
            </template>
          </el-table-column>
          <template slot="empty">
            <EmptyState type="todo" title="暂无待审批申请" desc="收到新的隐私调取申请后会出现在这里" />
          </template>
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
                <el-button type="primary" size="mini" @click="openApprove(row, 'APPROVE')">通过</el-button>
                <el-button type="danger" size="mini" @click="openApprove(row, 'REJECT')">驳回</el-button>
              </div>
            </template>
          </el-table-column>
          <template slot="empty">
            <EmptyState type="todo" title="暂无待审批申请" desc="初审通过的申请会流转到这里等待终审" />
          </template>
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
        <el-button :type="approveResult === 'APPROVE' ? 'primary' : 'danger'" :loading="approveLoading" @click="submitApprove">确 定</el-button>
      </div>
    </el-dialog>

    <div class="privacy-footer">
      <ComplianceFooter />
    </div>
  </div>
  <!-- ✅ 新增：脱敏详情弹窗 -->
  <el-dialog v-model="showMaskModal" title="脱敏详情">
    <div style="margin-bottom:12px;">
      <span>二次隐私遮挡：</span>
      <el-switch
        v-model="isSecondMask"
        @change="toggleMaskSwitch"
        active-text="开启"
        inactive-text="关闭"
      />
    </div>
    <div>银行卡号：{{ sensitiveInfo.bankCard }}</div>
    <div>家庭住址：{{ sensitiveInfo.address }}</div>
  </el-dialog>
</template>

<script>
import { listMyApplies, listTodoApplies, approvePrivacyAccess } from '@/api/privacy'
import ComplianceFooter from '@/components/ComplianceFooter.vue'
import EmptyState from '@/components/EmptyState.vue'

const FIELD_NAMES = {
  name: '姓名',
  idCard: '身份证号',
  phone: '电话',
  familyAddress: '家庭住址',
  familySituation: '家庭情况'
}

export default {
  name: 'PrivacyApprove',
  components: { ComplianceFooter, EmptyState },
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
      showMaskModal: false,
      isSecondMask: true,
      sessionKey: "",
      sensitiveInfo: {},
      timerId: null,
    }
  },
  computed: {
    role() {
      return this.$store.getters.role
    },
    // 仅用于顶部统计展示，不改变任何审批逻辑
    approvedCount() {
      return this.mineList.filter(item => item.status === 'APPROVED').length
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
    statusTagClass(status) {
      const map = {
        PENDING_L1: 'status-tag--l1',
        PENDING_L2: 'status-tag--l2',
        APPROVED: 'status-tag--approved',
        REJECTED: 'status-tag--rejected'
      }
      return map[status] || ''
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
    async openMaskDetail(adminId, targetId){
      this.showMaskModal = true;
      const resSession = await this.$axios.post("/api/privacy/mask/createViewSession",{
        adminId: adminId,
        targetId: targetId
      })
      this.sessionKey = resSession.data.sessionKey

      const resStatus = await this.$axios.get("/api/privacy/mask/getMaskStatus",{
        params:{ adminId: adminId }
      })
      this.isSecondMask = resStatus.data.isSecondMask

      await this.fetchSensitiveDetail()
      this.startAutoCloseTimer()
    },
    async fetchSensitiveDetail(){
      const res = await this.$axios.get("/api/privacy/mask/getSensitiveDetail",{
        params:{
          sessionKey: this.sessionKey,
          adminId: adminId,
          targetId: targetId
        }
      })
      this.sensitiveInfo = res.data
    },
    async toggleMaskSwitch(val){
      await this.$axios.post("/api/privacy/mask/switchMask",{
        adminId: adminId,
        isSecondMask: val
      })
      await this.fetchSensitiveDetail()
    },
    startAutoCloseTimer(){
      if(this.timerId){
        clearTimeout(this.timerId)
      }
      this.timerId = setTimeout(async ()=>{
        alert("查看会话超时，敏感信息已自动收起！")
        this.closeMaskModal()
      }, 15 * 60 * 1000)
    },
    async closeMaskModal(){
      this.showMaskModal = false
      if(this.timerId){
        clearTimeout(this.timerId)
        this.timerId = null
      }
      await this.$axios.post("/api/privacy/mask/closeSession",{
        sessionKey: this.sessionKey
      })
    }

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
/* 页头右侧合规标识 */
.grade-pill {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  height: 26px;
  padding: 0 12px;
  border-radius: var(--r-pill);
  background: var(--c-primary-light);
  border: 1px solid var(--c-primary-border);
  color: var(--c-primary);
  font-size: 12px;
  font-weight: 600;
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

/* 状态标签：待初审蓝描边 / 待终审橙 / 已通过绿填充 / 已驳回红 */
.el-tag.status-tag--l1 {
  background: transparent;
  border-color: var(--c-primary-border);
  color: var(--c-primary);
}
.el-tag.status-tag--l2 {
  background: var(--c-warning-light);
  border-color: #f2ddbe;
  color: var(--c-warning);
}
.el-tag.status-tag--approved {
  background: var(--c-success);
  border-color: var(--c-success);
  color: #fff;
}
.el-tag.status-tag--rejected {
  background: var(--c-danger-light);
  border-color: #f0cfcd;
  color: var(--c-danger);
}

/* 底部隐私提示：浅灰提示条 */
.privacy-footer {
  margin-top: 16px;
  padding: 10px 12px 10px 14px;
  background: #f7f9fc;
  border: 1px solid var(--c-border-light);
  border-left: 3px solid #dbe3ec;
  border-radius: 4px;
}
.privacy-footer ::v-deep .compliance-footer {
  margin-top: 0;
  text-align: left;
  color: var(--c-text-3);
  font-size: 12px;
  line-height: 1.6;
}
</style>
