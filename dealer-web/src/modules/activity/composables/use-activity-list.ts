import { ref } from 'vue'

import { fetchActivityPage } from '@/modules/activity/api/activity-api'
import type { Activity, ActivityQuery } from '@/modules/activity/model/activity.types'

export function useActivityList() {
  const activityList = ref<Activity[]>([])
  const total = ref(0)
  const pageSize = ref(10)
  const loading = ref(false)

  async function loadActivityPage(query: ActivityQuery): Promise<void> {
    loading.value = true
    try {
      const page = await fetchActivityPage(query)
      activityList.value = page.list
      total.value = page.total
      pageSize.value = page.pageSize ?? pageSize.value
    } finally {
      loading.value = false
    }
  }

  return {
    activityList,
    total,
    pageSize,
    loading,
    loadActivityPage,
  }
}
