/**
 * app.js — Shared utilities for CauLongVui front-end
 */

const API_BASE = '/api';

// ───────────────────────────────────────────────
// Generic fetch wrapper
// ───────────────────────────────────────────────
async function fetchAPI(endpoint, options = {}) {
  const defaultOptions = {
    headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
    ...options,
  };
  try {
    const res = await fetch(`${API_BASE}${endpoint}`, defaultOptions);
    const json = await res.json();
    if (!res.ok || !json.success) {
      throw new Error(json.message || `HTTP ${res.status}`);
    }
    return json.data;
  } catch (err) {
    console.error(`[API Error] ${endpoint}:`, err.message);
    throw err;
  }
}

// ───────────────────────────────────────────────
// Formatting helpers
// ───────────────────────────────────────────────
function formatCurrency(amount) {
  if (amount == null) return '—';
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency', currency: 'VND', maximumFractionDigits: 0
  }).format(amount);
}

function formatDate(dateStr) {
  if (!dateStr) return '—';
  const [y, m, d] = dateStr.split('-');
  return `${d}/${m}/${y}`;
}

function formatTime(timeStr) {
  if (!timeStr) return '—';
  const [h, m] = timeStr.split(':');
  return `${h}:${m}`;
}

// ───────────────────────────────────────────────
// Toast notifications
// ───────────────────────────────────────────────
const toastContainer = (() => {
  let el = document.getElementById('toast-container');
  if (!el) {
    el = document.createElement('div');
    el.id = 'toast-container';
    document.body.appendChild(el);
  }
  return el;
})();

function showToast(message, type = 'success', duration = 4000) {
  const icons = { success: '✅', error: '❌', warning: '⚠️', info: 'ℹ️' };
  const toast = document.createElement('div');
  toast.className = `toast ${type}`;
  toast.innerHTML = `
    <span class="toast-icon">${icons[type] || '💬'}</span>
    <span class="toast-msg">${message}</span>`;
  toastContainer.appendChild(toast);
  setTimeout(() => {
    toast.style.opacity = '0';
    toast.style.transform = 'translateX(100%)';
    toast.style.transition = 'all 0.3s ease';
    setTimeout(() => toast.remove(), 300);
  }, duration);
}

// ───────────────────────────────────────────────
// Status / badge helpers
// ───────────────────────────────────────────────
function courtStatusBadge(status) {
  const map = {
    AVAILABLE:   { cls: 'badge-success', label: '🟢 Còn trống' },
    BOOKED:      { cls: 'badge-warning', label: '🟡 Đã đặt' },
    MAINTENANCE: { cls: 'badge-danger',  label: '🔴 Bảo trì' },
  };
  const s = map[status] || { cls: 'badge-muted', label: status };
  return `<span class="badge ${s.cls}">${s.label}</span>`;
}

function bookingStatusBadge(status) {
  const map = {
    PENDING:   { cls: 'badge-warning', label: '⏳ Chờ xác nhận' },
    CONFIRMED: { cls: 'badge-success', label: '✅ Đã xác nhận' },
    CANCELLED: { cls: 'badge-danger',  label: '❌ Đã hủy' },
    COMPLETED: { cls: 'badge-muted',   label: '🏆 Hoàn thành' },
  };
  const s = map[status] || { cls: 'badge-muted', label: status };
  return `<span class="badge ${s.cls}">${s.label}</span>`;
}

// ───────────────────────────────────────────────
// Skeleton loader helper
// ───────────────────────────────────────────────
function renderSkeletons(container, count = 6, height = '280px') {
  container.innerHTML = Array.from({ length: count }, () =>
    `<div class="skeleton" style="height:${height};border-radius:14px;"></div>`
  ).join('');
}

// ───────────────────────────────────────────────
// Dynamic Component Loading
// ───────────────────────────────────────────────
async function loadComponent(id, url) {
  const container = document.getElementById(id);
  if (!container) return;
  try {
    const res = await fetch(url);
    if (!res.ok) throw new Error(`Could not load ${url}`);
    const html = await res.text();
    container.innerHTML = html;
    updateActiveNavLink();
    
    // Khởi tạo các sự kiện cho header nếu có
    if (id === 'header-placeholder') {
      initHeaderEvents();
    }
  } catch (err) {
    console.error(`[Load Error] ${url}:`, err.message);
  }
}

function updateActiveNavLink() {
  const path = location.pathname.replace(/\/$/, '') || '/index.html';
  document.querySelectorAll('.nav-links a').forEach(a => {
    const href = a.getAttribute('href');
    if (!href) return;
    const cleanHref = href.replace(/\/$/, '');
    
    // Logic so khớp đường dẫn
    const isActive = (path.endsWith(cleanHref) && cleanHref !== '') || 
                     (path === '/' && (cleanHref === '/' || cleanHref === '/index.html')) ||
                     (path.endsWith('index.html') && (cleanHref === '/' || cleanHref === '/index.html'));
    
    a.classList.toggle('active', isActive);
  });
}

function initHeaderEvents() {
  const nav = document.getElementById('mainNav');
  if (nav) {
    window.addEventListener('scroll', () => {
      nav.classList.toggle('scrolled', window.scrollY > 10);
    });
  }

  // ── Hiển thị trạng thái đăng nhập trong header
  const user      = getCurrentUser();
  const navActions = document.getElementById('navActions');
  if (!navActions) return;

  if (user) {
    // Đã đăng nhập — hiển thị avatar + tên + dropdown
    navActions.innerHTML = `
      <div class="nav-user-menu" id="navUserMenu">
        <button class="nav-user-btn" onclick="toggleUserMenu()" id="navUserBtn">
          <div class="nav-user-avatar">${(user.fullName || 'U').charAt(0).toUpperCase()}</div>
          <span class="nav-user-name">${(user.fullName || user.email).split(' ').slice(-1)[0]}</span>
          <span style="font-size:11px;color:var(--muted)">▾</span>
        </button>
        <div class="nav-user-dropdown" id="navUserDropdown" style="display:none;">
          <div class="dropdown-header">
            <div class="dropdown-name">${user.fullName}</div>
            <div class="dropdown-role">${roleLabel(user.role)}</div>
          </div>
          <a href="/profile.html" class="dropdown-item">👤 Tài khoản của tôi</a>
          ${user.role === 'ADMIN' ? '<a href="/admin/admin.html" class="dropdown-item">⚙️ Quản trị hệ thống</a>' : ''}
          ${(user.role === 'ADMIN' || user.role === 'STAFF') ? '<a href="/admin/court-management.html" class="dropdown-item">📋 Quản lý sân</a>' : ''}
          <a href="#" class="dropdown-item dropdown-logout" id="logoutBtn">🚪 Đăng xuất</a>
        </div>
      </div>
      ${(user.role === 'ADMIN' || user.role === 'STAFF') ? `<a href="/admin/court-management.html" class="btn-manage">⚙️ Quản lý</a>` : ''}
      <a href="/courts.html" class="btn-nav-cta">Đặt sân ngay</a>
    `;

    document.getElementById('logoutBtn').addEventListener('click', (e) => {
      e.preventDefault();
      if (confirm('Bạn có chắc chắn muốn đăng xuất?')) {
        logout();
      }
    });

    // Đóng dropdown khi click ngoài
    document.addEventListener('click', (e) => {
      if (!e.target.closest('#navUserMenu')) {
        const dd = document.getElementById('navUserDropdown');
        if (dd) dd.style.display = 'none';
      }
    });

  } else {
    // Chưa đăng nhập — hiển thị Đăng nhập + Đăng ký
    navActions.innerHTML = `
      <a href="/auth/login.html" class="btn-login">Đăng nhập</a>
      <a href="/auth/register.html" class="btn-nav-cta">Đăng ký</a>
    `;
  }
}

// ── Toggle user dropdown menu
function toggleUserMenu() {
  const dd = document.getElementById('navUserDropdown');
  if (dd) dd.style.display = dd.style.display === 'none' ? 'block' : 'none';
}

// ── Auth Helpers
function getCurrentUser() {
  try {
    return JSON.parse(localStorage.getItem('currentUser') || 'null');
  } catch {
    return null;
  }
}

function hasRole(...roles) {
  const user = getCurrentUser();
  if (!user) return false;
  return roles.includes(user.role);
}

function requireAuth(redirectUrl = null) {
  const user = getCurrentUser();
  if (!user) {
    const currentPage = window.location.pathname;
    window.location.href = `/auth/login.html?redirect=${encodeURIComponent(redirectUrl || currentPage)}`;
    return false;
  }
  return true;
}

function logout() {
  localStorage.removeItem('currentUser');
  window.location.href = '/auth/login.html';
}

function roleLabel(role) {
  const map = { ADMIN: '👑 Quản trị viên', STAFF: '🛠️ Nhân viên', CUSTOMER: '🧍 Khách hàng' };
  return map[role] || role;
}

// Khởi tạo header và footer khi trang tải xong
document.addEventListener('DOMContentLoaded', () => {
  loadComponent('header-placeholder', '/components/header.html');
  loadComponent('footer-placeholder', '/components/footer.html');
  updateActiveNavLink();
});
