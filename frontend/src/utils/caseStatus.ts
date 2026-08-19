/**
 * 案件狀態（Case Status）常數與標籤
 *
 * 後端 status 目前以整數儲存（過渡表示，見 case-search proposal）：
 * 0=PENDING（待處理）、1=RESOLVED（已處理）、2=CLOSED（已結案）。
 * 此處集中對映，待 case-lifecycle 遷移列舉後僅需調整此檔。
 */

export type CaseStatusValue = 'PENDING' | 'RESOLVED' | 'CLOSED'

/** 狀態下拉選單選項（value 為後端接受的列舉字串） */
export const STATUS_OPTIONS: { value: CaseStatusValue; label: string }[] = [
  { value: 'PENDING', label: '待處理' },
  { value: 'RESOLVED', label: '已處理' },
  { value: 'CLOSED', label: '已結案' },
]

/** 依整數狀態回傳顯示標籤；未知值回傳「待處理」以相容既有資料 */
export function statusLabel(status: number): string {
  switch (status) {
    case 1:
      return '已處理'
    case 2:
      return '已結案'
    default:
      return '待處理'
  }
}

/** 依整數狀態回傳 Bootstrap badge 樣式；未知值回傳待處理樣式 */
export function statusBadgeClass(status: number): string {
  switch (status) {
    case 1:
      return 'text-bg-success'
    case 2:
      return 'text-bg-dark'
    default:
      return 'text-bg-secondary'
  }
}