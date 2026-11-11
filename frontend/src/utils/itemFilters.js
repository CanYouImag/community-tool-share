export const ITEM_SORT_OPTIONS = [
  { value: 'latest', label: '最新发布' },
  { value: 'price_asc', label: '价格从低到高' },
  { value: 'price_desc', label: '价格从高到低' },
  { value: 'rating_desc', label: '评分优先' },
];

export function paginateItems(items, page = 1, size = 10) {
  const requestedPage = Number(page);
  const pageSize = Number(size);
  if (
    !Number.isSafeInteger(requestedPage) ||
    requestedPage < 1 ||
    !Number.isSafeInteger(pageSize) ||
    pageSize < 1 ||
    pageSize > 100
  ) {
    throw new Error('分页参数无效：页码须为正整数，每页数量须为 1～100');
  }

  const currentPage = Math.min(requestedPage, Math.max(1, Math.ceil(items.length / pageSize)));
  const start = (currentPage - 1) * pageSize;
  return {
    records: items.slice(start, start + pageSize),
    total: items.length,
    page: currentPage,
    size: pageSize,
  };
}

export function sortItems(items, sort = 'latest') {
  if (!ITEM_SORT_OPTIONS.some((option) => option.value === sort)) {
    throw new Error('排序方式无效');
  }

  const timestamp = (item) => Date.parse(item.createdAt) || 0;
  const rating = (item) =>
    Number.isFinite(item.rating) && item.rating >= 0 && item.rating <= 5 ? item.rating : -1;

  return [...items].sort((a, b) => {
    let difference = 0;
    if (sort === 'price_asc') difference = a.dailyPrice - b.dailyPrice;
    if (sort === 'price_desc') difference = b.dailyPrice - a.dailyPrice;
    if (sort === 'rating_desc') difference = rating(b) - rating(a);

    // 同价、同分时按发布时间和 ID 排序，避免列表顺序跳动。
    return difference || timestamp(b) - timestamp(a) || String(a.id).localeCompare(String(b.id));
  });
}

// 空值表示不限制；价格区间包含上下限。
export function parsePriceRange(minPrice, maxPrice) {
  const parseBound = (value) => {
    if (value === '' || value === undefined || value === null) return undefined;

    const amount = Number(value);
    if (!/^\d+(?:\.\d{1,2})?$/.test(String(value)) || !Number.isFinite(amount)) {
      throw new Error('日租金筛选须为非负金额，最多两位小数');
    }
    return amount;
  };

  const min = parseBound(minPrice);
  const max = parseBound(maxPrice);

  if (min !== undefined && max !== undefined && min > max) {
    throw new Error('最低日租金不能高于最高日租金');
  }

  return { minPrice: min, maxPrice: max };
}
