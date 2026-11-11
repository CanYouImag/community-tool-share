import http from '../request';

// 物品列表获取
export const getItems = (params = {}) => http.get('/v1/items', { params });
// 单件物品查看
export const getItem = (id) => http.get(`/v1/items/${encodeURIComponent(id)}`);
// 收藏物品
export const favoriteItem = (id) => http.post(`/v1/items/${encodeURIComponent(id)}/favorite`);
// 取消收藏
export const unfavoriteItem = (id) => http.delete(`/v1/items/${encodeURIComponent(id)}/favorite`);
