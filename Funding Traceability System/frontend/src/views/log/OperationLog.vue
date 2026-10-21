<template>
  <div class="page-container">
    <el-alert type="info" :closable="false" show-icon class="readonly-tip"
      title="操作日志为只读存证记录，一经写入不可删除、不可篡改" />

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
          <div class="log-card">
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
  methods: {
    isVoucherOp(item) {
      return item.targetType === 'fund' || item.targetType === 'review_task'
    },
    timelineColor(targetType) {
      const map = { fund: '#409EFF', review_task: '#E6A23C' }
      return map[targetType] || '#C0C4CC'
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
.page-container { background:#fff; padding:16px; border-radius:4px; }
.readonly-tip { margin-bottom:16px; }
.filter-bar { display:flex; align-items:center; gap:8px; margin-bottom:20px; flex-wrap:wrap; }
.timeline-wrap { padding:4px 8px; }
.log-card { background:#fafafa; border:1px solid #EBEEF5; border-radius:4px; padding:10px 14px; }
.log-title { display:flex; align-items:center; gap:8px; flex-wrap:wrap; }
.operation { font-weight:600; color:#303133; }
.lock-icon { color:#E6A23C; }
.sealed-tag { cursor:pointer; }
.log-meta { margin-top:6px; font-size:12px; color:#909399; display:flex; gap:16px; flex-wrap:wrap; }
.log-content { margin-top:6px; font-size:13px; color:#606266; word-break:break-all; }
.pagination-bar { margin-top:16px; text-align:right; }
.sealed-alert { margin-top:12px; }
</style>
