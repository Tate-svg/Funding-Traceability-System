import store from '@/store'

/**
 * v-permission 自定义指令
 * 用法：v-permission="['AUDIT_ADMIN']"
 * 当 store 中当前用户角色不在数组内时，移除该 DOM 元素
 */
export default {
  inserted(el, binding) {
    const { value } = binding
    const role = store.getters.role
    if (value && value instanceof Array && value.length > 0) {
      const hasPermission = value.includes(role)
      if (!hasPermission) {
        el.parentNode && el.parentNode.removeChild(el)
      }
    } else {
      // 未指定角色数组，默认不处理
    }
  }
}
