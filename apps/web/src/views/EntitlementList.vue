<script setup>
import { ref, onMounted } from 'vue'
import {
  getProducts, createProduct,
  getEntitlements, createEntitlement,
  assignEntitlement, getEffectiveEntitlements, revokeEntitlement,
  getUsers, getGroups,
} from '../api/client.js'
import { useNotification } from '../composables/useNotification.js'
import DataTable from '../components/DataTable.vue'
import LoadingSpinner from '../components/LoadingSpinner.vue'
import ErrorAlert from '../components/ErrorAlert.vue'

const { success, error: notifyError } = useNotification()

// Products
const products = ref([])
const productsLoading = ref(false)
const productsError = ref('')
const showAddProduct = ref(false)
const newProduct = ref({ name: '', description: '' })
const creatingProduct = ref(false)

// Entitlements
const entitlements = ref([])
const entLoading = ref(false)
const entError = ref('')
const showAddEnt = ref(false)
const newEnt = ref({ product_id: '', name: '', description: '' })
const creatingEnt = ref(false)

// Assignments
const assignments = ref([])
const assignLoading = ref(false)
const assignError = ref('')
const showAssignForm = ref(false)
const assignForm = ref({ entitlement_id: '', user_id: '', group_id: '' })
const assigning = ref(false)

// Effective entitlements lookup
const lookupUserId = ref('')
const effectiveEnts = ref([])
const effLoading = ref(false)
const effError = ref('')

const users = ref([])
const groups = ref([])

// ===== API calls =====

async function fetchProducts() {
  productsLoading.value = true
  productsError.value = ''
  try {
    const data = await getProducts()
    products.value = Array.isArray(data) ? data : data?.products || []
  } catch (err) {
    productsError.value = err.displayMessage || 'Failed to load products'
  } finally {
    productsLoading.value = false
  }
}

async function fetchEntitlements() {
  entLoading.value = true
  entError.value = ''
  try {
    const data = await getEntitlements()
    entitlements.value = Array.isArray(data) ? data : data?.entitlements || []
  } catch (err) {
    entError.value = err.displayMessage || 'Failed to load entitlements'
  } finally {
    entLoading.value = false
  }
}

async function fetchUsers() {
  try {
    const data = await getUsers()
    users.value = Array.isArray(data) ? data : data?.Resources || data?.users || []
  } catch {
    users.value = []
  }
}

async function fetchGroups() {
  try {
    const data = await getGroups()
    groups.value = Array.isArray(data) ? data : data?.Resources || data?.groups || []
  } catch {
    groups.value = []
  }
}

onMounted(async () => {
  await Promise.all([fetchProducts(), fetchEntitlements(), fetchUsers(), fetchGroups()])
})

// ===== Product CRUD =====

async function handleCreateProduct() {
  if (!newProduct.value.name) return
  creatingProduct.value = true
  try {
    await createProduct(newProduct.value)
    success('Product created')
    showAddProduct.value = false
    newProduct.value = { name: '', description: '' }
    await fetchProducts()
  } catch (err) {
    notifyError(err.displayMessage || 'Failed to create product')
  } finally {
    creatingProduct.value = false
  }
}

// ===== Entitlement CRUD =====

async function handleCreateEntitlement() {
  if (!newEnt.value.product_id || !newEnt.value.name) return
  creatingEnt.value = true
  try {
    await createEntitlement(newEnt.value)
    success('Entitlement created')
    showAddEnt.value = false
    newEnt.value = { product_id: '', name: '', description: '' }
    await fetchEntitlements()
  } catch (err) {
    notifyError(err.displayMessage || 'Failed to create entitlement')
  } finally {
    creatingEnt.value = false
  }
}

// ===== Assignments =====

async function handleAssign() {
  if (!assignForm.value.entitlement_id) return
  assigning.value = true
  try {
    const payload = { entitlement_id: assignForm.value.entitlement_id }
    if (assignForm.value.user_id) payload.user_id = assignForm.value.user_id
    if (assignForm.value.group_id) payload.group_id = assignForm.value.group_id
    await assignEntitlement(payload)
    success('Entitlement assigned')
    showAssignForm.value = false
    assignForm.value = { entitlement_id: '', user_id: '', group_id: '' }
  } catch (err) {
    notifyError(err.displayMessage || 'Failed to assign')
  } finally {
    assigning.value = false
  }
}

async function handleRevoke(id) {
  if (!confirm('Revoke this entitlement assignment?')) return
  try {
    await revokeEntitlement(id)
    success('Entitlement revoked')
    if (lookupUserId.value) await lookupEffective()
  } catch (err) {
    notifyError(err.displayMessage || 'Failed to revoke')
  }
}

async function lookupEffective() {
  if (!lookupUserId.value) return
  effLoading.value = true
  effError.value = ''
  try {
    const data = await getEffectiveEntitlements(lookupUserId.value)
    effectiveEnts.value = Array.isArray(data) ? data : data?.entitlements || []
  } catch (err) {
    effError.value = err.displayMessage || 'Failed to lookup entitlements'
    effectiveEnts.value = []
  } finally {
    effLoading.value = false
  }
}

// Table columns
const productColumns = [
  { key: 'name', label: 'Name', sortable: true },
  { key: 'description', label: 'Description', sortable: true },
]

const entColumns = [
  { key: 'name', label: 'Name', sortable: true },
  {
    key: 'product_id', label: 'Product', sortable: true,
    format: (v) => {
      if (!v) return '—'
      const p = products.value.find((p) => (p.id || p.ID) === v)
      return p?.name || p?.Name || v
    },
  },
  { key: 'description', label: 'Description' },
]

function getProductName(id) {
  const p = products.value.find((p) => (p.id || p.ID) === id)
  return p?.name || p?.Name || id
}

function getUserName(id) {
  const u = users.value.find((u) => (u.id || u.ID) === id)
  return u?.displayName || u?.userName || u?.DisplayName || id
}
</script>

<template>
  <div class="entitlement-page">
    <!-- ===== PRODUCTS ===== -->
    <section class="section">
      <div class="section-header">
        <h2>Products</h2>
        <button class="btn btn-primary btn-sm" @click="showAddProduct = !showAddProduct">
          {{ showAddProduct ? 'Cancel' : '+ Add Product' }}
        </button>
      </div>

      <div v-if="showAddProduct" class="card add-form mb-4">
        <form @submit.prevent="handleCreateProduct">
          <div class="form-row">
            <div class="form-group">
              <label class="form-label">Product Name *</label>
              <input v-model="newProduct.name" type="text" required placeholder="e.g. Premium SaaS" />
            </div>
            <div class="form-group">
              <label class="form-label">Description</label>
              <input v-model="newProduct.description" type="text" placeholder="Description of the product" />
            </div>
          </div>
          <button type="submit" class="btn btn-primary" :disabled="creatingProduct">
            {{ creatingProduct ? 'Creating...' : 'Create Product' }}
          </button>
        </form>
      </div>

      <ErrorAlert :message="productsError" @dismiss="productsError = ''" />
      <LoadingSpinner v-if="productsLoading" message="Loading products..." />
      <div v-else-if="products.length === 0 && !productsError" class="empty-section">
        <p class="text-muted">No products defined.</p>
      </div>
      <DataTable v-else :columns="productColumns" :data="products" title="Products" />
    </section>

    <!-- ===== ENTITLEMENTS ===== -->
    <section class="section">
      <div class="section-header">
        <h2>Entitlements</h2>
        <button class="btn btn-primary btn-sm" @click="showAddEnt = !showAddEnt">
          {{ showAddEnt ? 'Cancel' : '+ Add Entitlement' }}
        </button>
      </div>

      <div v-if="showAddEnt" class="card add-form mb-4">
        <form @submit.prevent="handleCreateEntitlement">
          <div class="form-row">
            <div class="form-group">
              <label class="form-label">Product *</label>
              <select v-model="newEnt.product_id" required>
                <option value="">Select product...</option>
                <option v-for="p in products" :key="p.id || p.ID" :value="p.id || p.ID">
                  {{ p.name || p.Name }}
                </option>
              </select>
            </div>
            <div class="form-group">
              <label class="form-label">Entitlement Name *</label>
              <input v-model="newEnt.name" type="text" required placeholder="e.g. can-invite-users" />
            </div>
          </div>
          <div class="form-group">
            <label class="form-label">Description</label>
            <input v-model="newEnt.description" type="text" placeholder="What this entitlement grants" />
          </div>
          <button type="submit" class="btn btn-primary" :disabled="creatingEnt">
            {{ creatingEnt ? 'Creating...' : 'Create Entitlement' }}
          </button>
        </form>
      </div>

      <ErrorAlert :message="entError" @dismiss="entError = ''" />
      <LoadingSpinner v-if="entLoading" message="Loading entitlements..." />
      <div v-else-if="entitlements.length === 0 && !entError" class="empty-section">
        <p class="text-muted">No entitlements defined.</p>
      </div>
      <DataTable v-else :columns="entColumns" :data="entitlements" title="Entitlements" />
    </section>

    <!-- ===== ASSIGNMENTS ===== -->
    <section class="section">
      <div class="section-header">
        <h2>Assignments</h2>
        <button class="btn btn-primary btn-sm" @click="showAssignForm = !showAssignForm">
          {{ showAssignForm ? 'Cancel' : '+ Assign Entitlement' }}
        </button>
      </div>

      <div v-if="showAssignForm" class="card add-form mb-4">
        <form @submit.prevent="handleAssign">
          <div class="form-group">
            <label class="form-label">Entitlement *</label>
            <select v-model="assignForm.entitlement_id" required>
              <option value="">Select entitlement...</option>
              <option v-for="e in entitlements" :key="e.id || e.ID" :value="e.id || e.ID">
                {{ e.name || e.Name }} ({{ getProductName(e.product_id || e.ProductID) }})
              </option>
            </select>
          </div>
          <div class="form-row">
            <div class="form-group">
              <label class="form-label">User</label>
              <select v-model="assignForm.user_id">
                <option value="">— None —</option>
                <option v-for="u in users" :key="u.id || u.ID" :value="u.id || u.ID">
                  {{ u.displayName || u.userName || u.id }}
                </option>
              </select>
            </div>
            <div class="form-group">
              <label class="form-label">Group</label>
              <select v-model="assignForm.group_id">
                <option value="">— None —</option>
                <option v-for="g in groups" :key="g.id || g.ID" :value="g.id || g.ID">
                  {{ g.displayName || g.DisplayName || g.id }}
                </option>
              </select>
            </div>
          </div>
          <button type="submit" class="btn btn-primary" :disabled="assigning">
            {{ assigning ? 'Assigning...' : 'Assign' }}
          </button>
        </form>
      </div>

      <!-- Effective Entitlements Lookup -->
      <div class="card lookup-section">
        <h3 class="card-title mb-2">Effective Entitlements Lookup</h3>
        <div class="lookup-form">
          <div class="form-group" style="flex: 1;">
            <label class="form-label">User ID</label>
            <div class="lookup-row">
              <input v-model="lookupUserId" type="text" placeholder="Enter user ID to look up effective entitlements" />
              <button class="btn btn-primary" @click="lookupEffective" :disabled="!lookupUserId || effLoading">
                {{ effLoading ? 'Searching...' : 'Lookup' }}
              </button>
            </div>
          </div>
        </div>

        <ErrorAlert :message="effError" @dismiss="effError = ''" />

        <LoadingSpinner v-if="effLoading" message="Looking up entitlements..." />

        <div v-else-if="effectiveEnts.length > 0" class="effective-list">
          <div v-for="ent in effectiveEnts" :key="ent.id || ent.ID" class="effective-item">
            <div class="eff-info">
              <span class="eff-name">{{ ent.name || ent.Name }}</span>
              <span v-if="ent.product_name || ent.ProductName" class="eff-product">
                — {{ ent.product_name || ent.ProductName }}
              </span>
            </div>
            <button class="btn btn-danger btn-sm" @click="handleRevoke(ent.id || ent.ID)">Revoke</button>
          </div>
        </div>

        <div v-else-if="lookupUserId && !effLoading && !effError" class="text-muted lookup-empty">
          No entitlements found for this user.
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.entitlement-page {
  max-width: 1000px;
}

.section {
  margin-bottom: 2rem;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 0.75rem;
}

.section-header h2 {
  font-size: 1.1rem;
}

.add-form {
  margin-bottom: 1rem;
}

.form-row {
  display: flex;
  gap: 1rem;
}

.form-row > * {
  flex: 1;
}

@media (max-width: 640px) {
  .form-row {
    flex-direction: column;
    gap: 0;
  }
}

.empty-section {
  padding: 1.5rem 1rem;
  text-align: center;
  background: var(--color-bg-card);
  border: 1px dashed var(--color-border);
  border-radius: var(--radius-lg);
}

.lookup-section {
  margin-top: 1rem;
}

.lookup-form {
  margin-bottom: 1rem;
}

.lookup-row {
  display: flex;
  gap: 0.5rem;
}

.lookup-row input {
  flex: 1;
}

.effective-list {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.effective-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.5rem 0.75rem;
  background: var(--color-bg-primary);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
}

.eff-name {
  font-weight: 500;
  color: var(--color-text-primary);
  font-size: 0.9rem;
}

.eff-product {
  color: var(--color-text-muted);
  font-size: 0.85rem;
}

.lookup-empty {
  padding: 1rem 0;
  text-align: center;
}
</style>
