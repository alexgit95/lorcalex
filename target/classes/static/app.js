// ─── API ─────────────────────────────────────────────────────────────────────

async function apiFetch(path, options = {}) {
  const token = localStorage.getItem('token');
  const headers = { 'Content-Type': 'application/json', ...options.headers };
  if (token) headers.Authorization = `Bearer ${token}`;

  const response = await fetch('/api' + path, { ...options, headers });

  if (response.status === 401) {
    localStorage.removeItem('token');
    localStorage.removeItem('username');
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
  login: (username, password) =>
    apiFetch('/auth/login', { method: 'POST', body: JSON.stringify({ username, password }) }),

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

  syncCards: () => apiFetch('/admin/sync', { method: 'POST' }),

  getApiStatus: () => apiFetch('/admin/api-status'),
  exportCollection: () => apiFetch('/admin/export'),
  importCollection: (data) => apiFetch('/admin/import', { method: 'POST', body: JSON.stringify(data) }),
};

// ─── Router ──────────────────────────────────────────────────────────────────

function currentPage() {
  return location.hash.replace(/^#\//, '') || 'collection';
}

function navigate(page) {
  const authenticated = !!localStorage.getItem('token');
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
  const authenticated = !!localStorage.getItem('token');

  if (!authenticated && page !== 'login') {
    navigate('login');
    return;
  }
  if (authenticated && page === 'login') {
    navigate('collection');
    return;
  }

  stopCamera(); // clean up scanner camera if leaving scanner page
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
      const data = await api.login(
        document.getElementById('loginUser').value,
        document.getElementById('loginPass').value,
      );
      localStorage.setItem('token', data.token);
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
    ...collState.editions.map(e =>
      `<button class="filter-chip${collState.edition == e.id ? ' active' : ''}" data-edition="${e.id}">${esc(e.code || e.name)}</button>`
    ),
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

const RARITY_COLORS = { Common:'#bdbdbd', Uncommon:'#81d4fa', Rare:'#ce93d8', 'Super Rare':'#ffb74d', Legendary:'#fff176', Enchanted:'#f48fb1' };

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
      <p>Essayez de synchroniser depuis l'API externe (Admin).</p>
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
  return `<div class="card-item ${card.owned ? 'owned' : 'missing'}" data-id="${card.id}">
    ${card.imageUrl
      ? `<img src="${esc(card.imageUrl)}" alt="${esc(card.name)}" loading="lazy" onerror="this.style.display='none'" />`
      : `<div style="width:100%;aspect-ratio:600/840;background:var(--bg-card2);display:flex;align-items:center;justify-content:center;color:var(--text-muted);font-size:1.5rem">🃏</div>`}
    ${card.owned ? `<div class="owned-badge">${card.quantity > 1 ? card.quantity : '✓'}</div>` : ''}
    <div class="card-info">
      <div class="card-number">#${esc(card.cardNumber)}</div>
      <div class="card-name">${esc(card.name)}</div>
      ${card.rarity ? `<div class="card-rarity" style="color:${rarityColor}">${esc(card.rarity)}</div>` : ''}
    </div>
  </div>`;
}

function openModal(cardId) {
  const card = collState.cards.find(c => c.id === cardId);
  if (!card) return;
  collState.modal = card;

  const rarityClass = (card.rarity || '').replace(' ', '-');
  document.getElementById('modalArea').innerHTML = `
    <div class="modal-overlay" id="modalOverlay">
      <div class="modal" id="modalContent">
        <div style="display:flex;gap:16px;margin-bottom:16px">
          ${card.imageUrl ? `<img src="${esc(card.imageUrl)}" alt="${esc(card.name)}" style="width:100px;border-radius:8px" onerror="this.style.display='none'" />` : ''}
          <div>
            <div style="font-size:.75rem;color:var(--text-muted)">#${esc(card.cardNumber)} • ${esc(card.editionCode)}</div>
            <h2 style="margin-bottom:8px">${esc(card.name)}</h2>
            <div class="rarity-${esc(rarityClass)}" style="font-size:.85rem;font-weight:600">${esc(card.rarity)}</div>
            <div style="font-size:.8rem;color:var(--text-muted);margin-top:4px">${esc(card.inkColor)} • ${esc(card.type)}</div>
          </div>
        </div>
        ${card.bodyText ? `<p style="font-size:.85rem;color:var(--text-muted);margin-bottom:16px">${esc(card.bodyText)}</p>` : ''}
        <div style="display:flex;align-items:center;justify-content:space-between">
          <span style="font-weight:600">${card.owned ? `Possédée (×${card.quantity})` : 'Non possédée'}</span>
          ${card.owned
            ? `<div class="qty-control">
                <button class="qty-btn" id="qtyMinus">−</button>
                <span class="qty-value" id="qtyVal">${card.quantity}</span>
                <button class="qty-btn" id="qtyPlus">＋</button>
               </div>`
            : `<button class="btn btn-accent" id="addCardBtn">+ Ajouter</button>`}
        </div>
      </div>
    </div>`;

  document.getElementById('modalOverlay').addEventListener('click', e => {
    if (e.target === document.getElementById('modalOverlay')) closeModal();
  });
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
              <div style="font-size:.7rem;color:var(--text-muted)">#${esc(c.cardNumber)} • ${esc(c.editionCode)}</div>
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

// ─── SCANNER ──────────────────────────────────────────────────────────────────

let _cameraStream = null;
let _ocrWorker = null;

function stopCamera() {
  if (_cameraStream) {
    _cameraStream.getTracks().forEach(t => t.stop());
    _cameraStream = null;
  }
}

async function getOcrWorker() {
  if (!_ocrWorker) {
    _ocrWorker = await Tesseract.createWorker('eng', 1, {
      workerPath: 'https://cdn.jsdelivr.net/npm/tesseract.js@5/dist/worker.min.js',
      langPath: 'https://tessdata.projectnaptha.com/4.0.0',
      corePath: 'https://cdn.jsdelivr.net/npm/tesseract.js-core@5/tesseract-core-lstm.wasm.js',
    });
    await _ocrWorker.setParameters({ tessedit_char_whitelist: '0123456789/ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz -' });
  }
  return _ocrWorker;
}

function extractCardNumber(text) {
  const slashMatch = text.match(/\b(\d{1,3})\/(\d{1,3})\b/);
  if (slashMatch) return { cardNumber: parseInt(slashMatch[1]), totalInSet: parseInt(slashMatch[2]) };
  const numMatch = text.match(/\b(\d{1,3})\b/);
  if (numMatch) return { cardNumber: parseInt(numMatch[1]), totalInSet: null };
  return null;
}

function preprocessCanvas(source) {
  const canvas = document.createElement('canvas');
  const ctx = canvas.getContext('2d');
  const cropRatio = 0.18;
  const cropY = Math.floor(source.height * (1 - cropRatio));
  const cropH = source.height - cropY;
  canvas.width = source.width;
  canvas.height = cropH;
  ctx.drawImage(source, 0, cropY, source.width, cropH, 0, 0, source.width, cropH);
  const id = ctx.getImageData(0, 0, canvas.width, canvas.height);
  for (let i = 0; i < id.data.length; i += 4) {
    const g = 0.299 * id.data[i] + 0.587 * id.data[i+1] + 0.114 * id.data[i+2];
    const v = g > 128 ? 255 : 0;
    id.data[i] = id.data[i+1] = id.data[i+2] = v;
  }
  ctx.putImageData(id, 0, 0);
  return canvas;
}

async function recognizeFromVideo(video) {
  const canvas = document.createElement('canvas');
  canvas.width = video.videoWidth;
  canvas.height = video.videoHeight;
  canvas.getContext('2d').drawImage(video, 0, 0);
  const processed = preprocessCanvas(canvas);
  const worker = await getOcrWorker();
  const { data: { text } } = await worker.recognize(processed);
  return extractCardNumber(text);
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

let _scanState = { editions: [], selectedEdition: '', scanning: false, foundCards: [] };

function renderScanner() {
  document.getElementById('app').innerHTML = `
    <div class="app">
      <div class="page">
        <div class="page-header"><h1>📷 Scanner</h1></div>
        <div style="padding:10px 12px">
          <select class="search-input" id="scanEditionSelect">
            <option value="">Toutes les éditions</option>
          </select>
        </div>
        <div id="scanCameraArea"></div>
        <div id="scanAlerts"></div>
        <div style="padding:12px">
          <div style="display:flex;gap:8px">
            <input class="search-input" id="manualNum" type="number" placeholder="Numéro manuel…" min="1" style="border-radius:8px" />
            <button class="btn btn-ghost" id="manualLookupBtn" style="flex-shrink:0">Chercher</button>
          </div>
        </div>
        <div id="foundCardsArea"></div>
      </div>
      ${navHTML('scanner')}
    </div>`;

  // Load editions
  if (_scanState.editions.length === 0) {
    api.getEditions().then(data => {
      _scanState.editions = data;
      if (data.length > 0) _scanState.selectedEdition = String(data[0].id);
      const sel = document.getElementById('scanEditionSelect');
      if (sel) {
        data.forEach(e => {
          const opt = document.createElement('option');
          opt.value = e.id;
          opt.textContent = `${e.name} (${e.code})`;
          sel.appendChild(opt);
        });
        sel.value = _scanState.selectedEdition;
      }
    });
  } else {
    const sel = document.getElementById('scanEditionSelect');
    if (sel) {
      _scanState.editions.forEach(e => {
        const opt = document.createElement('option');
        opt.value = e.id;
        opt.textContent = `${e.name} (${e.code})`;
        sel.appendChild(opt);
      });
      sel.value = _scanState.selectedEdition;
    }
  }

  document.getElementById('scanEditionSelect').addEventListener('change', e => {
    _scanState.selectedEdition = e.target.value;
  });

  // Start camera
  const cameraArea = document.getElementById('scanCameraArea');
  navigator.mediaDevices?.getUserMedia({ video: { facingMode: { ideal: 'environment' }, width: { ideal: 1280 }, height: { ideal: 720 } } })
    .then(stream => {
      _cameraStream = stream;
      cameraArea.innerHTML = `
        <div class="scanner-container">
          <video id="scanVideo" class="scanner-video" autoplay playsinline muted></video>
          <div class="scanner-overlay"><div class="scanner-frame"></div></div>
        </div>
        <div style="padding:14px 12px">
          <button class="btn btn-accent btn-full" id="captureBtn">📷 Scanner la carte</button>
          <p style="text-align:center;font-size:.75rem;color:var(--text-muted);margin-top:8px">
            Positionnez le bas de la carte dans le cadre pour lire le numéro.
          </p>
        </div>`;
      document.getElementById('scanVideo').srcObject = stream;
      document.getElementById('captureBtn').addEventListener('click', handleCapture);
    })
    .catch(() => {
      cameraArea.innerHTML = `<div class="alert alert-warning" style="margin:12px">Caméra non disponible. Utilisez la saisie manuelle ci-dessous.</div>`;
    });

  document.getElementById('manualLookupBtn').addEventListener('click', handleManualLookup);
  document.getElementById('manualNum').addEventListener('keydown', e => { if (e.key === 'Enter') handleManualLookup(); });
}

async function handleCapture() {
  if (_scanState.scanning) return;
  const video = document.getElementById('scanVideo');
  if (!video) return;
  _scanState.scanning = true;
  setScanAlert('');
  document.getElementById('foundCardsArea').innerHTML = '';
  const btn = document.getElementById('captureBtn');
  btn.disabled = true;
  btn.innerHTML = `<span class="spinner" style="width:18px;height:18px;border-width:2px"></span> Analyse OCR…`;

  try {
    const result = await recognizeFromVideo(video);
    if (!result) {
      setScanAlert('Numéro de carte non détecté. Essayez la saisie manuelle.', 'error');
      return;
    }
    btn.innerHTML = `<span class="spinner" style="width:18px;height:18px;border-width:2px"></span> Numéro détecté: ${result.cardNumber} — Recherche…`;
    const editionId = _scanState.selectedEdition ? parseInt(_scanState.selectedEdition) : undefined;
    const cards = await api.lookupCard(result.cardNumber, editionId);
    await handleFoundCards(cards, result.cardNumber);
  } catch (e) {
    setScanAlert('Erreur: ' + e.message, 'error');
  } finally {
    _scanState.scanning = false;
    const b = document.getElementById('captureBtn');
    if (b) { b.disabled = false; b.textContent = '📷 Scanner la carte'; }
  }
}

async function handleManualLookup() {
  const input = document.getElementById('manualNum');
  const num = parseInt(input?.value);
  if (!num || num < 1) { setScanAlert('Numéro de carte invalide.', 'error'); return; }
  setScanAlert('');
  document.getElementById('foundCardsArea').innerHTML = '';
  try {
    const editionId = _scanState.selectedEdition ? parseInt(_scanState.selectedEdition) : undefined;
    const cards = await api.lookupCard(num, editionId);
    await handleFoundCards(cards, num);
  } catch (e) {
    setScanAlert('Erreur: ' + e.message, 'error');
  }
}

async function handleFoundCards(cards, num) {
  if (cards.length === 0) {
    setScanAlert(`Carte #${num} non trouvée. Synchronisez d'abord via Admin.`, 'error');
  } else if (cards.length === 1) {
    await autoAddCard(cards[0]);
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

function renderFoundCards(cards) {
  const area = document.getElementById('foundCardsArea');
  if (!area) return;
  area.innerHTML = `<div style="padding:0 12px">
    <h3 style="font-size:.9rem;margin-bottom:10px">Plusieurs cartes trouvées — choisissez :</h3>
    ${cards.map(c => `
      <button class="btn btn-ghost btn-full" style="margin-bottom:8px;justify-content:flex-start;gap:12px" data-cardid="${c.id}">
        ${c.imageUrl ? `<img src="${esc(c.imageUrl)}" alt="" style="width:36px;border-radius:4px" onerror="this.style.display='none'" />` : ''}
        <div style="text-align:left">
          <div style="font-weight:700">#${esc(c.cardNumber)} — ${esc(c.name)}</div>
          <div style="font-size:.75rem;color:var(--text-muted)">${esc(c.editionCode)} • ${esc(c.rarity)}</div>
        </div>
      </button>`).join('')}
  </div>`;
  area.querySelectorAll('[data-cardid]').forEach(btn => {
    btn.addEventListener('click', async () => {
      const card = cards.find(c => c.id === parseInt(btn.dataset.cardid));
      area.innerHTML = '';
      await autoAddCard(card);
    });
  });
}

function setScanAlert(msg, type = 'success') {
  const el = document.getElementById('scanAlerts');
  if (!el) return;
  el.innerHTML = msg ? `<div class="alert alert-${type}" style="margin:0 12px 10px">${esc(msg)}</div>` : '';
}

// ─── ADMIN ────────────────────────────────────────────────────────────────────

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
    localStorage.removeItem('token');
    localStorage.removeItem('username');
    collState.editions = [];
    collState.cards = [];
    navigate('login');
  });

  Promise.all([api.getSettings(), api.getApiStatus()]).then(([settings, status]) => {
    const content = document.getElementById('adminContent');
    if (!content) return;

    content.innerHTML = `
      <div class="edition-item" style="margin-bottom:12px">
        <h3 style="margin-bottom:12px">API Externe Lorcana</h3>
        <div class="toggle-row">
          <div>
            <div class="toggle-label">Activer l'API externe</div>
            <div class="toggle-desc">Permet de synchroniser les cartes depuis internet</div>
          </div>
          <label class="toggle">
            <input type="checkbox" id="apiToggle" ${status.enabled ? 'checked' : ''} />
            <span class="toggle-slider"></span>
          </label>
        </div>
        <div class="form-group" style="margin-top:16px;margin-bottom:8px">
          <label>URL de l'API</label>
          <input type="url" id="apiUrl" value="${esc(status.url)}" placeholder="https://api.lorcana-api.com/cards/all" />
        </div>
        <button class="btn btn-ghost btn-full" id="saveUrlBtn" style="margin-bottom:16px">💾 Sauvegarder l'URL</button>
        <button class="btn btn-accent btn-full" id="syncBtn" ${status.enabled ? '' : 'disabled'}>🔄 Synchroniser les cartes</button>
        ${!status.enabled ? `<p style="font-size:.78rem;color:var(--text-muted);margin-top:8px;text-align:center">Activez l'API externe pour pouvoir synchroniser.</p>` : ''}
      </div>
      <div id="syncResult"></div>

      <div class="edition-item" style="margin-top:12px">
        <h3 style="margin-bottom:12px">Import / Export</h3>
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

      <div class="edition-item" style="margin-top:12px">
        <h3 style="margin-bottom:12px">Paramètres</h3>
        ${settings.map(s => `
          <div style="margin-bottom:10px">
            <div style="font-size:.75rem;color:var(--primary-light);font-weight:700;letter-spacing:.5px">${esc(s.settingKey)}</div>
            <div style="font-size:.85rem;color:var(--text-muted)">${esc(s.description)}</div>
            <div style="font-size:.9rem;font-weight:600;margin-top:2px">${esc(s.settingValue)}</div>
          </div>`).join('')}
      </div>
      <div class="edition-item" style="margin-top:12px">
        <h3 style="margin-bottom:8px">Informations</h3>
        <p style="font-size:.85rem;color:var(--text-muted);line-height:1.7">
          <strong style="color:var(--text)">Lorcalex</strong> — Gestionnaire de collection Lorcana.<br/>
          Backend : Spring Boot + ${location.hostname === 'localhost' ? 'SQLite (local)' : 'PostgreSQL (Docker)'}<br/>
          Frontend : HTML/JS vanilla (intégré au JAR)
        </p>
      </div>`;

    document.getElementById('apiToggle').addEventListener('change', async e => {
      const enabled = e.target.checked;
      await api.updateSetting('external_api_enabled', enabled ? 'true' : 'false');
      const syncBtn = document.getElementById('syncBtn');
      if (syncBtn) syncBtn.disabled = !enabled;
    });

    document.getElementById('saveUrlBtn').addEventListener('click', async () => {
      const url = document.getElementById('apiUrl').value;
      await api.updateSetting('external_api_url', url);
      showSyncResult({ success: true, message: 'URL sauvegardée.' });
    });

    document.getElementById('syncBtn').addEventListener('click', async () => {
      const btn = document.getElementById('syncBtn');
      btn.disabled = true;
      btn.innerHTML = `<span class="spinner" style="width:16px;height:16px;border-width:2px"></span> Synchronisation…`;
      try {
        const result = await api.syncCards();
        showSyncResult(result);
      } catch (e) {
        showSyncResult({ success: false, message: e.message || 'Erreur lors de la sync.' });
      } finally {
        btn.disabled = false;
        btn.textContent = '🔄 Synchroniser les cartes';
      }
    });

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
        showImportExportResult({ success: true, message: `${data.totalEntries} carte(s) exportée(s).` });
      } catch (e) {
        showImportExportResult({ success: false, message: 'Erreur export : ' + e.message });
      }
    });

    document.getElementById('importFile').addEventListener('change', async (e) => {
      const file = e.target.files[0];
      if (!file) return;
      try {
        const text = await file.text();
        const data = JSON.parse(text);
        const result = await api.importCollection(data);
        showImportExportResult(result);
        collState.cards = []; // invalidate card cache so collection refreshes
      } catch (err) {
        showImportExportResult({ success: false, message: 'Erreur import : ' + err.message });
      } finally {
        e.target.value = '';
      }
    });
  });
}

function showImportExportResult(result) {
  const el = document.getElementById('importExportResult');
  if (!el) return;
  el.innerHTML = `<div class="alert ${result.success ? 'alert-success' : 'alert-error'}">${esc(result.message)}</div>`;
  setTimeout(() => { if (el) el.innerHTML = ''; }, 5000);
}

function showSyncResult(result) {
  const el = document.getElementById('syncResult');
  if (!el) return;
  el.innerHTML = `<div class="alert ${result.success ? 'alert-success' : 'alert-error'}">${esc(result.message)}</div>`;
  setTimeout(() => { if (el) el.innerHTML = ''; }, 4000);
}

// ─── Boot ─────────────────────────────────────────────────────────────────────

handleRoute();
