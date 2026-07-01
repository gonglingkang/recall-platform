<template>
  <Layout v-if="$route.meta.requiresAuth">
    <router-view v-slot="{ Component }">
      <transition name="fade" mode="out-in">
        <component :is="Component" />
      </transition>
    </router-view>
  </Layout>
  <div v-else class="public-layout-wrapper">
    <PublicNavbar v-if="['home', 'login', 'register'].includes($route.name as string)" />
    <router-view v-slot="{ Component }">
      <transition name="fade" mode="out-in">
        <component :is="Component" />
      </transition>
    </router-view>
  </div>
</template>

<script setup lang="ts">
import { watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import Layout from '@/components/Layout.vue'
import PublicNavbar from '@/components/PublicNavbar.vue'

const authStore = useAuthStore()
const route = useRoute()
const router = useRouter()

// Reactive safety net: if the user logs out or session expires while on a protected page, instantly push to home '/'
watch(() => authStore.isLoggedIn, (loggedIn) => {
  if (!loggedIn && route.meta?.requiresAuth) {
    router.push('/')
  }
})
</script>

<style>
/* Global page transitions */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateY(4px);
}

/* Public pages container layout */
.public-layout-wrapper {
  background-color: #0b0f19;
  color: #f3f4f6;
  min-height: 100vh;
  font-family: var(--font-sans);
  margin: -32px -40px;
  padding: 32px 40px;
  box-sizing: border-box;
}

@media (max-width: 768px) {
  .public-layout-wrapper {
    margin: -24px -16px;
    padding: 24px 16px;
  }
}
</style>
