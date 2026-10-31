/**
 * TrackZen Enterprise Client Application
 * Communicates with Spring Boot backend at http://localhost:8080/api
 */

// Application API Endpoint (Auto-detects localhost vs Cloud/Render, or can be set via localStorage.setItem('trackzen_api_url', 'https://...'))
const API_BASE = window.TRACKZEN_API_BASE
    || localStorage.getItem('trackzen_api_url')
    || 'https://trackzen-backend-y8wx.onrender.com/api';

// Application State
let currentUser = null;
let currentNavView = 'tickets';
let allTickets = [];
let allAgents = [];
let allEscalations = [];
let currentSelectedTicketId = null;
let onlyUnresolvedEscalations = true;

// Initialize on page load
document.addEventListener('DOMContentLoaded', () => {
    loadUserSession();
    checkBackendHealth();
    setInterval(checkBackendHealth, 30000);
});

/* ==========================================================================
   API Client Helper
   ========================================================================== */

async function apiRequest(endpoint, method = 'GET', data = null) {
    const headers = {
        'Content-Type': 'application/json'
    };

    if (currentUser && currentUser.token) {
        headers['Authorization'] = `Bearer ${currentUser.token}`;
    }

    const options = {
        method,
        headers
    };

    if (data && (method === 'POST' || method === 'PUT')) {
        options.body = JSON.stringify(data);
    }

    try {
        const response = await fetch(`${API_BASE}${endpoint}`, options);

        if (response.status === 204) {
            return null;
        }

        const resData = await response.json().catch(() => ({}));

        if (!response.ok) {
            // Check for 401 Unauthorized (token invalid / expired) -> session expired
            if (response.status === 401 && currentUser) {
                showToast('Session expired. Please sign in again.', 'warning');
                logout();
                return null;
            }
            // 403 Forbidden -> show clean access denied message without kicking user out
            if (response.status === 403) {
                const msg = resData.message || 'Access Denied: Your current role does not have permission for this action.';
                throw new Error(msg);
            }
            const errorMsg = resData.message || resData.error || `HTTP error ${response.status}`;
            throw new Error(errorMsg);
        }

        return resData;
    } catch (err) {
        console.error(`API Error [${method} ${endpoint}]:`, err);
        if (err.name === 'TypeError' || err.message === 'Failed to fetch' || err.message.includes('fetch')) {
            throw new Error(`Cannot connect to backend server at ${API_BASE}. Make sure the backend is running.`);
        }
        throw err;
    }
}

async function checkBackendHealth() {
    const pills = [
        document.getElementById('backendStatusPill'),
        document.getElementById('authBackendStatusPill')
    ].filter(Boolean);

    if (pills.length === 0) return;

    try {
        // Quick ping to check API availability
        const healthUrl = API_BASE.replace(/\/api$/, '') + '/health';
        const res = await fetch(healthUrl, { method: 'GET' });
        if (res.ok) {
            pills.forEach(pill => {
                pill.innerHTML = `<span class="pulse-dot" style="width:7px; height:7px; border-radius:50%; background:#10b981; display:inline-block; box-shadow:0 0 8px #10b981;"></span> API Connected`;
                pill.style.background = 'rgba(16, 185, 129, 0.1)';
                pill.style.color = '#34d399';
                pill.style.border = '1px solid rgba(16, 185, 129, 0.25)';
            });
            return;
        }
        throw new Error('Not OK');
    } catch (e) {
        pills.forEach(pill => {
            pill.innerHTML = `<span class="pulse-dot" style="width:7px; height:7px; border-radius:50%; background:#ef4444; display:inline-block; box-shadow:0 0 8px #ef4444;"></span> Backend Offline`;
            pill.style.background = 'rgba(239, 68, 68, 0.1)';
            pill.style.color = '#f87171';
            pill.style.border = '1px solid rgba(239, 68, 68, 0.25)';
        });
    }
}

/* ==========================================================================
   User Session & Authentication
   ========================================================================== */

function loadUserSession() {
    const saved = localStorage.getItem('trackzen_user');
    if (saved) {
        try {
            currentUser = JSON.parse(saved);
            showAppView();
            return;
        } catch (e) {
            localStorage.removeItem('trackzen_user');
        }
    }
    showAuthView();
}

function showAuthView() {
    document.getElementById('authContainer').classList.remove('hidden');
    document.getElementById('appContainer').classList.add('hidden');
}

function showAppView() {
    document.getElementById('authContainer').classList.add('hidden');
    document.getElementById('appContainer').classList.remove('hidden');

    // Populate user profile info in navbar
    const nameEl = document.getElementById('userNameLabel');
    const roleEl = document.getElementById('userRoleBadge');
    const avatarEl = document.getElementById('userAvatar');

    if (nameEl) nameEl.textContent = currentUser.fullName || 'User';
    if (roleEl) {
        roleEl.textContent = currentUser.role.replace('ROLE_', '');
    }
    if (avatarEl && currentUser.fullName) {
        avatarEl.textContent = currentUser.fullName.charAt(0).toUpperCase();
    }

    // Role-based UI visibility
    applyRoleVisibility();

    // Load initial views & stats
    loadStats();
    switchNavView('tickets');
}

function applyRoleVisibility() {
    const role = currentUser.role;

    // Customer controls
    document.querySelectorAll('.customer-only').forEach(el => {
        el.style.display = (role === 'ROLE_CUSTOMER') ? '' : 'none';
    });

    // Supervisor controls
    document.querySelectorAll('.supervisor-only').forEach(el => {
        el.style.display = (role === 'ROLE_SUPERVISOR') ? '' : 'none';
    });

    // Supervisor & Agent controls
    document.querySelectorAll('.supervisor-agent-only').forEach(el => {
        el.style.display = (role === 'ROLE_SUPERVISOR' || role === 'ROLE_SUPPORT_AGENT') ? '' : 'none';
    });

    // Internal comment checkbox: only visible to Agents/Supervisors
    const internalCheckboxWrap = document.getElementById('internalNoteCheckboxWrap');
    if (internalCheckboxWrap) {
        internalCheckboxWrap.style.display = (role === 'ROLE_CUSTOMER') ? 'none' : 'flex';
    }

    // Hide SLA and Agents tabs from Customers
    const navSla = document.getElementById('navSlaTab');
    if (navSla) {
        navSla.style.display = (role === 'ROLE_CUSTOMER') ? 'none' : 'flex';
    }
}

function switchAuthTab(tab) {
    const loginForm = document.getElementById('loginForm');
    const registerForm = document.getElementById('registerForm');
    const tabLogin = document.getElementById('tabLoginBtn');
    const tabRegister = document.getElementById('tabRegisterBtn');

    if (tab === 'login') {
        loginForm.classList.remove('hidden');
        registerForm.classList.add('hidden');
        tabLogin.classList.add('active');
        tabRegister.classList.remove('active');
    } else {
        loginForm.classList.add('hidden');
        registerForm.classList.remove('hidden');
        tabLogin.classList.remove('active');
        tabRegister.classList.add('active');
    }
}

async function handleLogin(e) {
    e.preventDefault();
    const email = document.getElementById('loginEmail').value.trim();
    const password = document.getElementById('loginPassword').value;

    const btn = document.getElementById('loginSubmitBtn');
    btn.disabled = true;

    try {
        const res = await apiRequest('/auth/login', 'POST', { email, password });
        if (res && res.token) {
            currentUser = {
                email,
                token: res.token,
                role: res.role,
                fullName: res.fullName
            };
            localStorage.setItem('trackzen_user', JSON.stringify(currentUser));
            showToast(`Welcome back, ${res.fullName}!`, 'success');
            showAppView();
        }
    } catch (err) {
        showToast(err.message, 'error');
    } finally {
        btn.disabled = false;
    }
}

async function handleRegister(e) {
    e.preventDefault();
    const fullName = document.getElementById('regFullName').value.trim();
    const email = document.getElementById('regEmail').value.trim();
    const password = document.getElementById('regPassword').value;
    const role = document.getElementById('regRole').value;

    const btn = document.getElementById('registerSubmitBtn');
    btn.disabled = true;

    try {
        const res = await apiRequest('/auth/register', 'POST', {
            email,
            password,
            fullName,
            role
        });

        if (res && res.token) {
            currentUser = {
                email,
                token: res.token,
                role: res.role,
                fullName: res.fullName
            };
            localStorage.setItem('trackzen_user', JSON.stringify(currentUser));
            showToast(`Account created successfully! Logged in as ${res.role}`, 'success');
            showAppView();
        }
    } catch (err) {
        showToast(err.message, 'error');
    } finally {
        btn.disabled = false;
    }
}

async function quickLogin(email, password, fullName, role) {
    // Attempt login; if fails, auto-register then login
    try {
        const res = await apiRequest('/auth/login', 'POST', { email, password });
        if (res && res.token) {
            currentUser = { email, token: res.token, role: res.role, fullName: res.fullName };
            localStorage.setItem('trackzen_user', JSON.stringify(currentUser));
            showToast(`Logged in as ${res.fullName} (${res.role})`, 'success');
            showAppView();
            return;
        }
    } catch (loginErr) {
        // Auto-register demo account
        try {
            const regRes = await apiRequest('/auth/register', 'POST', { email, password, fullName, role });
            if (regRes && regRes.token) {
                currentUser = { email, token: regRes.token, role: regRes.role, fullName: regRes.fullName };
                localStorage.setItem('trackzen_user', JSON.stringify(currentUser));
                showToast(`Demo user created & logged in (${regRes.role})`, 'success');
                showAppView();
            }
        } catch (regErr) {
            showToast(regErr.message, 'error');
        }
    }
}

function logout() {
    currentUser = null;
    localStorage.removeItem('trackzen_user');
    showAuthView();
    showToast('Signed out successfully.', 'info');
}

/* ==========================================================================
   Role-Based Authorization & Visibility Filtering
   ========================================================================== */

function filterTicketsByRole(tickets) {
    if (!tickets || !Array.isArray(tickets)) return [];
    if (!currentUser) return [];

    const role = currentUser.role;
    const myName = (currentUser.fullName || '').trim().toLowerCase();
    const myEmail = (currentUser.email || '').trim().toLowerCase();
    const myId = currentUser.id ? Number(currentUser.id) : null;

    if (role === 'ROLE_SUPERVISOR') {
        // Supervisor has full visibility across all tickets
        return tickets;
    }

    if (role === 'ROLE_SUPPORT_AGENT') {
        // Support agent can ONLY view tickets assigned to them
        return tickets.filter(t => {
            if (!t.assignedAgentName) return false;
            return t.assignedAgentName.trim().toLowerCase() === myName;
        });
    }

    if (role === 'ROLE_CUSTOMER') {
        // Customer can ONLY view their own tickets (owner)
        return tickets.filter(t => {
            const matchName = t.creatorName && t.creatorName.trim().toLowerCase() === myName;
            const matchEmail = t.creatorEmail && t.creatorEmail.trim().toLowerCase() === myEmail;
            const matchId = myId && t.creatorId && Number(t.creatorId) === myId;
            return Boolean(matchName || matchEmail || matchId);
        });
    }

    return [];
}

/* ==========================================================================
   Navigation & View Routing
   ========================================================================== */

function switchNavView(viewName) {
    currentNavView = viewName;

    // Update nav links active state
    document.querySelectorAll('.nav-tab').forEach(tab => {
        if (tab.getAttribute('data-view') === viewName) {
            tab.classList.add('active');
        } else {
            tab.classList.remove('active');
        }
    });

    // Hide ALL view sections to prevent bleeding or overlapping
    const viewIds = ['viewDashboard', 'viewTickets', 'viewSlaRisk', 'viewAgents', 'viewEscalations'];
    viewIds.forEach(id => {
        const el = document.getElementById(id);
        if (el) el.classList.add('hidden');
    });

    const titleEl = document.getElementById('viewHeaderTitle');
    const descEl = document.getElementById('viewHeaderDesc');

    if (viewName === 'dashboard') {
        const dashEl = document.getElementById('viewDashboard');
        if (dashEl) dashEl.classList.remove('hidden');
        titleEl.textContent = 'Operational Dashboard';
        descEl.textContent = 'High-level metrics, active SLA tracking, and recent activity';
        loadDashboard();
    } else if (viewName === 'tickets') {
        document.getElementById('viewTickets').classList.remove('hidden');
        titleEl.textContent = 'Support Tickets Hub';
        descEl.textContent = 'Real-time overview of customer tickets, priorities, and workflow statuses';
        loadTickets();
    } else if (viewName === 'sla-risk') {
        if (!currentUser || currentUser.role === 'ROLE_CUSTOMER') {
            switchNavView('tickets');
            return;
        }
        document.getElementById('viewSlaRisk').classList.remove('hidden');
        titleEl.textContent = 'SLA Breach Risk Watch';
        descEl.textContent = 'Tickets approaching deadline expiration within the next 24 hours';
        loadSlaRiskTickets();
    } else if (viewName === 'agents') {
        if (!currentUser || currentUser.role !== 'ROLE_SUPERVISOR') {
            switchNavView('tickets');
            return;
        }
        document.getElementById('viewAgents').classList.remove('hidden');
        titleEl.textContent = 'Agent Fleet & Workload Distribution';
        descEl.textContent = 'Manage active capacities, availability statuses, and real-time workload';
        loadAgents();
    } else if (viewName === 'escalations') {
        if (!currentUser || currentUser.role !== 'ROLE_SUPERVISOR') {
            switchNavView('tickets');
            return;
        }
        document.getElementById('viewEscalations').classList.remove('hidden');
        titleEl.textContent = 'Escalation Resolution Queue';
        descEl.textContent = 'Multi-level ticket escalations requiring supervisor intervention';
        loadEscalations();
    }

    loadStats();
}

function refreshCurrentView() {
    switchNavView(currentNavView);
    showToast('View refreshed', 'info');
}

/* ==========================================================================
   Dashboard Overview
   ========================================================================== */

async function loadDashboard() {
    const listEl = document.getElementById('dashboardRecentTicketsList');
    const emptyEl = document.getElementById('dashboardEmptyState');

    try {
        const rawTickets = await apiRequest('/tickets');
        allTickets = filterTicketsByRole(rawTickets || []);

        if (currentUser && currentUser.role !== 'ROLE_SUPERVISOR') {
            updateStatsDisplay(calculateStatsFromTickets(allTickets));
        }

        if (listEl) {
            listEl.innerHTML = '';
            if (allTickets.length === 0) {
                if (emptyEl) emptyEl.classList.remove('hidden');
            } else {
                if (emptyEl) emptyEl.classList.add('hidden');
                const recent = allTickets.slice(0, 4);
                renderTickets(recent, listEl, emptyEl);
            }
        }
    } catch (err) {
        showToast(err.message, 'error');
    }
}

/* ==========================================================================
   Statistics
   ========================================================================== */

function updateStatsDisplay(stats) {
    if (!stats) return;
    const statTotalEl = document.getElementById('statTotal');
    const statOpenEl = document.getElementById('statOpen');
    const statInProgressEl = document.getElementById('statInProgress');
    const statEscalatedEl = document.getElementById('statEscalated');
    const statResolvedEl = document.getElementById('statResolved');

    if (statTotalEl) statTotalEl.textContent = stats.TOTAL || 0;
    if (statOpenEl) statOpenEl.textContent = stats.OPEN || 0;
    if (statInProgressEl) statInProgressEl.textContent = stats.IN_PROGRESS || 0;
    if (statEscalatedEl) statEscalatedEl.textContent = stats.ESCALATED || 0;
    if (statResolvedEl) statResolvedEl.textContent = stats.RESOLVED || 0;
}

function calculateStatsFromTickets(ticketList) {
    const list = ticketList || [];
    return {
        TOTAL: list.length,
        OPEN: list.filter(t => t.status === 'OPEN').length,
        IN_PROGRESS: list.filter(t => t.status === 'IN_PROGRESS').length,
        ESCALATED: list.filter(t => t.status === 'ESCALATED').length,
        RESOLVED: list.filter(t => t.status === 'RESOLVED').length
    };
}

async function loadStats() {
    try {
        let stats = null;
        if (currentUser && currentUser.role === 'ROLE_SUPERVISOR') {
            stats = await apiRequest('/tickets/stats');
        } else if (currentUser && (currentUser.role === 'ROLE_SUPPORT_AGENT' || currentUser.role === 'ROLE_CUSTOMER')) {
            if (!allTickets || allTickets.length === 0) {
                try {
                    const raw = await apiRequest('/tickets');
                    allTickets = filterTicketsByRole(raw || []);
                } catch (_) {}
            }
            stats = calculateStatsFromTickets(allTickets);
        }

        if (stats) {
            updateStatsDisplay(stats);
        }

        // Count SLA risk
        if (currentUser && currentUser.role !== 'ROLE_CUSTOMER') {
            let rawSla = await apiRequest('/tickets/sla-risk') || [];
            let slaTickets = filterTicketsByRole(rawSla);
            const count = (slaTickets && slaTickets.length) ? slaTickets.length : 0;
            const slaNavCount = document.getElementById('slaRiskCountNav');
            if (slaNavCount) slaNavCount.textContent = count;
        }
    } catch (e) {
        // Stats failure shouldn't block page
    }
}

/* ==========================================================================
   Ticket Management
   ========================================================================== */

async function loadTickets() {
    const listEl = document.getElementById('ticketsList');
    const emptyEl = document.getElementById('ticketsEmptyState');

    try {
        const rawTickets = await apiRequest('/tickets');
        allTickets = filterTicketsByRole(rawTickets || []);
        renderTickets(allTickets, listEl, emptyEl);
        if (currentUser && (currentUser.role === 'ROLE_CUSTOMER' || currentUser.role === 'ROLE_SUPPORT_AGENT')) {
            updateStatsDisplay(calculateStatsFromTickets(allTickets));
        }
    } catch (err) {
        showToast(err.message, 'error');
    }
}

function renderTickets(tickets, containerEl, emptyStateEl) {
    containerEl.innerHTML = '';

    if (!tickets || tickets.length === 0) {
        emptyStateEl.classList.remove('hidden');
        return;
    }
    emptyStateEl.classList.add('hidden');

    tickets.forEach(ticket => {
        const card = document.createElement('div');
        card.className = 'ticket-card';
        card.onclick = () => openTicketDetailModal(ticket.id);

        const slaMeta = formatSlaTime(ticket.slaDeadline, ticket.status);
        const assignedDisplay = ticket.assignedAgentName 
            ? `<span class="ticket-agent-badge"><span class="dot"></span>${escapeHtml(ticket.assignedAgentName)}</span>`
            : `<span class="ticket-agent-badge unassigned"><span class="dot"></span>Unassigned</span>`;

        card.innerHTML = `
            <div>
                <div class="ticket-card-header">
                    <span class="ticket-id">#${ticket.id}</span>
                    <div class="badge-row">
                        <span class="badge badge-priority-${ticket.priority}">${ticket.priority}</span>
                        <span class="badge badge-status-${ticket.status}">${ticket.status}</span>
                    </div>
                </div>
                <h3 class="ticket-card-title">${escapeHtml(ticket.title)}</h3>
                <div class="ticket-meta-rows" style="display:flex;flex-direction:column;gap:5px;margin:12px 0 16px 0;font-size:0.86rem;line-height:1.5;">
                    <div><strong style="color:var(--text-secondary);">Customer:</strong> <span style="color:#ffffff;">${escapeHtml(ticket.creatorName || 'Customer')}</span></div>
                    <div><strong style="color:var(--text-secondary);">Assigned:</strong> ${assignedDisplay}</div>
                    <div><strong style="color:var(--text-secondary);">SLA Window:</strong> <span style="color:${slaMeta.className === 'risk' ? '#fda4af' : '#6ee7b7'};font-weight:700;">${slaMeta.text}</span></div>
                    <div style="color:#cbd5e1;margin-top:4px;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical;overflow:hidden;">${escapeHtml(ticket.description)}</div>
                </div>
            </div>
            <div>
                <div class="ticket-card-footer" style="display:flex;align-items:center;justify-content:space-between;border-top:1px solid var(--border-subtle);padding-top:14px;">
                    <button class="btn btn-secondary btn-sm" onclick="event.stopPropagation(); openTicketDetailModal(${ticket.id})">
                        Manage Ticket
                    </button>
                    <div class="card-counters" style="display:flex;align-items:center;gap:12px;font-size:0.82rem;color:var(--text-muted);">
                        <span class="card-counter-item" title="Comments">💬 ${ticket.commentCount || 0}</span>
                        ${ticket.escalationCount > 0 ? `<span class="card-counter-item text-danger" title="Escalations" style="color:var(--color-danger);font-weight:700;">⚠️ ${ticket.escalationCount}</span>` : ''}
                    </div>
                </div>
            </div>
        `;
        containerEl.appendChild(card);
    });
}

function filterTickets() {
    const search = document.getElementById('ticketSearchInput').value.toLowerCase();
    const status = document.getElementById('filterStatus').value;
    const priority = document.getElementById('filterPriority').value;

    const filtered = allTickets.filter(ticket => {
        const matchesSearch = 
            ticket.title.toLowerCase().includes(search) || 
            ticket.description.toLowerCase().includes(search) || 
            String(ticket.id).includes(search);

        const matchesStatus = (status === 'ALL') || (ticket.status === status);
        const matchesPriority = (priority === 'ALL') || (ticket.priority === priority);

        return matchesSearch && matchesStatus && matchesPriority;
    });

    renderTickets(filtered, document.getElementById('ticketsList'), document.getElementById('ticketsEmptyState'));
}

async function loadSlaRiskTickets() {
    const listEl = document.getElementById('slaRiskList');
    const emptyEl = document.getElementById('slaRiskEmptyState');

    try {
        let rawTickets = await apiRequest('/tickets/sla-risk') || [];
        let tickets = filterTicketsByRole(rawTickets);
        renderTickets(tickets, listEl, emptyEl);
    } catch (err) {
        showToast(err.message, 'error');
    }
}

/* ==========================================================================
   Ticket Creation Modal (Customer Only)
   ========================================================================== */

function openCreateTicketModal() {
    document.getElementById('createTicketForm').reset();
    updateSlaBadgeNotice();
    openModal('modalCreateTicket');
}

function updateSlaBadgeNotice() {
    const prio = document.getElementById('ticketPriority').value;
    const hoursEl = document.getElementById('slaNoticeHours');
    let hours = '24 Hours';
    if (prio === 'CRITICAL') hours = '4 Hours (Immediate)';
    else if (prio === 'HIGH') hours = '12 Hours (Urgent)';
    else if (prio === 'MEDIUM') hours = '24 Hours (Standard)';
    else if (prio === 'LOW') hours = '72 Hours (Normal)';
    if (hoursEl) hoursEl.textContent = hours;
}

async function handleCreateTicket(e) {
    e.preventDefault();
    const title = document.getElementById('ticketTitle').value.trim();
    const category = document.getElementById('ticketCategory').value;
    const priority = document.getElementById('ticketPriority').value;
    const description = document.getElementById('ticketDescription').value.trim();

    try {
        const res = await apiRequest('/tickets', 'POST', {
            title,
            category,
            priority,
            description
        });

        if (!res) return;

        showToast(`Ticket #${res.id} created with SLA target: ${priority}`, 'success');
        closeModal('modalCreateTicket');
        loadTickets();
        loadStats();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

/* ==========================================================================
   Ticket Detail Modal & Status Transitions
   ========================================================================== */

async function openTicketDetailModal(ticketId) {
    currentSelectedTicketId = ticketId;

    try {
        const ticket = await apiRequest(`/tickets/${ticketId}`);
        if (!ticket) return;

        // Strict role-based authorization check
        if (currentUser && currentUser.role === 'ROLE_CUSTOMER') {
            const myName = (currentUser.fullName || '').trim().toLowerCase();
            const myId = currentUser.id ? Number(currentUser.id) : null;
            const isOwner = (ticket.creatorName && ticket.creatorName.trim().toLowerCase() === myName) ||
                            (myId && ticket.creatorId && Number(ticket.creatorId) === myId);
            if (!isOwner) {
                showToast('Access denied: You can only view your own tickets', 'error');
                return;
            }
        } else if (currentUser && currentUser.role === 'ROLE_SUPPORT_AGENT') {
            const myName = (currentUser.fullName || '').trim().toLowerCase();
            const isAssigned = (ticket.assignedAgentName && ticket.assignedAgentName.trim().toLowerCase() === myName);
            if (!isAssigned) {
                showToast('Access denied: You can only view tickets assigned to you', 'error');
                return;
            }
        }

        document.getElementById('detailTicketId').textContent = `#${ticket.id}`;
        document.getElementById('detailTicketTitle').textContent = ticket.title;
        document.getElementById('detailTicketCategory').textContent = ticket.category || 'General';

        // Badges
        const statusEl = document.getElementById('detailTicketStatus');
        statusEl.textContent = ticket.status;
        statusEl.className = `badge badge-status-${ticket.status}`;

        const priorityEl = document.getElementById('detailTicketPriority');
        priorityEl.textContent = ticket.priority;
        priorityEl.className = `badge badge-priority-${ticket.priority}`;

        document.getElementById('detailCreatorName').textContent = ticket.creatorName || `User #${ticket.creatorId}`;
        document.getElementById('detailAgentName').textContent = ticket.assignedAgentName || 'Unassigned';

        const slaMeta = formatSlaTime(ticket.slaDeadline, ticket.status);
        const slaEl = document.getElementById('detailSlaDeadline');
        slaEl.textContent = slaMeta.text;
        slaEl.className = `meta-val sla-countdown ${slaMeta.className}`;

        document.getElementById('detailDescriptionText').textContent = ticket.description;

        // Populate status dropdown with current value
        const statusSelect = document.getElementById('updateStatusSelect');
        if (statusSelect) {
            statusSelect.value = ticket.status;
        }

        // Load Comments
        loadComments(ticketId);

        openModal('modalTicketDetail');
    } catch (err) {
        showToast(err.message, 'error');
    }
}

async function handleUpdateStatus() {
    if (!currentSelectedTicketId) return;
    const targetStatus = document.getElementById('updateStatusSelect').value;

    try {
        const updated = await apiRequest(`/tickets/${currentSelectedTicketId}/status`, 'PUT', {
            status: targetStatus
        });

        showToast(`Ticket #${updated.id} status transitioned to ${updated.status}`, 'success');
        openTicketDetailModal(currentSelectedTicketId);
        loadTickets();
        loadStats();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

/* ==========================================================================
   Comments & Discussion
   ========================================================================== */

async function loadComments(ticketId) {
    const container = document.getElementById('commentsListContainer');
    const countLabel = document.getElementById('commentsCountLabel');
    container.innerHTML = '<span style="color:var(--text-muted);font-size:0.8rem;">Loading conversation...</span>';

    try {
        const comments = await apiRequest(`/tickets/${ticketId}/comments`);
        container.innerHTML = '';

        if (!comments || comments.length === 0) {
            container.innerHTML = '<span style="color:var(--text-muted);font-size:0.85rem;">No comments yet. Start the conversation below.</span>';
            if (countLabel) countLabel.textContent = '0';
            return;
        }

        if (countLabel) countLabel.textContent = comments.length;

        comments.forEach(c => {
            const bubble = document.createElement('div');
            bubble.className = `comment-bubble ${c.isInternal ? 'internal' : ''}`;

            const canDelete = (currentUser.role === 'ROLE_SUPERVISOR') || (c.authorName === currentUser.fullName);
            const deleteBtn = canDelete && (currentUser.role !== 'ROLE_CUSTOMER')
                ? `<button class="btn btn-outline btn-sm text-danger" style="padding:2px 6px;font-size:0.7rem;" onclick="deleteComment(${c.id})">Delete</button>` 
                : '';

            const internalTag = c.isInternal ? `<span class="internal-badge">🔒 INTERNAL NOTE</span>` : '';
            const roleTag = c.authorRole ? `<span class="comment-role-tag">${c.authorRole.replace('ROLE_', '')}</span>` : '';

            bubble.innerHTML = `
                <div class="comment-header-row">
                    <div>
                        <span class="comment-author">${escapeHtml(c.authorName || 'Agent')}</span>
                        ${roleTag}
                        ${internalTag}
                    </div>
                    <div style="display:flex;align-items:center;gap:8px;">
                        <span class="comment-time">${formatDate(c.createdAt)}</span>
                        ${deleteBtn}
                    </div>
                </div>
                <div class="comment-body">${escapeHtml(c.content)}</div>
            `;
            container.appendChild(bubble);
        });

        // Scroll to bottom
        container.scrollTop = container.scrollHeight;
    } catch (err) {
        container.innerHTML = `<span class="text-danger">Failed to load comments: ${err.message}</span>`;
    }
}

async function handleAddComment(e) {
    e.preventDefault();
    if (!currentSelectedTicketId) return;

    const contentInput = document.getElementById('commentContent');
    const content = contentInput.value.trim();
    const isInternal = document.getElementById('commentIsInternal').checked;

    try {
        await apiRequest(`/tickets/${currentSelectedTicketId}/comments`, 'POST', {
            content,
            isInternal
        });

        contentInput.value = '';
        document.getElementById('commentIsInternal').checked = false;
        showToast('Comment posted', 'success');
        loadComments(currentSelectedTicketId);
        loadTickets();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

async function deleteComment(commentId) {
    if (!confirm('Are you sure you want to delete this comment?')) return;
    try {
        await apiRequest(`/tickets/${currentSelectedTicketId}/comments/${commentId}`, 'DELETE');
        showToast('Comment deleted', 'info');
        loadComments(currentSelectedTicketId);
        loadTickets();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

/* ==========================================================================
   Agent Workload & Capacity (Supervisor)
   ========================================================================== */

async function loadAgents() {
    const listEl = document.getElementById('agentsList');
    listEl.innerHTML = '<span style="color:var(--text-muted);font-size:0.9rem;">Loading agent roster...</span>';

    try {
        const agents = await apiRequest('/agents/workload');
        allAgents = agents || [];
        listEl.innerHTML = '';

        // Update fleet summary bar metrics
        const totalAgents = allAgents.length;
        const availableAgents = allAgents.filter(a => a.isAvailable).length;
        const totalLoad = allAgents.reduce((sum, a) => sum + (a.currentLoad || 0), 0);
        const totalCapacity = allAgents.reduce((sum, a) => sum + (a.maxCapacity || 5), 0);
        const utilization = totalCapacity > 0 ? Math.round((totalLoad / totalCapacity) * 100) : 0;

        const elTotal = document.getElementById('fleetTotalAgents');
        const elAvail = document.getElementById('fleetAvailableAgents');
        const elLoad = document.getElementById('fleetTotalLoad');
        const elUtil = document.getElementById('fleetUtilization');

        if (elTotal) elTotal.textContent = totalAgents;
        if (elAvail) elAvail.textContent = `${availableAgents} / ${totalAgents}`;
        if (elLoad) elLoad.textContent = `${totalLoad} / ${totalCapacity}`;
        if (elUtil) elUtil.textContent = `${utilization}%`;

        if (allAgents.length === 0) {
            listEl.innerHTML = `
                <div class="empty-state" style="grid-column: 1 / -1;">
                    <div style="font-size:2.5rem;margin-bottom:12px;">👥</div>
                    <h3>No Agents Registered</h3>
                    <p>Register an account with role ROLE_SUPPORT_AGENT to populate this roster.</p>
                </div>
            `;
            return;
        }

        allAgents.forEach(agent => {
            const card = document.createElement('div');
            card.className = 'agent-card';

            const maxCap = agent.maxCapacity || 5;
            const currLoad = agent.currentLoad || 0;
            const loadPercentage = agent.loadPercentage !== undefined ? agent.loadPercentage : Math.round((currLoad / maxCap) * 100);
            
            let barColorClass = 'load-low';
            if (loadPercentage >= 80) barColorClass = 'load-high';
            else if (loadPercentage >= 50) barColorClass = 'load-med';

            const statusBadge = agent.isAvailable 
                ? `<span class="badge badge-status-RESOLVED"><span class="status-pulse-dot online"></span>AVAILABLE</span>` 
                : `<span class="badge badge-status-CLOSED"><span class="status-pulse-dot offline"></span>AWAY</span>`;

            card.innerHTML = `
                <div class="agent-card-content">
                    <div class="agent-card-header">
                        <div class="agent-profile-wrap">
                            <div class="agent-avatar">${(agent.fullName || 'A').charAt(0).toUpperCase()}</div>
                            <div class="agent-info">
                                <h4 class="agent-name">${escapeHtml(agent.fullName || 'Support Agent')}</h4>
                                <div class="agent-meta-tags">
                                    <span class="agent-dept">
                                        <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
                                        ${escapeHtml(agent.department || 'General Support')}
                                    </span>
                                    <span class="agent-email">
                                        <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/><polyline points="22,6 12,13 2,6"/></svg>
                                        ${escapeHtml(agent.email || '')}
                                    </span>
                                </div>
                            </div>
                        </div>
                        ${statusBadge}
                    </div>

                    <div class="agent-workload-wrap">
                        <div class="workload-meta">
                            <span class="workload-label">
                                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="2" y="7" width="20" height="14" rx="2" ry="2"/><path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16"/></svg>
                                Active Workload
                            </span>
                            <span class="workload-percent">${currLoad} / ${maxCap} (${loadPercentage}%)</span>
                        </div>
                        <div class="workload-bar-track">
                            <div class="workload-bar-fill ${barColorClass}" style="width: ${Math.min(100, Math.max(loadPercentage > 0 ? 6 : 0, loadPercentage))}%;"></div>
                        </div>
                        <div class="workload-sub-metrics">
                            <span class="metric-pill">Max: <strong>${maxCap}</strong></span>
                            <span class="metric-pill">Assigned: <strong>${currLoad}</strong></span>
                            <span class="metric-pill">Remaining: <strong>${Math.max(0, maxCap - currLoad)}</strong></span>
                        </div>
                    </div>
                </div>

                <div class="agent-actions">
                    <button class="btn btn-outline btn-sm flex-1 ${agent.isAvailable ? 'btn-set-away' : 'btn-set-available'}" onclick="toggleAgentAvailability(${agent.id})">
                        ${agent.isAvailable 
                            ? `<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="10" y1="15" x2="10" y2="9"/><line x1="14" y1="15" x2="14" y2="9"/></svg><span>Set Away</span>` 
                            : `<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="20 6 9 17 4 12"/></svg><span>Set Available</span>`
                        }
                    </button>
                    <button class="btn btn-outline btn-sm flex-1" onclick="recalculateAgentLoad(${agent.id})" title="Recompute load from DB assignments">
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="23 4 23 10 17 10"/><polyline points="1 20 1 14 7 14"/><path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"/></svg>
                        <span>Sync Load</span>
                    </button>
                </div>
            `;
            listEl.appendChild(card);
        });
    } catch (err) {
        showToast(err.message, 'error');
    }
}

async function toggleAgentAvailability(agentId) {
    try {
        await apiRequest(`/agents/${agentId}/toggle-availability`, 'PUT');
        showToast('Agent availability toggled', 'success');
        loadAgents();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

async function recalculateAgentLoad(agentId) {
    try {
        await apiRequest(`/agents/${agentId}/recalculate`, 'POST');
        showToast('Agent active load synced from database assignments', 'success');
        loadAgents();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

/* ==========================================================================
   Assignment & Auto-Assignment (Supervisor)
   ========================================================================== */

async function openAssignModal(ticketId) {
    currentSelectedTicketId = ticketId;
    const select = document.getElementById('assignAgentSelect');
    select.innerHTML = '<option value="">Loading available agents...</option>';

    try {
        const agents = await apiRequest('/agents');
        select.innerHTML = '';

        if (!agents || agents.length === 0) {
            select.innerHTML = '<option value="">No support agents found</option>';
            return;
        }

        agents.forEach(a => {
            const opt = document.createElement('option');
            opt.value = a.id;
            const statusNotice = a.isAvailable ? 'Available' : 'Away';
            opt.textContent = `${a.fullName} (${a.currentLoad}/${a.maxCapacity} assigned) - ${statusNotice}`;
            if (!a.isAvailable || a.currentLoad >= a.maxCapacity) {
                opt.textContent += ' [AT CAPACITY / UNAVAILABLE]';
            }
            select.appendChild(opt);
        });

        openModal('modalAssignAgent');
    } catch (err) {
        showToast(err.message, 'error');
    }
}

async function handleManualAssign(e) {
    e.preventDefault();
    const agentId = document.getElementById('assignAgentSelect').value;
    if (!agentId || !currentSelectedTicketId) return;

    try {
        await apiRequest('/assignments', 'POST', {
            ticketId: Number(currentSelectedTicketId),
            agentId: Number(agentId)
        });

        showToast('Ticket assigned successfully', 'success');
        closeModal('modalAssignAgent');
        openTicketDetailModal(currentSelectedTicketId);
        loadTickets();
        loadStats();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

async function handleAutoAssign(ticketId) {
    if (!ticketId) return;
    try {
        const res = await apiRequest(`/assignments/auto-assign/${ticketId}`, 'POST');
        showToast(res.message || 'Auto-assignment successful', 'success');
        openTicketDetailModal(ticketId);
        loadTickets();
        loadStats();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

async function handleUnassign(ticketId) {
    if (!ticketId) return;
    if (!confirm('Are you sure you want to unassign this ticket? Status will reset to OPEN.')) return;

    try {
        const res = await apiRequest(`/assignments/${ticketId}`, 'DELETE');
        showToast(res.message || 'Ticket unassigned successfully', 'info');
        openTicketDetailModal(ticketId);
        loadTickets();
        loadStats();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

/* ==========================================================================
   Escalation Management (Supervisor & Agent)
   ========================================================================== */

function openEscalateModal(ticketId) {
    currentSelectedTicketId = ticketId;
    document.getElementById('escalationReason').value = '';
    openModal('modalEscalateTicket');
}

async function handleSubmitEscalation(e) {
    e.preventDefault();
    if (!currentSelectedTicketId) return;

    const reason = document.getElementById('escalationReason').value.trim();

    try {
        const res = await apiRequest('/escalations', 'POST', {
            ticketId: Number(currentSelectedTicketId),
            reason
        });

        showToast(`Ticket escalated to Level ${res.escalationLevel}! Priority auto-bumped.`, 'warning');
        closeModal('modalEscalateTicket');
        openTicketDetailModal(currentSelectedTicketId);
        loadTickets();
        loadStats();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

async function loadEscalations() {
    const tbody = document.getElementById('escalationsTableBody');
    tbody.innerHTML = '<tr><td colspan="7" style="text-align:center;color:var(--text-muted);padding:24px;">Loading escalation queue...</td></tr>';

    try {
        const endpoint = onlyUnresolvedEscalations ? '/escalations/unresolved' : '/escalations';
        const escalations = await apiRequest(endpoint);
        tbody.innerHTML = '';

        if (!escalations || escalations.length === 0) {
            tbody.innerHTML = '<tr><td colspan="7" style="text-align:center;color:var(--text-muted);padding:32px;">No escalations in queue.</td></tr>';
            return;
        }

        escalations.forEach(esc => {
            const tr = document.createElement('tr');
            const resolveBtn = (!esc.isResolved && currentUser.role === 'ROLE_SUPERVISOR')
                ? `<button class="btn btn-secondary btn-sm" onclick="handleResolveEscalation(${esc.id})">Resolve & Resume</button>`
                : (esc.isResolved ? `<span style="color:#34d399;font-weight:600;font-size:0.8rem;">Resolved</span>` : `<span style="color:#94a3b8;font-size:0.8rem;">Supervisor Pending</span>`);

            tr.innerHTML = `
                <td>
                    <a href="javascript:void(0)" onclick="openTicketDetailModal(${esc.ticketId})" style="color:#a5b4fc;font-weight:700;text-decoration:none;">
                        #${esc.ticketId} - ${escapeHtml(esc.ticketTitle || 'Ticket')}
                    </a>
                </td>
                <td><span class="level-badge level-${esc.escalationLevel}">L${esc.escalationLevel}</span></td>
                <td style="max-width:280px;line-height:1.4;">${escapeHtml(esc.reason)}</td>
                <td>${escapeHtml(esc.escalatedByName || 'Staff')}</td>
                <td style="font-size:0.78rem;color:var(--text-muted);">${formatDate(esc.escalatedAt)}</td>
                <td>
                    ${esc.isResolved 
                        ? `<span class="badge badge-status-RESOLVED">RESOLVED</span>` 
                        : `<span class="badge badge-status-ESCALATED">ACTIVE</span>`}
                </td>
                <td>${resolveBtn}</td>
            `;
            tbody.appendChild(tr);
        });
    } catch (err) {
        showToast(err.message, 'error');
    }
}

function toggleEscalationFilter(unresolvedOnly) {
    onlyUnresolvedEscalations = unresolvedOnly;
    document.getElementById('btnFilterUnresolvedEsc').classList.toggle('active', unresolvedOnly);
    document.getElementById('btnFilterAllEsc').classList.toggle('active', !unresolvedOnly);
    loadEscalations();
}

async function handleResolveEscalation(escalationId) {
    try {
        await apiRequest(`/escalations/${escalationId}/resolve`, 'PUT');
        showToast('Escalation resolved! Ticket returned to IN_PROGRESS.', 'success');
        loadEscalations();
        loadStats();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

/* ==========================================================================
   Modal Helpers & Backdrop (Lock Background Scrolling)
   ========================================================================== */

function openModal(modalId) {
    const el = document.getElementById(modalId);
    if (el) {
        el.classList.remove('hidden');
        document.body.classList.add('modal-open');
    }
}

function closeModal(modalId) {
    const el = document.getElementById(modalId);
    if (el) {
        el.classList.add('hidden');
        const anyOpen = document.querySelectorAll('.modal-overlay:not(.hidden)').length > 0;
        if (!anyOpen) {
            document.body.classList.remove('modal-open');
        }
    }
}

function closeModalOnBackdrop(event, modalId) {
    if (event.target.id === modalId) {
        closeModal(modalId);
    }
}

/* ==========================================================================
   Utility Functions
   ========================================================================== */

function showToast(message, type = 'info') {
    const container = document.getElementById('toastContainer');
    if (!container) return;

    // Avoid stacking more than 3 toasts
    while (container.children.length >= 3) {
        container.removeChild(container.firstChild);
    }

    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;

    let icon = 'ℹ️';
    if (type === 'success') icon = '✅';
    if (type === 'error') icon = '⚠️';
    if (type === 'warning') icon = '⚡';

    toast.innerHTML = `
        <span>${icon}</span>
        <div style="flex:1;">${escapeHtml(message)}</div>
    `;

    container.appendChild(toast);

    setTimeout(() => {
        toast.style.opacity = '0';
        toast.style.transform = 'translateY(10px)';
        toast.style.transition = '0.3s ease';
        setTimeout(() => toast.remove(), 300);
    }, 4200);
}

function formatSlaTime(deadlineStr, status) {
    if (status === 'RESOLVED' || status === 'CLOSED') {
        return { text: 'COMPLETED', className: 'sla-safe' };
    }
    if (!deadlineStr) {
        return { text: 'NO SLA', className: 'sla-safe' };
    }

    const deadline = new Date(deadlineStr);
    const now = new Date();
    const diffMs = deadline - now;

    if (diffMs <= 0) {
        return { text: 'SLA BREACHED', className: 'sla-breached' };
    }

    const diffHours = Math.floor(diffMs / (1000 * 60 * 60));
    const diffMins = Math.floor((diffMs % (1000 * 60 * 60)) / (1000 * 60));

    if (diffHours < 24) {
        return { text: `${diffHours}h ${diffMins}m left`, className: 'sla-warning' };
    }
    const days = Math.floor(diffHours / 24);
    return { text: `${days}d ${diffHours % 24}h left`, className: 'sla-safe' };
}

function formatDate(isoStr) {
    if (!isoStr) return '-';
    const d = new Date(isoStr);
    return d.toLocaleString([], { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' });
}

function escapeHtml(str) {
    if (!str) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}
