/** 將不可信字串轉義，避免其內含的 HTML 被當作標籤執行 (防 XSS) */
export function escapeHtml (value: string): string {
  return value
    .replace (/&/g, '&amp;')
    .replace (/</g, '&lt;')
    .replace (/>/g, '&gt;')
    .replace (/"/g, '&quot;')
    .replace (/'/g, '&#39;')
}
