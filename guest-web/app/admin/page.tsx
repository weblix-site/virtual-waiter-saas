"use client";

import { useEffect, useMemo, useState } from "react";

const API_BASE = process.env.NEXT_PUBLIC_API_BASE ?? "http://localhost:8080";

type Category = {
  id: number;
  nameRu: string;
  nameRo?: string | null;
  nameEn?: string | null;
  sortOrder: number;
  isActive: boolean;
};

type MenuItem = {
  id: number;
  categoryId: number;
  nameRu: string;
  nameRo?: string | null;
  nameEn?: string | null;
  descriptionRu?: string | null;
  descriptionRo?: string | null;
  descriptionEn?: string | null;
  ingredientsRu?: string | null;
  ingredientsRo?: string | null;
  ingredientsEn?: string | null;
  allergens?: string | null;
  weight?: string | null;
  tags?: string | null;
  photoUrls?: string | null;
  kcal?: number | null;
  proteinG?: number | null;
  fatG?: number | null;
  carbsG?: number | null;
  priceCents: number;
  currency: string;
  isActive: boolean;
  isStopList: boolean;
};

type TableDto = {
  id: number;
  number: number;
  publicId: string;
  assignedWaiterId?: number | null;
};

type StaffUser = {
  id: number;
  branchId: number | null;
  username: string;
  role: string;
  isActive: boolean;
};

type BranchSettings = {
  branchId: number;
  requireOtpForFirstOrder: boolean;
  otpTtlSeconds: number;
  otpMaxAttempts: number;
  otpResendCooldownSeconds: number;
  otpLength: number;
  otpDevEchoCode: boolean;
  enablePartyPin: boolean;
  allowPayOtherGuestsItems: boolean;
  allowPayWholeTable: boolean;
  tipsEnabled: boolean;
  tipsPercentages: number[];
  payCashEnabled: boolean;
  payTerminalEnabled: boolean;
};

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

type StatsDailyRow = {
  day: string;
  ordersCount: number;
  callsCount: number;
  paidBillsCount: number;
  grossCents: number;
  tipsCents: number;
};

type TopItemRow = {
  menuItemId: number;
  name: string;
  qty: number;
  grossCents: number;
};

type TopCategoryRow = {
  categoryId: number;
  name: string;
  qty: number;
  grossCents: number;
};

type ModifierGroup = {
  id: number;
  nameRu: string;
  nameRo?: string | null;
  nameEn?: string | null;
  isActive: boolean;
};

type ModifierOption = {
  id: number;
  groupId: number;
  nameRu: string;
  nameRo?: string | null;
  nameEn?: string | null;
  priceCents: number;
  isActive: boolean;
};

type ItemModifierGroup = {
  groupId: number;
  isRequired: boolean;
  minSelect?: number | null;
  maxSelect?: number | null;
  sortOrder: number;
};

type AuditLog = {
  id: number;
  createdAt: string;
  actorUserId?: number | null;
  actorUsername?: string | null;
  actorRole?: string | null;
  branchId?: number | null;
  action: string;
  entityType: string;
  entityId?: number | null;
  detailsJson?: string | null;
};

function money(priceCents: number, currency = "MDL") {
  return `${(priceCents / 100).toFixed(2)} ${currency}`;
}

export default function AdminPage() {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [authReady, setAuthReady] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [categories, setCategories] = useState<Category[]>([]);
  const [items, setItems] = useState<MenuItem[]>([]);
  const [tables, setTables] = useState<TableDto[]>([]);
  const [staff, setStaff] = useState<StaffUser[]>([]);
  const [settings, setSettings] = useState<BranchSettings | null>(null);
  const [modGroups, setModGroups] = useState<ModifierGroup[]>([]);
  const [modOptions, setModOptions] = useState<Record<number, ModifierOption[]>>({});
  const [itemModGroups, setItemModGroups] = useState<Record<number, ItemModifierGroup[]>>({});
  const [qrByTable, setQrByTable] = useState<Record<number, string>>({});
  const [statsFrom, setStatsFrom] = useState("");
  const [statsTo, setStatsTo] = useState("");
  const [statsTableId, setStatsTableId] = useState<number | "">("");
  const [statsWaiterId, setStatsWaiterId] = useState<number | "">("");
  const [statsLimit, setStatsLimit] = useState(10);
  const [stats, setStats] = useState<StatsSummary | null>(null);
  const [daily, setDaily] = useState<StatsDailyRow[]>([]);
  const [topItems, setTopItems] = useState<TopItemRow[]>([]);
  const [topCategories, setTopCategories] = useState<TopCategoryRow[]>([]);
  const [auditLogs, setAuditLogs] = useState<AuditLog[]>([]);
  const [auditLoading, setAuditLoading] = useState(false);
  const [auditBeforeId, setAuditBeforeId] = useState<number | "">("");
  const [auditAfterId, setAuditAfterId] = useState<number | "">("");
  const [auditAction, setAuditAction] = useState("");
  const [auditEntityType, setAuditEntityType] = useState("");
  const [auditActor, setAuditActor] = useState("");

  const [newCatNameRu, setNewCatNameRu] = useState("");
  const [newCatSort, setNewCatSort] = useState(0);

  const [newItemCatId, setNewItemCatId] = useState<number | "">("");
  const [newItemNameRu, setNewItemNameRu] = useState("");
  const [newItemNameRo, setNewItemNameRo] = useState("");
  const [newItemNameEn, setNewItemNameEn] = useState("");
  const [newItemDescRu, setNewItemDescRu] = useState("");
  const [newItemDescRo, setNewItemDescRo] = useState("");
  const [newItemDescEn, setNewItemDescEn] = useState("");
  const [newItemIngredientsRu, setNewItemIngredientsRu] = useState("");
  const [newItemIngredientsRo, setNewItemIngredientsRo] = useState("");
  const [newItemIngredientsEn, setNewItemIngredientsEn] = useState("");
  const [newItemAllergens, setNewItemAllergens] = useState("");
  const [newItemWeight, setNewItemWeight] = useState("");
  const [newItemTags, setNewItemTags] = useState("");
  const [newItemPhotos, setNewItemPhotos] = useState("");
  const [newItemKcal, setNewItemKcal] = useState(0);
  const [newItemProtein, setNewItemProtein] = useState(0);
  const [newItemFat, setNewItemFat] = useState(0);
  const [newItemCarbs, setNewItemCarbs] = useState(0);
  const [newItemPrice, setNewItemPrice] = useState(0);
  const [newItemCurrency, setNewItemCurrency] = useState("MDL");
  const [newItemActive, setNewItemActive] = useState(true);
  const [newItemStopList, setNewItemStopList] = useState(false);

  const [editingCategoryId, setEditingCategoryId] = useState<number | null>(null);
  const [editCatNameRu, setEditCatNameRu] = useState("");
  const [editCatNameRo, setEditCatNameRo] = useState("");
  const [editCatNameEn, setEditCatNameEn] = useState("");
  const [editCatSort, setEditCatSort] = useState(0);
  const [editCatActive, setEditCatActive] = useState(true);

  const [editingItemId, setEditingItemId] = useState<number | null>(null);
  const [editItem, setEditItem] = useState<Partial<MenuItem>>({});

  const [newTableNumber, setNewTableNumber] = useState(1);
  const [newTablePublicId, setNewTablePublicId] = useState("");
  const [newTableWaiterId, setNewTableWaiterId] = useState<number | "">("");

  const [newStaffUser, setNewStaffUser] = useState("");
  const [newStaffPass, setNewStaffPass] = useState("");
  const [newStaffRole, setNewStaffRole] = useState("WAITER");
  const [newModGroupNameRu, setNewModGroupNameRu] = useState("");
  const [newModOptionNameRu, setNewModOptionNameRu] = useState("");
  const [newModOptionPrice, setNewModOptionPrice] = useState(0);
  const [activeModGroupId, setActiveModGroupId] = useState<number | null>(null);

  useEffect(() => {
    const u = localStorage.getItem("adminUser") ?? "";
    const p = localStorage.getItem("adminPass") ?? "";
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

  async function loadAll() {
    if (!authReady) return;
    setError(null);
    try {
      const [catsRes, itemsRes, tablesRes, staffRes, settingsRes, modGroupsRes] = await Promise.all([
        api("/api/admin/menu/categories"),
        api("/api/admin/menu/items"),
        api("/api/admin/tables"),
        api("/api/admin/staff"),
        api("/api/admin/branch-settings"),
        api("/api/admin/modifier-groups"),
      ]);
      setCategories(await catsRes.json());
      setItems(await itemsRes.json());
      setTables(await tablesRes.json());
      setStaff(await staffRes.json());
      setSettings(await settingsRes.json());
      setModGroups(await modGroupsRes.json());
    } catch (e: any) {
      setError(e?.message ?? "Load error");
    }
  }

  useEffect(() => {
    loadAll();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [authReady]);

  async function login() {
    setError(null);
    try {
      await api("/api/admin/me");
      localStorage.setItem("adminUser", username);
      localStorage.setItem("adminPass", password);
      setAuthReady(true);
    } catch (e: any) {
      setError(e?.message ?? "Auth error");
    }
  }

  async function createCategory() {
    await api("/api/admin/menu/categories", {
      method: "POST",
      body: JSON.stringify({ nameRu: newCatNameRu, sortOrder: newCatSort }),
    });
    setNewCatNameRu("");
    setNewCatSort(0);
    loadAll();
  }

  async function toggleCategory(c: Category) {
    await api(`/api/admin/menu/categories/${c.id}`, {
      method: "PATCH",
      body: JSON.stringify({ isActive: !c.isActive }),
    });
    loadAll();
  }

  async function createItem() {
    if (!newItemCatId) return;
    await api("/api/admin/menu/items", {
      method: "POST",
      body: JSON.stringify({
        categoryId: newItemCatId,
        nameRu: newItemNameRu,
        nameRo: newItemNameRo,
        nameEn: newItemNameEn,
        descriptionRu: newItemDescRu,
        descriptionRo: newItemDescRo,
        descriptionEn: newItemDescEn,
        ingredientsRu: newItemIngredientsRu,
        ingredientsRo: newItemIngredientsRo,
        ingredientsEn: newItemIngredientsEn,
        allergens: newItemAllergens,
        weight: newItemWeight,
        tags: newItemTags,
        photoUrls: newItemPhotos,
        kcal: newItemKcal,
        proteinG: newItemProtein,
        fatG: newItemFat,
        carbsG: newItemCarbs,
        priceCents: newItemPrice,
        currency: newItemCurrency,
        isActive: newItemActive,
        isStopList: newItemStopList,
      }),
    });
    setNewItemCatId("");
    setNewItemNameRu("");
    setNewItemNameRo("");
    setNewItemNameEn("");
    setNewItemDescRu("");
    setNewItemDescRo("");
    setNewItemDescEn("");
    setNewItemIngredientsRu("");
    setNewItemIngredientsRo("");
    setNewItemIngredientsEn("");
    setNewItemAllergens("");
    setNewItemWeight("");
    setNewItemTags("");
    setNewItemPhotos("");
    setNewItemKcal(0);
    setNewItemProtein(0);
    setNewItemFat(0);
    setNewItemCarbs(0);
    setNewItemPrice(0);
    setNewItemCurrency("MDL");
    setNewItemActive(true);
    setNewItemStopList(false);
    loadAll();
  }

  async function toggleItem(it: MenuItem) {
    await api(`/api/admin/menu/items/${it.id}`, {
      method: "PATCH",
      body: JSON.stringify({ isActive: !it.isActive }),
    });
    loadAll();
  }

  async function toggleStopList(it: MenuItem) {
    await api(`/api/admin/menu/items/${it.id}`, {
      method: "PATCH",
      body: JSON.stringify({ isStopList: !it.isStopList }),
    });
    loadAll();
  }

  async function editItem(it: MenuItem) {
    setEditingItemId(it.id);
    setEditItem({ ...it });
  }

  async function createTable() {
    await api("/api/admin/tables", {
      method: "POST",
      body: JSON.stringify({
        number: newTableNumber,
        publicId: newTablePublicId || undefined,
        assignedWaiterId: newTableWaiterId === "" ? null : newTableWaiterId,
      }),
    });
    setNewTablePublicId("");
    setNewTableNumber(1);
    setNewTableWaiterId("");
    loadAll();
  }

  async function assignWaiter(tableId: number, waiterId: number | null) {
    await api(`/api/admin/tables/${tableId}`, {
      method: "PATCH",
      body: JSON.stringify({ assignedWaiterId: waiterId }),
    });
    loadAll();
  }

  async function getSignedUrl(publicId: string) {
    const res = await api(`/api/admin/tables/${publicId}/signed-url`);
    const body = await res.json();
    alert(body.url);
  }

  async function showQr(tableId: number, publicId: string) {
    const res = await api(`/api/admin/tables/${publicId}/signed-url`);
    const body = await res.json();
    const url = body.url as string;
    setQrByTable((prev) => ({ ...prev, [tableId]: url }));
  }

  async function createStaff() {
    await api("/api/admin/staff", {
      method: "POST",
      body: JSON.stringify({ username: newStaffUser, password: newStaffPass, role: newStaffRole }),
    });
    setNewStaffUser("");
    setNewStaffPass("");
    setNewStaffRole("WAITER");
    loadAll();
  }

  async function createModGroup() {
    await api("/api/admin/modifier-groups", {
      method: "POST",
      body: JSON.stringify({ nameRu: newModGroupNameRu }),
    });
    setNewModGroupNameRu("");
    loadAll();
  }

  async function toggleModGroup(g: ModifierGroup) {
    await api(`/api/admin/modifier-groups/${g.id}`, {
      method: "PATCH",
      body: JSON.stringify({ isActive: !g.isActive }),
    });
    loadAll();
  }

  async function loadModOptions(groupId: number) {
    const res = await api(`/api/admin/modifier-options?groupId=${groupId}`);
    const list = await res.json();
    setModOptions((prev) => ({ ...prev, [groupId]: list }));
  }

  async function createModOption() {
    if (!activeModGroupId) return;
    await api(`/api/admin/modifier-options?groupId=${activeModGroupId}`, {
      method: "POST",
      body: JSON.stringify({ nameRu: newModOptionNameRu, priceCents: newModOptionPrice }),
    });
    setNewModOptionNameRu("");
    setNewModOptionPrice(0);
    loadModOptions(activeModGroupId);
  }

  async function toggleModOption(groupId: number, opt: ModifierOption) {
    await api(`/api/admin/modifier-options/${opt.id}`, {
      method: "PATCH",
      body: JSON.stringify({ isActive: !opt.isActive }),
    });
    loadModOptions(groupId);
  }

  async function loadItemModGroups(itemId: number) {
    const res = await api(`/api/admin/menu/items/${itemId}/modifier-groups`);
    const list = await res.json();
    setItemModGroups((prev) => ({ ...prev, [itemId]: list }));
  }

  async function saveItemModGroups(itemId: number, groups: ItemModifierGroup[]) {
    await api(`/api/admin/menu/items/${itemId}/modifier-groups`, {
      method: "PUT",
      body: JSON.stringify({ groups }),
    });
    loadItemModGroups(itemId);
  }

  async function toggleStaff(su: StaffUser) {
    await api(`/api/admin/staff/${su.id}`, {
      method: "PATCH",
      body: JSON.stringify({ isActive: !su.isActive }),
    });
    loadAll();
  }

  async function resetStaffPassword(su: StaffUser) {
    const pass = prompt("New password");
    if (!pass) return;
    await api(`/api/admin/staff/${su.id}`, {
      method: "PATCH",
      body: JSON.stringify({ password: pass }),
    });
    loadAll();
  }

  async function saveSettings() {
    if (!settings) return;
    await api("/api/admin/branch-settings", {
      method: "PUT",
      body: JSON.stringify({
        requireOtpForFirstOrder: settings.requireOtpForFirstOrder,
        otpTtlSeconds: settings.otpTtlSeconds,
        otpMaxAttempts: settings.otpMaxAttempts,
        otpResendCooldownSeconds: settings.otpResendCooldownSeconds,
        otpLength: settings.otpLength,
        otpDevEchoCode: settings.otpDevEchoCode,
        enablePartyPin: settings.enablePartyPin,
        allowPayOtherGuestsItems: settings.allowPayOtherGuestsItems,
        allowPayWholeTable: settings.allowPayWholeTable,
        tipsEnabled: settings.tipsEnabled,
        tipsPercentages: settings.tipsPercentages,
        payCashEnabled: settings.payCashEnabled,
        payTerminalEnabled: settings.payTerminalEnabled,
      }),
    });
    loadAll();
  }

  async function loadStats() {
    const qs = new URLSearchParams();
    if (statsFrom) qs.set("from", statsFrom);
    if (statsTo) qs.set("to", statsTo);
    if (statsTableId !== "") qs.set("tableId", String(statsTableId));
    if (statsWaiterId !== "") qs.set("waiterId", String(statsWaiterId));
    const res = await api(`/api/admin/stats/summary?${qs.toString()}`);
    const body = await res.json();
    setStats(body);
    const resDaily = await api(`/api/admin/stats/daily?${qs.toString()}`);
    const dailyBody = await resDaily.json();
    setDaily(dailyBody);
    const qsTop = new URLSearchParams(qs);
    qsTop.set("limit", String(statsLimit || 10));
    const resTopItems = await api(`/api/admin/stats/top-items?${qsTop.toString()}`);
    setTopItems(await resTopItems.json());
    const resTopCategories = await api(`/api/admin/stats/top-categories?${qsTop.toString()}`);
    setTopCategories(await resTopCategories.json());
  }

  async function downloadCsv() {
    const qs = new URLSearchParams();
    if (statsFrom) qs.set("from", statsFrom);
    if (statsTo) qs.set("to", statsTo);
    if (statsTableId !== "") qs.set("tableId", String(statsTableId));
    if (statsWaiterId !== "") qs.set("waiterId", String(statsWaiterId));
    const res = await api(`/api/admin/stats/daily.csv?${qs.toString()}`, {
      headers: { Authorization: authHeader },
    });
    const blob = await res.blob();
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = "stats-daily.csv";
    document.body.appendChild(a);
    a.click();
    a.remove();
    URL.revokeObjectURL(url);
  }

  async function saveEditedCategory() {
    if (!editingCategoryId) return;
    await api(`/api/admin/menu/categories/${editingCategoryId}`, {
      method: "PATCH",
      body: JSON.stringify({
        nameRu: editCatNameRu,
        nameRo: editCatNameRo,
        nameEn: editCatNameEn,
        sortOrder: editCatSort,
        isActive: editCatActive,
      }),
    });
    setEditingCategoryId(null);
    loadAll();
  }

  async function saveEditedItem() {
    if (!editingItemId) return;
    await api(`/api/admin/menu/items/${editingItemId}`, {
      method: "PATCH",
      body: JSON.stringify({
        categoryId: editItem.categoryId,
        nameRu: editItem.nameRu,
        nameRo: editItem.nameRo,
        nameEn: editItem.nameEn,
        descriptionRu: editItem.descriptionRu,
        descriptionRo: editItem.descriptionRo,
        descriptionEn: editItem.descriptionEn,
        ingredientsRu: editItem.ingredientsRu,
        ingredientsRo: editItem.ingredientsRo,
        ingredientsEn: editItem.ingredientsEn,
        allergens: editItem.allergens,
        weight: editItem.weight,
        tags: editItem.tags,
        photoUrls: editItem.photoUrls,
        kcal: editItem.kcal,
        proteinG: editItem.proteinG,
        fatG: editItem.fatG,
        carbsG: editItem.carbsG,
        priceCents: editItem.priceCents,
        currency: editItem.currency,
        isActive: editItem.isActive,
        isStopList: editItem.isStopList,
      }),
    });
    setEditingItemId(null);
    setEditItem({});
    loadAll();
  }

  async function loadAuditLogs() {
    setAuditLoading(true);
    try {
      const qs = new URLSearchParams();
      if (auditAction.trim()) qs.set("action", auditAction.trim());
      if (auditEntityType.trim()) qs.set("entityType", auditEntityType.trim());
      if (auditActor.trim()) qs.set("actorUsername", auditActor.trim());
      if (auditBeforeId !== "") qs.set("beforeId", String(auditBeforeId));
      if (auditAfterId !== "") qs.set("afterId", String(auditAfterId));
      const res = await api(`/api/admin/audit-logs?${qs.toString()}`);
      const body = await res.json();
      setAuditLogs(body);
    } catch (e: any) {
      setError(e?.message ?? "Audit load error");
    } finally {
      setAuditLoading(false);
    }
  }

  async function downloadAuditCsv() {
    const qs = new URLSearchParams();
    if (auditAction.trim()) qs.set("action", auditAction.trim());
    if (auditEntityType.trim()) qs.set("entityType", auditEntityType.trim());
    if (auditActor.trim()) qs.set("actorUsername", auditActor.trim());
    if (auditBeforeId !== "") qs.set("beforeId", String(auditBeforeId));
    if (auditAfterId !== "") qs.set("afterId", String(auditAfterId));
    const res = await api(`/api/admin/audit-logs.csv?${qs.toString()}`, {
      headers: { Authorization: authHeader },
    });
    const blob = await res.blob();
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = "audit-logs.csv";
    document.body.appendChild(a);
    a.click();
    a.remove();
    URL.revokeObjectURL(url);
  }

  function clearAuditFilters() {
    setAuditAction("");
    setAuditEntityType("");
    setAuditActor("");
    setAuditBeforeId("");
    setAuditAfterId("");
  }

  if (!authReady) {
    return (
      <main style={{ padding: 24, maxWidth: 520, margin: "0 auto", fontFamily: "system-ui, -apple-system, Segoe UI, Roboto" }}>
        <h1>Admin Login</h1>
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
    <main style={{ padding: 16, maxWidth: 1100, margin: "0 auto", fontFamily: "system-ui, -apple-system, Segoe UI, Roboto" }}>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
        <h1 style={{ margin: 0 }}>Admin</h1>
        <button onClick={loadAll}>Refresh</button>
      </div>
      {error && <div style={{ color: "#b11e46", marginTop: 8 }}>{error}</div>}

      <section style={{ marginTop: 24 }}>
        <h2>Settings</h2>
        {settings && (
          <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(260px, 1fr))", gap: 12 }}>
            <label><input type="checkbox" checked={settings.requireOtpForFirstOrder} onChange={(e) => setSettings({ ...settings, requireOtpForFirstOrder: e.target.checked })} /> requireOtpForFirstOrder</label>
            <label><input type="checkbox" checked={settings.enablePartyPin} onChange={(e) => setSettings({ ...settings, enablePartyPin: e.target.checked })} /> enablePartyPin</label>
            <label><input type="checkbox" checked={settings.allowPayOtherGuestsItems} onChange={(e) => setSettings({ ...settings, allowPayOtherGuestsItems: e.target.checked })} /> allowPayOtherGuestsItems</label>
            <label><input type="checkbox" checked={settings.allowPayWholeTable} onChange={(e) => setSettings({ ...settings, allowPayWholeTable: e.target.checked })} /> allowPayWholeTable</label>
            <label><input type="checkbox" checked={settings.tipsEnabled} onChange={(e) => setSettings({ ...settings, tipsEnabled: e.target.checked })} /> tipsEnabled</label>
            <label><input type="checkbox" checked={settings.payCashEnabled} onChange={(e) => setSettings({ ...settings, payCashEnabled: e.target.checked })} /> payCashEnabled</label>
            <label><input type="checkbox" checked={settings.payTerminalEnabled} onChange={(e) => setSettings({ ...settings, payTerminalEnabled: e.target.checked })} /> payTerminalEnabled</label>
            <label>otpTtlSeconds <input value={settings.otpTtlSeconds} onChange={(e) => setSettings({ ...settings, otpTtlSeconds: Number(e.target.value) })} /></label>
            <label>otpMaxAttempts <input value={settings.otpMaxAttempts} onChange={(e) => setSettings({ ...settings, otpMaxAttempts: Number(e.target.value) })} /></label>
            <label>otpResendCooldownSeconds <input value={settings.otpResendCooldownSeconds} onChange={(e) => setSettings({ ...settings, otpResendCooldownSeconds: Number(e.target.value) })} /></label>
            <label>otpLength <input value={settings.otpLength} onChange={(e) => setSettings({ ...settings, otpLength: Number(e.target.value) })} /></label>
            <label><input type="checkbox" checked={settings.otpDevEchoCode} onChange={(e) => setSettings({ ...settings, otpDevEchoCode: e.target.checked })} /> otpDevEchoCode</label>
            <label>tipsPercentages (comma) <input value={settings.tipsPercentages.join(",")} onChange={(e) => setSettings({ ...settings, tipsPercentages: e.target.value.split(",").map((x) => parseInt(x.trim(), 10)).filter((v) => !Number.isNaN(v)) })} /></label>
          </div>
        )}
        <button onClick={saveSettings} style={{ marginTop: 12, padding: "8px 12px" }}>Save settings</button>
      </section>

      <section style={{ marginTop: 24 }}>
        <h2>Stats</h2>
        <div style={{ display: "flex", gap: 8, flexWrap: "wrap", alignItems: "center" }}>
          <label>From <input type="date" value={statsFrom} onChange={(e) => setStatsFrom(e.target.value)} /></label>
          <label>To <input type="date" value={statsTo} onChange={(e) => setStatsTo(e.target.value)} /></label>
          <label>
            Table
            <select value={statsTableId} onChange={(e) => setStatsTableId(e.target.value ? Number(e.target.value) : "")}>
              <option value="">All</option>
              {tables.map((t) => (
                <option key={t.id} value={t.id}>#{t.number}</option>
              ))}
            </select>
          </label>
          <label>
            Waiter
            <select value={statsWaiterId} onChange={(e) => setStatsWaiterId(e.target.value ? Number(e.target.value) : "")}>
              <option value="">All</option>
              {staff.filter((s) => s.role === "WAITER").map((w) => (
                <option key={w.id} value={w.id}>{w.username} #{w.id}</option>
              ))}
            </select>
          </label>
          <label>Top limit <input type="number" min={1} max={100} value={statsLimit} onChange={(e) => setStatsLimit(Number(e.target.value))} style={{ width: 80 }} /></label>
          <button onClick={loadStats}>Load</button>
          <button onClick={downloadCsv}>Download CSV</button>
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
        {daily.length > 0 && (
          <div style={{ marginTop: 12 }}>
            <table style={{ width: "100%", borderCollapse: "collapse" }}>
              <thead>
                <tr>
                  <th style={{ textAlign: "left", borderBottom: "1px solid #ddd", padding: "6px 4px" }}>Day</th>
                  <th style={{ textAlign: "right", borderBottom: "1px solid #ddd", padding: "6px 4px" }}>Orders</th>
                  <th style={{ textAlign: "right", borderBottom: "1px solid #ddd", padding: "6px 4px" }}>Calls</th>
                  <th style={{ textAlign: "right", borderBottom: "1px solid #ddd", padding: "6px 4px" }}>Paid bills</th>
                  <th style={{ textAlign: "right", borderBottom: "1px solid #ddd", padding: "6px 4px" }}>Gross</th>
                  <th style={{ textAlign: "right", borderBottom: "1px solid #ddd", padding: "6px 4px" }}>Tips</th>
                </tr>
              </thead>
              <tbody>
                {daily.map((r) => (
                  <tr key={r.day}>
                    <td style={{ padding: "6px 4px", borderBottom: "1px solid #f0f0f0" }}>{r.day}</td>
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

        {(topItems.length > 0 || topCategories.length > 0) && (
          <div style={{ marginTop: 16, display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(280px, 1fr))", gap: 12 }}>
            <div style={{ border: "1px solid #eee", borderRadius: 8, padding: 10 }}>
              <strong>Top items</strong>
              {topItems.length === 0 ? (
                <div style={{ color: "#666", marginTop: 6 }}>No data</div>
              ) : (
                <table style={{ width: "100%", borderCollapse: "collapse", marginTop: 6 }}>
                  <thead>
                    <tr>
                      <th style={{ textAlign: "left", borderBottom: "1px solid #ddd", padding: "6px 4px" }}>Item</th>
                      <th style={{ textAlign: "right", borderBottom: "1px solid #ddd", padding: "6px 4px" }}>Qty</th>
                      <th style={{ textAlign: "right", borderBottom: "1px solid #ddd", padding: "6px 4px" }}>Gross</th>
                    </tr>
                  </thead>
                  <tbody>
                    {topItems.map((r) => (
                      <tr key={r.menuItemId}>
                        <td style={{ padding: "6px 4px", borderBottom: "1px solid #f0f0f0" }}>{r.name}</td>
                        <td style={{ padding: "6px 4px", textAlign: "right", borderBottom: "1px solid #f0f0f0" }}>{r.qty}</td>
                        <td style={{ padding: "6px 4px", textAlign: "right", borderBottom: "1px solid #f0f0f0" }}>{money(r.grossCents)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </div>
            <div style={{ border: "1px solid #eee", borderRadius: 8, padding: 10 }}>
              <strong>Top categories</strong>
              {topCategories.length === 0 ? (
                <div style={{ color: "#666", marginTop: 6 }}>No data</div>
              ) : (
                <table style={{ width: "100%", borderCollapse: "collapse", marginTop: 6 }}>
                  <thead>
                    <tr>
                      <th style={{ textAlign: "left", borderBottom: "1px solid #ddd", padding: "6px 4px" }}>Category</th>
                      <th style={{ textAlign: "right", borderBottom: "1px solid #ddd", padding: "6px 4px" }}>Qty</th>
                      <th style={{ textAlign: "right", borderBottom: "1px solid #ddd", padding: "6px 4px" }}>Gross</th>
                    </tr>
                  </thead>
                  <tbody>
                    {topCategories.map((r) => (
                      <tr key={r.categoryId}>
                        <td style={{ padding: "6px 4px", borderBottom: "1px solid #f0f0f0" }}>{r.name}</td>
                        <td style={{ padding: "6px 4px", textAlign: "right", borderBottom: "1px solid #f0f0f0" }}>{r.qty}</td>
                        <td style={{ padding: "6px 4px", textAlign: "right", borderBottom: "1px solid #f0f0f0" }}>{money(r.grossCents)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </div>
          </div>
        )}
      </section>

      <section style={{ marginTop: 24 }}>
        <h2>Audit Logs</h2>
        <div style={{ display: "flex", gap: 8, flexWrap: "wrap", alignItems: "center" }}>
          <label>
            Action
            <input list="audit-actions" value={auditAction} onChange={(e) => setAuditAction(e.target.value)} placeholder="CREATE/UPDATE/DELETE" />
          </label>
          <label>
            Entity
            <input list="audit-entities" value={auditEntityType} onChange={(e) => setAuditEntityType(e.target.value)} placeholder="MenuItem/StaffUser" />
          </label>
          <label>Actor <input value={auditActor} onChange={(e) => setAuditActor(e.target.value)} placeholder="username" /></label>
          <label>Before ID <input type="number" value={auditBeforeId} onChange={(e) => setAuditBeforeId(e.target.value ? Number(e.target.value) : "")} /></label>
          <label>After ID <input type="number" value={auditAfterId} onChange={(e) => setAuditAfterId(e.target.value ? Number(e.target.value) : "")} /></label>
          <button onClick={loadAuditLogs} disabled={auditLoading}>{auditLoading ? "Loading..." : "Load"}</button>
          <button onClick={downloadAuditCsv} disabled={auditLoading}>CSV</button>
          <button onClick={() => { setAuditAfterId(""); setAuditBeforeId(""); loadAuditLogs(); }} disabled={auditLoading}>Latest</button>
          <button onClick={clearAuditFilters} disabled={auditLoading}>Clear</button>
        </div>
        <datalist id="audit-actions">
          <option value="CREATE" />
          <option value="UPDATE" />
          <option value="DELETE" />
        </datalist>
        <datalist id="audit-entities">
          <option value="MenuCategory" />
          <option value="MenuItem" />
          <option value="CafeTable" />
          <option value="StaffUser" />
          <option value="ModifierGroup" />
          <option value="ModifierOption" />
        </datalist>
        {auditLogs.length > 0 && (
          <div style={{ marginTop: 10 }}>
            <table style={{ width: "100%", borderCollapse: "collapse" }}>
              <thead>
                <tr>
                  <th style={{ textAlign: "left", borderBottom: "1px solid #ddd", padding: "6px 4px" }}>ID</th>
                  <th style={{ textAlign: "left", borderBottom: "1px solid #ddd", padding: "6px 4px" }}>When</th>
                  <th style={{ textAlign: "left", borderBottom: "1px solid #ddd", padding: "6px 4px" }}>Actor</th>
                  <th style={{ textAlign: "left", borderBottom: "1px solid #ddd", padding: "6px 4px" }}>Action</th>
                  <th style={{ textAlign: "left", borderBottom: "1px solid #ddd", padding: "6px 4px" }}>Entity</th>
                </tr>
              </thead>
              <tbody>
                {auditLogs.map((a) => (
                  <tr key={a.id}>
                    <td style={{ padding: "6px 4px", borderBottom: "1px solid #f0f0f0" }}>{a.id}</td>
                    <td style={{ padding: "6px 4px", borderBottom: "1px solid #f0f0f0" }}>{a.createdAt}</td>
                    <td style={{ padding: "6px 4px", borderBottom: "1px solid #f0f0f0" }}>
                      {a.actorUsername ?? "-"} {a.actorRole ? `(${a.actorRole})` : ""}
                    </td>
                    <td style={{ padding: "6px 4px", borderBottom: "1px solid #f0f0f0" }}>{a.action}</td>
                    <td style={{ padding: "6px 4px", borderBottom: "1px solid #f0f0f0" }}>
                      {a.entityType} {a.entityId ? `#${a.entityId}` : ""}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      <section style={{ marginTop: 24 }}>
        <h2>Menu Categories</h2>
        <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
          <input placeholder="Name (RU)" value={newCatNameRu} onChange={(e) => setNewCatNameRu(e.target.value)} />
          <input type="number" placeholder="Sort" value={newCatSort} onChange={(e) => setNewCatSort(Number(e.target.value))} />
          <button onClick={createCategory}>Add</button>
        </div>
        {editingCategoryId && (
          <div style={{ marginTop: 10, border: "1px dashed #ddd", padding: 10 }}>
            <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
              <input placeholder="Name RU" value={editCatNameRu} onChange={(e) => setEditCatNameRu(e.target.value)} />
              <input placeholder="Name RO" value={editCatNameRo} onChange={(e) => setEditCatNameRo(e.target.value)} />
              <input placeholder="Name EN" value={editCatNameEn} onChange={(e) => setEditCatNameEn(e.target.value)} />
              <input type="number" placeholder="Sort" value={editCatSort} onChange={(e) => setEditCatSort(Number(e.target.value))} />
              <label><input type="checkbox" checked={editCatActive} onChange={(e) => setEditCatActive(e.target.checked)} /> Active</label>
            </div>
            <div style={{ marginTop: 8 }}>
              <button onClick={saveEditedCategory}>Save</button>
              <button onClick={() => setEditingCategoryId(null)} style={{ marginLeft: 8 }}>Cancel</button>
            </div>
          </div>
        )}
        <div style={{ marginTop: 10 }}>
          {categories.map((c) => (
            <div key={c.id} style={{ display: "flex", gap: 10, alignItems: "center", padding: "6px 0", borderBottom: "1px solid #eee" }}>
              <strong>{c.nameRu}</strong>
              <span>#{c.sortOrder}</span>
              <span>{c.isActive ? "ACTIVE" : "INACTIVE"}</span>
              <button onClick={() => {
                setEditingCategoryId(c.id);
                setEditCatNameRu(c.nameRu);
                setEditCatNameRo(c.nameRo ?? "");
                setEditCatNameEn(c.nameEn ?? "");
                setEditCatSort(c.sortOrder);
                setEditCatActive(c.isActive);
              }}>Edit</button>
              <button onClick={() => toggleCategory(c)}>{c.isActive ? "Disable" : "Enable"}</button>
            </div>
          ))}
        </div>
      </section>

      <section style={{ marginTop: 24 }}>
        <h2>Menu Items</h2>
        <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
          <select value={newItemCatId} onChange={(e) => setNewItemCatId(e.target.value ? Number(e.target.value) : "")}>
            <option value="">Category</option>
            {categories.map((c) => (
              <option key={c.id} value={c.id}>{c.nameRu}</option>
            ))}
          </select>
          <input placeholder="Name (RU)" value={newItemNameRu} onChange={(e) => setNewItemNameRu(e.target.value)} />
          <input placeholder="Name (RO)" value={newItemNameRo} onChange={(e) => setNewItemNameRo(e.target.value)} />
          <input placeholder="Name (EN)" value={newItemNameEn} onChange={(e) => setNewItemNameEn(e.target.value)} />
          <input placeholder="Desc (RU)" value={newItemDescRu} onChange={(e) => setNewItemDescRu(e.target.value)} />
          <input placeholder="Desc (RO)" value={newItemDescRo} onChange={(e) => setNewItemDescRo(e.target.value)} />
          <input placeholder="Desc (EN)" value={newItemDescEn} onChange={(e) => setNewItemDescEn(e.target.value)} />
          <input placeholder="Ingredients (RU)" value={newItemIngredientsRu} onChange={(e) => setNewItemIngredientsRu(e.target.value)} />
          <input placeholder="Ingredients (RO)" value={newItemIngredientsRo} onChange={(e) => setNewItemIngredientsRo(e.target.value)} />
          <input placeholder="Ingredients (EN)" value={newItemIngredientsEn} onChange={(e) => setNewItemIngredientsEn(e.target.value)} />
          <input placeholder="Allergens" value={newItemAllergens} onChange={(e) => setNewItemAllergens(e.target.value)} />
          <input placeholder="Weight" value={newItemWeight} onChange={(e) => setNewItemWeight(e.target.value)} />
          <input placeholder="Tags (csv)" value={newItemTags} onChange={(e) => setNewItemTags(e.target.value)} />
          <input placeholder="Photo URLs (csv)" value={newItemPhotos} onChange={(e) => setNewItemPhotos(e.target.value)} />
          <input type="number" placeholder="Kcal" value={newItemKcal} onChange={(e) => setNewItemKcal(Number(e.target.value))} />
          <input type="number" placeholder="Protein g" value={newItemProtein} onChange={(e) => setNewItemProtein(Number(e.target.value))} />
          <input type="number" placeholder="Fat g" value={newItemFat} onChange={(e) => setNewItemFat(Number(e.target.value))} />
          <input type="number" placeholder="Carbs g" value={newItemCarbs} onChange={(e) => setNewItemCarbs(Number(e.target.value))} />
          <input type="number" placeholder="Price cents" value={newItemPrice} onChange={(e) => setNewItemPrice(Number(e.target.value))} />
          <input placeholder="Currency" value={newItemCurrency} onChange={(e) => setNewItemCurrency(e.target.value)} />
          <label><input type="checkbox" checked={newItemActive} onChange={(e) => setNewItemActive(e.target.checked)} /> Active</label>
          <label><input type="checkbox" checked={newItemStopList} onChange={(e) => setNewItemStopList(e.target.checked)} /> Stop list</label>
          <button onClick={createItem}>Add</button>
        </div>
        {editingItemId && (
          <div style={{ marginTop: 10, border: "1px dashed #ddd", padding: 10 }}>
            <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(200px, 1fr))", gap: 8 }}>
              <select value={editItem.categoryId ?? ""} onChange={(e) => setEditItem({ ...editItem, categoryId: e.target.value ? Number(e.target.value) : undefined })}>
                <option value="">Category</option>
                {categories.map((c) => (
                  <option key={c.id} value={c.id}>{c.nameRu}</option>
                ))}
              </select>
              <input placeholder="Name RU" value={editItem.nameRu ?? ""} onChange={(e) => setEditItem({ ...editItem, nameRu: e.target.value })} />
              <input placeholder="Name RO" value={editItem.nameRo ?? ""} onChange={(e) => setEditItem({ ...editItem, nameRo: e.target.value })} />
              <input placeholder="Name EN" value={editItem.nameEn ?? ""} onChange={(e) => setEditItem({ ...editItem, nameEn: e.target.value })} />
              <input placeholder="Desc RU" value={editItem.descriptionRu ?? ""} onChange={(e) => setEditItem({ ...editItem, descriptionRu: e.target.value })} />
              <input placeholder="Desc RO" value={editItem.descriptionRo ?? ""} onChange={(e) => setEditItem({ ...editItem, descriptionRo: e.target.value })} />
              <input placeholder="Desc EN" value={editItem.descriptionEn ?? ""} onChange={(e) => setEditItem({ ...editItem, descriptionEn: e.target.value })} />
              <input placeholder="Ingredients RU" value={editItem.ingredientsRu ?? ""} onChange={(e) => setEditItem({ ...editItem, ingredientsRu: e.target.value })} />
              <input placeholder="Ingredients RO" value={editItem.ingredientsRo ?? ""} onChange={(e) => setEditItem({ ...editItem, ingredientsRo: e.target.value })} />
              <input placeholder="Ingredients EN" value={editItem.ingredientsEn ?? ""} onChange={(e) => setEditItem({ ...editItem, ingredientsEn: e.target.value })} />
              <input placeholder="Allergens" value={editItem.allergens ?? ""} onChange={(e) => setEditItem({ ...editItem, allergens: e.target.value })} />
              <input placeholder="Weight" value={editItem.weight ?? ""} onChange={(e) => setEditItem({ ...editItem, weight: e.target.value })} />
              <input placeholder="Tags csv" value={editItem.tags ?? ""} onChange={(e) => setEditItem({ ...editItem, tags: e.target.value })} />
              <input placeholder="Photo URLs csv" value={editItem.photoUrls ?? ""} onChange={(e) => setEditItem({ ...editItem, photoUrls: e.target.value })} />
              <input type="number" placeholder="Kcal" value={editItem.kcal ?? 0} onChange={(e) => setEditItem({ ...editItem, kcal: Number(e.target.value) })} />
              <input type="number" placeholder="Protein g" value={editItem.proteinG ?? 0} onChange={(e) => setEditItem({ ...editItem, proteinG: Number(e.target.value) })} />
              <input type="number" placeholder="Fat g" value={editItem.fatG ?? 0} onChange={(e) => setEditItem({ ...editItem, fatG: Number(e.target.value) })} />
              <input type="number" placeholder="Carbs g" value={editItem.carbsG ?? 0} onChange={(e) => setEditItem({ ...editItem, carbsG: Number(e.target.value) })} />
              <input type="number" placeholder="Price cents" value={editItem.priceCents ?? 0} onChange={(e) => setEditItem({ ...editItem, priceCents: Number(e.target.value) })} />
              <input placeholder="Currency" value={editItem.currency ?? "MDL"} onChange={(e) => setEditItem({ ...editItem, currency: e.target.value })} />
              <label><input type="checkbox" checked={!!editItem.isActive} onChange={(e) => setEditItem({ ...editItem, isActive: e.target.checked })} /> Active</label>
              <label><input type="checkbox" checked={!!editItem.isStopList} onChange={(e) => setEditItem({ ...editItem, isStopList: e.target.checked })} /> Stop list</label>
            </div>
            <div style={{ marginTop: 8 }}>
              <button onClick={saveEditedItem}>Save</button>
              <button onClick={() => { setEditingItemId(null); setEditItem({}); }} style={{ marginLeft: 8 }}>Cancel</button>
            </div>
          </div>
        )}
        <div style={{ marginTop: 10 }}>
          {items.map((it) => (
            <div key={it.id} style={{ display: "flex", gap: 10, alignItems: "center", padding: "6px 0", borderBottom: "1px solid #eee" }}>
              <strong>{it.nameRu}</strong>
              <span>{money(it.priceCents, it.currency)}</span>
              <span>{it.isActive ? "ACTIVE" : "INACTIVE"}</span>
              <span>{it.isStopList ? "STOP" : ""}</span>
              <button onClick={() => editItem(it)}>Edit</button>
              <button onClick={() => toggleItem(it)}>{it.isActive ? "Disable" : "Enable"}</button>
              <button onClick={() => toggleStopList(it)}>{it.isStopList ? "Unstop" : "Stop list"}</button>
            </div>
          ))}
        </div>
      </section>

      <section style={{ marginTop: 24 }}>
        <h2>Tables & QR</h2>
        <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
          <input type="number" placeholder="Table #" value={newTableNumber} onChange={(e) => setNewTableNumber(Number(e.target.value))} />
          <input placeholder="Public ID (optional)" value={newTablePublicId} onChange={(e) => setNewTablePublicId(e.target.value)} />
          <select value={newTableWaiterId} onChange={(e) => setNewTableWaiterId(e.target.value ? Number(e.target.value) : "")}>
            <option value="">Assign waiter</option>
            {staff.filter((s) => s.role === "WAITER").map((s) => (
              <option key={s.id} value={s.id}>{s.username}</option>
            ))}
          </select>
          <button onClick={createTable}>Add</button>
        </div>
        <div style={{ marginTop: 10 }}>
          {tables.map((t) => (
            <div key={t.id} style={{ display: "flex", gap: 10, alignItems: "center", padding: "6px 0", borderBottom: "1px solid #eee" }}>
              <strong>Table #{t.number}</strong>
              <span>{t.publicId}</span>
              <select
                value={t.assignedWaiterId ?? ""}
                onChange={(e) => assignWaiter(t.id, e.target.value ? Number(e.target.value) : null)}
              >
                <option value="">No waiter</option>
                {staff.filter((s) => s.role === "WAITER").map((s) => (
                  <option key={s.id} value={s.id}>{s.username}</option>
                ))}
              </select>
              <button onClick={() => getSignedUrl(t.publicId)}>QR URL</button>
              <button onClick={() => showQr(t.id, t.publicId)}>Show QR</button>
              {qrByTable[t.id] && (
                <a
                  href={`https://api.qrserver.com/v1/create-qr-code/?size=512x512&data=${encodeURIComponent(qrByTable[t.id])}`}
                  download={`table_${t.number}.png`}
                  style={{ marginLeft: 8 }}
                >
                  Download QR
                </a>
              )}
              {qrByTable[t.id] && (
                <img
                  src={`https://api.qrserver.com/v1/create-qr-code/?size=160x160&data=${encodeURIComponent(qrByTable[t.id])}`}
                  alt="QR"
                  style={{ marginLeft: 8, border: "1px solid #eee" }}
                />
              )}
            </div>
          ))}
        </div>
      </section>

      <section style={{ marginTop: 24 }}>
        <h2>Staff</h2>
        <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
          <input placeholder="Username" value={newStaffUser} onChange={(e) => setNewStaffUser(e.target.value)} />
          <input placeholder="Password" value={newStaffPass} onChange={(e) => setNewStaffPass(e.target.value)} />
          <select value={newStaffRole} onChange={(e) => setNewStaffRole(e.target.value)}>
            <option value="WAITER">WAITER</option>
            <option value="KITCHEN">KITCHEN</option>
            <option value="ADMIN">ADMIN</option>
          </select>
          <button onClick={createStaff}>Add</button>
        </div>
        <div style={{ marginTop: 10 }}>
          {staff.map((su) => (
            <div key={su.id} style={{ display: "flex", gap: 10, alignItems: "center", padding: "6px 0", borderBottom: "1px solid #eee" }}>
              <strong>{su.username}</strong>
              <span>{su.role}</span>
              <span>{su.isActive ? "ACTIVE" : "INACTIVE"}</span>
              <button onClick={() => resetStaffPassword(su)}>Reset password</button>
              <button onClick={() => toggleStaff(su)}>{su.isActive ? "Disable" : "Enable"}</button>
            </div>
          ))}
        </div>
      </section>

      <section style={{ marginTop: 24 }}>
        <h2>Modifiers</h2>
        <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
          <input placeholder="Group name (RU)" value={newModGroupNameRu} onChange={(e) => setNewModGroupNameRu(e.target.value)} />
          <button onClick={createModGroup}>Add group</button>
        </div>
        <div style={{ marginTop: 10, display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(300px, 1fr))", gap: 12 }}>
          {modGroups.map((g) => (
            <div key={g.id} style={{ border: "1px solid #eee", borderRadius: 8, padding: 10 }}>
              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                <strong>{g.nameRu}</strong>
                <button onClick={() => toggleModGroup(g)}>{g.isActive ? "Disable" : "Enable"}</button>
              </div>
              <button onClick={() => { setActiveModGroupId(g.id); loadModOptions(g.id); }} style={{ marginTop: 8 }}>Load options</button>
              {activeModGroupId === g.id && (
                <div style={{ marginTop: 8 }}>
                  <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
                    <input placeholder="Option (RU)" value={newModOptionNameRu} onChange={(e) => setNewModOptionNameRu(e.target.value)} />
                    <input type="number" placeholder="Price cents" value={newModOptionPrice} onChange={(e) => setNewModOptionPrice(Number(e.target.value))} />
                    <button onClick={createModOption}>Add option</button>
                  </div>
                  {(modOptions[g.id] ?? []).map((o) => (
                    <div key={o.id} style={{ display: "flex", gap: 8, alignItems: "center", padding: "4px 0" }}>
                      <span>{o.nameRu}</span>
                      <span>{o.priceCents}</span>
                      <button onClick={() => toggleModOption(g.id, o)}>{o.isActive ? "Disable" : "Enable"}</button>
                    </div>
                  ))}
                </div>
              )}
            </div>
          ))}
        </div>
      </section>

      <section style={{ marginTop: 24 }}>
        <h2>Item → Modifier Groups</h2>
        <div style={{ marginTop: 10 }}>
          {items.map((it) => (
            <div key={it.id} style={{ border: "1px solid #eee", borderRadius: 8, padding: 10, marginBottom: 10 }}>
              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                <strong>{it.nameRu}</strong>
                <button onClick={() => loadItemModGroups(it.id)}>Load</button>
              </div>
              {(itemModGroups[it.id] ?? []).map((g, idx) => (
                <div key={`${it.id}-${g.groupId}`} style={{ display: "flex", gap: 8, alignItems: "center", marginTop: 6 }}>
                  <span>Group #{g.groupId}</span>
                  <label><input type="checkbox" checked={g.isRequired} onChange={(e) => {
                    const next = (itemModGroups[it.id] ?? []).slice();
                    next[idx] = { ...g, isRequired: e.target.checked };
                    setItemModGroups((prev) => ({ ...prev, [it.id]: next }));
                  }} /> required</label>
                  <input type="number" placeholder="min" value={g.minSelect ?? ""} onChange={(e) => {
                    const next = (itemModGroups[it.id] ?? []).slice();
                    next[idx] = { ...g, minSelect: e.target.value ? Number(e.target.value) : null };
                    setItemModGroups((prev) => ({ ...prev, [it.id]: next }));
                  }} />
                  <input type="number" placeholder="max" value={g.maxSelect ?? ""} onChange={(e) => {
                    const next = (itemModGroups[it.id] ?? []).slice();
                    next[idx] = { ...g, maxSelect: e.target.value ? Number(e.target.value) : null };
                    setItemModGroups((prev) => ({ ...prev, [it.id]: next }));
                  }} />
                  <input type="number" placeholder="sort" value={g.sortOrder} onChange={(e) => {
                    const next = (itemModGroups[it.id] ?? []).slice();
                    next[idx] = { ...g, sortOrder: Number(e.target.value) };
                    setItemModGroups((prev) => ({ ...prev, [it.id]: next }));
                  }} />
                </div>
              ))}
              <div style={{ marginTop: 8, display: "flex", gap: 8, flexWrap: "wrap" }}>
                <select onChange={(e) => {
                  const groupId = Number(e.target.value);
                  if (!groupId) return;
                  const current = itemModGroups[it.id] ?? [];
                  const next = [...current, { groupId, isRequired: false, minSelect: null, maxSelect: null, sortOrder: 0 }];
                  setItemModGroups((prev) => ({ ...prev, [it.id]: next }));
                }}>
                  <option value="">Add group</option>
                  {modGroups.map((g) => (
                    <option key={g.id} value={g.id}>{g.nameRu}</option>
                  ))}
                </select>
                <button onClick={() => saveItemModGroups(it.id, itemModGroups[it.id] ?? [])}>Save</button>
              </div>
            </div>
          ))}
        </div>
      </section>
    </main>
  );
}
