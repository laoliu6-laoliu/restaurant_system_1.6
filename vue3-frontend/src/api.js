export async function api(path, options = {}) {
  const response = await fetch(`/api${path}`, {
    credentials: 'same-origin',
    ...options,
    headers: { 'Content-Type': 'application/json', ...options.headers }
  })
  const data = await response.json().catch(() => null)
  if (!response.ok) {
    if (response.status === 401) {
      sessionStorage.removeItem('session')
      window.location.assign('/')
    }
    throw new Error(data?.message || `请求失败（${response.status}），请重试`)
  }
  return data
}

export const paymentMethods = {
  DEMO_WECHAT: '微信（模拟）',
  DEMO_ALIPAY: '支付宝（模拟）',
  DEMO_CASH: '现金（模拟）'
}
