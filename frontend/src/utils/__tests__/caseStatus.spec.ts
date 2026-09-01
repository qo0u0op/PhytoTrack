import { describe, expect, it } from 'vitest'
import { STATUS_OPTIONS, statusBadgeClass, statusLabel } from '../caseStatus'

describe ('caseStatus', () => {
  it ('STATUS_OPTIONS 應對應後端列舉字串', () => {
    expect (STATUS_OPTIONS.map ((o) => o.value)).toEqual (['PENDING', 'RESOLVED', 'CLOSED'])
    expect (STATUS_OPTIONS.map ((o) => o.label)).toEqual (['待處理', '已處理', '已結案'])
  })

  it ('statusLabel 應對映字串狀態', () => {
    expect (statusLabel ('PENDING')).toBe ('待處理')
    expect (statusLabel ('RESOLVED')).toBe ('已處理')
    expect (statusLabel ('CLOSED')).toBe ('已結案')
  })

  it ('statusLabel 對未知狀態應回傳待處理 (相容既有資料)', () => {
    expect (statusLabel ('')).toBe ('待處理')
    expect (statusLabel ('UNKNOWN')).toBe ('待處理')
  })

  it ('statusBadgeClass 應對映字串狀態樣式', () => {
    expect (statusBadgeClass ('PENDING')).toBe ('text-bg-secondary')
    expect (statusBadgeClass ('RESOLVED')).toBe ('text-bg-success')
    expect (statusBadgeClass ('CLOSED')).toBe ('text-bg-dark')
    expect (statusBadgeClass ('UNKNOWN')).toBe ('text-bg-secondary')
  })
})