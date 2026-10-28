<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <div class="page-title">操作日志</div>
        <div class="page-subtitle">系统每一次业务操作都会在此留下只读存证痕迹，可检索、可追溯</div>
      </div>
      <div class="page-actions">
        <span class="readonly-pill"><i class="el-icon-lock"></i> 只读存证</span>
      </div>
    </div>

    <div class="note-bar">
      <i class="el-icon-lock"></i>
      <span>操作日志为只读存证记录，一经写入不可删除、不可篡改</span>
    </div>

    <div class="kpi-row">
      <div class="kpi-card">
        <div class="kpi-label">日志总数</div>
        <div class="kpi-value">{{ total }}</div>
        <div class="kpi-hint">条只读存证记录</div>
      </div>
      <div class="kpi-card">
        <div class="kpi-label">本页记录</div>
        <div class="kpi-value">{{ list.length }}</div>
        <div class="kpi-hint">当前页展示条数</div>
      </div>
      <div class="kpi-card kpi-card--warn">
        <div class="kpi-label">本页存证操作</div>
        <div class="kpi-value">{{ sealedCount }}</div>
        <div class="kpi-hint">资助单 / 复核任务类</div>
      </div>
    </div>

    <div class="filter-bar">
      <el-select v-model="listQuery.targetType" placeholder="操作对象" clearable style="width:150px" @change="handleFilter">
        <el-option label="资助单" value="fund" />
        <el-option label="复核任务" value="review_task" />
        <el-option label="学生" value="student" />
        <el-option label="回访" value="visit" />
        <el-option label="评分" value="score" />
        <el-option label="用户" value="user" />
      </el-select>
      <el-input v-model="listQuery.keyword" placeholder="操作内容关键词" clearable style="width:220px" @keyup.enter.native="handleFilter" />
      <el-button type="primary" icon="el-icon-search" @click="handleFilter">查询</el-button>
      <el-button icon="el-icon-refresh" @click="handleReset">重置</el-button>
    </div>

    <div v-loading="listLoading" class="timeline-wrap">
      <EmptyState v-if="!listLoading && list.length === 0" type="ledger" title="暂无操作日志" desc="系统产生的每一次业务操作都会在此留下只读存证记录" />
      <el-timeline v-else>
        <el-timeline-item
          v-for="item in list"
          :key="item.id"
          :timestamp="item.createTime"
          placement="top"
          :color="timelineColor(item.targetType)"
        >
          <div class="log-card" :style="{ borderLeftColor: timelineColor(item.targetType) }">
            <div class="log-title">
              <span class="operation">{{ item.operation }}</span>
              <el-tag :type="roleTagType(item.role)" size="mini">{{ roleText(item.role) }}</el-tag>
              <template v-if="isVoucherOp(item)">
                <i class="el-icon-lock lock-icon"></i>
                <el-tag type="warning" size="mini" effect="plain" class="sealed-tag" @click="showDetail(item)">已存证</el-tag>
              </template>
            </div>
            <div class="log-meta">
              <span>操作人：{{ item.username }}</span>
              <span>对象：{{ targetTypeText(item.targetType) }} #{{ item.targetId || '—' }}</span>
            </div>
            <div v-if="item.content" class="log-content">{{ item.content }}</div>
          </div>
        </el-timeline-item>
      </el-timeline>
    </div>

    <div class="pagination-bar">
      <el-pagination
        background
        :current-page="listQuery.page"
        :page-size="listQuery.size"
        :page-sizes="[10, 20, 50]"
        :total="total"
        layout="total, sizes, prev, pager, next"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>

    <!-- 存证详情弹窗 -->
    <el-dialog title="存证详情" :visible.sync="detailVisible" width="560px">
      <el-descriptions v-if="detail" :column="1" border size="small">
        <el-descriptions-item label="操作">{{ detail.operation }}</el-descriptions-item>
        <el-descriptions-item label="操作人">{{ detail.username }}（{{ roleText(detail.role) }}）</el-descriptions-item>
        <el-descriptions-item label="时间">{{ detail.createTime }}</el-descriptions-item>
        <el-descriptions-item label="对象">{{ targetTypeText(detail.targetType) }} #{{ detail.targetId || '—' }}</el-descriptions-item>
        <el-descriptions-item label="内容">{{ detail.content || '—' }}</el-descriptions-item>
      </el-descriptions>
      <el-alert type="warning" :closable="false" show-icon class="sealed-alert"
        title="该记录已写入只读存证日志，关联凭证采用 SHA-256 哈希校验，任何篡改都会导致哈希不一致" />
      <div slot="footer">
        <el-button type="primary" @click="detailVisible = false">关 闭</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listOperationLogs } from '@/api/log'
import EmptyState from '@/components/EmptyState.vue'

export default {
  name: 'OperationLog',
  components: { EmptyState },
  data() {
    return {
      list: [],
      total: 0,
      listLoading: false,
      listQuery: { page: 1, size: 10, targetType: undefined, targetId: undefined, keyword: '' },
      detailVisible: false,
      detail: null
    }
  },
  created() {
    this.getList()
  },
  computed: {
    // 本页中属于「存证类」操作的条数（仅展示统计，不改变任何逻辑）
    sealedCount() {
      return this.list.filter(item => this.isVoucherOp(item)).length
    }
  },
  methods: {
    isVoucherOp(item) {
      return item.targetType === 'fund' || item.targetType === 'review_task'
    },
    timelineColor(targetType) {
      const map = { fund: '#2b6cb0', review_task: '#c07f2e' }
      return map[targetType] || '#7a8798'
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
      const map = { AUDIT_ADMIN: 'danger', DISBURSE_ADMIN: 'warning', SUPERVISOR: 'primary', SPONSOR: 'success' }
      return map[role] || 'info'
    },
    targetTypeText(targetType) {
      const map = { fund: '资助单', review_task: '复核任务', student: '学生', visit: '回访', score: '评分', user: '用户' }
      return map[targetType] || targetType || '-'
    },
    getList() {
      this.listLoading = true
      listOperationLogs(this.listQuery).then(data => {
        this.list = (data && data.records) || []
        this.total = (data && data.total) || 0
      }).finally(() => { this.listLoading = false })
    },
    handleFilter() { this.listQuery.page = 1; this.getList() },
    handleReset() { this.listQuery.targetType = undefined; this.listQuery.keyword = ''; this.listQuery.page = 1; this.getList() },
    handleSizeChange(size) { this.listQuery.size = size; this.listQuery.page = 1; this.getList() },
    handleCurrentChange(page) { this.listQuery.page = page; this.getList() },
    showDetail(item) {
      this.detail = item
      this.detailVisible = true
    }
  }
}
</script>

<style scoped>
.readonly-pill {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  height: 26px;
  padding: 0 12px;
  border-radius: var(--r-pill);
  background: var(--c-warning-light);
  border: 1px solid #f2ddbe;
  color: var(--c-warning);
  font-size: 12px;
  font-weight: 600;
}
.filter-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 16px;
  padding: 10px 12px;
  background: var(--c-bg-head);
  border: 1px solid var(--c-border-light);
  border-radius: var(--r-panel);
}
.timeline-wrap { padding: 2px 4px; }
.timeline-wrap ::v-deep .el-timeline-item__timestamp {
  color: var(--c-text-3);
  font-size: 12px;
  font-variant-numeric: tabular-nums;
}
.timeline-wrap ::v-deep .el-timeline-item__tail {
  border-left-color: var(--c-border);
}
.log-card {
  background: var(--c-bg-card);
  border: 1px solid var(--c-border-light);
  border-left: 3px solid var(--c-border);
  border-radius: var(--r-panel);
  padding: 11px 14px;
  box-shadow: var(--sh-1);
  transition: box-shadow var(--t-fast), transform var(--t-fast);
}
.log-card:hover {
  box-shadow: var(--sh-2);
  transform: translateX(2px);
}
.log-title { display:flex; align-items:center; gap:8px; flex-wrap:wrap; }
.operation { font-weight:600; color:var(--c-text-1); font-size:13.5px; }
.lock-icon { color:var(--c-warning); }
.sealed-tag { cursor:pointer; }
.log-meta { margin-top:6px; font-size:12px; color:var(--c-text-3); display:flex; gap:16px; flex-wrap:wrap; }
.log-content { margin-top:6px; font-size:12.5px; color:var(--c-text-2); word-break:break-all; line-height:1.6; }
.pagination-bar { margin-top:16px; text-align:right; }
.sealed-alert { margin-top:12px; }

@media (max-width: 768px) {
  .filter-bar > .el-select,
  .filter-bar > .el-input {
    width: 100% !important;
  }
}
</style>
