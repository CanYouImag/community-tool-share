// Mock 路由分发：根据 method + url 返回假数据
import { useUserStore } from '../../store/user';
import { ITEM_CATEGORY } from '../../utils/constants';
import * as db from './data';

const favoriteItemIds = new Set();

function setMockFavorite(id, favorited) {
  const item = db.items.find((item) => item.id === id);

  if (!item) {
    throw new Error('物品不存在');
  }

  if (favorited) {
    favoriteItemIds.add(id);
  } else {
    favoriteItemIds.delete(id);
  }

  return { favorited: favoriteItemIds.has(id) };
}

const delay = (data, ms = 200) => new Promise((resolve) => setTimeout(() => resolve(data), ms));

function validateItemInput(params, existingImages = []) {
  const user = useUserStore();

  if (!user.isCertified) {
    throw new Error('请先完成实名认证');
  }

  const membership = db.myMemberships.find(
    (entry) => entry.id === params.communityId && entry.status === 'JOINED',
  );
  const community = db.communities.find((entry) => entry.id === params.communityId);

  if (!membership || !community) {
    throw new Error('只能向已加入的社区发布物品');
  }

  const title = String(params.title || '').trim();
  const condition = String(params.condition || '').trim();
  const description = String(params.description || '').trim();

  if (!title || !condition || !description) {
    throw new Error('请填写标题、成色说明和物品描述');
  }

  if (!ITEM_CATEGORY.includes(params.category)) {
    throw new Error('物品分类无效');
  }

  const moneyPattern = /^\d+(?:\.\d{1,2})?$/;
  const dailyPrice = Number(params.dailyPrice);
  const deposit = Number(params.deposit);

  if (
    !moneyPattern.test(String(params.dailyPrice)) ||
    !moneyPattern.test(String(params.deposit)) ||
    !Number.isFinite(dailyPrice) ||
    !Number.isFinite(deposit) ||
    dailyPrice <= 0 ||
    deposit < 0
  ) {
    throw new Error('租金必须大于零，押金不能为负，金额最多两位小数');
  }

  if (deposit > 500 && user.credit < 60) {
    throw new Error('信用分不足，无法发布高押金物品');
  }

  const pictures = params.images;

  if (
    !Array.isArray(pictures) ||
    pictures.length === 0 ||
    pictures.some(
      (image) =>
        typeof image !== 'string' ||
        (!existingImages.includes(image) && !/^data:image\/(?:jpeg|png|webp);base64,/.test(image)),
    )
  ) {
    throw new Error('请至少保留或上传一张有效图片');
  }

  return {
    title,
    category: params.category,
    condition,
    description,
    dailyPrice,
    deposit,
    communityId: community.id,
    communityName: community.name,
    image: pictures[0],
    images: [...pictures],
  };
}

// 按 url 匹配处理器
const handlers = [
  {
    method: 'GET',
    pattern: /^\/v1\/items\/mine$/,
    handler: (params) => {
      const user = useUserStore();

      if (!user.isLoggedIn) {
        throw new Error('请先登录');
      }

      const records = db.items.filter(
        (item) => item.ownerId === user.id && (!params.status || item.status === params.status),
      );

      return {
        records,
        total: records.length,
        page: 1,
        size: 10,
      };
    },
  },
  {
    method: 'GET',
    pattern: /^\/v1\/items$/,
    handler: (params) => {
      const keyword = (params.keyword || '').trim().toLowerCase();
      const category = params.category || '';
      const favoritesOnly = params.favoritesOnly === true || params.favoritesOnly === 'true';

      const records = db.items.filter((item) => {
        const matchesKeyword = !keyword || item.title.toLowerCase().includes(keyword);
        const matchesCategory = !category || item.category === category;
        const matchesFavorite = !favoritesOnly || favoriteItemIds.has(item.id);
        const isAvailable = (item.status ?? 'AVAILABLE') === 'AVAILABLE';
        return isAvailable && matchesKeyword && matchesCategory && matchesFavorite;
      });

      return {
        records,
        total: records.length,
        page: 1,
        size: 10,
      };
    },
  },
  {
    method: 'POST',
    pattern: /^\/v1\/items$/,
    handler: (params) => {
      const user = useUserStore();
      const fields = validateItemInput(params);

      const item = {
        ...fields,
        id: `i_${crypto.randomUUID()}`,
        ownerId: user.id,
        owner: user.name,
        ownerCredit: user.credit,
        status: 'PENDING_REVIEW',
        createdAt: new Date().toISOString(),
      };

      db.items.unshift(item);

      return { id: item.id, status: item.status };
    },
  },
  {
    method: 'DELETE',
    pattern: /^\/v1\/items\/([^/]+)$/,
    handler: (_params, matches) => {
      const user = useUserStore();
      const id = decodeURIComponent(matches[1]);

      if (!user.isCertified) {
        throw new Error('请先完成实名认证');
      }

      const index = db.items.findIndex((item) => item.id === id);
      const item = db.items[index];

      if (!item || item.ownerId !== user.id) {
        throw new Error('物品不存在或无权操作');
      }

      // 只有明确结束的订单才不会阻止删除
      const finishedStatuses = ['COMPLETED', 'CANCELLED', 'CLOSED'];
      const hasActiveOrder = [...db.orders, ...db.lendOrders].some(
        (order) => order.itemId === id && !finishedStatuses.includes(order.status),
      );

      if (hasActiveOrder) {
        throw new Error('该物品有进行中的订单，暂不能删除');
      }

      db.items.splice(index, 1);
      favoriteItemIds.delete(id);

      return { id, deleted: true };
    },
  },
  {
    method: 'PUT',
    pattern: /^\/v1\/items\/([^/]+)$/,
    handler: (params, matches) => {
      const user = useUserStore();
      const id = decodeURIComponent(matches[1]);
      const item = db.items.find((item) => item.id === id);

      if (!user.isCertified) {
        throw new Error('请先完成实名认证');
      }

      if (!item || item.ownerId !== user.id) {
        throw new Error('物品不存在或无权操作');
      }

      const editableStatuses = ['PENDING_REVIEW', 'AVAILABLE', 'REJECTED', 'PAUSED'];

      if (!editableStatuses.includes(item.status ?? 'AVAILABLE')) {
        throw new Error('当前状态不允许编辑');
      }

      const existingImages = item.images || (item.image ? [item.image] : []);
      const fields = validateItemInput(params, existingImages);

      // 全部校验通过后才修改原记录
      Object.assign(item, fields, {
        status: 'PENDING_REVIEW',
        updatedAt: new Date().toISOString(),
      });

      return { id: item.id, status: item.status };
    },
  },
  {
    method: 'GET',
    pattern: /^\/v1\/items\/mine\/([^/]+)$/,
    handler: (_params, matches) => {
      const user = useUserStore();
      const id = decodeURIComponent(matches[1]);
      const item = db.items.find((item) => item.id === id);

      if (!user.isCertified) {
        throw new Error('请先完成实名认证');
      }

      if (!item || item.ownerId !== user.id) {
        throw new Error('物品不存在或无权操作');
      }

      if (item.status === 'REMOVED') {
        throw new Error('违规下架的物品不能编辑');
      }

      return {
        ...item,
        images: [...(item.images || (item.image ? [item.image] : []))],
      };
    },
  },
  {
    method: 'PATCH',
    pattern: /^\/v1\/items\/([^/]+)\/status$/,
    handler: (params, matches) => {
      const user = useUserStore();
      const id = decodeURIComponent(matches[1]);
      const item = db.items.find((item) => item.id === id);

      if (!user.isCertified) {
        throw new Error('请先完成实名认证');
      }

      if (!item || item.ownerId !== user.id) {
        throw new Error('物品不存在或无权操作');
      }

      const currentStatus = item.status ?? 'AVAILABLE';
      const nextStatus = params.status;

      const canPause = currentStatus === 'AVAILABLE' && nextStatus === 'PAUSED';
      const canResume = currentStatus === 'PAUSED' && nextStatus === 'AVAILABLE';

      if (!canPause && !canResume) {
        throw new Error('当前状态不允许此操作，请刷新后重试');
      }

      item.status = nextStatus;
      item.updatedAt = new Date().toISOString();

      return { id: item.id, status: item.status };
    },
  },
  {
    method: 'POST',
    pattern: /^\/v1\/items\/([^/]+)\/favorite$/,
    handler: (_params, matches) => setMockFavorite(decodeURIComponent(matches[1]), true),
  },
  {
    method: 'DELETE',
    pattern: /^\/v1\/items\/([^/]+)\/favorite$/,
    handler: (_params, matches) => setMockFavorite(decodeURIComponent(matches[1]), false),
  },
  {
    method: 'GET',
    pattern: /^\/v1\/items\/([^/]+)$/,
    handler: (_params, matches) => {
      const id = decodeURIComponent(matches[1]);
      const item = db.items.find((item) => item.id === id);
      if (!item || (item.status ?? 'AVAILABLE') !== 'AVAILABLE') {
        throw new Error('物品不存在或尚未上架');
      }
      return {
        ...item,
        favorited: favoriteItemIds.has(id),
      };
    },
  },
  {
    method: 'GET',
    pattern: /^\/communities$/,
    handler: () => db.communities,
  },
  {
    method: 'GET',
    pattern: /^\/communities\/home\/(.+)$/,
    handler: (_params, matches) => {
      const cid = matches[1];
      const home = db.communityHome[cid] || { top: [], latest: [] };
      const community = db.communities.find((c) => c.id === cid);
      const resolve = (ids) =>
        ids
          .map((id) => db.items.find((item) => item.id === id))
          .filter((item) => item && (item.status ?? 'AVAILABLE') === 'AVAILABLE');
      return { ...community, top: resolve(home.top), latest: resolve(home.latest) };
    },
  },
  {
    method: 'GET',
    pattern: /^\/memberships\/mine$/,
    handler: () => db.myMemberships,
  },
  {
    method: 'POST',
    pattern: /^\/communities\/(.+)\/join$/,
    handler: () => ({ id: 'c_1002', status: 'PENDING' }),
  },
  {
    method: 'GET',
    pattern: /^\/orders$/,
    handler: (params) => {
      const role = params.role || 'borrow';
      const list = role === 'lend' ? db.lendOrders : db.orders;
      let result = [...list];
      if (params.keyword) {
        const kw = params.keyword.toLowerCase();
        result = result.filter((o) => o.id.toLowerCase().includes(kw) || o.itemTitle.includes(kw));
      }
      if (params.status) {
        result = result.filter((o) => o.status === params.status);
      }
      return { list: result, total: result.length };
    },
  },
  {
    method: 'GET',
    pattern: /^\/orders\/(.+)$/,
    handler: (_params, matches) => {
      const order = db.orders.find((o) => o.id === matches[1]) || db.orders[0];
      return order;
    },
  },
  {
    method: 'GET',
    pattern: /^\/orders\/(.+)\/messages$/,
    handler: () => [
      {
        id: 'm_1',
        from: '陈禾',
        mine: false,
        time: '2026-10-10 09:30',
        content: '取物时间我们约在周六上午可以吗？',
      },
      {
        id: 'm_2',
        from: 'Starling',
        mine: true,
        time: '2026-10-10 09:45',
        content: '可以，周六上午 10 点小区门口见。',
      },
    ],
  },
  {
    method: 'POST',
    pattern: /^\/orders\/(.+)\/messages$/,
    handler: () => ({ ok: true }),
  },
  {
    method: 'GET',
    pattern: /^\/wallet$/,
    handler: () => db.wallet,
  },
  {
    method: 'GET',
    pattern: /^\/wallet\/flows$/,
    handler: (params) => {
      let result = [...db.flows];
      if (params.type) {
        result = result.filter((f) => f.type === params.type);
      }
      return { list: result, total: result.length };
    },
  },
  {
    method: 'GET',
    pattern: /^\/wallet\/deposits$/,
    handler: () => db.deposits,
  },
  {
    method: 'GET',
    pattern: /^\/dispute$/,
    handler: () => db.dispute,
  },
  {
    method: 'POST',
    pattern: /^\/dispute$/,
    handler: () => ({ id: db.dispute.id }),
  },
  {
    method: 'GET',
    pattern: /^\/dispute\/award$/,
    handler: () => db.award,
  },
  {
    method: 'POST',
    pattern: /^\/reports$/,
    handler: () => ({ ok: true }),
  },
];

// 匹配并返回结果，未匹配返回 null
export async function matchMock(method, url, params) {
  const [path, query] = url.split('?');
  const q = new URLSearchParams(query || '');
  const mergedParams = { ...params, ...Object.fromEntries(q.entries()) };

  for (const h of handlers) {
    if (h.method !== method.toUpperCase()) continue;
    const m = path.match(h.pattern);
    if (m) {
      return delay(await h.handler(mergedParams, m));
    }
  }
  return null;
}
