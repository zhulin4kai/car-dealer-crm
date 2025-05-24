import { doGet, doPost } from '../http/httpRequest';

export function getActivityList(params) {
  return doGet('/api/activitys', params);
}

export function getOwnerList() {
  return doGet('/api/owner');
}

export function batchDeleteActivities(ids) {
  return doPost('/api/activity/batch',  ids );
}
