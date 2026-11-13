import http from '@/api/request';

// 纠纷与举报 API（对接后端 /disputes、/reports）

// 发起纠纷（UC-F1）
export const submitDispute = (payload) => http.post('/disputes', payload);

// 租客回应物主扣留押金（同意/不同意）
export const respondDispute = (id, payload) => http.post(`/disputes/${id}/respond`, payload);

// 我的纠纷列表（用户视角）
export const getMyDisputes = () => http.get('/disputes/mine');

// 纠纷详情（含留言、举证、裁决书）
export const getDisputeDetail = (id) => http.get(`/disputes/${id}`);

// 裁决书（UC-F6，双方可见）
export const getAward = (id) => http.get(`/disputes/${id}/award`);

// 提交举报（UC-F7）
export const submitReport = (payload) => http.post('/reports', payload);

// 我的举报
export const getMyReports = () => http.get('/reports/mine');
