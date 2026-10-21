<template>
  <div class="login-container">
    <div class="login-card">
      <div class="login-header">
        <i class="el-icon-school"></i>
        <h2 class="title">助学帮扶管理系统</h2>
        <p class="subtitle">Funding Traceability System</p>
      </div>
      <el-form
        ref="loginForm"
        :model="loginForm"
        :rules="loginRules"
        class="login-form"
        label-position="top"
      >
        <el-form-item prop="username">
          <el-input
            v-model="loginForm.username"
            placeholder="请输入用户名"
            prefix-icon="el-icon-user"
            clearable
            @keyup.enter.native="handleLogin"
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="请输入密码"
            prefix-icon="el-icon-lock"
            show-password
            @keyup.enter.native="handleLogin"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            class="login-btn"
            :loading="loading"
            @click="handleLogin"
          >
            登 录
          </el-button>
        </el-form-item>
        <div class="tips">
          <span>提示：管理员 / 资助人 由后端账号登录</span>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script>
export default {
  name: 'Login',
  data() {
    return {
      loginForm: {
        username: '',
        password: ''
      },
      loginRules: {
        username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
        password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
      },
      loading: false
    }
  },
  methods: {
    handleLogin() {
      this.$refs.loginForm.validate(valid => {
        if (!valid) return
        this.loading = true
        this.$store
          .dispatch('user/login', this.loginForm)
          .then(() => {
            const redirect = this.$route.query.redirect || '/'
            this.$router.push(redirect).catch(() => {})
          })
          .catch(() => {
            this.loading = false
          })
      })
    }
  }
}
</script>

<style scoped>
.login-container {
  width: 100%;
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #2b3a4d 0%, #3a5a7a 100%);
}
.login-card {
  width: 400px;
  padding: 40px 36px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.2);
}

/* 手机端登录卡片自适应 */
@media (max-width: 768px) {
  .login-card {
    width: 88vw;
    padding: 7vw 6vw;
  }
}
.login-header {
  text-align: center;
  margin-bottom: 24px;
}
.login-header .el-icon-school {
  font-size: 40px;
  color: #409EFF;
}
.title {
  margin: 10px 0 4px;
  font-size: 22px;
  color: #303133;
  font-weight: 600;
  letter-spacing: 2px;
}
.subtitle {
  margin: 0;
  font-size: 12px;
  color: #909399;
  letter-spacing: 1px;
}
.login-form {
  margin-top: 10px;
}
.login-btn {
  width: 100%;
}
.tips {
  text-align: center;
  font-size: 12px;
  color: #C0C4CC;
  margin-top: 4px;
}
</style>
