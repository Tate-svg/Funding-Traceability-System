import axios from 'axios'
import request from '@/utils/request'

/**
 * 系统内部接口：为指定资助单生成溯源核验二维码
 * 需登录 + fund:view 权限；资助人仅可生成本人名下单据
 */
export function getFundQrCode(id) {
  return request({
    url: '/funds/' + id + '/qrcode',
    method: 'get'
  })
}

/**
 * 公开核验接口：独立 axios 实例，与系统内部请求完全隔离
 * - 不附加 Authorization（扫码人员无需登录）
 * - 不弹全局 Message、401 不跳登录页
 * - 仅用于读取公开只读核验数据
 */
const publicApi = axios.create({
  baseURL: '/api',
  timeout: 15000
})

export function publicVerify(code) {
  return publicApi
    .get('/public/verify', {
      params: { code },
      headers: { 'Cache-Control': 'no-cache' }
    })
    .then(response => {
      const body = response.data || {}
      if (body.code === 200) {
        return body.data
      }
      const err = new Error(body.message || '核验失败，请稍后重试')
      err.isBusinessError = true
      err.code = body.code
      throw err
    })
    .catch(error => {
      // 业务错误（上方主动抛出）直接透传
      if (error && error.isBusinessError) {
        throw error
      }
      const data = error && error.response && error.response.data
      const err = new Error((data && data.message) || '网络异常，请检查网络后重试')
      err.code = (error && error.response && error.response.status) || 0
      throw err
    })
}
