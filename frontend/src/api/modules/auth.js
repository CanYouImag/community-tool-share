// 认证与个人资料 API
import http from '../request';

export const login = (data) => http.post('/v1/auth/login', data);

export const register = (data) => http.post('/v1/auth/register', data);

export const logout = () => http.post('/v1/auth/logout');

export const getMe = () => http.get('/v1/users/me');

export const updateProfile = (data) => http.put('/v1/users/me', data);

export const changePassword = (data) => http.put('/v1/users/me/password', data);

export const submitRealName = (data) => http.post('/v1/users/me/real-name', data);

export const updateAvatar = (avatarUrl) => http.put('/v1/users/me/avatar', { avatarUrl });
