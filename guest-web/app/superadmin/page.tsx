"use client";

import { useEffect, useMemo, useState } from "react";

const API_BASE = process.env.NEXT_PUBLIC_API_BASE ?? "http://localhost:8080";

type Tenant = { id: number; name: string; isActive: boolean };
type Branch = { id: number; tenantId: number; name: string; isActive: boolean };
type StaffUser = { id: number; branchId: number | null; username: string; role: string; isActive: boolean };

type StatsSummary = {
  from: string;
  to: string;
  ordersCount: number;
  callsCount: number;
  paidBillsCount: number;
  grossCents: number;
  tipsCents: number;
  activeTablesCount: number;
};

type BranchSummaryRow = {
  branchId: number;
  branchName: string;
  ordersCount: number;
  callsCount: number;
  paidBillsCount: number;
  grossCents: number;
  tipsCents: number;
};

function money(priceCents: number, currency = "MDL") {
  return `${(priceCents / 100).toFixed(2)} ${currency}`;
}

export default function SuperAdminPage() {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [authReady, setAuthReady] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [tenants, setTenants] = useState<Tenant[]>([]);
  const [branches, setBranches] = useState<Branch[]>([]);
  const [staff, setStaff] = useState<StaffUser[]>([]);
  const [tenantId, setTenantId] = useState<number | "">("");
  const [branchId, setBranchId] = useState<number | "">("");
  const [statsFrom, setStatsFrom] = useState("");
  const [statsTo, setStatsTo] = useState("");
  const [stats, setStats] = useState<StatsSummary | null>(null);
  const [branchStats, setBranchStats] = useState<BranchSummaryRow[]>([]);

  const [newTenantName, setNewTenantName] = useState("");
  const [newBranchName, setNewBranchName] = useState("");
  const [newStaffUser, setNewStaffUser] = useState("");
  const [newStaffPass, setNewStaffPass] = useState("");
  const [newStaffRole, setNewStaffRole] = useState("ADMIN");
  const [editingStaffId, setEditingStaffId] = useState<number | null>(null);
  const [editStaffRole, setEditStaffRole] = useState("ADMIN");
  const [editStaffActive, setEditStaffActive] = useState(true);

  useEffect(() => {
    const u = localStorage.getItem("superUser") ?? "";
    const p = localStorage.getItem("superPass") ?? "";
    if (u && p) {
      setUsername(u);
      setPassword(p);
      setAuthReady(true);
    }
  }, []);

  const authHeader = useMemo(() => {
    if (!authReady) return "";
    return "Basic " + btoa(`${username}:${password}`);
  }, [authReady, username, password]);

  async function api(path: string, init?: RequestInit) {
    const res = await fetch(`${API_BASE}${path}`, {
      ...init,
      headers: {
        "Content-Type": "application/json",
        Authorization: authHeader,
        ...(init?.headers ?? {}),
      },
    });
    if (!res.ok) {
      const body = await res.json().catch(() => ({}));
      throw new Error(body?.message ?? `Request failed (${res.status})`);
    }
    return res;
  }

  async function login() {
    setError(null);
    try {
      await api("/api/super/tenants");
      localStorage.setItem("superUser", username);
      localStorage.setItem("superPass", password);
      setAuthReady(true);
    } catch (e: any) {
      setError(e?.message ?? "Auth error");
    }
  }

  async function loadTenants() {
    if (!authReady) return;
    setError(null);
    try {
      const res = await api("/api/super/tenants");
      setTenants(await res.json());
      const resBranches = await api("/api/super/branches");
      setBranches(await resBranches.json());
    } catch (e: any) {
      setError(e?.message ?? "Load error");
    }
  }

  useEffect(() => {
    loadTenants();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [authReady]);

  async function loadStats() {
    if (!tenantId) return;
    const qs = new URLSearchParams();
    qs.set("tenantId", String(tenantId));
    if (statsFrom) qs.set("from", statsFrom);
    if (statsTo) qs.set("to", statsTo);
    const res = await api(`/api/super/stats/summary?${qs.toString()}`);
    const body = await res.json();
    setStats(body);
    const resBranches = await api(`/api/super/stats/branches?${qs.toString()}`);
    setBranchStats(await resBranches.json());
  }

  async function createTenant() {
    await api("/api/super/tenants", {
      method: "POST",
      body: JSON.stringify({ name: newTenantName }),
    });
    setNewTenantName("");
    loadTenants();
  }

  async function toggleTenant(t: Tenant) {
    await api(`/api/super/tenants/${t.id}`, {
      method: "PATCH",
      body: JSON.stringify({ isActive: !t.isActive }),
    });
    loadTenants();
  }

  async function createBranch() {
    if (!tenantId) return;
    await api(`/api/super/tenants/${tenantId}/branches`, {
      method: "POST",
      body: JSON.stringify({ name: newBranchName }),
    });
    setNewBranchName("");
    loadTenants();
  }

  async function toggleBranch(b: Branch) {
    await api(`/api/super/branches/${b.id}`, {
      method: "PATCH",
      body: JSON.stringify({ isActive: !b.isActive }),
    });
    loadTenants();
  }

  async function loadStaff() {
    const res = await api("/api/admin/staff?branchId=" + (branchId || ""));
    setStaff(await res.json());
  }

  async function createStaff() {
    if (!branchId) return;
    await api("/api/super/staff", {
      method: "POST",
      body: JSON.stringify({ branchId, username: newStaffUser, password: newStaffPass, role: newStaffRole }),
    });
    setNewStaffUser("");
    setNewStaffPass("");
    setNewStaffRole("ADMIN");
    loadStaff();
  }

  async function updateStaff(id: number) {
    await api(`/api/super/staff/${id}`, {
      method: "PATCH",
      body: JSON.stringify({ role: editStaffRole, isActive: editStaffActive }),
    });
    setEditingStaffId(null);
    loadStaff();
  }

  async function resetStaffPassword(id: number) {
    const pass = prompt("New password");
    if (!pass) return;
    await api(`/api/super/staff/${id}`, {
      method: "PATCH",
      body: JSON.stringify({ password: pass }),
    });
    loadStaff();
  }

  async function deleteBranch(id: number) {
    await api(`/api/super/branches/${id}`, { method: "DELETE" });
    loadTenants();
  }

  async function deleteTenant(id: number) {
    await api(`/api/super/tenants/${id}`, { method: "DELETE" });
    loadTenants();
  }

  async function deleteStaff(id: number) {
    await api(`/api/super/staff/${id}`, { method: "DELETE" });
    loadStaff();
  }

  if (!authReady) {
    return (
      <main style={{ padding: 24, maxWidth: 520, margin: "0 auto", fontFamily: "system-ui, -apple-system, Segoe UI, Roboto" }}>
        <h1>Super Admin Login</h1>
        {error && <div style={{ color: "#b11e46" }}>{error}</div>}
        <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
          <input value={username} onChange={(e) => setUsername(e.target.value)} placeholder="Username" />
          <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} placeholder="Password" />
          <button onClick={login} style={{ padding: "10px 14px" }}>Login</button>
        </div>
      </main>
    );
  }

  return (
    <main style={{ padding: 16, maxWidth: 900, margin: "0 auto", fontFamily: "system-ui, -apple-system, Segoe UI, Roboto" }}>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
        <h1 style={{ margin: 0 }}>Super Admin</h1>
        <button onClick={loadTenants}>Refresh</button>
      </div>
      {error && <div style={{ color: "#b11e46", marginTop: 8 }}>{error}</div>}

      <section style={{ marginTop: 24 }}>
        <h2>Tenants</h2>
        <div style={{ display: "flex", gap: 8, flexWrap: "wrap", alignItems: "center" }}>
          <select value={tenantId} onChange={(e) => setTenantId(e.target.value ? Number(e.target.value) : "")}>
            <option value="">Select tenant</option>
            {tenants.map((t) => (
              <option key={t.id} value={t.id}>{t.name} {t.isActive ? "" : "(inactive)"}</option>
            ))}
          </select>
          <input placeholder="New tenant name" value={newTenantName} onChange={(e) => setNewTenantName(e.target.value)} />
          <button onClick={createTenant}>Create tenant</button>
        </div>
        <div style={{ marginTop: 10 }}>
          {tenants.map((t) => (
            <div key={t.id} style={{ display: "flex", gap: 8, alignItems: "center", padding: "4px 0" }}>
              <strong>{t.name}</strong>
              <span>{t.isActive ? "ACTIVE" : "INACTIVE"}</span>
              <button onClick={() => toggleTenant(t)}>{t.isActive ? "Disable" : "Enable"}</button>
              <button onClick={() => deleteTenant(t.id)}>Delete</button>
            </div>
          ))}
        </div>
      </section>

      <section style={{ marginTop: 24 }}>
        <h2>Branches</h2>
        <div style={{ display: "flex", gap: 8, flexWrap: "wrap", alignItems: "center" }}>
          <select value={tenantId} onChange={(e) => setTenantId(e.target.value ? Number(e.target.value) : "")}>
            <option value="">Tenant</option>
            {tenants.map((t) => (
              <option key={t.id} value={t.id}>{t.name}</option>
            ))}
          </select>
          <input placeholder="New branch name" value={newBranchName} onChange={(e) => setNewBranchName(e.target.value)} />
          <button onClick={createBranch} disabled={!tenantId}>Create branch</button>
        </div>
        <div style={{ marginTop: 10 }}>
          {branches.filter((b) => !tenantId || b.tenantId === tenantId).map((b) => (
            <div key={b.id} style={{ display: "flex", gap: 8, alignItems: "center", padding: "4px 0" }}>
              <strong>{b.name}</strong>
              <span>tenant #{b.tenantId}</span>
              <span>{b.isActive ? "ACTIVE" : "INACTIVE"}</span>
              <button onClick={() => toggleBranch(b)}>{b.isActive ? "Disable" : "Enable"}</button>
              <button onClick={() => deleteBranch(b.id)}>Delete</button>
            </div>
          ))}
        </div>
      </section>

      <section style={{ marginTop: 24 }}>
        <h2>Staff (global)</h2>
        <div style={{ display: "flex", gap: 8, flexWrap: "wrap", alignItems: "center" }}>
          <select value={branchId} onChange={(e) => setBranchId(e.target.value ? Number(e.target.value) : "")}>
            <option value="">Select branch</option>
            {branches.map((b) => (
              <option key={b.id} value={b.id}>{b.name}</option>
            ))}
          </select>
          <button onClick={loadStaff} disabled={!branchId}>Load staff</button>
        </div>
        <div style={{ marginTop: 10, display: "flex", gap: 8, flexWrap: "wrap" }}>
          <input placeholder="Username" value={newStaffUser} onChange={(e) => setNewStaffUser(e.target.value)} />
          <input placeholder="Password" value={newStaffPass} onChange={(e) => setNewStaffPass(e.target.value)} />
          <select value={newStaffRole} onChange={(e) => setNewStaffRole(e.target.value)}>
            <option value="WAITER">WAITER</option>
            <option value="KITCHEN">KITCHEN</option>
            <option value="ADMIN">ADMIN</option>
          </select>
          <button onClick={createStaff} disabled={!branchId}>Create staff</button>
        </div>
        <div style={{ marginTop: 10 }}>
          {staff.map((s) => (
            <div key={s.id} style={{ display: "flex", gap: 8, alignItems: "center", padding: "4px 0" }}>
              <strong>{s.username}</strong>
              <span>{s.role}</span>
              <span>{s.isActive ? "ACTIVE" : "INACTIVE"}</span>
              <button onClick={() => { setEditingStaffId(s.id); setEditStaffRole(s.role); setEditStaffActive(s.isActive); }}>Edit</button>
              <button onClick={() => resetStaffPassword(s.id)}>Reset password</button>
              <button onClick={() => deleteStaff(s.id)}>Delete</button>
            </div>
          ))}
        </div>
        {editingStaffId && (
          <div style={{ marginTop: 10, border: "1px dashed #ddd", padding: 10 }}>
            <div style={{ display: "flex", gap: 8, flexWrap: "wrap", alignItems: "center" }}>
              <select value={editStaffRole} onChange={(e) => setEditStaffRole(e.target.value)}>
                <option value="WAITER">WAITER</option>
                <option value="KITCHEN">KITCHEN</option>
                <option value="ADMIN">ADMIN</option>
                <option value="SUPER_ADMIN">SUPER_ADMIN</option>
              </select>
              <label><input type="checkbox" checked={editStaffActive} onChange={(e) => setEditStaffActive(e.target.checked)} /> Active</label>
              <button onClick={() => updateStaff(editingStaffId)}>Save</button>
              <button onClick={() => setEditingStaffId(null)}>Cancel</button>
            </div>
          </div>
        )}
      </section>

      <section style={{ marginTop: 24 }}>
        <h2>Stats</h2>
        <div style={{ display: "flex", gap: 8, flexWrap: "wrap", alignItems: "center" }}>
          <label>From <input type="date" value={statsFrom} onChange={(e) => setStatsFrom(e.target.value)} /></label>
          <label>To <input type="date" value={statsTo} onChange={(e) => setStatsTo(e.target.value)} /></label>
          <button onClick={loadStats} disabled={!tenantId}>Load</button>
        </div>
        {stats && (
          <div style={{ marginTop: 10, border: "1px solid #eee", borderRadius: 8, padding: 10 }}>
            <div>Period: {stats.from} → {stats.to}</div>
            <div>Orders: {stats.ordersCount}</div>
            <div>Waiter calls: {stats.callsCount}</div>
            <div>Paid bills: {stats.paidBillsCount}</div>
            <div>Gross: {money(stats.grossCents)}</div>
            <div>Tips: {money(stats.tipsCents)}</div>
            <div>Active tables: {stats.activeTablesCount}</div>
          </div>
        )}
        {branchStats.length > 0 && (
          <div style={{ marginTop: 12 }}>
            <h3>By Branch</h3>
            <table style={{ width: "100%", borderCollapse: "collapse" }}>
              <thead>
                <tr>
                  <th style={{ textAlign: "left", borderBottom: "1px solid #ddd", padding: "6px 4px" }}>Branch</th>
                  <th style={{ textAlign: "right", borderBottom: "1px solid #ddd", padding: "6px 4px" }}>Orders</th>
                  <th style={{ textAlign: "right", borderBottom: "1px solid #ddd", padding: "6px 4px" }}>Calls</th>
                  <th style={{ textAlign: "right", borderBottom: "1px solid #ddd", padding: "6px 4px" }}>Paid bills</th>
                  <th style={{ textAlign: "right", borderBottom: "1px solid #ddd", padding: "6px 4px" }}>Gross</th>
                  <th style={{ textAlign: "right", borderBottom: "1px solid #ddd", padding: "6px 4px" }}>Tips</th>
                </tr>
              </thead>
              <tbody>
                {branchStats.map((r) => (
                  <tr key={r.branchId}>
                    <td style={{ padding: "6px 4px", borderBottom: "1px solid #f0f0f0" }}>{r.branchName}</td>
                    <td style={{ padding: "6px 4px", textAlign: "right", borderBottom: "1px solid #f0f0f0" }}>{r.ordersCount}</td>
                    <td style={{ padding: "6px 4px", textAlign: "right", borderBottom: "1px solid #f0f0f0" }}>{r.callsCount}</td>
                    <td style={{ padding: "6px 4px", textAlign: "right", borderBottom: "1px solid #f0f0f0" }}>{r.paidBillsCount}</td>
                    <td style={{ padding: "6px 4px", textAlign: "right", borderBottom: "1px solid #f0f0f0" }}>{money(r.grossCents)}</td>
                    <td style={{ padding: "6px 4px", textAlign: "right", borderBottom: "1px solid #f0f0f0" }}>{money(r.tipsCents)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      <section style={{ marginTop: 24 }}>
        <h2>RBAC Matrix</h2>
        <table style={{ width: "100%", borderCollapse: "collapse" }}>
          <thead>
            <tr>
              <th style={{ textAlign: "left", borderBottom: "1px solid #ddd", padding: "6px 4px" }}>Role</th>
              <th style={{ textAlign: "left", borderBottom: "1px solid #ddd", padding: "6px 4px" }}>Access</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td style={{ padding: "6px 4px" }}>WAITER</td>
              <td style={{ padding: "6px 4px" }}>Orders, Waiter calls, Bill requests, Confirm paid</td>
            </tr>
            <tr>
              <td style={{ padding: "6px 4px" }}>KITCHEN</td>
              <td style={{ padding: "6px 4px" }}>Kitchen queue, Order status updates</td>
            </tr>
            <tr>
              <td style={{ padding: "6px 4px" }}>ADMIN</td>
              <td style={{ padding: "6px 4px" }}>Menu, Tables/QR, Staff, Settings, Stats</td>
            </tr>
            <tr>
              <td style={{ padding: "6px 4px" }}>SUPER_ADMIN</td>
              <td style={{ padding: "6px 4px" }}>Tenants/Branches, Global Staff, Global Stats</td>
            </tr>
          </tbody>
        </table>
      </section>
    </main>
  );
}
