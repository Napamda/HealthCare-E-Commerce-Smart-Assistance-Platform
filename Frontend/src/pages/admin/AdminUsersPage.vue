<script setup>
import { ref, onMounted, watch } from 'vue'
import { useAuthStore } from '../../stores/auth.js'
import { ROLE_LABELS } from '../../config/permissions.js'
import {
  getUsers,
  setUserStatus,
  changeUserRole,
  getUserActivity,
} from '../../services/admin.js'
import '../../styles/components/admin-users.css'

const authStore = useAuthStore()

const ASSIGNABLE_ROLES = ['PATIENT', 'DOCTOR', 'PHARMACIST', 'VENDOR']

const users = ref([])
const loading = ref(true)
const saving = ref(false)
const message = ref('')
const messageType = ref('success')

// Filters
const search = ref('')
const roleFilter = ref('')
const statusFilter = ref('')
const page = ref(0)
const totalPages = ref(0)
const totalElements = ref(0)
const pageSize = 20

// Inline role editor
const editingRoleId = ref(null)
const roleDraft = ref('')

// Activity modal
const activityModal = ref(null) // { user, entries, page, totalPages, loading }
const activityLoading = ref(false)

let searchDebounce = null

function flash(type, text) {
  messageType.value = type
  message.value = text
  setTimeout(() => {
    message.value = ''
  }, 4000)
}

function errText(e) {
  return (
    e.response?.data?.error ||
    (e.response?.data?.errors ? Object.values(e.response.data.errors)[0] : null) ||
    e.message ||
    'Something went wrong'
  )
}

function formatDate(iso) {
  if (!iso) return '—'
  return new Date(iso).toLocaleDateString(undefined, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  })
}

function formatDateTime(iso) {
  if (!iso) return '—'
  return new Date(iso).toLocaleString(undefined, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

const ACTION_LABELS = {
  REGISTERED: 'Registered',
  LOGIN: 'Signed in',
  SUSPENDED: 'Suspended',
  ACTIVATED: 'Reactivated',
  ROLE_CHANGED: 'Role changed',
}

async function fetchUsers() {
  loading.value = true
  try {
    const data = await getUsers({
      search: search.value || undefined,
      role: roleFilter.value || undefined,
      status: statusFilter.value || undefined,
      page: page.value,
      size: pageSize,
    })
    users.value = data.content
    totalPages.value = data.totalPages
    totalElements.value = data.totalElements
  } catch (e) {
    flash('error', errText(e))
  } finally {
    loading.value = false
  }
}

watch([roleFilter, statusFilter], () => {
  page.value = 0
  fetchUsers()
})

watch(search, () => {
  clearTimeout(searchDebounce)
  searchDebounce = setTimeout(() => {
    page.value = 0
    fetchUsers()
  }, 350)
})

function nextPage() {
  if (page.value < totalPages.value - 1) {
    page.value += 1
    fetchUsers()
  }
}

function prevPage() {
  if (page.value > 0) {
    page.value -= 1
    fetchUsers()
  }
}

function isSelf(user) {
  return user.id === authStore.currentUser?.id
}

// ---- Suspend / activate ----

async function toggleStatus(user) {
  if (isSelf(user) || user.role === 'ADMIN') return

  if (user.status === 'ACTIVE') {
    const reason = window.prompt(
      `Suspend ${user.firstName} ${user.lastName}?\nOptionally enter a reason (shown in the activity log):`
    )
    if (reason === null) return
    saving.value = true
    try {
      const updated = await setUserStatus(user.id, 'SUSPENDED', reason || null)
      mergeUser(updated)
      flash('success', `${updated.firstName} has been suspended. Their sessions have been revoked.`)
    } catch (e) {
      flash('error', errText(e))
    } finally {
      saving.value = false
    }
  } else {
    if (!window.confirm(`Reactivate ${user.firstName} ${user.lastName}'s account?`)) return
    saving.value = true
    try {
      const updated = await setUserStatus(user.id, 'ACTIVE')
      mergeUser(updated)
      flash('success', `${updated.firstName}'s account has been reactivated.`)
    } catch (e) {
      flash('error', errText(e))
    } finally {
      saving.value = false
    }
  }
}

function mergeUser(updated) {
  const idx = users.value.findIndex((u) => u.id === updated.id)
  if (idx !== -1) users.value.splice(idx, 1, updated)
}

// ---- Role change ----

function startEditRole(user) {
  editingRoleId.value = user.id
  roleDraft.value = user.role
}

function cancelEditRole() {
  editingRoleId.value = null
  roleDraft.value = ''
}

async function saveRole(user) {
  if (!roleDraft.value || roleDraft.value === user.role) {
    cancelEditRole()
    return
  }
  if (
    !window.confirm(
      `Change ${user.firstName} ${user.lastName}'s role from ${ROLE_LABELS[user.role] || user.role} to ${ROLE_LABELS[roleDraft.value] || roleDraft.value}?`
    )
  ) {
    return
  }
  saving.value = true
  try {
    const updated = await changeUserRole(user.id, roleDraft.value)
    mergeUser(updated)
    flash('success', 'Role updated.')
    cancelEditRole()
  } catch (e) {
    flash('error', errText(e))
  } finally {
    saving.value = false
  }
}

// ---- Activity log modal ----

async function openActivity(user) {
  activityModal.value = { user, entries: [], page: 0, totalPages: 0 }
  activityLoading.value = true
  await loadActivityPage(0)
}

async function loadActivityPage(targetPage) {
  if (!activityModal.value) return
  activityLoading.value = true
  try {
    const data = await getUserActivity(activityModal.value.user.id, {
      page: targetPage,
      size: 10,
    })
    activityModal.value = {
      ...activityModal.value,
      entries: data.content,
      page: data.page,
      totalPages: data.totalPages,
    }
  } catch (e) {
    flash('error', errText(e))
  } finally {
    activityLoading.value = false
  }
}

function closeActivity() {
  activityModal.value = null
}

onMounted(fetchUsers)
</script>

<template>
  <div class="admin-users-page">
    <div class="admin-users-container">
      <header class="admin-users-header">
        <h1>User Management</h1>
        <p>View, search, moderate and audit all platform accounts.</p>
      </header>

      <div v-if="message" class="au-message" :class="messageType">{{ message }}</div>

      <!-- Filters -->
      <div class="au-filters">
        <input
          v-model="search"
          type="search"
          class="au-search"
          placeholder="Search by name or email…"
          aria-label="Search users"
        />
        <select v-model="roleFilter" class="au-select" aria-label="Filter by role">
          <option value="">All roles</option>
          <option v-for="(label, key) in ROLE_LABELS" :key="key" :value="key">
            {{ label }}
          </option>
        </select>
        <select v-model="statusFilter" class="au-select" aria-label="Filter by status">
          <option value="">All statuses</option>
          <option value="ACTIVE">Active</option>
          <option value="SUSPENDED">Suspended</option>
        </select>
      </div>

      <div v-if="loading" class="au-loading">Loading users…</div>

      <template v-else>
        <div class="au-table-wrap">
          <table class="au-table">
            <thead>
              <tr>
                <th>User</th>
                <th>Role</th>
                <th>Status</th>
                <th>Joined</th>
                <th class="au-actions-col">Actions</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="user in users" :key="user.id" :class="{ 'is-suspended': user.status === 'SUSPENDED' }">
                <td>
                  <div class="au-user-cell">
                    <span class="au-avatar">
                      <img v-if="user.avatarUrl" :src="user.avatarUrl" alt="" />
                      <template v-else>{{ user.firstName?.[0] || '?' }}</template>
                    </span>
                    <div class="au-user-meta">
                      <span class="au-user-name">
                        {{ user.firstName }} {{ user.lastName }}
                        <span v-if="isSelf(user)" class="au-self-badge">You</span>
                      </span>
                      <span class="au-user-email">{{ user.email }}</span>
                    </div>
                  </div>
                </td>
                <td>
                  <div v-if="editingRoleId === user.id" class="au-role-edit">
                    <select v-model="roleDraft" class="au-select au-select-sm">
                      <option v-for="r in ASSIGNABLE_ROLES" :key="r" :value="r">
                        {{ ROLE_LABELS[r] }}
                      </option>
                    </select>
                    <button class="au-icon-btn" :disabled="saving" @click="saveRole(user)" title="Save role">✓</button>
                    <button class="au-icon-btn" :disabled="saving" @click="cancelEditRole" title="Cancel">✕</button>
                  </div>
                  <span v-else class="au-role-badge" :class="'role-' + user.role.toLowerCase()">
                    {{ ROLE_LABELS[user.role] || user.role }}
                  </span>
                </td>
                <td>
                  <span class="au-status-badge" :class="user.status.toLowerCase()">
                    {{ user.status === 'SUSPENDED' ? 'Suspended' : 'Active' }}
                  </span>
                  <span v-if="!user.emailVerified" class="au-unverified">unverified</span>
                </td>
                <td class="au-joined">{{ formatDate(user.createdAt) }}</td>
                <td>
                  <div class="au-row-actions">
                    <button class="au-btn au-btn-ghost" @click="openActivity(user)">
                      Activity
                    </button>
                    <template v-if="!isSelf(user) && user.role !== 'ADMIN'">
                      <button class="au-btn au-btn-ghost" @click="startEditRole(user)">
                        Change Role
                      </button>
                      <button
                        class="au-btn"
                        :class="user.status === 'SUSPENDED' ? 'au-btn-success' : 'au-btn-danger'"
                        :disabled="saving"
                        @click="toggleStatus(user)"
                      >
                        {{ user.status === 'SUSPENDED' ? 'Activate' : 'Suspend' }}
                      </button>
                    </template>
                    <span v-else-if="user.role === 'ADMIN'" class="au-protected">Protected</span>
                  </div>
                </td>
              </tr>
              <tr v-if="users.length === 0">
                <td colspan="5" class="au-empty">No users match your filters.</td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="au-pagination">
          <span class="au-page-info">{{ totalElements }} user(s) total</span>
          <div class="au-page-controls">
            <button class="au-btn au-btn-ghost" :disabled="page === 0" @click="prevPage">
              ← Previous
            </button>
            <span class="au-page-num">Page {{ page + 1 }} / {{ Math.max(totalPages, 1) }}</span>
            <button
              class="au-btn au-btn-ghost"
              :disabled="page >= totalPages - 1"
              @click="nextPage"
            >
              Next →
            </button>
          </div>
        </div>
      </template>
    </div>

    <!-- Activity log modal -->
    <div v-if="activityModal" class="au-modal-overlay" @click.self="closeActivity">
      <div class="au-modal" role="dialog" aria-modal="true">
        <header class="au-modal-header">
          <div>
            <h2>Activity Log</h2>
            <p>{{ activityModal.user.firstName }} {{ activityModal.user.lastName }} — {{ activityModal.user.email }}</p>
          </div>
          <button class="au-icon-btn au-modal-close" @click="closeActivity" title="Close">✕</button>
        </header>

        <div v-if="activityLoading" class="au-loading">Loading activity…</div>
        <template v-else>
          <ul v-if="activityModal.entries.length" class="au-activity-list">
            <li v-for="entry in activityModal.entries" :key="entry.id" class="au-activity-item">
              <span class="au-activity-action" :class="'act-' + entry.action.toLowerCase().replace('_', '-')">
                {{ ACTION_LABELS[entry.action] || entry.action }}
              </span>
              <span class="au-activity-details">{{ entry.details || '—' }}</span>
              <span class="au-activity-time">{{ formatDateTime(entry.createdAt) }}</span>
            </li>
          </ul>
          <p v-else class="au-empty">No activity recorded for this user.</p>

          <div v-if="activityModal.totalPages > 1" class="au-page-controls au-modal-paging">
            <button
              class="au-btn au-btn-ghost"
              :disabled="activityModal.page === 0"
              @click="loadActivityPage(activityModal.page - 1)"
            >
              ← Newer
            </button>
            <span class="au-page-num">
              Page {{ activityModal.page + 1 }} / {{ activityModal.totalPages }}
            </span>
            <button
              class="au-btn au-btn-ghost"
              :disabled="activityModal.page >= activityModal.totalPages - 1"
              @click="loadActivityPage(activityModal.page + 1)"
            >
              Older →
            </button>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>
