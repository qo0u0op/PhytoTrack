import { describe, expect, it } from 'vitest'
import { STATUS_OPTIONS, statusBadgeClass, statusLabel } from '../caseStatus'

describe('caseStatus', () => {
  it('STATUS_OPTIONS 應對應後端列舉字串', () => {
    expect(STATUS_OPTIONS.map((o) => o.value)).toEqual(['PENDING', 'RESOLVED', 'CLOSED'])
    expect(STATUS_OPTIONS.map((o) => o.label)).toEqual(['待處理', '已處理', '已結案'])
  })

  it('statusLabel 應對映整數狀態', () => {
    expect(statusLabel(0)).toBe('待處理')
    expect(statusLabel(1)).toBe('已處理')
    expect(statusLabel(2)).toBe('已結案')
  })

  it('statusLabel 對未知狀態應回傳待處理（相容既有資料）', () => {
    expect(statusLabel(-1)).toBe('待處理')
    expect(statusLabel(9)).toBe('待處理')
  })

  it('statusBadgeClass 應對映整數狀態樣式', () => {
    expect(statusBadgeClass(0)).toBe('text-bg-secondary')
    expect(statusBadgeClass(1)).toBe('text-bg-success')
    expect(statusBadgeClass(2)).toBe('text-bg-dark')
    expect(statusBadgeClass(9)).toBe('text-bg-secondary')
  })
})