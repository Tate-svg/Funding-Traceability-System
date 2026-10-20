<template>
  <el-dialog
    title="材料脱敏预览"
    :visible="visible"
    width="720px"
    :close-on-click-modal="false"
    @update:visible="$emit('update:visible', $event)"
    @open="initText"
  >
    <div v-if="hits && hits.length > 0" class="hit-table-wrap">
      <div class="section-title">系统检测到 {{ hits.length }} 处敏感内容，已自动打码：</div>
      <el-table :data="hits" border size="mini" max-height="180">
        <el-table-column label="规则" width="100" align="center">
          <template slot-scope="{ row }">
            <el-tag size="mini" type="danger">{{ ruleName(row.rule) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="text" label="原始片段" min-width="160" show-overflow-tooltip />
        <el-table-column prop="masked" label="建议打码" min-width="160" show-overflow-tooltip />
      </el-table>
    </div>
    <div v-else class="section-title">未检测到敏感内容，可直接确认入库。</div>

    <el-input
      v-model="editedText"
      type="textarea"
      :rows="10"
      :class="{ 'has-hit': rescanHits.length > 0 }"
      placeholder="可手动调整打码范围"
    />
    <div v-if="rescanHits.length > 0" class="rescan-tip">
      <i class="el-icon-warning"></i>
      仍有 {{ rescanHits.length }} 处未打码敏感内容
    </div>

    <div slot="footer">
      <el-button @click="$emit('update:visible', false)">取 消</el-button>
      <el-button type="primary" :disabled="rescanHits.length > 0" :loading="submitting" @click="handleConfirm">确认入库</el-button>
    </div>
  </el-dialog>
</template>

<script>
import { RULE_NAMES, scanSensitive, applyMasks } from '@/utils/sensitive'
import { confirmAttachment } from '@/api/attachment'

export default {
  name: 'AttachmentMaskPreview',
  props: {
    visible: { type: Boolean, default: false },
    studentId: { type: [Number, String], default: null },
    attachmentId: { type: [Number, String], default: null },
    originalText: { type: String, default: '' },
    hits: { type: Array, default: () => [] }
  },
  data() {
    return {
      rules: [
        { rule: 'ID_CARD', regex: /\d{17}[\dXx]/ },
        { rule: 'PHONE', regex: /1[3-9]\d{9}/ },
        { rule: 'BANK_CARD', regex: /\d{16,19}/ },
        { rule: 'LONG_DIGIT', regex: /\d{8,}/ }
      ],
      editedText: '',
      submitting: false
    }
  },
  computed: {
    rescanHits() {
      return scanSensitive(this.editedText, this.rules)
    }
  },
  methods: {
    ruleName(rule) {
      return RULE_NAMES[rule] || rule
    },
    initText() {
      this.editedText = applyMasks(this.originalText, this.hits)
    },
    handleConfirm() {
      this.submitting = true
      confirmAttachment(this.studentId, this.attachmentId, this.editedText).then(() => {
        this.$message.success('材料已确认入库')
        this.$emit('update:visible', false)
        this.$emit('confirmed')
      }).finally(() => {
        this.submitting = false
      })
    }
  }
}
</script>

<style scoped>
.section-title {
  font-size: 13px;
  color: #606266;
  margin-bottom: 8px;
}
.hit-table-wrap {
  margin-bottom: 12px;
}
.rescan-tip {
  margin-top: 6px;
  font-size: 12px;
  color: #F56C6C;
}
::v-deep .has-hit .el-textarea__inner {
  border-color: #F56C6C;
}
</style>
