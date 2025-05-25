import { doPost, doGet, doDelete } from '../http/httpRequest';

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