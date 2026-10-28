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
  background: #ECF5FF;
  border-left: 4px solid #409EFF;
  color: #606266;
  font-size: 12px;
  line-height: 1.5;
  padding: 6px 10px;
  border-radius: 2px;
  margin-bottom: 12px;
}
.privacy-tip-bar i {
  color: #409EFF;
  font-size: 14px;
}
</style>
