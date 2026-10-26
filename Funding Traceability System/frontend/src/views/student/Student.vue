<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <div class="page-title">学生管理</div>
        <div class="page-subtitle">敏感字段加密独立存储，列表仅展示脱敏信息，明文须走隐私调取审批</div>
      </div>
      <div class="page-actions">
        <el-button v-permission="['AUDIT_ADMIN']" icon="el-icon-download" :loading="exportLoading" @click="handleExport">导出</el-button>
        <el-button v-permission="['AUDIT_ADMIN']" type="primary" icon="el-icon-plus" @click="handleCreate">新增学生</el-button>
      </div>
    </div>

    <PrivacyTipBar />

    <div class="kpi-row">
      <div class="kpi-card">
        <div class="kpi-label">学生档案总数</div>
        <div class="kpi-value">{{ total }}</div>
        <div class="kpi-hint">份学生档案</div>
      </div>
      <div class="kpi-card">
        <div class="kpi-label">本页档案</div>
        <div class="kpi-value">{{ list.length }}</div>
        <div class="kpi-hint">当前页展示条数</div>
      </div>
      <div class="kpi-card kpi-card--success">
        <div class="kpi-label">本页资助人</div>
        <div class="kpi-value">{{ sponsorCountInPage }}</div>
        <div class="kpi-hint">位资助人关联</div>
      </div>
    </div>

    <!-- 查询区 -->
    <div class="toolbar">
      <el-input
        v-model="listQuery.keyword"
        placeholder="虚拟ID / 学校"
        clearable
        style="width: 240px"
        @keyup.enter.native="handleFilter"
      />
      <el-button type="primary" icon="el-icon-search" @click="handleFilter">查询</el-button>
      <el-button icon="el-icon-refresh" @click="handleReset">重置</el-button>
    </div>

    <!-- 表格 -->
    <el-table
      v-loading="listLoading"
      :data="list"
      border
      stripe
      style="width: 100%"
    >
      <el-table-column type="index" label="序号" width="55" align="center" />
      <el-table-column prop="virtualId" label="虚拟ID" min-width="135" />
      <el-table-column prop="maskName" label="姓名(脱敏)" min-width="100" />
      <el-table-column prop="gender" label="性别" width="60" align="center" />
      <el-table-column prop="school" label="学校" min-width="120" show-overflow-tooltip />
      <el-table-column prop="grade" label="年级" min-width="100" show-overflow-tooltip />
      <el-table-column prop="sponsorName" label="资助人" min-width="90" />
      <el-table-column prop="createTime" label="创建时间" min-width="150" />
      <el-table-column label="操作" width="320" align="center" fixed="right" class-name="op-col">
        <template slot-scope="{ row }">
          <el-button type="text" class="link-secondary" icon="el-icon-key" @click="openPrivacy(row)">隐私调取</el-button>
          <template v-if="isAdmin">
            <el-button type="text" icon="el-icon-edit" @click="handleUpdate(row)">编辑</el-button>
            <el-button type="text" class="link-secondary" icon="el-icon-folder" @click="openAttachments(row)">材料</el-button>
            <el-button type="text" class="danger-text" icon="el-icon-delete" @click="handleDelete(row)">删除</el-button>
          </template>
        </template>
      </el-table-column>
      <template slot="empty">
        <EmptyState type="student" title="暂无学生档案" desc="新增学生后，档案将在这里统一管理与脱敏展示" />
      </template>
    </el-table>

    <!-- 分页 -->
    <div class="pagination-bar">
      <el-pagination
        background
        :current-page="listQuery.page"
        :page-size="listQuery.size"
        :page-sizes="[10, 20, 50]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>

    <!-- 新增/编辑弹窗 -->
    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="640px" :close-on-click-modal="false">
      <el-form ref="dataForm" :model="form" :rules="rules" label-width="100px" class="form-grid">
        <el-row :gutter="10">
          <el-col :span="12">
            <el-form-item label="姓名" prop="name">
              <el-input v-model="form.name" :placeholder="isEdit ? '已加密存储，留空表示不修改' : '请输入姓名'" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="性别" prop="gender">
              <el-select v-model="form.gender" placeholder="请选择性别" style="width:100%">
                <el-option label="男" value="男" />
                <el-option label="女" value="女" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="10">
          <el-col :span="12">
            <el-form-item label="身份证号" prop="idCard">
              <el-input v-model="form.idCard" :placeholder="isEdit ? '已加密存储，留空表示不修改' : '请输入身份证号'" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系电话" prop="phone">
              <el-input v-model="form.phone" :placeholder="isEdit ? '已加密存储，留空表示不修改' : '请输入联系电话'" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="10">
          <el-col :span="12">
            <el-form-item label="学校" prop="school">
              <el-input v-model="form.school" placeholder="请输入学校" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="年级" prop="grade">
              <el-input v-model="form.grade" placeholder="请输入年级" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="资助人" prop="sponsorId">
          <el-select v-model="form.sponsorId" placeholder="请选择资助人" filterable style="width:100%" @change="onSponsorChange">
            <el-option v-for="item in sponsorOptions" :key="item.id" :label="item.realName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="家庭住址" prop="familyAddress">
          <el-input v-model="form.familyAddress" :placeholder="isEdit ? '已加密存储，留空表示不修改' : '请输入家庭住址'" />
        </el-form-item>
        <el-form-item label="家庭情况" prop="familySituation">
          <SensitiveTextarea
            v-model="form.familySituation"
            field-name="家庭情况"
            :rows="3"
            :placeholder="isEdit ? '已加密存储，留空表示不修改' : '请描述家庭情况，请勿填写未打码的身份证号、手机号等敏感信息'"
            @block="familyBlock = $event"
          />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" :loading="submitLoading" :disabled="familyBlock" @click="submitForm">确 定</el-button>
      </div>
    </el-dialog>

    <!-- 隐私调取弹窗 -->
    <el-dialog title="隐私明文调取" :visible.sync="privacyVisible" width="600px" :close-on-click-modal="false">
      <el-descriptions :column="2" border size="small" class="privacy-desc">
        <el-descriptions-item label="虚拟ID">{{ privacyRow.virtualId }}</el-descriptions-item>
        <el-descriptions-item label="姓名(脱敏)">{{ privacyRow.maskName }}</el-descriptions-item>
      </el-descriptions>

      <el-form v-if="!canDirectView" label-width="90px" class="privacy-form">
        <el-form-item label="字段范围" required>
          <el-checkbox-group v-model="privacyFields">
            <el-checkbox label="name">姓名</el-checkbox>
            <el-checkbox label="idCard">身份证号</el-checkbox>
            <el-checkbox label="phone">电话</el-checkbox>
            <el-checkbox label="familyAddress">家庭住址</el-checkbox>
            <el-checkbox label="familySituation">家庭情况</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="调取理由" required>
          <el-input v-model="privacyReason" type="textarea" :rows="3" placeholder="请填写调取理由（必填）" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="applyLoading" @click="submitApply">提交申请</el-button>
          <span class="apply-tip">提交后须经初审、终审两级审批</span>
        </el-form-item>
      </el-form>
      <el-alert
        v-else
        class="privacy-form"
        type="success"
        :closable="false"
        title="您具有档案核验直接查看权，点击下方「查看明文」即可调取，调取行为将全程留痕"
        show-icon
      />

      <el-divider />

      <div class="profile-section">
        <el-button type="warning" icon="el-icon-view" :loading="profileLoading" @click="viewProfile">查看明文</el-button>
        <div v-if="profile" class="profile-result">
          <el-alert
            v-if="profile.masked"
            type="info"
            :closable="false"
            title="暂无有效审批单，请提交申请并等待两级审批"
            show-icon
          />
          <template v-else>
            <el-alert
              type="success"
              :closable="false"
              :title="profile.directView ? '直接查看（审核管理员档案核验权，已全程留痕）' : ('审批单 #' + profile.applyId + '，明文可见至 ' + profile.expireTime)"
              show-icon
              style="margin-bottom:12px"
            />
            <el-descriptions :column="1" border size="small">
              <el-descriptions-item v-if="profile.name" label="姓名">{{ profile.name }}</el-descriptions-item>
              <el-descriptions-item v-if="profile.idCard" label="身份证号">{{ profile.idCard }}</el-descriptions-item>
              <el-descriptions-item v-if="profile.phone" label="电话">{{ profile.phone }}</el-descriptions-item>
              <el-descriptions-item v-if="profile.familyAddress" label="家庭住址">{{ profile.familyAddress }}</el-descriptions-item>
              <el-descriptions-item v-if="profile.familySituation" label="家庭情况">{{ profile.familySituation }}</el-descriptions-item>
            </el-descriptions>
          </template>
        </div>
      </div>
    </el-dialog>

    <!-- 材料弹窗 -->
    <el-dialog title="佐证材料" :visible.sync="attVisible" width="640px" :close-on-click-modal="false">
      <el-table v-loading="attLoading" :data="attList" border size="mini" style="width:100%">
        <el-table-column prop="fileName" label="文件名" min-width="180" show-overflow-tooltip />
        <el-table-column label="状态" width="100" align="center">
          <template slot-scope="{ row }">
            <el-tag :type="row.maskStatus === 'CONFIRMED' ? 'success' : 'warning'" size="small">
              {{ row.maskStatus === 'CONFIRMED' ? '已确认' : '待脱敏' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="上传时间" min-width="150" />
      </el-table>
      <div class="upload-bar">
        <el-upload
          action=""
          accept=".txt"
          :show-file-list="false"
          :http-request="customUpload"
        >
          <el-button type="primary" size="small" icon="el-icon-upload2" :loading="uploadLoading">上传材料(.txt)</el-button>
        </el-upload>
        <span class="upload-tip">上传后系统将自动识别敏感内容，需确认打码后方可入库</span>
      </div>
    </el-dialog>

    <!-- 脱敏预览弹窗 -->
    <AttachmentMaskPreview
      :visible.sync="previewVisible"
      :student-id="currentStudentId"
      :attachment-id="previewData.attachmentId"
      :original-text="previewData.originalText"
      :hits="previewData.hits"
      @confirmed="onAttachmentConfirmed"
    />

    <ComplianceFooter />
  </div>
</template>

<script>
import { listStudents, getStudent, addStudent, updateStudent, deleteStudent, exportStudents } from '@/api/student'
import { userOptions } from '@/api/user'
import { applyPrivacyAccess, getPrivacyProfile } from '@/api/privacy'
import { uploadAttachment, listAttachments } from '@/api/attachment'
import { downloadFile } from '@/utils/download'
import PrivacyTipBar from '@/components/PrivacyTipBar.vue'
import ComplianceFooter from '@/components/ComplianceFooter.vue'
import SensitiveTextarea from '@/components/SensitiveTextarea.vue'
import AttachmentMaskPreview from '@/components/AttachmentMaskPreview.vue'
import EmptyState from '@/components/EmptyState.vue'

export default {
  name: 'Student',
  components: { PrivacyTipBar, ComplianceFooter, SensitiveTextarea, AttachmentMaskPreview, EmptyState },
  data() {
    return {
      list: [],
      total: 0,
      listLoading: false,
      listQuery: { page: 1, size: 10, keyword: '' },
      dialogVisible: false,
      submitLoading: false,
      exportLoading: false,
      dialogType: 'create',
      sponsorOptions: [],
      familyBlock: false,
      form: this.getEmptyForm(),
      // 隐私调取
      privacyVisible: false,
      privacyRow: {},
      privacyFields: ['name', 'idCard', 'phone', 'familyAddress', 'familySituation'],
      privacyReason: '',
      applyLoading: false,
      profile: null,
      profileLoading: false,
      // 材料
      attVisible: false,
      attList: [],
      attLoading: false,
      uploadLoading: false,
      currentStudentId: null,
      previewVisible: false,
      previewData: { attachmentId: null, originalText: '', hits: [] }
    }
  },
  computed: {
    isAdmin() {
      return this.$store.getters.role === 'AUDIT_ADMIN'
    },
    // 审核管理员：档案核验直接查看权，无需审批
    canDirectView() {
      return this.isAdmin
    },
    isEdit() {
      return this.dialogType === 'update'
    },
    dialogTitle() {
      return this.isEdit ? '编辑学生' : '新增学生'
    },
    // 仅用于顶部统计展示，不改变任何列表逻辑
    sponsorCountInPage() {
      return new Set(this.list.map(item => item.sponsorName).filter(Boolean)).size
    },
    rules() {
      return {
        name: [{ required: !this.isEdit, message: '请输入姓名', trigger: 'blur' }],
        gender: [{ required: true, message: '请选择性别', trigger: 'change' }],
        idCard: [{ required: !this.isEdit, message: '请输入身份证号', trigger: 'blur' }],
        school: [{ required: true, message: '请输入学校', trigger: 'blur' }],
        grade: [{ required: true, message: '请输入年级', trigger: 'blur' }],
        sponsorId: [{ required: true, message: '请选择资助人', trigger: 'change' }]
      }
    }
  },
  created() {
    this.getList()
    this.loadSponsors()
  },
  methods: {
    getEmptyForm() {
      return {
        id: null,
        name: '',
        gender: '',
        idCard: '',
        school: '',
        grade: '',
        familyAddress: '',
        familySituation: '',
        phone: '',
        sponsorId: undefined,
        sponsorName: ''
      }
    },
    getList() {
      this.listLoading = true
      listStudents(this.listQuery).then(data => {
        this.list = (data && data.records) || []
        this.total = (data && data.total) || 0
      }).finally(() => {
        this.listLoading = false
      })
    },
    loadSponsors() {
      userOptions('SPONSOR').then(data => {
        this.sponsorOptions = data || []
      }).catch(() => {})
    },
    handleFilter() {
      this.listQuery.page = 1
      this.getList()
    },
    handleReset() {
      this.listQuery.keyword = ''
      this.listQuery.page = 1
      this.getList()
    },
    handleSizeChange(size) {
      this.listQuery.size = size
      this.listQuery.page = 1
      this.getList()
    },
    handleCurrentChange(page) {
      this.listQuery.page = page
      this.getList()
    },
    handleCreate() {
      this.dialogType = 'create'
      this.form = this.getEmptyForm()
      this.familyBlock = false
      this.dialogVisible = true
      this.$nextTick(() => {
        this.$refs.dataForm && this.$refs.dataForm.clearValidate()
      })
    },
    handleUpdate(row) {
      this.dialogType = 'update'
      getStudent(row.id).then(data => {
        const form = Object.assign(this.getEmptyForm(), data || {})
        // 编辑态隐私字段留空，留空表示不修改
        form.name = ''
        form.idCard = ''
        form.phone = ''
        form.familyAddress = ''
        form.familySituation = ''
        this.form = form
        this.familyBlock = false
        this.dialogVisible = true
        this.$nextTick(() => {
          this.$refs.dataForm && this.$refs.dataForm.clearValidate()
        })
      })
    },
    onSponsorChange(val) {
      const target = this.sponsorOptions.find(s => s.id === val)
      this.form.sponsorName = target ? target.realName : ''
    },
    submitForm() {
      this.$refs.dataForm.validate(valid => {
        if (!valid) return
        this.submitLoading = true
        const payload = Object.assign({}, this.form)
        const action = this.dialogType === 'create'
          ? addStudent(payload)
          : updateStudent(payload.id, payload)
        action.then(() => {
          this.$message.success(this.dialogType === 'create' ? '新增成功' : '修改成功')
          this.dialogVisible = false
          this.getList()
        }).finally(() => {
          this.submitLoading = false
        })
      })
    },
    handleDelete(row) {
      this.$confirm(`确认删除学生「${row.maskName}」吗？`, '提示', {
        type: 'warning'
      }).then(() => {
        deleteStudent(row.id).then(() => {
          this.$message.success('删除成功')
          if (this.list.length === 1 && this.listQuery.page > 1) {
            this.listQuery.page--
          }
          this.getList()
        })
      }).catch(() => {})
    },
    handleExport() {
      this.exportLoading = true
      exportStudents().then(blob => {
        return downloadFile(blob, '学生列表.xlsx')
      }).then(() => {
        this.$message.success('导出成功')
      }).catch(err => {
        if (err && err.message) this.$message.error(err.message)
      }).finally(() => {
        this.exportLoading = false
      })
    },
    // 隐私调取
    openPrivacy(row) {
      this.privacyRow = row
      this.privacyFields = ['name', 'idCard', 'phone', 'familyAddress', 'familySituation']
      this.privacyReason = ''
      this.profile = null
      this.privacyVisible = true
    },
    submitApply() {
      if (!this.privacyFields.length) {
        this.$message.warning('请选择字段范围')
        return
      }
      if (!this.privacyReason.trim()) {
        this.$message.warning('请填写调取理由')
        return
      }
      this.applyLoading = true
      applyPrivacyAccess({
        virtualId: this.privacyRow.virtualId,
        fieldScope: this.privacyFields.join(','),
        reason: this.privacyReason.trim()
      }).then(() => {
        this.$message.success('申请已提交，请等待两级审批')
        this.privacyReason = ''
      }).finally(() => {
        this.applyLoading = false
      })
    },
    viewProfile() {
      this.profileLoading = true
      getPrivacyProfile(this.privacyRow.virtualId).then(data => {
        this.profile = data || { masked: true }
      }).finally(() => {
        this.profileLoading = false
      })
    },
    // 材料
    openAttachments(row) {
      this.currentStudentId = row.id
      this.attVisible = true
      this.loadAttachments()
    },
    loadAttachments() {
      this.attLoading = true
      listAttachments(this.currentStudentId).then(data => {
        this.attList = data || []
      }).finally(() => {
        this.attLoading = false
      })
    },
    customUpload({ file }) {
      this.uploadLoading = true
      uploadAttachment(this.currentStudentId, file).then(data => {
        this.previewData = {
          attachmentId: data.attachmentId,
          originalText: data.originalText || '',
          hits: data.hits || []
        }
        this.previewVisible = true
        this.loadAttachments()
      }).finally(() => {
        this.uploadLoading = false
      })
    },
    onAttachmentConfirmed() {
      this.loadAttachments()
    }
  }
}
</script>

<style scoped>
/* 顶部隐私提示栏：浅蓝底 + 左侧主色条 */
.page-container .privacy-tip-bar {
  background: #eaf2fb;
  border: 1px solid #cfe0f3;
  border-left: 3px solid #2b6cb0;
  border-radius: 8px;
  color: #2b6cb0;
  font-size: 12px;
  line-height: 1.6;
  padding: 8px 12px;
  margin-bottom: 14px;
  gap: 6px;
}
.page-container .privacy-tip-bar ::v-deep i {
  color: #2b6cb0;
  font-size: 14px;
}
.pagination-bar {
  margin-top: 16px;
  text-align: right;
}
.danger-text {
  color: var(--c-danger) !important;
}
.danger-text:hover,
.danger-text:focus {
  color: var(--c-danger) !important;
  background: var(--c-danger-light) !important;
}
/* 次级文字按钮：隐私调取 / 材料 */
.el-button--text.link-secondary {
  color: var(--c-text-3) !important;
}
.el-button--text.link-secondary:hover,
.el-button--text.link-secondary:focus {
  color: var(--c-primary) !important;
  background: var(--c-primary-light);
}
.privacy-desc {
  margin-bottom: 12px;
}
.privacy-form {
  margin-top: 4px;
}
.apply-tip {
  margin-left: 10px;
  font-size: 12px;
  color: var(--c-text-3);
}
.profile-section {
  margin-top: 4px;
}
.profile-result {
  margin-top: 10px;
}
/* 桌面端操作列按钮单行排列，避免换行错位（移动端仍按 responsive.css 换行） */
@media (min-width: 769px) {
  ::v-deep .op-col .cell {
    white-space: nowrap;
  }
}
.profile-alert {
  margin-bottom: 10px;
}
.upload-bar {
  margin-top: 12px;
  display: flex;
  align-items: center;
  gap: 10px;
}
.upload-tip {
  font-size: 12px;
  color: var(--c-text-3);
}
</style>
