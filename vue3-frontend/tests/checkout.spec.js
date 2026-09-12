import { test, expect } from '@playwright/test'

const names = ['招牌牛排','蒜香虾仁意面','香煎三文鱼','法式蘑菇汤','凯撒沙拉','柠檬红茶','黑椒烤鸡','玛格丽特披萨','番茄海鲜烩饭','南瓜浓汤','草莓芝士蛋糕','芒果酸奶','蜜汁烤鸭胸','香草羊排','松露奶油宽面','日式照烧鳗鱼饭','泰式冬阴功汤','玉米奶油浓汤','牛油果鲜虾沙拉','脆皮春卷','提拉米苏','熔岩巧克力蛋糕','百香果气泡水','抹茶拿铁']
const types = ['主菜','主食','主菜','汤品','凉菜','饮品','主菜','主食','主食','汤品','甜品','饮品','主菜','主菜','主食','主食','汤品','汤品','凉菜','小吃','甜品','甜品','饮品','饮品']
const prices = [88,68,92,38,32,18,58,56,72,28,36,24,78,98,76,82,42,30,46,32,38,42,22,26]
const today = new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Shanghai' }).format(new Date())
const currentMonth = today.slice(0, 7)
const tomorrow = new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Shanghai' }).format(new Date(Date.now() + 86400000))

async function setup(page, { role = 'customer', failFirstPayment = false } = {}) {
  const menu = names.map((mname, i) => ({ mno: `M${String(i + 1).padStart(4, '0')}`, mname, mtype: types[i], mprice: prices[i], stock: 2, imageUrl: `/images/menu/M${String(i + 1).padStart(4, '0')}.png` }))
  const state = { orders: [], details: [], payments: [], reservations: [], availabilityBlocks: [], tableStatus: '空闲', largeTableStatus: '空闲', largestTableStatus: '空闲', paymentCalls: 0, checkoutPayload: null, passwordCalls: 0, orderQueries: [], reportQueries: [] }
  await page.route('**/api/**', async route => {
    const path = new URL(route.request().url()).pathname.replace('/api', '')
    let data = []
    if (path === '/auth/login') data = { role, id: role === 'customer' ? 'C0001' : role === 'admin' ? 'A0001' : 'E0001', name: '测试用户' }
    else if (path === '/auth/password') {
      state.passwordCalls++
      const body = route.request().postDataJSON()
      if (body.oldPassword !== '123456') return route.fulfill({ status: 400, json: { message: '旧密码不正确' } })
      data = { message: '密码修改成功，下次登录请使用新密码' }
    }
    else if (path === '/menu') data = menu
    else if (path === '/tables') data = [
      { tno: 'T0001', seats: 2, tstatus: state.tableStatus },
      { tno: 'T0002', seats: 2, tstatus: '使用中' },
      { tno: 'T0009', seats: 2, tstatus: '空闲' },
      { tno: 'T0003', seats: 4, tstatus: '待清理' },
      { tno: 'T0004', seats: 4, tstatus: '已预订' },
      { tno: 'T0010', seats: 4, tstatus: '空闲' },
      { tno: 'T0005', seats: 6, tstatus: '空闲' },
      { tno: 'T0006', seats: 6, tstatus: '空闲' },
      { tno: 'T0011', seats: 6, tstatus: '空闲' },
      { tno: 'T0007', seats: 8, tstatus: '空闲' },
      { tno: 'T0008', seats: 8, tstatus: state.largeTableStatus },
      { tno: 'T0012', seats: 8, tstatus: '空闲' },
      { tno: 'T0013', seats: 12, tstatus: state.largestTableStatus },
      { tno: 'T0014', seats: 12, tstatus: '空闲' },
      { tno: 'T0015', seats: 12, tstatus: '空闲' }
    ]
    else if (path === '/customers') data = [{ cno: 'C0001', cname: '测试用户', cphone: '13800000001' }]
    else if (path === '/employees') data = [{ eno: 'E0001', ename: '测试员工' }]
    else if (path === '/reservations/availability') data = state.availabilityBlocks
    else if (path === '/reservations' && route.request().method() === 'GET') data = state.reservations
    else if (path === '/reservations' && route.request().method() === 'POST') {
      const body = route.request().postDataJSON()
      const depositAmount = body.tno === 'T0013' ? 70 : body.tno === 'T0008' ? 50 : 20
      const reservation = { reservationNo: 'Rtest0001', ...body, customerName: body.customerName || '测试用户', customerPhone: body.customerPhone || '13800000001', cno: 'C0001', eno: 'E0001', reservationSource: role === 'customer' ? 'ONLINE' : 'PHONE', reservedUntil: `${body.reservedAt.slice(0, 10)}T13:00:00`, arrivalDeadline: `${body.reservedAt.slice(0, 10)}T11:30:00`, depositAmount, depositStatus: 'PAID', depositPaidAt: `${today}T12:00:00`, status: 'RESERVED', createdAt: `${today}T12:00:00` }
      state.reservations = [reservation]
      state.availabilityBlocks.push({ tno: body.tno, reservedAt: body.reservedAt, reservedUntil: reservation.reservedUntil })
      if (body.tno === 'T0008') state.largeTableStatus = '已预订'; else state.tableStatus = '已预订'
      data = reservation
    }
    else if (path.endsWith('/no-show')) {
      state.reservations[0].status = 'NO_SHOW'; state.reservations[0].depositStatus = 'FORFEITED'; state.reservations[0].depositForfeitedAt = `${today}T12:00:00`; state.tableStatus = '空闲'; data = state.reservations[0]
    }
    else if (path.endsWith('/cancel')) {
      state.reservations[0].status = 'CANCELLED'; state.reservations[0].depositStatus = 'REFUNDED'; state.tableStatus = '空闲'; data = state.reservations[0]
    }
    else if (path === '/orders') {
      state.orderQueries.push(new URL(route.request().url()).searchParams.toString())
      data = { content: state.orders, totalElements: state.orders.length, totalPages: state.orders.length ? 1 : 0, page: 0, size: 10, startDate: today, endDate: today }
    }
    else if (path === '/order-details') data = state.details
    else if (path === '/orders/checkout') {
      const body = route.request().postDataJSON(); state.checkoutPayload = body
      const order = { ono: 'Otest0001', orderNumber: '001', orderDate: today, cno: 'C0001', eno: 'E0001', tno: body.tno, totalAmount: body.items.reduce((sum, d) => sum + menu.find(m => m.mno === d.mno).mprice * d.dishCount, 0), paymentStatus: 'UNPAID', orderStatus: 'ACTIVE', endedAt: null, orderTime: `${today}T12:00:00`, payment: null }
      state.orders.push(order)
      state.tableStatus = '使用中'
      if (state.reservations[0]) { state.reservations[0].status = 'SEATED'; state.reservations[0].ono = order.ono }
      state.details = body.items.map((d, i) => ({ ...d, dno: `D${i}`, ono: order.ono, amount: menu.find(m => m.mno === d.mno).mprice * d.dishCount }))
      data = order
    } else if (path === '/orders/transfer') {
      const body = route.request().postDataJSON()
      const order = state.orders.find(o => o.orderDate === body.orderDate && o.orderNumber === body.orderNumber)
      const oldTno = order.tno
      order.tno = body.targetTno
      if (oldTno === 'T0001') state.tableStatus = '待清理'
      if (oldTno === 'T0008') state.largeTableStatus = '待清理'
      if (body.targetTno === 'T0008') state.largeTableStatus = '使用中'
      if (body.targetTno === 'T0013') state.largestTableStatus = '使用中'
      if (state.reservations[0]?.ono === order.ono) state.reservations[0].tno = body.targetTno
      data = { ono: order.ono, orderDate: order.orderDate, orderNumber: order.orderNumber, oldTno, newTno: body.targetTno, targetSeats: body.targetTno === 'T0013' ? 12 : 8, partySize: body.partySize, transferredAt: `${today}T12:05:00` }
    } else if (/^\/orders\/[^/]+\/end$/.test(path)) {
      const ono = path.split('/')[2]
      const order = state.orders.find(o => o.ono === ono)
      order.orderStatus = 'ENDED'; order.endedAt = `${today}T12:10:00`
      if (order.tno === 'T0001') state.tableStatus = '待清理'
      if (order.tno === 'T0008') state.largeTableStatus = '待清理'
      if (order.tno === 'T0013') state.largestTableStatus = '待清理'
      data = order
    } else if (/^\/orders\/[^/]+$/.test(path) && route.request().method() === 'DELETE') {
      const ono = path.split('/')[2]
      state.orders = state.orders.filter(o => o.ono !== ono)
      state.details = state.details.filter(d => d.ono !== ono)
      state.payments = state.payments.filter(p => p.ono !== ono)
      data = {}
    } else if (path === '/payments/config') data = { mode: 'DEMO', enabled: true }
    else if (path === '/payments') data = { content: state.payments.map(p => ({ ...p, orderNumber: '001', orderDate: today })), totalElements: state.payments.length, totalPages: state.payments.length ? 1 : 0, page: 0, size: 10, startDate: today, endDate: today }
    else if (path.startsWith('/payments/')) {
      state.paymentCalls++
      if (failFirstPayment && state.paymentCalls === 1) return route.fulfill({ status: 503, json: { message: '支付服务暂时不可用，请重试' } })
      await new Promise(resolve => setTimeout(resolve, 250))
      state.orders[0].paymentStatus = 'PAID'
      data = { paymentNo: 'Ptestreceipt', ono: state.orders[0].ono, amount: state.orders[0].totalAmount, method: route.request().postDataJSON().method, status: 'SIMULATED_SUCCESS', paidAt: `${today}T12:01:00` }
      if (state.reservations[0]) {
        state.reservations[0].status = 'COMPLETED'; state.reservations[0].depositStatus = 'REFUNDED'; state.reservations[0].depositRefundedAt = `${today}T12:01:00`
        data.depositRefund = { reservationNo: state.reservations[0].reservationNo, amount: 20, status: 'REFUNDED', refundedAt: `${today}T12:01:00` }
      }
      state.orders[0].payment = data
      state.payments = [data]
    } else if (path === '/reports/dish-sales-rank') {
      state.reportQueries.push(new URL(route.request().url()).searchParams.toString())
      data = [{ mno: 'M0001', mname: '招牌牛排', mtype: '主菜', totalCount: 12, totalAmount: 1056 }]
    } else if (path === '/reports/order-summary') {
      state.reportQueries.push(new URL(route.request().url()).searchParams.toString())
      data = { month: new URL(route.request().url()).searchParams.get('month'), totalRevenue: 1056, orderCount: 3 }
    }
    return route.fulfill({ json: data })
  })
  await page.goto('/')
  await page.getByLabel('登录身份').selectOption(role)
  await page.getByPlaceholder('请输入编号').fill(role === 'customer' ? 'C0001' : role === 'admin' ? 'A0001' : 'E0001')
  await page.getByPlaceholder('请输入密码').fill('123456')
  await page.getByRole('button', { name: '登录', exact: true }).click()
  const routeName = role === 'customer' ? 'customer' : role === 'admin' ? 'admin' : 'employee'
  await page.waitForURL(`**/#/${routeName}`)
  return state
}

async function checkout(page) {
  await page.locator('.menu-card').first().getByRole('button').click()
  await page.locator('nav a').filter({ hasText: '购物车' }).click()
  await page.locator('.cart-submit select').selectOption('T0001')
  await page.getByRole('button', { name: '提交订单并支付' }).click()
  await expect(page.getByRole('dialog')).toBeVisible()
}

test('all twenty-four photos load, categories filter, and stock limits quantities', async ({ page }) => {
  await setup(page)
  await expect(page.locator('.menu-card')).toHaveCount(24)
  for (const photo of await page.locator('.menu-card img').all()) {
    await photo.scrollIntoViewIfNeeded()
    await expect.poll(() => photo.evaluate(img => img.complete && img.naturalWidth > 0)).toBeTruthy()
  }
  await page.getByRole('button', { name: '甜品', exact: true }).click()
  await expect(page.locator('.menu-card')).toHaveCount(3)
  await expect(page.locator('.menu-card')).toContainText(['草莓芝士蛋糕', '提拉米苏', '熔岩巧克力蛋糕'])
  await page.getByRole('button', { name: '全部', exact: true }).click()
  const add = page.locator('.menu-card').first().getByRole('button')
  await add.click(); await add.click(); await add.click()
  await expect(page.getByRole('status')).toContainText('库存上限')
  await page.locator('nav a').filter({ hasText: '购物车' }).click()
  await expect(page.locator('.qty-control span')).toHaveText('2')
  await expect(page.getByRole('button', { name: '+', exact: true })).toBeDisabled()
})

test('v0.8 provides three tables for every capacity including twelve seats', async ({ page }) => {
  await setup(page)
  await page.locator('nav a').filter({ hasText: '餐桌状态' }).click()
  await expect(page.locator('.table-card')).toHaveCount(15)
  for (const seats of [2, 4, 6, 8, 12]) {
    await expect(page.locator('.table-card').getByText(`${seats} 座`, { exact: true })).toHaveCount(3)
  }
  await expect(page.getByText('12 人桌 ¥70', { exact: true })).toBeVisible()
})

test('employee table management shows only today reservations and their times', async ({ page }) => {
  const state = await setup(page, { role: 'staff' })
  state.availabilityBlocks = [
    { tno: 'T0004', reservedAt: `${today}T17:00:00`, reservedUntil: `${today}T19:00:00` },
    { tno: 'T0001', reservedAt: `${tomorrow}T11:00:00`, reservedUntil: `${tomorrow}T13:00:00` }
  ]
  await page.reload()
  await page.locator('nav a').filter({ hasText: '餐桌管理' }).click()
  const bookedToday = page.locator('.table-card').filter({ hasText: 'T0004' })
  await expect(bookedToday.getByText('已被预订', { exact: true })).toBeVisible()
  await expect(bookedToday).toContainText('今日已被预订')
  await expect(bookedToday).toContainText('17:00–19:00')
  const futureOnly = page.locator('.table-card').filter({ hasText: 'T0001' })
  await expect(futureOnly.getByText('空闲', { exact: true }).first()).toBeVisible()
  await expect(futureOnly).not.toContainText('11:00–13:00')
  await page.screenshot({ path: '../artifacts/employee-table-today-v1.6.png', fullPage: true, animations: 'disabled' })
})

test('postpone, reload, then pay once and retain receipt', async ({ page }) => {
  const state = await setup(page)
  await checkout(page)
  expect(state.checkoutPayload).not.toHaveProperty('totalAmount')
  await page.getByRole('button', { name: '稍后支付' }).click()
  expect(state.paymentCalls).toBe(0)
  await expect(page.locator('.order-card')).toContainText('待支付')
  await page.reload()
  await page.locator('nav a').filter({ hasText: '我的订单' }).click()
  await page.getByRole('button', { name: '去支付', exact: true }).click()
  await page.getByLabel('支付宝（模拟）').check()
  await page.getByRole('button', { name: '确认模拟支付' }).click()
  await expect(page.getByRole('heading', { name: '模拟支付成功' })).toBeVisible()
  expect(state.paymentCalls).toBe(1)
  await expect(page.getByRole('dialog')).toContainText('Ptestreceipt')
  await page.screenshot({ path: '../artifacts/payment-v1.6.png' })
  await page.getByRole('button', { name: '完成', exact: true }).click()
  await expect(page.locator('.order-card')).toContainText('已支付（模拟）')
  await expect(page.getByRole('button', { name: '去支付', exact: true })).toHaveCount(0)
})

test('failed payment shows error and allows a retry', async ({ page }) => {
  const state = await setup(page, { failFirstPayment: true })
  await checkout(page)
  await page.getByRole('button', { name: '确认模拟支付' }).click()
  await expect(page.getByRole('alert')).toContainText('支付服务暂时不可用')
  expect(state.orders[0].paymentStatus).toBe('UNPAID')
  await page.getByRole('button', { name: '确认模拟支付' }).click()
  await expect(page.getByRole('heading', { name: '模拟支付成功' })).toBeVisible()
})

test('mobile menu fits while staff and admin omit payment records', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 })
  await setup(page)
  await expect(page.locator('.menu-card')).toHaveCount(24)
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBeTruthy()
  await page.locator('.menu-card img').first().evaluate(img => img.decode())
  await page.screenshot({ path: '../artifacts/menu-mobile-v1.6.png', animations: 'disabled' })
  await page.unroute('**/api/**')
  await page.setViewportSize({ width: 1440, height: 960 })
  const adminState = await setup(page, { role: 'admin' })
  await expect(page.locator('nav a').filter({ hasText: '支付记录' })).toHaveCount(0)
  await expect(page.getByLabel('统计月份')).toHaveValue(currentMonth)
  await expect(page.getByText('当月销量冠军')).toBeVisible()
  await expect(page.getByText('招牌牛排', { exact: true }).first()).toBeVisible()
  await page.getByLabel('统计月份').fill('2026-06')
  await page.getByLabel('统计月份').dispatchEvent('change')
  await expect.poll(() => adminState.reportQueries.at(-1)).toContain('month=2026-06')
  await page.screenshot({ path: '../artifacts/admin-monthly-dashboard-v1.6.png', fullPage: true, animations: 'disabled' })
  await page.locator('nav a').filter({ hasText: '菜品管理' }).click()
  await expect(page.locator('.data-table .dish-image')).toHaveCount(24)
  await page.getByRole('button', { name: '新增菜品' }).click()
  await expect(page.getByLabel('图片地址（/images/ 路径或 https:// 链接）')).toBeVisible()
  await page.getByRole('button', { name: '取消', exact: true }).click()
  await page.unroute('**/api/**')
  await setup(page, { role: 'staff' })
  await expect(page.locator('nav a').filter({ hasText: '支付记录' })).toHaveCount(0)
})

test('admin order detail query shows one order row and loads dishes on demand', async ({ page }) => {
  const state = await setup(page, { role: 'admin' })
  state.orders.push({ ono: 'Otest0001', orderNumber: '001', orderDate: today, orderTime: `${today}T12:00:00`, cno: 'C0001', eno: 'E0001', tno: 'T0001', totalAmount: 88, paymentStatus: 'UNPAID', orderStatus: 'ACTIVE', endedAt: null })
  state.details = [{ dno: 'D0001', ono: 'Otest0001', mno: 'M0001', dishCount: 1, amount: 88, remark: '七分熟' }]
  await page.locator('nav a').filter({ hasText: '订单明细' }).click()
  const panel = page.locator('.tab-page').filter({ has: page.getByRole('heading', { name: '订单明细查询' }) })
  await expect(panel.locator('.data-table tbody tr')).toHaveCount(1)
  await expect(panel.locator('.data-table')).toContainText('001')
  await expect(panel.locator('.data-table')).not.toContainText('招牌牛排')
  await page.screenshot({ path: '../artifacts/admin-order-detail-query-v1.6.png', fullPage: true, animations: 'disabled' })
  await panel.getByRole('button', { name: '订单详情' }).click()
  await expect(page.locator('.modal-mask')).toContainText('招牌牛排')
  await expect(page.locator('.modal-mask')).toContainText('七分熟')
  await page.screenshot({ path: '../artifacts/admin-order-detail-modal-v1.6.png', fullPage: true, animations: 'disabled' })
})

test('password change validates confirmation and old password', async ({ page }) => {
  const state = await setup(page)
  await page.locator('nav a').filter({ hasText: '个人信息' }).click()
  await page.getByRole('button', { name: '修改密码' }).click()
  await page.getByLabel('旧密码').fill('123456')
  await page.getByLabel('新密码', { exact: true }).fill('abcdef')
  await page.getByLabel('确认新密码').fill('abcdeg')
  await page.getByRole('button', { name: '确认修改密码' }).click()
  await expect(page.getByRole('alert')).toContainText('两次输入的新密码不一致')
  expect(state.passwordCalls).toBe(0)
  await page.getByLabel('确认新密码').fill('abcdef')
  await page.getByLabel('旧密码').fill('wrong')
  await page.getByRole('button', { name: '确认修改密码' }).click()
  await expect(page.getByRole('alert')).toContainText('旧密码不正确')
  await page.getByLabel('旧密码').fill('123456')
  await page.getByRole('button', { name: '确认修改密码' }).click()
  await expect(page.getByRole('status')).toContainText('密码修改成功')
  expect(state.passwordCalls).toBe(2)
})

test('orders default to today and date search sends paging parameters', async ({ page }) => {
  const state = await setup(page)
  await page.locator('nav a').filter({ hasText: '我的订单' }).click()
  await expect(page.getByLabel('开始日期')).toHaveValue(today)
  await expect(page.getByLabel('结束日期')).toHaveValue(today)
  await page.getByLabel('开始日期').fill('2026-06-17')
  await page.getByLabel('结束日期').fill('2026-06-18')
  await page.getByRole('button', { name: '查询', exact: true }).click()
  await expect.poll(() => state.orderQueries.at(-1)).toContain('startDate=2026-06-17')
  expect(state.orderQueries.at(-1)).toContain('endDate=2026-06-18')
  expect(state.orderQueries.at(-1)).toContain('size=10')
})

test('staff transfers an active paid order, ends it, and deletes it', async ({ page }) => {
  const state = await setup(page, { role: 'staff' })
  await page.getByRole('button', { name: '新增订单' }).click()
  await page.getByLabel('顾客编号').selectOption('C0001')
  await page.getByLabel('餐桌编号').selectOption('T0001')
  await page.getByRole('button', { name: '+ 添加菜品' }).click()
  await page.locator('.form-card').filter({ hasText: '添加菜品到订单' }).locator('select').first().selectOption('M0001')
  await page.getByRole('button', { name: '确认创建' }).click()
  await expect(page.locator('.data-table')).toContainText('001')

  await page.getByRole('button', { name: '换桌', exact: true }).click()
  await expect(page.getByLabel('订单日期')).toHaveValue(today)
  await expect(page.getByLabel('订单编号')).toHaveValue('001')
  await page.getByLabel('实际到店人数').fill('5')
  await page.getByLabel('选择更大空闲餐桌').selectOption('T0008')
  await page.screenshot({ path: '../artifacts/order-table-transfer-form-v1.6.png', fullPage: true, animations: 'disabled' })
  await page.getByRole('button', { name: '确认换桌并锁定新桌' }).click()
  await expect(page.getByRole('status')).toContainText('订单 001 已从 T0001 换至 T0008')
  await expect(page.locator('.data-table')).toContainText('T0008')
  expect(state.tableStatus).toBe('待清理')
  expect(state.largeTableStatus).toBe('使用中')
  await page.getByRole('button', { name: '收款', exact: true }).click()
  await page.getByRole('button', { name: '确认模拟支付' }).click()
  await expect(page.getByRole('heading', { name: '模拟支付成功' })).toBeVisible()
  await page.getByRole('button', { name: '完成', exact: true }).click()
  await expect(page.locator('.data-table')).toContainText('已支付（模拟）')
  await page.getByRole('button', { name: '换桌', exact: true }).click()
  await expect(page.getByText('当前订单（已支付）')).toBeVisible()
  await page.getByLabel('实际到店人数').fill('9')
  await page.getByLabel('选择更大空闲餐桌').selectOption('T0013')
  await page.getByRole('button', { name: '确认换桌并锁定新桌' }).click()
  await expect(page.getByRole('status')).toContainText('订单 001 已从 T0008 换至 T0013')
  await expect(page.locator('.data-table')).toContainText('T0013')
  await expect(page.locator('.data-table')).toContainText('已支付（模拟）')
  expect(state.largeTableStatus).toBe('待清理')
  expect(state.largestTableStatus).toBe('使用中')
  page.once('dialog', dialog => dialog.accept())
  await page.getByRole('button', { name: '结束订单', exact: true }).click()
  await expect(page.getByText('订单 001 已结束，不能再换桌')).toBeVisible()
  await expect(page.locator('.data-table')).toContainText('已结束')
  await expect(page.getByRole('button', { name: '换桌', exact: true })).toHaveCount(0)
  await expect(page.getByRole('button', { name: '删除已结束订单', exact: true })).toBeVisible()
  expect(state.largestTableStatus).toBe('待清理')
  await page.screenshot({ path: '../artifacts/employee-order-ended-v1.6.png', fullPage: true, animations: 'disabled' })
  page.once('dialog', dialog => dialog.accept())
  await page.getByRole('button', { name: '删除已结束订单', exact: true }).click()
  await expect(page.getByText('所选日期没有订单记录')).toBeVisible()
  expect(state.orders).toHaveLength(0)
})

test('admin ends and deletes an order from order overview', async ({ page }) => {
  const state = await setup(page, { role: 'admin' })
  state.orders.push({ ono: 'Oadmin001', orderNumber: '001', orderDate: today, orderTime: `${today}T18:00:00`, cno: 'C0001', eno: 'E0001', tno: 'T0001', totalAmount: 88, paymentStatus: 'PAID', orderStatus: 'ACTIVE', endedAt: null })
  state.tableStatus = '使用中'
  await page.locator('nav a').filter({ hasText: '订单总览' }).click()
  await expect(page.locator('.data-table')).toContainText('进行中')
  page.once('dialog', dialog => dialog.accept())
  await page.getByRole('button', { name: '结束订单', exact: true }).click()
  await expect(page.getByText('订单 001 已结束', { exact: true })).toBeVisible()
  await expect(page.getByRole('button', { name: '删除已结束订单', exact: true })).toBeVisible()
  await page.screenshot({ path: '../artifacts/admin-order-ended-v1.6.png', fullPage: true, animations: 'disabled' })
  page.once('dialog', dialog => dialog.accept())
  await page.getByRole('button', { name: '删除已结束订单', exact: true }).click()
  await expect(page.getByText('所选日期没有订单记录')).toBeVisible()
  expect(state.orders).toHaveLength(0)
})

test('customer views live table states and pays a calculated reservation deposit', async ({ page }) => {
  const state = await setup(page)
  state.availabilityBlocks = [{ tno: 'T0004', reservedAt: `${tomorrow}T11:00:00`, reservedUntil: `${tomorrow}T13:00:00` }]
  await page.locator('nav a').filter({ hasText: '餐桌状态' }).click()
  await expect(page.getByText('实时状态', { exact: true })).toBeVisible()
  for (const status of ['空闲', '使用中', '待清理', '已预订']) await expect(page.getByText(status, { exact: true }).first()).toBeVisible()
  await expect(page.getByText('建议预约前亲自致电餐厅询问')).toBeVisible()
  const reservedTable = page.locator('.table-card').filter({ hasText: 'T0004' })
  await reservedTable.getByRole('button', { name: '预约此桌' }).click()
  await expect(page.getByLabel('预约时间段')).toHaveValue('13:30')
  await expect(page.getByLabel('预约时间段').locator('option[value="11:00"]')).toHaveAttribute('disabled', '')
  const largeTable = page.locator('.table-card').filter({ hasText: 'T0008' })
  await largeTable.getByRole('button', { name: '预约此桌' }).click()
  await page.getByLabel('用餐人数').fill('5')
  await expect(page.getByText('需支付预约押金').locator('..')).toContainText('¥50.00')
  await page.screenshot({ path: '../artifacts/customer-reservation-form-v1.6.png', fullPage: true, animations: 'disabled' })
  await page.getByRole('button', { name: '支付 ¥50.00 押金并预约' }).click()
  await expect(page.getByRole('status')).toContainText('T0008 预约成功')
  await expect(largeTable).toContainText('已预订')
  await page.locator('nav a').filter({ hasText: '我的预约' }).click()
  await expect(page.getByRole('heading', { name: '我的预约餐桌' })).toBeVisible()
  const reservationCard = page.locator('.my-reservation-card').filter({ hasText: 'T0008' })
  await expect(reservationCard).toContainText('8 人桌')
  await expect(reservationCard).toContainText('实时桌况：已预订')
  await expect(reservationCard).toContainText(`${tomorrow} 11:00–13:00`)
  await expect(reservationCard).toContainText('13800000001')
  await expect(reservationCard).toContainText('Rtest0001')
  await expect(page.getByText('押金已支付', { exact: true })).toBeVisible()
  expect(state.largeTableStatus).toBe('已预订')
  await page.screenshot({ path: '../artifacts/customer-my-reservation-v1.6.png', fullPage: true, animations: 'disabled' })
})

test('staff reservation management shows customer online booking time and table', async ({ page }) => {
  const state = await setup(page, { role: 'staff' })
  state.reservations = [{ reservationNo: 'Ronline001', customerName: '测试用户', customerPhone: '13800000001', cno: 'C0001', eno: 'E0001', tno: 'T0013', partySize: 10, reservationSource: 'ONLINE', reservedAt: `${today}T19:30:00`, reservedUntil: `${today}T21:30:00`, arrivalDeadline: `${today}T20:00:00`, depositAmount: 70, depositMethod: 'DEMO_WECHAT', depositStatus: 'PAID', status: 'RESERVED', createdAt: `${today}T10:00:00` }]
  await page.reload()
  await page.locator('nav a').filter({ hasText: '预约管理' }).click()
  await expect(page.getByText('顾客线上预约', { exact: true })).toBeVisible()
  await expect(page.locator('.data-table')).toContainText(`${today} 19:30:00`)
  await expect(page.locator('.data-table')).toContainText('T0013')
  await page.screenshot({ path: '../artifacts/staff-online-reservation-v1.6.png', fullPage: true, animations: 'disabled' })
})

test('staff cancels a no-show reservation without refunding the deposit', async ({ page }) => {
  const state = await setup(page, { role: 'staff' })
  state.reservations = [{ reservationNo: 'Rnoshow001', customerName: '未到店顾客', customerPhone: '13800000002', cno: 'C0002', eno: 'E0001', tno: 'T0001', partySize: 2, reservationSource: 'ONLINE', reservedAt: `${today}T00:00:00`, reservedUntil: `${today}T02:00:00`, arrivalDeadline: `${today}T00:30:00`, depositAmount: 20, depositMethod: 'DEMO_WECHAT', depositStatus: 'PAID', status: 'RESERVED', createdAt: `${today}T00:00:00` }]
  await page.reload()
  await page.locator('nav a').filter({ hasText: '预约管理' }).click()
  page.once('dialog', dialog => dialog.accept())
  await page.getByRole('button', { name: '取消订单且不退回押金' }).click()
  await expect(page.getByRole('status')).toContainText('押金不予退回')
  await expect(page.getByText('订单已取消（押金不退）', { exact: true })).toBeVisible()
  await expect(page.locator('.data-table')).toContainText('押金已扣')
  expect(state.reservations[0].depositStatus).toBe('FORFEITED')
  await page.screenshot({ path: '../artifacts/staff-no-show-v1.6.png', fullPage: true, animations: 'disabled' })
})

test('staff records a phone reservation and deposit is refunded after bill payment', async ({ page }) => {
  const state = await setup(page, { role: 'staff' })
  await page.locator('nav a').filter({ hasText: '预约管理' }).click()
  await page.getByRole('button', { name: '登记电话预约' }).click()
  await page.getByLabel('预约人姓名').fill('测试顾客')
  await page.getByLabel('联系电话').fill('13800000001')
  await page.getByLabel('选择餐桌').selectOption('T0001')
  await expect(page.getByText('应收模拟押金').locator('..')).toContainText('¥20.00')
  await page.getByRole('button', { name: '收取押金并确认预约' }).click()
  await expect(page.getByRole('status')).toContainText('已收取模拟押金 ¥20.00')
  await expect(page.locator('.data-table')).toContainText('押金已收')
  await page.screenshot({ path: '../artifacts/phone-reservation-v1.6.png', fullPage: true, animations: 'disabled' })
  expect(state.tableStatus).toBe('已预订')
  await page.getByRole('button', { name: '接待并开单' }).click()
  await page.getByRole('button', { name: '+ 添加菜品' }).click()
  await page.locator('.form-card').filter({ hasText: '添加菜品到订单' }).locator('select').first().selectOption('M0001')
  await page.getByRole('button', { name: '确认创建' }).click()
  await page.getByRole('button', { name: '收款' }).click()
  await page.getByRole('button', { name: '确认模拟支付' }).click()
  await expect(page.getByText('预约押金已退回给顾客')).toBeVisible()
  await expect(page.getByText('已模拟原路退回')).toBeVisible()
  await page.screenshot({ path: '../artifacts/deposit-refund-v1.6.png', animations: 'disabled' })
  expect(state.reservations[0].depositStatus).toBe('REFUNDED')
})
