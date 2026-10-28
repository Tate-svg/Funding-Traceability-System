<template>
  <div class="compliance-footer">{{ text }}</div>
</template>

<script>
export default {
  name: 'ComplianceFooter',
  props: {
    text: {
      type: String,
      default: '本页面涉及个人信息处理，已按最小必要原则脱敏展示；隐私明文调取须经两级审批并全程留痕，严禁超范围使用。'
    }
  }
}
</script>

<style scoped>
.compliance-footer {
  margin-top: 16px;
  text-align: center;
  font-size: 12px;
  color: #909399;
}
</style>
