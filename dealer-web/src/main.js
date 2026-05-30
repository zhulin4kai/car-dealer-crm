//import...from...语句导入，从vue框架导入createApp函数
import { createApp } from 'vue'

//导入css样式，不需要from子句
// import './style.css'
import './assets/global.css'

//import...from...语句导入，从element-plus框架导入ElementPlus组件
import ElementPlus from 'element-plus'

//导入element-plus国际化的中文包组件
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'

//导入element-plus的css样式，不需要from子句
import 'element-plus/dist/index.css'

import * as ElementPlusIconsVue from '@element-plus/icons-vue'

//从router.js中导入router组件
import router from './router/router.js'

//从./App.vue页面导入App组件
import App from './App.vue'
import {doGet} from "./http/httpRequest.js";
import {getUserPermission, setUserPermission} from "./util/util.js"; //根组件

let app = createApp(App);

for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
    app.component(key, component)
}

//el：指令所绑定到的页面dom元素。这可以用于直接操作DOM。
//binding：是一个对象，里面包含很多属性，重点看value属性：传递给指令的值。我们传的是 clue:delete 这个值
app.directive("hasPermission",  (el, binding) => {
    // 从缓存中获取权限列表
    let permissionList = getUserPermission();
    
    // 如果缓存中没有权限信息，则从服务器获取
    if (!permissionList) {
        doGet("/api/login/info", {}).then(resp => {
            let user = resp.data.data;
            permissionList = user.permissionList || [];
            // 缓存权限信息
            setUserPermission(permissionList);
            
            checkPermission(el, binding.value, permissionList);
        })
    } else {
        checkPermission(el, binding.value, permissionList);
    }
})

function checkPermission(el, permission, permissionList) {
    let flag = false;
    
    for (let key in permissionList) {
        if (permissionList[key] === permission) {
            flag = true;
            break;
        }
    }
    if (!flag) {
        //没有权限，把页面元素隐藏掉
        //el.style.display = 'none';
        //把没有权限的按钮dom元素删除
        el.parentNode && el.parentNode.removeChild(el)
    }
}

//利用上面所导入的createApp()函数，创建一个vue应用，mount是挂载到#app地方
app.use(ElementPlus, {locale: zhCn}).use(router).mount('#app')
