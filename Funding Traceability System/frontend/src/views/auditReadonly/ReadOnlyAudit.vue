<template>
  <div class="page-container">
    <div class="page-header">
      <div><div class="page-title">只读审计</div><div class="page-subtitle">查阅资助台账、评分规则快照、节点记录与操作日志</div></div>
      <el-tag type="info">只读账号</el-tag>
    </div>
    <el-alert title="仅展示审计必要字段；学生姓名已脱敏，原始日志参数、自由文本备注和隐私档案不在此展示。" type="info" :closable="false" />
    <el-tabs v-model="active" @tab-click="changeTab">
      <el-tab-pane label="资助台账" name="funds" />
      <el-tab-pane label="评分记录" name="scores" />
      <el-tab-pane label="节点台账" name="timestamps" />
      <el-tab-pane label="操作日志" name="logs" />
    </el-tabs>
    <div class="toolbar">
      <el-input-number v-if="active === 'scores' || active === 'timestamps'" v-model="fundId" :min="1" :precision="0" :controls="false" placeholder="资助单ID（选填）" />
      <el-button type="primary" icon="el-icon-refresh" @click="reload">刷新</el-button>
      <el-button v-if="active === 'timestamps'" :loading="verifying" @click="verify">核验节点哈希链</el-button>
    </div>
    <el-alert v-if="verification" :title="verification" type="info" :closable="false" style="margin-bottom:12px" />
    <el-table :key="active" v-loading="loading" :data="records" border stripe>
      <template v-if="active === 'funds'">
        <el-table-column prop="id" label="资助单ID" width="100" />
        <el-table-column prop="voucherNo" label="凭证号" min-width="200" />
        <el-table-column prop="studentName" label="学生（脱敏）" />
        <el-table-column prop="amount" label="金额（元）" />
        <el-table-column prop="fundDate" label="资助日期" />
        <el-table-column label="状态"><template slot-scope="{ row }">{{ statusText(row.status) }}</template></el-table-column>
        <el-table-column prop="disburseTime" label="拨付登记时间" min-width="165" />
        <el-table-column label="查阅" width="110"><template slot-scope="{ row }"><el-button type="text" @click="relatedScores(row.id)">关联评分</el-button></template></el-table-column>
      </template>
      <template v-else-if="active === 'scores'">
        <el-table-column prop="id" label="评分ID" width="85" />
        <el-table-column prop="studentName" label="学生（脱敏）" />
        <el-table-column label="资助单" min-width="170"><template slot-scope="{ row }">{{ row.fundNo || '历史未关联' }}</template></el-table-column>
        <el-table-column prop="scoreDate" label="评分日期" />
        <el-table-column prop="totalScore" label="总分" />
        <el-table-column label="规则快照" min-width="145"><template slot-scope="{ row }">{{ shortVersion(row.ruleVersion) }}</template></el-table-column>
        <el-table-column label="查阅" width="85"><template slot-scope="{ row }"><el-button type="text" @click="showScore(row.id)">详情</el-button></template></el-table-column>
      </template>
      <template v-else-if="active === 'timestamps'">
        <el-table-column prop="serialNo" label="序号" width="85" />
        <el-table-column prop="bizType" label="业务类型" />
        <el-table-column prop="bizId" label="业务ID" />
        <el-table-column prop="nodeName" label="节点" min-width="140" />
        <el-table-column prop="operatorName" label="操作账号" />
        <el-table-column prop="tsTime" label="本地记录时间" min-width="165" />
        <el-table-column prop="tsHash" label="节点哈希" min-width="240" show-overflow-tooltip />
      </template>
      <template v-else>
        <el-table-column prop="id" label="日志ID" width="90" />
        <el-table-column prop="username" label="操作账号" />
        <el-table-column prop="role" label="角色" />
        <el-table-column prop="operation" label="操作" min-width="170" />
        <el-table-column prop="targetType" label="业务类型" />
        <el-table-column prop="targetId" label="业务ID" />
        <el-table-column prop="createTime" label="记录时间" min-width="165" />
      </template>
    </el-table>
    <el-pagination background :current-page="page" :page-size="10" :total="total" layout="total, prev, pager, next" style="margin-top:16px;text-align:right" @current-change="changePage" />
    <el-dialog append-to-body title="评分规则与得分" :visible.sync="detailVisible" width="720px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="评分ID">{{ detail.id }}</el-descriptions-item>
        <el-descriptions-item label="总分">{{ detail.totalScore }}</el-descriptions-item>
        <el-descriptions-item label="关联资助单">{{ detail.fundNo || '历史未关联' }}</el-descriptions-item>
        <el-descriptions-item label="规则留存时间">{{ detail.ruleCapturedAt || '历史未留存' }}</el-descriptions-item>
        <el-descriptions-item label="规则标识" :span="2">{{ detail.ruleVersion || '历史规则未留存，无法还原权重' }}</el-descriptions-item>
      </el-descriptions>
      <el-table :data="detail.details || []" border style="margin-top:12px">
        <el-table-column prop="itemName" label="评分时指标" />
        <el-table-column label="评分时权重（%）"><template slot-scope="{ row }">{{ row.weight == null ? '未留存' : row.weight }}</template></el-table-column>
        <el-table-column prop="itemScore" label="得分" />
      </el-table>
    </el-dialog>
  </div>
</template>

<script>
import { auditReadonlyList, auditReadonlyScore, auditReadonlyVerify } from '@/api/auditReadonly'
export default {
  name: 'ReadOnlyAudit',
  data() {
    return { active: 'funds', records: [], total: 0, page: 1, fundId: undefined, loading: false, verifying: false, verification: '', detailVisible: false, detail: {} }
  },
  created() { this.load() },
  methods: {
    load() {
      const active = this.active
      this.loading = true
      auditReadonlyList(active, { page: this.page, size: 10, fundId: this.fundId }).then(data => {
        if (active !== this.active) return
        this.records = data.records || []
        this.total = data.total || 0
      }).finally(() => { if (active === this.active) this.loading = false })
    },
    changeTab() { this.page = 1; this.records = []; this.verification = ''; this.load() },
    reload() { this.page = 1; this.load() },
    changePage(page) { this.page = page; this.load() },
    relatedScores(id) { this.fundId = id; this.active = 'scores'; this.changeTab() },
    shortVersion(value) { return value ? value.slice(0, 12) : '历史未留存' },
    showScore(id) { auditReadonlyScore(id).then(data => { this.detail = data; this.detailVisible = true }) },
    verify() {
      this.verifying = true
      auditReadonlyVerify(this.fundId).then(data => {
        this.verification = data.valid ? '节点元数据哈希链一致；此结果不包含当前金额或附件内容的重新核验。' : ('节点哈希链校验未通过，首个断点序号：' + data.chainBrokenAt)
      }).finally(() => { this.verifying = false })
    },
    statusText(value) { return ({ SUBMITTED: '待审核', AUDITED: '审核通过', REJECTED: '已驳回', REVIEWED: '复核通过', DISBURSED: '已拨付' })[value] || value }
  }
}
</script>
