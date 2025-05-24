import { doPost } from '../http/httpRequest';

export function batchDeleteCluesByIds(ids) {
    return doPost('/api/clue/batch', ids)
}