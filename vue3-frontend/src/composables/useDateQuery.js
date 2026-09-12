import { onBeforeUnmount, reactive } from 'vue'
import { api } from '../api'

export function restaurantToday() {
  const parts = new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Shanghai', year: 'numeric', month: '2-digit', day: '2-digit' }).formatToParts(new Date())
  return ['year', 'month', 'day'].map(type => parts.find(p => p.type === type).value).join('-')
}

export function useDateQuery(endpoint) {
  const today = restaurantToday()
  const q = reactive({ items: [], startDate: today, endDate: today, appliedStart: today, appliedEnd: today,
    page: 0, size: 10, totalElements: 0, totalPages: 0, loading: false, loaded: false, error: '' })
  let controller
  let generation = 0
  q.load = async () => {
    controller?.abort()
    controller = new AbortController()
    const current = ++generation
    q.loading = true; q.error = ''; q.items = []
    try {
      const params = new URLSearchParams({ startDate: q.appliedStart, endDate: q.appliedEnd, page: q.page, size: q.size })
      const data = await api(`${endpoint}?${params}`, { signal: controller.signal })
      if (current !== generation) return
      q.totalElements = data.totalElements; q.totalPages = data.totalPages; q.loaded = true
      if (q.totalPages > 0 && q.page >= q.totalPages) { q.page = q.totalPages - 1; return await q.load() }
      q.items = data.content
    } catch (e) {
      if (current === generation && e.name !== 'AbortError') { q.error = e.message; q.totalElements = 0; q.totalPages = 0 }
    } finally { if (current === generation) q.loading = false }
  }
  q.search = () => {
    if (!q.startDate || !q.endDate || q.startDate > q.endDate) { q.error = '请选择有效日期，结束日期不能早于开始日期'; return }
    q.appliedStart = q.startDate; q.appliedEnd = q.endDate; q.page = 0
    return q.load()
  }
  q.today = () => { q.startDate = restaurantToday(); q.endDate = q.startDate; return q.search() }
  q.turn = page => { if (!q.loading && page >= 0 && page < q.totalPages) { q.page = page; return q.load() } }
  q.resize = () => { q.page = 0; return q.load() }
  q.ensure = () => { if (!q.loaded && !q.loading) return q.load() }
  onBeforeUnmount(() => { generation++; controller?.abort() })
  return q
}
