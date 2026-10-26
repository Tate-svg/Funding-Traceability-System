<template>
  <div class="login-container">
    <!-- 背景装饰：网格纹理 + 柔光 + 几何圆环 -->
    <div class="bg-grid" aria-hidden="true"></div>
    <div class="bg-glow bg-glow--a" aria-hidden="true"></div>
    <div class="bg-glow bg-glow--b" aria-hidden="true"></div>
    <div class="bg-ring bg-ring--a" aria-hidden="true"></div>
    <div class="bg-ring bg-ring--b" aria-hidden="true"></div>

    <div class="login-card">
      <div class="login-header">
        <span class="logo-badge">
          <svg class="logo-svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"
            stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
            <path d="M3 8.6l9-3.8 9 3.8-9 3.8-9-3.8z" />
            <path d="M7.2 10.8v4.1c0 1.3 2.1 2.4 4.8 2.4s4.8-1.1 4.8-2.4v-4.1" />
            <path d="M21 8.6v4.2" />
          </svg>
        </span>
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
        <el-form-item class="submit-item">
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
/* ================= 背景层 ================= */
.login-container {
  position: relative;
  width: 100%;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px 16px;
  overflow: hidden;
  background:
    radial-gradient(1200px 620px at 12% -10%, rgba(77, 143, 207, 0.28) 0%, rgba(77, 143, 207, 0) 60%),
    radial-gradient(900px 520px at 100% 108%, rgba(47, 158, 110, 0.14) 0%, rgba(47, 158, 110, 0) 62%),
    linear-gradient(150deg, #13202f 0%, #1a344d 42%, #22506f 100%);
}
/* 网格纹理：向四周淡出，避免整屏生硬 */
.bg-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.045) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.045) 1px, transparent 1px);
  background-size: 54px 54px;
  -webkit-mask-image: radial-gradient(circle at 50% 45%, rgba(0, 0, 0, 0.9) 0%, rgba(0, 0, 0, 0) 72%);
  mask-image: radial-gradient(circle at 50% 45%, rgba(0, 0, 0, 0.9) 0%, rgba(0, 0, 0, 0) 72%);
  pointer-events: none;
}
/* 柔光光斑 */
.bg-glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(10px);
  pointer-events: none;
}
.bg-glow--a {
  width: 460px;
  height: 460px;
  left: -140px;
  top: -160px;
  background: radial-gradient(circle, rgba(110, 170, 222, 0.28) 0%, rgba(110, 170, 222, 0) 68%);
}
.bg-glow--b {
  width: 520px;
  height: 520px;
  right: -180px;
  bottom: -200px;
  background: radial-gradient(circle, rgba(78, 160, 150, 0.18) 0%, rgba(78, 160, 150, 0) 70%);
}
/* 几何圆环 */
.bg-ring {
  position: absolute;
  border-radius: 50%;
  border: 1px solid rgba(255, 255, 255, 0.07);
  pointer-events: none;
}
.bg-ring--a {
  width: 680px;
  height: 680px;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
}
.bg-ring--b {
  width: 980px;
  height: 980px;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  border-color: rgba(255, 255, 255, 0.04);
}

/* ================= 登录卡片 ================= */
.login-card {
  position: relative;
  z-index: 1;
  width: 400px;
  max-width: 92vw;
  padding: 42px 40px 24px;
  background: #ffffff;
  border-radius: 16px;
  overflow: hidden;
  box-shadow:
    0 0 0 1px rgba(255, 255, 255, 0.06),
    0 10px 24px rgba(8, 16, 26, 0.18),
    0 30px 70px rgba(8, 16, 26, 0.32);
  animation: cardIn 0.5s cubic-bezier(0.22, 0.61, 0.36, 1) both;
}
/* 卡片顶部主色细线 */
.login-card::before {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  top: 0;
  height: 3px;
  border-radius: 16px 16px 0 0;
  background: linear-gradient(90deg, #245a96 0%, #3c7cb8 50%, #6aa6de 100%);
}
@keyframes cardIn {
  from {
    opacity: 0;
    transform: translateY(18px) scale(0.985);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

/* ================= 标题区 ================= */
.login-header {
  text-align: center;
  margin-bottom: 26px;
}
.logo-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 46px;
  height: 46px;
  border-radius: 13px;
  color: var(--c-primary, #2b6cb0);
  background: linear-gradient(180deg, #f1f7fd 0%, #e6effa 100%);
  border: 1px solid #d9e6f5;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.9), 0 2px 6px rgba(43, 108, 176, 0.1);
}
.logo-svg {
  width: 24px;
  height: 24px;
}
.title {
  margin: 14px 0 6px;
  font-size: 23px;
  line-height: 1.3;
  color: var(--c-text-1, #1e2a3a);
  font-weight: 700;
  letter-spacing: 1.5px;
}
.subtitle {
  margin: 0;
  font-size: 11px;
  color: var(--c-text-4, #9aa6b5);
  letter-spacing: 1.6px;
  text-transform: uppercase;
}

/* ================= 表单 ================= */
.login-form {
  margin-top: 4px;
}
.login-form ::v-deep .el-form-item {
  margin-bottom: 20px;
}
.login-form ::v-deep .el-input__inner {
  height: 46px;
  line-height: 46px;
  padding-left: 40px;
  font-size: 14px;
  color: var(--c-text-1, #1e2a3a);
  background: #fbfcfe;
  border: 1px solid var(--c-border, #e3e8ef);
  border-radius: 8px;
  transition: border-color 0.2s ease, background-color 0.2s ease, box-shadow 0.2s ease;
}
.login-form ::v-deep .el-input__inner::placeholder {
  color: #b3bdc9;
}
.login-form ::v-deep .el-input__inner:hover {
  border-color: #c3cfdd;
  background: #ffffff;
}
.login-form ::v-deep .el-input__inner:focus {
  border-color: var(--c-primary, #2b6cb0);
  background: #ffffff;
  box-shadow: 0 0 0 3px rgba(43, 108, 176, 0.12);
}
.login-form ::v-deep .el-input__prefix {
  left: 13px;
  display: flex;
  align-items: center;
  color: #a6b1c0;
  transition: color 0.2s ease;
}
.login-form ::v-deep .el-input__icon {
  width: 16px;
  font-size: 16px;
  line-height: 46px;
}
.login-form ::v-deep .el-input__suffix {
  right: 10px;
}
.login-form ::v-deep .el-input__suffix-inner {
  display: flex;
  align-items: center;
  height: 46px;
}
.login-form ::v-deep .el-input__clear,
.login-form ::v-deep .el-input__password {
  font-size: 14px;
  color: #a6b1c0;
  transition: color 0.2s ease;
}
.login-form ::v-deep .el-input__clear:hover,
.login-form ::v-deep .el-input__password:hover {
  color: var(--c-primary, #2b6cb0);
}
/* 聚焦时前缀图标转为主色 */
.login-form ::v-deep .el-input:focus-within .el-input__prefix {
  color: var(--c-primary, #2b6cb0);
}
.login-form ::v-deep .el-form-item__error {
  padding-top: 5px;
  font-size: 12px;
}
.submit-item {
  margin-top: 26px;
  margin-bottom: 0 !important;
}

/* ================= 主按钮 ================= */
.login-btn.el-button--primary {
  width: 100%;
  height: 46px;
  padding: 0;
  border: none;
  border-radius: 8px;
  font-size: 15px;
  font-weight: 500;
  letter-spacing: 6px;
  color: #ffffff;
  background-image: linear-gradient(135deg, #3077ba 0%, #245a96 100%);
  background-color: #245a96;
  box-shadow: 0 6px 16px rgba(36, 90, 150, 0.26);
  transition: background-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
}
.login-btn.el-button--primary:hover,
.login-btn.el-button--primary:focus {
  background-image: linear-gradient(135deg, #2a6aab 0%, #1e4c7e 100%);
  background-color: #1e4c7e;
  box-shadow: 0 10px 24px rgba(30, 76, 126, 0.34);
  transform: translateY(-1px);
}
.login-btn.el-button--primary:active {
  background-image: linear-gradient(135deg, #245a96 0%, #1a426d 100%);
  box-shadow: 0 4px 10px rgba(30, 76, 126, 0.24);
  transform: translateY(1px);
}

/* ================= 底部提示 ================= */
.tips {
  margin-top: 20px;
  padding-top: 14px;
  border-top: 1px dashed var(--c-border-light, #eef1f6);
  text-align: center;
  font-size: 12px;
  line-height: 1.6;
  color: var(--c-text-4, #9aa6b5);
  letter-spacing: 0.2px;
}

/* ================= 响应式 ================= */
@media (max-width: 768px) {
  .login-container {
    padding: 24px 14px;
  }
  .login-card {
    width: 88vw;
    max-width: 420px;
    padding: 34px 24px 20px;
    border-radius: 14px;
  }
  .title {
    font-size: 20px;
    letter-spacing: 1px;
  }
  .logo-badge {
    width: 42px;
    height: 42px;
    border-radius: 12px;
  }
  .logo-svg {
    width: 22px;
    height: 22px;
  }
  .bg-ring--b {
    display: none;
  }
}

@media (prefers-reduced-motion: reduce) {
  .login-card {
    animation: none;
  }
}
</style>
