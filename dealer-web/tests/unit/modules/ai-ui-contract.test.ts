import fs from 'node:fs'
import path from 'node:path'
import { render, screen } from '@testing-library/vue'
import { describe, expect, it } from 'vitest'

import AiMarkdownMessage from '@/modules/ai/components/AiMarkdownMessage.vue'
import AiToolResultCard from '@/modules/ai/components/AiToolResultCard.vue'

const srcDir = path.resolve(__dirname, '../../../src')
const dashboardLayout = path.join(srcDir, 'layouts/DashboardLayout.vue')
const aiModuleDir = path.join(srcDir, 'modules/ai')
const aiPage = path.join(srcDir, 'pages/dashboard/ai.vue')
const providerConfigPage = path.join(srcDir, 'pages/dashboard/ai/provider-configs.vue')
const providerPresets = path.join(aiModuleDir, 'constants/provider-presets.ts')
const proposalCard = path.join(aiModuleDir, 'components/AiProposalCard.vue')
const assistantPanel = path.join(aiModuleDir, 'components/AiAssistantPanel.vue')
const sidePanel = path.join(aiModuleDir, 'components/AiSidePanel.vue')
const toolResultCard = path.join(aiModuleDir, 'components/AiToolResultCard.vue')
const workflowPanel = path.join(aiModuleDir, 'components/AiWorkflowPanel.vue')
const markdownMessage = path.join(aiModuleDir, 'components/AiMarkdownMessage.vue')

function collectSourceFiles(dir: string): string[] {
  return fs.readdirSync(dir, { withFileTypes: true }).flatMap((entry) => {
    const fullPath = path.join(dir, entry.name)
    if (entry.isDirectory()) return collectSourceFiles(fullPath)
    return /\.(ts|vue)$/.test(entry.name) ? [fullPath] : []
  })
}

describe('ai frontend ui contracts', () => {
  it('mounts global floating entry and side panel in dashboard layout', () => {
    const source = fs.readFileSync(dashboardLayout, 'utf8')

    expect(source).toContain('AiFloatingButton')
    expect(source).toContain('AiSidePanel')
    expect(source).toContain('PERMISSIONS.ai.assistantUse')
    expect(source).toContain('Sparkles')
    expect(source).toContain('canUseAi && !isAiPage')
    expect(source).toContain("label: '智能助手'")
    expect(source).toContain('matches: (item) => item.code === AI_MENU_CODE')
    expect(source).toContain("[AI_MENU_CODE]: { name: 'AI 助手', icon: 'Sparkles' }")
    expect(source).not.toContain("'menu:ai',\n  'menu:activity'")
    expect(source).toContain('openAiPanel')
    expect(source).toContain('expandAiPanel')
    expect(source).toContain("objectType: 'CUSTOMER'")
    expect(source).toContain("objectType: 'TRANSACTION'")
    expect(source).not.toMatch(/dataScope|permissions\s*:/)
  })

  it('side panel compresses main content and expands to ai page with run context', () => {
    const source = fs.readFileSync(sidePanel, 'utf8')

    expect(source).toContain('w-[420px] shrink-0')
    expect(source).toContain('currentConversationNo')
    expect(source).toContain('@conversation-change="handleConversationChange"')
    expect(source).toContain('createAiConversation')
    expect(source).toContain('listAiConversations')
    expect(source).toContain('切换 AI 会话')
    expect(source).toContain('@run-change="currentRunNo = $event"')
    expect(source).toContain("$emit('expand', { conversationNo: currentConversationNo, runNo: currentRunNo })")
    expect(source).not.toMatch(/fixed|inset-y-0|shadow-\[/)
  })

  it('ai page uses compact provider config entry without occupying a toolbar row', () => {
    const source = fs.readFileSync(aiPage, 'utf8')

    expect(source).toContain('TooltipProvider')
    expect(source).toContain('size="icon"')
    expect(source).toContain('to="/dashboard/ai/provider-configs"')
    expect(source).toContain('PERMISSIONS.ai.providerConfigView')
    expect(source).not.toContain('border-b border-[var(--crm-border-light)] px-6 py-3')
    expect(source).not.toContain('<Settings class="mr-2 h-4 w-4" />')
  })

  it('provider config page follows crm data page form style and preset workflow', () => {
    const source = fs.readFileSync(providerConfigPage, 'utf8')
    const presets = fs.readFileSync(providerPresets, 'utf8')

    expect(source).toContain('crm-data-page')
    expect(source).toContain('crm-panel')
    expect(source).toContain('TableHeader')
    expect(source).toContain('DialogContent')
    expect(source).toContain('goBackToAiPage')
    expect(source).toContain('返回 AI 助手')
    expect(source).toContain('StatusBadge')
    expect(source).toContain('enabledStatusTone')
    expect(source).toContain('testStatusTone')
    expect(source).toContain('bg-[#F7FDF9]')
    expect(source).toContain('DropdownMenu')
    expect(source).toContain('MoreHorizontal')
    expect(source).toContain('aria-label="更多操作"')
    expect(source).toContain('<Label>模型提供商</Label>')
    expect(source).toContain('<Label>API Key</Label>')
    expect(source).toContain('AI_PROVIDER_PRESETS')
    expect(source).toContain('高级配置')
    expect(source).toContain('FIELD_LIMITS.maxOutputTokens.max')
    expect(source).toContain("maxOutputTokens: { min: 1, max: 4096, label: '最大输出 Token' }")
    expect(source).toContain('${label}必须在')
    expect(source).toContain('submitError')
    expect(source).toContain('getErrorMessage')
    expect(source).toContain('openRotateDialog')
    expect(source).toContain('border-[var(--crm-info-bg)] text-[var(--crm-info)]')
    expect(source).toContain('border-[var(--crm-success-bg)] text-[var(--crm-success)]')
    expect(source).toContain('border-[var(--crm-danger-bg)] text-[var(--crm-danger)]')
    expect(source).not.toContain('flex flex-wrap gap-1.5')
    expect(source).not.toContain('placeholder="Provider 名称"')
    expect(source).not.toContain('placeholder="轮换 API Key"')

    expect(presets).toContain('千问（阿里云百炼）')
    expect(presets).toContain('https://dashscope.aliyuncs.com/compatible-mode/v1')
    expect(presets).toContain('deepseek-v4-pro')
    expect(presets).toContain('MiniMax-M3')
    expect(presets).toContain('自定义 Anthropic')
  })

  it('proposal card confirms saved proposal without editable business parameters', () => {
    const source = fs.readFileSync(proposalCard, 'utf8')

    expect(source).toContain("emit('confirm', proposal)")
    expect(source).toContain("emit('reject', proposal)")
    expect(source).toContain('proposal.impactSummary')
    expect(source).toContain('proposal.expiresTime')
    expect(source).not.toMatch(/<Input|<Textarea|v-model=/)
  })

  it('assistant panel displays turn-based sse and proposal events without direct html rendering', () => {
    const source = fs.readFileSync(assistantPanel, 'utf8')

    expect(source).toContain('streamAiRunEvents')
    expect(source).toContain('fetchAiRunTrace')
    expect(source).toContain('conversationTurns')
    expect(source).toContain('toTurnFromConversationTurn')
    expect(source).toContain('displayPayload')
    expect(source).toContain('AiMarkdownMessage')
    expect(source).toContain('proposal_created')
    expect(source).toContain('confirmAiProposal')
    expect(source).toContain('AiWorkflowPanel')
    expect(source).toContain('AiProactivePanel')
    expect(source).toContain('recommendations')
    expect(source).toContain('data-testid="ai-empty-state"')
    expect(source).toContain('data-testid="ai-composer"')
    expect(source).toContain('data-testid="ai-execution-details"')
    expect(source).toContain('data-testid="ai-business-results"')
    expect(source).toContain('data-testid="ai-conversation-turn"')
    expect(source).toContain('查看处理过程')
    expect(source).toContain('已停止生成，已保留当前部分内容。')
    expect(source).toContain('max-w-[980px]')
    expect(source).toContain('rows="1"')
    expect(source).not.toContain('min-h-20 resize-none')
    expect(source).not.toContain('已完成，本次结果见下方卡片')
    expect(source).not.toContain('工具摘要')
    expect(source).not.toContain('执行细节')
    expect(source).not.toContain('startWorkflow')
    expect(source).not.toContain('v-html')
  })

  it('tool result cards render crm business labels instead of raw fields', () => {
    const source = fs.readFileSync(toolResultCard, 'utf8')

    expect(source).toContain('交易结果')
    expect(source).toContain('客户结果')
    expect(source).toContain('库存预警')
    expect(source).toContain('跟进任务')
    expect(source).toContain('商品信息')
    expect(source).toContain('交易编号')
    expect(source).toContain('客户')
    expect(source).toContain('金额')
    expect(source).toContain('状态')
    expect(source).toContain('创建时间')
    expect(source).toContain('建议在交易管理中查看审批、收款、发票和交付事项。')
    expect(source).toContain('formatCurrency')
    expect(source).toContain('formatDateTime')
    expect(source).not.toContain('{{ result.objectRefs }}')
    expect(source).not.toContain('{{ key }}')
    expect(source).not.toContain('Object.entries(item)')
    expect(source).not.toMatch(/v-for="\[key,\s*value\]/)
  })

  it('renders transaction cards from persisted display payload', () => {
    render(AiToolResultCard, {
      props: {
        result: {
          toolName: 'list_pending_transaction_approvals',
          summary: '返回待审批交易 1 条',
          objectRefs: 'TRAN:PENDING',
          data: {
            items: [
              {
                tranNo: 'XS202606120001',
                customerName: '张伟',
                money: 509800,
                stageLabel: '待审批',
                createTime: '2026-06-12T11:06:00',
              },
            ],
          },
        },
      },
    })

    expect(screen.getByText('交易编号')).toBeTruthy()
    expect(screen.getAllByText('XS202606120001').length).toBeGreaterThanOrEqual(1)
    expect(screen.getByText('张伟')).toBeTruthy()
    expect(screen.getByText(/509,800/)).toBeTruthy()
    expect(screen.queryByText('TRAN:PENDING')).toBeNull()
  })

  it('renders markdown safely without model html execution', () => {
    render(AiMarkdownMessage, {
      props: {
        content:
          '**重点**\n\n- 跟进客户\n\n| 字段 | 值 |\n| --- | --- |\n| 客户 | 张伟 |\n\n<script>alert(1)</script>',
      },
    })

    expect(screen.getByText('重点')).toBeTruthy()
    expect(screen.getByText('跟进客户')).toBeTruthy()
    expect(screen.getByText('张伟')).toBeTruthy()
    expect(document.body.innerHTML).not.toContain('<script>')
  })

  it('workflow panel displays business process without internal identifiers', () => {
    const source = fs.readFileSync(workflowPanel, 'utf8')

    expect(source).toContain('处理过程')
    expect(source).toContain('workflowTitle')
    expect(source).toContain('stepBusinessTitle')
    expect(source).toContain('stepBusinessSummary')
    expect(source).not.toContain('{{ workflow.workflowNo }}')
    expect(source).not.toContain('workflow.workflowNo')
    expect(source).not.toContain('stepTypeLabel')
    expect(source).not.toContain('step.stepType')
  })

  it('assistant panel restores tool results and proposal status from run trace', () => {
    const source = fs.readFileSync(assistantPanel, 'utf8')

    expect(source).toContain('detail.turns?.length')
    expect(source).toContain('turn.toolResults')
    expect(source).toContain('toolCall.displayPayload')
    expect(source).toContain('trace.proposals.map')
    expect(source).toContain('status: proposal.status')
    expect(source).toContain('trace.workflows ?? []')
  })

  it('ai module only renders sanitized markdown html in the dedicated component', () => {
    const offenders = collectSourceFiles(aiModuleDir).filter((file) => {
      const source = fs.readFileSync(file, 'utf8')
      if (/dealer-ai|localhost:8091/.test(source)) return true
      if (/v-html/.test(source)) return path.normalize(file) !== path.normalize(markdownMessage)
      return false
    })

    expect(offenders.map((file) => path.relative(aiModuleDir, file))).toEqual([])
    const markdownSource = fs.readFileSync(markdownMessage, 'utf8')
    expect(markdownSource).toContain('html: false')
    expect(markdownSource).toContain('DOMPurify.sanitize')
    expect(markdownSource).toContain('v-html="renderedHtml"')
  })
})
