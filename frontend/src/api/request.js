// axios 统一封装
// Mock 阶段：__USE_MOCK__ 为 true 时走本地假数据；接口就绪后置为 false 即透传真实后端
import axios from 'axios'
import { matchMock } from './mock/index'

const http = axios.create({
  baseURL: '/api',
  timeout: 10000
})

// 请求拦截器：注入 token（后续对接 JWT）
http.interceptors.request.use(
  async (config) => {
    const token = localStorage.getItem('cts_token')
    if (token) {
      config.headers = config.headers || {}
      config.headers.Authorization = `Bearer ${token}`
    }

    // Mock 拦截：命中则直接返回假数据，不再发真实请求
    if (__USE_MOCK__) {
      const mockResult = await matchMock(
        config.method,
        config.url,
        config.params || config.data
      )
      if (mockResult !== null) {
        // 返回一个自定义 adapter 结果，跳过网络层
        config.adapter = () =>
          Promise.resolve({
            data: { code: 0, message: 'ok', data: mockResult },
            status: 200,
            statusText: 'OK',
            headers: {},
            config
          })
      }
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器：统一解包 { code, message, data }
http.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body && typeof body === 'object' && 'code' in body) {
      if (body.code === 0) return body.data
      return Promise.reject(new Error(body.message || '请求失败'))
    }
    return body
  },
  (error) => {
    const msg = error?.response?.data?.message || error.message || '网络错误'
    return Promise.reject(new Error(msg))
  }
)

export default http
