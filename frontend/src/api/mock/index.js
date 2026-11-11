// Mock 路由分发：根据 method + url 返回假数据
import * as db from './data'

const delay = (data, ms = 200) =>
  new Promise((resolve) => setTimeout(() => resolve(data), ms))

// 按 url 匹配处理器
const handlers = [
  {
    method: 'GET',
    pattern: /^\/v1\/items$/,
    handler: () => ({
        records: db.items,
        total: db.items.length,
        page: 1,
        size: 10
    })
  },
  {
    method: 'GET',
    pattern: /^\/communities$/,
    handler: () => db.communities
  },
  {
    method: 'GET',
    pattern: /^\/communities\/home\/(.+)$/,
    handler: (_params, matches) => {
      const cid = matches[1]
      const home = db.communityHome[cid] || { top: [], latest: [] }
      const community = db.communities.find((c) => c.id === cid)
      const resolve = (ids) =>
        ids.map((id) => db.items.find((i) => i.id === id)).filter(Boolean)
      return { ...community, top: resolve(home.top), latest: resolve(home.latest) }
    }
  },
  {
    method: 'GET',
    pattern: /^\/memberships\/mine$/,
    handler: () => db.myMemberships
  },
  {
    method: 'POST',
    pattern: /^\/communities\/(.+)\/join$/,
    handler: () => ({ id: 'c_1002', status: 'PENDING' })
  },
  {
    method: 'GET',
    pattern: /^\/orders$/,
    handler: (params) => {
      const role = params.role || 'borrow'
      const list = role === 'lend' ? db.lendOrders : db.orders
      let result = [...list]
      if (params.keyword) {
        const kw = params.keyword.toLowerCase()
        result = result.filter(
          (o) => o.id.toLowerCase().includes(kw) || o.itemTitle.includes(kw)
        )
      }
      if (params.status) {
        result = result.filter((o) => o.status === params.status)
      }
      return { list: result, total: result.length }
    }
  },
  {
    method: 'GET',
    pattern: /^\/orders\/(.+)$/,
    handler: (_params, matches) => {
      const order = db.orders.find((o) => o.id === matches[1]) || db.orders[0]
      return order
    }
  },
  {
    method: 'GET',
    pattern: /^\/orders\/(.+)\/messages$/,
    handler: () => [
      { id: 'm_1', from: '陈禾', mine: false, time: '2026-10-10 09:30', content: '取物时间我们约在周六上午可以吗？' },
      { id: 'm_2', from: 'Starling', mine: true, time: '2026-10-10 09:45', content: '可以，周六上午 10 点小区门口见。' }
    ]
  },
  {
    method: 'POST',
    pattern: /^\/orders\/(.+)\/messages$/,
    handler: () => ({ ok: true })
  },
  {
    method: 'GET',
    pattern: /^\/wallet$/,
    handler: () => db.wallet
  },
  {
    method: 'GET',
    pattern: /^\/wallet\/flows$/,
    handler: (params) => {
      let result = [...db.flows]
      if (params.type) {
        result = result.filter((f) => f.type === params.type)
      }
      return { list: result, total: result.length }
    }
  },
  {
    method: 'GET',
    pattern: /^\/wallet\/deposits$/,
    handler: () => db.deposits
  },
  {
    method: 'GET',
    pattern: /^\/dispute$/,
    handler: () => db.dispute
  },
  {
    method: 'POST',
    pattern: /^\/dispute$/,
    handler: () => ({ id: db.dispute.id })
  },
  {
    method: 'GET',
    pattern: /^\/dispute\/award$/,
    handler: () => db.award
  },
  {
    method: 'POST',
    pattern: /^\/reports$/,
    handler: () => ({ ok: true })
  }
]

// 匹配并返回结果，未匹配返回 null
export async function matchMock(method, url, params) {
  const [path, query] = url.split('?')
  const q = new URLSearchParams(query || '')
  const mergedParams = { ...params, ...Object.fromEntries(q.entries()) }

  for (const h of handlers) {
    if (h.method !== method.toUpperCase()) continue
    const m = path.match(h.pattern)
    if (m) {
      return delay(await h.handler(mergedParams, m))
    }
  }
  return null
}
