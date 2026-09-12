<template>
  <dialog ref="dialog" class="payment-dialog" @cancel.prevent="close" @click="onBackdrop">
    <form @submit.prevent="pay">
      <div class="payment-heading"><span class="menu-type-badge">模拟收银台</span><button type="button" class="btn-cancel" :disabled="paying" @click="close" aria-label="关闭支付窗口">关闭</button></div>
      <template v-if="!receipt">
        <h2>确认订单支付</h2>
        <p class="hint">订单 {{ order.orderNumber }} · {{ order.orderDate }} · 餐桌 {{ order.tno }}</p>
        <p class="payment-amount"><small>¥</small>{{ Number(order.totalAmount).toFixed(2) }}</p>
        <p class="payment-notice">当前为模拟支付，不会扣款或发起真实交易。</p>
        <fieldset :disabled="paying || !enabled">
          <legend>选择支付方式</legend>
          <label v-for="(label, key) in availableMethods" :key="key" :class="['payment-method', { selected: method === key }]">
            <input type="radio" v-model="method" :value="key" name="paymentMethod" />{{ label }}
          </label>
        </fieldset>
        <p v-if="!enabled" class="error">模拟支付已关闭，暂未配置实际收款渠道。</p>
        <p v-if="error" class="error" role="alert">{{ error }}</p>
        <button class="btn-primary payment-confirm" :disabled="paying || !enabled">{{ paying ? '正在确认…' : '确认模拟支付' }}</button>
        <button type="button" class="btn-cancel payment-confirm" :disabled="paying" @click="close">稍后支付</button>
      </template>
      <template v-else>
        <div class="payment-success-mark" aria-hidden="true">✓</div>
        <h2>模拟支付成功</h2>
        <p class="payment-amount"><small>¥</small>{{ Number(receipt.amount).toFixed(2) }}</p>
        <dl class="payment-receipt">
          <dt>订单编号</dt><dd>{{ order.orderDate }} · {{ order.orderNumber }}</dd>
          <dt>支付方式</dt><dd>{{ paymentMethods[receipt.method] }}</dd>
          <dt>支付时间</dt><dd>{{ receipt.paidAt?.replace('T', ' ') }}</dd>
          <dt>支付流水</dt><dd>{{ receipt.paymentNo }}</dd>
          <template v-if="receipt.depositRefund">
            <dt>预约押金</dt><dd>¥{{ Number(receipt.depositRefund.amount).toFixed(2) }}</dd>
            <dt>押金退款</dt><dd class="deposit-refund-text">已模拟原路退回 · {{ receipt.depositRefund.refundedAt?.replace('T', ' ') }}</dd>
          </template>
        </dl>
        <p v-if="receipt.depositRefund" class="deposit-refund-notice">预约押金已退回给顾客，预约流程已完成。</p>
        <p class="payment-notice">本次支付与押金退款均为演示交易，未产生实际扣款。</p>
        <button type="button" class="btn-primary payment-confirm" @click="close">完成</button>
      </template>
    </form>
  </dialog>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { api, paymentMethods } from '../api'
const props = defineProps({ order: { type: Object, required: true }, staff: Boolean })
const emit = defineEmits(['close', 'paid'])
const dialog = ref(null)
const method = ref('DEMO_WECHAT')
const paying = ref(false)
const error = ref('')
const receipt = ref(null)
const enabled = ref(false)
const availableMethods = computed(() => Object.fromEntries(Object.entries(paymentMethods).filter(([key]) => props.staff || key !== 'DEMO_CASH')))
function close() { if (!paying.value) { dialog.value.close(); emit('close') } }
function onBackdrop(event) { if (event.target === dialog.value) { const r = dialog.value.getBoundingClientRect(); if (event.clientX < r.left || event.clientX > r.right || event.clientY < r.top || event.clientY > r.bottom) close() } }
async function pay() {
  if (paying.value || !enabled.value) return
  paying.value = true; error.value = ''
  try {
    receipt.value = await api(`/payments/${props.order.ono}`, { method: 'POST', body: JSON.stringify({ method: method.value }) })
    emit('paid', receipt.value)
  } catch (e) { error.value = e.message }
  finally { paying.value = false }
}
onMounted(async () => {
  dialog.value.showModal()
  try { enabled.value = (await api('/payments/config')).enabled }
  catch (e) { error.value = e.message }
})
</script>
