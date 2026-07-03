<script setup>
import { ref, computed } from 'vue'
import { useRoute } from 'vue-router'
import Sidebar from './components/Sidebar.vue'
import ApiStatus from './components/ApiStatus.vue'
import TenantSelector from './components/TenantSelector.vue'
import { useNotification } from './composables/useNotification'

const route = useRoute()
const sidebarCollapsed = ref(false)

const { notifications, removeNotification } = useNotification()

const pageTitle = computed(() => {
  const name = route.name || ''
  const titles = {
    tenants: 'Tenants',
    tenantDetail: 'Tenant Details',
    idp: 'IdP Connections',
    users: 'Users',
    groups: 'Groups',
    roles: 'Role Mappings',
    entitlements: 'Entitlements',
    policy: 'Policy Tester',
    audit: 'Audit Log',
    onboarding: 'Onboarding Guide',
    notFound: 'Page Not Found',
  }
  return titles[name] || 'Identity Entitlement Broker'
})

function toggleSidebar() {
  sidebarCollapsed.value = !sidebarCollapsed.value
}
</script>

<template>
  <div class="app-layout">
    <Sidebar :collapsed="sidebarCollapsed" @toggle="toggleSidebar" />

    <div class="main-wrapper" :class="{ 'sidebar-collapsed': sidebarCollapsed }">
      <header class="top-header">
        <div class="header-left">
          <button class="sidebar-toggle btn-ghost" @click="toggleSidebar" title="Toggle sidebar">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <line x1="3" y1="6" x2="21" y2="6" />
              <line x1="3" y1="12" x2="21" y2="12" />
              <line x1="3" y1="18" x2="21" y2="18" />
            </svg>
          </button>
          <h1 class="page-title">{{ pageTitle }}</h1>
        </div>
        <div class="header-right">
          <TenantSelector />
        </div>
      </header>

      <main class="main-content">
        <router-view />
      </main>
    </div>

    <!-- Notification Toasts -->
    <div class="notification-container">
      <div
        v-for="n in notifications"
        :key="n.id"
        :class="['notification-toast', n.type]"
      >
        <span class="notification-message">{{ n.message }}</span>
        <button class="notification-close" @click="removeNotification(n.id)">&times;</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.app-layout {
  display: flex;
  min-height: 100vh;
}

.main-wrapper {
  flex: 1;
  margin-left: var(--sidebar-width);
  display: flex;
  flex-direction: column;
  transition: margin-left var(--transition-normal);
}

.main-wrapper.sidebar-collapsed {
  margin-left: 64px;
}

.top-header {
  height: var(--header-height);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 1.5rem;
  background-color: var(--color-bg-secondary);
  border-bottom: 1px solid var(--color-border);
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.sidebar-toggle {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0.35rem;
  border-radius: var(--radius-md);
  cursor: pointer;
  color: var(--color-text-secondary);
  background: none;
  border: none;
}

.sidebar-toggle:hover {
  color: var(--color-text-primary);
  background-color: var(--color-bg-hover);
}

.page-title {
  font-size: 1.15rem;
  font-weight: 600;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.main-content {
  flex: 1;
  padding: 1.5rem;
  overflow-y: auto;
}
</style>
