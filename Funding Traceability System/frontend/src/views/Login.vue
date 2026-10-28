<template>
  <div class="login-container">
    <!-- 背景层：档案网格 + 柔光 + 存证圆环 + 哈希链 -->
    <div class="bg-layer" aria-hidden="true">
      <div class="bg-grid"></div>
      <div class="bg-glow bg-glow--a"></div>
      <div class="bg-glow bg-glow--b"></div>
      <div class="bg-rings">
        <span class="bg-ring bg-ring--outer"></span>
        <span class="bg-ring bg-ring--mid"></span>
        <span class="bg-ring bg-ring--inner"></span>
      </div>
      <div class="bg-chain"></div>
      <div class="bg-vignette"></div>
    </div>

    <section class="login-card">
      <span class="card-accent" aria-hidden="true"></span>

      <div class="login-header">
        <span class="logo-seal">
          <span class="seal-ring" aria-hidden="true"></span>
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

      <div class="login-divider" aria-hidden="true">
        <span class="divider-line"></span>
        <i class="divider-dot"></i>
        <span class="divider-line"></span>
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
            name="username"
            autocomplete="username"
            aria-label="用户名"
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
            name="password"
            autocomplete="current-password"
            aria-label="密码"
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
    </section>
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
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  min-height: 100vh;
  padding: 36px 16px;
  overflow: hidden;
  background:
    radial-gradient(1100px 620px at 6% -14%, rgba(77, 143, 207, 0.30) 0%, rgba(77, 143, 207, 0) 62%),
    radial-gradient(820px 520px at 104% 108%, rgba(47, 158, 110, 0.16) 0%, rgba(47, 158, 110, 0) 66%),
    linear-gradient(154deg, #0e1a27 0%, #14293d 36%, #1b3c58 70%, #214c6b 100%);
}
.bg-layer {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

/* 档案纸网格：中心清晰、四周淡出 */
.bg-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.042) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.042) 1px, transparent 1px);
  background-size: 52px 52px;
  -webkit-mask-image: radial-gradient(circle at 50% 44%, rgba(0, 0, 0, 0.95) 0%, rgba(0, 0, 0, 0) 74%);
  mask-image: radial-gradient(circle at 50% 44%, rgba(0, 0, 0, 0.95) 0%, rgba(0, 0, 0, 0) 74%);
}

/* 冷光斑 */
.bg-glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(6px);
}
.bg-glow--a {
  top: -170px;
  left: -150px;
  width: 480px;
  height: 480px;
  background: radial-gradient(circle, rgba(110, 170, 222, 0.30) 0%, rgba(110, 170, 222, 0) 68%);
}
.bg-glow--b {
  right: -190px;
  bottom: -210px;
  width: 540px;
  height: 540px;
  background: radial-gradient(circle, rgba(78, 160, 150, 0.20) 0%, rgba(78, 160, 150, 0) 70%);
}

/* 存证圆环：外圈实线、中圈刻度缓转、内圈细线 */
.bg-rings {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
}
.bg-ring {
  position: absolute;
  top: 50%;
  left: 50%;
  border-radius: 50%;
  transform: translate(-50%, -50%);
}
.bg-ring--outer {
  width: 940px;
  height: 940px;
  border: 1px solid rgba(255, 255, 255, 0.055);
}
.bg-ring--mid {
  width: 640px;
  height: 640px;
  border: 1px dashed rgba(255, 255, 255, 0.075);
  background: repeating-conic-gradient(
    from 0deg,
    rgba(255, 255, 255, 0.09) 0deg 0.6deg,
    rgba(255, 255, 255, 0) 0.6deg 9deg
  );
  -webkit-mask: radial-gradient(farthest-side, transparent calc(100% - 12px), #000 calc(100% - 11px));
  mask: radial-gradient(farthest-side, transparent calc(100% - 12px), #000 calc(100% - 11px));
  animation: sealSpin 96s linear infinite;
}
.bg-ring--inner {
  width: 380px;
  height: 380px;
  border: 1px solid rgba(255, 255, 255, 0.05);
}
@keyframes sealSpin {
  to { transform: translate(-50%, -50%) rotate(360deg); }
}

/* 哈希链：节点 + 连线，末端为已存证节点 */
.bg-chain {
  position: absolute;
  right: 7%;
  bottom: 13%;
  width: 300px;
  height: 16px;
  background-image:
    radial-gradient(circle at 40px 50%, rgba(255, 255, 255, 0.34) 2.5px, rgba(255, 255, 255, 0) 3.2px),
    radial-gradient(circle at 100px 50%, rgba(255, 255, 255, 0.34) 2.5px, rgba(255, 255, 255, 0) 3.2px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.16) 0%, rgba(255, 255, 255, 0.16) 100%);
  background-size: 120px 16px, 120px 16px, 100% 1px;
  background-position: 0 0, 0 0, 0 50%;
  background-repeat: repeat-x, repeat-x, no-repeat;
  -webkit-mask-image: linear-gradient(90deg, transparent 0%, #000 28%, #000 78%, transparent 100%);
  mask-image: linear-gradient(90deg, transparent 0%, #000 28%, #000 78%, transparent 100%);
}
.bg-chain::after {
  content: '';
  position: absolute;
  top: 50%;
  right: 0;
  width: 7px;
  height: 7px;
  margin-top: -3.5px;
  border-radius: 50%;
  background: #2f9e6e;
  box-shadow: 0 0 0 4px rgba(47, 158, 110, 0.14), 0 0 12px rgba(47, 158, 110, 0.45);
}

/* 四周压暗，聚焦中心卡片 */
.bg-vignette {
  position: absolute;
  inset: 0;
  background: radial-gradient(ellipse at 50% 44%, rgba(0, 0, 0, 0) 36%, rgba(5, 11, 18, 0.52) 100%);
}

/* ================= 登录卡片 ================= */
.login-card {
  position: relative;
  z-index: 2;
  width: 424px;
  max-width: 92vw;
  padding: 40px 40px 24px;
  background: #ffffff;
  border-radius: var(--r-card, 12px);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.9),
    0 14px 30px rgba(8, 16, 26, 0.20),
    0 36px 84px rgba(8, 16, 26, 0.34);
  animation: cardIn 0.62s cubic-bezier(0.22, 0.61, 0.36, 1) both;
}
/* 顶部主色刻度线 */
.card-accent {
  position: absolute;
  top: 0;
  left: 40px;
  right: 40px;
  height: 2px;
  border-radius: 0 0 2px 2px;
  background: var(--c-primary, #2b6cb0);
  opacity: 0.85;
}
/* 对角边角刻线：凭证边框的暗示 */
.login-card::before,
.login-card::after {
  content: '';
  position: absolute;
  width: 12px;
  height: 12px;
  border-color: rgba(43, 108, 176, 0.26);
  border-style: solid;
  pointer-events: none;
}
.login-card::before {
  top: 12px;
  left: 12px;
  border-width: 1px 0 0 1px;
}
.login-card::after {
  right: 12px;
  bottom: 12px;
  border-width: 0 1px 1px 0;
}
@keyframes cardIn {
  from {
    opacity: 0;
    transform: translateY(20px) scale(0.985);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

/* ================= 标题区 ================= */
.login-header {
  text-align: center;
  margin-bottom: 18px;
  animation: riseIn 0.55s ease-out 0.08s both;
}
.logo-seal {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  border-radius: 50%;
  color: var(--c-primary, #2b6cb0);
  background: #eff6fd;
  border: 1px solid var(--c-primary-border, #cfe0f3);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.95),
    0 4px 12px rgba(43, 108, 176, 0.14);
}
.seal-ring {
  position: absolute;
  inset: -7px;
  border-radius: 50%;
  border: 1px dashed rgba(43, 108, 176, 0.28);
}
.logo-svg {
  width: 27px;
  height: 27px;
}
.title {
  margin: 16px 0 8px;
  font-size: 19px;
  line-height: 1.3;
  font-weight: 600;
  letter-spacing: 1px;
  color: var(--c-text-1, #1e2a3a);
}
.subtitle {
  margin: 0;
  font-size: 12.5px;
  letter-spacing: 2.4px;
  text-transform: uppercase;
  color: var(--c-text-3, #66748a);
}

/* 分隔线：细线 + 菱形节点 */
.login-divider {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 0 0 22px;
  animation: fadeIn 0.5s ease-out 0.14s both;
}
.divider-line {
  flex: 1;
  height: 1px;
  background: var(--c-border-light, #eef1f6);
}
.divider-dot {
  width: 5px;
  height: 5px;
  background: var(--c-primary, #2b6cb0);
  opacity: 0.5;
  transform: rotate(45deg);
}

/* ================= 表单 ================= */
.login-form {
  margin: 0;
}
.login-form ::v-deep .el-form-item {
  margin-bottom: 16px;
}
.login-form ::v-deep .el-form-item:nth-child(1) {
  animation: riseIn 0.5s ease-out 0.18s both;
}
.login-form ::v-deep .el-form-item:nth-child(2) {
  animation: riseIn 0.5s ease-out 0.24s both;
}
.login-form ::v-deep .el-form-item:nth-child(3) {
  animation: riseIn 0.5s ease-out 0.30s both;
}
.login-form ::v-deep .el-input__inner {
  height: 44px;
  line-height: 44px;
  padding-left: 40px;
  font-size: 14px;
  color: var(--c-text-1, #1e2a3a);
  background: #f8fafc;
  border: 1px solid var(--c-border, #e3e8ef);
  border-radius: var(--r-input, 6px);
  transition: border-color var(--t-fast, 0.2s ease), background-color var(--t-fast, 0.2s ease),
    box-shadow var(--t-fast, 0.2s ease);
}
.login-form ::v-deep .el-input__inner::placeholder {
  color: var(--c-text-4, #9aa6b5);
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
  color: var(--c-text-3, #66748a);
  transition: color var(--t-fast, 0.2s ease);
}
.login-form ::v-deep .el-input__icon {
  width: 16px;
  font-size: 16px;
  line-height: 44px;
}
.login-form ::v-deep .el-input__suffix {
  right: 10px;
}
.login-form ::v-deep .el-input__suffix-inner {
  display: flex;
  align-items: center;
  height: 44px;
}
.login-form ::v-deep .el-input__clear,
.login-form ::v-deep .el-input__password {
  font-size: 14px;
  color: var(--c-text-3, #66748a);
  transition: color var(--t-fast, 0.2s ease);
}
.login-form ::v-deep .el-input__clear:hover,
.login-form ::v-deep .el-input__password:hover {
  color: var(--c-primary, #2b6cb0);
}
.login-form ::v-deep .el-input:focus-within .el-input__prefix {
  color: var(--c-primary, #2b6cb0);
}
.login-form ::v-deep .el-form-item__error {
  padding-top: 5px;
  font-size: 12px;
}
.submit-item {
  margin-top: 24px;
  margin-bottom: 0 !important;
}

/* ================= 主按钮 ================= */
.login-btn.el-button--primary {
  position: relative;
  overflow: hidden;
  width: 100%;
  height: 44px;
  padding: 0;
  border: none;
  border-radius: var(--r-btn, 6px);
  font-size: 15px;
  font-weight: 500;
  letter-spacing: 6px;
  color: #ffffff;
  background: var(--c-primary, #2b6cb0);
  box-shadow: 0 6px 16px rgba(36, 90, 150, 0.26);
  transition: background-color var(--t-fast, 0.2s ease), box-shadow var(--t-fast, 0.2s ease),
    transform var(--t-fast, 0.2s ease);
}
/* 悬停时掠过一道高光 */
.login-btn.el-button--primary::after {
  content: '';
  position: absolute;
  top: -50%;
  left: -70%;
  width: 45%;
  height: 200%;
  background: linear-gradient(90deg, rgba(255, 255, 255, 0) 0%, rgba(255, 255, 255, 0.22) 50%, rgba(255, 255, 255, 0) 100%);
  transform: skewX(-20deg);
  transition: left 0.55s ease;
  pointer-events: none;
}
.login-btn.el-button--primary:hover,
.login-btn.el-button--primary:focus {
  background: var(--c-primary-hover, #245a96);
  box-shadow: 0 10px 24px rgba(30, 76, 126, 0.34);
  transform: translateY(-1px);
}
.login-btn.el-button--primary:hover::after,
.login-btn.el-button--primary:focus::after {
  left: 125%;
}
.login-btn.el-button--primary:active {
  background: var(--c-primary-active, #1e4c7e);
  box-shadow: 0 4px 10px rgba(30, 76, 126, 0.24);
  transform: translateY(1px);
}

/* ================= 底部提示 ================= */
.tips {
  margin-top: 20px;
  padding-top: 14px;
  border-top: 1px dashed var(--c-border-light, #eef1f6);
  text-align: center;
  font-size: 12.5px;
  line-height: 1.6;
  letter-spacing: 0.3px;
  color: var(--c-text-3, #66748a);
  animation: fadeIn 0.5s ease-out 0.36s both;
}

/* ================= 入场动效 ================= */
@keyframes riseIn {
  from {
    opacity: 0;
    transform: translateY(12px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

/* ================= 响应式 ================= */
@media (max-width: 768px) {
  .login-container {
    padding: 26px 14px;
  }
  .login-card {
    width: 88vw;
    max-width: 420px;
    padding: 32px 24px 20px;
  }
  .card-accent {
    left: 24px;
    right: 24px;
  }
  .title {
    font-size: 19px;
    letter-spacing: 1px;
  }
  .logo-seal {
    width: 50px;
    height: 50px;
  }
  .logo-svg {
    width: 24px;
    height: 24px;
  }
  .bg-ring--outer,
  .bg-chain {
    display: none;
  }
  .bg-ring--mid {
    width: 460px;
    height: 460px;
  }
  .bg-ring--inner {
    width: 300px;
    height: 300px;
  }
}

@media (max-height: 640px) and (min-width: 769px) {
  .login-card {
    padding: 28px 38px 20px;
  }
  .login-header {
    margin-bottom: 14px;
  }
  .logo-seal {
    width: 46px;
    height: 46px;
  }
  .logo-svg {
    width: 22px;
    height: 22px;
  }
  .title {
    margin-top: 12px;
    font-size: 20px;
  }
  .login-divider {
    margin-bottom: 16px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .login-card,
  .login-header,
  .login-divider,
  .login-form ::v-deep .el-form-item,
  .tips {
    animation: none;
  }
  .bg-ring--mid {
    animation: none;
  }
  .login-btn.el-button--primary::after {
    display: none;
  }
}
</style>
