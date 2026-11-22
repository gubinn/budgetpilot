// 桌面端与移动端共用的展示格式化工具

export function formatNum(n) {
  if (n == null) return '0.00'
  return Number(n).toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  })
}

export function getProgressStatus(status) {
  return {
    EXCEEDED: 'error',
    WARNING: 'warning',
    CAUTION: 'warning',
    NORMAL: 'success'
  }[status] || 'success'
}