// 纠纷与举报相关 API
import http from '../request';

export const getDispute = () => http.get('/dispute');

export const submitDispute = (payload) => http.post('/dispute', payload);

export const getAward = () => http.get('/dispute/award');

export const submitReport = (payload) => http.post('/reports', payload);
