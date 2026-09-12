<template>
  <div class="app-layout">
    <header class="top-bar">
      <div class="brand"><span class="brand-icon">🍽️</span><h1>臻味餐厅 · 员工工作台</h1><span class="version-badge">v1.6</span></div>
      <div class="user-area">
        <span class="user-badge">👨‍🍳 {{ user.name }}（员工）</span>
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
        <p v-if="pageError" class="error" role="alert">{{ pageError }}</p>
        <PaymentDialog v-if="paymentOrder" :order="paymentOrder" staff @close="paymentOrder = null" @paid="onPaid" />
        <PasswordChangeDialog v-if="passwordOpen" @close="passwordOpen = false" />
        <section v-if="activeTab === 'reservations'" class="tab-page">
          <div class="page-header"><div><h2>📅 预约管理</h2><p class="query-description">统一查看线上与电话预约；顾客未到店时可取消订单且不退回押金</p></div><button class="btn-primary" @click="showReservationForm = !showReservationForm">{{ showReservationForm ? '收起登记' : '登记电话预约' }}</button></div>
          <div class="reservation-policy">
            <strong>押金标准</strong><span>2 人桌 ¥20</span><span>4 人桌 ¥30</span><span>6 人桌 ¥40</span><span>8 人桌 ¥50</span><span>12 人桌 ¥70</span><small>结账成功后自动退回；顾客未到店时取消订单且押金不退。</small>
          </div>
          <div v-if="showReservationForm" class="form-card reservation-form">
            <h3>登记电话预约</h3>
            <div class="form-grid">
              <label>预约人姓名<input v-model.trim="reservationForm.customerName" maxlength="30" placeholder="请输入来电顾客姓名" /></label>
              <label>联系电话<input v-model.trim="reservationForm.customerPhone" maxlength="20" inputmode="tel" placeholder="手机号或联系电话" /></label>
              <label>用餐人数<input v-model.number="reservationForm.partySize" type="number" min="1" max="100" /></label>
              <label>预约时间<input v-model="reservationForm.reservedAt" type="datetime-local" /></label>
              <label>选择餐桌<select v-model="reservationForm.tno"><option value="">-- 请选择 --</option><option v-for="t in reservationTables" :key="t.tno" :value="t.tno">{{ t.tno }} · {{ t.seats }} 座 · {{ t.tstatus }} · 押金 ¥{{ depositForSeats(t.seats) }}</option></select></label>
              <label>押金支付方式<select v-model="reservationForm.depositMethod"><option v-for="(label, key) in paymentMethods" :key="key" :value="key">{{ label }}</option></select></label>
            </div>
            <div class="reservation-deposit"><span>应收模拟押金</span><strong>¥{{ reservationDeposit.toFixed(2) }}</strong></div>
            <div class="form-actions"><button class="btn-primary" :disabled="reservationSaving" @click="createReservation">{{ reservationSaving ? '确认中…' : '收取押金并确认预约' }}</button><button class="btn-cancel" :disabled="reservationSaving" @click="showReservationForm = false">取消</button></div>
          </div>
          <p v-if="reservationMsg" :class="reservationError ? 'error' : 'success'" role="status">{{ reservationMsg }}</p>
          <div class="table-wrap"><table class="data-table">
            <thead><tr><th>预约来源</th><th>预约时间</th><th>餐桌编号</th><th>顾客</th><th>人数</th><th>押金</th><th>押金状态</th><th>预约状态</th><th>操作</th></tr></thead>
            <tbody><tr v-for="r in reservations" :key="r.reservationNo">
              <td><span :class="['status-tag', r.reservationSource === 'ONLINE' ? 'payment-paid' : 'payment-unpaid']">{{ reservationSourceLabels[r.reservationSource] || '历史预约' }}</span></td>
              <td>{{ r.reservedAt?.replace('T', ' ') }}<small class="table-date">{{ r.reservationNo.slice(0, 9) }}</small></td>
              <td><strong>{{ r.tno }}</strong></td>
              <td>{{ r.customerName }}<small class="table-date"><a :href="`tel:${r.customerPhone}`">{{ r.customerPhone }}</a></small></td>
              <td>{{ r.partySize }} 人</td><td>¥{{ r.depositAmount }}<small class="table-date">{{ paymentMethods[r.depositMethod] }}</small></td>
              <td><span :class="['status-tag', r.depositStatus === 'REFUNDED' ? 'payment-paid' : r.depositStatus === 'FORFEITED' ? 'deposit-forfeited' : 'payment-unpaid']">{{ depositStatusLabels[r.depositStatus] || r.depositStatus }}</span></td>
              <td>{{ reservationStatusLabels[r.status] || r.status }}</td>
              <td><button v-if="r.status === 'RESERVED'" class="btn-sm" @click="startReservedOrder(r)">接待并开单</button><button v-if="canMarkNoShow(r)" class="btn-sm danger" @click="markNoShow(r)">取消订单且不退回押金</button><button v-if="r.status === 'RESERVED'" class="btn-sm danger" @click="cancelReservation(r)">正常取消并退押金</button><span v-else-if="r.status === 'SEATED'" class="hint">账单支付后自动退款</span><span v-else-if="r.status === 'NO_SHOW'" class="hint">订单已取消，押金不退</span><span v-else class="hint">{{ r.depositRefundedAt ? `退款于 ${r.depositRefundedAt.replace('T', ' ')}` : '已结束' }}</span></td>
            </tr></tbody>
          </table></div>
          <div v-if="!reservations.length" class="empty-state"><p>暂无预约记录</p></div>
        </section>
        <section v-if="activeTab === 'orders'" class="tab-page">
          <div class="page-header"><div><h2>📦 订单管理</h2><p class="query-description">进行中的待支付和已支付订单均可换桌；订单结束后可删除</p></div><div class="form-actions"><button class="btn-cancel" @click="showTransferForm = !showTransferForm">{{ showTransferForm ? '收起换桌' : '订单换桌' }}</button><button class="btn-primary" @click="showOrderForm = true">新增订单</button></div></div>
          <DateQueryBar :query="ordersQuery" />
          <p v-if="orderMsg" :class="orderError ? 'error' : 'success'" role="status">{{ orderMsg }}</p>
          <div v-if="showTransferForm" class="form-card transfer-form">
            <h3>按订单编号换桌</h3>
            <p class="query-description">待支付和已支付订单均可换桌。订单编号每天从 001 重新开始，请同时核对日期；新桌锁定后，原桌将转为待清理。</p>
            <div class="form-grid">
              <label>订单日期<input v-model="transferForm.orderDate" type="date" /></label>
              <label>订单编号<input v-model.trim="transferForm.orderNumber" maxlength="3" inputmode="numeric" placeholder="例如 001" /></label>
              <label>实际到店人数<input v-model.number="transferForm.partySize" type="number" min="1" max="100" /></label>
              <label>选择更大空闲餐桌<select v-model="transferForm.targetTno"><option value="">-- 请选择 --</option><option v-for="t in transferTables" :key="t.tno" :value="t.tno">{{ t.tno }}（{{ t.seats }} 座 · 空闲）</option></select></label>
            </div>
            <div v-if="transferSourceOrder" class="transfer-route"><span>当前订单（{{ transferSourceOrder.paymentStatus === 'PAID' ? '已支付' : '待支付' }}）</span><strong>{{ transferSourceOrder.orderNumber }}：{{ transferSourceOrder.tno }} → {{ transferForm.targetTno || '请选择新桌' }}</strong></div>
            <div class="form-actions"><button class="btn-primary" :disabled="transferSaving" @click="transferOrder">{{ transferSaving ? '换桌中…' : '确认换桌并锁定新桌' }}</button><button class="btn-cancel" :disabled="transferSaving" @click="showTransferForm = false">取消</button></div>
          </div>
          <p v-if="transferMsg" :class="transferError ? 'error' : 'success'" role="status">{{ transferMsg }}</p>
          <div v-if="showOrderForm" class="form-card">
            <h3>新建订单</h3>
            <div class="form-grid">
              <div class="generated-order-number"><span>订单编号</span><strong>系统按当天顺序自动生成 001–999</strong></div>
              <label>顾客编号<select v-model="newOrder.cno" :disabled="newCustomer"><option value="">-- 请选择 --</option><option v-for="c in customers" :key="c.cno" :value="c.cno">{{ c.cno }} {{ c.cname }}</option></select></label>
              <label style="display:flex;align-items:center;gap:6px;font-size:12px"><input type="checkbox" v-model="newCustomer" style="width:auto" /> 新建顾客</label>
              <label>餐桌编号<select v-model="newOrder.tno"><option value="">-- 请选择 --</option><option v-for="t in orderTables" :key="t.tno" :value="t.tno">{{ t.tno }}（{{ t.seats }}座 · {{ t.tstatus }}）</option></select></label>
<label>合计（按菜品计算）<input :value="newOrderTotal.toFixed(2)" readonly /></label>
            </div>
            <div v-if="newCustomer" class="form-grid" style="margin-top:8px">
              <label>顾客编号<input v-model="newCustForm.cno" placeholder="C0005" /></label>
              <label>姓名<input v-model="newCustForm.cname" placeholder="姓名" /></label>
              <label>性别<select v-model="newCustForm.csex"><option>男</option><option>女</option></select></label>
              <label>电话<input v-model="newCustForm.cphone" placeholder="138..." /></label>
            </div>
            <div class="form-actions"><button class="btn-primary" :disabled="creating" @click="createOrder">{{ creating ? '创建中…' : '确认创建' }}</button><button class="btn-cancel" @click="showOrderForm = false">取消</button></div>
            <p v-if="formMsg" :class="formError ? 'error' : 'success'">{{ formMsg }}</p>
          </div>
          <div v-if="showOrderForm" class="form-card">
            <h3>添加菜品到订单 <button class="btn-sm" @click="addDetailRow">+ 添加菜品</button></h3>
            <div v-for="(d, idx) in newDetails" :key="idx" class="form-grid" style="margin-bottom:8px">
              <label>菜品<select v-model="d.mno"><option v-for="m in menu" :key="m.mno" :value="m.mno">{{ m.mname }} (¥{{ m.mprice }})</option></select></label>
              <label>数量<input v-model.number="d.dishCount" type="number" min="1" /></label>
              <label>备注<input v-model="d.remark" placeholder="备注" /></label>
              <button class="btn-sm danger" @click="newDetails.splice(idx, 1)" style="align-self:flex-end">删除</button>
            </div>
          </div>
          <div class="table-wrap"><table class="data-table">
            <thead><tr><th>订单编号</th><th>金额</th><th>时间</th><th>员工</th><th>顾客</th><th>餐桌</th><th>支付状态</th><th>订单状态</th><th>操作</th></tr></thead>
            <tbody>
              <tr v-for="o in ordersQuery.items" :key="o.ono">
                <td><strong>{{ o.orderNumber }}</strong><small class="table-date">{{ o.orderDate }}</small></td><td>¥{{ o.totalAmount }}</td><td>{{ o.orderTime }}</td><td>{{ o.eno }}</td><td>{{ o.cno }}</td><td>{{ o.tno }}</td><td><span :class="['status-tag', o.paymentStatus === 'PAID' ? 'payment-paid' : 'payment-unpaid']">{{ o.paymentStatus === 'PAID' ? '已支付（模拟）' : '待支付' }}</span></td><td><span :class="['status-tag', o.orderStatus === 'ENDED' ? 'order-ended' : 'order-active']">{{ o.orderStatus === 'ENDED' ? '已结束' : '进行中' }}</span><small v-if="o.endedAt" class="table-date">{{ o.endedAt.replace('T', ' ') }}</small></td>
                <td><button v-if="o.orderStatus !== 'ENDED'" class="btn-sm" @click="openTransfer(o)">换桌</button><button v-if="o.orderStatus !== 'ENDED' && o.paymentStatus !== 'PAID'" class="btn-sm" @click="paymentOrder = o">收款</button><button class="btn-sm" @click="viewOrderDetail(o.ono)">详情</button><button v-if="o.orderStatus !== 'ENDED'" class="btn-sm" @click="endOrder(o)">结束订单</button><button v-else class="btn-sm danger" @click="deleteOrder(o)">删除已结束订单</button></td>
              </tr>
            </tbody>
          </table></div>
          <div v-if="!ordersQuery.loading && !ordersQuery.items.length" class="empty-state"><p>所选日期没有订单记录</p></div>
          <QueryPagination :query="ordersQuery" />
          <div v-if="showDetail" class="modal-mask" @click.self="showDetail = false">
            <div class="modal">
              <h3>订单 {{ currentOrder?.orderNumber || currentOno }} · {{ currentOrder?.orderDate }} 详情</h3>
              <table class="data-table">
                <thead><tr><th>细则编号</th><th>菜品编号</th><th>数量</th><th>金额</th><th>备注</th></tr></thead>
                <tbody><tr v-for="d in currentDetails" :key="d.dno"><td>{{ d.dno }}</td><td>{{ menuNameMap[d.mno] || d.mno }}</td><td>{{ d.dishCount }}</td><td>¥{{ d.amount }}</td><td>{{ d.remark }}</td></tr></tbody>
              </table>
              <button class="btn-cancel" @click="showDetail = false">关闭</button>
            </div>
          </div>
        </section>
        <section v-if="activeTab === 'tables'" class="tab-page">
          <div class="page-header"><div><h2>🪑 餐桌管理</h2><p class="query-description">预约状态按今天计算，并显示今天每个已占用的预约时段</p></div></div>
          <div class="table-grid">
            <div v-for="t in tables" :key="t.tno" :class="['table-card', statusClass(tableTodayStatus(t))]">
              <h3>{{ t.tno }}</h3><p>{{ t.seats }} 座</p>
              <span class="table-today-status"><small>今日状态</small><strong>{{ tableTodayStatus(t) }}</strong></span>
              <div v-if="todayReservationSlots(t).length" class="table-reservation-times"><strong>今日已被预订</strong><span v-for="slot in todayReservationSlots(t)" :key="slot.reservedAt">{{ formatReservationTime(slot) }}</span></div>
              <select v-model="t.tstatus" @change="updateTable(t)"><option v-for="s in tableStatuses" :key="s" :value="s">{{ s }}</option><option value="已预订" disabled>已预订</option></select>
            </div>
          </div>
          <p v-if="tableMsg" :class="tableError ? 'error' : 'success'">{{ tableMsg }}</p>
        </section>
        <section v-if="activeTab === 'customers'" class="tab-page">
          <div class="page-header"><h2>👥 顾客查询</h2></div>
          <table class="data-table">
            <thead><tr><th>编号</th><th>姓名</th><th>性别</th><th>电话</th></tr></thead>
            <tbody><tr v-for="c in customers" :key="c.cno"><td>{{ c.cno }}</td><td>{{ c.cname }}</td><td>{{ c.csex }}</td><td>{{ c.cphone }}</td></tr></tbody>
          </table>
        </section>
        <section v-if="activeTab === 'menu'" class="tab-page">
          <div class="page-header"><h2>🍜 菜品总览</h2></div>
          <div class="menu-grid">
            <div v-for="item in menu" :key="item.mno" class="menu-card small">
              <DishImage :dish="item" />
              <div class="menu-card-header"><span class="menu-type-badge">{{ item.mtype }}</span><span class="menu-price">¥{{ item.mprice }}</span></div>
              <h3>{{ item.mname }}</h3><p class="menu-code">{{ item.mno }}</p><p class="menu-code">余量：{{ item.stock ?? '-' }}</p>
            </div>
          </div>
        </section>
        <section v-if="activeTab === 'profile'" class="tab-page">
          <div class="page-header"><h2>👨‍🍳 个人信息</h2><div class="form-actions"><button class="btn-cancel" @click="passwordOpen = true">修改密码</button><button class="btn-primary" @click="editProfile = !editProfile">{{ editProfile ? '取消' : '编辑' }}</button></div></div>
          <div v-if="myInfo" class="profile-card">
            <template v-if="!editProfile">
              <div class="profile-row"><span>工号：</span><strong>{{ myInfo.eno }}</strong></div>
              <div class="profile-row"><span>姓名：</span><strong>{{ myInfo.ename }}</strong></div>
              <div class="profile-row"><span>年龄：</span><strong>{{ myInfo.eage }}</strong></div>
              <div class="profile-row"><span>性别：</span><strong>{{ myInfo.esex }}</strong></div>
            </template>
            <template v-else>
              <div class="form-grid">
                <label>姓名<input v-model="editForm.ename" /></label>
                <label>年龄<input v-model="editForm.eage" type="number" /></label>
                <label>性别<select v-model="editForm.esex"><option>男</option><option>女</option></select></label>
              </div>
              <div class="form-actions">
                <button class="btn-primary" @click="saveProfile">保存</button>
              </div>
              <p v-if="profileMsg" :class="profileError ? 'error' : 'success'">{{ profileMsg }}</p>
            </template>
          </div>
        </section>
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
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
const orderMsg = ref('')
const orderError = ref(false)
const passwordOpen = ref(false)
const ordersQuery = useDateQuery('/orders')
async function onPaid() { await Promise.all([ordersQuery.load(), loadData()]) }
const router = useRouter()
const user = JSON.parse(sessionStorage.getItem('session') || '{}')

const activeTab = ref('orders')
const tables = ref([])
const customers = ref([])
const menu = ref([])
const currentDetails = ref([])
const myInfo = ref(null)
const reservations = ref([])
const todayAvailability = ref([])

const showOrderForm = ref(false)
const showDetail = ref(false)
const currentOno = ref('')
const formMsg = ref('')
const formError = ref(false)
const tableMsg = ref('')
const tableError = ref(false)
const editProfile = ref(false)
const editForm = ref({})
const profileMsg = ref('')
const profileError = ref(false)
const showReservationForm = ref(false)
const reservationSaving = ref(false)
const reservationMsg = ref('')
const reservationError = ref(false)
const showTransferForm = ref(false)
const transferSaving = ref(false)
const transferMsg = ref('')
const transferError = ref(false)

const newOrder = ref({ cno: '', tno: '', reservationNo: '' })
const newDetails = ref([])
const creating = ref(false)
const newOrderTotal = computed(() => newDetails.value.reduce((sum, d) => sum + Number(menu.value.find(m => m.mno === d.mno)?.mprice || 0) * (d.dishCount || 0), 0))
const newCustomer = ref(false)
const newCustForm = ref({ cno: '', cname: '', csex: '男', cphone: '', cpassword: '123456' })
const tableStatuses = ['空闲', '使用中', '待清理']
const reservationForm = ref(defaultReservationForm())
const transferForm = ref(defaultTransferForm())
const reservationStatusLabels = { RESERVED: '已预订', SEATED: '已入座', COMPLETED: '已结账', CANCELLED: '已取消', NO_SHOW: '订单已取消（押金不退）' }
const depositStatusLabels = { PAID: '押金已收', REFUNDED: '押金已退', FORFEITED: '押金已扣' }
const reservationSourceLabels = { ONLINE: '顾客线上预约', PHONE: '员工电话预约' }

const tabs = [
  { id: 'reservations', icon: '📅', label: '预约管理' },
  { id: 'orders', icon: '📦', label: '订单管理' },
  { id: 'tables', icon: '🪑', label: '餐桌管理' },
  { id: 'customers', icon: '👥', label: '顾客查询' },
  { id: 'menu', icon: '🍜', label: '菜品总览' },
  { id: 'profile', icon: '👨‍🍳', label: '个人信息' }
]

const orderTables = computed(() => tables.value.filter(t => t.tstatus === '空闲' || t.tstatus === '已预订'))
const reservationTables = computed(() => tables.value.filter(t => Number(t.seats || 0) >= Number(reservationForm.value.partySize || 1)))
const selectedReservationTable = computed(() => tables.value.find(t => t.tno === reservationForm.value.tno))
const reservationDeposit = computed(() => selectedReservationTable.value ? depositForSeats(selectedReservationTable.value.seats) : 0)
const transferSourceOrder = computed(() => ordersQuery.items.find(o => o.orderDate === transferForm.value.orderDate && o.orderNumber === transferForm.value.orderNumber))
const transferSourceTable = computed(() => tables.value.find(t => t.tno === transferSourceOrder.value?.tno))
const transferTables = computed(() => tables.value.filter(t => t.tstatus === '空闲' && Number(t.seats || 0) >= Number(transferForm.value.partySize || 1) && (!transferSourceTable.value || Number(t.seats || 0) > Number(transferSourceTable.value.seats || 0))))
const menuNameMap = computed(() => {
  const map = {}
  menu.value.forEach(m => { map[m.mno] = m.mname })
  return map
})
const currentOrder = computed(() => ordersQuery.items.find(o => o.ono === currentOno.value))

function statusClass(status) {
  if (status === '空闲') return 'free'
  if (status === '使用中') return 'busy'
  if (status === '已被预订' || status === '已预订') return 'reserved'
  if (status === '待清理') return 'cleaning'
  return 'other'
}

function todayReservationSlots(table) {
  const today = restaurantToday()
  return todayAvailability.value.filter(slot => slot.tno === table.tno && slot.reservedAt?.slice(0, 10) === today)
}
function tableTodayStatus(table) {
  if (table.tstatus === '使用中' || table.tstatus === '待清理') return table.tstatus
  return todayReservationSlots(table).length ? '已被预订' : '空闲'
}
function formatReservationTime(slot) { return `${slot.reservedAt?.slice(11, 16)}–${slot.reservedUntil?.slice(11, 16)}` }

async function viewOrderDetail(ono) {
  currentOno.value = ono; currentDetails.value = []; showDetail.value = true
  try { currentDetails.value = await api(`/order-details?ono=${encodeURIComponent(ono)}`) }
  catch (e) { pageError.value = e.message }
}

function defaultReservationTime() {
  const date = new Date(Date.now() + 60 * 60 * 1000)
  const pad = value => String(value).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`
}
function defaultReservationForm() { return { customerName: '', customerPhone: '', partySize: 2, reservedAt: defaultReservationTime(), tno: '', depositMethod: 'DEMO_WECHAT' } }
function defaultTransferForm() { return { orderDate: restaurantToday(), orderNumber: '', partySize: 3, targetTno: '' } }
function depositForSeats(seats) { return 20 + Math.max(0, Math.floor((Math.max(Number(seats) || 2, 2) - 1) / 2)) * 10 }

async function createReservation() {
  if (reservationSaving.value) return
  if (!reservationForm.value.customerName || !reservationForm.value.customerPhone || !reservationForm.value.tno || !reservationForm.value.reservedAt) { reservationMsg.value = '请完整填写预约信息并选择餐桌'; reservationError.value = true; return }
  reservationSaving.value = true; reservationMsg.value = ''
  try {
    const result = await api('/reservations', { method: 'POST', body: JSON.stringify(reservationForm.value) })
    reservationMsg.value = `预约成功，${result.tno} 已锁定，已收取模拟押金 ¥${Number(result.depositAmount).toFixed(2)}`
    reservationError.value = false; showReservationForm.value = false; reservationForm.value = defaultReservationForm()
    await loadData()
  } catch (e) { reservationMsg.value = e.message; reservationError.value = true }
  finally { reservationSaving.value = false }
}

async function cancelReservation(reservation) {
  if (!confirm(`确认取消 ${reservation.customerName} 的预约并退还押金 ¥${reservation.depositAmount}？`)) return
  try { await api(`/reservations/${reservation.reservationNo}/cancel`, { method: 'POST' }); reservationMsg.value = '预约已取消，押金已模拟退回，餐桌恢复空闲'; reservationError.value = false; await loadData() }
  catch (e) { reservationMsg.value = e.message; reservationError.value = true }
}

function canMarkNoShow(reservation) {
  return reservation.status === 'RESERVED' && reservation.reservedAt && new Date(reservation.reservedAt).getTime() <= Date.now()
}

async function markNoShow(reservation) {
  if (!confirm(`确认 ${reservation.customerName} 未到店？订单将取消，押金 ¥${reservation.depositAmount} 不予退回。`)) return
  try {
    await api(`/reservations/${reservation.reservationNo}/no-show`, { method: 'POST' })
    reservationMsg.value = '未到店订单已取消，押金不予退回，餐桌已恢复空闲'
    reservationError.value = false
    await loadData()
  } catch (e) { reservationMsg.value = e.message; reservationError.value = true }
}

function startReservedOrder(reservation) {
  const customer = customers.value.find(c => c.cno === reservation.cno || c.cphone === reservation.customerPhone)
  newOrder.value = { cno: customer?.cno || '', tno: reservation.tno, reservationNo: reservation.reservationNo }
  if (!customer) {
    newCustomer.value = true
    newCustForm.value = { cno: '', cname: reservation.customerName, csex: '男', cphone: reservation.customerPhone, cpassword: '123456' }
    formMsg.value = '该电话尚未关联顾客账号，请补充顾客编号后开单'
  } else { newCustomer.value = false; formMsg.value = `已载入 ${reservation.customerName} 的预约桌` }
  formError.value = false; showOrderForm.value = true; activeTab.value = 'orders'
}

function addDetailRow() { newDetails.value.push({ mno: '', dishCount: 1, remark: '' }) }

async function createOrder() {
  if (creating.value) return
  if (!newOrder.value.tno || (!newCustomer.value && !newOrder.value.cno) || !newDetails.value.length) { formMsg.value = '请选择顾客、餐桌并添加菜品'; formError.value = true; return }
  if (newCustomer.value && (!newCustForm.value.cno || !newCustForm.value.cname)) { formMsg.value = '请填写新顾客信息'; formError.value = true; return }
  creating.value = true
  try {
    if (newCustomer.value) {
      await api('/customers', { method: 'POST', body: JSON.stringify(newCustForm.value) })
      newOrder.value.cno = newCustForm.value.cno
      newCustomer.value = false
    }
    await api('/orders/checkout', { method: 'POST', body: JSON.stringify({ cno: newOrder.value.cno, tno: newOrder.value.tno, reservationNo: newOrder.value.reservationNo || null, items: newDetails.value }) })
    formMsg.value = '订单创建成功'; formError.value = false; showOrderForm.value = false
    newOrder.value = { cno: '', tno: '', reservationNo: '' }; newDetails.value = []
    await ordersQuery.today(); await loadData()
  } catch (e) { formMsg.value = e.message; formError.value = true }
  finally { creating.value = false }
}
async function endOrder(order) {
  if (!confirm(`确认结束订单 ${order.orderNumber}？结束后将不能换桌。`)) return
  try {
    await api(`/orders/${order.ono}/end`, { method: 'POST' })
    orderMsg.value = `订单 ${order.orderNumber} 已结束，不能再换桌`; orderError.value = false
    await Promise.all([ordersQuery.load(), loadData()])
  } catch (e) { orderMsg.value = e.message; orderError.value = true }
}
async function deleteOrder(order) {
  if (!confirm(`确认永久删除已结束订单 ${order.orderNumber}？`)) return
  try {
    await api(`/orders/${order.ono}`, { method: 'DELETE' })
    orderMsg.value = `已结束订单 ${order.orderNumber} 已删除`; orderError.value = false
    await Promise.all([ordersQuery.load(), loadData()])
  } catch (e) { orderMsg.value = e.message; orderError.value = true }
}

function openTransfer(order) {
  const source = tables.value.find(t => t.tno === order.tno)
  transferForm.value = { orderDate: order.orderDate, orderNumber: order.orderNumber, partySize: Number(source?.seats || 1) + 1, targetTno: '' }
  transferMsg.value = ''; transferError.value = false; showTransferForm.value = true
}

async function transferOrder() {
  if (transferSaving.value) return
  const form = transferForm.value
  if (!form.orderDate || !/^\d{3}$/.test(form.orderNumber) || !form.partySize || !form.targetTno) {
    transferMsg.value = '请填写订单日期、三位订单编号、实际人数并选择新餐桌'; transferError.value = true; return
  }
  transferSaving.value = true; transferMsg.value = ''
  try {
    const result = await api('/orders/transfer', { method: 'POST', body: JSON.stringify(form) })
    transferMsg.value = `订单 ${result.orderNumber} 已从 ${result.oldTno} 换至 ${result.newTno}，${result.targetSeats} 座新桌已锁定`
    transferError.value = false; showTransferForm.value = false
    await Promise.all([ordersQuery.load(), loadData()])
  } catch (e) { transferMsg.value = e.message; transferError.value = true }
  finally { transferSaving.value = false }
}

async function updateTable(table) {
  try {
    await api(`/tables/${table.tno}`, { method: 'PUT', body: JSON.stringify(table) })
    tableMsg.value = `餐桌 ${table.tno} 状态已更新`; tableError.value = false
  } catch (e) { tableMsg.value = e.message; tableError.value = true; await loadData() }
}

async function saveProfile() {
  try {
    const payload = { ...myInfo.value, ...editForm.value }
    await api(`/employees/${user.id}`, {
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
    const [t, c, m, r, empData, todaySlots] = await Promise.all([
      api('/tables'),
      api('/customers'),
      api('/menu'),
      api('/reservations'),
      api('/employees'),
      api(`/reservations/availability?date=${restaurantToday()}`)
    ])
    tables.value = t; customers.value = c; menu.value = m; reservations.value = r; todayAvailability.value = todaySlots
    myInfo.value = empData.find(e => e.eno === user.id) || null
    if (myInfo.value) editForm.value = { ename: myInfo.value.ename, eage: myInfo.value.eage, esex: myInfo.value.esex }
  } catch (e) { pageError.value = e.message }
}

async function logout() { await api('/auth/logout', { method: 'POST' }); sessionStorage.removeItem('session'); router.push('/') }

onMounted(loadData)
watch(activeTab, tab => { if (tab === 'orders') ordersQuery.ensure() }, { immediate: true })
</script>
