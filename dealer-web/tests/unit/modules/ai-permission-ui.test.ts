import { render, screen } from '@testing-library/vue'
import { describe, expect, it } from 'vitest'

import AiProactivePanel from '@/modules/ai/components/AiProactivePanel.vue'
import AiProposalCard from '@/modules/ai/components/AiProposalCard.vue'
import AiWorkflowPanel from '@/modules/ai/components/AiWorkflowPanel.vue'

describe('ai permission-aware controls', () => {
  it('keeps proposal readable but hides confirmation controls without permission', () => {
    render(AiProposalCard, {
      props: {
        canConfirm: false,
        proposal: {
          proposalId: 1,
          proposalType: 'create_follow_task_proposal',
          riskLevel: 'LOW',
          permissionCode: 'follow-task:create',
          relatedObjectType: 'CUSTOMER',
          relatedObjectId: '99',
          paramsSummary: '明天电话回访客户',
          impactSummary: '确认后创建一条跟进任务',
          expiresTime: '2026-07-12T10:00:00+08:00',
          status: 'PENDING_CONFIRMATION',
        },
      },
    })

    expect(screen.getByText('创建跟进任务')).toBeTruthy()
    expect(screen.getByText(/没有确认权限/)).toBeTruthy()
    expect(screen.queryByRole('button', { name: '确认' })).toBeNull()
    expect(screen.queryByText('follow-task:create')).toBeNull()
    expect(screen.queryByText('99')).toBeNull()
  })

  it('shows workflow progress without management controls', () => {
    render(AiWorkflowPanel, {
      props: {
        canManage: false,
        workflows: [
          {
            workflowNo: 'AIW1',
            workflowType: 'CUSTOMER_FOLLOW_UP',
            title: '客户跟进处理',
            status: 'RUNNING',
            steps: [{ stepNo: 1, stepType: 'READ_CUSTOMER', title: '查询客户信息', status: 'RUNNING' }],
          },
        ],
      },
    })

    expect(screen.getByText('查询客户信息')).toBeTruthy()
    expect(screen.queryByRole('button', { name: '暂停' })).toBeNull()
    expect(screen.queryByText('AIW1')).toBeNull()
  })

  it('shows proactive summaries without subscription actions', () => {
    render(AiProactivePanel, {
      props: {
        canManage: false,
        subscriptions: [
          {
            subscriptionNo: 'AIS1',
            subscriptionType: 'FOLLOW_UP_REMINDER',
            status: 'ACTIVE',
            frequency: 'DAILY',
            dailyLimit: 3,
            maxResults: 5,
            duplicateWindowMinutes: 60,
          },
        ],
        events: [],
      },
    })

    expect(screen.getByText('跟进提醒')).toBeTruthy()
    expect(screen.getByText(/每天/)).toBeTruthy()
    expect(screen.queryByRole('button', { name: '生成' })).toBeNull()
    expect(screen.queryByRole('button', { name: '暂停' })).toBeNull()
  })
})
