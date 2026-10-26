import Vue from 'vue'
import VueRouter from 'vue-router'
import store from '@/store'
import { getToken } from '@/utils/auth'
import Layout from '@/layout/Layout.vue'

Vue.use(VueRouter)

// 静态路由：公开核验页、登录页、404 页
export const constantRoutes = [
  {
    // 扫码公开核验页：不经过 Layout，无需登录，只读
    path: '/verify/:code',
    name: 'PublicVerify',
    component: () => import('@/views/public/Verify.vue'),
    meta: { public: true, title: '资助凭证核验' },
    hidden: true
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    hidden: true
  },
  {
    path: '/404',
    name: 'NotFound',
    component: () => import('@/views/404.vue'),
    hidden: true
  }
]

// 角色首页映射：登录后 / 访问根路径时按角色跳转
export const roleHomeMap = {
  AUDIT_ADMIN: '/audit',
  DISBURSE_ADMIN: '/disburse',
  SUPERVISOR: '/review',
  SPONSOR: '/dashboard'
}

const ALL_ROLES = ['AUDIT_ADMIN', 'DISBURSE_ADMIN', 'SUPERVISOR', 'SPONSOR']

// 动态路由：根据角色过滤后动态挂载
export const asyncRoutes = [
  {
    path: '/',
    component: Layout,
    redirect: () => roleHomeMap[store.getters.role] || '/login',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: { title: '首页', icon: 'el-icon-s-home', roles: ['SPONSOR'] }
      },
      {
        path: 'audit',
        name: 'AuditWorkbench',
        component: () => import('@/views/audit/AuditWorkbench.vue'),
        meta: { title: '审核工作台', icon: 'el-icon-s-check', roles: ['AUDIT_ADMIN'] }
      },
      {
        path: 'disburse',
        name: 'DisburseWorkbench',
        component: () => import('@/views/disburse/DisburseWorkbench.vue'),
        meta: { title: '拨付工作台', icon: 'el-icon-bank-card', roles: ['DISBURSE_ADMIN'] }
      },
      {
        path: 'review',
        name: 'ReviewPool',
        component: () => import('@/views/review/ReviewPool.vue'),
        meta: { title: '复核抽检池', icon: 'el-icon-view', roles: ['SUPERVISOR'] }
      },
      {
        path: 'trace',
        name: 'TraceLedger',
        component: () => import('@/views/trace/TraceLedger.vue'),
        meta: { title: '溯源台账', icon: 'el-icon-stamp', roles: ['AUDIT_ADMIN', 'SUPERVISOR'] }
      },
      {
        path: 'privacy',
        name: 'PrivacyApprove',
        component: () => import('@/views/privacy/PrivacyApprove.vue'),
        meta: { title: '隐私审批', icon: 'el-icon-lock', roles: ['AUDIT_ADMIN', 'SUPERVISOR', 'SPONSOR'] }
      },
      {
        path: 'operation-log',
        name: 'OperationLog',
        component: () => import('@/views/log/OperationLog.vue'),
        meta: { title: '操作日志', icon: 'el-icon-time', roles: ['SUPERVISOR'] }
      },
      {
        path: 'student',
        name: 'Student',
        component: () => import('@/views/student/Student.vue'),
        meta: { title: '学生管理', icon: 'el-icon-user', roles: ['AUDIT_ADMIN', 'SPONSOR'] }
      },
      {
        path: 'fund',
        name: 'Fund',
        component: () => import('@/views/fund/Fund.vue'),
        meta: { title: '资金管理', icon: 'el-icon-money', roles: ALL_ROLES }
      },
      {
        path: 'fund/detail/:id',
        name: 'FundDetail',
        component: () => import('@/views/fund/FundDetail.vue'),
        meta: { title: '资助单详情', roles: ALL_ROLES, activeMenu: '/fund' },
        hidden: true
      },
      {
        path: 'visit',
        name: 'Visit',
        component: () => import('@/views/visit/Visit.vue'),
        meta: { title: '回访管理', icon: 'el-icon-chat-line-round', roles: ['AUDIT_ADMIN', 'SPONSOR', 'SUPERVISOR'] }
      },
      {
        path: 'score',
        name: 'Score',
        component: () => import('@/views/score/Score.vue'),
        meta: { title: '评分管理', icon: 'el-icon-document-checked', roles: ['AUDIT_ADMIN', 'SPONSOR'] }
      },
      {
        path: 'score-item',
        name: 'ScoreItem',
        component: () => import('@/views/score/ScoreItem.vue'),
        meta: { title: '评分指标', icon: 'el-icon-set-up', roles: ['AUDIT_ADMIN'] }
      },
      {
        path: 'user',
        name: 'User',
        component: () => import('@/views/user/User.vue'),
        meta: { title: '用户管理', icon: 'el-icon-s-custom', roles: ['AUDIT_ADMIN'] }
      }
    ]
  },
  // 404 兜底（必须放在最后，避免在动态路由加载前被 * 捕获）
  { path: '*', redirect: '/404', hidden: true }
]

// 根据角色递归过滤动态路由
export function filterAsyncRoutes(routes, role) {
  const res = []
  routes.forEach(route => {
    const tmp = { ...route }
    if (tmp.children) {
      tmp.children = filterAsyncRoutes(tmp.children, role)
    }
    const roles = tmp.meta && tmp.meta.roles
    if (!roles || roles.length === 0) {
      // 未限定角色，直接放行
      res.push(tmp)
    } else if (roles.includes(role)) {
      res.push(tmp)
    }
  })
  return res
}

const createRouter = () => new VueRouter({
  scrollBehavior: () => ({ y: 0 }),
  routes: constantRoutes
})

const router = createRouter()

// 重置路由（退出登录后清空动态路由）
export function resetRouter() {
  const newRouter = createRouter()
  router.matcher = newRouter.matcher
}

// 全局前置守卫
const whiteList = ['/login', '/404']

router.beforeEach(async (to, from, next) => {
  // 公开核验页：扫码人员无需登录，也不加载系统内部动态路由
  if (to.meta && to.meta.public) {
    next()
    return
  }
  const hasToken = getToken()
  if (hasToken) {
    if (to.path === '/login') {
      // 已登录，禁止再进入登录页
      next({ path: '/' })
      return
    }
    const hasRole = store.getters.role && store.getters.role.length > 0
    if (hasRole) {
      next()
    } else {
      try {
        // 无角色信息，先拉取用户信息
        await store.dispatch('user/getInfo')
        // 根据角色过滤路由并动态挂载
        const accessRoutes = filterAsyncRoutes(asyncRoutes, store.getters.role)
        store.commit('user/SET_ACCESS_ROUTES', accessRoutes)
        router.addRoutes(accessRoutes)
        // hack：重新导航，确保新路由生效
        next({ ...to, replace: true })
      } catch (error) {
        // 拉取信息失败，清除状态并跳登录
        await store.dispatch('user/resetToken')
        next(`/login?redirect=${to.path}`)
      }
    }
  } else {
    if (whiteList.includes(to.path)) {
      next()
    } else {
      next(`/login?redirect=${to.path}`)
    }
  }
})

export default router
