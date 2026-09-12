<template>
  <dialog ref="dialog" class="password-dialog" aria-labelledby="password-title" @cancel.prevent="close">
    <form @submit.prevent="save">
      <h2 id="password-title">修改密码</h2>
      <p class="hint">先验证旧密码，再输入两次相同的新密码。</p>
      <template v-if="!success">
        <label>旧密码<input v-model="form.oldPassword" type="password" autocomplete="current-password" required maxlength="20" :disabled="saving" /></label>
        <label>新密码<input v-model="form.newPassword" type="password" autocomplete="new-password" required minlength="6" maxlength="20" placeholder="6 至 20 位" :disabled="saving" /></label>
        <label>确认新密码<input v-model="form.confirmPassword" type="password" autocomplete="new-password" required minlength="6" maxlength="20" placeholder="再次输入新密码" :disabled="saving" /></label>
        <p v-if="error" class="error" role="alert">{{ error }}</p>
        <div class="form-actions"><button type="submit" :disabled="saving">{{ saving ? '验证并修改中…' : '确认修改密码' }}</button><button type="button" class="btn-cancel" :disabled="saving" @click="close">取消</button></div>
      </template>
      <template v-else><p class="success" role="status">密码修改成功，下次登录请使用新密码。</p><button type="button" @click="close">完成</button></template>
    </form>
  </dialog>
</template>
<script setup>
import { onMounted, reactive, ref } from 'vue'
import { api } from '../api'
const emit = defineEmits(['close'])
const dialog = ref(null)
const form = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const saving = ref(false)
const success = ref(false)
const error = ref('')
function close() { if (!saving.value) { dialog.value.close(); emit('close') } }
async function save() {
  if (saving.value) return
  error.value = ''
  if (form.newPassword !== form.confirmPassword) { error.value = '两次输入的新密码不一致'; return }
  saving.value = true
  try {
    await api('/auth/password', { method: 'POST', body: JSON.stringify(form) })
    form.oldPassword = ''; form.newPassword = ''; form.confirmPassword = ''; success.value = true
  } catch (e) { error.value = e.message }
  finally { saving.value = false }
}
onMounted(() => dialog.value.showModal())
</script>
