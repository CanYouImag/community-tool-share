import { ITEM_SORT_OPTIONS, parsePriceRange } from '@/utils/itemFilters';

const routeKeys = [
  'tab',
  'keyword',
  'category',
  'community',
  'sort',
  'minPrice',
  'maxPrice',
  'size',
  'page',
];

export function pickItemListQuery(raw = {}) {
  return Object.fromEntries(
    routeKeys.filter((key) => typeof raw[key] === 'string').map((key) => [key, raw[key]]),
  );
}

export function readItemListQuery(raw, isLoggedIn) {
  const values = pickItemListQuery(raw);
  const favoritesOnly = values.tab === 'favorites';
  const community = values.community || 'all';
  let priceRange;
  try {
    priceRange = parsePriceRange(values.minPrice, values.maxPrice);
  } catch {
    priceRange = {};
  }
  const requestedPage = Number(values.page);
  return {
    page: Number.isSafeInteger(requestedPage) && requestedPage > 0 ? requestedPage : 1,
    query: {
      keyword: (values.keyword || '').trim(),
      // 路由里的 category 现承载分类 ID（字符串），非法/空则不限
      categoryId: /^\d+$/.test(values.category) ? values.category : '',
      favoritesOnly,
      sort: ITEM_SORT_OPTIONS.some((option) => option.value === values.sort)
        ? values.sort
        : 'newest',
      communityId: community === 'all' ? '' : community,
      size: [3, 6, 12].includes(Number(values.size)) ? Number(values.size) : 6,
      minPrice: priceRange.minPrice,
      maxPrice: priceRange.maxPrice,
    },
  };
}

export function itemListQuery(query, page = 1) {
  const result = { community: query.communityId || 'all' };
  if (query.favoritesOnly) result.tab = 'favorites';
  if (query.keyword) result.keyword = query.keyword;
  if (query.categoryId) result.category = String(query.categoryId);
  if (query.sort !== 'newest') result.sort = query.sort;
  if (query.minPrice !== undefined) result.minPrice = String(query.minPrice);
  if (query.maxPrice !== undefined) result.maxPrice = String(query.maxPrice);
  if (query.size !== 6) result.size = String(query.size);
  if (page > 1) result.page = String(page);
  return result;
}
