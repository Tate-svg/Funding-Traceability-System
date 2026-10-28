<template>
  <div class="trace-timeline" v-loading="loading">
    <!-- 核验结论 -->
    <el-alert
      v-if="verify"
      :type="verify.valid ? 'success' : 'error'"
      :closable="false"
      show-icon
      class="verify-alert"
      :title="verify.valid
        ? ('哈希链核验通过：该单据 ' + nodes.length + ' 个节点时间戳均与链上一致，时序异常 ' + verify.anomalyCount + ' 条')
        : ('哈希链核验失败：序列 #' + verify.chainBrokenAt + ' 起与链上哈希不一致，疑似篡改')"
    />

    <EmptyState v-if="!loading && nodes.length === 0" type="ledger" title="暂无时间戳记录" desc="该单据尚未产生时间戳存证节点" />
    <el-timeline v-else class="timeline">
      <el-timeline-item
        v-for="item in nodes"
        :key="item.serialNo"
        :timestamp="'时间戳 #' + item.serialNo + ' · ' + item.tsTime"
        placement="top"
        :color="nodeColor(item.node)"
        :icon="item.anomaly === 1 ? 'el-icon-warning' : 'el-icon-time'"
        :type="item.anomaly === 1 ? 'danger' : undefined"
      >
        <el-tooltip
          :disabled="item.anomaly !== 1"
          :content="item.anomalyReason || '时序异常'"
          placement="top"
        >
          <div class="trace-node" :class="['op-' + opType(item), { 'is-anomaly': item.anomaly === 1, 'is-broken': item.hashValid === false }]">
            <div class="node-head">
              <span class="node-tag" :style="{ background: nodeColor(item.node) }">{{ item.nodeName }}</span>
              <span class="node-serial">#{{ item.serialNo }}</span>
              <span class="op-flag" :class="'op-' + opType(item)">
                <i :class="opIcon(item)"></i> {{ opLabel(item) }}
              </span>
              <el-tag v-if="item.anomaly === 1" type="danger" size="mini" effect="dark">时序异常</el-tag>
              <el-tag v-else-if="item.hashValid === false" type="danger" size="mini" effect="plain">哈希不一致</el-tag>
              <span class="stamp-flag"><i class="el-icon-lock"></i> 可信存证</span>
            </div>
            <div class="node-meta">
              <span>操作人：{{ item.operatorName || '—' }}</span>
              <el-tag :type="roleTagType(item.role)" size="mini">{{ roleText(item.role) }}</el-tag>
            </div>
            <div class="node-summary">{{ item.summary }}</div>
          </div>
        </el-tooltip>
      </el-timeline-item>
    </el-timeline>
  </div>
</template>

<script>
import { listTimestamps, verifyTimestamps } from '@/api/trace'
import EmptyState from '@/components/EmptyState.vue'

const NODE_COLORS = {
  SUBMIT: '#2b6cb0',
  AUDIT_PASS: '#2f9e6e',
  AUDIT_REJECT: '#c24a45',
  REVIEW: '#c07f2e',
  REVIEW_DRAW: '#9e6420',
  DISBURSE: '#a88523',
  VOUCHER_MODIFY: '#6b5bb5',
  VISIT_RECORD: '#2f8f9e',
  STUDENT_UPLOAD: '#22998c',
  OTHER: '#7a8798'
}

export default {
  name: 'TraceTimeline',
  components: { EmptyState },
  props: {
    bizType: { type: String, required: true },
    bizId: { type: [String, Number], required: true }
  },
  data() {
    return {
      nodes: [],
      verify: null,
      loading: false
    }
  },
  created() {
    this.loadData()
  },
  methods: {
    nodeColor(node) {
      return NODE_COLORS[node] || NODE_COLORS.OTHER
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
    // 归纳操作类型：新增 / 修改 / 删除（仅用于视觉标注，不改动字段本身）
    opType(item) {
      const summary = (item && item.summary) || ''
      if (summary.indexOf('删除') > -1) return 'del'
      if (summary.indexOf('修改') > -1 || summary.indexOf('更新') > -1) return 'edit'
      if (summary.indexOf('新增') > -1 || summary.indexOf('录入') > -1 || summary.indexOf('上传') > -1 || summary.indexOf('提交') > -1) return 'add'
      const node = item && item.node
      if (node === 'SUBMIT' || node === 'VISIT_RECORD' || node === 'STUDENT_UPLOAD' || node === 'DISBURSE') return 'add'
      return 'edit'
    },
    opLabel(item) {
      const map = { add: '新增', edit: '修改', del: '删除' }
      return map[this.opType(item)]
    },
    opIcon(item) {
      const map = { add: 'el-icon-plus', edit: 'el-icon-edit', del: 'el-icon-delete' }
      return map[this.opType(item)]
    },
    loadData() {
      this.loading = true
      const params = { bizType: this.bizType, bizId: this.bizId, page: 1, size: 50 }
      Promise.all([
        listTimestamps(params),
        verifyTimestamps({ bizType: this.bizType, bizId: this.bizId })
      ]).then(([pageData, verifyData]) => {
        // 台账接口按 serial DESC 返回，时间轴按业务先后正序展示
        this.nodes = ((pageData && pageData.records) || []).slice().reverse()
        this.verify = verifyData
      }).catch(() => {}).finally(() => { this.loading = false })
    }
  }
}
</script>

<style scoped>
.trace-timeline { padding: 4px 8px; }
.verify-alert { margin-bottom: 16px; }
.timeline { padding-left: 4px; }

/* 节点卡片：可信存证水印底纹 */
.trace-node {
  position: relative; overflow: hidden;
  background: var(--c-bg-head); border: 1px solid var(--c-border-light); border-left: 3px solid transparent; border-radius: var(--r-panel);
  padding: 10px 14px;
}
.trace-node::before {
  content: '可信存证 · 不可篡改';
  position: absolute; right: -16px; top: 50%;
  transform: translateY(-50%) rotate(-24deg);
  font-size: 22px; font-weight: 700; letter-spacing: 5px;
  color: rgba(43, 108, 176, .05);
  white-space: nowrap; pointer-events: none;
}
/* 时序异常节点：红色边框高亮 */
.trace-node.is-anomaly {
  border-color: var(--c-danger); background: var(--c-danger-light);
  box-shadow: 0 0 0 2px rgba(194, 74, 69, .12);
  cursor: help;
}
.trace-node.is-anomaly::before { color: rgba(194, 74, 69, .07); }
.trace-node.is-broken { border-style: dashed; }

.node-head { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.node-tag { color: #fff; font-size: 12px; padding: 2px 10px; border-radius: var(--r-pill); line-height: 18px; }
.node-serial { font-family: Consolas, monospace; font-size: 12px; color: var(--c-text-3); }
.stamp-flag {
  display: inline-flex; align-items: center; gap: 4px;
  color: var(--c-success); background: var(--c-success-light); border: 1px solid #cbe9dc;
  font-size: 11px; line-height: 18px; padding: 0 8px; border-radius: var(--r-table);
  margin-left: auto;
}
.node-meta { margin-top: 6px; font-size: 12px; color: var(--c-text-3); display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.node-summary { margin-top: 6px; font-size: 13px; color: var(--c-text-2); word-break: break-all; }

/* 左侧色条按操作类型区分：新增绿 / 修改蓝 / 删除红 */
.trace-node.op-add { border-left-color: var(--c-success); }
.trace-node.op-edit { border-left-color: var(--c-primary); }
.trace-node.op-del { border-left-color: var(--c-danger); }
.op-flag {
  display: inline-flex; align-items: center; gap: 3px;
  font-size: 11px; line-height: 18px; padding: 0 8px;
  border-radius: var(--r-table); border: 1px solid transparent;
}
.op-flag.op-add { color: var(--c-success); background: var(--c-success-light); border-color: #cbe9dc; }
.op-flag.op-edit { color: var(--c-primary); background: var(--c-primary-light); border-color: #cfe0f3; }
.op-flag.op-del { color: var(--c-danger); background: var(--c-danger-light); border-color: #f2d3d1; }
/* 时序异常保留红色边框与浅红底，覆盖操作色条 */
.trace-node.is-anomaly { border-left-color: var(--c-danger); }

@media (max-width: 768px) {
  .trace-timeline { padding: 0; }
  .stamp-flag { margin-left: 0; }
}
</style>
