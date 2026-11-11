// 钱包相关 API
import http from '../request';

export const getWallet = () => http.get('/wallet');

export const getFlows = (params) => http.get('/wallet/flows', { params });

export const getDeposits = () => http.get('/wallet/deposits');
