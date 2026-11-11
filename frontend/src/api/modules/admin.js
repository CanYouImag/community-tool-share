// 管理后台 API（社区管理 / 调解 / 平台管理 / 仲裁 / 举报 / 日志）
import http from '../request'

// —— 社区管理员 ——
export const getMyAdminCommunity = () => http.get('/communities/my-admin')
export const getJoinApplies = (id, params) => http.get(`/communities/${id}/join-applies`, { params })
export const auditJoinApply = (id, applyId, data) => http.post(`/communities/${id}/join-applies/${applyId}/audit`, data)
export const getMembers = (id, params) => http.get(`/communities/${id}/members`, { params })
export const kickMember = (id, userId) => http.delete(`/communities/${id}/members/${userId}`)
export const getAnnouncements = (id) => http.get(`/communities/${id}/announcements`)
export const publishAnnouncement = (id, data) => http.post(`/communities/${id}/announcements`, data)
export const deleteAnnouncement = (id, announcementId) => http.delete(`/communities/${id}/announcements/${announcementId}`)
export const getCommunityStats = () => http.get('/admin/community-stats')

// —— 纠纷（调解 / 仲裁）——
export const getDisputes = (params) => http.get('/disputes', { params })
export const getDisputeDetail = (id) => http.get(`/disputes/${id}`)
export const mediateDispute = (id, data) => http.post(`/disputes/${id}/messages`, data)
export const resolveDispute = (id, data) => http.post(`/disputes/${id}/resolve`, data)
export const escalateDispute = (id, data) => http.post(`/disputes/${id}/escalate`, data)
export const arbitrateDispute = (id, data) => http.post(`/disputes/${id}/arbitrate`, data)

// —— 举报 ——
export const getReports = (params) => http.get('/reports', { params })
export const handleReport = (id, data) => http.post(`/reports/${id}/handle`, data)

// —— 平台管理 ——
export const getPlatformCommunities = (params) => http.get('/admin/communities', { params })
export const createCommunity = (data) => http.post('/admin/communities', data)
export const updateCommunity = (id, data) => http.put(`/admin/communities/${id}`, data)
export const appointAdmin = (id, data) => http.post(`/admin/communities/${id}/admin`, data)
export const dismissAdmin = (id, userId) => http.delete(`/admin/communities/${id}/admin/${userId}`)
export const getUsers = (params) => http.get('/admin/users', { params })
export const banUser = (userId, data) => http.post(`/admin/users/${userId}/ban`, data)
export const unbanUser = (userId, data) => http.post(`/admin/users/${userId}/unban`, data)
export const getConfig = () => http.get('/admin/config')
export const updateConfig = (data) => http.put('/admin/config', data)
export const getPlatformStats = () => http.get('/admin/stats')
export const getLogs = (params) => http.get('/admin/logs', { params })
