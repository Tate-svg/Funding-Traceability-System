<template>
  <div class="sensitive-textarea">
    <el-input
      :value="value"
      type="textarea"
      :rows="rows"
      :placeholder="placeholder"
      :class="{ 'has-hit': hits.length > 0 }"
      @input="onInput"
    />
    <div v-if="hits.length > 0" class="hit-panel">
      <div class="hit-title">
        <i class="el-icon-warning"></i>
        检测到 {{ hits.length }} 处未打码敏感内容，请打码后再提交
        <el-button type="text" size="mini" class="mask-btn" @click="maskAll">一键打码</el-button>
      </div>
      <div v-for="(hit, idx) in hits" :key="idx" class="hit-item">
        <el-tag size="mini" type="danger">{{ ruleName(hit.rule) }}</el-tag>
        <span class="hit-raw">{{ hit.text }}</span>
        <i class="el-icon-right"></i>
        <span class="hit-masked">{{ hit.masked }}</span>
      </div>
    </div>
  </div>
</template>

<script>
import { RULE_NAMES, scanSensitive, applyMasks } from '@/utils/sensitive'

export default {
  name: 'SensitiveTextarea',
  props: {
    value: { type: String, default: '' },
    placeholder: { type: String, default: '' },
    rows: { type: Number, default: 3 },
    fieldName: { type: String, default: '内容' }
  },
  data() {
    return {
      // 与后端同款规则（按优先级、区间不重叠）
      rules: [
        { rule: 'ID_CARD', regex: /\d{17}[\dXx]/ },
        { rule: 'PHONE', regex: /1[3-9]\d{9}/ },
        { rule: 'BANK_CARD', regex: /\d{16,19}/ },
        { rule: 'LONG_DIGIT', regex: /\d{8,}/ }
      ],
      hits: []
    }
  },
  watch: {
    value: {
      immediate: true,
      handler() {
        this.scan()
      }
    },
    'hits.length'(val, oldVal) {
      if (val !== oldVal) {
        this.$emit('block', val > 0)
      }
    }
  },
  methods: {
    ruleName(rule) {
      return RULE_NAMES[rule] || rule
    },
    scan() {
      this.hits = scanSensitive(this.value, this.rules)
    },
    onInput(val) {
      this.$emit('input', val)
    },
    maskAll() {
      const maskedText = applyMasks(this.value, this.hits)
      this.$emit('input', maskedText)
    }
  }
}
</script>

<style scoped>
.sensitive-textarea {
  width: 100%;
}
.sensitive-textarea ::v-deep .has-hit .el-textarea__inner {
  border-color: #F56C6C;
}
.hit-panel {
  margin-top: 6px;
  padding: 6px 8px;
  background: #FEF0F0;
  border: 1px solid #FDE2E2;
  border-radius: 4px;
  font-size: 12px;
  color: #F56C6C;
}
.hit-title {
  display: flex;
  align-items: center;
  gap: 4px;
}
.mask-btn {
  margin-left: auto;
  padding: 0;
  color: #409EFF;
}
.hit-item {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 4px;
  color: #606266;
}
.hit-raw {
  color: #F56C6C;
  word-break: break-all;
}
.hit-masked {
  color: #67C23A;
  word-break: break-all;
}
</style>
