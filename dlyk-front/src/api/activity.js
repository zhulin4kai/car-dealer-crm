import { doGet, doPost, doDelete, doPut } from '../http/httpRequest';

export function getActivityList(params) {
  return doGet('/api/activitys', params);
}

export function getOwnerList() {
  return doGet('/api/owner');
}

export function batchDeleteActivities(ids) {
  return doPost('/api/activity/batch',  ids );
}

export function deleteActivity(id) {
  return doDelete(`/api/activity/${id}`);
}

export function getActivityById(id) {
  return doGet(`/api/activity/${id}`);
}

export function createActivity(formData) {
  return doPost('/api/activity', formData);
}

export function updateActivity(formData) {
  return doPut('/api/activity', formData);
}
