import {ElMessage, ElMessageBox} from "element-plus";

/**
 * 消息提示
 *
 * @param msg
 * @param type
 */
export function messageTip(msg, type) {
    ElMessage({
        showClose: true, //是否显示关闭按钮
        center: true, //文字是否居中
        duration: 3000, //显示时间，单位为毫秒
        message: msg, //提示的消息内容
        type: type, //消息类型：'success' | 'warning' | 'info' | 'error'
    })
}

/**
 * 获取存储在sessionStorage或者localStorage中的token(jwt)的名字
 *
 * @returns {string}
 */
export function getTokenName() {
    return "dlyk_token";
}

/**
 * 删除localStorage和sessionStorage中存储的token
 *
 */
export function removeToken() {
    window.sessionStorage.removeItem(getTokenName());
    window.localStorage.removeItem(getTokenName());
}

/**
 * 消息确认提示
 *
 * @param msg
 * @returns {Promise<MessageBoxData>}
 */
export function messageConfirm(msg) {
    return ElMessageBox.confirm(
        msg, //提示语
        '系统提醒', //提示的标题
        {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning',
        }
    )
}

/**
 * 封装返回函数
 *
 */
export function goBack() {
    window.history.back();
}

/**
 * 获取token
 *
 * @returns {string}
 */
export function getToken() {
    let token = window.sessionStorage.getItem(getTokenName());
    if (!token) { //前面加了一个！，表示token不存在，token是空的，token没有值，这个意思
        token = window.localStorage.getItem(getTokenName());
    }
    if (token) { //表示token存在，token不是空的，token有值，这个意思
        return token;
    } else {
        messageConfirm("请求token为空，是否重新去登录？").then(() => { //用户点击"确定"按钮就会触发then函数
            //既然后端验证token未通过，那么前端的token肯定是有问题的，那没必要存储在浏览器中，直接删除一下
            removeToken();
            //跳到登录页
            window.location.href = "/";
        }).catch(() => { //用户点击"取消"按钮就会触发catch函数
            messageTip("取消去登录", "warning");
        })
    }
}

/**
 * 获取缓存的用户权限列表
 *
 * @returns {Array|null}
 */
export function getUserPermission() {
    const cached = window.sessionStorage.getItem('user_permissions');
    if (cached) {
        try {
            return JSON.parse(cached);
        } catch (e) {
            return null;
        }
    }
    return null;
}

/**
 * 设置用户权限列表到缓存
 *
 * @param {Array} permissions
 */
export function setUserPermission(permissions) {
    window.sessionStorage.setItem('user_permissions', JSON.stringify(permissions));
}

/**
 * 清除用户权限缓存
 *
 */
export function clearUserPermission() {
    window.sessionStorage.removeItem('user_permissions');
}
