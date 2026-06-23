import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { saveBlob } from '@/shared/utils/browser-download'

describe('saveBlob', () => {
  let createObjectURLSpy: ReturnType<typeof vi.spyOn>
  let revokeObjectURLSpy: ReturnType<typeof vi.spyOn>

  beforeEach(() => {
    vi.useFakeTimers()
    createObjectURLSpy = vi.spyOn(URL, 'createObjectURL').mockReturnValue('blob:fake-url')
    revokeObjectURLSpy = vi.spyOn(URL, 'revokeObjectURL').mockImplementation(() => undefined)
  })

  afterEach(() => {
    createObjectURLSpy.mockRestore()
    revokeObjectURLSpy.mockRestore()
    vi.useRealTimers()
  })

  it('creates object URL, triggers download, and revokes URL', () => {
    const blob = new Blob(['test'], { type: 'text/plain' })

    saveBlob(blob, 'test.txt')

    expect(createObjectURLSpy).toHaveBeenCalledWith(blob)
    expect(revokeObjectURLSpy).not.toHaveBeenCalled()
    vi.runAllTimers()
    expect(revokeObjectURLSpy).toHaveBeenCalledWith('blob:fake-url')
  })

  it('revokes object URL even when appendChild throws', () => {
    const blob = new Blob(['test'], { type: 'text/plain' })

    const appendChildSpy = vi.spyOn(document.body, 'appendChild').mockImplementation(() => {
      throw new Error('appendChild failed')
    })

    expect(() => saveBlob(blob, 'test.txt')).toThrow('appendChild failed')
    expect(revokeObjectURLSpy).not.toHaveBeenCalled()
    vi.runAllTimers()
    expect(revokeObjectURLSpy).toHaveBeenCalledWith('blob:fake-url')

    appendChildSpy.mockRestore()
  })
})
