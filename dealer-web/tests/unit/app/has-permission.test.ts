import { describe, expect, it } from 'vitest'

import { setPermissionVisibility } from '@/app/directives/has-permission'

describe('has-permission directive visibility', () => {
  it('hides a denied element without removing it from Vue managed DOM', () => {
    const parent = document.createElement('div')
    const button = document.createElement('button')
    parent.appendChild(button)

    setPermissionVisibility(button, false)

    expect(button.parentNode).toBe(parent)
    expect(button.style.getPropertyValue('display')).toBe('none')
    expect(button.style.getPropertyPriority('display')).toBe('important')
    expect(button.getAttribute('aria-hidden')).toBe('true')
  })

  it('restores an element when permission becomes available', () => {
    const button = document.createElement('button')
    setPermissionVisibility(button, false)

    setPermissionVisibility(button, true)

    expect(button.style.getPropertyValue('display')).toBe('')
    expect(button.hasAttribute('aria-hidden')).toBe(false)
  })
})
