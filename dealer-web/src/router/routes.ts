import type { RouteRecordRaw } from 'vue-router'

export const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'login',
    component: () => import('@/pages/index.vue'),
    meta: { requiresAuth: false, title: '登录' },
  },
  {
    path: '/dashboard',
    name: 'dashboard',
    component: () => import('@/layouts/DashboardLayout.vue'),
    meta: { requiresAuth: true, title: '仪表盘' },
    children: [
      {
        path: '',
        name: 'dashboard-statistic',
        component: () => import('@/pages/dashboard/index.vue'),
        meta: { requiresAuth: true, title: '统计', activeMenu: '/dashboard' },
      },
      {
        path: 'user',
        name: 'user',
        component: () => import('@/pages/dashboard/user.vue'),
        meta: { requiresAuth: true, title: '用户管理', activeMenu: '/dashboard/user' },
      },
      {
        path: 'activity',
        name: 'activity',
        component: () => import('@/pages/dashboard/activity/index.vue'),
        meta: { requiresAuth: true, title: '市场活动', activeMenu: '/dashboard/activity' },
      },
      {
        path: 'activity/:id',
        name: 'activity-detail',
        component: () => import('@/pages/dashboard/activity/[id].vue'),
        meta: { requiresAuth: true, title: '市场活动详情', activeMenu: '/dashboard/activity' },
      },
      {
        path: 'clue',
        name: 'clue',
        component: () => import('@/pages/dashboard/clue/index.vue'),
        meta: { requiresAuth: true, title: '线索管理', activeMenu: '/dashboard/clue' },
      },
      {
        path: 'clue/detail/:id',
        name: 'clue-detail',
        component: () => import('@/pages/dashboard/clue/detail/[id].vue'),
        meta: { requiresAuth: true, title: '线索详情', activeMenu: '/dashboard/clue' },
      },
      {
        path: 'customer',
        name: 'customer',
        component: () => import('@/pages/dashboard/customer.vue'),
        meta: { requiresAuth: true, title: '客户管理', activeMenu: '/dashboard/customer' },
      },
      {
        path: 'product',
        name: 'product',
        component: () => import('@/pages/dashboard/product/index.vue'),
        meta: { requiresAuth: true, title: '产品管理', activeMenu: '/dashboard/product' },
      },
      {
        path: 'product/category',
        name: 'product-category',
        component: () => import('@/pages/dashboard/product/category.vue'),
        meta: { requiresAuth: true, title: '产品分类', activeMenu: '/dashboard/product/category' },
      },
      {
        path: 'product/promotion',
        name: 'product-promotion',
        component: () => import('@/pages/dashboard/product/promotion.vue'),
        meta: { requiresAuth: true, title: '促销管理', activeMenu: '/dashboard/product/promotion' },
      },
      {
        path: 'product/stock',
        name: 'product-stock',
        component: () => import('@/pages/dashboard/product/stock.vue'),
        meta: { requiresAuth: true, title: '库存预警', activeMenu: '/dashboard/product/stock' },
      },
      {
        path: 'tran',
        name: 'tran',
        component: () => import('@/pages/dashboard/tran/index.vue'),
        meta: { requiresAuth: true, title: '交易管理', activeMenu: '/dashboard/tran' },
      },
      {
        path: 'tran/:id',
        name: 'tran-detail',
        component: () => import('@/pages/dashboard/tran/[id].vue'),
        meta: { requiresAuth: true, title: '交易详情', activeMenu: '/dashboard/tran' },
      },
      {
        path: 'tran/approve/:id',
        name: 'tran-approve',
        component: () => import('@/pages/dashboard/tran/approve/[id].vue'),
        meta: { requiresAuth: true, title: '交易审批', activeMenu: '/dashboard/tran' },
      },
      {
        path: 'tran/invoice/:id',
        name: 'tran-invoice',
        component: () => import('@/pages/dashboard/tran/invoice/[id].vue'),
        meta: { requiresAuth: true, title: '交易发票', activeMenu: '/dashboard/tran' },
      },
      {
        path: 'dict/type',
        name: 'dict-type',
        component: () => import('@/pages/dashboard/dict/type.vue'),
        meta: { requiresAuth: true, title: '字典类型', activeMenu: '/dashboard/dict/type' },
      },
      {
        path: 'dict/value',
        name: 'dict-value',
        component: () => import('@/pages/dashboard/dict/value.vue'),
        meta: { requiresAuth: true, title: '字典值', activeMenu: '/dashboard/dict/value' },
      },
      {
        path: 'system',
        name: 'system',
        component: () => import('@/pages/dashboard/system.vue'),
        meta: { requiresAuth: true, title: '系统管理', activeMenu: '/dashboard/system' },
      },
    ],
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/dashboard',
    meta: { requiresAuth: false },
  },
]
