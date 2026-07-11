<script setup lang="ts">
import { computed } from 'vue'
import DOMPurify from 'dompurify'
import MarkdownIt from 'markdown-it'

defineOptions({
  name: 'AiMarkdownMessage',
})

const props = defineProps<{
  content: string
}>()

const markdown = new MarkdownIt({
  html: false,
  breaks: true,
  linkify: true,
})
const defaultLinkOpen = markdown.renderer.rules.link_open
markdown.renderer.rules.link_open = (tokens, index, options, env, self) => {
  const token = tokens[index]
  token.attrSet('target', '_blank')
  token.attrSet('rel', 'noopener noreferrer')
  return defaultLinkOpen ? defaultLinkOpen(tokens, index, options, env, self) : self.renderToken(tokens, index, options)
}

const allowedTags = [
  'a',
  'blockquote',
  'br',
  'code',
  'em',
  'hr',
  'li',
  'ol',
  'p',
  'pre',
  'strong',
  'table',
  'tbody',
  'td',
  'th',
  'thead',
  'tr',
  'ul',
]

const normalizedContent = computed(() => normalizeModelMarkdown(props.content))

const renderedHtml = computed(() =>
  DOMPurify.sanitize(markdown.render(normalizedContent.value), {
    ALLOWED_TAGS: allowedTags,
    ALLOWED_ATTR: ['href', 'rel', 'target', 'title'],
    FORBID_TAGS: ['style', 'script', 'iframe', 'img'],
    FORBID_ATTR: ['style', 'onerror', 'onclick', 'onload'],
  }),
)

function normalizeModelMarkdown(content: string): string {
  const inlineMarkers = content.match(/(?:^|[。！？!?])\s*\d{1,2}[.、](?=[^\s\d])/g)
  if (!inlineMarkers || inlineMarkers.length < 2) return content
  return content.replace(
    /(^|[。！？!?])\s*(\d{1,2})[.、](?=[^\s\d])/g,
    (_match, punctuation: string, index: string) =>
      `${punctuation}${punctuation ? '\n\n' : ''}**${index}.** `,
  )
}
</script>

<template>
  <div class="ai-markdown-message" v-html="renderedHtml" />
</template>

<style scoped>
.ai-markdown-message {
  overflow-wrap: anywhere;
}

.ai-markdown-message :deep(p) {
  margin: 0;
}

.ai-markdown-message :deep(p + p),
.ai-markdown-message :deep(ul),
.ai-markdown-message :deep(ol),
.ai-markdown-message :deep(blockquote),
.ai-markdown-message :deep(pre),
.ai-markdown-message :deep(table) {
  margin-top: 0.5rem;
}

.ai-markdown-message :deep(ul),
.ai-markdown-message :deep(ol) {
  padding-left: 1.25rem;
}

.ai-markdown-message :deep(ul) {
  list-style: disc;
}

.ai-markdown-message :deep(ol) {
  list-style: decimal;
}

.ai-markdown-message :deep(code) {
  border-radius: 4px;
  background: var(--crm-bg-muted);
  padding: 0.125rem 0.25rem;
  font-size: 0.85em;
}

.ai-markdown-message :deep(pre) {
  overflow-x: auto;
  border-radius: 6px;
  background: var(--crm-bg-muted);
  padding: 0.75rem;
}

.ai-markdown-message :deep(pre code) {
  background: transparent;
  padding: 0;
}

.ai-markdown-message :deep(table) {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.875em;
}

.ai-markdown-message :deep(th),
.ai-markdown-message :deep(td) {
  border: 1px solid var(--crm-border-light);
  padding: 0.375rem 0.5rem;
  text-align: left;
}

.ai-markdown-message :deep(blockquote) {
  border-left: 3px solid var(--crm-border-medium);
  padding-left: 0.75rem;
  color: var(--crm-text-secondary);
}
</style>
