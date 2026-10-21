import Vue from 'vue'
import App from './App.vue'
import router from './router'
import store from './store'
import ElementUI from 'element-ui'
import 'element-ui/lib/theme-chalk/index.css'
import './styles/theme.css'
import './styles/responsive.css'
import permission from './utils/permission'

Vue.use(ElementUI, { size: 'small' })

// 注册全局 v-permission 指令
Vue.directive('permission', permission)

Vue.config.productionTip = false

new Vue({
  router,
  store,
  render: h => h(App)
}).$mount('#app')
