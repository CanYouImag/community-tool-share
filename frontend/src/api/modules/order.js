import http from '@/api/request';

// 订单相关 API

export const getOrders = (params) => http.get('/orders', { params });

export const getOrder = (id) => http.get(`/orders/${id}`);

export const getOrderMessages = (id) => http.get(`/orders/${id}/messages`);

export const postOrderMessage = (id, content) => http.post(`/orders/${id}/messages`, { content });

export const createOrder = (data) => http.post('/v1/orders', data);

export const payOrder = (id) => http.post(`/v1/orders/${id}/pay`);

export const acceptOrder = (id) => http.post(`/v1/orders/${id}/accept`);

export const rejectOrder = (id, reason) =>
  http.post(`/v1/orders/${id}/reject`, { reason });

export const pickupOrder = (id, code) => http.post(`/v1/orders/${id}/pickup`, { code });

export const returnOrder = (id) => http.post(`/v1/orders/${id}/return`);

export const confirmReturn = (id) => http.post(`/v1/orders/${id}/confirm-return`);

export const inspectOrder = (id, pass, reason = '') =>
  http.post(`/v1/orders/${id}/inspect`, { pass, reason });

export const cancelOrder = (id, reason) =>
  http.post(`/v1/orders/${id}/cancel`, { reason });

// 发起纠纷时选择「我是物主（出租者）」的订单（完整订单域，role=lessor）
export const getMyOrders = () => http.get('/orders', { params: { role: 'lessor', size: 100 } });
