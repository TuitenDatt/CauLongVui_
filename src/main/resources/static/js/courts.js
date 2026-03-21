/**
 * courts.js — Court listing and booking functionality
 */

// ───────────────────────────────────────────────
// DOM references
// ───────────────────────────────────────────────
const courtsGrid      = document.getElementById('courts-grid');
const filterStatus    = document.getElementById('filter-status');
const searchInput     = document.getElementById('search-input');
const searchDate      = document.getElementById('search-date');
const bookingModal    = document.getElementById('booking-modal');
const bookingForm     = document.getElementById('booking-form');
const bookingCourtInfo = document.getElementById('booking-court-info');
const bookingCourtId  = document.getElementById('booking-court-id');
const closeModalBtn   = document.getElementById('close-modal');

let allCourts = [];

// ───────────────────────────────────────────────
// Load & render courts
// ───────────────────────────────────────────────
async function loadCourts() {
  renderSkeletons(courtsGrid, 6, '320px');
  try {
    allCourts = await fetchAPI('/courts');
    renderCourts(allCourts);
  } catch (err) {
    courtsGrid.innerHTML = `
      <div class="empty-state" style="grid-column:1/-1">
        <div class="empty-icon">⚠️</div>
        <h3>Không thể tải danh sách sân</h3>
        <p>${err.message}</p>
      </div>`;
    showToast('Lỗi tải danh sách sân. Kiểm tra kết nối API.', 'error');
  }
}

function renderCourts(courts) {
  if (!courts.length) {
    courtsGrid.innerHTML = `
      <div class="empty-state" style="grid-column:1/-1">
        <div class="empty-icon">🏸</div>
        <h3>Không có sân nào</h3>
        <p>Chưa có sân nào được thêm vào hệ thống.</p>
      </div>`;
    return;
  }
  courtsGrid.innerHTML = courts.map(court => {
    const status = courtStatus(court.status);
    const avatar = (court.name || 'S').substring(0, 2).toUpperCase();
    const canBook = court.status === 'AVAILABLE';
    
    const imgs = [
      'https://images.unsplash.com/photo-1626224583764-f87db24ac4ea?q=80&w=800&auto=format&fit=crop',
      'https://images.unsplash.com/photo-1613918108466-292b78a8ef95?q=80&w=800&auto=format&fit=crop',
      'https://images.unsplash.com/photo-1521537634581-0dced2fee2ef?q=80&w=800&auto=format&fit=crop'
    ];
    const courtImg = court.imageUrl || imgs[court.id % imgs.length];

    return `
    <div class="card court-card" id="court-${court.id}">
      <div class="card-img-wrapper">
        <img src="${courtImg}" alt="${escapeHtml(court.name)}">
        <div class="card-badge">
           ${status.label}
        </div>
        <div class="card-fav">💜</div>
      </div>
      
      <div class="card-content">
        <h3>${escapeHtml(court.name)}</h3>
        
        <div class="card-meta-inline">
          <div class="card-meta-item">
            <span class="icon">⭐</span> 4.7
          </div>
        </div>
        
        <div class="card-meta-item">
          <span class="icon">📍</span> ${escapeHtml(court.description || 'Sân tiêu chuẩn tầng 1')}
        </div>
        
        <div class="card-divider"></div>
        
        <div class="card-owner-row">
          <div class="owner-avatar">${avatar}</div>
          <div class="owner-info">
            Chủ sân: <strong>${escapeHtml(court.name)}</strong>
          </div>
        </div>
        
        <div class="card-action-row">
          <div class="price-box">
            <span class="price-label">Giá từ</span>
            <div class="price-value">
              ${formatCurrency(court.pricePerHour).replace('₫', '')}<span class="price-unit">₫/giờ</span>
            </div>
          </div>
          
          <button class="btn-book-premium ${canBook ? '' : 'disabled'}"
            onclick="goToSchedule(${court.id},'${escapeHtml(court.name)}',${court.pricePerHour},'${court.status}')"
            ${!canBook ? 'disabled' : ''}>
            Đặt sân &rarr;
          </button>
        </div>
      </div>
    </div>`;
  }).join('');
}

function courtStatus(status) {
  const map = {
    'AVAILABLE':   { label: '🟢 Còn trống' },
    'BOOKED':      { label: '🟡 Đã đặt' },
    'MAINTENANCE': { label: '🔴 Bảo trì' }
  };
  return map[status] || { label: status };
}

// ───────────────────────────────────────────────
// Filtering
// ───────────────────────────────────────────────
function applyFilters() {
  const status = filterStatus?.value || '';
  const search = searchInput?.value.trim().toLowerCase() || '';
  const date   = searchDate?.value || '';

  let filtered = allCourts;
  if (status) filtered = filtered.filter(c => c.status === status);
  if (search) filtered = filtered.filter(c =>
    c.name.toLowerCase().includes(search) ||
    (c.description || '').toLowerCase().includes(search)
  );
  // (Lưu ý: Logic lọc theo ngày hiện tại là client-side, 
  // trong thực tế có thể cần gọi API để kiểm tra slot trống theo ngày)
  
  renderSkeletons(courtsGrid, 3, '400px'); // Hiệu ứng chờ khi lọc
  setTimeout(() => renderCourts(filtered), 300);
}

filterStatus?.addEventListener('change', applyFilters);
searchInput?.addEventListener('input', applyFilters);
searchDate?.addEventListener('change', applyFilters);

// ───────────────────────────────────────────────
// Navigate to schedule page
// ───────────────────────────────────────────────
function goToSchedule(courtId, courtName, pricePerHour, status) {
  if (status !== 'AVAILABLE') { showToast('Sân này hiện không khả dụng', 'warning'); return; }
  localStorage.setItem('bk_courtId', courtId);
  localStorage.setItem('bk_courtName', courtName);
  localStorage.setItem('bk_pricePerHour', pricePerHour);
  window.location.href = '/schedule.html?courtId=' + courtId + '&courtName=' + encodeURIComponent(courtName) + '&price=' + pricePerHour;
}

// ───────────────────────────────────────────────
// Booking Modal (legacy - kept for compatibility)
// ───────────────────────────────────────────────
function openBooking(courtId, courtName, pricePerHour, status) {
  goToSchedule(courtId, courtName, pricePerHour, status);
}

closeModalBtn?.addEventListener('click', () => bookingModal.classList.remove('open'));
bookingModal?.addEventListener('click', e => {
  if (e.target === bookingModal) bookingModal.classList.remove('open');
});

// ───────────────────────────────────────────────
// Submit booking
// ───────────────────────────────────────────────
bookingForm?.addEventListener('submit', async (e) => {
  e.preventDefault();
  const submitBtn = bookingForm.querySelector('[type="submit"]');
  submitBtn.disabled = true;
  submitBtn.textContent = 'Đang xử lý...';

  const payload = {
    courtId:      parseInt(bookingCourtId.value),
    customerName: document.getElementById('customer-name').value.trim(),
    customerPhone: document.getElementById('customer-phone').value.trim(),
    bookingDate:  document.getElementById('booking-date').value,
    startTime:    document.getElementById('start-time').value,
    endTime:      document.getElementById('end-time').value,
  };

  try {
    const result = await fetchAPI('/bookings', {
      method: 'POST',
      body: JSON.stringify(payload),
    });
    bookingModal.classList.remove('open');
    bookingForm.reset();
    showToast(`✅ Đặt sân thành công! Tổng tiền: ${formatCurrency(result.totalPrice)}`, 'success', 6000);
    await loadCourts();
  } catch (err) {
    showToast(err.message || 'Đặt sân thất bại. Vui lòng thử lại.', 'error');
  } finally {
    submitBtn.disabled = false;
    submitBtn.textContent = '📅 Xác nhận đặt sân';
  }
});

// ───────────────────────────────────────────────
// Escape HTML helper
// ───────────────────────────────────────────────
function escapeHtml(str) {
  const div = document.createElement('div');
  div.appendChild(document.createTextNode(str || ''));
  return div.innerHTML;
}

// ───────────────────────────────────────────────
// Init
// ───────────────────────────────────────────────
document.addEventListener('DOMContentLoaded', () => {
  loadCourts();
  // Set default date
  const dateInput = document.getElementById('search-date');
  if (dateInput) {
    dateInput.value = new Date().toISOString().split('T')[0];
  }
});
