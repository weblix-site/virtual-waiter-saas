"use client";

import { useEffect, useMemo, useState } from "react";

const API_BASE = process.env.NEXT_PUBLIC_API_BASE ?? "http://localhost:8080";

type Tenant = { id: number; name: string; isActive: boolean };

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

function money(priceCents: number, currency = "MDL") {
  return `${(priceCents / 100).toFixed(2)} ${currency}`;
}

export default function SuperAdminPage() {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [authReady, setAuthReady] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [tenants, setTenants] = useState<Tenant[]>([]);
  const [tenantId, setTenantId] = useState<number | "">("");
  const [statsFrom, setStatsFrom] = useState("");
  const [statsTo, setStatsTo] = useState("");
  const [stats, setStats] = useState<StatsSummary | null>(null);

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
        </div>
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
      </section>
    </main>
  );
}
