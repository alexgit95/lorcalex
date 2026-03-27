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
    try { const j = await response.json(); msg = j.message || msg; } catch { /* ignore */ }
    throw new Error(msg);
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

  addToCollection: (cardId, quantity = 1) =>
    apiFetch('/collection', { method: 'POST', body: JSON.stringify({ cardId, quantity }) }),

  updateQuantity: (cardId, quantity) =>
    apiFetch(`/collection/${cardId}`, { method: 'PUT', body: JSON.stringify({ quantity }) }),

  removeFromCollection: (cardId) =>
    apiFetch(`/collection/${cardId}`, { method: 'DELETE' }),

  getStatistics: () => apiFetch('/statistics'),

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

  computeHashes: () => apiFetch('/admin/compute-hashes', { method: 'POST' }),

  exportCollection: () => apiFetch('/admin/export'),
  importCollection: (data) => apiFetch('/admin/import', { method: 'POST', body: JSON.stringify(data) }),
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
  renderPage(page);
}

function renderPage(page) {
  switch (page) {
    case 'login':      renderLogin();      break;
    case 'collection': renderCollection(); break;
    case 'statistics': renderStatistics(); break;
    case 'scanner':    renderScanner();    break;
    case 'admin':      renderAdmin();      break;
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
    ${item('scanner',
      `<svg viewBox="0 0 24 24" fill="currentColor"><path d="M9.5 6.5v3h-3v-3h3M11 5H5v6h6V5zm-1.5 9.5v3h-3v-3h3M11 13H5v6h6v-6zm6.5-6.5v3h-3v-3h3M19 5h-6v6h6V5zm-6 8h1.5v1.5H13V13zm1.5 1.5H16V16h-1.5v-1.5zM16 13h1.5v1.5H16V13zm-3 3h1.5v1.5H13V16zm1.5 1.5H16V19h-1.5v-1.5zM16 16h1.5v1.5H16V16zm1.5-1.5H19V16h-1.5v-1.5zm0 3H19V19h-1.5v-1.5zM22 7h-2V4h-3V2h5v5zm0 15v-5h-2v3h-3v2h5zM2 22h5v-2H4v-3H2v5zM2 2v5h2V4h3V2H2z"/></svg>`,
      'Scanner')}
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
          ${['all','owned','missing'].map((k,i) =>
            `<button class="filter-chip${collState.filter===k?' active':''}" data-filter="${k}">${['Toutes','Possédées','Manquantes'][i]}</button>`
          ).join('')}
        </div>
        <div class="search-bar">
          <input class="search-input" id="searchInput" placeholder="Rechercher une carte…" value="${esc(collState.search)}" />
        </div>
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
    collState.search = e.target.value;
    renderCards();
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
  const filtered = cards.filter(c => {
    if (filter === 'owned' && !c.owned) return false;
    if (filter === 'missing' && c.owned) return false;
    if (search && !c.name.toLowerCase().includes(search.toLowerCase())) return false;
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
}

function cardItemHTML(card) {
  const rarityColor = RARITY_COLORS[card.rarity] || 'var(--text-muted)';
  const setLabel = card.editionSetNumber ? `S${card.editionSetNumber}·` : '';
  return `<div class="card-item ${card.owned ? 'owned' : 'missing'}" data-id="${card.id}">
    ${card.imageUrl
      ? `<img src="${esc(card.imageUrl)}" alt="${esc(card.name)}" loading="lazy" onerror="this.style.display='none'" />`
      : `<div style="width:100%;aspect-ratio:600/840;background:var(--bg-card2);display:flex;align-items:center;justify-content:center;color:var(--text-muted);font-size:1.5rem">🃏</div>`}
    ${card.owned ? `<div class="owned-badge">${card.quantity > 1 ? card.quantity : '✓'}</div>` : ''}
    <div class="card-info">
      <div class="card-number">${setLabel}#${esc(card.cardNumber)}</div>
      <div class="card-name">${esc(card.name)}</div>
      ${card.rarity ? `<div class="card-rarity" style="color:${rarityColor}">${esc(card.rarity)}</div>` : ''}
    </div>
  </div>`;
}

function openModal(cardId) {
  const card = collState.cards.find(c => c.id === cardId);
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

          <div class="modal-qty-section">
            ${card.owned
              ? `<div style="display:flex;align-items:center;justify-content:space-between">
                  <span style="font-size:.9rem;color:var(--text-muted)">En collection</span>
                  <div class="qty-control">
                    <button class="qty-btn" id="qtyMinus">−</button>
                    <span class="qty-value" id="qtyVal">${card.quantity}</span>
                    <button class="qty-btn" id="qtyPlus">＋</button>
                  </div>
                </div>`
              : `<button class="btn btn-accent btn-full" id="addCardBtn">+ Ajouter à la collection</button>`}
          </div>
        </div>
      </div>
    </div>`;

  document.getElementById('modalOverlay').addEventListener('click', e => {
    if (e.target === document.getElementById('modalOverlay')) closeModal();
  });
  document.getElementById('modalCloseBtn').addEventListener('click', closeModal);
  document.getElementById('modalContent').addEventListener('click', e => e.stopPropagation());

  if (card.owned) {
    document.getElementById('qtyMinus').addEventListener('click', () => updateQty(card.id, card.quantity - 1));
    document.getElementById('qtyPlus').addEventListener('click', () => updateQty(card.id, card.quantity + 1));
  } else {
    document.getElementById('addCardBtn').addEventListener('click', () => addCard(card.id));
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
            : `<button class="btn btn-accent" style="flex-shrink:0;padding:8px 14px;font-size:.85rem" data-action="add" data-id="${c.id}">+ Ajouter</button>`}
        </div>`).join('')}
    </div>`;

    area.addEventListener('click', async e => {
      const btn = e.target.closest('[data-action]');
      if (!btn) return;
      const cardId = parseInt(btn.dataset.id);
      const action = btn.dataset.action;
      let updated;
      if (action === 'add') {
        updated = await api.addToCollection(cardId, 1);
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
        const addBtn = row.querySelector('[data-action="add"]');
        if (updated.owned) {
          const ctrl = `<div class="qty-control" style="flex-shrink:0">
            <button class="qty-btn" data-action="minus" data-id="${updated.id}" data-qty="${updated.quantity}">−</button>
            <span class="qty-value">${updated.quantity}</span>
            <button class="qty-btn" data-action="plus" data-id="${updated.id}" data-qty="${updated.quantity}">＋</button>
          </div>`;
          if (qtyControl) qtyControl.outerHTML = ctrl;
          else if (addBtn) addBtn.outerHTML = ctrl;
        } else {
          const ab = `<button class="btn btn-accent" style="flex-shrink:0;padding:8px 14px;font-size:.85rem" data-action="add" data-id="${updated.id}">+ Ajouter</button>`;
          if (addBtn) addBtn.outerHTML = ab;
          else if (qtyControl) qtyControl.outerHTML = ab;
        }
      });
    });
  } catch (e) {
    area.innerHTML = `<div class="alert alert-error">${esc(e.message)}</div>`;
  }
}

async function addCard(cardId) {
  const updated = await api.addToCollection(cardId, 1);
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
    updated = await api.updateQuantity(cardId, qty);
  }
  collState.cards = collState.cards.map(c => c.id === updated.id ? updated : c);
  collState.modal = updated;
  renderCards();
  openModal(cardId);
}

// ─── STATISTICS ───────────────────────────────────────────────────────────────

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

    const rarityCharts = stats.byEdition
      .filter(e => e.byRarity?.length > 0)
      .map((e, i) => `
        <div class="chart-container">
          <h3>Rareté — ${esc(e.editionCode)}</h3>
          <div style="height:180px"><canvas id="rarityChart${i}"></canvas></div>
        </div>`).join('');

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

      ${rarityCharts}`;

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
        new Chart(document.getElementById('globalRarityBar'), {
          type: 'bar',
          data: {
            labels: rarities,
            datasets: [
              { label: 'Possédées', data: rarities.map(r => rarityMap[r].owned), backgroundColor: '#66bb6a' },
              { label: 'Manquantes', data: rarities.map(r => rarityMap[r].missing), backgroundColor: '#ef5350' },
            ],
          },
          options: stackedOpts,
        });
      }

      // Per-edition rarity charts
      stats.byEdition.filter(e => e.byRarity?.length > 0).forEach((e, i) => {
        new Chart(document.getElementById(`rarityChart${i}`), {
          type: 'bar',
          data: {
            labels: e.byRarity.map(r => r.rarity),
            datasets: [
              { label: 'Possédées', data: e.byRarity.map(r => r.ownedCards), backgroundColor: '#66bb6a' },
              { label: 'Manquantes', data: e.byRarity.map(r => r.missingCards), backgroundColor: '#ef5350' },
            ],
          },
          options: { ...stackedOpts, maintainAspectRatio: false },
        });
      });
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

// ⚠️ Charge l'image sélectionnée via le sélecteur de fichier et la dessine sur un canvas.
async function loadFileCanvas() {
  const input = document.getElementById('scanImageFile');
  const file = input?.files?.[0];
  if (!file) throw new Error('Sélectionnez d\'abord une image via le bouton "Choisir une image".');
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = e => {
      const img = new Image();
      img.onload = () => {
        const c = document.createElement('canvas');
        c.width  = img.naturalWidth;
        c.height = img.naturalHeight;
        c.getContext('2d').drawImage(img, 0, 0);
        resolve(c);
      };
      img.onerror = () => reject(new Error('Impossible de décoder l\'image sélectionnée.'));
      img.src = e.target.result;
    };
    reader.onerror = () => reject(new Error('Impossible de lire le fichier sélectionné.'));
    reader.readAsDataURL(file);
  });
}

// ── Parsing du code Lorcana ───────────────────────────────────────────────────
// Format : "N/TOTAL • LANG • SET"  ex : "1/204 • FR • 4"
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
  if (total   < 2  || total   > 400) return null;
  if (cardNum > total)               return null;
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

let _scanState = { scanning: false };

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
  navigator.mediaDevices?.getUserMedia({
    video: { facingMode: { ideal: 'environment' }, width: { ideal: 1920 }, height: { ideal: 1080 } }
  })
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
          <div style="display:flex;align-items:center;gap:10px;margin:4px 0">
            <hr style="flex:1;border:none;border-top:1px solid var(--border)" />
            <span style="font-size:.72rem;color:var(--text-muted);text-transform:uppercase;letter-spacing:.5px">ou</span>
            <hr style="flex:1;border:none;border-top:1px solid var(--border)" />
          </div>
          <label class="btn btn-ghost btn-full" style="cursor:pointer">
            🖼️ Choisir une image
            <input type="file" id="scanImageFile" accept="image/*" style="display:none" />
          </label>
          <p id="scanImageName" style="text-align:center;font-size:.72rem;color:var(--text-muted);margin:0">Aucune image sélectionnée</p>
          <button class="btn btn-ghost btn-full" id="scanImageBtn" disabled>🔍 Lancer l'OCR sur l'image</button>
        </div>`;
      document.getElementById('scanVideo').srcObject = stream;
      document.getElementById('captureBtn').addEventListener('click', () => handleCapture('camera'));
      document.getElementById('scanImageBtn').addEventListener('click', () => handleCapture('file'));
      document.getElementById('scanImageFile').addEventListener('change', e => {
        const f = e.target.files?.[0];
        const nameEl = document.getElementById('scanImageName');
        const imgBtn = document.getElementById('scanImageBtn');
        if (nameEl) nameEl.textContent = f ? `✔ ${f.name}` : 'Aucune image sélectionnée';
        if (imgBtn) imgBtn.disabled = !f;
      });
    })
    .catch(err => {
      // Caméra indisponible : proposer uniquement le mode fichier
      cameraArea.innerHTML = `
        <div class="alert alert-warning" style="margin:12px">Caméra indisponible (${esc(err.message)})</div>
        <div style="padding:0 12px 12px;display:flex;flex-direction:column;gap:8px">
          <label class="btn btn-ghost btn-full" style="cursor:pointer">
            🖼️ Choisir une image
            <input type="file" id="scanImageFile" accept="image/*" style="display:none" />
          </label>
          <p id="scanImageName" style="text-align:center;font-size:.72rem;color:var(--text-muted);margin:0">Aucune image sélectionnée</p>
          <button class="btn btn-accent btn-full" id="scanImageBtn" disabled>🔍 Lancer l'OCR sur l'image</button>
        </div>`;
      document.getElementById('scanImageBtn').addEventListener('click', () => handleCapture('file'));
      document.getElementById('scanImageFile').addEventListener('change', e => {
        const f = e.target.files?.[0];
        const nameEl = document.getElementById('scanImageName');
        const imgBtn = document.getElementById('scanImageBtn');
        if (nameEl) nameEl.textContent = f ? `✔ ${f.name}` : 'Aucune image sélectionnée';
        if (imgBtn) imgBtn.disabled = !f;
      });
    });

  document.getElementById('manualLookupBtn').addEventListener('click', handleManualLookup);
  document.getElementById('manualNum').addEventListener('keydown', e => { if (e.key === 'Enter') handleManualLookup(); });
  document.getElementById('manualSet').addEventListener('keydown', e => { if (e.key === 'Enter') handleManualLookup(); });
}

async function handleCapture(mode) {
  if (_scanState.scanning) return;
  _scanState.scanning = true;
  setScanAlert('');
  setScanDebug([]);
  document.getElementById('foundCardsArea').innerHTML = '';

  // Désactiver les deux boutons pendant le scan
  const camBtn = document.getElementById('captureBtn');
  const imgBtn = document.getElementById('scanImageBtn');
  if (camBtn) camBtn.disabled = true;
  if (imgBtn) imgBtn.disabled = true;
  const activeBtn = mode === 'camera' ? camBtn : imgBtn;

  try {
    // Étape 1 — Acquisition de l'image (caméra ou fichier)
    if (activeBtn) activeBtn.innerHTML = `<span class="spinner" style="width:18px;height:18px;border-width:2px"></span> Capture…`;
    let cardCanvas;
    let sourceLabel;
    if (mode === 'camera') {
      const video = document.getElementById('scanVideo');
      if (!video || !video.videoWidth) throw new Error('Flux vidéo indisponible. Vérifiez que la caméra est active.');
      cardCanvas = cropToCardFrame(video);
      sourceLabel = `📸 Caméra (${cardCanvas.width}×${cardCanvas.height} px)`;
    } else {
      cardCanvas = await loadFileCanvas();
      const fileName = document.getElementById('scanImageFile')?.files?.[0]?.name ?? 'image';
      sourceLabel = `🖼️ ${fileName} (${cardCanvas.width}×${cardCanvas.height} px)`;
    }

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
      setScanAlert(
        'Code illisible. Vérifiez : éclairage suffisant, carte bien centrée dans le cadre, code "N/TOTAL • FR • N" visible net en bas-gauche. Consultez le détail OCR ci-dessous.',
        'error'
      );
      return;
    }

    // Étape 4 — Recherche de la carte en base
    if (activeBtn) activeBtn.innerHTML = `<span class="spinner" style="width:18px;height:18px;border-width:2px"></span> Recherche carte #${parsed.cardNum}…`;
    const cards = await api.lookupCard(parsed.cardNum, undefined);
    if (cards.length === 0) {
      setScanAlert(`Aucune carte #${parsed.cardNum} en base. Importez d'abord le catalogue via Administration.`, 'error');
      return;
    }
    let matchedCards = cards;
    if (parsed.setNum !== null && cards.length > 1) {
      const filtered = cards.filter(c => c.editionSetNumber === parsed.setNum);
      if (filtered.length > 0) matchedCards = filtered;
    }
    await handleFoundCards(matchedCards, parsed.cardNum);
  } catch (e) {
    const msg = e instanceof Error ? e.message : String(e);
    setScanAlert(`Erreur : ${msg}`, 'error');
    console.error('[Scanner] handleCapture :', e);
  } finally {
    _scanState.scanning = false;
    if (camBtn) { camBtn.disabled = false; camBtn.textContent = '📸 Scanner depuis la caméra'; }
    const fileInput = document.getElementById('scanImageFile');
    if (imgBtn) { imgBtn.disabled = !fileInput?.files?.[0]; imgBtn.textContent = '🔍 Lancer l\'OCR sur l\'image'; }
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

async function autoAddCard(card) {
  if (card.owned) {
    setScanAlert(`"${card.name}" déjà dans la collection (×${card.quantity})`, 'success');
    playBeep(440, 150);
    navigator.vibrate?.([50]);
    return;
  }
  await api.addToCollection(card.id, 1);
  setScanAlert(`✓ "${card.name}" ajoutée à la collection !`, 'success');
  playBeep(880, 200);
  navigator.vibrate?.([100, 50, 100]);
}

// Affiche la carte trouvée avec une image et un bouton de confirmation avant ajout.
function renderCardConfirmation(card) {
  const area = document.getElementById('foundCardsArea');
  if (!area) return;
  setScanAlert('');
  const imgHtml = card.imageUrl
    ? `<img src="${esc(card.imageUrl)}" alt="" style="width:110px;border-radius:8px;flex-shrink:0;box-shadow:0 4px 12px rgba(0,0,0,.4)" onerror="this.style.display='none'" />`
    : `<div style="width:110px;height:154px;border-radius:8px;background:var(--bg-card2);flex-shrink:0;display:flex;align-items:center;justify-content:center;font-size:2.5rem">🃏</div>`;
  const alreadyOwned = card.owned
    ? `<div style="font-size:.78rem;color:var(--success);margin-top:6px">✓ Déjà en collection (×${card.quantity})</div>`
    : '';
  area.innerHTML = `
    <div style="padding:12px">
      <h3 style="font-size:.85rem;color:var(--text-muted);text-align:center;margin-bottom:12px;text-transform:uppercase;letter-spacing:.5px">Carte identifiée — confirmer ?</h3>
      <div style="display:flex;gap:14px;align-items:flex-start">
        ${imgHtml}
        <div style="flex:1">
          <div style="font-weight:800;font-size:1rem;line-height:1.3">${esc(card.name)}</div>
          <div style="font-size:.78rem;color:var(--primary-light);font-weight:700;margin-top:4px">${esc(card.editionCode)}</div>
          <div style="font-size:.78rem;color:var(--text-muted);margin-top:2px">#${esc(String(card.cardNumber))} · ${esc(card.rarity)}</div>
          ${alreadyOwned}
        </div>
      </div>
      <div style="display:flex;gap:8px;margin-top:16px">
        <button class="btn btn-accent btn-full" id="confirmAddBtn">${card.owned ? '+ Ajouter un exemplaire' : '✓ Ajouter à la collection'}</button>
        <button class="btn btn-ghost" id="cancelConfirmBtn" style="flex-shrink:0;padding:0 14px">✕</button>
      </div>
    </div>`;
  document.getElementById('confirmAddBtn').addEventListener('click', async () => {
    area.innerHTML = '';
    await autoAddCard(card);
  });
  document.getElementById('cancelConfirmBtn').addEventListener('click', () => {
    area.innerHTML = '';
    setScanAlert('Annulé.', 'info');
  });
}

function renderFoundCards(cards) {
  const area = document.getElementById('foundCardsArea');
  if (!area) return;
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
  </div>`;
  area.querySelectorAll('[data-cardid]').forEach(btn => {
    btn.addEventListener('click', () => {
      const card = cards.find(c => c.id === parseInt(btn.dataset.cardid));
      renderCardConfirmation(card);
    });
  });
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
  ['syncUrlBtn', 'computeHashesBtn'].forEach(id => {
    const el = document.getElementById(id);
    if (el) el.disabled = busy;
  });
  const fi = document.getElementById('lorcajsonFile');
  if (fi) fi.disabled = busy;
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

  Promise.all([api.getSettings(), api.getLorcaJsonUrl(), api.getProgress()]).then(([settings, urlData, progressData]) => {
    const content = document.getElementById('adminContent');
    if (!content) return;
    const currentUrl = urlData.url || 'https://lorcanajson.org/files/current/fr/allCards.json';

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

      <!-- Progression (partagée étapes 1 & 2) -->
      <div id="adminProgressBox" class="edition-item" style="margin-bottom:12px;display:none"></div>

      <!-- Étape 2 : Empreintes -->
      <div class="edition-item" style="margin-bottom:12px">
        <div style="display:flex;align-items:center;gap:8px;margin-bottom:10px">
          <span style="background:var(--primary-light);color:#fff;border-radius:50%;width:24px;height:24px;display:inline-flex;align-items:center;justify-content:center;font-size:.75rem;font-weight:700;flex-shrink:0">2</span>
          <h3 style="margin:0">Empreintes visuelles (scanner)</h3>
        </div>
        <p style="font-size:.83rem;color:var(--text-muted);margin-bottom:10px;line-height:1.5">
          Après la synchronisation, calculez les empreintes visuelles des cartes pour la reconnaissance par scanner.
          Cette opération télécharge chaque vignette — elle peut prendre plusieurs minutes.
        </p>
        <button class="btn btn-ghost btn-full" id="computeHashesBtn">🔍 Calculer les empreintes</button>
      </div>

      <!-- Collection Import / Export -->
      <div class="edition-item" style="margin-bottom:12px">
        <h3 style="margin-bottom:12px">Collection — Import / Export</h3>
        <p style="font-size:.85rem;color:var(--text-muted);margin-bottom:12px">
          Sauvegardez ou restaurez l'intégralité de votre collection au format JSON.
        </p>
        <button class="btn btn-ghost btn-full" id="exportBtn" style="margin-bottom:8px">⬇️ Exporter la collection (JSON)</button>
        <label class="btn btn-ghost btn-full" style="cursor:pointer;margin-bottom:0">
          ⬆️ Importer une collection (JSON)
          <input type="file" id="importFile" accept=".json" style="display:none" />
        </label>
        <div id="importExportResult" style="margin-top:8px"></div>
      </div>

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

    // ── Compute hashes ─────────────────────────────────────────────────────
    document.getElementById('computeHashesBtn').addEventListener('click', async () => {
      setSyncBusy(true);
      try {
        const result = await api.computeHashes();
        if (result.started) {
          _fingerprintsCache = null;
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

    // ── Export collection ──────────────────────────────────────────────────
    document.getElementById('exportBtn').addEventListener('click', async () => {
      try {
        const data = await api.exportCollection();
        const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `lorcalex-export-${new Date().toISOString().slice(0, 10)}.json`;
        a.click();
        URL.revokeObjectURL(url);
        showAdminResult('importExportResult', { success: true, message: `${data.totalEntries} carte(s) exportée(s).` });
      } catch (e) {
        showAdminResult('importExportResult', { success: false, message: 'Erreur export : ' + e.message });
      }
    });

    // ── Import collection ──────────────────────────────────────────────────
    document.getElementById('importFile').addEventListener('change', async (e) => {
      const file = e.target.files[0];
      if (!file) return;
      try {
        const text = await file.text();
        const data = JSON.parse(text);
        const result = await api.importCollection(data);
        showAdminResult('importExportResult', result);
        collState.cards = [];
      } catch (err) {
        showAdminResult('importExportResult', { success: false, message: 'Erreur import : ' + err.message });
      } finally {
        e.target.value = '';
      }
    });
  });
}

function showAdminResult(elementId, result) {
  const el = document.getElementById(elementId);
  if (!el) return;
  el.innerHTML = `<div class="alert ${result.success ? 'alert-success' : 'alert-error'}">${esc(result.message)}</div>`;
  if (result.success) {
    setTimeout(() => { if (el) el.innerHTML = ''; }, 6000);
  }
}

// ─── Boot ─────────────────────────────────────────────────────────────────────

handleRoute();
