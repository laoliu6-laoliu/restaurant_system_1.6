<template>
  <div class="app-layout">
    <header class="top-bar">
      <div class="brand"><span class="brand-icon">🍽️</span><h1>臻味餐厅 · 管理后台</h1><span class="version-badge">v1.6</span></div>
      <div class="user-area">
        <span class="user-badge">👨‍💼 {{ user.name }}（管理员）</span>
        <button class="btn-logout" @click="passwordOpen = true">修改密码</button>
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
        <section v-if="activeTab === 'dashboard'" class="tab-page">
          <div class="page-header">
            <div><h2>📊 运营仪表盘</h2><p class="query-description">{{ selectedMonthLabel }}经营数据</p></div>
            <label class="month-picker">统计月份<input v-model="selectedMonth" type="month" :disabled="monthlyLoading" @change="loadMonthlyReports" /></label>
          </div>
          <p v-if="monthlyError" class="error" role="alert">{{ monthlyError }}</p>
          <div class="stats-grid">
            <div class="stat-card highlight"><h3>¥{{ revenue.totalRevenue }}</h3><p>💰 当月模拟已收款</p></div>
            <div class="stat-card"><h3>{{ revenue.orderCount }}</h3><p>📦 当月订单</p></div>
            <div class="stat-card"><h3>{{ stats.employees }}</h3><p>👨‍🍳 员工</p></div>
            <div class="stat-card"><h3>{{ stats.customers }}</h3><p>👥 顾客</p></div>
            <div class="stat-card"><h3>{{ stats.tables }}</h3><p>🪑 餐桌</p></div>
            <div class="stat-card"><h3>{{ stats.menuItems }}</h3><p>🍜 菜品</p></div>
          </div>
          <div class="stats-grid" style="margin-top:16px">
            <div class="stat-card free-card"><h3>{{ tableStats.free }}</h3><p>🟢 空闲餐桌</p></div>
            <div class="stat-card busy-card"><h3>{{ tableStats.busy }}</h3><p>🔴 使用中</p></div>
            <div class="stat-card"><h3>{{ tableStats.other }}</h3><p>🟡 待清理/已预订</p></div>
            <div class="stat-card"><h3>{{ topDish.mname || '-' }}</h3><p>🏆 当月销量冠军</p></div>
            <div class="stat-card"><h3>¥{{ topDish.totalAmount || 0 }}</h3><p>🏆 当月冠军销售额</p></div>
            <div class="stat-card"><h3>{{ lowStockDishes.length }}</h3><p>⚠️ 库存不足(≤10)</p></div>
          </div>
          <div v-if="lowStockDishes.length" class="panel-card" style="margin-top:16px">
            <header><div><h3>⚠️ 低库存预警</h3><p>以下菜品余量不足，请及时补货</p></div></header>
            <div class="table-wrap"><table class="data-table">
              <thead><tr><th>编号</th><th>菜名</th><th>类型</th><th>价格</th><th>当前余量</th></tr></thead>
              <tbody><tr v-for="d in lowStockDishes" :key="d.mno"><td>{{ d.mno }}</td><td>{{ d.mname }}</td><td>{{ d.mtype }}</td><td>¥{{ d.mprice }}</td><td style="color:#ef4444;font-weight:700">{{ d.stock }}</td></tr></tbody>
            </table></div>
          </div>
          <div class="panel-card" style="margin-top:16px">
            <header><div><h3>🏆 {{ selectedMonthLabel }}菜品销售排行 Top 5</h3><p>仅统计当月已支付订单，按销量降序</p></div></header>
            <div class="table-wrap"><table class="data-table">
              <thead><tr><th>排名</th><th>菜名</th><th>类型</th><th>销量</th><th>销售额</th><th>余量</th></tr></thead>
              <tbody><tr v-for="(item, idx) in salesRank.slice(0, 5)" :key="idx"><td>#{{ idx + 1 }}</td><td>{{ item.mname }}</td><td>{{ item.mtype }}</td><td>{{ item.totalCount }}</td><td>¥{{ item.totalAmount }}</td><td>{{ (menu.find(m => m.mno === item.mno) || {}).stock || '-' }}</td></tr></tbody>
            </table></div>
            <div v-if="!monthlyLoading && !salesRank.length" class="empty-state"><p>{{ selectedMonthLabel }}暂无已支付菜品销售数据</p></div>
          </div>
        </section>
        <section v-if="activeTab === 'employees'" class="tab-page">
          <div class="page-header"><h2>👨‍🍳 员工管理</h2><button class="btn-primary" @click="openForm('employee')">新增员工</button></div>
          <table class="data-table">
            <thead><tr><th>工号</th><th>姓名</th><th>年龄</th><th>性别</th><th>密码</th><th>操作</th></tr></thead>
            <tbody>
              <tr v-for="e in employees" :key="e.eno">
                <td>{{ e.eno }}</td><td>{{ e.ename }}</td><td>{{ e.eage }}</td><td>{{ e.esex }}</td><td>****</td>
                <td><button class="btn-sm" @click="editItem('employee', e)">编辑</button><button class="btn-sm danger" @click="deleteEmployee(e.eno)">删除</button></td>
              </tr>
            </tbody>
          </table>
        </section>
        <section v-if="activeTab === 'customers'" class="tab-page">
          <div class="page-header"><h2>👥 顾客管理</h2><button class="btn-primary" @click="openForm('customer')">新增顾客</button></div>
          <table class="data-table">
            <thead><tr><th>编号</th><th>姓名</th><th>性别</th><th>电话</th><th>密码</th><th>操作</th></tr></thead>
            <tbody>
              <tr v-for="c in customers" :key="c.cno">
                <td>{{ c.cno }}</td><td>{{ c.cname }}</td><td>{{ c.csex }}</td><td>{{ c.cphone }}</td><td>****</td>
                <td><button class="btn-sm" @click="editItem('customer', c)">编辑</button><button class="btn-sm danger" @click="deleteCustomer(c.cno)">删除</button></td>
              </tr>
            </tbody>
          </table>
        </section>
        <section v-if="activeTab === 'tables'" class="tab-page">
          <div class="page-header"><h2>🪑 餐桌管理</h2><button class="btn-primary" @click="openForm('table')">新增餐桌</button></div>
          <table class="data-table">
            <thead><tr><th>餐桌号</th><th>座位数</th><th>状态</th><th>操作</th></tr></thead>
            <tbody>
              <tr v-for="t in tables" :key="t.tno">
                <td>{{ t.tno }}</td><td>{{ t.seats }}</td><td>{{ t.tstatus }}</td>
                <td><button class="btn-sm" @click="editItem('table', t)">编辑</button><button class="btn-sm danger" @click="deleteTable(t.tno)">删除</button></td>
              </tr>
            </tbody>
          </table>
        </section>
        <section v-if="activeTab === 'menu'" class="tab-page">
          <div class="page-header"><h2>🍜 菜品管理</h2><button class="btn-primary" @click="openForm('menuItem')">新增菜品</button></div>
          <table class="data-table">
            <thead><tr><th>图片</th><th>编号</th><th>菜名</th><th>类型</th><th>价格</th><th>余量</th><th>操作</th></tr></thead>
            <tbody>
              <tr v-for="m in menu" :key="m.mno">
                <td><DishImage :dish="m" small /></td><td>{{ m.mno }}</td><td>{{ m.mname }}</td><td>{{ m.mtype }}</td><td>¥{{ m.mprice }}</td><td>{{ m.stock }}</td>
                <td><button class="btn-sm" @click="editItem('menuItem', m)">编辑</button><button class="btn-sm danger" @click="deleteMenu(m.mno)">删除</button></td>
              </tr>
            </tbody>
          </table>
        </section>
        <section v-if="activeTab === 'allOrders'" class="tab-page">
          <div class="page-header"><div><h2>📦 订单总览</h2><p class="query-description">可结束进行中的订单，并删除已结束订单</p></div></div>
          <DateQueryBar :query="ordersQuery" />
          <p v-if="orderMsg" :class="orderError ? 'error' : 'success'" role="status">{{ orderMsg }}</p>
          <div class="table-wrap"><table class="data-table">
            <thead><tr><th>编号</th><th>金额</th><th>时间</th><th>员工</th><th>顾客</th><th>餐桌</th><th>支付状态</th><th>订单状态</th><th>操作</th></tr></thead>
            <tbody>
              <tr v-for="o in ordersQuery.items" :key="o.ono">
                <td><strong>{{ o.orderNumber }}</strong><small class="table-date">{{ o.orderDate }}</small></td><td>¥{{ o.totalAmount }}</td><td>{{ o.orderTime }}</td><td>{{ o.eno }}</td><td>{{ o.cno }}</td><td>{{ o.tno }}</td><td><span :class="['status-tag', o.paymentStatus === 'PAID' ? 'payment-paid' : 'payment-unpaid']">{{ o.paymentStatus === 'PAID' ? '已支付（模拟）' : '待支付' }}</span></td><td><span :class="['status-tag', o.orderStatus === 'ENDED' ? 'order-ended' : 'order-active']">{{ o.orderStatus === 'ENDED' ? '已结束' : '进行中' }}</span><small v-if="o.endedAt" class="table-date">{{ o.endedAt.replace('T', ' ') }}</small></td>
                <td><button v-if="o.orderStatus !== 'ENDED' && o.paymentStatus !== 'PAID'" class="btn-sm" @click="paymentOrder = o">收款</button><button class="btn-sm" @click="viewOrderDetail(o.ono)">详情</button><button v-if="o.orderStatus !== 'ENDED'" class="btn-sm" @click="endOrder(o)">结束订单</button><button v-else class="btn-sm danger" @click="deleteOrder(o)">删除已结束订单</button></td>
              </tr>
            </tbody>
          </table></div>
          <div v-if="!ordersQuery.loading && !ordersQuery.items.length" class="empty-state"><p>所选日期没有订单记录</p></div>
          <QueryPagination :query="ordersQuery" />
        </section>
        <section v-if="activeTab === 'orderDetailView'" class="tab-page">
          <div class="page-header"><div><h2>📋 订单明细查询</h2><p class="query-description">每个订单只显示一行，点击订单详情查看该订单所点菜品</p></div></div>
          <DateQueryBar :query="detailQuery" />
          <div class="table-wrap"><table class="data-table">
            <thead><tr><th>订单编号</th><th>下单时间</th><th>操作</th></tr></thead>
            <tbody>
              <tr v-for="order in detailQuery.items" :key="order.ono">
                <td><strong>{{ order.orderNumber }}</strong><small class="table-date">{{ order.orderDate }}</small></td>
                <td>{{ order.orderTime }}</td>
                <td><button class="btn-sm" @click="viewOrderDetail(order.ono)">订单详情</button></td>
              </tr>
            </tbody>
          </table></div>
          <div v-if="!detailQuery.loading && !detailQuery.items.length" class="empty-state"><p>所选日期没有订单记录</p></div>
          <QueryPagination :query="detailQuery" />
        </section>

        <section v-if="activeTab === 'salesRank'" class="tab-page">
          <div class="page-header"><div><h2>🏆 菜品销售排行</h2><p class="query-description">{{ selectedMonthLabel }} · 仅统计已支付订单</p></div><label class="month-picker">统计月份<input v-model="selectedMonth" type="month" :disabled="monthlyLoading" @change="loadMonthlyReports" /></label></div>
          <p v-if="monthlyError" class="error" role="alert">{{ monthlyError }}</p>
          <table class="data-table">
            <thead><tr><th>排名</th><th>编号</th><th>菜名</th><th>类型</th><th>月销量</th><th>月销售额</th></tr></thead>
            <tbody>
              <tr v-for="(item, idx) in salesRank" :key="idx">
                <td>#{{ idx + 1 }}</td><td>{{ item.mno }}</td><td>{{ item.mname }}</td><td>{{ item.mtype }}</td><td>{{ item.totalCount }}</td><td>¥{{ item.totalAmount }}</td>
              </tr>
            </tbody>
          </table>
          <div v-if="!monthlyLoading && !salesRank.length" class="empty-state"><p>{{ selectedMonthLabel }}暂无已支付菜品销售数据</p></div>
        </section>
        <div v-if="showDetail" class="modal-mask" @click.self="showDetail = false">
          <div class="modal">
            <h3>订单 {{ currentOrder?.orderNumber || currentOno }} · {{ currentOrder?.orderDate }} 详情</h3>
            <p class="hint" v-if="currentOrder">顾客：{{ currentOrder.cno }} · 员工：{{ currentOrder.eno }} · 餐桌：{{ currentOrder.tno }} · 合计：¥{{ currentOrder.totalAmount }}</p>
            <div class="table-wrap"><table class="data-table">
              <thead><tr><th>菜品</th><th>数量</th><th>金额</th><th>备注</th></tr></thead>
              <tbody><tr v-for="d in currentDetails" :key="d.dno"><td>{{ dishNameMap[d.mno] || d.mno }}</td><td>{{ d.dishCount }}</td><td>¥{{ d.amount }}</td><td>{{ d.remark || '-' }}</td></tr></tbody>
            </table></div>
            <div v-if="detailLoading" class="empty-state"><p>正在加载订单详情…</p></div>
            <div v-else-if="!currentDetails.length" class="empty-state"><p>该订单暂无菜品明细</p></div>
            <button class="btn-cancel" @click="showDetail = false">关闭</button>
          </div>
        </div>
        <div v-if="showForm" class="modal-mask" @click.self="showForm = false">
          <div class="modal">
            <h3>{{ formMode === 'edit' ? '编辑' : '新增' }} {{ formTitle }}</h3>
            <div class="form-grid">
              <label v-for="(label, key) in formFields" :key="key">{{ label }} <input v-model="formData[key]" /></label>
            </div>
            <div class="form-actions"><button class="btn-primary" @click="submitForm">{{ formMode === 'edit' ? '保存修改' : '确认新增' }}</button><button class="btn-cancel" @click="showForm = false">取消</button></div>
            <p v-if="formMsg" :class="formError ? 'error' : 'success'">{{ formMsg }}</p>
          </div>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import DishImage from '../../components/DishImage.vue'
import PaymentDialog from '../../components/PaymentDialog.vue'
import PasswordChangeDialog from '../../components/PasswordChangeDialog.vue'
import DateQueryBar from '../../components/DateQueryBar.vue'
import QueryPagination from '../../components/QueryPagination.vue'
import { restaurantToday, useDateQuery } from '../../composables/useDateQuery'
import { api } from '../../api'

const API = '/api'
const paymentOrder = ref(null)
const pageError = ref('')
const orderMsg = ref('')
const orderError = ref(false)
const passwordOpen = ref(false)
const ordersQuery = useDateQuery('/orders')
const detailQuery = useDateQuery('/orders')
async function onPaid() { await Promise.all([ordersQuery.load(), loadMonthlyReports()]) }
const router = useRouter()
const user = JSON.parse(sessionStorage.getItem('session') || '{}')

const activeTab = ref('dashboard')
const employees = ref([])
const customers = ref([])
const tables = ref([])
const menu = ref([])
const currentDetails = ref([])
const detailLoading = ref(false)
const salesRank = ref([])
const revenue = reactive({ totalRevenue: 0, orderCount: 0 })
const stats = reactive({ employees: 0, customers: 0, tables: 0, menuItems: 0, orders: 0 })
const selectedMonth = ref(restaurantToday().slice(0, 7))
const monthlyLoading = ref(false)
const monthlyError = ref('')

const showDetail = ref(false)
const currentOno = ref('')
const showForm = ref(false)
const formMode = ref('create')
const formType = ref('')
const formData = ref({})
const formMsg = ref('')
const formError = ref(false)

const tableStats = computed(() => ({
  free: tables.value.filter(t => t.tstatus === '空闲').length,
  busy: tables.value.filter(t => t.tstatus === '使用中').length,
  other: tables.value.filter(t => t.tstatus !== '空闲' && t.tstatus !== '使用中').length
}))
const lowStockDishes = computed(() => menu.value.filter(m => (m.stock || 0) <= 10))
const topDish = computed(() => salesRank.value.length > 0 ? salesRank.value[0] : {})
const selectedMonthLabel = computed(() => {
  const [year, month] = selectedMonth.value.split('-')
  return year && month ? `${year}年${Number(month)}月` : '所选月份'
})
const dishNameMap = computed(() => {
  const map = {}
  menu.value.forEach(item => { map[item.mno] = item.mname })
  return map
})
const currentOrder = computed(() => ordersQuery.items.find(item => item.ono === currentOno.value) || detailQuery.items.find(item => item.ono === currentOno.value) || null)

const tabs = [
  { id: 'dashboard', icon: '📊', label: '运营仪表盘' },
  { id: 'employees', icon: '👨‍🍳', label: '员工管理' },
  { id: 'customers', icon: '👥', label: '顾客管理' },
  { id: 'tables', icon: '🪑', label: '餐桌管理' },
  { id: 'menu', icon: '🍜', label: '菜品管理' },
  { id: 'allOrders', icon: '📦', label: '订单总览' },
  { id: 'orderDetailView', icon: '📋', label: '订单明细' },
  { id: 'salesRank', icon: '🏆', label: '销售排行' }
]

const formTitle = computed(() => {
  const titles = { employee: '员工', customer: '顾客', table: '餐桌', menuItem: '菜品' }
  return titles[formType.value] || ''
})

const formFields = computed(() => {
  const fields = {
    employee: { eno: '工号', ename: '姓名', eage: '年龄', esex: '性别', ...(formMode.value === 'create' ? { epassword: '初始密码' } : {}) },
    customer: { cno: '编号', cname: '姓名', csex: '性别', cphone: '电话', ...(formMode.value === 'create' ? { cpassword: '初始密码' } : {}) },
    table: { tno: '餐桌号', seats: '座位数', tstatus: '状态' },
    menuItem: { mno: '编号', mname: '菜名', mtype: '类型', mprice: '价格', stock: '余量', imageUrl: '图片地址（/images/ 路径或 https:// 链接）' }
  }
  return fields[formType.value] || {}
})

async function viewOrderDetail(ono) {
  currentOno.value = ono; currentDetails.value = []; detailLoading.value = true; showDetail.value = true
  try { currentDetails.value = await api(`/order-details?ono=${encodeURIComponent(ono)}`) }
  catch (e) { pageError.value = e.message }
  finally { detailLoading.value = false }
}

function openForm(type, item = null) {
  formType.value = type
  formMode.value = item ? 'edit' : 'create'
  const defaults = {
    employee: { eno: '', ename: '', eage: '', esex: '男', epassword: '123456' },
    customer: { cno: '', cname: '', csex: '男', cphone: '', cpassword: '123456' },
    table: { tno: '', seats: '4', tstatus: '空闲' },
    menuItem: { mno: '', mname: '', mtype: '主菜', mprice: '', stock: '50', imageUrl: '' }
  }
  formData.value = item ? { ...item } : { ...defaults[type] }
  formMsg.value = ''
  showForm.value = true
}

function editItem(type, item) { openForm(type, item) }

async function submitForm() {
  const endpoints = {
    employee: { path: '/employees', idKey: 'eno' },
    customer: { path: '/customers', idKey: 'cno' },
    table: { path: '/tables', idKey: 'tno' },
    menuItem: { path: '/menu', idKey: 'mno' }
  }
  const ep = endpoints[formType.value]
  if (!ep) return
  try {
    const url = formMode.value === 'edit' ? ep.path + '/' + formData.value[ep.idKey] : ep.path
    const method = formMode.value === 'edit' ? 'PUT' : 'POST'
    await api(url, { method, body: JSON.stringify(formData.value) })
    formMsg.value = formMode.value === 'edit' ? '修改成功' : '新增成功'
    formError.value = false; showForm.value = false
    await loadData()
  } catch (e) { formMsg.value = e.message; formError.value = true }
}

async function deleteEmployee(id) { if (confirm('确认删除？')) { await fetch(`${API}/employees/${id}`, { method: 'DELETE' }); await loadData() } }
async function deleteCustomer(id) { if (confirm('确认删除？')) { await fetch(`${API}/customers/${id}`, { method: 'DELETE' }); await loadData() } }
async function deleteTable(id) { if (confirm('确认删除？')) { await fetch(`${API}/tables/${id}`, { method: 'DELETE' }); await loadData() } }
async function deleteMenu(id) { if (confirm('确认删除？')) { await fetch(`${API}/menu/${id}`, { method: 'DELETE' }); await loadData() } }
async function endOrder(order) {
  if (!confirm(`确认结束订单 ${order.orderNumber}？结束后将不能换桌。`)) return
  try { await api(`/orders/${order.ono}/end`, { method: 'POST' }); orderMsg.value = `订单 ${order.orderNumber} 已结束`; orderError.value = false; await Promise.all([ordersQuery.load(), loadData()]) }
  catch (e) { orderMsg.value = e.message; orderError.value = true }
}
async function deleteOrder(order) {
  if (!confirm(`确认永久删除已结束订单 ${order.orderNumber}？`)) return
  try { await api(`/orders/${order.ono}`, { method: 'DELETE' }); orderMsg.value = `已结束订单 ${order.orderNumber} 已删除`; orderError.value = false; await Promise.all([ordersQuery.load(), loadData()]) }
  catch (e) { orderMsg.value = e.message; orderError.value = true }
}

async function loadData() {
  pageError.value = ''
  try {
    const [emp, cust, tab, men] = await Promise.all([
      api('/employees'),
      api('/customers'),
      api('/tables'),
      api('/menu')
    ])
    employees.value = emp; customers.value = cust; tables.value = tab; menu.value = men
    stats.employees = emp.length; stats.customers = cust.length; stats.tables = tab.length
    stats.menuItems = men.length
    await loadMonthlyReports()
  } catch (e) { pageError.value = e.message }
}

async function loadMonthlyReports() {
  if (!/^\d{4}-\d{2}$/.test(selectedMonth.value)) { monthlyError.value = '请选择有效月份'; return }
  monthlyLoading.value = true; monthlyError.value = ''
  try {
    const month = encodeURIComponent(selectedMonth.value)
    const [rank, summary] = await Promise.all([
      api(`/reports/dish-sales-rank?month=${month}`),
      api(`/reports/order-summary?month=${month}`)
    ])
    salesRank.value = rank || []
    revenue.totalRevenue = summary.totalRevenue
    revenue.orderCount = summary.orderCount
    stats.orders = summary.orderCount
  } catch (e) { monthlyError.value = e.message }
  finally { monthlyLoading.value = false }
}

async function logout() { await api('/auth/logout', { method: 'POST' }); sessionStorage.removeItem('session'); router.push('/') }

onMounted(loadData)
watch(activeTab, tab => {
  if (tab === 'allOrders') ordersQuery.ensure()
  if (tab === 'orderDetailView') detailQuery.ensure()
})
</script>
