/**
 * products.js — Product listing with role-based actions
 */

const productsGrid    = document.getElementById('products-grid');
const searchInput     = document.getElementById('search-input');
const categoryFilter  = document.getElementById('filter-category');

let allProducts = [];

const categoryIcons = {
  'Vợt': '🏸', 'Cầu': '🪶', 'Cầu lông': '🪶', 'Giày': '👟',
  'Túi': '🎒', 'Quần áo': '👕', 'Phụ kiện': '🔧',
  'Nước uống': '🧋', 'Đồ ăn': '🍜',
};

function getIcon(category, name) {
  if (category && categoryIcons[category]) return categoryIcons[category];
  const n = (name || '').toLowerCase();
  if (n.includes('vợt'))                       return '🏸';
  if (n.includes('cầu'))                       return '🪶';
  if (n.includes('giày') || n.includes('dép')) return '👟';
  if (n.includes('túi'))                       return '🎒';
  if (n.includes('áo') || n.includes('quần')) return '👕';
  if (n.includes('trà') || n.includes('nước')) return '🧋';
  if (n.includes('mì') || n.includes('xúc xích') || n.includes('trứng')) return '🍜';
  return '🛒';
}

// ─────────────────────────────────────────────────
// Load & render
// ─────────────────────────────────────────────────
async function loadProducts() {
  renderSkeletons(productsGrid, 8, '300px');
  try {
    allProducts = await fetchAPI('/products');
    populateCategoryFilter(allProducts);
    renderProducts(allProducts);

    // Nút Thêm sản phẩm cho ADMIN ở đầu trang
    const addBar = document.getElementById('admin-add-bar');
    if (addBar) {
      const user = getCurrentUser();
      addBar.style.display = (user && user.role === 'ADMIN') ? 'flex' : 'none';
    }
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

  const user    = getCurrentUser();
  const isAdmin = user && user.role === 'ADMIN';

  productsGrid.innerHTML = products.map(p => {
    const icon = getIcon(p.category, p.name);
    const safeName = escapeHtml(p.name);

    // Hình ảnh hiển thị: ưu tiên ảnh upload, nếu không thì dùng icon bù nhìn
    const imageContent = p.imageUrl 
      ? `<img src="${escapeHtml(p.imageUrl)}" alt="${safeName}" onerror="this.parentElement.innerHTML='<span style=\\'font-size:3rem\\'>${icon}</span>'">`
      : `<div style="width:100%;height:100%;display:flex;align-items:center;justify-content:center;background:var(--green-pale);font-size:48px;color:var(--green);">${icon}</div>`;

    return `
    <div class="card court-card product-card">
      <div class="card-img-wrapper" style="height: 180px;">
        ${imageContent}
        <div class="card-badge">
           ${isAdmin ? '⚙️ Admin' : stockLabel(p.stockQuantity)}
        </div>
        ${isAdmin 
          ? `<div class="card-fav" onclick="openShopEditModal(${p.id})" title="Sửa sản phẩm" style="font-size:16px;">✏️</div>` 
          : `<div class="card-fav" onclick="openDetailModal(${p.id})" title="Xem chi tiết" style="font-size:16px;">👁️</div>`
        }
      </div>
      
      <div class="card-content">
        <h3>${safeName}</h3>
        
        <div class="card-meta-inline" style="margin-bottom: 2px;">
          <div class="card-meta-item">
            <span class="icon">🏷️</span> ${escapeHtml(p.category || 'Dụng cụ')}
          </div>
        </div>
        
        <div class="card-meta-item" style="font-weight:400; font-size:13px; flex:1; align-items:flex-start; overflow:hidden; display:-webkit-box; -webkit-line-clamp:2; -webkit-box-orient:vertical;">
          ${escapeHtml(p.description || '')}
        </div>
        
        <div class="card-divider"></div>
        
        <div class="card-action-row" style="margin-top: auto; padding-top: 8px;">
          <div class="price-box">
            <span class="price-label">Giá bán</span>
            <div class="price-value" style="font-size: 20px;">
              ${formatCurrency(p.price).replace('₫', '').trim()}<span class="price-unit">₫</span>
            </div>
          </div>
          
          <button class="btn-book-premium ${p.stockQuantity <= 0 ? 'disabled' : ''}" style="padding: 8px 14px; font-size: 13px;"
            onclick="addToCart(${p.id}, '${safeName}', ${p.price})"
            ${p.stockQuantity <= 0 ? 'disabled' : ''}>
            🛒 ${p.stockQuantity > 0 ? 'Thêm vào giỏ' : 'Hết hàng'}
          </button>
        </div>
        
        ${isAdmin ? `
        <div style="display:flex; gap:8px; margin-top:12px;">
           <button class="btn btn-danger-sm btn-sm" style="flex:1; border-radius:8px;" onclick="openShopDeleteConfirm(${p.id}, '${safeName}')">🗑️ Xóa sản phẩm</button>
        </div>
        ` : ''}
      </div>
    </div>`;
  }).join('');
}

function stockLabel(qty) {
  if (qty <= 0) return `<span style="color:var(--red);">🔴 Hết hàng</span>`;
  if (qty <= 5) return `<span style="color:#b8860b;">🟡 Còn ${qty}</span>`;
  return `<span style="color:var(--green);">🟢 Còn hàng</span>`;
}

// ─────────────────────────────────────────────────
// Filters
// ─────────────────────────────────────────────────
function populateCategoryFilter(products) {
  if (!categoryFilter) return;
  const categories = [...new Set(products.map(p => p.category).filter(Boolean))];
  categoryFilter.innerHTML = '<option value="">Tất cả danh mục</option>'
    + categories.map(c => `<option value="${escapeHtml(c)}">${escapeHtml(c)}</option>`).join('');
}

function applyFilters() {
  const search   = searchInput?.value.trim().toLowerCase() || '';
  const category = categoryFilter?.value || '';
  let filtered   = allProducts;
  if (category) filtered = filtered.filter(p => p.category === category);
  if (search)   filtered = filtered.filter(p =>
    p.name.toLowerCase().includes(search) ||
    (p.description || '').toLowerCase().includes(search)
  );
  renderProducts(filtered);
}

searchInput?.addEventListener('input', applyFilters);
categoryFilter?.addEventListener('change', applyFilters);

// ─────────────────────────────────────────────────
// Modal Xem chi tiết (User)
// ─────────────────────────────────────────────────
function openDetailModal(id) {
  const p = allProducts.find(x => x.id === id);
  if (!p) return;
  const icon   = getIcon(p.category, p.name);
  const imgHtml = p.imageUrl
    ? `<img src="${escapeHtml(p.imageUrl)}" alt="${escapeHtml(p.name)}"
            style="width:100%;max-height:240px;object-fit:cover;border-radius:12px;margin-bottom:16px;"
            onerror="this.remove()" />`
    : `<div style="font-size:64px;text-align:center;margin-bottom:16px;">${icon}</div>`;

  document.getElementById('detailBody').innerHTML = `
    ${imgHtml}
    <p style="font-size:12px;font-weight:700;color:var(--green);text-transform:uppercase;letter-spacing:1px;margin-bottom:4px;">
      ${escapeHtml(p.category || '—')}
    </p>
    <h2 style="font-size:20px;font-weight:800;margin-bottom:10px;">${escapeHtml(p.name)}</h2>
    <p style="color:var(--muted);font-size:14px;line-height:1.7;margin-bottom:16px;">
      ${escapeHtml(p.description || 'Chưa có mô tả sản phẩm.')}
    </p>
    <div style="display:flex;align-items:center;justify-content:space-between;padding:14px 0;border-top:1px solid var(--border);">
      <span style="font-size:26px;font-weight:900;color:var(--green);">${formatCurrency(p.price)}</span>
      ${stockLabel(p.stockQuantity)}
    </div>`;

  const cartBtn = document.getElementById('detailCartBtn');
  cartBtn.disabled    = p.stockQuantity <= 0;
  cartBtn.textContent = p.stockQuantity > 0 ? '🛒 Thêm vào giỏ hàng' : '❌ Hết hàng';
  cartBtn.onclick = () => { addToCart(p.id, p.name, p.price); closeModal('detailModal'); };

  openModal('detailModal');
}

// ─────────────────────────────────────────────────
// Modal Sửa / Thêm sản phẩm (Admin)
// ─────────────────────────────────────────────────
function openShopAddModal() {
  document.getElementById('shopEditModalTitle').textContent = '➕ Thêm sản phẩm mới';
  ['shopEditId','shopFName','shopFPrice','shopFStock','shopFDesc','shopFImageUrl'].forEach(id => {
    document.getElementById(id).value = '';
  });
  document.getElementById('shopFCategory').value = '';
  shopResetImgPreview();
  openModal('shopEditModal');
}

function openShopEditModal(id) {
  const p = allProducts.find(x => x.id === id);
  if (!p) return;
  document.getElementById('shopEditModalTitle').textContent = '✏️ Chỉnh sửa sản phẩm';
  document.getElementById('shopEditId').value    = p.id;
  document.getElementById('shopFName').value     = p.name          || '';
  document.getElementById('shopFPrice').value    = p.price         != null ? p.price : '';
  document.getElementById('shopFStock').value    = p.stockQuantity != null ? p.stockQuantity : '';
  document.getElementById('shopFCategory').value = p.category      || '';
  document.getElementById('shopFDesc').value     = p.description   || '';
  document.getElementById('shopFImageUrl').value = p.imageUrl      || '';
  shopPreviewImage(p.imageUrl || '');
  openModal('shopEditModal');
}

async function saveShopProduct() {
  const name     = document.getElementById('shopFName').value.trim();
  const price    = parseFloat(document.getElementById('shopFPrice').value);
  const stock    = parseInt(document.getElementById('shopFStock').value);
  const category = document.getElementById('shopFCategory').value;
  const desc     = document.getElementById('shopFDesc').value.trim();
  const imageUrl = document.getElementById('shopFImageUrl').value.trim();
  const editId   = document.getElementById('shopEditId').value;

  if (!name)                     return showToast('Vui lòng nhập tên sản phẩm!', 'warning');
  if (isNaN(price) || price < 0) return showToast('Giá bán không hợp lệ!', 'warning');
  if (isNaN(stock) || stock < 0) return showToast('Số lượng không hợp lệ!', 'warning');
  if (!category)                 return showToast('Vui lòng chọn danh mục!', 'warning');

  const payload = { name, price, stockQuantity: stock, category,
                    description: desc, imageUrl: imageUrl || null };
  const btn = document.getElementById('shopSaveBtn');
  btn.disabled = true; btn.innerHTML = '⏳ Đang lưu...';

  try {
    if (editId) {
      await fetchAPI(`/products/${editId}`, { method: 'PUT', body: JSON.stringify(payload) });
      showToast('✅ Cập nhật sản phẩm thành công!', 'success');
    } else {
      await fetchAPI('/products', { method: 'POST', body: JSON.stringify(payload) });
      showToast('✅ Thêm sản phẩm thành công!', 'success');
    }
    closeModal('shopEditModal');
    await loadProducts();
  } catch (err) {
    showToast('❌ Lỗi: ' + err.message, 'error');
  } finally {
    btn.disabled = false; btn.innerHTML = '💾 Lưu sản phẩm';
  }
}

function shopPreviewImage(url) {
  const wrap = document.getElementById('shopImgPreview');
  if (!url) { shopResetImgPreview(); return; }
  wrap.innerHTML = `<img src="${url}" style="width:100%;max-height:140px;object-fit:cover;border-radius:8px;"
    onerror="this.parentElement.innerHTML='<p style=\\'color:#ef4444;font-size:12px;text-align:center;\\'>⚠️ Không tải được ảnh</p>'" />`;
}
function shopResetImgPreview() {
  document.getElementById('shopImgPreview').innerHTML =
    `<p style="color:var(--muted);font-size:12px;text-align:center;padding:20px 0;">🖼️ Nhập URL để xem trước</p>`;
}

// ─────────────────────────────────────────────────
// Modal Xóa sản phẩm (Admin)
// ─────────────────────────────────────────────────
let shopPendingDeleteId = null;

function openShopDeleteConfirm(id, name) {
  shopPendingDeleteId = id;
  document.getElementById('shopConfirmName').textContent = name;
  openModal('shopConfirmModal');
}

async function executeShopDelete() {
  if (!shopPendingDeleteId) return;
  const btn = document.getElementById('shopConfirmDeleteBtn');
  btn.disabled = true; btn.innerHTML = '⏳ Đang xóa...';
  try {
    await fetchAPI(`/products/${shopPendingDeleteId}`, { method: 'DELETE' });
    showToast('🗑️ Đã xóa sản phẩm thành công!', 'success');
    closeModal('shopConfirmModal');
    await loadProducts();
  } catch (err) {
    showToast('❌ Lỗi: ' + err.message, 'error');
  } finally {
    btn.disabled = false; btn.innerHTML = '🗑️ Xóa vĩnh viễn';
    shopPendingDeleteId = null;
  }
}

// ─────────────────────────────────────────────────
// Add to cart — lưu localStorage + cập nhật badge
// ─────────────────────────────────────────────────
function addToCart(id, name, price) {
  if (!requireAuth()) return;

  const product = allProducts.find(p => p.id === id);
  try {
    const cart = JSON.parse(localStorage.getItem('cart') || '[]');
    const existing = cart.find(i => i.id === id);
    if (existing) {
      existing.qty += 1;
    } else {
      cart.push({
        id,
        name,
        price,
        qty: 1,
        imageUrl:  product ? product.imageUrl  : null,
        category:  product ? product.category  : null,
      });
    }
    localStorage.setItem('cart', JSON.stringify(cart));
    updateCartBadge();
    showToast(`🛒 Đã thêm “<strong>${escapeHtml(name)}</strong>” vào giỏ hàng`, 'success');
  } catch (err) {
    showToast('❗ Lỗi giỏ hàng: ' + err.message, 'error');
  }
}

// ─────────────────────────────────────────────────
// Upload ảnh sản phẩm
// ─────────────────────────────────────────────────
async function uploadProductImage(file) {
  if (!file) throw new Error('Không có file nào được chọn.');

  const form = new FormData();
  form.append('file', file);
  const res  = await fetch('/api/upload', { method: 'POST', body: form });
  const json = await res.json();
  if (!res.ok || !json.success) throw new Error(json.message || 'Upload thất bại');
  return json.data; // Return the URL of the uploaded image
}

async function shopUploadProductImage() {
  const input = document.getElementById('shopFileInput');
  const file  = input?.files?.[0];
  if (!file) return;

  const uploadBtn = document.getElementById('shopUploadBtn');
  uploadBtn.disabled = true;
  uploadBtn.textContent = '⏳...';

  try {
    const form = new FormData();
    form.append('file', file);
    const res  = await fetch('/api/upload', { method: 'POST', body: form });
    const json = await res.json();
    if (!res.ok || !json.success) throw new Error(json.message || 'Upload thất bại');

    document.getElementById('shopFImageUrl').value = json.data;
    shopPreviewImage(json.data);
    showToast('✅ Upload ảnh thành công!', 'success');
  } catch (err) {
    showToast('❌ Lỗi upload: ' + err.message, 'error');
  } finally {
    uploadBtn.disabled = false;
    uploadBtn.textContent = '📷 Chọn ảnh';
    input.value = '';
  }
}

// ─────────────────────────────────────────────────
// Modal helpers
// ─────────────────────────────────────────────────
function openModal(id) {
  document.getElementById(id).classList.add('open');
  document.body.style.overflow = 'hidden';
}
function closeModal(id) {
  document.getElementById(id).classList.remove('open');
  document.body.style.overflow = '';
}

// ─────────────────────────────────────────────────
// Helper
// ─────────────────────────────────────────────────
function escapeHtml(str) {
  const div = document.createElement('div');
  div.appendChild(document.createTextNode(str || ''));
  return div.innerHTML;
}

document.addEventListener('DOMContentLoaded', () => {
  loadProducts();
  document.querySelectorAll('.products-modal-backdrop').forEach(bd => {
    bd.addEventListener('click', e => { if (e.target === bd) closeModal(bd.id); });
  });
  document.addEventListener('keydown', e => {
    if (e.key === 'Escape')
      document.querySelectorAll('.products-modal-backdrop.open').forEach(m => closeModal(m.id));
  });
});
