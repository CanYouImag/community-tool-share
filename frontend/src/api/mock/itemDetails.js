import { isDateKey, localDateKey } from '../../utils/itemCalendar';
import * as db from './data';

const finishedStatuses = ['COMPLETED', 'CANCELLED', 'CLOSED'];
const awaitingReturnStatuses = [
  'IN_USE',
  'PENDING_RETURN',
  'PENDING_INSPECT',
  'OVERDUE',
  'DISPUTED',
];

export function getItemRentalDetails(itemId, today = localDateKey()) {
  // 两个订单视角可能包含同一笔订单，按订单 ID 去重。
  const ordersById = new Map();
  for (const order of [...db.orders, ...db.lendOrders, ...db.itemRentalHistory]) {
    if (order.itemId !== itemId) continue;
    const versions = ordersById.get(order.id) || [];
    versions.push(order);
    ordersById.set(order.id, versions);
  }

  const completedIds = new Set();
  const blockedRanges = new Map();
  let availabilityKnown = true;

  for (const [id, versions] of ordersById) {
    const active = versions.filter((order) => !finishedStatuses.includes(order.status));
    if (active.length === 0 && versions.some((order) => order.status === 'COMPLETED')) {
      completedIds.add(id);
    }
    for (const order of active) {
      if (
        !isDateKey(order.startDate) ||
        !isDateKey(order.endDate) ||
        order.startDate > order.endDate
      ) {
        availabilityKnown = false;
        continue;
      }
      // 租期已过但仍未归还/验收时，不能把后续日期显示为可租。
      const endDate =
        order.endDate < today && awaitingReturnStatuses.includes(order.status)
          ? null
          : order.endDate;
      blockedRanges.set(`${order.startDate}/${endDate}`, { startDate: order.startDate, endDate });
    }
  }

  const reviews = db.itemReviews
    .filter((review) => review.itemId === itemId && completedIds.has(review.orderId))
    .map(({ id, authorName, rating, comment, createdAt }) => ({
      id,
      authorName,
      rating,
      comment,
      createdAt,
    }))
    .sort((a, b) => Date.parse(b.createdAt) - Date.parse(a.createdAt));
  const rating = reviews.length
    ? Math.round((reviews.reduce((sum, review) => sum + review.rating, 0) / reviews.length) * 10) /
      10
    : null;

  return {
    rentalCount: completedIds.size,
    rating,
    reviews,
    blockedRanges: availabilityKnown ? [...blockedRanges.values()] : null,
  };
}
