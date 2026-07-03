import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    redirect: '/tenants',
  },
  {
    path: '/tenants',
    name: 'tenants',
    component: () => import('../views/TenantList.vue'),
  },
  {
    path: '/tenants/:id',
    name: 'tenantDetail',
    component: () => import('../views/TenantDetail.vue'),
    props: true,
  },
  {
    path: '/idp',
    name: 'idp',
    component: () => import('../views/IdpList.vue'),
  },
  {
    path: '/users',
    name: 'users',
    component: () => import('../views/UserList.vue'),
  },
  {
    path: '/groups',
    name: 'groups',
    component: () => import('../views/GroupList.vue'),
  },
  {
    path: '/roles',
    name: 'roles',
    component: () => import('../views/RoleMappingList.vue'),
  },
  {
    path: '/entitlements',
    name: 'entitlements',
    component: () => import('../views/EntitlementList.vue'),
  },
  {
    path: '/policy',
    name: 'policy',
    component: () => import('../views/PolicyTester.vue'),
  },
  {
    path: '/audit',
    name: 'audit',
    component: () => import('../views/AuditExplorer.vue'),
  },
  {
    path: '/onboarding',
    name: 'onboarding',
    component: () => import('../views/OnboardingGuide.vue'),
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'notFound',
    component: () => import('../views/NotFound.vue'),
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

export default router
