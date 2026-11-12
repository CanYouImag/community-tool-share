import http from '@/api/request';

// 订单相关 API

export const getOrders = (params) => http.get('/orders', { params });

export const getOrder = (id) => http.get(`/orders/${id}`);

export const getOrderMessages = (id) => http.get(`/orders/${id}/messages`);

export const postOrderMessage = (id, content) => http.post(`/orders/${id}/messages`, { content });

// 最小订单载体：发起纠纷时选择我的订单（真实后端）
export const getMyOrders = () => http.get('/orders/mine');
