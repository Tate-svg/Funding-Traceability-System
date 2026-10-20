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

    <el-empty v-if="!loading && nodes.length === 0" description="暂无时间戳记录" :image-size="60" />
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
          <div class="trace-node" :class="{ 'is-anomaly': item.anomaly === 1, 'is-broken': item.hashValid === false }">
            <div class="node-head">
              <span class="node-tag" :style="{ background: nodeColor(item.node) }">{{ item.nodeName }}</span>
              <span class="node-serial">#{{ item.serialNo }}</span>
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

const NODE_COLORS = {
  SUBMIT: '#409EFF',
  AUDIT_PASS: '#67C23A',
  AUDIT_REJECT: '#F56C6C',
  REVIEW: '#E6A23C',
  REVIEW_DRAW: '#CC7A00',
  DISBURSE: '#C9A227',
  VOUCHER_MODIFY: '#8E6FE8',
  VISIT_RECORD: '#36A3B5',
  STUDENT_UPLOAD: '#13B5A6',
  OTHER: '#909399'
}

export default {
  name: 'TraceTimeline',
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
  background: #FAFBFC; border: 1px solid #EBEEF5; border-radius: 6px;
  padding: 10px 14px;
}
.trace-node::before {
  content: '可信存证 · 不可篡改';
  position: absolute; right: -16px; top: 50%;
  transform: translateY(-50%) rotate(-24deg);
  font-size: 22px; font-weight: 700; letter-spacing: 5px;
  color: rgba(64, 158, 255, .05);
  white-space: nowrap; pointer-events: none;
}
/* 时序异常节点：红色边框高亮 */
.trace-node.is-anomaly {
  border-color: #F56C6C; background: #FFF7F7;
  box-shadow: 0 0 0 2px rgba(245, 108, 108, .12);
  cursor: help;
}
.trace-node.is-anomaly::before { color: rgba(245, 108, 108, .07); }
.trace-node.is-broken { border-style: dashed; }

.node-head { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.node-tag { color: #fff; font-size: 12px; padding: 2px 10px; border-radius: 10px; line-height: 18px; }
.node-serial { font-family: Consolas, monospace; font-size: 12px; color: #909399; }
.stamp-flag { color: #67C23A; font-size: 12px; margin-left: auto; }
.node-meta { margin-top: 6px; font-size: 12px; color: #909399; display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.node-summary { margin-top: 6px; font-size: 13px; color: #606266; word-break: break-all; }
</style>
