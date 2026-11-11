import http from '@/api/request';

// 社区相关 API

export const getCommunities = () => http.get('/communities');

export const getCommunityHome = (id) => http.get(`/communities/home/${id}`);

export const getMyMemberships = () => http.get('/memberships/mine');

export const joinCommunity = (id) => http.post(`/communities/${id}/join`);

export const quitCommunity = (id) => http.post(`/communities/${id}/quit`);
