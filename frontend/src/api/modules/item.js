import http from '../request';

// 物品列表获取
export const getItems = (params = {}) => http.get('/v1/items', { params });
// 单件物品查看
export const getItem = (id) => http.get(`/v1/items/${encodeURIComponent(id)}`);
