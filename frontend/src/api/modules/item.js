import http from '../request';

// 物品列表获取
export const getItems = () => http.get('/v1/items');
