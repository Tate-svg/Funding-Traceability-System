<template>
  <div class="page-container">
    <div class="filter-bar">
      <el-button icon="el-icon-refresh" @click="getList">刷新</el-button>
      <div class="filter-actions">
        <el-button v-permission="['AUDIT_ADMIN']" type="success" icon="el-icon-plus" @click="handleCreate">新增指标</el-button>
      </div>
    </div>

    <el-alert title="指标权重为百分比(0-100)，所有指标权重之和建议为 100；评分总分由系统按权重加权自动计算。" type="info" :closable="false" show-icon style="margin-bottom:12px" />

    <el-table v-loading="listLoading" :data="list" border stripe style="width:100%">
      <el-table-column type="index" label="序号" width="60" align="center" />
      <el-table-column prop="name" label="指标名称" min-width="140" />
      <el-table-column prop="weight" label="权重(%)" width="100" align="center" />
      <el-table-column prop="description" label="描述" min-width="240" show-overflow-tooltip />
      <el-table-column v-if="isAdmin" label="操作" width="150" align="center">
        <template slot-scope="{ row }">
          <el-button type="text" icon="el-icon-edit" @click="handleUpdate(row)">编辑</el-button>
          <el-button type="text" class="danger-text" icon="el-icon-delete" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="footer-tip">指标合计权重：{{ totalWeight }}%</div>

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

export default {
  name: 'ScoreItem',
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
.page-container { background:#fff; padding:16px; border-radius:4px; }
.filter-bar { display:flex; align-items:center; gap:8px; margin-bottom:12px; }
.filter-actions { margin-left:auto; display:flex; gap:8px; }
.danger-text { color:#F56C6C; }
.footer-tip { margin-top:12px; color:#909399; font-size:13px; text-align:right; }
</style>
