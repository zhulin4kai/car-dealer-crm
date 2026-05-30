import { doPost, doGet, doDelete, doPut } from '../http/httpRequest';

export function batchDeleteCluesByIds(ids) {
    return doPost('/api/clue/batch', ids)
}

export function getCurrentClues(current) {
    return doGet('/api/clues', {
        current: current
    })
}

export function importExcelAPI(file) {
    return doPost('/api/importExcel', file)
}

export function delClueById(id) {
    return doDelete('/api/clue/' + id)
}

export function checkPhoneIsExist(phone) {
    return doGet("/api/clue/" + phone, {});
}

export function getLoginInfo() {
    return doGet("/api/login/info", {});
}

export function getClueDetail(id) {
    return doGet("/api/clue/detail/" + id, {});
}

export function addClue(formData) {
    return doPost("/api/clue", formData);
}

export function updateClue(formData) {
    return doPut("/api/clue", formData);
}

// 线索详情页相关的 API 函数
export function addClueRemark(clueId, noteContent, noteWay) {
    return doPost("/api/clue/remark", {
        clueId: clueId,
        noteContent: noteContent,
        noteWay: noteWay
    });
}

export function getClueRemarkList(current, clueId) {
    return doGet("/api/clue/remark", {
        current: current,
        clueId: clueId
    });
}

export function convertClueToCustomer(clueId, product, description, nextContactTime) {
    return doPost("/api/clue/customer", {
        clueId: clueId,
        product: product,
        description: description,
        nextContactTime: nextContactTime
    });
}