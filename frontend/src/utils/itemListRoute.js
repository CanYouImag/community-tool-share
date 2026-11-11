import { ITEM_CATEGORY } from './constants';
import { ITEM_SORT_OPTIONS, parsePriceRange } from './itemFilters';

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
  const community = values.community || (isLoggedIn && !favoritesOnly ? 'joined' : 'all');
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
      category: ITEM_CATEGORY.includes(values.category) ? values.category : '',
      favoritesOnly,
      sort: ITEM_SORT_OPTIONS.some((option) => option.value === values.sort)
        ? values.sort
        : 'latest',
      communityScope: community === 'joined' && isLoggedIn ? 'joined' : 'all',
      communityId: ['joined', 'all'].includes(community) ? undefined : community,
      size: [3, 6, 12].includes(Number(values.size)) ? Number(values.size) : 6,
      minPrice: priceRange.minPrice,
      maxPrice: priceRange.maxPrice,
    },
  };
}

export function itemListQuery(query, page = 1) {
  const result = { community: query.communityId || query.communityScope };
  if (query.favoritesOnly) result.tab = 'favorites';
  if (query.keyword) result.keyword = query.keyword;
  if (query.category) result.category = query.category;
  if (query.sort !== 'latest') result.sort = query.sort;
  if (query.minPrice !== undefined) result.minPrice = String(query.minPrice);
  if (query.maxPrice !== undefined) result.maxPrice = String(query.maxPrice);
  if (query.size !== 6) result.size = String(query.size);
  if (page > 1) result.page = String(page);
  return result;
}
