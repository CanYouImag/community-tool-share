import http from '@/api/request';

// 钱包相关 API

export const getWallet = () => http.get('/wallet');

export const getFlows = (params) => http.get('/wallet/flows', { params });

export const getDeposits = () => http.get('/wallet/deposits');
