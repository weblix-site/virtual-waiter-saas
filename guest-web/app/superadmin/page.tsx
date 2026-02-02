"use client";

import { useEffect, useMemo, useRef, useState } from "react";

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

type HallDto = {
  id: number;
  branchId: number;
  name: string;
  isActive: boolean;
  sortOrder: number;
  backgroundUrl?: string | null;
  zonesJson?: string | null;
  activePlanId?: number | null;
};

type HallPlanDto = {
  id: number;
  hallId: number;
  name: string;
  isActive: boolean;
  sortOrder: number;
  backgroundUrl?: string | null;
  zonesJson?: string | null;
};

type TableDto = {
  id: number;
  number: number;
  publicId: string;
  assignedWaiterId?: number | null;
  hallId?: number | null;
  layoutX?: number | null;
  layoutY?: number | null;
  layoutW?: number | null;
  layoutH?: number | null;
  layoutShape?: string | null;
  layoutRotation?: number | null;
  layoutZone?: string | null;
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
  const [tenantStatusFilter, setTenantStatusFilter] = useState<"" | "ACTIVE" | "INACTIVE">("");
  const [branchStatusFilter, setBranchStatusFilter] = useState<"" | "ACTIVE" | "INACTIVE">("");
  const [statsFrom, setStatsFrom] = useState("");
  const [statsTo, setStatsTo] = useState("");
  const [stats, setStats] = useState<StatsSummary | null>(null);
  const [branchStats, setBranchStats] = useState<BranchSummaryRow[]>([]);
  const [tables, setTables] = useState<TableDto[]>([]);
  const [halls, setHalls] = useState<HallDto[]>([]);
  const [hallPlans, setHallPlans] = useState<HallPlanDto[]>([]);
  const [hallPlanId, setHallPlanId] = useState<number | "">("");
  const [hallId, setHallId] = useState<number | "">("");
  const [newHallName, setNewHallName] = useState("");
  const [newHallSort, setNewHallSort] = useState(0);
  const [selectedTableId, setSelectedTableId] = useState<number | null>(null);
  const [planEditMode, setPlanEditMode] = useState(true);
  const [snapEnabled, setSnapEnabled] = useState(true);
  const [planBgUrl, setPlanBgUrl] = useState("");
  const [planZones, setPlanZones] = useState<{ id: string; name: string; x: number; y: number; w: number; h: number; color: string }[]>([]);
  const planRef = useRef<HTMLDivElement | null>(null);
  const zoneDragRef = useRef<{
    id: string;
    startX: number;
    startY: number;
    baseX: number;
    baseY: number;
    baseW: number;
    baseH: number;
    mode: "move" | "resize";
    corner?: "nw" | "ne" | "sw" | "se" | "n" | "s" | "e" | "w";
  } | null>(null);
  const dragRef = useRef<{
    id: number;
    startX: number;
    startY: number;
    baseX: number;
    baseY: number;
    baseW: number;
    baseH: number;
    mode: "move" | "resize";
    corner?: "nw" | "ne" | "sw" | "se";
  } | null>(null);

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

  const waiterPalette = ["#FF6B6B", "#4ECDC4", "#FFD166", "#6C5CE7", "#00B894", "#FD79A8", "#0984E3"];
  const waiterColor = (id?: number | null) => {
    if (!id) return "#9aa0a6";
    return waiterPalette[id % waiterPalette.length];
  };

  const clamp = (v: number, min: number, max: number) => Math.min(Math.max(v, min), max);
  const snap = (v: number, step = 2) => (snapEnabled ? Math.round(v / step) * step : v);

  const layoutDefaults = (idx: number) => {
    const cols = 6;
    const col = idx % cols;
    const row = Math.floor(idx / cols);
    return {
      layoutX: 5 + col * 15,
      layoutY: 6 + row * 16,
      layoutW: 10,
      layoutH: 10,
      layoutShape: "ROUND",
      layoutRotation: 0,
      layoutZone: "",
    };
  };

  const getTableLayout = (t: TableDto, idx: number) => {
    const d = layoutDefaults(idx);
    return {
      layoutX: t.layoutX ?? d.layoutX,
      layoutY: t.layoutY ?? d.layoutY,
      layoutW: t.layoutW ?? d.layoutW,
      layoutH: t.layoutH ?? d.layoutH,
      layoutShape: t.layoutShape ?? d.layoutShape,
      layoutRotation: t.layoutRotation ?? d.layoutRotation,
      layoutZone: t.layoutZone ?? d.layoutZone,
    };
  };

  useEffect(() => {
    const handleMove = (e: PointerEvent) => {
      if (!planRef.current) return;
      const rect = planRef.current.getBoundingClientRect();
      if (rect.width <= 0 || rect.height <= 0) return;
      const startRef = zoneDragRef.current ?? dragRef.current;
      if (!startRef) return;
      const dx = ((e.clientX - startRef.startX) / rect.width) * 100;
      const dy = ((e.clientY - startRef.startY) / rect.height) * 100;
      if (zoneDragRef.current) {
        const z = zoneDragRef.current;
        let nx = z.baseX;
        let ny = z.baseY;
        let nw = z.baseW;
        let nh = z.baseH;
        const minSize = 6;
        if (z.mode === "move") {
          nx = snap(clamp(z.baseX + dx, 0, 100 - z.baseW));
          ny = snap(clamp(z.baseY + dy, 0, 100 - z.baseH));
        } else {
          const c = z.corner;
          if (c === "se") {
            nw = snap(clamp(z.baseW + dx, minSize, 100 - nx));
            nh = snap(clamp(z.baseH + dy, minSize, 100 - ny));
          } else if (c === "e") {
            nw = snap(clamp(z.baseW + dx, minSize, 100 - nx));
          } else if (c === "s") {
            nh = snap(clamp(z.baseH + dy, minSize, 100 - ny));
          } else if (c === "sw") {
            nw = snap(clamp(z.baseW - dx, minSize, 100));
            nx = snap(clamp(z.baseX + dx, 0, 100 - nw));
            nh = snap(clamp(z.baseH + dy, minSize, 100 - ny));
          } else if (c === "w") {
            nw = snap(clamp(z.baseW - dx, minSize, 100));
            nx = snap(clamp(z.baseX + dx, 0, 100 - nw));
          } else if (c === "ne") {
            nw = snap(clamp(z.baseW + dx, minSize, 100 - nx));
            nh = snap(clamp(z.baseH - dy, minSize, 100));
            ny = snap(clamp(z.baseY + dy, 0, 100 - nh));
          } else if (c === "n") {
            nh = snap(clamp(z.baseH - dy, minSize, 100));
            ny = snap(clamp(z.baseY + dy, 0, 100 - nh));
          } else if (c === "nw") {
            nw = snap(clamp(z.baseW - dx, minSize, 100));
            nx = snap(clamp(z.baseX + dx, 0, 100 - nw));
            nh = snap(clamp(z.baseH - dy, minSize, 100));
            ny = snap(clamp(z.baseY + dy, 0, 100 - nh));
          }
        }
        setPlanZones((prev) => prev.map((p) => (p.id === z.id ? { ...p, x: nx, y: ny, w: nw, h: nh } : p)));
        return;
      }
      if (!dragRef.current) return;
      setTables((prev) =>
        prev.map((t) => {
          if (t.id !== dragRef.current!.id) return t;
          const minSize = 4;
          const wBase = dragRef.current!.baseW;
          const hBase = dragRef.current!.baseH;
          let nx = dragRef.current!.baseX;
          let ny = dragRef.current!.baseY;
          let nw = wBase;
          let nh = hBase;
          if (dragRef.current!.mode === "move") {
            nx = snap(clamp(dragRef.current!.baseX + dx, 0, 100 - nw));
            ny = snap(clamp(dragRef.current!.baseY + dy, 0, 100 - nh));
          } else {
            const c = dragRef.current!.corner;
            if (c === "se") {
              nw = snap(clamp(wBase + dx, minSize, 100 - nx));
              nh = snap(clamp(hBase + dy, minSize, 100 - ny));
            } else if (c === "sw") {
              nw = snap(clamp(wBase - dx, minSize, 100));
              nx = snap(clamp(dragRef.current!.baseX + dx, 0, 100 - nw));
              nh = snap(clamp(hBase + dy, minSize, 100 - ny));
            } else if (c === "ne") {
              nw = snap(clamp(wBase + dx, minSize, 100 - nx));
              nh = snap(clamp(hBase - dy, minSize, 100));
              ny = snap(clamp(dragRef.current!.baseY + dy, 0, 100 - nh));
            } else if (c === "nw") {
              nw = snap(clamp(wBase - dx, minSize, 100));
              nx = snap(clamp(dragRef.current!.baseX + dx, 0, 100 - nw));
              nh = snap(clamp(hBase - dy, minSize, 100));
              ny = snap(clamp(dragRef.current!.baseY + dy, 0, 100 - nh));
            }
          }
          return { ...t, layoutX: nx, layoutY: ny, layoutW: nw, layoutH: nh };
        })
      );
    };
    const handleUp = () => {
      dragRef.current = null;
      zoneDragRef.current = null;
    };
    window.addEventListener("pointermove", handleMove);
    window.addEventListener("pointerup", handleUp);
    return () => {
      window.removeEventListener("pointermove", handleMove);
      window.removeEventListener("pointerup", handleUp);
    };
  }, []);

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
      const qsTenants = new URLSearchParams();
      if (tenantStatusFilter) qsTenants.set("isActive", tenantStatusFilter === "ACTIVE" ? "true" : "false");
      const res = await api(`/api/super/tenants${qsTenants.toString() ? `?${qsTenants.toString()}` : ""}`);
      setTenants(await res.json());
      const qsBranches = new URLSearchParams();
      if (tenantId) qsBranches.set("tenantId", String(tenantId));
      if (branchStatusFilter) qsBranches.set("isActive", branchStatusFilter === "ACTIVE" ? "true" : "false");
      const resBranches = await api(`/api/super/branches${qsBranches.toString() ? `?${qsBranches.toString()}` : ""}`);
      setBranches(await resBranches.json());
    } catch (e: any) {
      setError(e?.message ?? "Load error");
    }
  }

  useEffect(() => {
    loadTenants();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [authReady, tenantStatusFilter, branchStatusFilter, tenantId]);

  async function loadTables() {
    if (!branchId) return;
    const hallsRes = await api(`/api/admin/halls?branchId=${branchId}`);
    const hallsBody = await hallsRes.json();
    setHalls(hallsBody);
    if (!hallId && hallsBody.length > 0) {
      setHallId(hallsBody[0].id);
    }
    const res = await api(`/api/admin/tables?branchId=${branchId}`);
    setTables(await res.json());
  }

  async function saveTableLayout() {
    if (!branchId || !hallId) return;
    const payload = tables.map((t, idx) => {
      const layout = getTableLayout(t, idx);
      return {
        id: t.id,
        layoutX: layout.layoutX,
        layoutY: layout.layoutY,
        layoutW: layout.layoutW,
        layoutH: layout.layoutH,
        layoutShape: layout.layoutShape,
        layoutRotation: layout.layoutRotation,
        layoutZone: layout.layoutZone,
        hallId,
      };
    });
    await api(`/api/admin/tables/layout?branchId=${branchId}`, {
      method: "POST",
      body: JSON.stringify({ tables: payload }),
    });
    if (hallPlanId) {
      await api(`/api/admin/hall-plans/${hallPlanId}`, {
        method: "PATCH",
        body: JSON.stringify({ backgroundUrl: planBgUrl, zonesJson: JSON.stringify(planZones) }),
      });
      await api(`/api/admin/halls/${hallId}`, {
        method: "PATCH",
        body: JSON.stringify({ activePlanId: hallPlanId }),
      });
    } else {
      await api(`/api/admin/halls/${hallId}`, {
        method: "PATCH",
        body: JSON.stringify({ backgroundUrl: planBgUrl, zonesJson: JSON.stringify(planZones) }),
      });
    }
    loadTables();
  }

  function autoLayoutTables() {
    setTables((prev) =>
      prev.map((t, idx) => {
        const d = layoutDefaults(idx);
        return {
          ...t,
          layoutX: d.layoutX,
          layoutY: d.layoutY,
          layoutW: d.layoutW,
          layoutH: d.layoutH,
          layoutShape: d.layoutShape,
          layoutRotation: d.layoutRotation,
          layoutZone: d.layoutZone,
        };
      })
    );
  }

  const selectedTable = tables.find((t) => t.id === selectedTableId) ?? null;

  function updateSelectedTable(patch: Partial<TableDto>) {
    if (!selectedTable) return;
    setTables((prev) => prev.map((t) => (t.id === selectedTable.id ? { ...t, ...patch } : t)));
  }

  useEffect(() => {
    if (branchId) loadTables();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [branchId]);

  useEffect(() => {
    if (!branchId || !hallId) return;
    (async () => {
      try {
        const [hallRes, plansRes] = await Promise.all([
          api(`/api/admin/halls/${hallId}`),
          api(`/api/admin/halls/${hallId}/plans`),
        ]);
        const hall = await hallRes.json();
        const plans = await plansRes.json();
        setHallPlans(plans);
        const activePlan = hall?.activePlanId ?? (plans[0]?.id ?? null);
        setHallPlanId(activePlan ?? "");
        if (activePlan) {
          const plan = plans.find((p: HallPlanDto) => p.id === activePlan);
          setPlanBgUrl(plan?.backgroundUrl ?? "");
          if (plan?.zonesJson) {
            try {
              const parsed = JSON.parse(plan.zonesJson);
              if (Array.isArray(parsed)) setPlanZones(parsed);
            } catch (_) {}
          } else {
            setPlanZones([]);
          }
        } else {
          setPlanBgUrl(hall?.backgroundUrl ?? "");
          if (hall?.zonesJson) {
            try {
              const parsed = JSON.parse(hall.zonesJson);
              if (Array.isArray(parsed)) setPlanZones(parsed);
            } catch (_) {}
          } else {
            setPlanZones([]);
          }
        }
      } catch (_) {}
    })();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [hallId, branchId]);

  useEffect(() => {
    if (!hallPlanId) return;
    const plan = hallPlans.find((p) => p.id === hallPlanId);
    if (!plan) return;
    setPlanBgUrl(plan.backgroundUrl ?? "");
    if (plan.zonesJson) {
      try {
        const parsed = JSON.parse(plan.zonesJson);
        if (Array.isArray(parsed)) setPlanZones(parsed);
      } catch (_) {}
    } else {
      setPlanZones([]);
    }
  }, [hallPlanId, hallPlans]);

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

  async function downloadSummaryCsv() {
    if (!tenantId) return;
    const qs = new URLSearchParams();
    qs.set("tenantId", String(tenantId));
    if (statsFrom) qs.set("from", statsFrom);
    if (statsTo) qs.set("to", statsTo);
    const res = await api(`/api/super/stats/summary.csv?${qs.toString()}`, { headers: { Authorization: authHeader } });
    const blob = await res.blob();
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = "tenant-summary.csv";
    document.body.appendChild(a);
    a.click();
    a.remove();
    URL.revokeObjectURL(url);
  }

  async function downloadBranchesCsv() {
    if (!tenantId) return;
    const qs = new URLSearchParams();
    qs.set("tenantId", String(tenantId));
    if (statsFrom) qs.set("from", statsFrom);
    if (statsTo) qs.set("to", statsTo);
    const res = await api(`/api/super/stats/branches.csv?${qs.toString()}`, { headers: { Authorization: authHeader } });
    const blob = await res.blob();
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = "tenant-branches.csv";
    document.body.appendChild(a);
    a.click();
    a.remove();
    URL.revokeObjectURL(url);
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
          <select value={tenantStatusFilter} onChange={(e) => setTenantStatusFilter(e.target.value as any)}>
            <option value="">All statuses</option>
            <option value="ACTIVE">Active</option>
            <option value="INACTIVE">Inactive</option>
          </select>
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
          <select value={branchStatusFilter} onChange={(e) => setBranchStatusFilter(e.target.value as any)}>
            <option value="">All statuses</option>
            <option value="ACTIVE">Active</option>
            <option value="INACTIVE">Inactive</option>
          </select>
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
        <h2>Floor Plan (by branch)</h2>
        <div style={{ display: "flex", gap: 8, flexWrap: "wrap", alignItems: "center" }}>
          <select value={branchId} onChange={(e) => setBranchId(e.target.value ? Number(e.target.value) : "")}>
            <option value="">Select branch</option>
            {branches.map((b) => (
              <option key={b.id} value={b.id}>{b.name}</option>
            ))}
          </select>
          <label style={{ display: "flex", alignItems: "center", gap: 6 }}>
            <input type="checkbox" checked={planEditMode} onChange={(e) => setPlanEditMode(e.target.checked)} />
            Edit mode
          </label>
          <label style={{ display: "flex", alignItems: "center", gap: 6 }}>
            <input type="checkbox" checked={snapEnabled} onChange={(e) => setSnapEnabled(e.target.checked)} />
            Snap to grid
          </label>
          <select value={hallId} onChange={(e) => setHallId(e.target.value ? Number(e.target.value) : "")} disabled={!branchId}>
            <option value="">Select hall</option>
            {halls.map((h) => (
              <option key={h.id} value={h.id}>{h.name}</option>
            ))}
          </select>
          <select value={hallPlanId} onChange={(e) => setHallPlanId(e.target.value ? Number(e.target.value) : "")} disabled={!hallId}>
            <option value="">Default plan</option>
            {hallPlans.map((p) => (
              <option key={p.id} value={p.id}>{p.name}</option>
            ))}
          </select>
          <button
            onClick={async () => {
              if (!hallId || !hallPlanId) return;
              await api(`/api/admin/halls/${hallId}`, { method: "PATCH", body: JSON.stringify({ activePlanId: hallPlanId }) });
              loadTables();
            }}
            disabled={!hallId || !hallPlanId}
          >
            Set active
          </button>
          <button
            onClick={async () => {
              if (!hallPlanId) return;
              await api(`/api/admin/hall-plans/${hallPlanId}`, { method: "DELETE" });
              setHallPlanId("");
              loadTables();
            }}
            disabled={!hallPlanId}
          >
            Delete plan
          </button>
          <button onClick={loadTables} disabled={!branchId}>Load tables</button>
          <button onClick={autoLayoutTables} disabled={!branchId}>Auto layout</button>
          <button onClick={saveTableLayout} disabled={!branchId || !hallId}>Save layout</button>
        </div>
        <div style={{ display: "flex", gap: 8, flexWrap: "wrap", alignItems: "center", marginTop: 8 }}>
          <input placeholder="New hall name" value={newHallName} onChange={(e) => setNewHallName(e.target.value)} />
          <input type="number" placeholder="Sort" value={newHallSort} onChange={(e) => setNewHallSort(Number(e.target.value))} />
          <button
            onClick={async () => {
              if (!branchId || !newHallName.trim()) return;
              await api(`/api/admin/halls?branchId=${branchId}`, { method: "POST", body: JSON.stringify({ name: newHallName.trim(), sortOrder: newHallSort }) });
              setNewHallName("");
              setNewHallSort(0);
              loadTables();
            }}
            disabled={!branchId}
          >
            Add hall
          </button>
          <input placeholder="New plan name" value={newPlanName} onChange={(e) => setNewPlanName(e.target.value)} />
          <input type="number" placeholder="Plan sort" value={newPlanSort} onChange={(e) => setNewPlanSort(Number(e.target.value))} />
          <button
            onClick={async () => {
              if (!hallId || !newPlanName.trim()) return;
              await api(`/api/admin/halls/${hallId}/plans`, { method: "POST", body: JSON.stringify({ name: newPlanName.trim(), sortOrder: newPlanSort }) });
              setNewPlanName("");
              setNewPlanSort(0);
              loadTables();
            }}
            disabled={!hallId}
          >
            Add plan
          </button>
        </div>
        <div style={{ display: "flex", gap: 8, flexWrap: "wrap", alignItems: "center", marginTop: 8 }}>
          <label>
            Background URL
            <input
              placeholder="https://.../floor.png"
              value={planBgUrl}
              onChange={(e) => setPlanBgUrl(e.target.value)}
              style={{ minWidth: 260 }}
            />
          </label>
        </div>
        <div style={{ display: "grid", gridTemplateColumns: "1.3fr 0.7fr", gap: 16, marginTop: 12 }}>
          <div
            ref={planRef}
            style={{
              position: "relative",
              height: 520,
              borderRadius: 16,
              border: "1px solid #e6e6e6",
              background:
                "radial-gradient(circle at 10% 10%, rgba(78,205,196,0.08), transparent 40%)," +
                "radial-gradient(circle at 80% 20%, rgba(108,92,231,0.08), transparent 45%)," +
                "linear-gradient(180deg, #fafafa, #f5f5f7)",
              overflow: "hidden",
            }}
          >
            {planBgUrl && (
              <div
                style={{
                  position: "absolute",
                  inset: 0,
                  backgroundImage: `url(${planBgUrl})`,
                  backgroundSize: "cover",
                  backgroundPosition: "center",
                  opacity: 0.35,
                }}
              />
            )}
            <div
              style={{
                position: "absolute",
                inset: 0,
                backgroundImage:
                  "linear-gradient(rgba(0,0,0,0.04) 1px, transparent 1px), linear-gradient(90deg, rgba(0,0,0,0.04) 1px, transparent 1px)",
                backgroundSize: "24px 24px",
                pointerEvents: "none",
              }}
            />
            {planZones.map((z) => (
              <div
                key={z.id}
                onPointerDown={(e) => {
                  if (!planEditMode) return;
                  zoneDragRef.current = {
                    id: z.id,
                    startX: e.clientX,
                    startY: e.clientY,
                    baseX: z.x,
                    baseY: z.y,
                    baseW: z.w,
                    baseH: z.h,
                    mode: "move",
                  };
                }}
                style={{
                  position: "absolute",
                  left: `${z.x}%`,
                  top: `${z.y}%`,
                  width: `${z.w}%`,
                  height: `${z.h}%`,
                  borderRadius: 16,
                  border: `1px dashed ${z.color}`,
                  background: `${z.color}22`,
                  display: "flex",
                  alignItems: "flex-start",
                  justifyContent: "flex-start",
                  padding: 6,
                  fontSize: 12,
                  color: "#333",
                }}
              >
                {z.name}
                {planEditMode && (
                  <>
                    {["nw", "ne", "sw", "se"].map((corner) => (
                      <div
                        key={corner}
                        onPointerDown={(e) => {
                          e.stopPropagation();
                          zoneDragRef.current = {
                            id: z.id,
                            startX: e.clientX,
                            startY: e.clientY,
                            baseX: z.x,
                            baseY: z.y,
                            baseW: z.w,
                            baseH: z.h,
                            mode: "resize",
                            corner: corner as "nw" | "ne" | "sw" | "se",
                          };
                        }}
                        style={{
                          position: "absolute",
                          width: 8,
                          height: 8,
                          background: "#111",
                          borderRadius: 2,
                          boxShadow: "0 0 0 2px #fff",
                          top: corner.includes("n") ? -4 : undefined,
                          bottom: corner.includes("s") ? -4 : undefined,
                          left: corner.includes("w") ? -4 : undefined,
                          right: corner.includes("e") ? -4 : undefined,
                          cursor: `${corner}-resize`,
                        }}
                      />
                    ))}
                  </>
                )}
              </div>
            ))}
            {tables
              .filter((t) => (hallId === "" ? true : t.hallId === hallId))
              .map((t, idx) => {
              const layout = getTableLayout(t, idx);
              const color = waiterColor(t.assignedWaiterId ?? null);
              const selected = t.id === selectedTableId;
              return (
                <div
                  key={t.id}
                  onPointerDown={(e) => {
                    if (!planEditMode) return;
                    dragRef.current = {
                      id: t.id,
                      startX: e.clientX,
                      startY: e.clientY,
                      baseX: layout.layoutX ?? 0,
                      baseY: layout.layoutY ?? 0,
                      baseW: layout.layoutW ?? 10,
                      baseH: layout.layoutH ?? 10,
                      mode: "move",
                    };
                    setSelectedTableId(t.id);
                  }}
                  onClick={() => setSelectedTableId(t.id)}
                  style={{
                    position: "absolute",
                    left: `${layout.layoutX}%`,
                    top: `${layout.layoutY}%`,
                    width: `${layout.layoutW}%`,
                    height: `${layout.layoutH}%`,
                    borderRadius: layout.layoutShape === "ROUND" ? 999 : 14,
                    border: selected ? `2px solid ${color}` : "1px solid rgba(0,0,0,0.12)",
                    background: selected ? "rgba(255,255,255,0.95)" : "rgba(255,255,255,0.9)",
                    boxShadow: selected ? "0 10px 28px rgba(0,0,0,0.18)" : "0 6px 18px rgba(0,0,0,0.12)",
                    transform: `rotate(${layout.layoutRotation ?? 0}deg)`,
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "center",
                    flexDirection: "column",
                    gap: 4,
                    cursor: planEditMode ? "grab" : "default",
                    userSelect: "none",
                  }}
                >
                  <div style={{ fontWeight: 700, fontSize: 16 }}>#{t.number}</div>
                  <div style={{ fontSize: 12, color }}>
                    {t.assignedWaiterId ? `Waiter #${t.assignedWaiterId}` : "Unassigned"}
                  </div>
                  {layout.layoutZone ? (
                    <div style={{ fontSize: 11, color: "#666" }}>{layout.layoutZone}</div>
                  ) : null}
                  {planEditMode && selected && (
                    <>
                      {["nw", "ne", "sw", "se"].map((corner) => (
                        <div
                          key={corner}
                          onPointerDown={(e) => {
                            e.stopPropagation();
                            dragRef.current = {
                              id: t.id,
                              startX: e.clientX,
                              startY: e.clientY,
                              baseX: layout.layoutX ?? 0,
                              baseY: layout.layoutY ?? 0,
                              baseW: layout.layoutW ?? 10,
                              baseH: layout.layoutH ?? 10,
                              mode: "resize",
                              corner: corner as "nw" | "ne" | "sw" | "se",
                            };
                          }}
                          style={{
                            position: "absolute",
                            width: 10,
                            height: 10,
                            background: "#111",
                            borderRadius: 2,
                            boxShadow: "0 0 0 2px #fff",
                            top: corner.includes("n") ? -4 : undefined,
                            bottom: corner.includes("s") ? -4 : undefined,
                            left: corner.includes("w") ? -4 : undefined,
                            right: corner.includes("e") ? -4 : undefined,
                            cursor: `${corner}-resize`,
                          }}
                        />
                      ))}
                      {["n", "s", "e", "w"].map((edge) => (
                        <div
                          key={edge}
                          onPointerDown={(e) => {
                            e.stopPropagation();
                            zoneDragRef.current = {
                              id: z.id,
                              startX: e.clientX,
                              startY: e.clientY,
                              baseX: z.x,
                              baseY: z.y,
                              baseW: z.w,
                              baseH: z.h,
                              mode: "resize",
                              corner: edge as "nw" | "ne" | "sw" | "se",
                            };
                          }}
                          style={{
                            position: "absolute",
                            width: edge === "n" || edge === "s" ? 18 : 8,
                            height: edge === "e" || edge === "w" ? 18 : 8,
                            background: "#111",
                            borderRadius: 4,
                            boxShadow: "0 0 0 2px #fff",
                            top: edge === "n" ? -6 : edge === "s" ? undefined : "50%",
                            bottom: edge === "s" ? -6 : undefined,
                            left: edge === "w" ? -6 : edge === "e" ? undefined : "50%",
                            right: edge === "e" ? -6 : undefined,
                            transform: edge === "n" || edge === "s" ? "translateX(-50%)" : "translateY(-50%)",
                            cursor: `${edge}-resize`,
                          }}
                        />
                      ))}
                    </>
                  )}
                </div>
              );
            })}
          </div>
          <div style={{ border: "1px solid #eee", borderRadius: 12, padding: 12, background: "#fff" }}>
            <h3 style={{ marginTop: 0 }}>Table settings</h3>
            {selectedTable ? (
              <div style={{ display: "flex", flexDirection: "column", gap: 10 }}>
                <div><strong>Table #{selectedTable.number}</strong></div>
                <label>
                  Shape
                  <select
                    value={selectedTable.layoutShape ?? "ROUND"}
                    onChange={(e) => updateSelectedTable({ layoutShape: e.target.value })}
                  >
                    <option value="ROUND">Round</option>
                    <option value="RECT">Rectangle</option>
                  </select>
                </label>
                <label>
                  Width (%)
                  <input
                    type="number"
                    min={4}
                    max={30}
                    value={selectedTable.layoutW ?? 10}
                    onChange={(e) => updateSelectedTable({ layoutW: Number(e.target.value) })}
                  />
                </label>
                <label>
                  Height (%)
                  <input
                    type="number"
                    min={4}
                    max={30}
                    value={selectedTable.layoutH ?? 10}
                    onChange={(e) => updateSelectedTable({ layoutH: Number(e.target.value) })}
                  />
                </label>
                <label>
                  Rotation (deg)
                  <input
                    type="number"
                    min={0}
                    max={360}
                    value={selectedTable.layoutRotation ?? 0}
                    onChange={(e) => updateSelectedTable({ layoutRotation: Number(e.target.value) })}
                  />
                </label>
                <label>
                  Zone
                  <input
                    placeholder="e.g. Terrace, Hall A"
                    value={selectedTable.layoutZone ?? ""}
                    onChange={(e) => updateSelectedTable({ layoutZone: e.target.value })}
                  />
                </label>
                <div style={{ display: "flex", gap: 8 }}>
                  <button onClick={saveTableLayout}>Save</button>
                  <button onClick={() => updateSelectedTable({ layoutShape: "ROUND", layoutW: 10, layoutH: 10, layoutRotation: 0 })}>
                    Reset
                  </button>
                </div>
              </div>
            ) : (
              <div style={{ color: "#666" }}>Click a table on the map to edit.</div>
            )}
            <div style={{ marginTop: 16, borderTop: "1px solid #eee", paddingTop: 12 }}>
              <h4 style={{ margin: 0 }}>Zones</h4>
              {planZones.map((z, zi) => (
                <div key={z.id} style={{ display: "grid", gridTemplateColumns: "1fr 60px 60px", gap: 6, marginTop: 8 }}>
                  <input
                    placeholder="Zone name"
                    value={z.name}
                    onChange={(e) =>
                      setPlanZones((prev) => prev.map((p, i) => (i === zi ? { ...p, name: e.target.value } : p)))
                    }
                  />
                  <input
                    type="color"
                    value={z.color}
                    onChange={(e) =>
                      setPlanZones((prev) => prev.map((p, i) => (i === zi ? { ...p, color: e.target.value } : p)))
                    }
                  />
                  <button onClick={() => setPlanZones((prev) => prev.filter((_, i) => i !== zi))}>Del</button>
                  <input
                    type="number"
                    min={0}
                    max={100}
                    value={z.x}
                    onChange={(e) =>
                      setPlanZones((prev) => prev.map((p, i) => (i === zi ? { ...p, x: Number(e.target.value) } : p)))
                    }
                    placeholder="X"
                  />
                  <input
                    type="number"
                    min={0}
                    max={100}
                    value={z.y}
                    onChange={(e) =>
                      setPlanZones((prev) => prev.map((p, i) => (i === zi ? { ...p, y: Number(e.target.value) } : p)))
                    }
                    placeholder="Y"
                  />
                  <input
                    type="number"
                    min={4}
                    max={100}
                    value={z.w}
                    onChange={(e) =>
                      setPlanZones((prev) => prev.map((p, i) => (i === zi ? { ...p, w: Number(e.target.value) } : p)))
                    }
                    placeholder="W"
                  />
                  <input
                    type="number"
                    min={4}
                    max={100}
                    value={z.h}
                    onChange={(e) =>
                      setPlanZones((prev) => prev.map((p, i) => (i === zi ? { ...p, h: Number(e.target.value) } : p)))
                    }
                    placeholder="H"
                  />
                </div>
              ))}
              <button
                style={{ marginTop: 8 }}
                onClick={() =>
                  setPlanZones((prev) => [
                    ...prev,
                    { id: String(Date.now()), name: "Zone", x: 10, y: 10, w: 30, h: 20, color: "#6C5CE7" },
                  ])
                }
              >
                Add zone
              </button>
            </div>
          </div>
        </div>
      </section>

      <section style={{ marginTop: 24 }}>
        <h2>Stats</h2>
        <div style={{ display: "flex", gap: 8, flexWrap: "wrap", alignItems: "center" }}>
          <label>From <input type="date" value={statsFrom} onChange={(e) => setStatsFrom(e.target.value)} /></label>
          <label>To <input type="date" value={statsTo} onChange={(e) => setStatsTo(e.target.value)} /></label>
          <button onClick={loadStats} disabled={!tenantId}>Load</button>
          <button onClick={downloadSummaryCsv} disabled={!tenantId}>Summary CSV</button>
          <button onClick={downloadBranchesCsv} disabled={!tenantId}>Branches CSV</button>
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
