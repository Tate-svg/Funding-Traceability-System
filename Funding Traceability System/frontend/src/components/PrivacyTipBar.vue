<template>
  <div class="privacy-tip-bar">
    <i class="el-icon-info"></i>
    <span>{{ text }}</span>
  </div>
</template>

<script>
export default {
  name: 'PrivacyTipBar',
  props: {
    text: {
      type: String,
      default: '隐私保护提示：请优先在本地完成脱敏后再提交；系统将在服务端自动二次脱敏校验，未脱敏内容将被拦截并记录。'
    }
  }
}
</script>

<style scoped>
.privacy-tip-bar {
  display: flex;
  align-items: center;
  gap: 6px;
  background: var(--c-info-light);
  border: 1px solid var(--c-info-border);
  border-left: 3px solid var(--c-info);
  color: var(--c-info-text);
  font-size: 12.5px;
  line-height: 1.6;
  padding: 8px 12px;
  border-radius: var(--r-panel);
  margin-bottom: 12px;
}
.privacy-tip-bar i {
  color: var(--c-info-text);
  font-size: 14px;
}
</style>
