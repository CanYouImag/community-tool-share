import http from '@/api/request';

// 订单相关 API

export const getOrders = (params) => http.get('/orders', { params });

export const getOrder = (id) => http.get(`/orders/${id}`);

export const getOrderMessages = (id) => http.get(`/orders/${id}/messages`);

export const postOrderMessage = (id, content) => http.post(`/orders/${id}/messages`, { content });

// 发起纠纷时选择「我是物主（出租者）」的订单（完整订单域，role=lessor）
export const getMyOrders = () => http.get('/orders', { params: { role: 'lessor', size: 100 } });
