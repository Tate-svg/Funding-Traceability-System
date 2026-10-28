<template>
  <div class="page-404">
    <div class="content">
      <h1 class="code">404</h1>
      <p class="desc">抱歉，您访问的页面不存在</p>
      <el-button type="primary" @click="goHome">返回首页</el-button>
    </div>
  </div>
</template>

<script>
export default {
  name: 'NotFound',
  methods: {
    goHome() {
      this.$router.push('/')
    }
  }
}
</script>

<style scoped>
.page-404 {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100vh;
  background: #f0f2f5;
}
.content {
  text-align: center;
}
.code {
  font-size: 90px;
  color: var(--c-primary);
  margin: 0 0 10px;
}
.desc {
  color: var(--c-text-3);
  margin-bottom: 24px;
}
</style>
