/**
 * products.js — Product listing for the equipment store
 */

const productsGrid = document.getElementById('products-grid');
const searchInput  = document.getElementById('search-input');
const categoryFilter = document.getElementById('filter-category');

let allProducts = [];

const categoryIcons = {
  'Vợt': '🏸', 'Cầu': '🪶', 'Giày': '👟',
  'Túi': '🎒', 'Quần áo': '👕', 'Phụ kiện': '🔧',
};

function getIcon(category, name) {
  if (category && categoryIcons[category]) return categoryIcons[category];
  const n = (name || '').toLowerCase();
  if (n.includes('vợt')) return '🏸';
  if (n.includes('cầu')) return '🪶';
  if (n.includes('giày') || n.includes('dép')) return '👟';
  if (n.includes('túi')) return '🎒';
  if (n.includes('áo') || n.includes('quần')) return '👕';
  return '🛒';
}

// ───────────────────────────────────────────────
// Load & render products
// ───────────────────────────────────────────────
async function loadProducts() {
  renderSkeletons(productsGrid, 8, '300px');
  try {
    allProducts = await fetchAPI('/products');
    populateCategoryFilter(allProducts);
    renderProducts(allProducts);
  } catch (err) {
    productsGrid.innerHTML = `
      <div class="empty-state" style="grid-column:1/-1">
        <div class="empty-icon">⚠️</div>
        <h3>Không thể tải sản phẩm</h3>
        <p>${err.message}</p>
      </div>`;
    showToast('Lỗi tải sản phẩm. Kiểm tra kết nối API.', 'error');
  }
}

function renderProducts(products) {
  if (!products.length) {
    productsGrid.innerHTML = `
      <div class="empty-state" style="grid-column:1/-1">
        <div class="empty-icon">🛒</div>
        <h3>Không tìm thấy sản phẩm</h3>
        <p>Thử thay đổi từ khóa tìm kiếm.</p>
      </div>`;
    return;
  }
  productsGrid.innerHTML = products.map(p => `
    <div class="card product-card">
      <div class="product-img">${getIcon(p.category, p.name)}</div>
      <div class="product-info">
        <p class="category">${escapeHtml(p.category || 'Dụng cụ cầu lông')}</p>
        <h3>${escapeHtml(p.name)}</h3>
        <p class="desc">${escapeHtml(p.description || '')}</p>
        <div class="price-row">
          <span class="price">${formatCurrency(p.price)}</span>
          <span class="stock">${stockLabel(p.stockQuantity)}</span>
        </div>
      </div>
      <div class="card-footer">
        <button class="btn btn-primary btn-sm" style="flex:1"
          onclick="addToCart(${p.id}, '${escapeHtml(p.name)}', ${p.price})"
          ${p.stockQuantity <= 0 ? 'disabled' : ''}>
          🛒 ${p.stockQuantity > 0 ? 'Thêm vào giỏ' : 'Hết hàng'}
        </button>
      </div>
    </div>`).join('');
}

function stockLabel(qty) {
  if (qty <= 0) return `<span class="badge badge-danger">Hết hàng</span>`;
  if (qty <= 5) return `<span class="badge badge-warning">Còn ${qty}</span>`;
  return `<span class="badge badge-success">Còn hàng</span>`;
}

// ───────────────────────────────────────────────
// Category filter
// ───────────────────────────────────────────────
function populateCategoryFilter(products) {
  if (!categoryFilter) return;
  const categories = [...new Set(products.map(p => p.category).filter(Boolean))];
  categoryFilter.innerHTML = '<option value="">Tất cả danh mục</option>'
    + categories.map(c => `<option value="${escapeHtml(c)}">${escapeHtml(c)}</option>`).join('');
}

function applyFilters() {
  const search   = searchInput?.value.trim().toLowerCase() || '';
  const category = categoryFilter?.value || '';
  let filtered = allProducts;
  if (category) filtered = filtered.filter(p => p.category === category);
  if (search)   filtered = filtered.filter(p =>
    p.name.toLowerCase().includes(search) ||
    (p.description || '').toLowerCase().includes(search)
  );
  renderProducts(filtered);
}

searchInput?.addEventListener('input', applyFilters);
categoryFilter?.addEventListener('change', applyFilters);

// ───────────────────────────────────────────────
// Add to cart (simple toast demo — no backend cart yet)
// ───────────────────────────────────────────────
function addToCart(id, name, price) {
  // ── Kiểm tra quyền: GUEST phải đăng nhập
  if (!requireAuth()) return;

  showToast(`🛒 Đã thêm "<strong>${escapeHtml(name)}</strong>" vào giỏ hàng`, 'success');
}

function escapeHtml(str) {
  const div = document.createElement('div');
  div.appendChild(document.createTextNode(str || ''));
  return div.innerHTML;
}

document.addEventListener('DOMContentLoaded', loadProducts);
