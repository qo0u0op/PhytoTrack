/**
 * 案件狀態（Case Status）常數與標籤
 *
 * 後端以列舉字串傳遞（case-lifecycle 遷移完成）：PENDING（待處理）、
 * RESOLVED（已處理）、CLOSED（已結案）。
 */

export type CaseStatusValue = 'PENDING' | 'RESOLVED' | 'CLOSED'

/** 狀態下拉選單選項（value 為後端接受的列舉字串） */
export const STATUS_OPTIONS: { value: CaseStatusValue; label: string }[] = [
  { value: 'PENDING', label: '待處理' },
  { value: 'RESOLVED', label: '已處理' },
  { value: 'CLOSED', label: '已結案' },
]

/** 依字串狀態回傳顯示標籤；未知值回傳「待處理」以相容既有資料 */
export function statusLabel(status?: string): string {
  switch (status) {
    case 'RESOLVED':
      return '已處理'
    case 'CLOSED':
      return '已結案'
    default:
      return '待處理'
  }
}

/** 依字串狀態回傳 Bootstrap badge 樣式；未知值回傳待處理樣式 */
export function statusBadgeClass(status?: string): string {
  switch (status) {
    case 'RESOLVED':
      return 'text-bg-success'
    case 'CLOSED':
      return 'text-bg-dark'
    default:
      return 'text-bg-secondary'
  }
}