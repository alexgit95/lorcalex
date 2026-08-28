// ─── API ─────────────────────────────────────────────────────────────────────

function getToken() {
  return localStorage.getItem('token') || sessionStorage.getItem('token');
}

function removeToken() {
  localStorage.removeItem('token');
  sessionStorage.removeItem('token');
  localStorage.removeItem('username');
}

async function apiFetch(path, options = {}) {
  const token = getToken();
  const headers = { 'Content-Type': 'application/json', ...options.headers };
  if (token) headers.Authorization = `Bearer ${token}`;

  const response = await fetch('/api' + path, { ...options, headers });

  if (response.status === 401) {
    removeToken();
    navigate('login');
    throw new Error('Non autorisé');
  }

  if (!response.ok) {
    let msg = `HTTP ${response.status}`;
    let rootCause;
    try { const j = await response.json(); msg = j.message || msg; rootCause = j.rootCause; } catch { /* ignore */ }
    const error = new Error(msg);
    error.rootCause = rootCause;
    throw error;
  }

  const ct = response.headers.get('content-type') || '';
  if (ct.includes('application/json')) return response.json();
  return response.text();
}

const api = {
  login: (username, password, rememberMe = false) =>
    apiFetch('/auth/login', { method: 'POST', body: JSON.stringify({ username, password, rememberMe }) }),

  getEditions: () => apiFetch('/editions'),

  getCards: (editionId) => {
    const params = new URLSearchParams();
    if (editionId) params.set('editionId', editionId);
    const qs = params.toString();
    return apiFetch('/cards' + (qs ? '?' + qs : ''));
  },

  lookupCard: (number, editionId) => {
    const params = new URLSearchParams({ number });
    if (editionId) params.set('editionId', String(editionId));
    return apiFetch('/cards/lookup?' + params);
  },

  getFingerprints: () => apiFetch('/cards/fingerprints'),

  setWanted: (cardId, wanted) =>
    apiFetch(`/cards/${cardId}/wanted`, { method: 'PATCH', body: JSON.stringify({ wanted }) }),

  addToCollection: (cardId, quantity = 1, foilQuantity = 0) =>
    apiFetch('/collection', { method: 'POST', body: JSON.stringify({ cardId, quantity, foilQuantity }) }),

  updateQuantity: (cardId, quantity, foilQuantity = undefined) =>
    apiFetch(`/collection/${cardId}`, { method: 'PUT', body: JSON.stringify(
      foilQuantity !== undefined ? { quantity, foilQuantity } : { quantity }
    ) }),

  removeFromCollection: (cardId) =>
    apiFetch(`/collection/${cardId}`, { method: 'DELETE' }),

  getStatistics: () => apiFetch('/statistics'),

  getPricingInsights: () => apiFetch('/pricing/insights'),
  removeCardPrice: (cardId) => apiFetch(`/pricing/cards/${cardId}/price`, { method: 'DELETE' }),
  getTrend: () => apiFetch('/pricing/trend'),
  getEditionDeltas: () => apiFetch('/pricing/edition-deltas'),
  recomputeValue: () => apiFetch('/pricing/recompute-value', { method: 'POST' }),

  getSettings: () => apiFetch('/admin/settings'),

  updateSetting: (key, value) =>
    apiFetch(`/admin/settings/${key}`, { method: 'PUT', body: JSON.stringify({ value }) }),

  syncFromUrl: (url) =>
    apiFetch('/admin/sync/url', { method: 'POST', body: JSON.stringify({ url }) }),

  syncFromFile: (file) => {
    const formData = new FormData();
    formData.append('file', file);
    const token = getToken();
    const headers = {};
    if (token) headers.Authorization = `Bearer ${token}`;
    return fetch('/api/admin/sync/file', { method: 'POST', headers, body: formData })
      .then(async r => {
        const json = await r.json();
        if (!r.ok) throw new Error(json.message || `HTTP ${r.status}`);
        return json;
      });
  },

  getLorcaJsonUrl: () => apiFetch('/admin/lorcajson-url'),

  getProgress: () => apiFetch('/admin/progress'),

  getPricingStatus: () => apiFetch('/admin/pricing/status'),

  runPricingSync: (maxAttempts) => apiFetch('/admin/pricing/run', {
    method: 'POST',
    body: JSON.stringify(maxAttempts === undefined ? {} : { maxAttempts })
  }),

  simulatePricingImport: (json) => apiFetch('/admin/pricing/simulate-import', {
    method: 'POST',
    body: JSON.stringify({ json })
  }),

  computeHashes: () => apiFetch('/admin/compute-hashes', { method: 'POST' }),

  fullBackup: () => apiFetch('/admin/backup'),
  fullRestore: (data) => apiFetch('/admin/restore', { method: 'POST', body: JSON.stringify(data) }),

  getRecentCards: (limit = 20) => apiFetch('/collection/recent?limit=' + limit),

  listApiKeys: () => apiFetch('/admin/apikeys'),
  createApiKey: (name, validityDays) =>
    apiFetch('/admin/apikeys', { method: 'POST', body: JSON.stringify({ name, validityDays }) }),
  deleteApiKey: (id) => apiFetch(`/admin/apikeys/${id}`, { method: 'DELETE' }),

  importCompanionCollection: (file, merge = true) => {
    const formData = new FormData();
    formData.append('file', file);
    const token = getToken();
    const headers = {};
    if (token) headers.Authorization = `Bearer ${token}`;
    const params = new URLSearchParams({ merge: String(!!merge) });
    return fetch('/api/admin/import/companion?' + params.toString(), { method: 'POST', headers, body: formData })
      .then(async r => {
        const json = await r.json();
        if (!r.ok) throw new Error(json.message || `HTTP ${r.status}`);
        return json;
      });
  },
};

// ─── Router ──────────────────────────────────────────────────────────────────

function currentPage() {
  return location.hash.replace(/^#\//, '') || 'collection';
}

function navigate(page) {
  const authenticated = !!getToken();
  if (!authenticated && page !== 'login') {
    location.hash = '#/login';
    return;
  }
  if (authenticated && page === 'login') {
    location.hash = '#/collection';
    return;
  }
  location.hash = '#/' + page;
}

window.addEventListener('hashchange', handleRoute);

function handleRoute() {
  const page = currentPage();
  const authenticated = !!getToken();

  if (!authenticated && page !== 'login') {
    navigate('login');
    return;
  }
  if (authenticated && page === 'login') {
    navigate('collection');
    return;
  }

  stopCamera(); // clean up scanner camera if leaving scanner page
  stopSyncPoll(); // stop admin progress polling
  disconnectCollectionColumnObserver(); // stop column resize tracking if leaving collection page
  stopWantedCelebration(); // stop looping confetti if leaving scanner page
  renderPage(page);
}

function renderPage(page) {
  switch (page) {
    case 'login':      renderLogin();           break;
    case 'collection': renderCollection();      break;
    case 'statistics': renderStatistics();      break;
    case 'pricing':    renderPricingPage();     break;
    case 'scanner':    renderScanner();         break;
    case 'recent':     renderRecentScansPage(); break;
    case 'admin':      renderAdmin();           break;
    default:           navigate('collection');
  }
}

// ─── Navigation bar HTML ─────────────────────────────────────────────────────

function navHTML(active) {
  const item = (page, icon, label) => `
    <a class="nav-item${active === page ? ' active' : ''}" href="#/${page}">
      ${icon}
      <span>${label}</span>
    </a>`;

  return `<nav class="bottom-nav">
    ${item('collection',
      `<svg viewBox="0 0 24 24" fill="currentColor"><path d="M4 5h3v13H4zm5 0h3v13H9zm5 0h3v13h-3zm5 0h3v13h-3z" opacity=".3"/><path d="M2 3v18h20V3H2zm4 16H4V5h2v14zm5 0H9V5h2v14zm5 0h-2V5h2v14zm3 0h-2V5h2v14z"/></svg>`,
      'Collection')}
    ${item('statistics',
      `<svg viewBox="0 0 24 24" fill="currentColor"><path d="M5 9.2h3V19H5V9.2zM10.6 5h2.8v14h-2.8V5zM16.2 13h2.8v6h-2.8v-6z"/></svg>`,
      'Stats')}
    ${item('pricing',
      `<svg viewBox="0 0 24 24" fill="currentColor"><path d="M12 1.5a10.5 10.5 0 1 0 10.5 10.5A10.51 10.51 0 0 0 12 1.5zm0 19a8.5 8.5 0 1 1 8.5-8.5 8.51 8.51 0 0 1-8.5 8.5zm1.2-8.9-1.9-.7c-.7-.2-.9-.5-.9-.9 0-.5.4-.9 1.1-.9.7 0 1.2.3 1.5.7l1.5-1.1A3.39 3.39 0 0 0 13 7.3V6h-2v1.3a3.01 3.01 0 0 0-2.7 3c0 1.6 1 2.4 2.6 3l1.8.7c.6.2.9.5.9 1 0 .6-.5 1-1.3 1-.8 0-1.5-.4-1.9-1l-1.6 1.1A4.18 4.18 0 0 0 11 17.7V19h2v-1.3a3.17 3.17 0 0 0 2.8-3.1c0-1.6-.9-2.5-2.6-3.1z"/></svg>`,
      'Prix')}
    ${item('scanner',
      `<svg viewBox="0 0 24 24" fill="currentColor"><path d="M9.5 6.5v3h-3v-3h3M11 5H5v6h6V5zm-1.5 9.5v3h-3v-3h3M11 13H5v6h6v-6zm6.5-6.5v3h-3v-3h3M19 5h-6v6h6V5zm-6 8h1.5v1.5H13V13zm1.5 1.5H16V16h-1.5v-1.5zM16 13h1.5v1.5H16V13zm-3 3h1.5v1.5H13V16zm1.5 1.5H16V19h-1.5v-1.5zM16 16h1.5v1.5H16V16zm1.5-1.5H19V16h-1.5v-1.5zm0 3H19V19h-1.5v-1.5zM22 7h-2V4h-3V2h5v5zm0 15v-5h-2v3h-3v2h5zM2 22h5v-2H4v-3H2v5zM2 2v5h2V4h3V2H2z"/></svg>`,
      'Scanner')}
    ${item('recent',
      `<svg viewBox="0 0 24 24" fill="currentColor"><path d="M13 3a9 9 0 1 0 9 9h-2a7 7 0 1 1-7-7v4l5-5-5-5v4z"/></svg>`,
      'Récents')}
    ${item('admin',
      `<svg viewBox="0 0 24 24" fill="currentColor"><path d="M19.14 12.94c.04-.3.06-.61.06-.94 0-.32-.02-.64-.07-.94l2.03-1.58a.49.49 0 0 0 .12-.61l-1.92-3.32a.488.488 0 0 0-.59-.22l-2.39.96a7.2 7.2 0 0 0-1.62-.94l-.36-2.54a.484.484 0 0 0-.48-.41h-3.84c-.24 0-.43.17-.47.41l-.36 2.54c-.59.24-1.13.57-1.62.94l-2.39-.96a.48.48 0 0 0-.59.22L2.74 8.87a.47.47 0 0 0 .12.61l2.03 1.58c-.05.3-.09.63-.09.94s.02.64.07.94l-2.03 1.58a.49.49 0 0 0-.12.61l1.92 3.32c.12.22.37.29.59.22l2.39-.96c.5.38 1.03.7 1.62.94l.36 2.54c.05.24.24.41.48.41h3.84c.24 0 .44-.17.47-.41l.36-2.54c.59-.24 1.13-.56 1.62-.94l2.39.96c.22.08.47 0 .59-.22l1.92-3.32a.47.47 0 0 0-.12-.61l-2.01-1.58zM12 15.6a3.6 3.6 0 1 1 0-7.2 3.6 3.6 0 0 1 0 7.2z"/></svg>`,
      'Admin')}
  </nav>`;
}

// ─── Utility helpers ──────────────────────────────────────────────────────────

function esc(str) {
  return String(str ?? '')
    .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
}

function loadingHTML(label = 'Chargement...') {
  return `<div class="loading-center"><div class="spinner"></div><span>${label}</span></div>`;
}

function showToast(message, { error = false, duration = 3500 } = {}) {
  const toast = document.createElement('div');
  toast.className = `toast${error ? ' toast-error' : ''}`;
  toast.textContent = message;
  document.body.appendChild(toast);
  setTimeout(() => toast.remove(), duration);
}

function formatDate(isoStr) {
  if (!isoStr) return '';
  const d = new Date(isoStr);
  return d.toLocaleDateString('fr-FR', { day: '2-digit', month: '2-digit', year: 'numeric' });
}

function formatDateTime(isoStr) {
  if (!isoStr) return '';
  const d = new Date(isoStr);
  return d.toLocaleDateString('fr-FR', { day: '2-digit', month: '2-digit', year: 'numeric' })
    + ' ' + d.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' });
}

function formatEuro(value) {
  const amount = typeof value === 'number' ? value : Number(value || 0);
  return new Intl.NumberFormat('fr-FR', {
    style: 'currency',
    currency: 'EUR',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(Number.isFinite(amount) ? amount : 0);
}

function priceMetadataHTML(card) {
  const price = card.marketPrice != null
    ? `<div>Prix : <strong style="color:var(--accent)">${formatEuro(card.marketPrice)}</strong></div>`
    : '';
  const updatedAt = card.lastPriceAt
    ? `<div>Dernière mise à jour : <strong style="color:var(--text)">${formatDateTime(card.lastPriceAt)}</strong></div>`
    : '';

  if (!price && !updatedAt) return '';

  return `<div style="font-size:.8rem;color:var(--text-muted);margin-top:10px;line-height:1.6">${price}${updatedAt}</div>`;
}

function formatSignedPercent(value) {
  if (value == null || value === '') return '—';
  const num = Number(value);
  if (!Number.isFinite(num)) return '—';
  return `${num >= 0 ? '+' : ''}${num.toFixed(2)}%`;
}

function percentTone(value) {
  if (value == null || value === '') return 'var(--text-muted)';
  const num = Number(value);
  if (!Number.isFinite(num)) return 'var(--text-muted)';
  return num >= 0 ? 'var(--success)' : 'var(--danger)';
}

let recentCardsState = [];
let recentLimit = 20;
let pricingCardsState = [];
let ownedPricingCardsState = [];
let ownedPricingLimit = 20;

// ─── LOGIN ────────────────────────────────────────────────────────────────────

function renderLogin() {
  document.getElementById('app').innerHTML = `
    <div class="login-page">
      <div class="login-logo">
        <h1>✦ LORCALEX</h1>
        <p>Gérez votre collection Lorcana</p>
      </div>
      <div class="login-card">
        <form id="loginForm">
          <div id="loginError"></div>
          <div class="form-group">
            <label>Nom d'utilisateur</label>
            <input id="loginUser" type="text" placeholder="admin" autocomplete="username" required />
          </div>
          <div class="form-group">
            <label>Mot de passe</label>
            <input id="loginPass" type="password" placeholder="••••••••" autocomplete="current-password" required />
          </div>
          <div class="form-group" style="display:flex;align-items:center;gap:10px;margin-bottom:20px">
            <input id="rememberMe" type="checkbox" style="width:18px;height:18px;cursor:pointer;accent-color:var(--accent)" />
            <label for="rememberMe" style="font-size:.9rem;color:var(--text-muted);cursor:pointer;margin:0">Se souvenir de moi (12 mois)</label>
          </div>
          <button type="submit" id="loginBtn" class="btn btn-accent btn-full">Se connecter</button>
        </form>
      </div>
    </div>`;

  document.getElementById('loginForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    const btn = document.getElementById('loginBtn');
    const errDiv = document.getElementById('loginError');
    btn.disabled = true;
    btn.textContent = 'Connexion…';
    errDiv.innerHTML = '';
    try {
      const rememberMe = document.getElementById('rememberMe').checked;
      const data = await api.login(
        document.getElementById('loginUser').value,
        document.getElementById('loginPass').value,
        rememberMe
      );
      if (rememberMe) {
        localStorage.setItem('token', data.token);
      } else {
        sessionStorage.setItem('token', data.token);
      }
      localStorage.setItem('username', data.username);
      navigate('collection');
    } catch {
      errDiv.innerHTML = `<div class="alert alert-error" style="margin-bottom:16px">Identifiants incorrects. Veuillez réessayer.</div>`;
      btn.disabled = false;
      btn.textContent = 'Se connecter';
    }
  });
}

// ─── COLLECTION ───────────────────────────────────────────────────────────────

const collState = { editions: [], edition: null, cards: [], filter: 'all', search: '', modal: null };

// Nombre de colonnes de la grille Collection, plafonné à 10, recalculé au resize.
let _collResizeObserver = null;
let _collResizeRaf = null;

function disconnectCollectionColumnObserver() {
  if (_collResizeObserver) {
    _collResizeObserver.disconnect();
    _collResizeObserver = null;
  }
  if (_collResizeRaf) {
    cancelAnimationFrame(_collResizeRaf);
    _collResizeRaf = null;
  }
}

function setupCollectionColumnObserver() {
  const area = document.getElementById('cardsArea');
  if (!area) return;
  disconnectCollectionColumnObserver();
  const recompute = () => {
    const cols = Math.max(1, Math.min(10, Math.floor(area.clientWidth / 110)));
    area.style.setProperty('--cols', cols);
  };
  _collResizeObserver = new ResizeObserver(() => {
    if (_collResizeRaf) cancelAnimationFrame(_collResizeRaf);
    _collResizeRaf = requestAnimationFrame(recompute);
  });
  _collResizeObserver.observe(area);
  recompute();
}

let _searchDebounceTimer = null;

function updateSearchHint(trimmedQuery) {
  const hintEl = document.getElementById('searchHint');
  if (!hintEl) return;
  hintEl.textContent = (trimmedQuery.length > 0 && trimmedQuery.length < 3)
    ? 'Tapez au moins 3 caractères pour rechercher.'
    : '';
}

function renderCollection() {
  document.getElementById('app').innerHTML = `
    <div class="app">
      <div class="page" id="collPage">
        <div class="page-header">
          <h1>✦ Collection</h1>
          <span id="collCount" style="margin-left:auto;color:var(--accent);font-weight:700;font-size:.9rem"></span>
        </div>
        <div id="editionBar" class="filter-bar">${loadingHTML('')}</div>
        <div id="filterBar" class="filter-bar">
          ${['all','owned','missing','foil'].map((k,i) =>
            `<button class="filter-chip${collState.filter===k?' active':''}" data-filter="${k}">${['Toutes','Possédées','Manquantes','✦ Foil'][i]}</button>`
          ).join('')}
        </div>
        <div class="search-bar">
          <input class="search-input" id="searchInput" placeholder="Rechercher une carte…" value="${esc(collState.search)}" />
        </div>
        <div id="searchHint" style="font-size:.72rem;color:var(--text-muted);padding:0 12px 6px"></div>
        <div id="cardsArea">${loadingHTML()}</div>
      </div>
      ${navHTML('collection')}
      <button class="fab" id="addManualBtn" title="Ajouter une carte manuellement">+</button>
    </div>
    <div id="modalArea"></div>`;

  document.getElementById('filterBar').addEventListener('click', e => {
    const btn = e.target.closest('[data-filter]');
    if (!btn) return;
    collState.filter = btn.dataset.filter;
    document.querySelectorAll('#filterBar .filter-chip').forEach(b => b.classList.toggle('active', b.dataset.filter === collState.filter));
    renderCards();
  });

  document.getElementById('addManualBtn').addEventListener('click', openAddManualModal);

  document.getElementById('searchInput').addEventListener('input', e => {
    const value = e.target.value;
    updateSearchHint(value.trim());
    clearTimeout(_searchDebounceTimer);
    _searchDebounceTimer = setTimeout(() => {
      collState.search = value;
      renderCards();
    }, 300);
  });

  if (collState.editions.length === 0) {
    api.getEditions().then(data => {
      collState.editions = data;
      if (!collState.edition && data.length > 0) collState.edition = data[0].id;
      renderEditionBar();
      loadCards();
    });
  } else {
    renderEditionBar();
    loadCards();
  }
  setupCollectionColumnObserver();
}

function renderEditionBar() {
  const bar = document.getElementById('editionBar');
  if (!bar) return;
  bar.innerHTML = [
    `<button class="filter-chip${collState.edition === 'all' ? ' active' : ''}" data-edition="all">Toutes</button>`,
    ...collState.editions.map(e => {
      const label = e.setNumber ? `Set ${e.setNumber} — ${esc(e.name)}` : esc(e.code || e.name);
      return `<button class="filter-chip${collState.edition == e.id ? ' active' : ''}" data-edition="${e.id}">${label}</button>`;
    }),
  ].join('');

  bar.addEventListener('click', e => {
    const btn = e.target.closest('[data-edition]');
    if (!btn) return;
    const val = btn.dataset.edition;
    collState.edition = val === 'all' ? 'all' : parseInt(val);
    bar.querySelectorAll('.filter-chip').forEach(b => b.classList.toggle('active', b.dataset.edition === btn.dataset.edition));
    loadCards();
  });
}

function loadCards() {
  const area = document.getElementById('cardsArea');
  if (area) area.innerHTML = loadingHTML();
  const edId = collState.edition === 'all' ? undefined : collState.edition;
  api.getCards(edId).then(data => {
    collState.cards = data;
    renderCards();
  });
}

const RARITY_COLORS = {
  // French rarities (LorcaJson)
  'Habituelle':   '#bdbdbd',
  'Inhabituelle': '#81d4fa',
  'Rare':         '#ce93d8',
  'Très Rare':    '#ffb74d',
  'Légendaire':   '#fff176',
  'Enchanté':     '#f48fb1',
  // English fallbacks
  'Common':     '#bdbdbd',
  'Uncommon':   '#81d4fa',
  'Super Rare': '#ffb74d',
  'Legendary':  '#fff176',
  'Enchanted':  '#f48fb1',
};

function renderCards() {
  const { cards, filter, search } = collState;
  const trimmedSearch = search.trim();
  const searchActive = trimmedSearch.length >= 3;
  const filtered = cards.filter(c => {
    if (filter === 'owned' && !c.owned) return false;
    if (filter === 'missing' && c.owned) return false;
    if (filter === 'foil' && !(c.foilQuantity > 0)) return false;
    if (searchActive && !c.name.toLowerCase().includes(trimmedSearch.toLowerCase())) return false;
    return true;
  });

  const count = document.getElementById('collCount');
  if (count) count.textContent = `${cards.filter(c => c.owned).length}/${cards.length}`;

  const area = document.getElementById('cardsArea');
  if (!area) return;

  if (filtered.length === 0) {
    area.innerHTML = `<div class="empty-state">
      <svg viewBox="0 0 24 24" fill="currentColor"><path d="M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-1 16H6c-.55 0-1-.45-1-1V6c0-.55.45-1 1-1h12c.55 0 1 .45 1 1v12c0 .55-.45 1-1 1z"/></svg>
      <h3>Aucune carte</h3>
      <p>Importez le catalogue depuis la page Administration.</p>
    </div>`;
    return;
  }

  area.innerHTML = `<div class="cards-grid">${filtered.map(c => cardItemHTML(c)).join('')}</div>`;
  area.querySelectorAll('.card-item').forEach(el => {
    el.addEventListener('click', () => openModal(parseInt(el.dataset.id)));
  });
  area.querySelectorAll('.wanted-toggle').forEach(el => {
    el.addEventListener('click', e => {
      e.stopPropagation();
      toggleWantedInGrid(parseInt(el.dataset.id));
    });
  });
  area.querySelectorAll('.card-img-lazy').forEach(img => {
    img.addEventListener('load', () => img.classList.remove('card-img-lazy'), { once: true });
    getLazyImageObserver().observe(img);
  });
}

// Observateur partagé : ne charge une image de la grille Collection qu'à l'approche du viewport.
let _lazyImageObserver = null;
function getLazyImageObserver() {
  if (_lazyImageObserver) return _lazyImageObserver;
  _lazyImageObserver = new IntersectionObserver((entries, observer) => {
    entries.forEach(entry => {
      if (!entry.isIntersecting) return;
      const img = entry.target;
      if (img.dataset.src) {
        img.src = img.dataset.src;
        img.removeAttribute('data-src');
      }
      observer.unobserve(img);
    });
  }, { rootMargin: '100% 0px' });
  return _lazyImageObserver;
}

async function toggleWantedInGrid(cardId) {
  const card = collState.cards.find(c => c.id === cardId);
  if (!card) return;
  const updated = await api.setWanted(cardId, !card.wanted);
  collState.cards = collState.cards.map(c => c.id === updated.id ? updated : c);
  renderCards();
}

function cardItemHTML(card) {
  const rarityColor = RARITY_COLORS[card.rarity] || 'var(--text-muted)';
  const setLabel = card.editionSetNumber ? `S${card.editionSetNumber}·` : '';
  const totalQty = (card.quantity || 0) + (card.foilQuantity || 0);
  const hasFoil = card.foilQuantity && card.foilQuantity > 0;
  const showWanted = card.wanted && !card.owned;
  const gridImageUrl = card.thumbnailUrl || card.imageUrl;
  return `<div class="card-item ${card.owned ? 'owned' : 'missing'}${hasFoil ? ' foil' : ''}${showWanted ? ' wanted' : ''}" data-id="${card.id}">
    ${gridImageUrl
      ? `<img data-src="${esc(gridImageUrl)}" alt="${esc(card.name)}" class="card-img-lazy" onerror="this.style.display='none'" />`
      : `<div style="width:100%;aspect-ratio:600/840;background:var(--bg-card2);display:flex;align-items:center;justify-content:center;color:var(--text-muted);font-size:1.5rem">🃏</div>`}
    ${hasFoil ? `<div class="foil-badge">✦ Foil</div>` : ''}
    ${card.owned ? `<div class="owned-badge">${totalQty > 1 ? totalQty : '✓'}</div>` : `<button class="wanted-toggle${card.wanted ? ' active' : ''}" data-id="${card.id}" title="Marquer comme voulue">${card.wanted ? '⭐' : '☆'}</button>`}
    <div class="card-info">
      <div class="card-number">${setLabel}#${esc(card.cardNumber)}</div>
      <div class="card-name">${esc(card.name)}</div>
      ${card.rarity ? `<div class="card-rarity" style="color:${rarityColor}">${esc(card.rarity)}</div>` : ''}
    </div>
  </div>`;
}

function recentCardItemHTML(card) {
  const rarityColor = RARITY_COLORS[card.rarity] || 'var(--text-muted)';
  const setLabel = card.editionSetNumber ? `S${card.editionSetNumber}·` : '';
  const totalQty = (card.quantity || 0) + (card.foilQuantity || 0);
  const hasFoil = card.foilQuantity && card.foilQuantity > 0;
  const scanDate = card.lastAddedAt ? formatDateTime(card.lastAddedAt) : '';
  return `<div class="card-item ${card.owned ? 'owned' : 'missing'}${hasFoil ? ' foil' : ''}" data-id="${card.id}">
    ${card.imageUrl
      ? `<img src="${esc(card.imageUrl)}" alt="${esc(card.name)}" loading="lazy" onerror="this.style.display='none'" />`
      : `<div style="width:100%;aspect-ratio:600/840;background:var(--bg-card2);display:flex;align-items:center;justify-content:center;color:var(--text-muted);font-size:1.5rem">🃏</div>`}
    ${hasFoil ? `<div class="foil-badge">✦ Foil</div>` : ''}
    ${card.owned ? `<div class="owned-badge">${totalQty > 1 ? totalQty : '✓'}</div>` : ''}
    <div class="card-info">
      <div class="card-number">${setLabel}#${esc(card.cardNumber)}</div>
      <div class="card-name">${esc(card.name)}</div>
      ${card.rarity ? `<div class="card-rarity" style="color:${rarityColor}">${esc(card.rarity)}</div>` : ''}
      ${scanDate ? `<div style="font-size:.65rem;color:var(--text-muted);margin-top:2px">🕐 ${scanDate}</div>` : ''}
    </div>
  </div>`;
}

function pricingCardItemHTML(card) {
  const rarityColor = RARITY_COLORS[card.rarity] || 'var(--text-muted)';
  const setLabel = card.editionSetNumber ? `S${card.editionSetNumber}·` : '';
  const priceLabel = card.marketPrice != null ? formatEuro(card.marketPrice) : 'N/A';
  const pricedAt = card.lastPriceAt ? formatDateTime(card.lastPriceAt) : '';
  return `<div class="card-item" data-id="${card.id}">
    ${card.imageUrl
      ? `<img src="${esc(card.imageUrl)}" alt="${esc(card.name)}" loading="lazy" onerror="this.style.display='none'" />`
      : `<div style="width:100%;aspect-ratio:600/840;background:var(--bg-card2);display:flex;align-items:center;justify-content:center;color:var(--text-muted);font-size:1.5rem">🃏</div>`}
    <div class="card-info">
      <div class="card-number">${setLabel}#${esc(card.cardNumber)}</div>
      <div class="card-name">${esc(card.name)}</div>
      ${card.rarity ? `<div class="card-rarity" style="color:${rarityColor}">${esc(card.rarity)}</div>` : ''}
      <div style="font-size:.7rem;color:var(--accent);font-weight:700;margin-top:3px">${priceLabel}</div>
      ${pricedAt ? `<div style="font-size:.65rem;color:var(--text-muted);margin-top:2px">🕐 ${pricedAt}</div>` : ''}
    </div>
  </div>`;
}

function ownedPricingCardItemHTML(card) {
  const rarityColor = RARITY_COLORS[card.rarity] || 'var(--text-muted)';
  const setLabel = card.editionSetNumber ? `S${card.editionSetNumber}·` : '';
  const regularQuantity = card.quantity || 0;
  const foilQuantity = card.foilQuantity || 0;
  return `<div class="card-item owned" data-id="${card.id}">
    ${card.imageUrl
      ? `<img src="${esc(card.imageUrl)}" alt="${esc(card.name)}" loading="lazy" onerror="this.style.display='none'" />`
      : `<div style="width:100%;aspect-ratio:600/840;background:var(--bg-card2);display:flex;align-items:center;justify-content:center;color:var(--text-muted);font-size:1.5rem">🃏</div>`}
    <div class="owned-badge">${regularQuantity + foilQuantity}</div>
    <div class="card-info">
      <div class="card-number">${setLabel}#${esc(card.cardNumber)}</div>
      <div class="card-name">${esc(card.name)}</div>
      ${card.rarity ? `<div class="card-rarity" style="color:${rarityColor}">${esc(card.rarity)}</div>` : ''}
      <div style="font-size:.7rem;color:var(--accent);font-weight:700;margin-top:3px">${formatEuro(card.marketPrice)}</div>
      <div style="font-size:.65rem;color:var(--text-muted);margin-top:2px">Normal : ${regularQuantity} · Foil : ${foilQuantity}</div>
    </div>
  </div>`;
}

function renderOwnedPricingRanking() {
  const area = document.getElementById('ownedPricingRanking');
  if (!area) return;
  const visibleCards = ownedPricingCardsState.slice(0, ownedPricingLimit);
  area.innerHTML = visibleCards.length === 0
    ? `<div class="empty-state" style="padding:24px 8px"><h3>Aucune carte possédée valorisée</h3><p>Le top s'affichera après les premières mises à jour de prix en EUR.</p></div>`
    : `<div class="cards-grid">${visibleCards.map(c => ownedPricingCardItemHTML(c)).join('')}</div>`;
  area.querySelectorAll('.card-item[data-id]').forEach(el => {
    el.addEventListener('click', () => openModal(parseInt(el.dataset.id, 10)));
  });
}

function renderPricingPage() {
  document.getElementById('app').innerHTML = `
    <div class="app">
      <div class="page">
        <div class="page-header"><h1>💶 Prix</h1></div>
        <div id="pricingTabContent">${loadingHTML()}</div>
      </div>
      ${navHTML('pricing')}
    </div>
    <div id="modalArea"></div>`;

  loadPricingData();
}

function loadPricingData() {
  return api.getPricingInsights().then(data => {
    pricingCardsState = data.latestPricedCards || [];
    ownedPricingCardsState = data.ownedCardPriceRanking || [];
    const editionRows = (data.editionValuations || []).map(e => `
      <div class="edition-item">
        <div style="display:flex;justify-content:space-between;align-items:flex-start">
          <div>
            <h3>${esc(e.editionName || '')}</h3>
            <div class="edition-code">${esc(e.editionCode || '')}</div>
          </div>
          <span style="color:var(--accent);font-weight:700">${formatEuro(e.totalValueEur || 0)}</span>
        </div>
        <div style="margin-top:8px;padding:6px 8px;border-radius:8px;background:var(--bg-card2);font-size:.78rem;">
          <div style="color:var(--text-muted)">Coût des cartes manquantes (Courantes et Légendaire)</div>
          <div style="margin-top:4px;font-weight:700">${formatEuro(e.completionCostBaseEur || 0)}</div>
        </div>
        ${e.missingCardsUnknownPrice > 0
          ? `<div style="margin-top:6px;font-size:.72rem;color:var(--warning)">⚠ prix inconnu pour ${e.missingCardsUnknownPrice} carte${e.missingCardsUnknownPrice > 1 ? 's' : ''}, coût minoré</div>`
          : ''}
      </div>`).join('');

    const content = document.getElementById('pricingTabContent');
    if (!content) return;

    content.innerHTML = `
      <div class="stats-grid">
        <div class="stat-card">
          <div class="stat-value">${formatEuro(data.totalCollectionValueEur || 0)}</div>
          <div class="stat-label">Valeur totale (EUR)</div>
          <button class="btn btn-ghost" id="recomputeValueBtn" style="margin-top:8px;padding:4px 10px;font-size:.78rem">🔄 Recalculer</button>
        </div>
        <div class="stat-card"><div class="stat-value" style="color:var(--warning)">${data.excludedNoPrice ?? 0}</div><div class="stat-label">Exclues sans prix</div></div>
        <div class="stat-card"><div class="stat-value" style="color:var(--danger)">${data.excludedNonEur ?? 0}</div><div class="stat-label">Exclues non EUR</div></div>
      </div>

      <div class="chart-container" style="margin-bottom:10px">
        <div style="display:flex;justify-content:space-between;align-items:center;gap:10px;margin-bottom:10px">
          <h3>Top de mes cartes par prix unitaire</h3>
          <div class="filter-bar" id="ownedPricingLimitBar" style="margin:0">
            ${[20, 50, 100].map(limit => `<button class="filter-chip${ownedPricingLimit === limit ? ' active' : ''}" data-limit="${limit}">${limit}</button>`).join('')}
          </div>
        </div>
        <div id="ownedPricingRanking"></div>
      </div>

      <div class="chart-container" style="margin:10px 0 16px">
        <h3>Évolution de la valeur totale de la collection</h3>
        <div style="height:220px"><canvas id="collectionTrendChart"></canvas></div>
      </div>

      <div style="padding:0 12px 4px">
        <h3 style="color:var(--text-muted);font-size:.8rem;text-transform:uppercase;letter-spacing:.5px;margin-bottom:8px">Tendance par édition (Δ 7j / 30j)</h3>
        <div id="editionDeltaTable" aria-live="polite"></div>
      </div>

      <div style="padding:0 12px 4px">
        <h3 style="color:var(--text-muted);font-size:.8rem;text-transform:uppercase;letter-spacing:.5px;margin-bottom:8px">Valeur par édition suivie</h3>
        ${editionRows || `<div class="empty-state" style="padding:24px 8px"><h3>Aucune édition suivie</h3><p>Activez des sets dans l'administration pour voir les valorisations.</p></div>`}
      </div>

      <div class="chart-container" style="margin:10px 0 16px">
        <h3>20 dernières cartes du catalogue valorisées</h3>
        ${pricingCardsState.length === 0
          ? `<div class="empty-state" style="padding:24px 8px"><h3>Aucune carte valorisée</h3><p>La liste s'affichera après les premières mises à jour de prix en EUR.</p></div>`
          : `<div class="cards-grid">${pricingCardsState.map(c => pricingCardItemHTML(c)).join('')}</div>`}
      </div>`;

    renderOwnedPricingRanking();
    document.getElementById('ownedPricingLimitBar').addEventListener('click', event => {
      const button = event.target.closest('[data-limit]');
      if (!button) return;
      ownedPricingLimit = parseInt(button.dataset.limit, 10);
      document.querySelectorAll('#ownedPricingLimitBar .filter-chip').forEach(chip =>
        chip.classList.toggle('active', parseInt(chip.dataset.limit, 10) === ownedPricingLimit)
      );
      renderOwnedPricingRanking();
    });

    content.querySelectorAll('.card-item[data-id]').forEach(el => {
      el.addEventListener('click', () => openModal(parseInt(el.dataset.id, 10)));
    });

    const recomputeBtn = document.getElementById('recomputeValueBtn');
    if (recomputeBtn) {
      recomputeBtn.addEventListener('click', () => {
        recomputeBtn.disabled = true;
        recomputeBtn.textContent = '⏳ Recalcul…';
        api.recomputeValue()
          .then(() => {
            showToast('Snapshot mis à jour');
            return loadPricingData();
          })
          .catch(err => {
            const detail = err.rootCause && err.rootCause !== err.message ? ` (${err.rootCause})` : '';
            showToast(`Échec du recalcul : ${err.message}${detail}`, { error: true, duration: 6000 });
            recomputeBtn.disabled = false;
            recomputeBtn.textContent = '🔄 Recalculer';
          });
      });
    }

    return Promise.all([
      api.getTrend(),
      api.getEditionDeltas(),
    ]);
  }).then(([trendData, editionDeltaData]) => {
    const trendPoints = Array.isArray(trendData?.trend) ? trendData.trend : [];
    const chartEl = document.getElementById('collectionTrendChart');
    if (chartEl && trendPoints.length > 0) {
      const labels = trendPoints.map(p => {
        const d = new Date(p.recordedAt);
        return d.toLocaleDateString('fr-FR', { day: '2-digit', month: '2-digit' });
      });
      const values = trendPoints.map(p => Number(p.totalCollectionValueEur ?? 0));

      new Chart(chartEl, {
        type: 'line',
        data: {
          labels,
          datasets: [{
            label: 'Valeur totale (EUR)',
            data: values,
            borderColor: '#7fb3ff',
            backgroundColor: 'rgba(127,179,255,0.18)',
            borderWidth: 2,
            pointRadius: 3,
            fill: true,
            tension: 0.22,
          }],
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          interaction: { mode: 'nearest', intersect: false },
          plugins: { legend: { labels: { color: '#e8eaf6' } } },
          scales: {
            x: {
              ticks: { color: '#90a4ae' },
              grid: { color: '#2d4060' },
            },
            y: {
              ticks: { color: '#90a4ae', callback: value => `${value}€` },
              grid: { color: '#2d4060' },
            },
          },
        },
      });
    }

    const tableEl = document.getElementById('editionDeltaTable');
    if (tableEl) {
      const rows = (Array.isArray(editionDeltaData) ? editionDeltaData : []).map(e => `
        <div class="edition-item">
          <div style="display:flex;justify-content:space-between;align-items:flex-start;gap:8px">
            <div>
              <h3>${esc(e.editionName || e.editionCode || 'Édition')}</h3>
              <div class="edition-code">${esc(e.editionCode || '')}</div>
            </div>
            <span style="color:var(--accent);font-weight:700">${formatEuro(e.currentValueEur || 0)}</span>
          </div>
          <div style="margin-top:8px;display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:6px;font-size:.78rem;">
            <div style="padding:6px 8px;border-radius:8px;background:var(--bg-card2);">
              <div style="color:var(--text-muted)">7 jours</div>
              <div style="margin-top:4px; font-weight:700; color:${percentTone(e.delta7dPercent)}">${formatSignedPercent(e.delta7dPercent)}</div>
              <div style="color:var(--text-muted)">${e.value7dEur != null ? formatEuro(e.value7dEur) : '—'}</div>
            </div>
            <div style="padding:6px 8px;border-radius:8px;background:var(--bg-card2);">
              <div style="color:var(--text-muted)">30 jours</div>
              <div style="margin-top:4px; font-weight:700; color:${percentTone(e.delta30dPercent)}">${formatSignedPercent(e.delta30dPercent)}</div>
              <div style="color:var(--text-muted)">${e.value30dEur != null ? formatEuro(e.value30dEur) : '—'}</div>
            </div>
          </div>
        </div>`).join('');

      tableEl.innerHTML = rows || `<div class="empty-state" style="padding:24px 8px"><h3>Aucune donnée historique</h3><p>Les snapshots de valeur seront ajoutés après les prochaines synchronisations de prix.</p></div>`;
    }
  }).catch(err => {
    const content = document.getElementById('pricingTabContent');
    if (!content) return;
    content.innerHTML = `<div class="empty-state"><h3>Erreur</h3><p>${esc(err.message)}</p></div>`;
  });
}

function renderRecentScansPage() {
  document.getElementById('app').innerHTML = `
    <div class="app">
      <div class="page">
        <div class="page-header"><h1>\uD83D\uDD50 Derniers scans</h1></div>
        <div class="filter-bar" id="recentLimitBar">
          ${[10, 20, 25, 50].map(n =>
            `<button class="filter-chip${recentLimit === n ? ' active' : ''}" data-limit="${n}">${n} cartes</button>`
          ).join('')}
        </div>
        <div id="recentScansArea">${loadingHTML()}</div>
      </div>
      ${navHTML('recent')}
    </div>
    <div id="modalArea"></div>`;

  document.getElementById('recentLimitBar').addEventListener('click', e => {
    const btn = e.target.closest('[data-limit]');
    if (!btn) return;
    recentLimit = parseInt(btn.dataset.limit);
    document.querySelectorAll('#recentLimitBar .filter-chip').forEach(b =>
      b.classList.toggle('active', parseInt(b.dataset.limit) === recentLimit)
    );
    const area = document.getElementById('recentScansArea');
    if (area) area.innerHTML = loadingHTML('');
    api.getRecentCards(recentLimit).then(cards => {
      recentCardsState = cards || [];
      renderRecentScansSection();
    }).catch(() => {
      const el = document.getElementById('recentScansArea');
      if (el) el.innerHTML = `<div class="empty-state"><h3>Erreur</h3><p>Impossible de charger les derniers scans.</p></div>`;
    });
  });

  api.getRecentCards(recentLimit).then(cards => {
    recentCardsState = cards || [];
    renderRecentScansSection();
  }).catch(() => {
    const el = document.getElementById('recentScansArea');
    if (el) el.innerHTML = `<div class="empty-state"><h3>Erreur</h3><p>Impossible de charger les derniers scans.</p></div>`;
  });
}

function renderRecentScansSection() {
  const area = document.getElementById('recentScansArea');
  if (!area) return;
  if (recentCardsState.length === 0) {
    area.innerHTML = `<div class="empty-state">
      <svg viewBox="0 0 24 24" fill="currentColor"><path d="M13 3a9 9 0 1 0 9 9h-2a7 7 0 1 1-7-7v4l5-5-5-5v4z"/></svg>
      <h3>Aucune carte scannée</h3>
      <p>Les 15 dernières cartes ajoutées apparaîtront ici.</p>
    </div>`;
    return;
  }
  area.innerHTML = `<div class="cards-grid">${recentCardsState.map(c => recentCardItemHTML(c)).join('')}</div>`;
  area.querySelectorAll('.card-item').forEach(el => {
    el.addEventListener('click', () => openModal(parseInt(el.dataset.id)));
  });
}

function openModal(cardId) {
  const card = collState.cards.find(c => c.id === cardId)
             || recentCardsState.find(c => c.id === cardId)
             || ownedPricingCardsState.find(c => c.id === cardId)
             || pricingCardsState.find(c => c.id === cardId);
  if (!card) return;
  collState.modal = card;

  document.getElementById('modalArea').innerHTML = `
    <div class="modal-overlay" id="modalOverlay">
      <div class="modal-card-detail" id="modalContent">
        <button class="modal-close-btn" id="modalCloseBtn" aria-label="Fermer">✕</button>

        ${card.imageUrl
          ? `<img src="${esc(card.imageUrl)}" alt="${esc(card.name)}" class="modal-card-image"
               onerror="this.style.display='none'" />`
          : `<div class="modal-card-placeholder">🃏</div>`}

        <div class="modal-card-info">
          <div class="modal-card-meta">
            ${card.editionSetNumber ? `<span class="modal-set-badge">Set ${card.editionSetNumber}</span>` : ''}
            <span style="color:var(--text-muted);font-size:.8rem">#${esc(card.cardNumber)}</span>
          </div>
          <h2 class="modal-card-name">${esc(card.name)}</h2>
          ${card.rarity
            ? `<div style="font-size:.85rem;font-weight:600;margin-bottom:12px;color:${RARITY_COLORS[card.rarity]||'var(--text-muted)'}">${esc(card.rarity)}</div>`
            : ''}
          ${priceMetadataHTML(card)}
          ${card.owned && card.marketPrice != null && ownedPricingCardsState.some(c => c.id === card.id)
            ? `<button class="btn btn-ghost btn-full" id="removeCardPriceBtn" style="margin-top:10px">Supprimer le prix</button>`
            : ''}

          <div class="modal-qty-section">
            ${card.owned
              ? `<div>
                  <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:10px">
                    <span style="font-size:.9rem;color:var(--text-muted)">Régulière</span>
                    <div class="qty-control">
                      <button class="qty-btn" id="qtyMinus">−</button>
                      <span class="qty-value" id="qtyVal">${card.quantity}</span>
                      <button class="qty-btn" id="qtyPlus">＋</button>
                    </div>
                  </div>
                  <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:8px">
                    <span style="font-size:.9rem;color:var(--text-muted)">Foilée</span>
                    <div class="qty-control">
                      <button class="qty-btn" id="qtyFoilMinus">−</button>
                      <span class="qty-value" id="qtyFoilVal">${card.foilQuantity}</span>
                      <button class="qty-btn" id="qtyFoilPlus">＋</button>
                    </div>
                  </div>
                  ${(card.firstAddedAt || card.lastAddedAt) ? `
                  <div style="font-size:.75rem;color:var(--text-muted);margin-top:4px;line-height:1.6">
                    ${card.firstAddedAt ? `<div>Ajoutée le : <strong style="color:var(--text)">${formatDate(card.firstAddedAt)}</strong></div>` : ''}
                    ${card.lastAddedAt && card.lastAddedAt !== card.firstAddedAt ? `<div>Dernière modif. : <strong style="color:var(--text)">${formatDate(card.lastAddedAt)}</strong></div>` : ''}
                  </div>` : ''}
                </div>`
              : `<div style="display:flex;gap:10px;flex-direction:column">
                  <button class="btn btn-accent btn-full" id="addCardRegularBtn">◇ Ajouter exemplaire normal</button>
                  <button class="btn btn-ghost btn-full" id="addCardFoilBtn">✦ Ajouter exemplaire foil</button>
                </div>`}
          </div>
        </div>
      </div>
    </div>`;

  document.getElementById('modalOverlay').addEventListener('click', e => {
    if (e.target === document.getElementById('modalOverlay')) closeModal();
  });
  document.getElementById('modalCloseBtn').addEventListener('click', closeModal);
  document.getElementById('modalContent').addEventListener('click', e => e.stopPropagation());

  const removeCardPriceButton = document.getElementById('removeCardPriceBtn');
  if (removeCardPriceButton) {
    removeCardPriceButton.addEventListener('click', async () => {
      if (!window.confirm(`Supprimer le prix de "${card.name}" ? Les quantités possédées ne seront pas modifiées.`)) return;
      try {
        await api.removeCardPrice(card.id);
        closeModal();
        renderPricingPage();
      } catch (error) {
        window.alert(error.message);
      }
    });
  }

  if (card.owned) {
    document.getElementById('qtyMinus').addEventListener('click', () => updateQtyRegular(card.id, card.quantity - 1));
    document.getElementById('qtyPlus').addEventListener('click', () => updateQtyRegular(card.id, card.quantity + 1));
    document.getElementById('qtyFoilMinus').addEventListener('click', () => updateQtyFoiled(card.id, (card.foilQuantity || 0) - 1));
    document.getElementById('qtyFoilPlus').addEventListener('click', () => updateQtyFoiled(card.id, (card.foilQuantity || 0) + 1));
  } else {
    document.getElementById('addCardRegularBtn').addEventListener('click', () => addCard(card.id, 1, 0));
    document.getElementById('addCardFoilBtn').addEventListener('click', () => addCard(card.id, 0, 1));
  }
}

function closeModal() {
  document.getElementById('modalArea').innerHTML = '';
  collState.modal = null;
}

// ─── Manual add modal ────────────────────────────────────────────────────────

function openAddManualModal() {
  document.getElementById('modalArea').innerHTML = `
    <div class="modal-overlay" id="addModalOverlay">
      <div class="modal" id="addModalContent" style="border-radius:20px 20px 0 0">
        <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:16px">
          <h2 style="margin:0">Ajouter une carte</h2>
          <button class="btn btn-ghost" id="addModalClose" style="padding:6px 10px;font-size:1.1rem">✕</button>
        </div>
        <div style="display:flex;gap:8px;margin-bottom:12px">
          <input class="search-input" id="addSearchInput" placeholder="Nom ou numéro de carte…"
            style="border-radius:8px;flex:1" autocomplete="off" />
          <button class="btn btn-accent" id="addSearchBtn" style="flex-shrink:0">Chercher</button>
        </div>
        <div id="addSearchResults"></div>
      </div>
    </div>`;

  const input = document.getElementById('addSearchInput');
  document.getElementById('addModalOverlay').addEventListener('click', e => {
    if (e.target === document.getElementById('addModalOverlay')) closeModal();
  });
  document.getElementById('addModalContent').addEventListener('click', e => e.stopPropagation());
  document.getElementById('addModalClose').addEventListener('click', closeModal);
  document.getElementById('addSearchBtn').addEventListener('click', () => doAddSearch(input.value));
  input.addEventListener('keydown', e => { if (e.key === 'Enter') doAddSearch(input.value); });
  input.focus();
}

async function doAddSearch(query) {
  const q = query.trim();
  if (!q) return;
  const area = document.getElementById('addSearchResults');
  if (!area) return;
  area.innerHTML = loadingHTML();

  try {
    let results;
    const asNum = parseInt(q);
    if (!isNaN(asNum) && String(asNum) === q) {
      // numeric → lookup by number across all editions
      results = await api.lookupCard(asNum, undefined);
    } else {
      // text → search by name
      results = await apiFetch('/cards?q=' + encodeURIComponent(q));
    }

    if (results.length === 0) {
      area.innerHTML = `<div class="empty-state" style="padding:32px 0">
        <h3>Aucun résultat</h3>
        <p style="font-size:.85rem">Vérifiez l'orthographe ou synchronisez le catalogue (Admin).</p>
      </div>`;
      return;
    }

    area.innerHTML = `<div style="display:flex;flex-direction:column;gap:8px;max-height:55vh;overflow-y:auto;padding-bottom:4px">
      ${results.map(c => `
        <div class="add-result-row" data-id="${c.id}">
          <div style="display:flex;align-items:center;gap:12px;flex:1;min-width:0">
            ${c.imageUrl
              ? `<img src="${esc(c.imageUrl)}" alt="" style="width:44px;border-radius:6px;flex-shrink:0" onerror="this.style.display='none'" />`
              : `<div style="width:44px;height:62px;background:var(--bg-card2);border-radius:6px;flex-shrink:0;display:flex;align-items:center;justify-content:center;font-size:1.2rem">🃏</div>`}
            <div style="min-width:0">
              <div style="font-size:.7rem;color:var(--text-muted)">${c.editionSetNumber ? `Set ${c.editionSetNumber} · ` : ''}#${esc(c.cardNumber)} • ${esc(c.editionCode)}</div>
              <div style="font-weight:700;white-space:nowrap;overflow:hidden;text-overflow:ellipsis">${esc(c.name)}</div>
              <div style="font-size:.75rem;color:var(--text-muted)">${esc(c.rarity || '')}</div>
            </div>
          </div>
          ${c.owned
            ? `<div class="qty-control" style="flex-shrink:0">
                <button class="qty-btn" data-action="minus" data-id="${c.id}" data-qty="${c.quantity}">−</button>
                <span class="qty-value">${c.quantity}</span>
                <button class="qty-btn" data-action="plus" data-id="${c.id}" data-qty="${c.quantity}">＋</button>
               </div>`
            : `<div class="add-action-container" style="display:flex;flex-direction:column;align-items:flex-end;gap:4px;flex-shrink:0">
                <button class="btn btn-accent" style="padding:4px 10px;font-size:.7rem" data-action="add-regular" data-id="${c.id}">◇ Normal</button>
                <button class="btn btn-ghost" style="padding:4px 10px;font-size:.7rem" data-action="add-foil" data-id="${c.id}">✦ Foil</button>
               </div>`}
        </div>`).join('')}
    </div>`;

    area.addEventListener('click', async e => {
      const btn = e.target.closest('[data-action]');
      if (!btn) return;
      const cardId = parseInt(btn.dataset.id);
      const action = btn.dataset.action;
      let updated;
      if (action === 'add-regular') {
        updated = await api.addToCollection(cardId, 1, 0);
      } else if (action === 'add-foil') {
        updated = await api.addToCollection(cardId, 0, 1);
      } else if (action === 'plus') {
        updated = await api.updateQuantity(cardId, parseInt(btn.dataset.qty) + 1);
      } else if (action === 'minus') {
        const newQty = parseInt(btn.dataset.qty) - 1;
        updated = newQty <= 0 ? await api.removeFromCollection(cardId) : await api.updateQuantity(cardId, newQty);
      }
      // refresh search results and main collection cache
      collState.cards = collState.cards.map(c => c.id === updated.id ? updated : c);
      results = results.map(c => c.id === updated.id ? updated : c);
      renderCards();
      // re-render results in-place
      area.querySelectorAll('.add-result-row').forEach(row => {
        if (parseInt(row.dataset.id) !== updated.id) return;
        const qtyControl = row.querySelector('.qty-control');
        const addContainer = row.querySelector('.add-action-container');
        if (updated.owned) {
          const ctrl = `<div class="qty-control" style="flex-shrink:0">
            <button class="qty-btn" data-action="minus" data-id="${updated.id}" data-qty="${updated.quantity}">−</button>
            <span class="qty-value">${updated.quantity}</span>
            <button class="qty-btn" data-action="plus" data-id="${updated.id}" data-qty="${updated.quantity}">＋</button>
          </div>`;
          const target = qtyControl || addContainer;
          if (target) target.outerHTML = ctrl;
        } else {
          const ab = `<div class="add-action-container" style="display:flex;flex-direction:column;align-items:flex-end;gap:4px;flex-shrink:0">
            <button class="btn btn-accent" style="padding:4px 10px;font-size:.7rem" data-action="add-regular" data-id="${updated.id}">◇ Normal</button>
            <button class="btn btn-ghost" style="padding:4px 10px;font-size:.7rem" data-action="add-foil" data-id="${updated.id}">✦ Foil</button>
          </div>`;
          const target = addContainer || qtyControl;
          if (target) target.outerHTML = ab;
        }
      });
    });
  } catch (e) {
    area.innerHTML = `<div class="alert alert-error">${esc(e.message)}</div>`;
  }
}

async function addCard(cardId, quantity = 1, foilQuantity = 0) {
  const updated = await api.addToCollection(cardId, quantity, foilQuantity);
  collState.cards = collState.cards.map(c => c.id === updated.id ? updated : c);
  collState.modal = updated;
  renderCards();
  openModal(cardId);
}

async function updateQty(cardId, qty) {
  let updated;
  if (qty <= 0) {
    updated = await api.removeFromCollection(cardId);
  } else {
    updated = await api.updateQuantity(cardId, qty, undefined);
  }
  collState.cards = collState.cards.map(c => c.id === updated.id ? updated : c);
  collState.modal = updated;
  renderCards();
  openModal(cardId);
}

async function updateQtyRegular(cardId, qty) {
  const sourceCard = collState.cards.find(c => c.id === cardId)
                  || (collState.modal?.id === cardId ? collState.modal : null)
                  || recentCardsState.find(c => c.id === cardId)
                  || pricingCardsState.find(c => c.id === cardId);
  if (!sourceCard) return;
  const foilQty = sourceCard.foilQuantity || 0;
  let updated;
  if (qty <= 0 && foilQty <= 0) {
    updated = await api.removeFromCollection(cardId);
  } else {
    updated = await api.updateQuantity(cardId, qty, foilQty);
  }
  collState.cards = collState.cards.map(c => c.id === updated.id ? updated : c);
  recentCardsState = recentCardsState.map(c => c.id === updated.id ? updated : c);
  pricingCardsState = pricingCardsState.map(c => c.id === updated.id ? updated : c);
  collState.modal = updated;
  renderCards();
  renderRecentScansSection();
  openModal(cardId);
}

async function updateQtyFoiled(cardId, qty) {
  const sourceCard = collState.cards.find(c => c.id === cardId)
                  || (collState.modal?.id === cardId ? collState.modal : null)
                  || recentCardsState.find(c => c.id === cardId)
                  || pricingCardsState.find(c => c.id === cardId);
  if (!sourceCard) return;
  const regQty = sourceCard.quantity || 0;
  let updated;
  if (regQty <= 0 && qty <= 0) {
    updated = await api.removeFromCollection(cardId);
  } else {
    updated = await api.updateQuantity(cardId, regQty, qty);
  }
  collState.cards = collState.cards.map(c => c.id === updated.id ? updated : c);
  recentCardsState = recentCardsState.map(c => c.id === updated.id ? updated : c);
  pricingCardsState = pricingCardsState.map(c => c.id === updated.id ? updated : c);
  collState.modal = updated;
  renderCards();
  renderRecentScansSection();
  openModal(cardId);
}

// ─── STATISTICS ───────────────────────────────────────────────────────────────

const INK_COLORS = ['Ambre', 'Améthyste', 'Émeraude', 'Rubis', 'Saphir', 'Acier'];
const RARITY_ORDER = ['Commune', 'Inhabituelle', 'Rare', 'Très Rare', 'Légendaire'];

function normalizeIconName(value) {
  return String(value || '')
    .toLowerCase()
    .normalize('NFD').replace(/[\u0300-\u036f]/g, '')
    .replace(/\s+/g, '');
}

// <img> with a text fallback shown if the icon file fails to load (accessibility + resilience)
function iconWithFallback(src, label, sizePx) {
  return `<span>
    <img src="${src}" alt="${esc(label)}" title="${esc(label)}"
      style="width:${sizePx}px;height:${sizePx}px;vertical-align:middle"
      onerror="this.style.display='none';this.nextElementSibling.style.display='inline'" />
    <span style="display:none;font-size:.75rem">${esc(label)}</span>
  </span>`;
}

// preloaded once so Chart.js can draw them as x-axis tick icons (see rarityIconTicksPlugin)
const RARITY_ICON_IMAGES = {};
RARITY_ORDER.forEach(r => {
  const img = new Image();
  img.src = `icons/rarity/${normalizeIconName(r)}.png`;
  RARITY_ICON_IMAGES[r] = img;
});

const rarityIconTicksPlugin = {
  id: 'rarityIconTicks',
  afterDraw(chart) {
    const xScale = chart.scales?.x;
    if (!xScale) return;
    const ctx = chart.ctx;
    const labels = chart.data.labels || [];
    const size = 20;
    labels.forEach((label, i) => {
      const img = RARITY_ICON_IMAGES[label];
      if (!img || !img.complete || img.naturalWidth === 0) return;
      const x = xScale.getPixelForTick(i);
      ctx.drawImage(img, x - size / 2, xScale.bottom + 6, size, size);
    });
  },
};

function buildMissingByColorTable(stats) {
  const editionsWithMissing = (stats.byEdition || []).filter(e => e.missingCards > 0);
  if (editionsWithMissing.length === 0) return '';

  const headerCells = INK_COLORS.map(color => `
    <th style="text-align:center;padding:8px 10px">${iconWithFallback(`icons/ink/${normalizeIconName(color)}.png`, color, 26)}</th>`).join('');

  const rows = editionsWithMissing.map(e => {
    const missingByColor = e.missingByColor || [];
    const cells = INK_COLORS.map(color => {
      const entry = missingByColor.find(m => m.inkColor === color);
      const ordered = entry
        ? RARITY_ORDER.map(r => (entry.byRarity || []).find(rc => rc.rarity === r)).filter(Boolean)
        : [];
      if (ordered.length === 0) {
        return '<td style="text-align:center;color:var(--text-muted);border-bottom:1px solid var(--border)">—</td>';
      }
      const total = ordered.reduce((sum, rc) => sum + rc.missingCards, 0);
      const pills = ordered.map(rc => `
        <span style="display:inline-flex;align-items:center;gap:3px;margin:1px 4px 1px 0;white-space:nowrap">
          ${iconWithFallback(`icons/rarity/${normalizeIconName(rc.rarity)}.png`, rc.rarity, 14)}
          <span style="font-size:.78rem">${rc.missingCards}</span>
        </span>`).join('');
      return `<td style="border-bottom:1px solid var(--border)">${pills}<span style="display:block;margin-top:4px;font-size:.75rem;color:var(--text-muted)">(=${total})</span></td>`;
    }).join('');
    return `<tr>
      <td style="font-weight:700;white-space:nowrap;padding:8px 10px;border-bottom:1px solid var(--border)">${esc(e.editionName)}</td>
      ${cells}
      <td style="text-align:center;font-weight:700;color:#ffca28;padding:8px 10px;border-bottom:1px solid var(--border)">${e.missingCards}</td>
    </tr>`;
  }).join('');

  return `
    <div class="chart-container">
      <h3>Manquantes par édition</h3>
      <div style="overflow-x:auto">
        <table style="border-collapse:collapse;width:100%;font-size:.85rem">
          <thead>
            <tr style="border-bottom:1px solid var(--border)">
              <th style="text-align:left;padding:8px 10px">Édition</th>
              ${headerCells}
              <th style="padding:8px 10px">Total</th>
            </tr>
          </thead>
          <tbody>${rows}</tbody>
        </table>
      </div>
    </div>`;
}

function renderStatistics() {
  document.getElementById('app').innerHTML = `
    <div class="app">
      <div class="page">
        <div class="page-header"><h1>📊 Statistiques</h1></div>
        <div id="statsContent">${loadingHTML()}</div>
      </div>
      ${navHTML('statistics')}
    </div>`;

  api.getStatistics().then(stats => {
    const content = document.getElementById('statsContent');
    if (!content) return;

    const editionRows = stats.byEdition.map(e => `
      <div class="edition-item">
        <div style="display:flex;justify-content:space-between;align-items:flex-start">
          <div>
            <h3>${esc(e.editionName)}</h3>
            <div class="edition-code">${esc(e.editionCode)}</div>
          </div>
          <span style="color:var(--accent);font-weight:700">${e.completionPercentage.toFixed(0)}%</span>
        </div>
        <div class="edition-progress">${e.ownedCards}/${e.totalCards} cartes</div>
        <div class="progress-bar" style="margin-top:8px">
          <div class="progress-bar-fill" style="width:${e.completionPercentage}%"></div>
        </div>
      </div>`).join('');

    const missingByColorTable = buildMissingByColorTable(stats);

    content.innerHTML = `
      <div class="stats-grid">
        <div class="stat-card"><div class="stat-value">${stats.totalCards}</div><div class="stat-label">Total</div></div>
        <div class="stat-card"><div class="stat-value" style="color:var(--success)">${stats.ownedCards}</div><div class="stat-label">Possédées</div></div>
        <div class="stat-card"><div class="stat-value" style="color:var(--danger)">${stats.missingCards}</div><div class="stat-label">Manquantes</div></div>
      </div>

      <div class="chart-container">
        <h3>Complétion globale — ${stats.completionPercentage.toFixed(1)}%</h3>
        <div style="max-width:220px;margin:0 auto"><canvas id="globalDoughnut"></canvas></div>
      </div>

      <div style="padding:0 12px 4px">
        <h3 style="color:var(--text-muted);font-size:.8rem;text-transform:uppercase;letter-spacing:.5px;margin-bottom:8px">Progression par édition</h3>
        ${editionRows}
      </div>

      ${stats.byEdition.length > 0 ? `
      <div class="chart-container">
        <h3>Cartes par édition</h3>
        <div style="height:200px"><canvas id="editionBar"></canvas></div>
      </div>` : ''}

      ${Object.keys(stats.byEdition.reduce((m, e) => { (e.byRarity||[]).forEach(r => m[r.rarity]=1); return m; }, {})).length > 0 ? `
      <div class="chart-container">
        <h3>Cartes par rareté</h3>
        <div style="height:220px"><canvas id="globalRarityBar"></canvas></div>
      </div>` : ''}

      ${missingByColorTable}`;

    const legendColor = '#e8eaf6';
    const tickColor = '#90a4ae';
    const gridColor = '#2d4060';
    const borderColor = '#1e2d3d';

    // Global doughnut
    new Chart(document.getElementById('globalDoughnut'), {
      type: 'doughnut',
      data: {
        labels: ['Possédées', 'Manquantes'],
        datasets: [{ data: [stats.ownedCards, stats.missingCards], backgroundColor: ['#66bb6a','#ef5350'], borderColor, borderWidth: 2 }],
      },
      options: {
        responsive: true, cutout: '70%',
        plugins: { legend: { labels: { color: legendColor, padding: 16, font: { size: 12 } } } },
      },
    });

    // Stacked bar by edition
    if (stats.byEdition.length > 0) {
      const stackedOpts = {
        responsive: true, maintainAspectRatio: false,
        plugins: { legend: { labels: { color: legendColor } } },
        scales: {
          x: { ticks: { color: tickColor }, grid: { color: gridColor }, stacked: true },
          y: { ticks: { color: tickColor }, grid: { color: gridColor }, stacked: true },
        },
      };
      new Chart(document.getElementById('editionBar'), {
        type: 'bar',
        data: {
          labels: stats.byEdition.map(e => e.editionCode || e.editionName),
          datasets: [
            { label: 'Possédées', data: stats.byEdition.map(e => e.ownedCards), backgroundColor: '#66bb6a' },
            { label: 'Manquantes', data: stats.byEdition.map(e => e.missingCards), backgroundColor: '#ef5350' },
          ],
        },
        options: stackedOpts,
      });

      // Global rarity aggregation
      const rarityMap = {};
      for (const edition of stats.byEdition) {
        for (const r of edition.byRarity || []) {
          if (!rarityMap[r.rarity]) rarityMap[r.rarity] = { owned: 0, missing: 0 };
          rarityMap[r.rarity].owned += r.ownedCards;
          rarityMap[r.rarity].missing += r.missingCards;
        }
      }
      const rarities = Object.keys(rarityMap);
      if (rarities.length > 0) {
        const globalRarityChart = new Chart(document.getElementById('globalRarityBar'), {
          type: 'bar',
          data: {
            labels: rarities,
            datasets: [
              { label: 'Possédées', data: rarities.map(r => rarityMap[r].owned), backgroundColor: '#66bb6a' },
              { label: 'Manquantes', data: rarities.map(r => rarityMap[r].missing), backgroundColor: '#ef5350' },
            ],
          },
          options: {
            ...stackedOpts,
            layout: { padding: { bottom: 26 } },
            scales: {
              ...stackedOpts.scales,
              x: { ...stackedOpts.scales.x, ticks: { ...stackedOpts.scales.x.ticks, callback: () => '' } },
            },
          },
          plugins: [rarityIconTicksPlugin],
        });
        // icons may still be loading on first render; redraw once each finishes
        rarities.forEach(r => {
          const img = RARITY_ICON_IMAGES[r];
          if (img && !img.complete) img.onload = () => globalRarityChart.update();
        });
      }
    }
  });
}

// ─── SCANNER ─ Lecture OCR du code Lorcana en bas-gauche de la carte ──────────
// Format : "N/TOTAL • LANG • SET"   exemple : "1/204 • FR • 4"

let _cameraStream = null;

function stopCamera() {
  if (_cameraStream) {
    _cameraStream.getTracks().forEach(t => t.stop());
    _cameraStream = null;
  }
}

// ── Capture vidéo recadrée sur le cadre carte ─────────────────────────────────
// Convertit les coordonnées écran → pixels natifs (object-fit:cover).
function cropToCardFrame(video) {
  const c = document.createElement('canvas');
  const frame = document.querySelector('.scanner-card-frame');
  if (!frame || !video.videoWidth) {
    c.width = video.videoWidth || 640;
    c.height = video.videoHeight || 480;
    c.getContext('2d').drawImage(video, 0, 0);
    return c;
  }
  const vR = video.getBoundingClientRect();
  const fR = frame.getBoundingClientRect();
  const nW = video.videoWidth, nH = video.videoHeight;
  const dW = vR.width,        dH = vR.height;
  const s  = Math.max(dW / nW, dH / nH);
  const ox = (nW * s - dW) / 2;
  const oy = (nH * s - dH) / 2;
  const cropX = Math.max(0, ((fR.left - vR.left) + ox) / s);
  const cropY = Math.max(0, ((fR.top  - vR.top)  + oy) / s);
  const cropW = Math.min(nW - cropX, fR.width  / s);
  const cropH = Math.min(nH - cropY, fR.height / s);
  c.width  = Math.round(cropW);
  c.height = Math.round(cropH);
  c.getContext('2d').drawImage(video, cropX, cropY, cropW, cropH, 0, 0, c.width, c.height);
  return c;
}

// ── Extraction de la zone code (bas-gauche) ───────────────────────────────────
// Prend les 12 % inférieurs × 55 % gauches de la carte, upscale ×8 pour l'OCR.
function extractCodeZone(cardCanvas) {
  const sw = cardCanvas.width, sh = cardCanvas.height;
  const zW = Math.round(sw * 0.55);
  const zH = Math.round(sh * 0.12);
  const zY = sh - zH;
  const SCALE = 3;
  const out = document.createElement('canvas');
  out.width  = zW * SCALE;
  out.height = zH * SCALE;
  const ctx = out.getContext('2d');
  ctx.imageSmoothingEnabled = true;
  ctx.imageSmoothingQuality = 'high';
  ctx.drawImage(cardCanvas, 0, zY, zW, zH, 0, 0, out.width, out.height);
  return out;
}

// ── Prétraitements image ──────────────────────────────────────────────────────

// Conversion niveaux de gris in-place.
function toGrayscale(canvas) {
  const ctx = canvas.getContext('2d');
  const img = ctx.getImageData(0, 0, canvas.width, canvas.height);
  const d = img.data;
  for (let i = 0; i < d.length; i += 4) {
    const v = (d[i] * 299 + d[i+1] * 587 + d[i+2] * 114) / 1000 | 0;
    d[i] = d[i+1] = d[i+2] = v;
  }
  ctx.putImageData(img, 0, 0);
  return canvas;
}

// Binarisation par seuil d'Otsu (in-place, image déjà en niveaux de gris).
function otsuThreshold(canvas) {
  const ctx = canvas.getContext('2d');
  const img = ctx.getImageData(0, 0, canvas.width, canvas.height);
  const d = img.data;
  const N = canvas.width * canvas.height;
  const hist = new Int32Array(256);
  for (let i = 0; i < d.length; i += 4) hist[d[i]]++;
  let sum = 0;
  for (let i = 0; i < 256; i++) sum += i * hist[i];
  let sumB = 0, wB = 0, maxVar = 0, threshold = 128;
  for (let t = 0; t < 256; t++) {
    wB += hist[t];
    if (!wB) continue;
    const wF = N - wB;
    if (!wF) break;
    sumB += t * hist[t];
    const mB = sumB / wB;
    const mF = (sum - sumB) / wF;
    const v = wB * wF * (mB - mF) ** 2;
    if (v > maxVar) { maxVar = v; threshold = t; }
  }
  for (let i = 0; i < d.length; i += 4) {
    const v = d[i] > threshold ? 255 : 0;
    d[i] = d[i+1] = d[i+2] = v;
  }
  ctx.putImageData(img, 0, 0);
  return canvas;
}

// Retourne un nouveau canvas inversé (noir ↔ blanc).
function invertCanvas(src) {
  const out = document.createElement('canvas');
  out.width = src.width; out.height = src.height;
  const ctx = out.getContext('2d');
  ctx.drawImage(src, 0, 0);
  const img = ctx.getImageData(0, 0, out.width, out.height);
  const d = img.data;
  for (let i = 0; i < d.length; i += 4) {
    d[i] = 255 - d[i]; d[i+1] = 255 - d[i+1]; d[i+2] = 255 - d[i+2];
  }
  ctx.putImageData(img, 0, 0);
  return out;
}

// Copie un canvas.
function cloneCanvas(src) {
  const out = document.createElement('canvas');
  out.width = src.width; out.height = src.height;
  out.getContext('2d').drawImage(src, 0, 0);
  return out;
}

// ── Tesseract worker (v5) ────────────────────────────────────────────────────
// v5 corrige le bug WASM "SetImageFile, e is null" de la v4.
// Un seul worker peut être réutilisé pour plusieurs recognize() consécutifs.
async function createTessWorker() {
  if (typeof Tesseract === 'undefined') {
    throw new Error('Tesseract.js non chargé. Vérifiez votre connexion internet et rechargez la page.');
  }
  const w = await Tesseract.createWorker('eng');
  return w;
}

// Convertit un canvas en Blob PNG (format fiable pour le transfert vers le Web Worker Tesseract).
function canvasToBlob(canvas) {
  return new Promise(resolve => canvas.toBlob(resolve, 'image/png'));
}

// ── Parsing du code Lorcana ───────────────────────────────────────────────────
// Format : "N/TOTAL • LANG • SET"  ex : "1/204 • FR • 4"
const DEFAULT_SCANNER_TOTAL_MAX = 500;
let scannerTotalMax = DEFAULT_SCANNER_TOTAL_MAX;

async function loadScannerSettings() {
  try {
    const settings = await api.getSettings();
    const row = (settings || []).find(s => s.settingKey === 'scanner_total_max');
    if (!row || row.settingValue == null) {
      scannerTotalMax = DEFAULT_SCANNER_TOTAL_MAX;
      return;
    }
    const parsed = parseInt(String(row.settingValue), 10);
    scannerTotalMax = Number.isFinite(parsed) && parsed >= 2 && parsed <= 999
      ? parsed
      : DEFAULT_SCANNER_TOTAL_MAX;
  } catch {
    scannerTotalMax = DEFAULT_SCANNER_TOTAL_MAX;
  }
}

function parseCardCode(rawText) {
  const text = rawText
    .toUpperCase()
    .replace(/\n/g,     ' ')
    .replace(/[oO@]/g,  '0')
    .replace(/[lI|!]/g, '1')
    .replace(/[–—―‒]/g, '/')
    .replace(/\s+/g,    ' ')
    .trim();
  const m = text.match(/(\d{1,3})\s*[\/\\]\s*(\d{2,3})/);
  if (!m) return null;
  const cardNum = parseInt(m[1], 10);
  const total   = parseInt(m[2], 10);
  if (cardNum < 1 || cardNum > 999) return null;
  if (total   < 2  || total   > scannerTotalMax) return null;
  const after  = text.slice(text.indexOf(m[0]) + m[0].length);
  const langM  = after.match(/\b([A-Z]{2})\b/);
  let setNum = null;
  if (langM) {
    const afterLang = after.slice(langM.index + langM[0].length);
    const setM = afterLang.match(/(\d+)/);
    if (setM) setNum = parseInt(setM[1], 10);
  } else {
    const setM = after.match(/(\d+)(?=[^\d]*$)/);
    if (setM) setNum = parseInt(setM[1], 10);
  }
  return {
    cardNum,
    total,
    lang:   langM ? langM[1] : null,
    setNum,
    raw:    rawText.trim(),
  };
}

let _fingerprintsCache = null; // conservé pour le reset de cache dans Admin
let _syncPolling = null;

async function loadFingerprints() {
  if (_fingerprintsCache) return _fingerprintsCache;
  _fingerprintsCache = await api.getFingerprints();
  return _fingerprintsCache;
}

let _scanState = {
  scanning: false,
  continuous: false,
  continuousRunId: 0,
};

function sleep(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}

function updateContinuousButton() {
  const btn = document.getElementById('scanContinuousBtn');
  if (!btn) return;
  if (_scanState.continuous) {
    btn.textContent = '⏹ Arrêter le scan continu';
    btn.classList.remove('btn-ghost');
    btn.classList.add('btn-accent');
  } else {
    btn.textContent = '▶ Démarrer le scan continu';
    btn.classList.remove('btn-accent');
    btn.classList.add('btn-ghost');
  }
}

async function startContinuousScan() {
  if (_scanState.continuous) return;
  _scanState.continuous = true;
  const runId = ++_scanState.continuousRunId;
  updateContinuousButton();
  setScanAlert('Scan continu actif : présentez une carte dans le cadre.', 'success');
  setScanDebug([]);
  document.getElementById('foundCardsArea').innerHTML = '';

  try {
    while (_scanState.continuous && runId === _scanState.continuousRunId) {
      const matched = await handleCapture('camera', {
        silentNoMatch: true,
        keepFoundArea: true,
      });
      if (matched) {
        playBeep(1200, 180);
        navigator.vibrate?.([120]);
        _scanState.continuous = false;
        updateContinuousButton();
        return;
      }
      await sleep(350);
    }
  } finally {
    if (runId === _scanState.continuousRunId) {
      _scanState.continuous = false;
      updateContinuousButton();
    }
  }
}

function stopContinuousScan() {
  _scanState.continuous = false;
  _scanState.continuousRunId += 1;
  updateContinuousButton();
  setScanAlert('Scan continu arrêté.', 'success');
}

function toggleContinuousScan() {
  if (_scanState.continuous) {
    stopContinuousScan();
  } else {
    startContinuousScan();
  }
}

function setScannerCameraVisible(visible) {
  const cameraArea = document.getElementById('scanCameraArea');
  if (!cameraArea) return;
  cameraArea.style.display = visible ? '' : 'none';
}

function restartScannerCapture() {
  stopWantedCelebration();
  setScannerCameraVisible(true);
  setScanAlert('');
  setScanDebug([]);
  const area = document.getElementById('foundCardsArea');
  if (area) area.innerHTML = '';
  startContinuousScan();
}

function renderScanner() {
  document.getElementById('app').innerHTML = `
    <div class="app">
      <div class="page">
        <div class="page-header"><h1>📷 Scanner</h1></div>
        <div id="scanCameraArea"></div>
        <div id="scanAlerts"></div>
        <div id="scanDebug"></div>
        <div id="foundCardsArea"></div>
        <div style="padding:12px;border-top:1px solid var(--border);margin-top:8px">
          <p style="font-size:.75rem;color:var(--text-muted);margin:0 0 8px">Saisie manuelle</p>
          <div style="display:flex;gap:8px">
            <input class="search-input" id="manualNum" type="number" placeholder="N° carte" min="1" style="border-radius:8px" />
            <input class="search-input" id="manualSet" type="number" placeholder="Set" min="1" style="border-radius:8px;width:68px" />
            <button class="btn btn-ghost" id="manualLookupBtn" style="flex-shrink:0">Chercher</button>
          </div>
        </div>
      </div>
      ${navHTML('scanner')}
    </div>`;

  const cameraArea = document.getElementById('scanCameraArea');
  loadScannerSettings().then(() => navigator.mediaDevices?.getUserMedia({
    video: { facingMode: { ideal: 'environment' }, width: { ideal: 1920 }, height: { ideal: 1080 } }
  }))
    .then(stream => {
      _cameraStream = stream;
      cameraArea.innerHTML = `
        <div class="scanner-container">
          <video id="scanVideo" class="scanner-video" autoplay playsinline muted></video>
          <div class="scanner-overlay">
            <div class="scanner-card-frame">
              <span class="scanner-corner tl"></span>
              <span class="scanner-corner tr"></span>
              <span class="scanner-corner bl"></span>
              <span class="scanner-corner br"></span>
            </div>
          </div>
        </div>
        <div style="padding:14px 12px;display:flex;flex-direction:column;gap:8px">
          <button class="btn btn-accent btn-full" id="captureBtn">📸 Scanner depuis la caméra</button>
          <button class="btn btn-ghost btn-full" id="scanContinuousBtn">▶ Démarrer le scan continu</button>
        </div>`;
      document.getElementById('scanVideo').srcObject = stream;
      document.getElementById('captureBtn').addEventListener('click', () => handleCapture('camera'));
      document.getElementById('scanContinuousBtn').addEventListener('click', toggleContinuousScan);
      updateContinuousButton();
      startContinuousScan();
    })
    .catch(err => {
      // Caméra indisponible : message d'erreur
      cameraArea.innerHTML = `
        <div class="alert alert-warning" style="margin:12px">Caméra indisponible (${esc(err.message)})</div>`;
    });

  document.getElementById('manualLookupBtn').addEventListener('click', handleManualLookup);
  document.getElementById('manualNum').addEventListener('keydown', e => { if (e.key === 'Enter') handleManualLookup(); });
  document.getElementById('manualSet').addEventListener('keydown', e => { if (e.key === 'Enter') handleManualLookup(); });
}

async function handleCapture(mode, options = {}) {
  const silentNoMatch = !!options.silentNoMatch;
  const keepFoundArea = !!options.keepFoundArea;
  if (_scanState.scanning) return false;
  _scanState.scanning = true;
  if (!silentNoMatch) setScanAlert('');
  setScanDebug([]);
  if (!keepFoundArea) document.getElementById('foundCardsArea').innerHTML = '';

  // Désactiver les deux boutons pendant le scan
  const camBtn = document.getElementById('captureBtn');
  if (camBtn) camBtn.disabled = true;
  const activeBtn = camBtn;

  try {
    // Étape 1 — Acquisition de l'image (caméra ou fichier)
    if (activeBtn) activeBtn.innerHTML = `<span class="spinner" style="width:18px;height:18px;border-width:2px"></span> Capture…`;
    let cardCanvas;
    let sourceLabel;
    const video = document.getElementById('scanVideo');
    if (!video || !video.videoWidth) throw new Error('Flux vidéo indisponible. Vérifiez que la caméra est active.');
    cardCanvas = cropToCardFrame(video);
    sourceLabel = `📸 Caméra (${cardCanvas.width}×${cardCanvas.height} px)`;

    const zoneRaw = extractCodeZone(cardCanvas);

    // Étape 2 — 4 variantes de prétraitement pour maximiser la détection
    const canvC = otsuThreshold(toGrayscale(cloneCanvas(zoneRaw)));
    const variants = [
      { label: 'naturel',      canvas: zoneRaw },
      { label: 'inversé',      canvas: invertCanvas(zoneRaw) },
      { label: 'Otsu',         canvas: canvC },
      { label: 'Otsu+inversé', canvas: invertCanvas(cloneCanvas(canvC)) },
    ];

    // Étape 3 — OCR : un seul worker v5 pour toutes les variantes
    if (activeBtn) activeBtn.innerHTML = `<span class="spinner" style="width:18px;height:18px;border-width:2px"></span> Initialisation OCR…`;
    const worker = await createTessWorker();
    const debugLines = [
      sourceLabel,
      `Zone extraite : ${zoneRaw.width}×${zoneRaw.height} px`,
    ];
    let parsed = null;
    try {
      for (const { label, canvas } of variants) {
        if (activeBtn) activeBtn.innerHTML = `<span class="spinner" style="width:18px;height:18px;border-width:2px"></span> OCR [${label}]…`;
        try {
          const blob = await canvasToBlob(canvas);
          const result = await worker.recognize(blob);
          const raw  = result.data.text.trim().replace(/\n/g, ' ');
          const conf = Math.round(result.data.confidence);
          const p    = parseCardCode(raw);
          const info = p
            ? `✔ #${p.cardNum}/${p.total}  lang=${p.lang ?? '?'}  set=${p.setNum ?? '?'}`
            : '✘ non reconnu';
          debugLines.push(`[${label}] conf=${conf}%   "${raw}"   →   ${info}`);
          if (!parsed && p) parsed = p;
        } catch (ocrErr) {
          const errMsg = ocrErr instanceof Error ? ocrErr.message : String(ocrErr ?? 'erreur inconnue');
          debugLines.push(`[${label}] ❌ Erreur Tesseract : ${errMsg}`);
        }
      }
    } finally {
      try { await worker.terminate(); } catch { /* ignore */ }
    }

    // Panneau debug toujours affiché pour faciliter le diagnostic
    setScanDebug(debugLines);

    if (!parsed) {
      if (!silentNoMatch) {
        setScanAlert(
          'Code illisible. Vérifiez : éclairage suffisant, carte bien centrée dans le cadre, code "N/TOTAL • FR • N" visible net en bas-gauche. Consultez le détail OCR ci-dessous.',
          'error'
        );
      }
      return false;
    }

    // Étape 4 — Recherche de la carte en base
    if (activeBtn) activeBtn.innerHTML = `<span class="spinner" style="width:18px;height:18px;border-width:2px"></span> Recherche carte #${parsed.cardNum}…`;
    const cards = await api.lookupCard(parsed.cardNum, undefined);
    if (cards.length === 0) {
      if (!silentNoMatch) {
        setScanAlert(`Aucune carte #${parsed.cardNum} en base. Importez d'abord le catalogue via Administration.`, 'error');
      }
      return false;
    }
    let matchedCards = cards;
    if (parsed.setNum !== null && cards.length > 1) {
      const filtered = cards.filter(c => c.editionSetNumber === parsed.setNum);
      if (filtered.length > 0) matchedCards = filtered;
    }
    setScannerCameraVisible(false);
    setScanAlert('');
    setScanDebug([]);
    await handleFoundCards(matchedCards, parsed.cardNum);
    return true;
  } catch (e) {
    const msg = e instanceof Error ? e.message : String(e);
    if (!silentNoMatch) setScanAlert(`Erreur : ${msg}`, 'error');
    console.error('[Scanner] handleCapture :', e);
    return false;
  } finally {
    _scanState.scanning = false;
    if (camBtn) { camBtn.disabled = false; camBtn.textContent = '📸 Scanner depuis la caméra'; }
  }
}

async function handleManualLookup() {
  const numInput = document.getElementById('manualNum');
  const setInput = document.getElementById('manualSet');
  const num    = parseInt(numInput?.value, 10);
  const setNum = parseInt(setInput?.value, 10) || null;
  if (!num || num < 1) { setScanAlert('Numéro de carte invalide.', 'error'); return; }
  setScanAlert('');
  setScanDebug([]);
  document.getElementById('foundCardsArea').innerHTML = '';
  try {
    const cards = await api.lookupCard(num, undefined);
    let matchedCards = cards;
    if (setNum && cards.length > 1) {
      const filtered = cards.filter(c => c.editionSetNumber === setNum);
      if (filtered.length > 0) matchedCards = filtered;
    }
    await handleFoundCards(matchedCards, num);
  } catch (e) {
    setScanAlert(`Erreur : ${e.message}`, 'error');
  }
}

async function handleFoundCards(cards, num) {
  if (cards.length === 0) {
    setScanAlert(`Carte #${num} non trouvée. Importez d'abord le catalogue via Administration.`, 'error');
  } else if (cards.length === 1) {
    renderCardConfirmation(cards[0]);
  } else {
    renderFoundCards(cards);
  }
}

async function autoAddCard(card, foil = false) {
  const quantity = foil ? 0 : 1;
  const foilQuantity = foil ? 1 : 0;
  const updated = await api.addToCollection(card.id, quantity, foilQuantity);
  collState.cards = collState.cards.map(c => c.id === updated.id ? updated : c);
  setScanAlert(`✓ "${updated.name}" quantité mise à jour (×${updated.quantity + updated.foilQuantity})`, 'success');
  playBeep(880, 200);
  navigator.vibrate?.([100, 50, 100]);
  // Rafraîchit le cache récents (mis à jour à la prochaine visite de l'onglet Récents)
  api.getRecentCards(recentLimit).then(cards => { recentCardsState = cards || []; }).catch(() => {});
  return updated;
}

// Affiche la carte trouvée avec une image et un bouton de confirmation avant ajout.
function renderCardConfirmation(card) {
  const area = document.getElementById('foundCardsArea');
  if (!area) return;
  setScanAlert('');
  setScanDebug([]);
  if (card.wanted) celebrateWantedCardScan();
  const isNewCard = !card.owned;
  const showWanted = card.wanted && !card.owned;
  const imgHtml = card.imageUrl
    ? `<img src="${esc(card.imageUrl)}" alt="" style="width:110px;border-radius:8px;flex-shrink:0;box-shadow:0 4px 12px rgba(0,0,0,.4)${showWanted ? ';border:2px solid #d4af37' : ''}" onerror="this.style.display='none'" />`
    : `<div style="width:110px;height:154px;border-radius:8px;background:var(--bg-card2);flex-shrink:0;display:flex;align-items:center;justify-content:center;font-size:2.5rem${showWanted ? ';border:2px solid #d4af37' : ''}">🃏</div>`;
  const newCardBanner = isNewCard
    ? `<div style="background:#d32f2f;color:#fff;font-size:.68rem;font-weight:800;letter-spacing:.4px;text-transform:uppercase;padding:4px 8px;border-radius:6px;margin-bottom:6px;text-align:center">Nouvelle carte</div>`
    : '';
  const ownedQty = card.owned ? ((card.quantity || 0) + (card.foilQuantity || 0)) : 0;
  const ownedInfo = `<div style="font-size:.8rem;color:var(--text-muted);margin-top:6px">En collection : <strong style="color:var(--text)">×${ownedQty}</strong></div>`;
  area.innerHTML = `
    <div style="padding:12px">
      <h3 style="font-size:.85rem;color:var(--text-muted);text-align:center;margin-bottom:12px;text-transform:uppercase;letter-spacing:.5px">Carte identifiée — confirmer ?</h3>
      <div style="display:flex;gap:14px;align-items:flex-start">
        <div style="width:110px;flex-shrink:0">
          ${newCardBanner}
          ${imgHtml}
        </div>
        <div style="flex:1">
          <div style="display:flex;align-items:flex-start;justify-content:space-between;gap:8px">
            <div style="font-weight:800;font-size:1rem;line-height:1.3">${esc(card.name)}</div>
            <button class="wanted-toggle${card.wanted ? ' active' : ''}" id="confirmWantedToggleBtn" title="Marquer comme voulue">${card.wanted ? '⭐' : '☆'}</button>
          </div>
          <div style="font-size:.78rem;color:var(--primary-light);font-weight:700;margin-top:4px">${esc(card.editionCode)}</div>
          <div style="font-size:.78rem;color:var(--text-muted);margin-top:2px">#${esc(String(card.cardNumber))} · ${esc(card.rarity)}</div>
          ${ownedInfo}
          ${priceMetadataHTML(card)}
        </div>
      </div>
      <div style="display:flex;gap:8px;margin-top:16px;flex-direction:column">
        <div style="display:flex;gap:8px">
          <button class="btn btn-accent btn-full" id="confirmAddRegularBtn">◇ Ajouter normal</button>
          <button class="btn btn-ghost btn-full" id="confirmAddFoilBtn">✦ Ajouter foil</button>
        </div>
        <div style="display:flex;gap:8px">
          <button class="btn btn-ghost" id="restartScanBtn" style="flex-shrink:0;padding:0 14px">↻ Recommencer</button>
        </div>
      </div>
    </div>`;
  document.getElementById('confirmAddRegularBtn').addEventListener('click', async () => {
    await autoAddCard(card, false);
    restartScannerCapture();
  });
  document.getElementById('confirmAddFoilBtn').addEventListener('click', async () => {
    await autoAddCard(card, true);
    restartScannerCapture();
  });
  document.getElementById('restartScanBtn').addEventListener('click', restartScannerCapture);
  document.getElementById('confirmWantedToggleBtn').addEventListener('click', async () => {
    const updated = await api.setWanted(card.id, !card.wanted);
    card.wanted = updated.wanted;
    renderCardConfirmation(card);
  });
}

// Overlay non-bloquant de confettis, déclenché à la reconnaissance d'une carte voulue.
// Tourne en boucle jusqu'à stopWantedCelebration() (relance du scan).
let _wantedCelebrationOverlay = null;

function celebrateWantedCardScan() {
  stopWantedCelebration();
  const overlay = document.createElement('div');
  overlay.className = 'confetti-overlay';
  const colors = ['#d4af37', '#ff6f61', '#4fc3f7', '#81c784', '#ba68c8', '#ffd54f'];
  for (let i = 0; i < 40; i++) {
    const piece = document.createElement('span');
    piece.className = 'confetti-piece';
    piece.style.left = `${Math.random() * 100}%`;
    piece.style.background = colors[i % colors.length];
    piece.style.animationDelay = `${Math.random() * 1.6}s`;
    piece.style.animationDuration = `${1.1 + Math.random() * 0.6}s`;
    overlay.appendChild(piece);
  }
  document.body.appendChild(overlay);
  _wantedCelebrationOverlay = overlay;
}

function stopWantedCelebration() {
  if (_wantedCelebrationOverlay) {
    _wantedCelebrationOverlay.remove();
    _wantedCelebrationOverlay = null;
  }
}


function renderFoundCards(cards) {
  const area = document.getElementById('foundCardsArea');
  if (!area) return;
  setScanAlert('');
  setScanDebug([]);
  area.innerHTML = `<div style="padding:0 12px">
    <h3 style="font-size:.9rem;margin-bottom:10px">Plusieurs cartes trouvées — choisissez :</h3>
    ${cards.map(c => `
      <button class="btn btn-ghost btn-full" style="margin-bottom:8px;justify-content:flex-start;gap:12px" data-cardid="${c.id}">
        ${c.imageUrl ? `<img src="${esc(c.imageUrl)}" alt="" style="width:36px;border-radius:4px" onerror="this.style.display='none'" />` : ''}
        <div style="text-align:left">
          <div style="font-weight:700">${c.editionSetNumber ? `Set ${c.editionSetNumber} · ` : ''}#${esc(c.cardNumber)} — ${esc(c.name)}</div>
          <div style="font-size:.75rem;color:var(--text-muted)">${esc(c.editionCode)} • ${esc(c.rarity)}</div>
        </div>
      </button>`).join('')}
    <button class="btn btn-ghost btn-full" id="restartScanBtnList" style="margin-top:8px">↻ Recommencer</button>
  </div>`;
  area.querySelectorAll('[data-cardid]').forEach(btn => {
    btn.addEventListener('click', () => {
      const card = cards.find(c => c.id === parseInt(btn.dataset.cardid));
      renderCardConfirmation(card);
    });
  });
  document.getElementById('restartScanBtnList')?.addEventListener('click', restartScannerCapture);
}

function setScanAlert(msg, type = 'success') {
  const el = document.getElementById('scanAlerts');
  if (!el) return;
  el.innerHTML = msg ? `<div class="alert alert-${type}" style="margin:0 12px 10px">${esc(msg)}</div>` : '';
}

// Affiche les lignes de debug OCR dans un panneau dépliable.
function setScanDebug(lines) {
  const el = document.getElementById('scanDebug');
  if (!el) return;
  if (!lines || lines.length === 0) { el.innerHTML = ''; return; }
  el.innerHTML = `
    <details style="margin:0 12px 8px;font-size:.72rem;font-family:monospace;background:var(--bg-card2);border-radius:8px;padding:8px 10px">
      <summary style="cursor:pointer;color:var(--text-muted);user-select:none">🔍 Détail OCR — cliquer pour afficher</summary>
      ${lines.map(r => `<div style="margin-top:4px;color:var(--text-muted);word-break:break-all">${esc(r)}</div>`).join('')}
    </details>`;
}

function playBeep(freq = 880, dur = 200) {
  try {
    const ctx = new (window.AudioContext || window.webkitAudioContext)();
    const osc = ctx.createOscillator();
    const gain = ctx.createGain();
    osc.connect(gain); gain.connect(ctx.destination);
    osc.frequency.value = freq;
    osc.start(); gain.gain.setValueAtTime(0.3, ctx.currentTime);
    gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + dur / 1000);
    osc.stop(ctx.currentTime + dur / 1000);
  } catch { /* Web Audio not available */ }
}

// ─── ADMIN ────────────────────────────────────────────────────────────────────

function startSyncPoll() {
  stopSyncPoll();
  _syncPolling = setInterval(async () => {
    try {
      const p = await api.getProgress();
      updateAdminProgress(p);
      if (!p.running) {
        stopSyncPoll();
        setSyncBusy(false);
      }
    } catch { stopSyncPoll(); }
  }, 900);
}

function stopSyncPoll() {
  if (_syncPolling) { clearInterval(_syncPolling); _syncPolling = null; }
}

function updateAdminProgress(p) {
  const box = document.getElementById('adminProgressBox');
  if (!box) return;
  const visible = p.running || p.phase === 'done' || p.phase === 'error';
  box.style.display = visible ? 'block' : 'none';
  if (!visible) return;

  const PHASE = {
    downloading: '⬇️ Téléchargement',
    parsing:     '📄 Analyse JSON',
    sync:        '🔄 Synchronisation',
    hashing:     '🔍 Calcul empreintes',
    companion_parsing: '📄 Analyse Companion',
    companion_import:  '📥 Import Companion',
    done:        '✅ Terminé',
    error:       '❌ Erreur',
  };
  const pct = p.percent ?? 0;
  const isIndeterminate = p.running && p.total === 0;
  const color = p.error ? 'var(--danger)' : p.phase === 'done' ? 'var(--success)' : 'var(--accent)';
  const barFill = isIndeterminate
    ? `<div style="height:100%;border-radius:10px;background:${color};width:30%;animation:syncIndeterminate 1.2s ease-in-out infinite"></div>`
    : `<div class="progress-bar-fill" style="width:${pct}%;background:${color}"></div>`;

  box.innerHTML = `
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:8px">
      <span style="font-weight:700;font-size:.9rem">${PHASE[p.phase] || p.phase}</span>
      <span style="font-size:.9rem;font-weight:700;color:${color}">${p.total > 0 ? pct + '%' : (p.running ? '…' : '')}</span>
    </div>
    <div class="progress-bar" style="overflow:hidden">${barFill}</div>
    <div style="display:flex;justify-content:space-between;margin-top:6px">
      <span style="font-size:.78rem;color:var(--text-muted)">${esc(p.message)}</span>
      ${p.total > 0 ? `<span style="font-size:.78rem;color:var(--text-muted);font-weight:600">${p.current}\u202f/\u202f${p.total}</span>` : ''}
    </div>`;
}

function setSyncBusy(busy) {
  ['syncUrlBtn', 'pricingRunBtn'].forEach(id => {
    const el = document.getElementById(id);
    if (el) el.disabled = busy;
  });
  ['lorcajsonFile', 'companionImportFile', 'companionMergeMode'].forEach(id => {
    const el = document.getElementById(id);
    if (el) el.disabled = busy;
  });
}

function renderAdmin() {
  document.getElementById('app').innerHTML = `
    <div class="app">
      <div class="page">
        <div class="page-header">
          <h1>⚙️ Administration</h1>
          <button class="btn btn-ghost" id="logoutBtn" style="margin-left:auto;font-size:.8rem;padding:6px 12px">Déconnexion</button>
        </div>
        <div id="adminContent" style="padding:12px">${loadingHTML()}</div>
      </div>
      ${navHTML('admin')}
    </div>`;

  document.getElementById('logoutBtn').addEventListener('click', () => {
    removeToken();
    collState.editions = [];
    collState.cards = [];
    _fingerprintsCache = null;
    stopSyncPoll();
    navigate('login');
  });

  Promise.all([
    api.getSettings(),
    api.getLorcaJsonUrl(),
    api.getProgress(),
    api.getEditions(),
    api.getPricingStatus().catch(() => null),
  ]).then(([settings, urlData, progressData, editions, pricingStatus]) => {
    const content = document.getElementById('adminContent');
    if (!content) return;
    const currentUrl = urlData.url || 'https://lorcanajson.org/files/current/fr/allCards.json';
    const settingVal = (key, fallback = '') => {
      const row = settings.find(s => s.settingKey === key);
      return row && row.settingValue != null ? String(row.settingValue) : fallback;
    };
    const pricingSyncEnabled = settingVal('pricing_sync_enabled', 'true');
    const pricingLogHighPriceEnabled = settingVal('pricing_log_high_price_enabled', 'true');
    const pricingLogUnresolvedMappingEnabled = settingVal('pricing_log_unresolved_mapping_enabled', 'false');
    const pricingDailyHardLimit = settingVal('pricing_daily_hard_limit', settingVal('pricing_daily_budget', '100'));
    const pricingDailySafetyMargin = settingVal('pricing_daily_safety_margin', '5');
    const pricingMinuteLimit = settingVal('pricing_minute_limit', '30');
    const pricingProviderHost = settingVal('pricing_provider_host', 'lorcana-api-by-tcggo.p.rapidapi.com');
    const pricingProviderEpisodesPath = settingVal('pricing_provider_episodes_path', '/episodes');
    const pricingProviderEpisodeCardsPathTemplate = settingVal('pricing_provider_episode_cards_path_template', '/episodes/{episodeId}/cards');
    const pricingProviderCurrency = settingVal('pricing_provider_currency', 'EUR');
    const pricingProviderApiKey = settingVal('pricing_provider_api_key', '');
    const pricingScheduleCron = settingVal('pricing_schedule_cron', '0 0 2 * * *');

    const statsSetsSetting = settings.find(s => s.settingKey === 'stats_enabled_sets');
    const savedStatsSetIds = statsSetsSetting
      ? (statsSetsSetting.settingValue || '').split(',').map(v => parseInt(v.trim(), 10)).filter(v => !isNaN(v))
      : editions.map(e => e.id);
    const selectedSetIds = new Set(savedStatsSetIds);

    content.innerHTML = `
      <!-- Étape 1 : Synchronisation -->
      <div class="edition-item" style="margin-bottom:12px">
        <div style="display:flex;align-items:center;gap:8px;margin-bottom:14px">
          <span style="background:var(--accent);color:#fff;border-radius:50%;width:24px;height:24px;display:inline-flex;align-items:center;justify-content:center;font-size:.75rem;font-weight:700;flex-shrink:0">1</span>
          <h3 style="margin:0">Synchronisation des cartes</h3>
        </div>

        <div style="margin-bottom:12px">
          <label style="font-size:.78rem;color:var(--text-muted);font-weight:700;text-transform:uppercase;letter-spacing:.5px;display:block;margin-bottom:6px">Depuis une URL LorcaJson</label>
          <div style="display:flex;gap:6px;margin-bottom:8px">
            <input type="url" id="lorcajsonUrl" value="${esc(currentUrl)}"
              placeholder="https://lorcanajson.org/files/current/fr/allCards.json"
              style="flex:1;border-radius:8px;font-size:.85rem" />
            <button class="btn btn-ghost" id="saveUrlBtn" style="flex-shrink:0;padding:8px 12px" title="Sauvegarder l'URL">💾</button>
          </div>
          <button class="btn btn-accent btn-full" id="syncUrlBtn">🔄 Importer depuis l'URL</button>
          <div id="urlSaveResult" style="margin-top:4px"></div>
        </div>

        <div>
          <label style="font-size:.78rem;color:var(--text-muted);font-weight:700;text-transform:uppercase;letter-spacing:.5px;display:block;margin-bottom:6px">Depuis un fichier local</label>
          <label class="btn btn-ghost btn-full" style="cursor:pointer">
            📁 Choisir le fichier allCards.json
            <input type="file" id="lorcajsonFile" accept=".json" style="display:none" />
          </label>
        </div>
      </div>

      <!-- Progression -->
      <div id="adminProgressBox" class="edition-item" style="margin-bottom:12px;display:none"></div>

      <!-- Pricing sync -->
      <div class="edition-item" style="margin-bottom:12px">
        <h3 style="margin-bottom:12px">💶 Synchronisation des prix</h3>
        <p style="font-size:.85rem;color:var(--text-muted);margin-bottom:12px">
          Limites strictes: jamais plus de 100 appels/jour et jamais plus de 30 appels/minute. Priorité: sans prix, prix > 7 jours, puis le reste.
        </p>

        <div style="display:grid;grid-template-columns:1fr 1fr;gap:8px;margin-bottom:8px">
          <label style="font-size:.84rem;color:var(--text-muted)">
            <span style="display:block;margin-bottom:4px">Sync activée</span>
            <select id="pricingSyncEnabled" style="width:100%;border-radius:8px;padding:8px 10px;background:var(--bg-input,var(--bg-card2));border:1px solid var(--border);color:var(--text)">
              <option value="true" ${pricingSyncEnabled === 'true' ? 'selected' : ''}>Oui</option>
              <option value="false" ${pricingSyncEnabled === 'false' ? 'selected' : ''}>Non</option>
            </select>
          </label>
          <label style="font-size:.84rem;color:var(--text-muted)">
            <span style="display:block;margin-bottom:4px">Hard cap journalier (max 100)</span>
            <input id="pricingDailyHardLimit" type="number" min="0" max="100" value="${esc(pricingDailyHardLimit)}"
              style="width:100%;border-radius:8px;padding:8px 10px" />
          </label>
        </div>

        <div style="display:grid;grid-template-columns:1fr 1fr;gap:8px;margin-bottom:8px">
          <label style="font-size:.84rem;color:var(--text-muted)">
            <span style="display:block;margin-bottom:4px">Log "High market price"</span>
            <select id="pricingLogHighPriceEnabled" style="width:100%;border-radius:8px;padding:8px 10px;background:var(--bg-input,var(--bg-card2));border:1px solid var(--border);color:var(--text)">
              <option value="true" ${pricingLogHighPriceEnabled === 'true' ? 'selected' : ''}>Oui</option>
              <option value="false" ${pricingLogHighPriceEnabled === 'false' ? 'selected' : ''}>Non</option>
            </select>
          </label>
          <label style="font-size:.84rem;color:var(--text-muted)">
            <span style="display:block;margin-bottom:4px">Log diagnostic "Unresolved mapping"</span>
            <select id="pricingLogUnresolvedMappingEnabled" style="width:100%;border-radius:8px;padding:8px 10px;background:var(--bg-input,var(--bg-card2));border:1px solid var(--border);color:var(--text)">
              <option value="true" ${pricingLogUnresolvedMappingEnabled === 'true' ? 'selected' : ''}>Oui</option>
              <option value="false" ${pricingLogUnresolvedMappingEnabled === 'false' ? 'selected' : ''}>Non</option>
            </select>
          </label>
        </div>

        <div style="display:grid;grid-template-columns:1fr 1fr;gap:8px;margin-bottom:8px">
          <label style="font-size:.84rem;color:var(--text-muted)">
            <span style="display:block;margin-bottom:4px">Marge de sécurité quotidienne</span>
            <input id="pricingDailySafetyMargin" type="number" min="0" max="100" value="${esc(pricingDailySafetyMargin)}"
              style="width:100%;border-radius:8px;padding:8px 10px" />
          </label>
          <label style="font-size:.84rem;color:var(--text-muted)">
            <span style="display:block;margin-bottom:4px">Limite minute (max 30)</span>
            <input id="pricingMinuteLimit" type="number" min="1" max="30" value="${esc(pricingMinuteLimit)}"
              style="width:100%;border-radius:8px;padding:8px 10px" />
          </label>
        </div>

        <div style="display:grid;grid-template-columns:1fr 1fr;gap:8px;margin-bottom:8px">
          <label style="font-size:.84rem;color:var(--text-muted)">
            <span style="display:block;margin-bottom:4px">Provider host</span>
            <input id="pricingProviderHost" type="text" value="${esc(pricingProviderHost)}"
              style="width:100%;border-radius:8px;padding:8px 10px" />
          </label>
          <label style="font-size:.84rem;color:var(--text-muted)">
            <span style="display:block;margin-bottom:4px">Provider episodes path</span>
            <input id="pricingProviderEpisodesPath" type="text" value="${esc(pricingProviderEpisodesPath)}"
              style="width:100%;border-radius:8px;padding:8px 10px" />
          </label>
        </div>

        <div style="display:grid;grid-template-columns:1fr;gap:8px;margin-bottom:8px">
          <label style="font-size:.84rem;color:var(--text-muted)">
            <span style="display:block;margin-bottom:4px">Provider episode cards path template</span>
            <input id="pricingProviderEpisodeCardsPathTemplate" type="text" value="${esc(pricingProviderEpisodeCardsPathTemplate)}"
              style="width:100%;border-radius:8px;padding:8px 10px" />
          </label>
        </div>

        <div style="display:grid;grid-template-columns:1fr 1fr;gap:8px;margin-bottom:8px">
          <label style="font-size:.84rem;color:var(--text-muted)">
            <span style="display:block;margin-bottom:4px">Devise cible</span>
            <input id="pricingProviderCurrency" type="text" value="${esc(pricingProviderCurrency)}"
              style="width:100%;border-radius:8px;padding:8px 10px" />
          </label>
          <label style="font-size:.84rem;color:var(--text-muted)">
            <span style="display:block;margin-bottom:4px">Cron quotidien</span>
            <input id="pricingScheduleCron" type="text" value="${esc(pricingScheduleCron)}"
              placeholder="0 0 2 * * *"
              style="width:100%;border-radius:8px;padding:8px 10px" />
          </label>
        </div>

        <div style="display:grid;grid-template-columns:1fr;gap:8px;margin-bottom:8px">
          <label style="font-size:.84rem;color:var(--text-muted)">
            <span style="display:block;margin-bottom:4px">API key RapidAPI</span>
            <input id="pricingProviderApiKey" type="password" value="${esc(pricingProviderApiKey)}"
              placeholder="Renseigner la clé provider"
              style="width:100%;border-radius:8px;padding:8px 10px" />
          </label>
        </div>

        <div style="display:grid;grid-template-columns:1fr 1fr 1fr;gap:8px;margin-bottom:8px">
          <button class="btn btn-accent" id="pricingSaveBtn">💾 Enregistrer</button>
          <button class="btn btn-ghost" id="pricingRefreshStatusBtn">🔄 Rafraîchir statut</button>
          <button class="btn btn-ghost" id="pricingRunBtn">▶️ Lancer maintenant</button>
        </div>

        <div style="display:flex;gap:8px;align-items:center;margin-bottom:8px">
          <label style="font-size:.84rem;color:var(--text-muted);margin:0">Max appels run manuel</label>
          <input id="pricingMaxAttempts" type="number" min="1" placeholder="illimité"
            style="width:120px;border-radius:8px;padding:6px 8px" />
        </div>

        <div id="pricingStatusBox" style="font-size:.84rem;color:var(--text-muted);background:var(--bg-card2);border:1px solid var(--border);border-radius:10px;padding:10px;margin-bottom:8px"></div>
        <div id="pricingSettingsResult"></div>
      </div>

      <!-- Simulation import prix (temporaire, à retirer après validation) -->
      <div class="edition-item" style="margin-bottom:12px;border:1px dashed var(--accent)">
        <h3 style="margin-bottom:8px">🧪 Simulation import prix (temporaire)</h3>
        <p style="font-size:.85rem;color:var(--text-muted);margin-bottom:12px">
          Collez ici la réponse JSON brute de l'API pricing (par exemple <code>/episodes/{id}/cards</code>) pour appliquer
          les prix aux cartes locales <strong>sans appeler l'API</strong> et sans consommer de budget d'appels.
        </p>
        <textarea id="pricingSimulateJson" rows="8" placeholder='{"data":[{"card_number":143,"episode":{"code":"11WSP"},"prices":{...}}]}'
          style="width:100%;border-radius:8px;font-family:monospace;font-size:.8rem;padding:8px"></textarea>
        <button class="btn btn-accent btn-full" id="pricingSimulateBtn" style="margin-top:8px">▶️ Appliquer les prix depuis ce JSON</button>
        <div id="pricingSimulateResult" style="margin-top:8px"></div>
      </div>

      <!-- Sauvegarde / Restauration complètes -->
      <div class="edition-item" style="margin-bottom:12px">
        <h3 style="margin-bottom:12px">💾 Sauvegarde &amp; Restauration complètes</h3>
        <p style="font-size:.85rem;color:var(--text-muted);margin-bottom:12px">
          Exporte <strong>tout</strong> : catalogue de cartes, collection et paramètres (dont les sets suivis dans Stats).
          Permet une restauration intégrale sur une nouvelle instance.
        </p>
        <button class="btn btn-accent btn-full" id="fullBackupBtn" style="margin-bottom:8px">⬇️ Télécharger la sauvegarde complète</button>
        <label class="btn btn-ghost btn-full" style="cursor:pointer;margin-bottom:0">
          🔄 Restaurer depuis une sauvegarde
          <input type="file" id="fullRestoreFile" accept=".json" style="display:none" />
        </label>
        <div id="fullBackupResult" style="margin-top:8px"></div>
      </div>

      <!-- Companion Import -->
      <div class="edition-item" style="margin-bottom:12px">
        <h3 style="margin-bottom:12px">Import depuis Lorcana Companion</h3>
        <p style="font-size:.85rem;color:var(--text-muted);margin-bottom:12px">
          Importez un export Companion (clé <strong>OwnedCardQuantitiesV2</strong>). Les quantités Regular et Foiled sont traitées séparément, en mode fusion ou remplacement.
        </p>
        <label style="display:flex;align-items:center;gap:8px;font-size:.85rem;color:var(--text-muted);margin:0 0 10px;cursor:pointer">
          <input type="checkbox" id="companionMergeMode" checked style="accent-color:var(--accent)" />
          Mode fusion : ajouter aux quantités existantes (sinon remplacement)
        </label>
        <label class="btn btn-ghost btn-full" style="cursor:pointer;margin-bottom:0">
          📥 Importer un export Companion (.json)
          <input type="file" id="companionImportFile" accept=".json,application/json" style="display:none" />
        </label>
        <div id="companionImportResult" style="margin-top:8px"></div>
      </div>

      <!-- Stats set filter -->
      <div class="edition-item" style="margin-bottom:12px">
        <h3 style="margin-bottom:12px">Sets suivis dans Statistiques</h3>
        <p style="font-size:.85rem;color:var(--text-muted);margin-bottom:12px">
          Cochez les sets à inclure dans les calculs de l'onglet Stats. Ce réglage n'affecte pas la collection.
        </p>
        <div id="statsSetChecklist" style="display:flex;flex-direction:column;gap:8px;max-height:220px;overflow:auto;padding:2px 0 8px"></div>
        <div style="display:flex;gap:8px">
          <button class="btn btn-ghost" id="statsSetsAllBtn" style="flex:1">Tout cocher</button>
          <button class="btn btn-ghost" id="statsSetsNoneBtn" style="flex:1">Tout décocher</button>
        </div>
        <button class="btn btn-accent btn-full" id="saveStatsSetsBtn" style="margin-top:8px">Enregistrer les sets suivis</button>
        <div id="statsSetsResult" style="margin-top:8px"></div>
      </div>

      <!-- API Keys -->
      <details id="apiKeysSection" class="edition-item" style="margin-bottom:12px">
        <summary style="cursor:pointer;display:flex;align-items:center;gap:8px;padding:4px 0;list-style:none;user-select:none">
          <span style="font-size:1.15rem">🔑</span>
          <h3 style="margin:0;flex:1">Clés API</h3>
          <span id="apiKeysSummaryCount" style="font-size:.8rem;color:var(--text-muted)"></span>
          <svg style="width:18px;height:18px;flex-shrink:0;transition:transform .2s;transform:rotate(0deg)" id="apiKeyChevron" viewBox="0 0 24 24" fill="currentColor"><path d="M7 10l5 5 5-5z"/></svg>
        </summary>

        <div style="margin-top:12px">
          <p style="font-size:.85rem;color:var(--text-muted);margin-bottom:12px">
            Générez des clés pour accéder à l'export de la collection via
            <code style="font-size:.8rem;background:var(--bg-card2);padding:2px 5px;border-radius:4px">GET /api/export?apiKey=…</code>
            sans authentification JWT.
          </p>

          <!-- Création -->
          <div style="display:flex;flex-direction:column;gap:8px;margin-bottom:14px;padding:10px;background:var(--bg-card2);border-radius:10px">
            <div style="font-size:.78rem;color:var(--text-muted);font-weight:700;text-transform:uppercase;letter-spacing:.5px">Nouvelle clé API</div>
            <input type="text" id="apiKeyName" placeholder="Nom de la clé (ex : Home Assistant)"
              style="border-radius:8px;font-size:.88rem" maxlength="80" />
            <div style="display:flex;gap:8px;align-items:center">
              <select id="apiKeyValidity" style="border-radius:8px;font-size:.88rem;flex:1;padding:8px 10px;background:var(--bg-input,var(--bg-card2));border:1px solid var(--border);color:var(--text)">
                <option value="7">7 jours</option>
                <option value="30" selected>30 jours</option>
                <option value="90">90 jours</option>
                <option value="180">180 jours</option>
                <option value="365">1 an</option>
                <option value="3650">10 ans</option>
              </select>
              <button class="btn btn-accent" id="createApiKeyBtn" style="flex-shrink:0">Générer</button>
            </div>
            <div id="apiKeyNewResult"></div>
          </div>

          <!-- Tableau des clés existantes -->
          <div id="apiKeysList"></div>
        </div>
      </details>

      <!-- Paramètres -->
      <div class="edition-item" style="margin-bottom:12px">
        <h3 style="margin-bottom:12px">Paramètres</h3>
        ${settings.map(s => `
          <div style="margin-bottom:10px">
            <div style="font-size:.75rem;color:var(--primary-light);font-weight:700;letter-spacing:.5px">${esc(s.settingKey)}</div>
            <div style="font-size:.85rem;color:var(--text-muted)">${esc(s.description)}</div>
            <div style="font-size:.9rem;font-weight:600;margin-top:2px">${esc(s.settingValue)}</div>
          </div>`).join('')}
      </div>

      <div class="edition-item">
        <h3 style="margin-bottom:8px">Informations</h3>
        <p style="font-size:.85rem;color:var(--text-muted);line-height:1.7">
          <strong style="color:var(--text)">Lorcalex</strong> — Gestionnaire de collection Lorcana (FR).<br/>
          Backend : Spring Boot + ${location.hostname === 'localhost' ? 'SQLite (local)' : 'PostgreSQL (Docker)'}<br/>
          Source cartes : <a href="https://lorcanajson.org" target="_blank" rel="noopener" style="color:var(--accent)">lorcanajson.org</a>
        </p>
      </div>`;

    // ── Initialise progress from server state ──────────────────────────────
    updateAdminProgress(progressData);
    if (progressData.running) {
      setSyncBusy(true);
      startSyncPoll();
    }

    function renderPricingStatus(status) {
      const box = document.getElementById('pricingStatusBox');
      if (!box) return;
      if (!status) {
        box.innerHTML = 'Statut pricing indisponible.';
        return;
      }
      const lines = [
        `Sync activée: ${status.syncEnabled ? 'oui' : 'non'}`,
        `Hard cap: ${status.dailyHardLimit ?? 0}`,
        `Marge sécurité: ${status.dailySafetyMargin ?? 0}`,
        `Budget effectif: ${status.effectiveDailyBudget ?? status.dailyBudget ?? 0}`,
        `Consommé: ${status.usedAttempts ?? 0}`,
        `Restant: ${status.remainingAttempts ?? 0}`,
        `Limite minute (effective): ${status.minuteLimit ?? 30}`,
        `Date usage: ${esc(status.usageDate ?? '')}`,
        `Cron configuré: ${esc(status.scheduleCron ?? '')}`,
        `Cron effectif: ${esc(status.scheduleEffectiveCron ?? '')}`,
        `Cron valide: ${status.scheduleValid ? 'oui' : 'non (fallback)'}`,
        `Prochain run: ${esc(status.scheduleNextRun ?? '')}`,
        `Dernier run planifié: ${esc(status.lastScheduledRunDate ?? '')}`,
        `Provider: ${esc(status.provider ?? '')}`,
        `Provider configuré: ${status.providerConfigured ? 'oui' : 'non'}`,
        `Episodes path: ${esc(status.providerEpisodesPath ?? '')}`,
        `Episode cards path: ${esc(status.providerEpisodeCardsPathTemplate ?? '')}`,
        `En cours: ${status.running ? 'oui' : 'non'}`,
        `Queue sans prix: ${status.queueWithoutPrice ?? 0}`,
        `Queue stale > 7j: ${status.queueStaleOver7Days ?? 0}`,
        `Queue avec prix: ${status.queueWithPrice ?? 0}`,
        `Curseur: ${esc(JSON.stringify(status.cursor || {}))}`,
        `Dernier stop: ${esc(status.lastStopReason ?? status.stopReason ?? '')}`,
      ];
      box.innerHTML = lines.map(line => `<div>${line}</div>`).join('');
    }

    async function refreshPricingStatus() {
      try {
        const status = await api.getPricingStatus();
        renderPricingStatus(status);
      } catch (err) {
        showAdminResult('pricingSettingsResult', { success: false, message: 'Erreur statut pricing : ' + err.message });
      }
    }

    renderPricingStatus(pricingStatus);

    document.getElementById('pricingRefreshStatusBtn').addEventListener('click', refreshPricingStatus);

    document.getElementById('pricingSaveBtn').addEventListener('click', async () => {
      const syncEnabled = document.getElementById('pricingSyncEnabled').value;
      const logHighPriceEnabled = document.getElementById('pricingLogHighPriceEnabled').value;
      const logUnresolvedMappingEnabled = document.getElementById('pricingLogUnresolvedMappingEnabled').value;
      const dailyHardLimit = document.getElementById('pricingDailyHardLimit').value.trim();
      const dailySafetyMargin = document.getElementById('pricingDailySafetyMargin').value.trim();
      const minuteLimit = document.getElementById('pricingMinuteLimit').value.trim();
      const providerHost = document.getElementById('pricingProviderHost').value.trim();
      const providerEpisodesPath = document.getElementById('pricingProviderEpisodesPath').value.trim();
      const providerEpisodeCardsPathTemplate = document.getElementById('pricingProviderEpisodeCardsPathTemplate').value.trim();
      const providerCurrency = document.getElementById('pricingProviderCurrency').value.trim();
      const scheduleCron = document.getElementById('pricingScheduleCron').value.trim();
      const providerApiKey = document.getElementById('pricingProviderApiKey').value.trim();

      try {
        await Promise.all([
          api.updateSetting('pricing_sync_enabled', syncEnabled),
          api.updateSetting('pricing_log_high_price_enabled', logHighPriceEnabled),
          api.updateSetting('pricing_log_unresolved_mapping_enabled', logUnresolvedMappingEnabled),
          api.updateSetting('pricing_daily_hard_limit', dailyHardLimit || '100'),
          api.updateSetting('pricing_daily_budget', dailyHardLimit || '100'),
          api.updateSetting('pricing_daily_safety_margin', dailySafetyMargin || '0'),
          api.updateSetting('pricing_minute_limit', minuteLimit || '30'),
          api.updateSetting('pricing_provider_host', providerHost),
          api.updateSetting('pricing_provider_episodes_path', providerEpisodesPath || '/episodes'),
          api.updateSetting('pricing_provider_episode_cards_path_template', providerEpisodeCardsPathTemplate || '/episodes/{episodeId}/cards'),
          api.updateSetting('pricing_provider_currency', providerCurrency),
          api.updateSetting('pricing_schedule_cron', scheduleCron || '0 0 2 * * *'),
          api.updateSetting('pricing_provider_api_key', providerApiKey),
        ]);
        showAdminResult('pricingSettingsResult', { success: true, message: 'Paramètres pricing enregistrés.' });
        refreshPricingStatus();
      } catch (err) {
        showAdminResult('pricingSettingsResult', { success: false, message: 'Erreur sauvegarde pricing : ' + err.message });
      }
    });

    document.getElementById('pricingRunBtn').addEventListener('click', async () => {
      const rawMax = document.getElementById('pricingMaxAttempts').value.trim();
      const maxAttempts = rawMax ? parseInt(rawMax, 10) : undefined;
      setSyncBusy(true);
      try {
        const result = await api.runPricingSync(Number.isFinite(maxAttempts) ? maxAttempts : undefined);
        const statusCounts = result.statusCounts && typeof result.statusCounts === 'object'
          ? Object.entries(result.statusCounts).map(([k, v]) => `${k}:${v}`).join(', ')
          : '';
        const details = [
          `Appels: ${result.attempted ?? 0}`,
          `Pages episodes: ${result.episodePagesProcessed ?? 0}`,
          `Pages cartes: ${result.episodeCardsPagesProcessed ?? 0}`,
          `Succès: ${result.successCount ?? 0}`,
          `Non résolues: ${result.unresolvedCount ?? 0}`,
          `Erreurs: ${result.errorCount ?? 0}`,
          `Restant: ${result.remainingAttempts ?? 0}`,
          `File sans prix: ${result.queueWithoutPrice ?? 0}`,
          `File stale > 7j: ${result.queueStaleOver7Days ?? 0}`,
          `File avec prix: ${result.queueWithPrice ?? 0}`,
          `Stop: ${result.stopReason ?? result.reasonCode ?? ''}`,
          statusCounts ? `Statuts: ${statusCounts}` : '',
        ].join(' | ');
        const defaultMessage = `Run pricing terminé. ${details}`;
        const isBudgetExhausted = result.reasonCode === 'BUDGET_EXHAUSTED' || result.reasonCode === 'BUDGET_EXHAUSTED_AFTER_ATTEMPTS';
        const isProviderConfigMissing = result.reasonCode === 'PROVIDER_CONFIG_MISSING';
        showAdminResult('pricingSettingsResult', {
          success: !!result.started,
          level: (isBudgetExhausted || isProviderConfigMissing) ? 'warning' : undefined,
          message: result.message ? `${result.message} (${details})` : defaultMessage
        });
        await refreshPricingStatus();
      } catch (err) {
        showAdminResult('pricingSettingsResult', { success: false, message: 'Erreur run pricing : ' + err.message });
      } finally {
        setSyncBusy(false);
      }
    });

    document.getElementById('pricingSimulateBtn').addEventListener('click', async () => {
      const rawJson = document.getElementById('pricingSimulateJson').value.trim();
      if (!rawJson) {
        showAdminResult('pricingSimulateResult', { success: false, message: 'Collez un JSON avant de lancer la simulation.' });
        return;
      }
      try {
        const result = await api.simulatePricingImport(rawJson);
        showAdminResult('pricingSimulateResult', {
          success: !!result.success,
          message: result.message || (result.success ? 'Import simulé appliqué.' : 'Echec de la simulation.')
        });
        if (result.success) await refreshPricingStatus();
      } catch (err) {
        showAdminResult('pricingSimulateResult', { success: false, message: 'Erreur simulation import : ' + err.message });
      }
    });

    // ── API Keys ──────────────────────────────────────────────────────────
    const apiKeysSection = document.getElementById('apiKeysSection');
    if (apiKeysSection) {
      apiKeysSection.addEventListener('toggle', () => {
        const chevron = document.getElementById('apiKeyChevron');
        if (chevron) chevron.style.transform = apiKeysSection.open ? 'rotate(180deg)' : 'rotate(0deg)';
        if (apiKeysSection.open) loadApiKeysList();
      });
    }

    document.getElementById('createApiKeyBtn').addEventListener('click', async () => {
      const name = document.getElementById('apiKeyName').value.trim();
      const validityDays = parseInt(document.getElementById('apiKeyValidity').value, 10);
      const resultEl = document.getElementById('apiKeyNewResult');
      if (!name) {
        resultEl.innerHTML = `<div class="alert alert-error" style="padding:6px 10px;font-size:.82rem">Le nom est obligatoire.</div>`;
        return;
      }
      try {
        const res = await api.createApiKey(name, validityDays);
        const key = res.key;
        resultEl.innerHTML = `
          <div class="alert alert-success" style="padding:8px 10px;font-size:.82rem">
            <div style="font-weight:700;margin-bottom:6px">✅ Clé créée ! Copiez-la maintenant, elle ne sera plus visible.</div>
            <div style="display:flex;gap:6px;align-items:center;flex-wrap:wrap">
              <code id="newApiKeyValue" style="background:var(--bg-card2);padding:4px 8px;border-radius:6px;font-size:.78rem;word-break:break-all;flex:1">${esc(key)}</code>
              <button class="btn btn-ghost" style="padding:4px 10px;font-size:.78rem;flex-shrink:0" id="copyNewApiKeyBtn">📋 Copier</button>
            </div>
          </div>`;
        document.getElementById('copyNewApiKeyBtn').addEventListener('click', () => {
          navigator.clipboard.writeText(key).then(() => {
            document.getElementById('copyNewApiKeyBtn').textContent = '✅ Copié';
            setTimeout(() => { const b = document.getElementById('copyNewApiKeyBtn'); if (b) b.textContent = '📋 Copier'; }, 2000);
          });
        });
        document.getElementById('apiKeyName').value = '';
        loadApiKeysList();
      } catch (err) {
        resultEl.innerHTML = `<div class="alert alert-error" style="padding:6px 10px;font-size:.82rem">Erreur : ${esc(err.message)}</div>`;
      }
    });

    async function loadApiKeysList() {
      const listEl = document.getElementById('apiKeysList');
      if (!listEl) return;
      try {
        const keys = await api.listApiKeys();
        const countEl = document.getElementById('apiKeysSummaryCount');
        if (countEl) countEl.textContent = keys.length > 0 ? `(${keys.length})` : '';
        if (keys.length === 0) {
          listEl.innerHTML = `<p style="font-size:.85rem;color:var(--text-muted);text-align:center;padding:10px 0">Aucune clé API.</p>`;
          return;
        }
        listEl.innerHTML = `
          <table style="width:100%;border-collapse:collapse;font-size:.8rem">
            <thead>
              <tr style="color:var(--text-muted);font-size:.72rem;text-transform:uppercase;letter-spacing:.4px;border-bottom:1px solid var(--border)">
                <th style="text-align:left;padding:4px 6px">Nom</th>
                <th style="text-align:left;padding:4px 6px">Préfixe</th>
                <th style="text-align:left;padding:4px 6px">Expiration</th>
                <th style="text-align:left;padding:4px 6px">Dernière util.</th>
                <th style="padding:4px 6px"></th>
              </tr>
            </thead>
            <tbody>
              ${keys.map(k => {
                const expired = k.expired;
                const rowStyle = expired ? 'background:rgba(211,47,47,.12);color:var(--danger,#e57373)' : '';
                const expDate = k.expiresAt ? k.expiresAt.replace('T', ' ').slice(0, 16) : '—';
                const lastUsed = k.lastUsedAt ? k.lastUsedAt.replace('T', ' ').slice(0, 16) : '—';
                return `<tr style="${rowStyle};border-bottom:1px solid var(--border)">
                  <td style="padding:6px 6px;font-weight:600">${esc(k.name)}${expired ? ' <span style="font-size:.68rem;font-weight:700;color:var(--danger,#e57373)">[EXPIRÉE]</span>' : ''}</td>
                  <td style="padding:6px 6px;font-family:monospace">${esc(k.keyPrefix)}…</td>
                  <td style="padding:6px 6px">${esc(expDate)}</td>
                  <td style="padding:6px 6px">${esc(lastUsed)}</td>
                  <td style="padding:6px 6px;text-align:right">
                    <button class="btn btn-ghost" data-delete-key="${k.id}" style="padding:3px 8px;font-size:.75rem;color:var(--danger,#e57373)">🗑 Supprimer</button>
                  </td>
                </tr>`;
              }).join('')}
            </tbody>
          </table>`;
        listEl.querySelectorAll('[data-delete-key]').forEach(btn => {
          btn.addEventListener('click', async () => {
            const id = parseInt(btn.dataset.deleteKey, 10);
            const keyName = btn.closest('tr').querySelector('td:first-child').textContent.trim();
            if (!confirm(`Supprimer la clé "${keyName}" ?`)) return;
            try {
              await api.deleteApiKey(id);
              loadApiKeysList();
            } catch (err) {
              alert('Erreur suppression : ' + err.message);
            }
          });
        });
      } catch (err) {
        listEl.innerHTML = `<div class="alert alert-error" style="padding:6px 10px;font-size:.82rem">Erreur chargement : ${esc(err.message)}</div>`;
      }
    }

    // ── Stats sets filter ─────────────────────────────────────────────────
    const checklist = document.getElementById('statsSetChecklist');
    if (checklist) {
      checklist.innerHTML = editions.map(e => {
        const checked = selectedSetIds.has(e.id) ? 'checked' : '';
        const label = e.setNumber ? `Set ${e.setNumber} — ${esc(e.name)}` : esc(e.code || e.name);
        return `<label style="display:flex;align-items:center;gap:8px;font-size:.86rem;color:var(--text-muted);cursor:pointer">
          <input type="checkbox" class="stats-set-checkbox" value="${e.id}" ${checked} style="accent-color:var(--accent)" />
          <span>${label}</span>
        </label>`;
      }).join('');
    }

    document.getElementById('statsSetsAllBtn').addEventListener('click', () => {
      document.querySelectorAll('.stats-set-checkbox').forEach(cb => { cb.checked = true; });
    });

    document.getElementById('statsSetsNoneBtn').addEventListener('click', () => {
      document.querySelectorAll('.stats-set-checkbox').forEach(cb => { cb.checked = false; });
    });

    document.getElementById('saveStatsSetsBtn').addEventListener('click', async () => {
      const selected = Array.from(document.querySelectorAll('.stats-set-checkbox:checked'))
        .map(cb => parseInt(cb.value, 10))
        .filter(v => !isNaN(v));
      const value = selected.join(',');

      try {
        await api.updateSetting('stats_enabled_sets', value);
        showAdminResult('statsSetsResult', {
          success: true,
          message: `${selected.length} set(s) activé(s) pour l'onglet Stats.`
        });
      } catch (err) {
        showAdminResult('statsSetsResult', {
          success: false,
          message: 'Erreur sauvegarde sets Stats : ' + err.message
        });
      }
    });

    // ── Save URL ───────────────────────────────────────────────────────────
    document.getElementById('saveUrlBtn').addEventListener('click', async () => {
      const url = document.getElementById('lorcajsonUrl').value.trim();
      await api.updateSetting('lorcajson_url', url);
      const el = document.getElementById('urlSaveResult');
      if (el) {
        el.innerHTML = `<div class="alert alert-success" style="padding:6px 10px;font-size:.8rem">URL sauvegardée.</div>`;
        setTimeout(() => { if (el) el.innerHTML = ''; }, 3000);
      }
    });

    // ── Sync from URL ──────────────────────────────────────────────────────
    document.getElementById('syncUrlBtn').addEventListener('click', async () => {
      const url = document.getElementById('lorcajsonUrl').value.trim();
      setSyncBusy(true);
      try {
        const result = await api.syncFromUrl(url);
        if (result.started) {
          _fingerprintsCache = null;
          collState.cards = [];
          collState.editions = [];
          startSyncPoll();
        } else {
          setSyncBusy(false);
          updateAdminProgress({ phase: 'error', percent: 0, current: 0, total: 0,
            message: result.message, running: false, error: true });
        }
      } catch (e) {
        setSyncBusy(false);
        updateAdminProgress({ phase: 'error', percent: 0, current: 0, total: 0,
          message: e.message, running: false, error: true });
      }
    });

    // ── Sync from file ─────────────────────────────────────────────────────
    document.getElementById('lorcajsonFile').addEventListener('change', async (e) => {
      const file = e.target.files[0];
      if (!file) return;
      e.target.value = '';
      setSyncBusy(true);
      try {
        const result = await api.syncFromFile(file);
        if (result.started) {
          _fingerprintsCache = null;
          collState.cards = [];
          collState.editions = [];
          startSyncPoll();
        } else {
          setSyncBusy(false);
          updateAdminProgress({ phase: 'error', percent: 0, current: 0, total: 0,
            message: result.message, running: false, error: true });
        }
      } catch (err) {
        setSyncBusy(false);
        updateAdminProgress({ phase: 'error', percent: 0, current: 0, total: 0,
          message: err.message, running: false, error: true });
      }
    });

    // ── Sauvegarde complète ────────────────────────────────────────────────
    document.getElementById('fullBackupBtn').addEventListener('click', async () => {
      try {
        const data = await api.fullBackup();
        const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `lorcalex-backup-${new Date().toISOString().slice(0, 10)}.json`;
        a.click();
        URL.revokeObjectURL(url);
        showAdminResult('fullBackupResult', { success: true, message: `Sauvegarde complète : ${data.totalCards} carte(s), ${data.totalCollection} en collection, ${(data.settings || []).length} paramètre(s).` });
      } catch (e) {
        showAdminResult('fullBackupResult', { success: false, message: 'Erreur sauvegarde : ' + e.message });
      }
    });

    // ── Restauration complète ─────────────────────────────────────────────
    document.getElementById('fullRestoreFile').addEventListener('change', async (e) => {
      const file = e.target.files[0];
      if (!file) return;
      if (!confirm('⚠️ ATTENTION : cette opération va EFFACER et remplacer toutes les données (cartes, collection, paramètres). Cette action est irréversible.\n\nContinuer ?')) {
        e.target.value = '';
        return;
      }
      showAdminResult('fullBackupResult', { success: true, message: '⏳ Restauration en cours, veuillez patienter…' });
      try {
        const text = await file.text();
        const data = JSON.parse(text);
        const result = await api.fullRestore(data);
        showAdminResult('fullBackupResult', result);
        collState.cards = [];
        collState.editions = [];
      } catch (err) {
        showAdminResult('fullBackupResult', { success: false, message: 'Erreur restauration : ' + err.message });
      } finally {
        e.target.value = '';
      }
    });

    // ── Import Companion ───────────────────────────────────────────────────
    document.getElementById('companionImportFile').addEventListener('change', async (e) => {
      const file = e.target.files[0];
      if (!file) return;
      const mergeMode = document.getElementById('companionMergeMode')?.checked !== false;
      setSyncBusy(true);
      try {
        const result = await api.importCompanionCollection(file, mergeMode);
        if (result.started) {
          collState.cards = [];
          collState.editions = [];
          showAdminResult('companionImportResult', {
            success: true,
            message: result.message || 'Import Companion démarré.'
          });
          startSyncPoll();
        } else {
          setSyncBusy(false);
          updateAdminProgress({ phase: 'error', percent: 0, current: 0, total: 0,
            message: result.message || 'Import Companion non démarré.', running: false, error: true });
        }
      } catch (err) {
        setSyncBusy(false);
        showAdminResult('companionImportResult', { success: false, message: 'Erreur import Companion : ' + err.message });
      } finally {
        e.target.value = '';
      }
    });
  });
}

function showAdminResult(elementId, result) {
  const el = document.getElementById(elementId);
  if (!el) return;
  const level = result.level || (result.success ? 'success' : 'error');
  const klass = level === 'warning' ? 'alert-warning' : (level === 'success' ? 'alert-success' : 'alert-error');
  el.innerHTML = `<div class="alert ${klass}">${esc(result.message)}</div>`;
  if (level === 'success') {
    setTimeout(() => { if (el) el.innerHTML = ''; }, 6000);
  }
}

// ─── Boot ─────────────────────────────────────────────────────────────────────

handleRoute();
