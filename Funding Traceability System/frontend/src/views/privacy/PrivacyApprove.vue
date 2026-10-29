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

    <!-- 二次遮挡开关：对已获取的敏感字段再遮一层 -->
    <div class="mask-config">
      <div class="mask-config-main">
        <span class="mask-config-icon"><i class="el-icon-view"></i></span>
        <div class="mask-config-text">
          <div class="mask-config-title">敏感字段二次遮挡</div>
          <div class="mask-config-desc">开启后，查看详情时电话与家庭住址会再遮挡一层；关闭则按审批授权范围展示</div>
        </div>
      </div>
      <el-switch
        v-model="secondMask"
        :loading="maskSwitching"
        active-text="已开启"
        inactive-text="已关闭"
        @change="handleMaskSwitch"
      />
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
          <el-table-column label="操作" width="110" align="center" fixed="right">
            <template slot-scope="{ row }">
              <el-button type="text" icon="el-icon-view" @click="openMaskDetail(row)">查看详情</el-button>
            </template>
          </el-table-column>
          <template slot="empty">
            <EmptyState type="todo" title="暂无隐私调取申请" desc="提交隐私调取申请后，审批进度会在这里展示" />
          </template>
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
          <el-table-column label="操作" width="196" align="center" fixed="right">
            <template slot-scope="{ row }">
              <div class="op-btns">
                <el-button type="text" size="mini" icon="el-icon-view" @click="openMaskDetail(row)">详情</el-button>
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
          <el-table-column label="操作" width="196" align="center" fixed="right">
            <template slot-scope="{ row }">
              <div class="op-btns">
                <el-button type="text" size="mini" icon="el-icon-view" @click="openMaskDetail(row)">详情</el-button>
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

    <!-- 敏感详情弹窗：限时会话 + 二次遮挡 -->
    <el-dialog
      title="敏感详情"
      :visible.sync="maskVisible"
      width="560px"
      :close-on-click-modal="false"
      @closed="handleMaskClosed"
    >
      <div v-loading="maskLoading" class="mask-dialog">
        <div class="mask-session">
          <span class="mask-chip"><i class="el-icon-time"></i> 会话剩余 {{ countdownText }}</span>
          <span class="mask-chip" :class="detail.masked ? 'mask-chip--masked' : 'mask-chip--plain'">
            <i :class="detail.masked ? 'el-icon-lock' : 'el-icon-unlock'"></i>
            {{ detail.masked ? '脱敏视图' : '授权明文' }}
          </span>
          <span class="mask-chip mask-chip--ghost">{{ detail.secondMask ? '二次遮挡已开启' : '二次遮挡已关闭' }}</span>
        </div>

        <ul v-if="detailFields.length" class="mask-fields">
          <li v-for="f in detailFields" :key="f.key" class="mask-field">
            <span class="mask-label">{{ f.label }}</span>
            <span class="mask-value" :class="{ 'is-mono': f.mono }">{{ f.value }}</span>
          </li>
        </ul>
        <p v-else-if="!maskLoading" class="mask-empty">当前申请单暂无可展示字段</p>

        <p v-if="detail.masked" class="mask-tip">
          <i class="el-icon-info"></i>
          未获得终审明文授权时仅展示脱敏档案；如需明文请提交隐私调取申请并完成两级审批。
        </p>
        <p class="mask-foot-tip">
          <i class="el-icon-warning-outline"></i>
          会话到期将自动关闭，每次查看均写入隐私调取日志。
        </p>
      </div>
      <div slot="footer">
        <el-button @click="closeMaskDialog">关 闭</el-button>
      </div>
    </el-dialog>

    <div class="privacy-footer">
      <ComplianceFooter />
    </div>
  </div>
</template>

<script>
import {
  listMyApplies,
  listTodoApplies,
  approvePrivacyAccess,
  getMaskStatus,
  switchMask,
  createViewSession,
  getSensitiveDetail,
  closeViewSession
} from '@/api/privacy'
import ComplianceFooter from '@/components/ComplianceFooter.vue'
import EmptyState from '@/components/EmptyState.vue'

const FIELD_NAMES = {
  name: '姓名',
  idCard: '身份证号',
  phone: '电话',
  familyAddress: '家庭住址',
  familySituation: '家庭情况'
}

// 敏感详情展示字段（按后端返回的键位映射）
const DETAIL_FIELDS = [
  { key: 'name', label: '姓名' },
  { key: 'idCard', label: '身份证号', mono: true },
  { key: 'phone', label: '电话', mono: true },
  { key: 'familyAddress', label: '家庭住址' },
  { key: 'familySituation', label: '家庭情况' },
  { key: 'realName', label: '资助人姓名' },
  { key: 'sponsorPhone', label: '资助人电话', mono: true },
  { key: 'maskName', label: '学生（脱敏姓名）' },
  { key: 'school', label: '学校' },
  { key: 'grade', label: '年级' },
  { key: 'gender', label: '性别' }
]

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
      currentRow: null,
      // 二次遮挡
      secondMask: false,
      maskSwitching: false,
      // 敏感详情会话
      maskVisible: false,
      maskLoading: false,
      sessionKey: '',
      remainingSeconds: 0,
      countdownTimer: null,
      detail: {}
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
    },
    countdownText() {
      const total = Math.max(this.remainingSeconds, 0)
      const m = String(Math.floor(total / 60)).padStart(2, '0')
      const s = String(total % 60).padStart(2, '0')
      return m + ':' + s
    },
    detailFields() {
      return DETAIL_FIELDS.filter(f => {
        const v = this.detail[f.key]
        return v !== undefined && v !== null && String(v).trim() !== ''
      }).map(f => ({ ...f, value: this.detail[f.key] }))
    }
  },
  created() {
    this.loadMine()
    this.loadMaskStatus()
    // 预载待办数量用于 Tab 红色未读角标
    if (this.role === 'AUDIT_ADMIN' || this.role === 'SUPERVISOR') {
      this.loadTodo()
    }
  },
  beforeDestroy() {
    this.stopCountdown()
    this.releaseSession()
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
    },
    /* ---------- 二次遮挡开关 ---------- */
    loadMaskStatus() {
      getMaskStatus().then(data => {
        this.secondMask = !!(data && data.isSecondMask)
      }).catch(() => {
        this.secondMask = false
      })
    },
    handleMaskSwitch(val) {
      this.maskSwitching = true
      switchMask(val).then(() => {
        this.$message.success(val ? '已开启二次遮挡' : '已关闭二次遮挡')
      }).catch(() => {
        // 失败时回滚开关状态
        this.secondMask = !val
      }).finally(() => {
        this.maskSwitching = false
      })
    },
    /* ---------- 敏感详情（限时会话） ---------- */
    openMaskDetail(row) {
      this.detail = {}
      this.sessionKey = ''
      this.maskVisible = true
      this.maskLoading = true
      createViewSession(row.id).then(data => {
        this.sessionKey = (data && data.sessionKey) || ''
        this.remainingSeconds = ((data && data.expireMinutes) || 15) * 60
        return getSensitiveDetail(this.sessionKey, row.id)
      }).then(data => {
        this.detail = data || {}
        this.startCountdown()
      }).catch(() => {
        this.maskVisible = false
      }).finally(() => {
        this.maskLoading = false
      })
    },
    startCountdown() {
      this.stopCountdown()
      this.countdownTimer = setInterval(() => {
        if (this.remainingSeconds <= 1) {
          this.remainingSeconds = 0
          this.stopCountdown()
          this.$message.warning('查看会话已到期，敏感详情已自动关闭')
          this.closeMaskDialog()
          return
        }
        this.remainingSeconds -= 1
      }, 1000)
    },
    stopCountdown() {
      if (this.countdownTimer) {
        clearInterval(this.countdownTimer)
        this.countdownTimer = null
      }
    },
    releaseSession() {
      if (this.sessionKey) {
        closeViewSession(this.sessionKey).catch(() => {})
        this.sessionKey = ''
      }
    },
    closeMaskDialog() {
      this.maskVisible = false
    },
    handleMaskClosed() {
      this.stopCountdown()
      this.releaseSession()
      this.detail = {}
      this.remainingSeconds = 0
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

/* 二次遮挡开关条 */
.mask-config {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
  padding: 12px 16px;
  background: var(--c-bg-card);
  border: 1px solid var(--c-border-light);
  border-left: 3px solid var(--c-primary);
  border-radius: var(--r-panel);
}
.mask-config-main {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}
.mask-config-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  flex: none;
  border-radius: var(--r-btn);
  background: var(--c-primary-light);
  color: var(--c-primary);
  font-size: 16px;
}
.mask-config-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--c-text-1);
  line-height: 1.4;
}
.mask-config-desc {
  margin-top: 2px;
  font-size: 12.5px;
  color: var(--c-text-3);
  line-height: 1.5;
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
  border-color: var(--c-warning-border);
  color: var(--c-warning-text);
}
.el-tag.status-tag--approved {
  background: var(--c-success);
  border-color: var(--c-success);
  color: #fff;
}
.el-tag.status-tag--rejected {
  background: var(--c-danger-light);
  border-color: var(--c-danger-border);
  color: var(--c-danger-text);
}

/* 敏感详情弹窗 */
.mask-dialog {
  min-height: 96px;
}
.mask-session {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-bottom: 14px;
}
.mask-chip {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 3px 10px;
  border-radius: var(--r-pill);
  background: var(--c-primary-light);
  border: 1px solid var(--c-primary-border);
  color: var(--c-primary);
  font-size: 12px;
  font-variant-numeric: tabular-nums;
}
.mask-chip--plain {
  background: var(--c-success-light);
  border-color: var(--c-success-border);
  color: var(--c-success-text);
}
.mask-chip--masked {
  background: var(--c-warning-light);
  border-color: var(--c-warning-border);
  color: var(--c-warning-text);
}
.mask-chip--ghost {
  background: var(--c-bg-head);
  border-color: var(--c-border-light);
  color: var(--c-text-3);
}
.mask-fields {
  list-style: none;
  margin: 0;
  padding: 0;
  border: 1px solid var(--c-border-light);
  border-radius: var(--r-table);
  overflow: hidden;
}
.mask-field {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 9px 12px;
  font-size: 13px;
  border-bottom: 1px solid var(--c-border-light);
}
.mask-field:last-child {
  border-bottom: none;
}
.mask-field:nth-child(odd) {
  background: var(--c-bg-head);
}
.mask-label {
  width: 96px;
  flex: none;
  color: var(--c-text-3);
}
.mask-value {
  flex: 1;
  color: var(--c-text-1);
  font-weight: 500;
  word-break: break-all;
}
.mask-value.is-mono {
  font-family: Consolas, 'SFMono-Regular', Menlo, monospace;
  letter-spacing: 0.2px;
}
.mask-empty {
  margin: 8px 0;
  font-size: 13px;
  color: var(--c-text-3);
  text-align: center;
}
.mask-tip {
  margin: 12px 0 0;
  padding: 8px 10px;
  background: var(--c-info-light, var(--c-primary-light));
  border: 1px solid var(--c-info-border, var(--c-primary-border));
  border-radius: var(--r-table);
  font-size: 12.5px;
  line-height: 1.6;
  color: var(--c-info-text, var(--c-text-2));
}
.mask-foot-tip {
  margin: 10px 0 0;
  font-size: 12px;
  line-height: 1.6;
  color: var(--c-text-3);
}

/* 底部隐私提示：浅灰提示条 */
.privacy-footer {
  margin-top: 16px;
  padding: 8px 12px;
  background: var(--c-bg-head);
  border: 1px solid var(--c-border-light);
  border-left: 3px solid var(--c-border);
  border-radius: var(--r-table);
}
.privacy-footer ::v-deep .compliance-footer {
  margin-top: 0;
  text-align: left;
  color: var(--c-text-3);
  font-size: 12px;
  line-height: 1.6;
}
</style>
