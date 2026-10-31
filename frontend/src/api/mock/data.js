// 假数据：字段命名与需求 4.4 节核心实体对齐
// 接口就绪后本文件整体废弃，由真实后端接管

const asset = (name) => `/assets/${name}`

export const communities = [
  {
    id: 'c_1001',
    name: '青禾社区',
    intro: '让工具在邻里之间流动',
    members: 126,
    onRent: 38,
    admin: '周宁',
    notice: '取物和归还时请核对配件并拍照。',
    autoApprove: false
  },
  {
    id: 'c_1002',
    name: '梧桐里社区',
    intro: '梧桐树下，好物共享',
    members: 94,
    onRent: 26,
    admin: '林晚',
    notice: '欢迎新邻居加入，共享闲置工具。',
    autoApprove: false
  },
  {
    id: 'c_1003',
    name: '溪岸社区',
    intro: '临溪而居，邻里互助',
    members: 78,
    onRent: 19,
    admin: '顾言',
    notice: '本周六社区置换活动，欢迎参加。',
    autoApprove: true
  }
]

// 当前用户的社区成员关系
export const myMemberships = [
  { id: 'c_1001', name: '青禾社区', status: 'JOINED', time: '2026-10-01' },
  { id: 'c_1002', name: '梧桐里社区', status: 'PENDING', time: '2026-10-09' }
]

export const items = [
  {
    id: 'i_1001',
    title: '18V 充电式电钻',
    category: '工具',
    communityId: 'c_1001',
    communityName: '青禾社区',
    dailyPrice: 20,
    deposit: 200,
    owner: '陈禾',
    ownerCredit: 92,
    image: asset('item-30556edc.svg')
  },
  {
    id: 'i_1002',
    title: '双人露营帐篷',
    category: '户外',
    communityId: 'c_1001',
    communityName: '青禾社区',
    dailyPrice: 35,
    deposit: 300,
    owner: '周宁',
    ownerCredit: 95,
    image: asset('item-dde811a0.svg')
  },
  {
    id: 'i_1003',
    title: '便携高清投影仪',
    category: '数码',
    communityId: 'c_1001',
    communityName: '青禾社区',
    dailyPrice: 45,
    deposit: 500,
    owner: '许安',
    ownerCredit: 90,
    image: asset('item-15f4c496.svg')
  },
  {
    id: 'i_1004',
    title: '家用折叠梯',
    category: '工具',
    communityId: 'c_1001',
    communityName: '青禾社区',
    dailyPrice: 15,
    deposit: 100,
    owner: '李青',
    ownerCredit: 86,
    image: asset('item-206bd1ad.svg')
  },
  {
    id: 'i_1005',
    title: '布艺清洗机',
    category: '家电',
    communityId: 'c_1001',
    communityName: '青禾社区',
    dailyPrice: 40,
    deposit: 250,
    owner: 'Starling',
    ownerCredit: 88,
    image: asset('item-cce1a346.svg')
  },
  {
    id: 'i_1006',
    title: '周末露营天幕',
    category: '户外',
    communityId: 'c_1001',
    communityName: '青禾社区',
    dailyPrice: 25,
    deposit: 150,
    owner: '陆川',
    ownerCredit: 82,
    image: asset('item-dde811a0.svg')
  }
]

// 社区主页：置顶推荐 + 最新上架
export const communityHome = {
  c_1001: {
    top: ['i_1001', 'i_1002', 'i_1003'],
    latest: ['i_1001', 'i_1002', 'i_1003', 'i_1004']
  },
  c_1002: { top: ['i_1005'], latest: ['i_1005', 'i_1006'] },
  c_1003: { top: [], latest: [] }
}

export const orders = [
  {
    id: 'LZ202610090018',
    itemId: 'i_1001',
    itemTitle: '18V 充电式电钻',
    communityName: '青禾社区',
    lender: '陈禾',
    borrower: 'Starling',
    startDate: '2026-10-12',
    endDate: '2026-10-14',
    days: 3,
    dailyPrice: 20,
    rent: 60,
    deposit: 200,
    status: 'PENDING_PAYMENT',
    createdAt: '2026-10-09 10:00'
  },
  {
    id: 'LZ202610080012',
    itemId: 'i_1002',
    itemTitle: '双人露营帐篷',
    communityName: '青禾社区',
    lender: '周宁',
    borrower: 'Starling',
    startDate: '2026-10-10',
    endDate: '2026-10-12',
    days: 3,
    dailyPrice: 35,
    rent: 105,
    deposit: 300,
    status: 'PENDING_PICKUP',
    pickupCode: '826 419',
    createdAt: '2026-10-08 09:00'
  },
  {
    id: 'LZ202610050009',
    itemId: 'i_1003',
    itemTitle: '便携投影仪',
    communityName: '青禾社区',
    lender: '许安',
    borrower: 'Starling',
    startDate: '2026-10-06',
    endDate: '2026-10-08',
    days: 3,
    dailyPrice: 45,
    rent: 135,
    deposit: 500,
    status: 'OVERDUE',
    createdAt: '2026-10-05 14:00'
  },
  {
    id: 'LZ202610010003',
    itemId: 'i_1004',
    itemTitle: '家用折叠梯',
    communityName: '青禾社区',
    lender: '李青',
    borrower: 'Starling',
    startDate: '2026-10-02',
    endDate: '2026-10-04',
    days: 3,
    dailyPrice: 15,
    rent: 45,
    deposit: 100,
    status: 'COMPLETED',
    createdAt: '2026-10-01 09:00'
  }
]

// 租出视角（当前用户作为出租者）
export const lendOrders = [
  {
    id: 'LZ202610090018',
    itemTitle: '18V 充电式电钻',
    lender: 'Starling',
    borrower: '陈禾',
    startDate: '2026-10-12',
    endDate: '2026-10-14',
    rent: 60,
    deposit: 200,
    status: 'PENDING_ACCEPT'
  },
  {
    id: 'LZ202610080012',
    itemTitle: '双人露营帐篷',
    lender: 'Starling',
    borrower: '许安',
    startDate: '2026-10-10',
    endDate: '2026-10-12',
    rent: 105,
    deposit: 300,
    status: 'PENDING_PICKUP'
  },
  {
    id: 'LZ202610050009',
    itemTitle: '便携投影仪',
    lender: 'Starling',
    borrower: '李青',
    startDate: '2026-10-06',
    endDate: '2026-10-08',
    rent: 135,
    deposit: 500,
    status: 'PENDING_INSPECT'
  },
  {
    id: 'LZ202610010003',
    itemTitle: '家用折叠梯',
    lender: 'Starling',
    borrower: '周宁',
    startDate: '2026-10-02',
    endDate: '2026-10-04',
    rent: 45,
    deposit: 100,
    status: 'COMPLETED'
  }
]

// 钱包（字段对齐 WalletDO：balance + freezeBalance）
export const wallet = {
  balance: 240.0,
  freezeBalance: 200.0,
  totalIncome: 0.0,
  recentRentSpent: 60.0
}

// 资金流水
export const flows = [
  {
    id: 'f_1',
    time: '2026-10-09 10:00',
    type: 'FREEZE',
    amount: 200.0,
    orderId: 'LZ202610090018',
    remark: '冻结押金'
  },
  {
    id: 'f_2',
    time: '2026-10-09 10:00',
    type: 'EXPENSE',
    amount: 60.0,
    orderId: 'LZ202610090018',
    remark: '支付租金'
  },
  {
    id: 'f_3',
    time: '2026-10-09 09:00',
    type: 'INCOME',
    amount: 500.0,
    orderId: '',
    remark: '初始化测试余额'
  }
]

// 押金明细（冻结押金按订单展开）
export const deposits = [
  {
    orderId: 'LZ202610090018',
    itemTitle: '18V 充电式电钻',
    amount: 200.0,
    status: 'FREEZE',
    time: '2026-10-09 10:00'
  },
  {
    orderId: 'LZ202610080012',
    itemTitle: '双人露营帐篷',
    amount: 300.0,
    status: 'UNFREEZE',
    time: '2026-10-12 12:00'
  }
]

// 纠纷单（两级机制：社区调解 → 平台仲裁）
export const dispute = {
  id: 'JF20261015001',
  orderId: 'LZ202610090018',
  itemTitle: '18V 充电式电钻 / 双电池套装',
  communityName: '青禾社区',
  lender: '陈禾',
  borrower: 'Starling',
  reason: '归还时发现电钻外壳损坏，双方对责任存在分歧。',
  deposit: 200.0,
  status: 'MEDIATING',
  initiator: '陈禾',
  // 当前处理阶段：mediation（社区调解）/ arbitration（平台仲裁）
  stage: 'mediation',
  // 社区调解信息
  mediation: {
    mediator: '周宁',
    mediatorRole: '青禾社区管理员',
    status: 'MEDIATING',
    conclusion: null,
    timeline: [
      { time: '10-15 09:00', actor: '系统', text: '纠纷已提交', detail: '陈禾提交理由与归还照片，押金 ¥200.00 已冻结。' },
      { time: '10-15 11:20', actor: '周宁', text: '介入调解', detail: '请双方确认交接时的外壳状态，并补充近距离照片。' }
    ]
  },
  // 平台仲裁信息（调解不成立后升级）
  arbitration: null,
  timeline: [
    { time: '10-15 09:00', actor: '系统', text: '纠纷已提交', detail: '陈禾提交理由与归还照片。' },
    { time: '10-15 11:20', actor: '周宁', text: '介入调解', detail: '请双方确认交接时的外壳状态。' }
  ],
  mediatorMessage: {
    from: '社区管理员 周宁',
    content: '已查看取还照片，请补充近距离照片与交接说明。'
  }
}

// 裁决书
export const award = {
  id: 'ZC20261016001',
  orderId: 'LZ202610090018',
  applicant: '陈禾',
  counterpart: 'Starling',
  deposit: 200.0,
  type: '部分扣除',
  toLender: 50.0,
  toBorrower: 150.0,
  rentRelief: 0.0,
  reason: '根据取还存证及双方说明，认定归还时新增损坏，维修补偿金额为 ¥50.00。',
  time: '2026-10-16 14:30',
  arbitrator: '系统管理员'
}
