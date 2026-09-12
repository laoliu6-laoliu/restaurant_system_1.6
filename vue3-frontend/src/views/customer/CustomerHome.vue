<template>
  <div class="app-layout">
    <header class="top-bar">
      <div class="brand"><span class="brand-icon">🍽️</span><h1>臻味餐厅</h1><span class="version-badge">v1.6</span></div>
      <div class="user-area">
        <span class="user-badge">👤 {{ user.name }}（顾客）</span>
        <span class="cart-badge" v-if="cart.length" @click="activeTab = 'cart'">🛒 {{ cart.length }} 项</span>
        <button class="btn-logout" @click="logout">退出</button>
      </div>
    </header>
    <div class="layout-body">
      <aside class="sidebar">
        <nav>
          <a v-for="tab in tabs" :key="tab.id" :class="{ active: activeTab === tab.id }" @click="activeTab = tab.id">
            <span class="tab-icon">{{ tab.icon }}</span>{{ tab.label }}
          </a>
        </nav>
      </aside>
      <main class="main-content">
        <p v-if="pageError" class="error" role="alert">{{ pageError }} <button class="btn-sm" @click="loadData">重新加载</button></p>
        <p v-if="submitMsg" :class="submitError ? 'error' : 'success'" role="status">{{ submitMsg }}</p>
        <PaymentDialog v-if="paymentOrder" :order="paymentOrder" @close="paymentOrder = null" @paid="onPaid" />
        <PasswordChangeDialog v-if="passwordOpen" @close="passwordOpen = false" />
        <section v-if="activeTab === 'menu'" class="tab-page">
          <div class="page-header"><h2>🍜 菜品菜单</h2>
            <div class="filter-bar">
              <button v-for="t in dishTypes" :key="t" :class="{ active: filterType === t }" @click="filterType = t">{{ t }}</button>
            </div>
          </div>
          <p class="menu-intro">为这一餐，选一点喜欢的。<span>{{ menu.length }} 道精选 · 下单后可模拟支付</span></p>
          <div class="menu-grid">
            <div v-for="item in filteredMenu" :key="item.mno" class="menu-card">
              <DishImage :dish="item" />
              <div class="menu-card-header"><span class="menu-type-badge">{{ item.mtype }}</span><span class="menu-price">¥{{ item.mprice }}</span></div>
              <h3>{{ item.mname }}</h3><p class="menu-code">{{ item.mno }}</p><p class="menu-code">余量：{{ item.stock ?? '-' }}</p>
              <button class="btn-add" :disabled="(item.stock ?? 0) <= 0" @click="addToCart(item)">{{ (item.stock ?? 0) <= 0 ? '已售罄' : '➕ 加入购物车' }}</button>
            </div>
            <div v-if="filteredMenu.length === 0" class="empty-state"><p>暂无菜品数据</p></div>
          </div>
        </section>
        <section v-if="activeTab === 'cart'" class="tab-page">
          <div class="page-header"><h2>🛒 我的购物车</h2><button v-if="cart.length" class="btn-danger-text" @click="cart = []">清空购物车</button></div>
          <div v-if="cart.length" class="cart-list">
            <div v-for="(item, idx) in cart" :key="idx" class="cart-item">
              <DishImage :dish="item" small />
              <div class="cart-item-info"><h4>{{ item.mname }} <span class="cart-item-code">{{ item.mno }}</span></h4></div>
              <div class="cart-item-controls">
                <div class="qty-control"><button @click="changeQty(idx, -1)">−</button><span>{{ item.dishCount }}</span><button :disabled="item.dishCount >= item.stock" @click="changeQty(idx, 1)">+</button></div>
                <span class="cart-item-price">¥{{ (item.mprice * item.dishCount).toFixed(2) }}</span>
                <input class="remark-input" v-model="item.remark" placeholder="备注（选填）" maxlength="100" />
                <button class="btn-icon" @click="cart.splice(idx, 1)">🗑️</button>
              </div>
            </div>
            <div class="cart-footer">
              <div class="cart-summary"><span>共 {{ cart.reduce((s, i) => s + i.dishCount, 0) }} 件</span><span class="cart-total">合计：¥{{ cartTotal.toFixed(2) }}</span></div>
              <div class="cart-submit">
                <label>选择餐桌：</label>
                <select v-model="selectedTable">
                  <option value="">-- 请选择餐桌 --</option>
                  <option v-for="t in availableTables" :key="t.tno" :value="t.tno">{{ t.tno }}（{{ t.seats }}座 / {{ t.tstatus }}）</option>
                </select>
                <button class="btn-submit" :disabled="!selectedTable || !cart.length || submitting" @click="submitOrder">{{ submitting ? '提交中...' : '提交订单并支付' }}</button>
              </div>

              <div v-if="latestPreview.length" class="order-details">
                <div class="detail-row" v-for="item in latestPreview" :key="item.mno + item.remark">
                  <span>{{ item.mname }}</span><span>×{{ item.dishCount }}</span><span>¥{{ (item.mprice * item.dishCount).toFixed(2) }}</span><span v-if="item.remark" class="remark">备注：{{ item.remark }}</span>
                </div>
              </div>
            </div>
          </div>
          <div v-else class="empty-state"><p>🛒 购物车暂时为空，去菜单看看吧！</p></div>
        </section>
        <section v-if="activeTab === 'orders'" class="tab-page">
          <div class="page-header"><h2>📋 我的订单</h2></div>
          <DateQueryBar :query="ordersQuery" />
          <div v-if="myOrders.length" class="order-list">
            <div v-for="order in myOrders" :key="order.ono" class="order-card">
              <div class="order-header"><h3>订单 {{ order.orderNumber }} <small>{{ order.orderDate }}</small></h3><div><span :class="['status-tag', order.paymentStatus === 'PAID' ? 'payment-paid' : 'payment-unpaid']">{{ order.paymentStatus === 'PAID' ? '已支付（模拟）' : '待支付' }}</span><span :class="['status-tag', order.orderStatus === 'ENDED' ? 'order-ended' : 'order-active']">{{ order.orderStatus === 'ENDED' ? '已结束' : '进行中' }}</span></div></div>
              <div class="order-meta"><span>💰 ¥{{ order.totalAmount }}</span><span>🕐 {{ order.orderTime }}</span><span>🪑 {{ order.tno }}</span></div>
              <div class="order-payment-row">
                <button v-if="order.paymentStatus !== 'PAID'" class="btn-primary" @click="paymentOrder = order">去支付</button>
                <span v-else class="hint">{{ paymentMethods[order.payment?.method] }} · {{ order.payment?.paidAt?.replace('T', ' ') }}<br />流水：{{ order.payment?.paymentNo }}</span>
                <button class="btn-cancel" :disabled="detailsLoading[order.ono]" @click="toggleDetails(order)">{{ order.details ? '收起明细' : detailsLoading[order.ono] ? '加载中…' : '查看明细' }}</button>
              </div>
              <div v-if="order.details" class="order-details">
                <div v-for="d in order.details" :key="d.dno" class="detail-row">
                  <span>{{ d.mname }}</span><span>×{{ d.dishCount }}</span><span>¥{{ d.amount }}</span><span v-if="d.remark" class="remark">备注：{{ d.remark }}</span>
                </div>
              </div>
            </div>
          </div>
          <div v-else-if="!ordersQuery.loading" class="empty-state"><p>所选日期没有订单记录</p></div>
          <QueryPagination :query="ordersQuery" />
        </section>
        <section v-if="activeTab === 'reservations'" class="tab-page">
          <div class="page-header"><div><h2>📅 我的预约餐桌</h2><p class="query-description">查看当前账号已预约的餐桌、时段、座位与押金信息</p></div><button class="btn-cancel" :disabled="tableRefreshing" @click="refreshTables">{{ tableRefreshing ? '刷新中…' : '刷新预约' }}</button></div>
          <div class="customer-reservation-summary">
            <div><small>当前预约</small><strong>{{ activeCustomerReservations.length }}</strong></div>
            <div><small>预约总数</small><strong>{{ customerReservations.length }}</strong></div>
            <span><span class="live-dot"></span>餐桌状态更新于 {{ tableUpdatedAt || '--:--:--' }}</span>
          </div>
          <div v-if="activeCustomerReservations.length" class="my-reservation-grid">
            <article v-for="r in activeCustomerReservations" :key="r.reservationNo" class="my-reservation-card">
              <header><div><small>已预约餐桌</small><h3>{{ r.tno }}</h3></div><span :class="['status-tag', r.status === 'SEATED' ? 'payment-paid' : 'order-active']">{{ reservationStatusLabels[r.status] || r.status }}</span></header>
              <div class="reserved-table-overview">
                <div class="reserved-table-icon">🪑</div>
                <div><strong>{{ reservationTableInfo(r).seats ?? '--' }} 人桌</strong><span>实时桌况：{{ reservationTableInfo(r).tstatus || '未知' }}</span></div>
              </div>
              <dl class="reservation-info-list">
                <dt>预约时段</dt><dd>{{ formatSlot(r) }}</dd>
                <dt>到店截止</dt><dd>{{ formatDateTime(r.arrivalDeadline) }}</dd>
                <dt>用餐人数</dt><dd>{{ r.partySize }} 人</dd>
                <dt>联系电话</dt><dd>{{ r.customerPhone }}</dd>
                <dt>预约来源</dt><dd>{{ reservationSourceLabels[r.reservationSource] || '历史预约' }}</dd>
                <dt>预约编号</dt><dd>{{ r.reservationNo }}</dd>
              </dl>
              <div class="reservation-card-deposit"><span>预约押金<small>{{ paymentMethods[r.depositMethod] }}</small></span><strong>¥{{ Number(r.depositAmount).toFixed(2) }}</strong><span :class="['status-tag', r.depositStatus === 'REFUNDED' ? 'payment-paid' : r.depositStatus === 'FORFEITED' ? 'deposit-forfeited' : 'payment-unpaid']">{{ depositStatusLabels[r.depositStatus] || r.depositStatus }}</span></div>
            </article>
          </div>
          <div v-else class="empty-state compact"><p>当前没有待到店的预约餐桌，可前往“餐桌状态”选择空闲时段预约。</p><button class="btn-primary" @click="activeTab = 'tables'">去预约餐桌</button></div>
          <template v-if="reservationHistory.length">
            <div class="page-header reservation-history-heading"><div><h3>历史预约</h3><p class="query-description">已结账、已取消或未到店的预约记录</p></div></div>
            <div class="table-wrap"><table class="data-table">
              <thead><tr><th>预约时段</th><th>餐桌信息</th><th>人数</th><th>预约状态</th><th>押金状态</th></tr></thead>
              <tbody><tr v-for="r in reservationHistory" :key="r.reservationNo"><td>{{ formatSlot(r) }}<small class="table-date">{{ r.reservationNo }}</small></td><td><strong>{{ r.tno }}</strong><small class="table-date">{{ reservationTableInfo(r).seats ?? '--' }} 人桌</small></td><td>{{ r.partySize }} 人</td><td>{{ reservationStatusLabels[r.status] || r.status }}</td><td><span :class="['status-tag', r.depositStatus === 'REFUNDED' ? 'payment-paid' : r.depositStatus === 'FORFEITED' ? 'deposit-forfeited' : 'payment-unpaid']">{{ depositStatusLabels[r.depositStatus] || r.depositStatus }}</span></td></tr></tbody>
            </table></div>
          </template>
        </section>
        <section v-if="activeTab === 'profile'" class="tab-page">
          <div class="page-header"><h2>👤 个人信息</h2><div class="form-actions"><button class="btn-cancel" @click="passwordOpen = true">修改密码</button><button class="btn-primary" @click="editProfile = !editProfile">{{ editProfile ? '取消' : '编辑' }}</button></div></div>
          <div v-if="myInfo" class="profile-card">
            <template v-if="!editProfile">
              <div class="profile-row"><span>顾客编号：</span><strong>{{ myInfo.cno }}</strong></div>
              <div class="profile-row"><span>姓名：</span><strong>{{ myInfo.cname }}</strong></div>
              <div class="profile-row"><span>性别：</span><strong>{{ myInfo.csex }}</strong></div>
              <div class="profile-row"><span>电话：</span><strong>{{ myInfo.cphone }}</strong></div>
            </template>
            <template v-else>
              <div class="form-grid">
                <label>姓名<input v-model="editForm.cname" /></label>
                <label>性别<select v-model="editForm.csex"><option>男</option><option>女</option></select></label>
                <label>电话<input v-model="editForm.cphone" /></label>
              </div>
              <div class="form-actions">
                <button class="btn-primary" @click="saveProfile">保存</button>
              </div>
              <p v-if="profileMsg" :class="profileError ? 'error' : 'success'">{{ profileMsg }}</p>
            </template>
          </div>
        </section>
        <section v-if="activeTab === 'tables'" class="tab-page">
          <div class="page-header"><div><h2>🪑 餐桌状态与预约</h2><p class="query-description">实时状态不影响未来预约；同一餐桌在不同日期或空闲时段可重复预约</p></div><button class="btn-cancel" :disabled="tableRefreshing" @click="refreshTables">{{ tableRefreshing ? '刷新中…' : '立即刷新' }}</button></div>
          <div class="table-live-summary">
            <span class="live-dot"></span><strong>实时状态</strong><span>空闲 {{ statusCounts['空闲'] || 0 }}</span><span>使用中 {{ statusCounts['使用中'] || 0 }}</span><span>待清理 {{ statusCounts['待清理'] || 0 }}</span><span>已预订 {{ statusCounts['已预订'] || 0 }}</span><small>更新于 {{ tableUpdatedAt || '--:--:--' }}</small>
          </div>
          <div class="reservation-reminder"><span>☎️</span><div><strong>预约提醒</strong><p>线上餐桌状态仅供参考，建议预约前亲自致电餐厅询问，由员工确认用餐人数与预约时段。</p></div></div>
          <div v-if="selectedReservationTable" class="form-card customer-reservation-form">
            <div class="reservation-form-heading"><div><h3>预约 {{ selectedReservationTable.tno }}</h3><p>{{ selectedReservationTable.seats }} 人桌 · 已预约的时间段不可重复选择</p></div><button class="btn-icon" aria-label="关闭预约表单" @click="reservationForm.tno = ''">✕</button></div>
            <div class="form-grid">
              <label>预约日期<input v-model="reservationForm.date" type="date" :min="today" :max="maxReservationDate" /></label>
              <label>预约时间段<select v-model="reservationForm.slotStart" :disabled="availabilityLoading"><option v-for="slot in reservationSlots" :key="slot.start" :value="slot.start" :disabled="isSlotReserved(slot.start)">{{ slot.label }}{{ isSlotReserved(slot.start) ? '（已被预约）' : '' }}</option></select></label>
              <label>用餐人数<input v-model.number="reservationForm.partySize" type="number" min="1" :max="selectedReservationTable.seats" /></label>
              <label>押金支付方式<select v-model="reservationForm.depositMethod"><option value="DEMO_WECHAT">微信（模拟）</option><option value="DEMO_ALIPAY">支付宝（模拟）</option></select></label>
            </div>
            <p v-if="!availabilityLoading && !availableReservationSlots.length" class="error">该餐桌在所选日期的线上时段已全部约满，请更换日期或致电餐厅询问。</p>
            <div class="reservation-deposit"><span>需支付预约押金<small>按餐桌容量计费，结账后原路退还</small></span><strong>¥{{ reservationDeposit.toFixed(2) }}</strong></div>
            <p class="reservation-arrival-note">请在 {{ reservationForm.slotStart }} 后 30 分钟内到店并由员工接待开单；超时未到店将扣除押金并自动释放餐桌。</p>
            <div class="form-actions"><button class="btn-primary" :disabled="reservationSaving || availabilityLoading || !reservationForm.slotStart || isSlotReserved(reservationForm.slotStart)" @click="submitReservation">{{ reservationSaving ? '支付确认中…' : `支付 ¥${reservationDeposit.toFixed(2)} 押金并预约` }}</button><button class="btn-cancel" :disabled="reservationSaving" @click="reservationForm.tno = ''">取消</button></div>
          </div>
          <p v-if="reservationMsg" :class="reservationError ? 'error' : 'success'" role="status">{{ reservationMsg }}</p>
          <div class="table-grid">
            <div v-for="t in tables" :key="t.tno" :class="['table-card', statusClass(t.tstatus)]">
              <h3>{{ t.tno }}</h3><p>{{ t.seats }} 座</p><span class="table-status">{{ t.tstatus }}</span>
              <button class="btn-sm table-reserve-button" @click="startReservation(t)">预约此桌</button><small class="table-unavailable">可查看未来日期与空闲时段</small>
            </div>
          </div>
          <div class="reservation-policy customer-policy"><strong>押金标准</strong><span>2 人桌 ¥20</span><span>4 人桌 ¥30</span><span>6 人桌 ¥40</span><span>8 人桌 ¥50</span><span>12 人桌 ¥70</span><small>按桌型收费；按时到店并完成结账后原路退还。</small></div>
          <div class="reservation-list-shortcut"><div><strong>已经预约成功？</strong><p>前往“我的预约”查看餐桌编号、座位数、预约时段和押金状态。</p></div><button class="btn-primary" @click="activeTab = 'reservations'">查看我的预约</button></div>
        </section>
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import DishImage from '../../components/DishImage.vue'
import PaymentDialog from '../../components/PaymentDialog.vue'
import PasswordChangeDialog from '../../components/PasswordChangeDialog.vue'
import DateQueryBar from '../../components/DateQueryBar.vue'
import QueryPagination from '../../components/QueryPagination.vue'
import { restaurantToday, useDateQuery } from '../../composables/useDateQuery'
import { api, paymentMethods } from '../../api'

const API = '/api'
const paymentOrder = ref(null)
const pageError = ref('')
const passwordOpen = ref(false)
const detailsLoading = ref({})
const ordersQuery = useDateQuery('/orders')
async function onPaid() { await Promise.all([ordersQuery.load(), refreshTables()]) }
const router = useRouter()
const user = JSON.parse(sessionStorage.getItem('session') || '{}')

const activeTab = ref('menu')
const filterType = ref('全部')
const cart = ref([])
const selectedTable = ref('')
const submitting = ref(false)
const submitMsg = ref('')
const submitError = ref(false)
const menu = ref([])
const tables = ref([])
const myInfo = ref(null)
const editProfile = ref(false)
const editForm = ref({})
const profileMsg = ref('')
const profileError = ref(false)
const customerReservations = ref([])
const tableRefreshing = ref(false)
const tableUpdatedAt = ref('')
const reservationSaving = ref(false)
const reservationMsg = ref('')
const reservationError = ref(false)
const availabilityBlocks = ref([])
const availabilityLoading = ref(false)
const today = restaurantToday()
const maxReservationDate = dateAfter(365)
const reservationSlots = [
  { start: '11:00', label: '11:00–13:00 午餐' },
  { start: '13:30', label: '13:30–15:30 午后' },
  { start: '17:00', label: '17:00–19:00 晚餐' },
  { start: '19:30', label: '19:30–21:30 夜间' }
]
const reservationForm = ref(defaultReservationForm())
const reservationStatusLabels = { RESERVED: '已预订', SEATED: '已到店', COMPLETED: '已结账', CANCELLED: '已取消', NO_SHOW: '订单已取消（押金不退）' }
const depositStatusLabels = { PAID: '押金已支付', REFUNDED: '押金已退回', FORFEITED: '押金已扣除' }
const reservationSourceLabels = { ONLINE: '线上预约', PHONE: '电话预约' }

const tabs = [
  { id: 'menu', icon: '🍜', label: '菜品浏览' },
  { id: 'cart', icon: '🛒', label: '购物车' },
  { id: 'orders', icon: '📋', label: '我的订单' },
  { id: 'reservations', icon: '📅', label: '我的预约' },
  { id: 'tables', icon: '🪑', label: '餐桌状态' },
  { id: 'profile', icon: '👤', label: '个人信息' }
]

const dishTypes = computed(() => {
  const types = new Set(menu.value.map(m => m.mtype))
  return ['全部', ...Array.from(types)]
})

const filteredMenu = computed(() => {
  if (filterType.value === '全部') return menu.value
  return menu.value.filter(m => m.mtype === filterType.value)
})

const cartTotal = computed(() =>
  cart.value.reduce((s, i) => s + i.mprice * i.dishCount, 0)
)

const availableTables = computed(() =>
  tables.value.filter(t => t.tstatus === '空闲')
)
const latestPreview = computed(() => cart.value)
const selectedReservationTable = computed(() => tables.value.find(t => t.tno === reservationForm.value.tno))
const reservationDeposit = computed(() => selectedReservationTable.value ? depositForSeats(selectedReservationTable.value.seats) : 0)
const availableReservationSlots = computed(() => reservationSlots.filter(slot => !isSlotReserved(slot.start)))
const statusCounts = computed(() => tables.value.reduce((counts, table) => { counts[table.tstatus] = (counts[table.tstatus] || 0) + 1; return counts }, {}))
const activeCustomerReservations = computed(() => customerReservations.value.filter(r => r.status === 'RESERVED' || r.status === 'SEATED'))
const reservationHistory = computed(() => customerReservations.value.filter(r => r.status !== 'RESERVED' && r.status !== 'SEATED'))

const myOrders = computed(() => {
  return ordersQuery.items
})

async function toggleDetails(order) {
  if (order.details) { order.details = null; return }
  detailsLoading.value[order.ono] = true
  try {
    const details = await api(`/order-details?ono=${encodeURIComponent(order.ono)}`)
    order.details = details.map(d => ({ ...d, mname: menu.value.find(m => m.mno === d.mno)?.mname || d.mno }))
  } catch (e) { pageError.value = e.message }
  finally { detailsLoading.value[order.ono] = false }
}

function statusClass(status) {
  if (status === '空闲') return 'free'
  if (status === '使用中') return 'busy'
  if (status === '已预订') return 'reserved'
  if (status === '待清理') return 'cleaning'
  return 'other'
}

function dateAfter(days) {
  return new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Shanghai' }).format(new Date(Date.now() + days * 86400000))
}
function defaultReservationForm() { return { tno: '', date: dateAfter(1), slotStart: '11:00', partySize: 2, depositMethod: 'DEMO_WECHAT' } }
function depositForSeats(seats) { return 20 + Math.max(0, Math.floor((Math.max(Number(seats) || 2, 2) - 1) / 2)) * 10 }
function formatDateTime(value) { return value ? value.replace('T', ' ').slice(0, 16) : '--' }
function formatSlot(reservation) { return `${formatDateTime(reservation.reservedAt)}–${reservation.reservedUntil?.slice(11, 16) || '--:--'}` }
function reservationTableInfo(reservation) { return tables.value.find(table => table.tno === reservation.tno) || { tno: reservation.tno, seats: null, tstatus: '未知' } }

function isSlotReserved(slotStart, tno = reservationForm.value.tno) {
  if (!tno || !reservationForm.value.date || !slotStart) return false
  const start = new Date(`${reservationForm.value.date}T${slotStart}:00`).getTime()
  const end = start + 2 * 60 * 60 * 1000
  return availabilityBlocks.value.some(block => block.tno === tno && start < new Date(block.reservedUntil).getTime() && new Date(block.reservedAt).getTime() < end)
}

function selectFirstAvailableSlot() {
  if (!reservationForm.value.tno) return
  if (!reservationForm.value.slotStart || isSlotReserved(reservationForm.value.slotStart)) {
    reservationForm.value.slotStart = availableReservationSlots.value[0]?.start || ''
  }
}

let availabilityRequest = 0
async function loadAvailability(date = reservationForm.value.date) {
  if (!date) return
  const request = ++availabilityRequest
  availabilityLoading.value = true
  try {
    const blocks = await api(`/reservations/availability?date=${encodeURIComponent(date)}`)
    if (request !== availabilityRequest) return
    availabilityBlocks.value = blocks
    selectFirstAvailableSlot()
  } catch (e) {
    if (request === availabilityRequest) pageError.value = e.message
  } finally {
    if (request === availabilityRequest) availabilityLoading.value = false
  }
}

function startReservation(table) {
  const date = reservationForm.value.date || defaultReservationForm().date
  reservationForm.value = { ...defaultReservationForm(), date, tno: table.tno, partySize: Math.min(2, Number(table.seats || 1)) }
  selectFirstAvailableSlot()
  reservationMsg.value = ''; reservationError.value = false
}

async function submitReservation() {
  if (reservationSaving.value || !selectedReservationTable.value) return
  const form = reservationForm.value
  if (!form.date || !form.slotStart || isSlotReserved(form.slotStart) || !form.partySize || form.partySize > selectedReservationTable.value.seats) {
    reservationMsg.value = '请选择有效时间段，并确认用餐人数不超过餐桌座位数'; reservationError.value = true; return
  }
  reservationSaving.value = true; reservationMsg.value = ''
  try {
    const result = await api('/reservations', { method: 'POST', body: JSON.stringify({
      tno: form.tno, partySize: form.partySize, reservedAt: `${form.date}T${form.slotStart}:00`, depositMethod: form.depositMethod
    }) })
    reservationMsg.value = `${result.tno} 预约成功，已支付模拟押金 ¥${Number(result.depositAmount).toFixed(2)}；请于 ${formatDateTime(result.arrivalDeadline)} 前到店`
    reservationError.value = false; reservationForm.value = defaultReservationForm()
    await refreshTables()
  } catch (e) { reservationMsg.value = e.message; reservationError.value = true; await refreshTables() }
  finally { reservationSaving.value = false }
}

function addToCart(menuItem) {
  const existing = cart.value.find(i => i.mno === menuItem.mno)
  if ((existing?.dishCount || 0) >= (menuItem.stock || 0)) { submitMsg.value = '已达到当前库存上限'; submitError.value = true; return }
  if (existing) existing.dishCount++
  else cart.value.push({ ...menuItem, dishCount: 1, remark: '' })
  submitMsg.value = ''; submitError.value = false
}

function changeQty(idx, delta) {
  const item = cart.value[idx]
  if (delta > 0 && item.dishCount >= item.stock) return
  item.dishCount += delta
  if (item.dishCount <= 0) cart.value.splice(idx, 1)
}

async function submitOrder() {
  if (!selectedTable.value || !cart.value.length || submitting.value) return
  submitting.value = true; submitMsg.value = ''; submitError.value = false
  try {
    const order = await api('/orders/checkout', { method: 'POST', body: JSON.stringify({
      tno: selectedTable.value,
      items: cart.value.map(({ mno, dishCount, remark }) => ({ mno, dishCount, remark }))
    }) })
    cart.value = []; selectedTable.value = ''; activeTab.value = 'orders'
    submitMsg.value = '订单已提交，可立即支付或稍后在“我的订单”中支付。'
    await ordersQuery.today()
    paymentOrder.value = order
  } catch (e) { submitMsg.value = e.message; submitError.value = true; await loadData() }
  finally { submitting.value = false }
}

async function saveProfile() {
  try {
    const payload = { ...myInfo.value, ...editForm.value, cno: user.id }
    await api(`/customers/${user.id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    })
    profileMsg.value = '个人信息已更新'
    profileError.value = false
    editProfile.value = false
    await loadData()
  } catch (e) {
    profileMsg.value = e.message
    profileError.value = true
  }
}

async function loadData() {
  pageError.value = ''
  try {
    const [m, t, customers, reservations] = await Promise.all([
      api('/menu'), api('/tables'), api('/customers'), api('/reservations')
    ])
    menu.value = m; tables.value = t; customerReservations.value = reservations
    tableUpdatedAt.value = new Date().toLocaleTimeString('zh-CN', { hour12: false })
    cart.value.forEach(item => { item.stock = m.find(dish => dish.mno === item.mno)?.stock || 0 })
    myInfo.value = customers.find(c => c.cno === user.id) || null
    if (myInfo.value) editForm.value = { cname: myInfo.value.cname, csex: myInfo.value.csex, cphone: myInfo.value.cphone }
  } catch (e) { pageError.value = e.message }
}

async function refreshTables() {
  if (tableRefreshing.value) return
  tableRefreshing.value = true
  try {
    const [currentTables, reservations] = await Promise.all([api('/tables'), api('/reservations'), loadAvailability()])
    tables.value = currentTables; customerReservations.value = reservations
    tableUpdatedAt.value = new Date().toLocaleTimeString('zh-CN', { hour12: false })
  } catch (e) { pageError.value = e.message }
  finally { tableRefreshing.value = false }
}
async function logout() { await api('/auth/logout', { method: 'POST' }); sessionStorage.removeItem('session'); router.push('/') }

let tableStatusTimer
onMounted(async () => {
  await loadData()
  tableStatusTimer = window.setInterval(() => { if (activeTab.value === 'tables' || activeTab.value === 'reservations') refreshTables() }, 5000)
})
onBeforeUnmount(() => window.clearInterval(tableStatusTimer))
watch(activeTab, tab => {
  if (tab === 'orders') ordersQuery.ensure()
  if (tab === 'tables' || tab === 'reservations') refreshTables()
}, { immediate: true })
watch(() => reservationForm.value.date, date => loadAvailability(date), { immediate: true })
</script>
