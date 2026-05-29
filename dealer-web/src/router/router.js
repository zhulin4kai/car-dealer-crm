//从vue-router这个依赖库中导入createRouter()函数, createWebHistory()函数
import { createRouter, createWebHistory } from "vue-router";
import {getTokenName} from "../util/util.js";

//定义一个变量
let router = createRouter({
    //路由历史
    history: createWebHistory(),

    //配置路由，是一个数组，里面可以配置多个路由
    routes: [
        {
            //路由路径
            path: '/',
            //路由路径所对应的页面
            component : () => import('../view/LoginView.vue'),
        },
        {
            //路由路径
            path: '/dashboard',
            //路由路径所对应的页面
            component : () => import('../view/DashboardView.vue'),
            //子路由
            children : [
                {
                    //路由路径，子路由路径不能以斜杠开头
                    path: '',
                    //路由路径所对应的页面
                    component : () => import('../view/StatisticView.vue'),
                },
                {
                    //路由路径，子路由路径不能以斜杠开头
                    path: 'user',
                    //路由路径所对应的页面
                    component : () => import('../view/UserView.vue'),
                },                {
                    //路由路径，子路由路径不能以斜杠开头
                    path: 'activity',
                    //路由路径所对应的页面
                    component : () => import('../view/ActivityView.vue'),
                },
                {
                    //路由路径，子路由路径不能以斜杠开头，id是动态变量，这个叫动态路由
                    path: 'activity/:id',
                    //路由路径所对应的页面
                    component : () => import('../view/ActivityDetailView.vue'),
                },{
                    //路由路径，子路由路径不能以斜杠开头
                    path: 'clue',
                    //路由路径所对应的页面
                    component : () => import('../view/ClueView.vue'),
                },
                {
                    //路由路径，子路由路径不能以斜杠开头，id是动态变量，这个叫动态路由
                    path: 'clue/detail/:id',
                    //路由路径所对应的页面
                    component : () => import('../view/ClueDetailView.vue'),
                },
                {
                    //路由路径，子路由路径不能以斜杠开头
                    path: 'customer',
                    //路由路径所对应的页面
                    component : () => import('../view/CustomerView.vue'),
                },
                {
                    //路由路径，子路由路径不能以斜杠开头
                    path: 'product',
                    //路由路径所对应的页面
                    component : () => import('../view/ProductView.vue'),
                },
                {
                    //路由路径，子路由路径不能以斜杠开头
                    path: 'product/category',
                    //路由路径所对应的页面
                    component : () => import('../view/ProductCategoryView.vue'),
                },
                {
                    //路由路径，子路由路径不能以斜杠开头
                    path: 'product/promotion',
                    //路由路径所对应的页面
                    component : () => import('../view/ProductPromotionView.vue'),
                },
                {
                    //路由路径，子路由路径不能以斜杠开头
                    path: 'product/stock',
                    //路由路径所对应的页面
                    component : () => import('../view/ProductStockAlertView.vue'),
                },                {
                    //路由路径，子路由路径不能以斜杠开头
                    path: 'tran',
                    //路由路径所对应的页面
                    component : () => import('../view/TranView.vue'),
                },
                {
                    //路由路径，子路由路径不能以斜杠开头，id是动态变量，这个叫动态路由
                    path: 'tran/:id',
                    //路由路径所对应的页面
                    component : () => import('../view/TranDetailView.vue'),
                },
                {
                    //路由路径，子路由路径不能以斜杠开头
                    path: 'tran/approve/:id',
                    //路由路径所对应的页面
                    component : () => import('../view/TranApproveView.vue'),
                },
                {
                    //路由路径，子路由路径不能以斜杠开头
                    path: 'tran/invoice/:id',
                    //路由路径所对应的页面
                    component : () => import('../view/TranInvoiceView.vue'),
                },
                {
                    //路由路径，子路由路径不能以斜杠开头
                    path: 'dict/type',
                    //路由路径所对应的页面
                    component : () => import('../view/DictTypeView.vue'),
                },
                {
                    //路由路径，子路由路径不能以斜杠开头
                    path: 'dict/value',
                    //路由路径所对应的页面
                    component : () => import('../view/DictValueView.vue'),
                },
                {
                    //路由路径，子路由路径不能以斜杠开头
                    path: 'system',
                    //路由路径所对应的页面
                    component : () => import('../view/SystemView.vue'),
                }
            ]
        },
        {
            //路由路径
            path: '/:pathMatch(.*)*',
            redirect: '/dashboard',
        }
    ]
})

//路由守卫 - 检查token
router.beforeEach((to, from, next) => {
    let token = window.sessionStorage.getItem(getTokenName());
    if (!token) {
        token = window.localStorage.getItem(getTokenName());
    }
    // 如果访问的是受保护的路由（以/dashboard开头）且没有token，跳转到登录页
    if (to.path.startsWith('/dashboard') && !token) {
        next('/');
    } else {
        next();
    }
})

//导出创建的路由对象
export default router;