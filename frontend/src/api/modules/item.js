import http from '@/api/request';

// 物品相关 API（对齐后端 ItemController /api/v1/items）

// 搜索与筛选物品列表
export const getItems = (params = {}) => http.get('/v1/items', { params });
// 单件物品查看（编辑时复用，物主可查看自己非上架物品）
export const getItem = (id) => http.get(`/v1/items/${encodeURIComponent(id)}`);
// 分类列表（C1）
export const getCategories = () => http.get('/v1/items/categories');
// 图片上传（multipart），返回 { url }
export const uploadItemImage = (file) => {
  const form = new FormData();
  form.append('file', file);
  return http.post('/v1/items/images', form);
};
// 收藏 / 取消收藏
export const favoriteItem = (id) => http.post(`/v1/items/${encodeURIComponent(id)}/favorite`);
export const unfavoriteItem = (id) => http.delete(`/v1/items/${encodeURIComponent(id)}/favorite`);
// 我的收藏
export const getFavorites = (params = {}) => http.get('/v1/items/favorites', { params });
// 我的物品
export const getMyItems = (params = {}) => http.get('/v1/items/mine', { params });
export const createItem = (data) => http.post('/v1/items', data);
export const updateItem = (id, data) => http.put(`/v1/items/${encodeURIComponent(id)}`, data);
// 上架/下架（C7，后端 PUT /items/{id}/shelf）
export const shelfItem = (id, onShelf) =>
  http.put(`/v1/items/${encodeURIComponent(id)}/shelf`, { onShelf });
// 删除自己的物品
export const deleteItem = (id) => http.delete(`/v1/items/${encodeURIComponent(id)}`);
