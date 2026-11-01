// 订单相关 API
import http from '../request'

export const getOrders = (params) => http.get('/orders', { params })

export const getOrder = (id) => http.get(`/orders/${id}`)

export const getOrderMessages = (id) => http.get(`/orders/${id}/messages`)

export const postOrderMessage = (id, content) =>
  http.post(`/orders/${id}/messages`, { content })
