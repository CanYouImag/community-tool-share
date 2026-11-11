// 社区相关 API
import http from '../request';

export const getCommunities = () => http.get('/communities');

export const getCommunityHome = (id) => http.get(`/communities/home/${id}`);

export const getMyMemberships = () => http.get('/memberships/mine');

export const joinCommunity = (id) => http.post(`/communities/${id}/join`);

export const quitCommunity = (id) => http.post(`/communities/${id}/quit`);
