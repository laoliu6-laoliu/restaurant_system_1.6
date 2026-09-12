import { createRouter, createWebHashHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    name: 'login',
    component: () => import('../views/LoginPage.vue')
  },
  {
    path: '/customer',
    name: 'customer',
    component: () => import('../views/customer/CustomerHome.vue'),
    meta: { role: 'customer' }
  },
  {
    path: '/employee',
    name: 'employee',
    component: () => import('../views/employee/EmployeeHome.vue'),
    meta: { role: 'staff' }
  },
  {
    path: '/admin',
    name: 'admin',
    component: () => import('../views/admin/AdminHome.vue'),
    meta: { role: 'admin' }
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const session = sessionStorage.getItem('session')
  if (to.name !== 'login' && !session) {
    next({ name: 'login' })
    return
  }
  if (session) {
    const user = JSON.parse(session)
    const roleMap = { admin: 'admin', staff: 'employee', customer: 'customer' }
    const expected = roleMap[user.role]
    if (to.name !== 'login' && to.name !== expected) {
      next({ name: expected })
      return
    }
  }
  next()
})

export default router
