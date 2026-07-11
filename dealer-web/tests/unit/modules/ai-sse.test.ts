import { afterEach, describe, expect, it, vi } from 'vitest'

import { decodeAiSseStream, streamAiRunEvents } from '@/modules/ai/api/ai-api'
import type { AiSseEvent } from '@/modules/ai/model/ai.types'

function event(sequence: number, type: string, payload: Record<string, unknown>): string {
  return JSON.stringify({
    eventId: `event-${sequence}`,
    runNo: 'AIR1',
    sequence,
    type,
    occurredAt: '2026-07-11T10:00:00+08:00',
    payload,
  })
}

function byteStream(chunks: Uint8Array[]): ReadableStream<Uint8Array> {
  return new ReadableStream({
    start(controller) {
      chunks.forEach((chunk) => controller.enqueue(chunk))
      controller.close()
    },
  })
}

describe('ai sse decoder', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('decodes CRLF, LF, multiline data, split UTF-8 bytes and ignores bad frames', async () => {
    const first = event(1, 'message_delta', { content_delta: '你' })
    const second = event(2, 'message_delta', { content_delta: '好' })
    const completed = event(3, 'run_completed', { status: 'COMPLETED' })
    const secondBreak = Math.floor(second.length / 2)
    const source = [
      `data: ${first}\r\n\r\n`,
      'data: {not-json}\n\n',
      `data: ${second.slice(0, secondBreak)}\n`,
      `data: ${second.slice(secondBreak)}\n\n`,
      `data: ${completed}\r\n\r\n`,
    ].join('')
    const bytes = new TextEncoder().encode(source)
    const chineseByte = bytes.findIndex((value) => value > 127)
    const chunks = [
      bytes.slice(0, chineseByte + 1),
      bytes.slice(chineseByte + 1, chineseByte + 2),
      bytes.slice(chineseByte + 2),
    ]
    const received: AiSseEvent[] = []

    const result = await decodeAiSseStream(byteStream(chunks), (item) => received.push(item))

    expect(received.map((item) => [item.sequence, item.payload.content_delta])).toEqual([
      [1, '你'],
      [2, '好'],
      [3, undefined],
    ])
    expect(result).toEqual({ lastSequence: 3, terminal: true })
  })

  it('deduplicates events at or before afterSequence', async () => {
    const source = [
      `data: ${event(1, 'message_delta', { content_delta: '旧' })}`,
      `data: ${event(2, 'message_delta', { content_delta: '新' })}`,
      `data: ${event(3, 'run_completed', { status: 'COMPLETED' })}`,
      '',
    ].join('\n\n')
    const received: AiSseEvent[] = []

    await decodeAiSseStream(
      byteStream([new TextEncoder().encode(source)]),
      (item) => received.push(item),
      1,
    )

    expect(received.map((item) => item.sequence)).toEqual([2, 3])
  })

  it('keeps reading after an error until the explicit run terminal event', async () => {
    const source = [
      `data: ${event(1, 'error', { code: 'MODEL_PROVIDER_FAILED' })}`,
      `data: ${event(2, 'run_completed', { status: 'FAILED' })}`,
      '',
    ].join('\n\n')
    const received: AiSseEvent[] = []

    const result = await decodeAiSseStream(
      byteStream([new TextEncoder().encode(source)]),
      (item) => received.push(item),
    )

    expect(received.map((item) => item.type)).toEqual(['error', 'run_completed'])
    expect(result).toEqual({ lastSequence: 2, terminal: true })
  })

  it('reconnects with afterSequence and does not replay delivered events', async () => {
    const firstStream = byteStream([
      new TextEncoder().encode(`data: ${event(1, 'message_delta', { content_delta: '第' })}\n\n`),
    ])
    const resumedStream = byteStream([
      new TextEncoder().encode(
        [
          `data: ${event(1, 'message_delta', { content_delta: '旧' })}`,
          `data: ${event(2, 'message_delta', { content_delta: '二' })}`,
          `data: ${event(3, 'run_completed', { status: 'COMPLETED' })}`,
          '',
        ].join('\n\n'),
      ),
    ])
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(new Response(firstStream, { status: 200 }))
      .mockResolvedValueOnce(new Response(resumedStream, { status: 200 }))
    vi.stubGlobal('fetch', fetchMock)
    const received: AiSseEvent[] = []

    await streamAiRunEvents('AIR1', (item) => received.push(item))

    expect(String(fetchMock.mock.calls[1]?.[0])).toContain('afterSequence=1')
    expect(received.map((item) => item.sequence)).toEqual([1, 2, 3])
  })
})
