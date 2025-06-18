import { doPost, doGet, doDelete, doPut } from '../http/httpRequest';

export function batchDeleteCluesByIds(ids) {
    return doPost('/api/clue/batch', ids)
}

export function getCurrentClues(current) {
    return doGet('api/clues', {
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

export function getOwnerList() {
    return doGet("/api/owner", {});
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