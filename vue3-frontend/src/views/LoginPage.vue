<template>
  <main class="login-page">
    <form class="login-card" @submit.prevent="login">
      <div class="login-brand">
        <span class="brand-icon">🍽️</span>
        <h1>臻味餐厅管理系统</h1><span class="version-badge">v1.6</span>
        <p class="subtitle">请选择身份并登录</p>
      </div>
      <label>登录身份
        <select v-model="loginForm.role">
          <option value="admin">👨‍💼 管理员</option>
          <option value="staff">👨‍🍳 员工</option>
          <option value="customer">👤 顾客</option>
        </select>
      </label>
      <label>{{ loginIdLabel }}
        <input v-model="loginForm.id" required placeholder="请输入编号" />
      </label>
      <label>密码
        <input v-model="loginForm.password" required type="password" placeholder="请输入密码" />
      </label>
      <button type="submit" :disabled="loading">{{ loading ? '登录中...' : '登录' }}</button>
      <p v-if="loginError" class="error">{{ loginError }}</p>
      <p class="hint">测试账号：管理员 A0001 / 员工 E0001 / 顾客 C0001 &nbsp; 密码均为 123456</p>
    </form>
  </main>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'

const API = '/api'
const router = useRouter()
const loginError = ref('')
const loading = ref(false)
const loginForm = reactive({ role: 'admin', id: '', password: '' })

const loginIdLabel = computed(() => {
  if (loginForm.role === 'admin') return '管理员编号'
  if (loginForm.role === 'staff') return '员工工号'
  return '顾客编号'
})

async function login() {
  loginError.value = ''
  loading.value = true
  try {
    const res = await fetch(`${API}/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(loginForm)
    })
    if (!res.ok) throw new Error()
    const data = await res.json()
    sessionStorage.setItem('session', JSON.stringify(data))
    const roleMap = { admin: '/admin', staff: '/employee', customer: '/customer' }
    router.push(roleMap[data.role] || '/')
  } catch {
    loginError.value = '编号或密码错误，请重新输入。'
  } finally {
    loading.value = false
  }
}
</script>
