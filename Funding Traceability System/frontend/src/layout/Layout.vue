<template>
  <el-container class="app-wrapper">
    <!-- 顶部导航 -->
    <el-header class="app-header">
      <div class="header-left">
        <i class="el-icon-s-fold hamburger" @click="sidebarOpen = !sidebarOpen"></i>
        <i class="el-icon-school logo-icon"></i>
        <span class="system-title">助学帮扶管理系统</span>
      </div>
      <div class="header-right">
        <span class="user-name">{{ realName || username }}</span>
        <el-tag :type="roleTagType" size="small" class="role-tag">{{ roleText }}</el-tag>
        <el-dropdown @command="handleCommand">
          <span class="el-dropdown-link">
            <i class="el-icon-arrow-down"></i>
          </span>
          <el-dropdown-menu slot="dropdown">
            <el-dropdown-item command="logout" icon="el-icon-switch-button">退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </el-dropdown>
      </div>
    </el-header>

    <el-container class="main-container">
      <!-- 移动端抽屉遮罩 -->
      <div class="sidebar-mask" v-show="sidebarOpen" @click="sidebarOpen = false"></div>
      <!-- 侧边栏菜单 -->
      <el-aside width="210px" class="app-sidebar" :class="{ 'mobile-open': sidebarOpen }">
        <el-menu
          :default-active="activeMenu"
          router
          background-color="#304156"
          text-color="#bfcbd9"
          active-text-color="#409EFF"
          @select="onMenuSelect"
        >
          <template v-for="route in menuRoutes">
            <el-menu-item
              v-if="!route.hidden"
              :key="route.path"
              :index="resolvePath(route)"
            >
              <i v-if="route.meta && route.meta.icon" :class="route.meta.icon"></i>
              <span slot="title">{{ route.meta ? route.meta.title : route.name }}</span>
            </el-menu-item>
          </template>
        </el-menu>
      </el-aside>

      <!-- 主内容区 -->
      <el-main class="app-main">
        <!-- 移动端友好提示（仅小屏显示，可关闭） -->
        <div v-if="!tipClosed" class="mobile-tip">
          <i class="el-icon-mobile-phone"></i>
          <span>当前为移动端视图，横屏或使用电脑访问体验更佳</span>
          <i class="el-icon-close tip-close" @click="tipClosed = true"></i>
        </div>
        <transition name="fade-transform" mode="out-in">
          <router-view />
        </transition>
      </el-main>
    </el-container>
  </el-container>
</template>

<script>
import { mapGetters } from 'vuex'
import { resetRouter } from '@/router'

export default {
  name: 'Layout',
  data() {
    return {
      // 移动端抽屉式侧边栏开关（仅小屏生效，PC 端侧边栏常驻）
      sidebarOpen: false,
      tipClosed: false
    }
  },
  computed: {
    ...mapGetters(['realName', 'username', 'role', 'accessRoutes']),
    activeMenu() {
      const route = this.$route
      const { meta, path } = route
      if (meta && meta.activeMenu) {
        return meta.activeMenu
      }
      return path
    },
    // 侧边栏菜单项 = Layout 父路由下的可见子路由
    menuRoutes() {
      const layoutRoute = (this.accessRoutes || []).find(r => r.path === '/')
      if (layoutRoute && layoutRoute.children) {
        return layoutRoute.children
      }
      return []
    },
    roleText() {
      const map = {
        AUDIT_ADMIN: '审核管理员',
        DISBURSE_ADMIN: '拨付管理员',
        SUPERVISOR: '监事/审计',
        SPONSOR: '资助人'
      }
      return map[this.role] || this.role || '未知'
    },
    roleTagType() {
      const map = {
        AUDIT_ADMIN: 'danger',
        DISBURSE_ADMIN: 'warning',
        SUPERVISOR: 'primary',
        SPONSOR: 'success'
      }
      return map[this.role] || 'info'
    }
  },
  methods: {
    // 移动端选中菜单后自动收起抽屉
    onMenuSelect() {
      this.sidebarOpen = false
    },
    resolvePath(route) {
      // 子路由 path 已为绝对路径风格（如 dashboard），拼成 /dashboard
      const p = route.path || ''
      if (p.startsWith('/')) return p
      return '/' + p
    },
    handleCommand(command) {
      if (command === 'logout') {
        this.handleLogout()
      }
    },
    handleLogout() {
      this.$confirm('确认退出登录吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.$store.dispatch('user/logout').then(() => {
          resetRouter()
          this.$router.push('/login')
        }).catch(() => {
          resetRouter()
          this.$router.push('/login')
        })
      }).catch(() => {})
    }
  }
}
</script>

<style scoped>
.app-wrapper {
  height: 100vh;
  width: 100%;
}
.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background-color: #2b3a4d;
  color: #fff;
  height: 60px;
  line-height: 60px;
  padding: 0 20px;
}
.header-left {
  display: flex;
  align-items: center;
}
.logo-icon {
  font-size: 22px;
  color: #409EFF;
  margin-right: 10px;
}
.system-title {
  font-size: 18px;
  font-weight: 600;
  letter-spacing: 2px;
}
.header-right {
  display: flex;
  align-items: center;
}
.user-name {
  margin-right: 10px;
  font-size: 14px;
}
.role-tag {
  margin-right: 14px;
}
.el-dropdown-link {
  color: #fff;
  cursor: pointer;
  font-size: 14px;
}
.main-container {
  height: calc(100vh - 60px);
}
.app-sidebar {
  background-color: #304156;
  overflow-y: auto;
  overflow-x: hidden;
}
.app-sidebar .el-menu {
  border-right: none;
}
.app-main {
  background-color: #f0f2f5;
  padding: 16px;
  overflow-y: auto;
}
.fade-transform-enter-active,
.fade-transform-leave-active {
  transition: all 0.3s;
}
.fade-transform-enter,
.fade-transform-leave-to {
  opacity: 0;
  transform: translateX(10px);
}

/* ============ 移动端元素默认隐藏，PC 端完全保持原样 ============ */
.hamburger,
.sidebar-mask,
.mobile-tip {
  display: none;
}

/* ============ 手机端（≤768px）：汉堡抽屉式侧边栏 ============ */
@media (max-width: 768px) {
  .app-header {
    padding: 0 3vw;
    height: 52px;
    line-height: 52px;
  }
  .hamburger {
    display: inline-flex;
    font-size: 20px;
    margin-right: 3vw;
    cursor: pointer;
    padding: 6px;
  }
  .logo-icon {
    font-size: 18px;
    margin-right: 2vw;
  }
  .system-title {
    font-size: 15px;
    letter-spacing: 1px;
  }
  .user-name {
    display: none;
  }
  .main-container {
    height: calc(100vh - 52px);
    position: relative;
  }
  /* 侧边栏改为抽屉：默认移出屏外，点击汉堡滑入 */
  .app-sidebar {
    position: fixed;
    left: 0;
    top: 52px;
    bottom: 0;
    z-index: 1002;
    width: 56vw !important;
    max-width: 240px;
    transform: translateX(-100%);
    transition: transform 0.25s ease;
  }
  .app-sidebar.mobile-open {
    transform: translateX(0);
  }
  .sidebar-mask {
    display: block;
    position: fixed;
    left: 0;
    top: 52px;
    right: 0;
    bottom: 0;
    background: rgba(0, 0, 0, 0.4);
    z-index: 1001;
  }
  /* 移动端友好提示条 */
  .mobile-tip {
    display: flex;
    align-items: center;
    background: #ecf5ff;
    border: 1px solid #d9ecff;
    color: #409eff;
    border-radius: 4px;
    padding: 8px 10px;
    font-size: 12px;
    margin-bottom: 2vw;
  }
  .mobile-tip span {
    flex: 1;
    margin-left: 6px;
  }
  .tip-close {
    cursor: pointer;
    padding: 4px;
  }
}
</style>
