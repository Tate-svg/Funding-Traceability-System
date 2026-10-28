<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <div class="page-title">评分指标</div>
        <div class="page-subtitle">维护评分维度与权重，评分总分由系统按权重加权自动计算</div>
      </div>
      <div class="page-actions">
        <el-button icon="el-icon-refresh" @click="getList">刷新</el-button>
        <el-button v-permission="['AUDIT_ADMIN']" type="primary" icon="el-icon-plus" @click="handleCreate">新增指标</el-button>
      </div>
    </div>

    <div class="note-bar">
      <i class="el-icon-info"></i>
      <span>指标权重为百分比(0-100)，所有指标权重之和建议为 100；评分总分由系统按权重加权自动计算。</span>
    </div>

    <div class="kpi-row">
      <div class="kpi-card">
        <div class="kpi-label">指标数量</div>
        <div class="kpi-value">{{ list.length }}</div>
        <div class="kpi-hint">项评分维度</div>
      </div>
      <div class="kpi-card" :class="Number(totalWeight) === 100 ? 'kpi-card--success' : 'kpi-card--warn'">
        <div class="kpi-label">合计权重</div>
        <div class="kpi-value">{{ totalWeight }}%</div>
        <div class="kpi-hint">{{ Number(totalWeight) === 100 ? '权重配置完整' : '尚未达到 100%' }}</div>
      </div>
      <div class="kpi-card" :class="Number(totalWeight) === 100 ? 'kpi-card--success' : 'kpi-card--danger'">
        <div class="kpi-label">与 100% 的偏差</div>
        <div class="kpi-value">{{ weightGap }}%</div>
        <div class="kpi-hint">建议调整为 0%</div>
      </div>
    </div>

    <el-table v-loading="listLoading" :data="list" border stripe style="width:100%">
      <el-table-column type="index" label="序号" width="60" align="center" />
      <el-table-column prop="name" label="指标名称" min-width="140" />
      <el-table-column prop="weight" label="权重(%)" width="170" align="center">
        <template slot-scope="{ row }">
          <div class="weight-cell">
            <el-progress
              :percentage="weightPercent(row.weight)"
              :stroke-width="6"
              :show-text="false"
              :color="weightColor(row.weight)"
              class="weight-progress"
            />
            <span class="weight-value">{{ row.weight }}%</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="description" label="描述" min-width="240" show-overflow-tooltip />
      <el-table-column v-if="isAdmin" label="操作" width="150" align="center">
        <template slot-scope="{ row }">
          <el-button type="text" icon="el-icon-edit" @click="handleUpdate(row)">编辑</el-button>
          <el-button type="text" class="danger-text" icon="el-icon-delete" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
      <template slot="empty">
        <EmptyState type="chart" title="暂无评分指标" desc="新增指标后可在此查看权重占比" />
      </template>
    </el-table>

    <div class="footer-tip">
      <span class="footer-label">指标合计权重：</span>
      <span class="footer-value">{{ totalWeight }}%</span>
      <span v-if="Number(totalWeight) !== 100" class="footer-hint footer-hint--warn">（与 100% 相差 {{ weightGap }}%）</span>
      <span v-else class="footer-hint footer-hint--ok">（权重合计已达 100%）</span>
    </div>

    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="480px" :close-on-click-modal="false">
      <el-form ref="dataForm" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="指标名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入指标名称" />
        </el-form-item>
        <el-form-item label="权重(%)" prop="weight">
          <el-input-number v-model="form.weight" :min="0" :max="100" :precision="1" :step="5" controls-position="right" style="width:100%" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入描述" />
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
import { listScoreItems, addScoreItem, updateScoreItem, deleteScoreItem } from '@/api/scoreItem'
import EmptyState from '@/components/EmptyState.vue'

export default {
  name: 'ScoreItem',
  components: { EmptyState },
  data() {
    return {
      list: [],
      listLoading: false,
      dialogVisible: false,
      submitLoading: false,
      dialogType: 'create',
      form: this.getEmptyForm(),
      rules: {
        name: [{ required: true, message: '请输入指标名称', trigger: 'blur' }],
        weight: [{ required: true, message: '请输入权重', trigger: 'blur' }]
      }
    }
  },
  computed: {
    isAdmin() { return this.$store.getters.role === 'AUDIT_ADMIN' },
    dialogTitle() { return this.dialogType === 'create' ? '新增评分指标' : '编辑评分指标' },
    totalWeight() {
      return this.list.reduce((s, d) => s + Number(d.weight || 0), 0).toFixed(1)
    },
    weightGap() {
      return Math.abs(100 - Number(this.totalWeight)).toFixed(1)
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getEmptyForm() {
      return { id: null, name: '', weight: 0, description: '' }
    },
    getList() {
      this.listLoading = true
      listScoreItems().then(data => {
        this.list = data || []
      }).finally(() => { this.listLoading = false })
    },
    weightPercent(val) {
      const n = Number(val)
      if (isNaN(n)) return 0
      return Math.max(0, Math.min(100, n))
    },
    // 按权重占比深浅着色：占比越高颜色越深（仅展示用）
    weightColor(val) {
      const n = Number(val || 0)
      if (n >= 30) return '#1e4c7e'
      if (n >= 15) return '#2b6cb0'
      return '#7fa9d6'
    },
    handleCreate() {
      this.dialogType = 'create'
      this.form = this.getEmptyForm()
      this.dialogVisible = true
      this.$nextTick(() => { this.$refs.dataForm && this.$refs.dataForm.clearValidate() })
    },
    handleUpdate(row) {
      this.dialogType = 'update'
      this.form = Object.assign(this.getEmptyForm(), row)
      this.dialogVisible = true
      this.$nextTick(() => { this.$refs.dataForm && this.$refs.dataForm.clearValidate() })
    },
    submitForm() {
      this.$refs.dataForm.validate(valid => {
        if (!valid) return
        this.submitLoading = true
        const payload = { name: this.form.name, weight: this.form.weight, description: this.form.description }
        const action = this.dialogType === 'create' ? addScoreItem(payload) : updateScoreItem(this.form.id, payload)
        action.then(() => {
          this.$message.success(this.dialogType === 'create' ? '新增成功' : '修改成功')
          this.dialogVisible = false
          this.getList()
        }).finally(() => { this.submitLoading = false })
      })
    },
    handleDelete(row) {
      this.$confirm(`确认删除指标「${row.name}」吗？删除后相关评分将受影响。`, '提示', { type: 'warning' }).then(() => {
        deleteScoreItem(row.id).then(() => {
          this.$message.success('删除成功')
          this.getList()
        })
      }).catch(() => {})
    }
  }
}
</script>

<style scoped>
.danger-text { color:var(--c-danger); }
.weight-cell { display:flex; align-items:center; justify-content:center; gap:8px; }
.weight-progress { flex:1; min-width:60px; }
.weight-value { width:44px; text-align:right; font-size:12px; color:var(--c-text-2); font-variant-numeric:tabular-nums; }
.footer-tip {
  margin-top:14px;
  padding:12px 16px;
  text-align:right;
  background:var(--c-primary-light);
  border:1px solid var(--c-primary-border);
  border-radius:var(--r-panel);
  font-size:13px;
  color:var(--c-text-3);
}
.footer-label { color:var(--c-text-2); font-weight:600; }
.footer-value { color:var(--c-primary); font-size:18px; font-weight:700; font-variant-numeric:tabular-nums; }
.footer-hint { font-size:12px; margin-left:4px; }
.footer-hint--warn { color:var(--c-warning); }
.footer-hint--ok { color:var(--c-success); }
</style>
