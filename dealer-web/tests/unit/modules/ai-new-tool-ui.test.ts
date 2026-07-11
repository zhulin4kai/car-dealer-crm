import { render, screen } from '@testing-library/vue'
import { describe, expect, it } from 'vitest'

import AiToolResultCard from '@/modules/ai/components/AiToolResultCard.vue'
import type { AiToolResult } from '@/modules/ai/model/ai.types'

describe('new ai readonly tool displays', () => {
  it('renders opportunity details with business labels and translated stage', () => {
    renderResult({
      toolName: 'get_opportunity_detail',
      summary: '返回商机详情',
      data: {
        id: 101,
        opportunityNo: 'SJ-2026-001',
        customerName: '张伟',
        ownerName: '李敏',
        productName: '宝马 X5',
        stage: 'NEED_ANALYSIS',
        expectedAmount: 520000,
        expectedCloseDate: '2026-08-10',
        nextActionTime: '2026-07-15',
        requirement: '家庭用车',
      },
    })

    expect(screen.getByText('商机进展')).toBeTruthy()
    expect(screen.getByText('需求分析')).toBeTruthy()
    expect(screen.getAllByText('SJ-2026-001').length).toBeGreaterThanOrEqual(1)
    expect(screen.queryByText('NEED_ANALYSIS')).toBeNull()
    expect(screen.queryByText('101')).toBeNull()
  })

  it('renders quote totals and safe item table without internal ids or status codes', () => {
    renderResult({
      toolName: 'get_quote_detail',
      summary: '返回报价详情',
      data: {
        id: 202,
        customerId: 11,
        opportunityId: 22,
        quoteNo: 'BJ-2026-001',
        status: 'PENDING_APPROVAL',
        versionNo: 3,
        validUntil: '2026-07-31T18:00:00',
        totalAmount: 498000,
        totalItemCount: 1,
        items: [
          {
            productSku: 'INTERNAL-SKU',
            productName: '宝马 X5',
            productSpecification: '尊享型',
            unitPrice: 500000,
            quantity: 1,
            lineAmount: 498000,
            promotionName: '现车优惠',
            promotionAmount: 2000,
          },
        ],
      },
    })

    expect(screen.getByText('报价详情')).toBeTruthy()
    expect(screen.getAllByText('待审批').length).toBeGreaterThanOrEqual(1)
    expect(screen.getByText('报价商品')).toBeTruthy()
    expect(screen.getByText('宝马 X5')).toBeTruthy()
    expect(screen.queryByText('PENDING_APPROVAL')).toBeNull()
    expect(screen.queryByText('INTERNAL-SKU')).toBeNull()
    expect(screen.queryByText('202')).toBeNull()
  })

  it('renders test drive details without ids, VIN or internal enums', () => {
    renderResult({
      toolName: 'get_test_drive_detail',
      summary: '返回试驾详情',
      data: {
        id: 303,
        testDriveNo: 'SJ-TRY-001',
        customerName: '王芳',
        vehicleName: '雷克萨斯 ES',
        ownerName: '陈晨',
        status: 'SCHEDULED',
        plannedStartTime: '2026-07-15T10:00:00',
        plannedEndTime: '2026-07-15T11:00:00',
        contactName: '王芳',
        contactPhoneMasked: '138****1001',
        nextAction: '到店前再次确认',
      },
    })

    expect(screen.getByText('试驾详情')).toBeTruthy()
    expect(screen.getByText('已预约')).toBeTruthy()
    expect(screen.getByText('138****1001')).toBeTruthy()
    expect(screen.queryByText('SCHEDULED')).toBeNull()
    expect(screen.queryByText('303')).toBeNull()
  })

  it('renders delivery information without exposing related database ids', () => {
    renderResult({
      toolName: 'get_delivery_detail',
      summary: '返回交付详情',
      data: {
        id: 404,
        tranId: 44,
        customerId: 55,
        vehicleId: 66,
        responsibleUserId: 77,
        status: 'PENDING_SIGN',
        plannedDeliveryTime: '2026-07-20T10:00:00',
        signerName: '赵先生',
        signMethod: 'ELECTRONIC',
      },
    })

    expect(screen.getByText('交付详情')).toBeTruthy()
    expect(screen.getAllByText('待签收').length).toBeGreaterThanOrEqual(1)
    expect(screen.getByText('电子签收')).toBeTruthy()
    expect(screen.queryByText('PENDING_SIGN')).toBeNull()
    expect(screen.queryByText('404')).toBeNull()
    expect(screen.queryByText('77')).toBeNull()
  })

  it('renders vehicle product status with a business label', () => {
    renderResult({
      toolName: 'resolve_vehicle_product',
      summary: '返回车辆商品',
      data: {
        id: 505,
        sku: 'LEXUS-ES300H-25-P',
        name: '雷克萨斯 ES',
        categoryName: '新能源车',
        specification: '2025款 300h 尊享版',
        price: 399900,
        stock: 3,
        minStock: 1,
        status: 'ON_SALE',
      },
    })

    expect(screen.getByText('上架')).toBeTruthy()
    expect(screen.queryByText('ON_SALE')).toBeNull()
    expect(screen.queryByText('状态待确认')).toBeNull()
    expect(screen.queryByText('505')).toBeNull()
  })

  it('renders business metrics and bounded distributions with Chinese labels', () => {
    renderResult({
      toolName: 'get_business_overview',
      summary: '返回经营概览',
      data: {
        summary: {
          effectiveActivityCount: 3,
          totalActivityCount: 5,
          totalClueCount: 18,
          totalCustomerCount: 9,
          successTranAmount: 800000,
          totalTranAmount: 1200000,
        },
        salesFunnel: [{ name: 'NEED_ANALYSIS', value: 4 }],
        sourceDistribution: [{ name: 'CUSTOMER_REFERRAL', value: 6 }],
      },
    })

    expect(screen.getByText('经营概览')).toBeTruthy()
    expect(screen.getByText('销售漏斗')).toBeTruthy()
    expect(screen.getByText('需求分析')).toBeTruthy()
    expect(screen.getByText('客户转介绍')).toBeTruthy()
    expect(screen.queryByText('effectiveActivityCount')).toBeNull()
    expect(screen.queryByText('CUSTOMER_REFERRAL')).toBeNull()
  })

  it('hides unknown payload fields instead of falling back to raw JSON', () => {
    renderResult({
      toolName: 'future_readonly_tool',
      summary: '查询已完成',
      data: { secretBackendField: 'DO_NOT_RENDER', id: 999 },
    })

    expect(screen.getAllByText('查询已完成')).toHaveLength(2)
    expect(screen.getByText('查询已完成，当前结果暂无可展示的业务明细。')).toBeTruthy()
    expect(screen.queryByText('DO_NOT_RENDER')).toBeNull()
    expect(screen.queryByText('secretBackendField')).toBeNull()
    expect(screen.queryByText('999')).toBeNull()
  })
})

function renderResult(result: AiToolResult): void {
  render(AiToolResultCard, { props: { result } })
}
