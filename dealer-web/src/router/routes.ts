import type { RouteRecordRaw } from 'vue-router'
import { PERMISSIONS } from '@/shared/constants/permissions'

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
        meta: {
          requiresAuth: true,
          title: '统计',
          activeMenu: '/dashboard',
          permission: PERMISSIONS.statistic.view,
        },
      },
      {
        path: 'user',
        name: 'user',
        component: () => import('@/pages/dashboard/user.vue'),
        meta: {
          requiresAuth: true,
          title: '用户管理',
          activeMenu: '/dashboard/user',
          permission: PERMISSIONS.user.list,
        },
      },
      {
        path: 'activity',
        name: 'activity',
        component: () => import('@/pages/dashboard/activity/index.vue'),
        meta: {
          requiresAuth: true,
          title: '市场活动',
          activeMenu: '/dashboard/activity',
          permission: PERMISSIONS.activity.list,
        },
      },
      {
        path: 'activity/:id',
        name: 'activity-detail',
        component: () => import('@/pages/dashboard/activity/[id].vue'),
        meta: {
          requiresAuth: true,
          title: '市场活动详情',
          activeMenu: '/dashboard/activity',
          permission: PERMISSIONS.activity.view,
        },
      },
      {
        path: 'clue',
        name: 'clue',
        component: () => import('@/pages/dashboard/clue/index.vue'),
        meta: {
          requiresAuth: true,
          title: '线索管理',
          activeMenu: '/dashboard/clue',
          permission: PERMISSIONS.clue.list,
        },
      },
      {
        path: 'clue/detail/:id',
        name: 'clue-detail',
        component: () => import('@/pages/dashboard/clue/detail/[id].vue'),
        meta: {
          requiresAuth: true,
          title: '线索详情',
          activeMenu: '/dashboard/clue',
          permission: PERMISSIONS.clue.view,
        },
      },
      {
        path: 'customer',
        name: 'customer',
        component: () => import('@/pages/dashboard/customer.vue'),
        meta: {
          requiresAuth: true,
          title: '客户管理',
          activeMenu: '/dashboard/customer',
          permission: PERMISSIONS.customer.list,
        },
      },
      {
        path: 'customer/:id',
        name: 'customer-detail',
        component: () => import('@/pages/dashboard/customer/[id].vue'),
        meta: {
          requiresAuth: true,
          title: '客户详情',
          activeMenu: '/dashboard/customer',
          permission: PERMISSIONS.customer.view,
        },
      },
      {
        path: 'product',
        name: 'product',
        component: () => import('@/pages/dashboard/product/index.vue'),
        meta: {
          requiresAuth: true,
          title: '产品列表',
          activeMenu: '/dashboard/product',
          permission: PERMISSIONS.product.list,
        },
      },
      {
        path: 'product/category',
        name: 'product-category',
        component: () => import('@/pages/dashboard/product/category.vue'),
        meta: {
          requiresAuth: true,
          title: '产品分类',
          activeMenu: '/dashboard/product/category',
          permission: PERMISSIONS.product.category.list,
        },
      },
      {
        path: 'product/promotion',
        name: 'product-promotion',
        component: () => import('@/pages/dashboard/product/promotion.vue'),
        meta: {
          requiresAuth: true,
          title: '促销管理',
          activeMenu: '/dashboard/product/promotion',
          permission: PERMISSIONS.product.promotion.list,
        },
      },
      {
        path: 'product/stock',
        name: 'product-stock',
        component: () => import('@/pages/dashboard/product/stock.vue'),
        meta: {
          requiresAuth: true,
          title: '库存管理',
          activeMenu: '/dashboard/product/stock',
          permission: PERMISSIONS.product.stock.view,
        },
      },
      {
        path: 'quote',
        name: 'quote',
        component: () => import('@/pages/dashboard/quote.vue'),
        meta: {
          requiresAuth: true,
          title: '报价订单',
          activeMenu: '/dashboard/quote',
          permission: PERMISSIONS.quote.list,
        },
      },
      {
        path: 'delivery',
        name: 'delivery',
        component: () => import('@/pages/dashboard/delivery.vue'),
        meta: {
          requiresAuth: true,
          title: '交付管理',
          activeMenu: '/dashboard/delivery',
          permission: PERMISSIONS.delivery.list,
        },
      },
      {
        path: 'tran',
        name: 'tran',
        component: () => import('@/pages/dashboard/tran/index.vue'),
        meta: {
          requiresAuth: true,
          title: '交易管理',
          activeMenu: '/dashboard/tran',
          permission: PERMISSIONS.tran.list,
        },
      },
      {
        path: 'tran/:id',
        name: 'tran-detail',
        component: () => import('@/pages/dashboard/tran/[id].vue'),
        meta: {
          requiresAuth: true,
          title: '交易详情',
          activeMenu: '/dashboard/tran',
          permission: PERMISSIONS.tran.view,
        },
      },
      {
        path: 'tran/approve/:id',
        name: 'tran-approve',
        component: () => import('@/pages/dashboard/tran/approve/[id].vue'),
        meta: {
          requiresAuth: true,
          title: '交易审批',
          activeMenu: '/dashboard/tran',
          permission: PERMISSIONS.tran.approve,
        },
      },
      {
        path: 'tran/invoice/:id',
        name: 'tran-invoice',
        component: () => import('@/pages/dashboard/tran/invoice/[id].vue'),
        meta: {
          requiresAuth: true,
          title: '交易发票',
          activeMenu: '/dashboard/tran',
          permission: PERMISSIONS.tran.invoice,
        },
      },
      {
        path: 'dict/type',
        name: 'dict-type',
        component: () => import('@/pages/dashboard/dict/type.vue'),
        meta: {
          requiresAuth: true,
          title: '字典管理',
          activeMenu: '/dashboard/dict/type',
          permission: PERMISSIONS.dict.type.list,
        },
      },
      {
        path: 'dict/value',
        name: 'dict-value',
        component: () => import('@/pages/dashboard/dict/value.vue'),
        meta: {
          requiresAuth: true,
          title: '字典管理',
          activeMenu: '/dashboard/dict/type',
          permission: PERMISSIONS.dict.value.list,
        },
      },
    ],
  },
  {
    path: '/403',
    name: 'forbidden',
    component: () => import('@/pages/403.vue'),
    meta: { requiresAuth: true, title: '无权访问' },
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/dashboard',
    meta: { requiresAuth: false },
  },
]
