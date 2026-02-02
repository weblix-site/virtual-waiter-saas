"use client";

import { useEffect, useMemo, useRef, useState, type DragEvent } from "react";

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
  hallId?: number | null;
  layoutX?: number | null;
  layoutY?: number | null;
  layoutW?: number | null;
  layoutH?: number | null;
  layoutShape?: string | null;
  layoutRotation?: number | null;
  layoutZone?: string | null;
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

type PartyDto = {
  id: number;
  tableId: number;
  tableNumber: number;
  pin: string;
  status: string;
  createdAt: string;
  expiresAt: string;
  closedAt?: string | null;
  guestSessionIds: number[];
};

type HallPlanTemplateDto = {
  id: number;
  hallId: number;
  name: string;
  payloadJson: string;
  createdAt: string;
  updatedAt: string;
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
  const [halls, setHalls] = useState<HallDto[]>([]);
  const [hallPlans, setHallPlans] = useState<HallPlanDto[]>([]);
  const [hallPlanId, setHallPlanId] = useState<number | "">("");
  const [newPlanName, setNewPlanName] = useState("");
  const [newPlanSort, setNewPlanSort] = useState(0);
  const [hallId, setHallId] = useState<number | "">("");
  const [newHallName, setNewHallName] = useState("");
  const [newHallSort, setNewHallSort] = useState(0);
  const [selectedTableId, setSelectedTableId] = useState<number | null>(null);
  const [planEditMode, setPlanEditMode] = useState(true);
  const [planPreview, setPlanPreview] = useState(false);
  const [snapEnabled, setSnapEnabled] = useState(true);
  const [planZoom, setPlanZoom] = useState(1);
  const [planPan, setPlanPan] = useState({ x: 0, y: 0 });
  const [panMode, setPanMode] = useState(false);
  const [planBgUrl, setPlanBgUrl] = useState("");
  const [planZones, setPlanZones] = useState<{ id: string; name: string; x: number; y: number; w: number; h: number; color: string }[]>([]);
  const planRef = useRef<HTMLDivElement | null>(null);
  const [dragWaiterId, setDragWaiterId] = useState<number | null>(null);
  const [dragOverTableId, setDragOverTableId] = useState<number | null>(null);
  const [planTemplates, setPlanTemplates] = useState<HallPlanTemplateDto[]>([]);
  const [applyLayoutsOnImport, setApplyLayoutsOnImport] = useState(true);
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
  const panDragRef = useRef<{
    startX: number;
    startY: number;
    baseX: number;
    baseY: number;
  } | null>(null);
  const [staff, setStaff] = useState<StaffUser[]>([]);
  const [settings, setSettings] = useState<BranchSettings | null>(null);
  const [modGroups, setModGroups] = useState<ModifierGroup[]>([]);
  const [modOptions, setModOptions] = useState<Record<number, ModifierOption[]>>({});
  const [itemModGroups, setItemModGroups] = useState<Record<number, ItemModifierGroup[]>>({});
  const [qrByTable, setQrByTable] = useState<Record<number, string>>({});
  const [statsFrom, setStatsFrom] = useState("");
  const [statsTo, setStatsTo] = useState("");
  const [statsTableId, setStatsTableId] = useState<number | "">("");
  const [statsHallId, setStatsHallId] = useState<number | "">("");
  const [statsHallPlanId, setStatsHallPlanId] = useState<number | "">("");
  const [statsHallPlans, setStatsHallPlans] = useState<HallPlanDto[]>([]);
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
  const [auditFrom, setAuditFrom] = useState("");
  const [auditTo, setAuditTo] = useState("");
  const [auditLimit, setAuditLimit] = useState(200);
  const [parties, setParties] = useState<PartyDto[]>([]);
  const [partyStatusFilter, setPartyStatusFilter] = useState("ACTIVE");
  const [expandedPartyId, setExpandedPartyId] = useState<number | null>(null);
  const [partyTableFilter, setPartyTableFilter] = useState("");
  const [partyPinFilter, setPartyPinFilter] = useState("");
  const [partyExpiringMinutes, setPartyExpiringMinutes] = useState(30);
  const [adminFiltersDirty, setAdminFiltersDirty] = useState(false);
  const [adminFiltersCount, setAdminFiltersCount] = useState(0);

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
  const [menuSearch, setMenuSearch] = useState("");
  const [menuFilterCategoryId, setMenuFilterCategoryId] = useState<number | "">("");
  const [menuFilterActive, setMenuFilterActive] = useState<string | "">("");
  const [menuFilterStopList, setMenuFilterStopList] = useState<string | "">("");

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
  const [tableFilterText, setTableFilterText] = useState("");
  const [tableFilterWaiterId, setTableFilterWaiterId] = useState<number | "">("");
  const [tableFilterHallId, setTableFilterHallId] = useState<number | "">("");
  const [tableFilterAssigned, setTableFilterAssigned] = useState<string | "">("");
  const [bulkHallId, setBulkHallId] = useState<number | "">("");

  const [newStaffUser, setNewStaffUser] = useState("");
  const [newStaffPass, setNewStaffPass] = useState("");
  const [newStaffRole, setNewStaffRole] = useState("WAITER");
  const [staffFilterText, setStaffFilterText] = useState("");
  const [staffFilterRole, setStaffFilterRole] = useState<string | "">("");
  const [staffFilterActive, setStaffFilterActive] = useState<string | "">("");
  const [newModGroupNameRu, setNewModGroupNameRu] = useState("");
  const [newModOptionNameRu, setNewModOptionNameRu] = useState("");
  const [newModOptionPrice, setNewModOptionPrice] = useState(0);
  const [activeModGroupId, setActiveModGroupId] = useState<number | null>(null);

  useEffect(() => {
    const u = localStorage.getItem("adminUser") ?? "";
    const p = localStorage.getItem("adminPass") ?? "";
    const exp = localStorage.getItem("partyExpiringMinutes");
    const menuSearchSaved = localStorage.getItem("admin_menu_search");
    const menuCatSaved = localStorage.getItem("admin_menu_cat");
    const menuActiveSaved = localStorage.getItem("admin_menu_active");
    const menuStopSaved = localStorage.getItem("admin_menu_stop");
    const tableTextSaved = localStorage.getItem("admin_table_search");
    const tableWaiterSaved = localStorage.getItem("admin_table_waiter");
    const tableHallSaved = localStorage.getItem("admin_table_hall");
    const tableAssignedSaved = localStorage.getItem("admin_table_assigned");
    const staffTextSaved = localStorage.getItem("admin_staff_search");
    const staffRoleSaved = localStorage.getItem("admin_staff_role");
    const staffActiveSaved = localStorage.getItem("admin_staff_active");
    const auditLimitSaved = localStorage.getItem("admin_audit_limit");
    if (u && p) {
      setUsername(u);
      setPassword(p);
      setAuthReady(true);
    }
    if (exp) {
      const n = Number(exp);
      if (!Number.isNaN(n) && n > 0) setPartyExpiringMinutes(n);
    }
    if (menuSearchSaved) setMenuSearch(menuSearchSaved);
    if (menuCatSaved) setMenuFilterCategoryId(menuCatSaved ? Number(menuCatSaved) : "");
    if (menuActiveSaved) setMenuFilterActive(menuActiveSaved);
    if (menuStopSaved) setMenuFilterStopList(menuStopSaved);
    if (tableTextSaved) setTableFilterText(tableTextSaved);
    if (tableWaiterSaved) setTableFilterWaiterId(tableWaiterSaved ? Number(tableWaiterSaved) : "");
    if (tableHallSaved) setTableFilterHallId(tableHallSaved ? Number(tableHallSaved) : "");
    if (tableAssignedSaved) setTableFilterAssigned(tableAssignedSaved);
    if (staffTextSaved) setStaffFilterText(staffTextSaved);
    if (staffRoleSaved) setStaffFilterRole(staffRoleSaved);
    if (staffActiveSaved) setStaffFilterActive(staffActiveSaved);
    if (auditLimitSaved) {
      const n = Number(auditLimitSaved);
      if (!Number.isNaN(n) && n > 0) setAuditLimit(n);
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
  const isInteractive = planEditMode && !planPreview;

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

  const computePlanBounds = () => {
    let maxX = 0;
    let maxY = 0;
    const hallTables = tables.filter((t) => (hallId === "" ? true : t.hallId === hallId));
    hallTables.forEach((t, idx) => {
      const l = getTableLayout(t, idx);
      maxX = Math.max(maxX, (l.layoutX ?? 0) + (l.layoutW ?? 0));
      maxY = Math.max(maxY, (l.layoutY ?? 0) + (l.layoutH ?? 0));
    });
    planZones.forEach((z) => {
      maxX = Math.max(maxX, z.x + z.w);
      maxY = Math.max(maxY, z.y + z.h);
    });
    return { maxX: Math.max(1, maxX), maxY: Math.max(1, maxY) };
  };

  const fitPlanToScreen = () => {
    const { maxX, maxY } = computePlanBounds();
    const zoomX = 100 / maxX;
    const zoomY = 100 / maxY;
    const target = Math.min(2, Math.max(0.3, Math.min(zoomX, zoomY)));
    setPlanZoom(Number(target.toFixed(2)));
  };

  useEffect(() => {
    const handleMove = (e: PointerEvent) => {
      if (panDragRef.current) {
        const dx = e.clientX - panDragRef.current.startX;
        const dy = e.clientY - panDragRef.current.startY;
        const nextX = panDragRef.current.baseX + dx;
        const nextY = panDragRef.current.baseY + dy;
        const clamp = (v: number) => Math.min(800, Math.max(-800, v));
        setPlanPan({ x: clamp(nextX), y: clamp(nextY) });
        return;
      }
      if (!planRef.current) return;
      const rect = planRef.current.getBoundingClientRect();
      if (rect.width <= 0 || rect.height <= 0) return;
      const startRef = zoneDragRef.current ?? dragRef.current;
      if (!startRef) return;
      const scale = planZoom || 1;
      const dx = (((e.clientX - startRef.startX) / rect.width) * 100) / scale;
      const dy = (((e.clientY - startRef.startY) / rect.height) * 100) / scale;
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
      panDragRef.current = null;
    };
    window.addEventListener("pointermove", handleMove);
    window.addEventListener("pointerup", handleUp);
    return () => {
      window.removeEventListener("pointermove", handleMove);
      window.removeEventListener("pointerup", handleUp);
    };
  }, [planZoom, snapEnabled]);

  useEffect(() => {
    if (!authReady || !hallId) return;
    (async () => {
      try {
        const res = await api(`/api/admin/halls/${hallId}/plan-templates`);
        const body = await res.json();
        setPlanTemplates(body);
      } catch (_) {
        setPlanTemplates([]);
      }
    })();
  }, [authReady, hallId]);

  async function loadAll() {
    if (!authReady) return;
    setError(null);
    try {
      const [catsRes, itemsRes, tablesRes, staffRes, settingsRes, modGroupsRes, partiesRes, hallsRes] = await Promise.all([
        api("/api/admin/menu/categories"),
        api("/api/admin/menu/items"),
        api("/api/admin/tables"),
        api("/api/admin/staff"),
        api("/api/admin/branch-settings"),
        api("/api/admin/modifier-groups"),
        api(`/api/admin/parties?status=${encodeURIComponent(partyStatusFilter)}`),
        api("/api/admin/halls"),
      ]);
      setCategories(await catsRes.json());
      setItems(await itemsRes.json());
      setTables(await tablesRes.json());
      setStaff(await staffRes.json());
      setSettings(await settingsRes.json());
      setModGroups(await modGroupsRes.json());
      setParties(await partiesRes.json());
      const hallsBody = await hallsRes.json();
      setHalls(hallsBody);
      if (!hallId && hallsBody.length > 0) {
        setHallId(hallsBody[0].id);
      }
    } catch (e: any) {
      setError(e?.message ?? "Load error");
    }
  }

  useEffect(() => {
    if (!authReady || !hallId) return;
    (async () => {
      try {
        const [hallRes, plansRes, tablesRes] = await Promise.all([
          api(`/api/admin/halls/${hallId}`),
          api(`/api/admin/halls/${hallId}/plans`),
          api(`/api/admin/tables`),
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
        const allTables = await tablesRes.json();
        setTables(allTables);
      } catch (e: any) {
        setError(e?.message ?? "Hall load error");
      }
    })();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [hallId, authReady]);

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

  useEffect(() => {
    if (!authReady || statsHallId === "") {
      setStatsHallPlans([]);
      setStatsHallPlanId("");
      return;
    }
    setStatsHallPlanId("");
    (async () => {
      try {
        const res = await api(`/api/admin/halls/${statsHallId}/plans`);
        const body = await res.json();
        setStatsHallPlans(body);
      } catch (_) {
        setStatsHallPlans([]);
      }
    })();
  }, [authReady, statsHallId]);

  useEffect(() => {
    localStorage.setItem("admin_menu_search", menuSearch);
    localStorage.setItem("admin_menu_cat", menuFilterCategoryId === "" ? "" : String(menuFilterCategoryId));
    localStorage.setItem("admin_menu_active", menuFilterActive);
    localStorage.setItem("admin_menu_stop", menuFilterStopList);
  }, [menuSearch, menuFilterCategoryId, menuFilterActive, menuFilterStopList]);

  useEffect(() => {
    localStorage.setItem("admin_table_search", tableFilterText);
    localStorage.setItem("admin_table_waiter", tableFilterWaiterId === "" ? "" : String(tableFilterWaiterId));
    localStorage.setItem("admin_table_hall", tableFilterHallId === "" ? "" : String(tableFilterHallId));
    localStorage.setItem("admin_table_assigned", tableFilterAssigned);
  }, [tableFilterText, tableFilterWaiterId, tableFilterHallId, tableFilterAssigned]);

  useEffect(() => {
    localStorage.setItem("admin_staff_search", staffFilterText);
    localStorage.setItem("admin_staff_role", staffFilterRole);
    localStorage.setItem("admin_staff_active", staffFilterActive);
  }, [staffFilterText, staffFilterRole, staffFilterActive]);

  useEffect(() => {
    if (auditLimit > 0) {
      localStorage.setItem("admin_audit_limit", String(auditLimit));
    }
  }, [auditLimit]);

  useEffect(() => {
    let count = 0;
    if (menuSearch) count++;
    if (menuFilterCategoryId !== "") count++;
    if (menuFilterActive) count++;
    if (menuFilterStopList) count++;
    if (tableFilterText) count++;
    if (tableFilterWaiterId !== "") count++;
    if (tableFilterHallId !== "") count++;
    if (tableFilterAssigned) count++;
    if (staffFilterText) count++;
    if (staffFilterRole) count++;
    if (staffFilterActive) count++;
    setAdminFiltersCount(count);
    setAdminFiltersDirty(count > 0);
  }, [
    menuSearch,
    menuFilterCategoryId,
    menuFilterActive,
    menuFilterStopList,
    tableFilterText,
    tableFilterWaiterId,
    tableFilterHallId,
    tableFilterAssigned,
    staffFilterText,
    staffFilterRole,
    staffFilterActive,
  ]);

  useEffect(() => {
    loadAll();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [authReady, partyStatusFilter]);

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
        hallId: hallId === "" ? null : hallId,
      }),
    });
    setNewTablePublicId("");
    setNewTableNumber(1);
    setNewTableWaiterId("");
    loadAll();
  }

  async function saveTableLayout() {
    if (!hallId) return;
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
    await api("/api/admin/tables/layout", {
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
    loadAll();
  }

  async function exportPlanJson() {
    if (!hallPlanId) return;
    const res = await api(`/api/admin/hall-plans/${hallPlanId}/export`);
    const body = await res.json();
    const blob = new Blob([JSON.stringify(body, null, 2)], { type: "application/json" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `${body.name || "plan"}-export.json`;
    document.body.appendChild(a);
    a.click();
    a.remove();
    URL.revokeObjectURL(url);
  }

  async function importPlanJson(file: File, applyLayouts = true) {
    if (!hallId) return;
    if (applyLayouts) {
      const ok = window.confirm(
        "Apply layouts will overwrite current table positions/sizes for this hall. Continue?"
      );
      if (!ok) return;
    }
    const text = await file.text();
    const parsed = JSON.parse(text);
    const name = parsed.name || `Imported ${new Date().toISOString()}`;
    const payload = {
      name,
      backgroundUrl: parsed.backgroundUrl ?? "",
      zonesJson: parsed.zonesJson ?? "",
      tables: parsed.tables ?? [],
      applyLayouts,
    };
    const res = await api(`/api/admin/halls/${hallId}/plans/import`, {
      method: "POST",
      body: JSON.stringify(payload),
    });
    const plan = await res.json();
    setHallPlanId(plan.id);
    loadAll();
  }

  function saveTemplate() {
    if (!hallPlanId) return;
    const name = prompt("Template name", hallPlans.find((p) => p.id === hallPlanId)?.name ?? "Template");
    if (!name) return;
    const payload = {
      name,
      backgroundUrl: planBgUrl,
      zonesJson: JSON.stringify(planZones),
      tables: tables
        .filter((t) => (hallId === "" ? true : t.hallId === hallId))
        .map((t, idx) => {
          const layout = getTableLayout(t, idx);
          return {
            publicId: t.publicId,
            number: t.number,
            layoutX: layout.layoutX,
            layoutY: layout.layoutY,
            layoutW: layout.layoutW,
            layoutH: layout.layoutH,
            layoutShape: layout.layoutShape,
            layoutRotation: layout.layoutRotation,
            layoutZone: layout.layoutZone,
          };
        }),
    };
    api(`/api/admin/halls/${hallId}/plan-templates`, {
      method: "POST",
      body: JSON.stringify({ name, payloadJson: JSON.stringify(payload) }),
    })
      .then((r) => r.json())
      .then((saved) => {
        setPlanTemplates((prev) => [saved, ...prev.filter((t) => t.id !== saved.id)]);
      })
      .catch(() => {});
  }

  async function applyTemplate(t: HallPlanTemplateDto) {
    if (!hallId) return;
    const ok = window.confirm(
      "Apply template will overwrite current table positions/sizes for this hall. Continue?"
    );
    if (!ok) return;
    let payload: any = null;
    try {
      payload = JSON.parse(t.payloadJson);
    } catch (_) {}
    if (!payload) return;
    const res = await api(`/api/admin/halls/${hallId}/plans/import`, {
      method: "POST",
      body: JSON.stringify({ ...payload, applyLayouts: true }),
    });
    const plan = await res.json();
    setHallPlanId(plan.id);
    loadAll();
  }

  function removeTemplate(id: number) {
    if (!hallId) return;
    api(`/api/admin/hall-plan-templates/${id}`, { method: "DELETE" })
      .then(() => setPlanTemplates((prev) => prev.filter((t) => t.id !== id)))
      .catch(() => {});
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
          hallId: hallId === "" ? t.hallId : hallId,
        };
      })
    );
  }

  function resetAllFilters() {
    setMenuSearch("");
    setMenuFilterCategoryId("");
    setMenuFilterActive("");
    setMenuFilterStopList("");
    setTableFilterText("");
    setTableFilterWaiterId("");
    setTableFilterHallId("");
    setTableFilterAssigned("");
    setStaffFilterText("");
    setStaffFilterRole("");
    setStaffFilterActive("");
  }

  const selectedTable = tables.find((t) => t.id === selectedTableId) ?? null;

  function updateSelectedTable(patch: Partial<TableDto>) {
    if (!selectedTable) return;
    setTables((prev) => prev.map((t) => (t.id === selectedTable.id ? { ...t, ...patch } : t)));
  }

  async function assignWaiter(tableId: number, waiterId: number | null) {
    await api(`/api/admin/tables/${tableId}`, {
      method: "PATCH",
      body: JSON.stringify({ assignedWaiterId: waiterId }),
    });
    loadAll();
  }

  function handleWaiterDragStart(e: DragEvent<HTMLDivElement>, waiterId: number) {
    if (!isInteractive) return;
    e.dataTransfer.setData("text/plain", String(waiterId));
    e.dataTransfer.effectAllowed = "move";
    setDragWaiterId(waiterId);
  }

  function handleWaiterDragEnd() {
    setDragWaiterId(null);
    setDragOverTableId(null);
  }

  async function assignHall(tableId: number, newHallId: number | null) {
    await api(`/api/admin/tables/${tableId}`, {
      method: "PATCH",
      body: JSON.stringify({ hallId: newHallId }),
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

  async function refreshAllQrs() {
    const updates: Record<number, string> = {};
    for (const t of tables) {
      try {
        const res = await api(`/api/admin/tables/${t.publicId}/signed-url`);
        const body = await res.json();
        updates[t.id] = body.url as string;
      } catch {
        // ignore per-table failures
      }
    }
    if (Object.keys(updates).length > 0) {
      setQrByTable((prev) => ({ ...prev, ...updates }));
    }
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

  async function updateStaffRole(su: StaffUser, role: string) {
    await api(`/api/admin/staff/${su.id}`, {
      method: "PATCH",
      body: JSON.stringify({ role }),
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
    if (statsHallId !== "") qs.set("hallId", String(statsHallId));
    if (statsHallPlanId !== "") qs.set("planId", String(statsHallPlanId));
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
    if (statsHallId !== "") qs.set("hallId", String(statsHallId));
    if (statsHallPlanId !== "") qs.set("planId", String(statsHallPlanId));
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

  function buildAuditQuery(overrides?: {
    beforeId?: number | "";
    afterId?: number | "";
    from?: string;
    to?: string;
  }) {
    const qs = new URLSearchParams();
    if (auditAction.trim()) qs.set("action", auditAction.trim());
    if (auditEntityType.trim()) qs.set("entityType", auditEntityType.trim());
    if (auditActor.trim()) qs.set("actorUsername", auditActor.trim());
    const beforeIdVal = overrides?.beforeId ?? auditBeforeId;
    const afterIdVal = overrides?.afterId ?? auditAfterId;
    const fromVal = overrides?.from ?? auditFrom;
    const toVal = overrides?.to ?? auditTo;
    if (beforeIdVal !== "") qs.set("beforeId", String(beforeIdVal));
    if (afterIdVal !== "") qs.set("afterId", String(afterIdVal));
    if (fromVal) qs.set("from", fromVal);
    if (toVal) qs.set("to", toVal);
    if (auditLimit) qs.set("limit", String(auditLimit));
    return qs;
  }

  async function loadAuditLogs(overrides?: {
    beforeId?: number | "";
    afterId?: number | "";
    from?: string;
    to?: string;
  }) {
    setAuditLoading(true);
    try {
      const qs = buildAuditQuery(overrides);
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
    const qs = buildAuditQuery();
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
    setAuditFrom("");
    setAuditTo("");
  }

  async function loadAuditNextPage() {
    if (auditLogs.length === 0) return;
    const lastId = auditLogs[auditLogs.length - 1]?.id;
    if (!lastId) return;
    setAuditBeforeId(lastId);
    setAuditAfterId("");
    await loadAuditLogs({ beforeId: lastId, afterId: "" });
  }

  async function loadAuditPrevPage() {
    if (auditLogs.length === 0) return;
    const firstId = auditLogs[0]?.id;
    if (!firstId) return;
    setAuditAfterId(firstId);
    setAuditBeforeId("");
    await loadAuditLogs({ afterId: firstId, beforeId: "" });
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
      <div style={{ marginTop: 8, display: "flex", alignItems: "center", gap: 10 }}>
        <button onClick={resetAllFilters}>Reset all filters</button>
        {adminFiltersDirty && (
          <span
            style={{
              fontSize: 12,
              padding: "2px 8px",
              borderRadius: 999,
              background: "#fee2e2",
              color: "#991b1b",
              fontWeight: 600,
            }}
          >
            Filters active: {adminFiltersCount}
          </span>
        )}
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
        <h2>Parties</h2>
        <div style={{ display: "flex", gap: 8, flexWrap: "wrap", alignItems: "center" }}>
          <label>
            Status
            <select value={partyStatusFilter} onChange={(e) => setPartyStatusFilter(e.target.value)}>
              <option value="ACTIVE">ACTIVE</option>
              <option value="CLOSED">CLOSED</option>
            </select>
          </label>
          <label>
            Table
            <input
              value={partyTableFilter}
              onChange={(e) => setPartyTableFilter(e.target.value)}
              placeholder="#"
              style={{ width: 80 }}
            />
          </label>
          <label>
            PIN
            <input
              value={partyPinFilter}
              onChange={(e) => setPartyPinFilter(e.target.value)}
              placeholder="0000"
              style={{ width: 80 }}
            />
          </label>
          <label>
            Expiring (min)
            <input
              type="number"
              min={1}
              value={partyExpiringMinutes}
              onChange={(e) => {
                const n = Number(e.target.value);
                setPartyExpiringMinutes(n);
                if (!Number.isNaN(n) && n > 0) localStorage.setItem("partyExpiringMinutes", String(n));
              }}
              style={{ width: 80 }}
            />
          </label>
          <div style={{ display: "flex", alignItems: "center", gap: 6, fontSize: 12, color: "#7c2d12" }}>
            <span style={{ width: 10, height: 10, borderRadius: 999, background: "#fde68a", display: "inline-block" }} />
            Expiring
          </div>
          <button onClick={loadAll}>Refresh</button>
        </div>
        {parties.length === 0 ? (
          <div style={{ marginTop: 8, color: "#666" }}>No parties</div>
        ) : (
          <div style={{ marginTop: 10 }}>
            <table style={{ width: "100%", borderCollapse: "collapse" }}>
              <thead>
                <tr>
                  <th style={{ textAlign: "left", borderBottom: "1px solid #ddd", padding: "6px 4px" }}>ID</th>
                  <th style={{ textAlign: "left", borderBottom: "1px solid #ddd", padding: "6px 4px" }}>Table</th>
                  <th style={{ textAlign: "left", borderBottom: "1px solid #ddd", padding: "6px 4px" }}>PIN</th>
                  <th style={{ textAlign: "left", borderBottom: "1px solid #ddd", padding: "6px 4px" }}>Status</th>
                  <th style={{ textAlign: "left", borderBottom: "1px solid #ddd", padding: "6px 4px" }}>Participants</th>
                  <th style={{ textAlign: "left", borderBottom: "1px solid #ddd", padding: "6px 4px" }}>Created</th>
                  <th style={{ textAlign: "left", borderBottom: "1px solid #ddd", padding: "6px 4px" }}>Expires</th>
                </tr>
              </thead>
              <tbody>
                {parties
                  .filter((p) => {
                    if (partyTableFilter.trim()) {
                      const num = Number(partyTableFilter.replace("#", "").trim());
                      if (!Number.isNaN(num) && p.tableNumber !== num) return false;
                      if (Number.isNaN(num) && !String(p.tableNumber).includes(partyTableFilter.trim())) return false;
                    }
                    if (partyPinFilter.trim() && !String(p.pin ?? "").includes(partyPinFilter.trim())) return false;
                    return true;
                  })
                  .map((p) => {
                    const expanded = expandedPartyId === p.id;
                    const expiresAtMs = Date.parse(p.expiresAt);
                    const expiringSoon =
                      p.status === "ACTIVE" &&
                      !Number.isNaN(expiresAtMs) &&
                      expiresAtMs - Date.now() <= partyExpiringMinutes * 60 * 1000 &&
                      expiresAtMs > Date.now();
                    return (
                      <React.Fragment key={p.id}>
                        <tr
                          onClick={() => setExpandedPartyId(expanded ? null : p.id)}
                          style={{
                            cursor: "pointer",
                            background: expanded ? "#fafafa" : expiringSoon ? "#fff4e5" : "transparent",
                          }}
                        >
                          <td style={{ padding: "6px 4px", borderBottom: "1px solid #f0f0f0" }}>{p.id}</td>
                          <td style={{ padding: "6px 4px", borderBottom: "1px solid #f0f0f0" }}>#{p.tableNumber}</td>
                          <td style={{ padding: "6px 4px", borderBottom: "1px solid #f0f0f0" }}>{p.pin}</td>
                          <td style={{ padding: "6px 4px", borderBottom: "1px solid #f0f0f0" }}>
                            {p.status}
                            {expiringSoon && (
                              <span
                                style={{
                                  marginLeft: 8,
                                  padding: "2px 6px",
                                  borderRadius: 999,
                                  background: "#fde68a",
                                  color: "#7c2d12",
                                  fontSize: 11,
                                  fontWeight: 600,
                                }}
                              >
                                EXPIRING
                              </span>
                            )}
                          </td>
                          <td style={{ padding: "6px 4px", borderBottom: "1px solid #f0f0f0" }}>
                            {p.guestSessionIds.length === 0 ? "-" : p.guestSessionIds.length}
                          </td>
                          <td style={{ padding: "6px 4px", borderBottom: "1px solid #f0f0f0" }}>{p.createdAt}</td>
                          <td style={{ padding: "6px 4px", borderBottom: "1px solid #f0f0f0" }}>{p.expiresAt}</td>
                        </tr>
                        {expanded && (
                          <tr>
                            <td colSpan={7} style={{ padding: "8px 6px", borderBottom: "1px solid #f0f0f0", background: "#fafafa" }}>
                              <div style={{ fontSize: 12, color: "#666", marginBottom: 6 }}>Participants (GuestSession IDs)</div>
                              {p.guestSessionIds.length === 0 ? (
                                <div style={{ color: "#999" }}>No participants</div>
                              ) : (
                                <div style={{ display: "flex", flexWrap: "wrap", gap: 6 }}>
                                  {p.guestSessionIds.map((id) => (
                                    <span
                                      key={id}
                                      style={{
                                        padding: "4px 8px",
                                        borderRadius: 999,
                                        background: "#eef2ff",
                                        color: "#334155",
                                        fontSize: 12,
                                      }}
                                    >
                                      #{id}
                                    </span>
                                  ))}
                                </div>
                              )}
                              <div style={{ marginTop: 8 }}>
                                <button
                                  onClick={() => {
                                    const text = p.guestSessionIds.join(", ");
                                    navigator.clipboard?.writeText(text);
                                  }}
                                  disabled={p.guestSessionIds.length === 0}
                                >
                                  Copy IDs
                                </button>
                              </div>
                            </td>
                          </tr>
                        )}
                      </React.Fragment>
                    );
                  })}
              </tbody>
            </table>
          </div>
        )}
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
            Hall
            <select value={statsHallId} onChange={(e) => setStatsHallId(e.target.value ? Number(e.target.value) : "")}>
              <option value="">All</option>
              {halls.map((h) => (
                <option key={h.id} value={h.id}>{h.name}</option>
              ))}
            </select>
          </label>
          <label>
            Plan
            <select
              value={statsHallPlanId}
              onChange={(e) => setStatsHallPlanId(e.target.value ? Number(e.target.value) : "")}
              disabled={statsHallId === ""}
            >
              <option value="">All</option>
              {statsHallPlans.map((p) => (
                <option key={p.id} value={p.id}>{p.name}</option>
              ))}
            </select>
          </label>
          {statsHallId === "" && (
            <span style={{ fontSize: 12, color: "#666" }}>Plan доступен после выбора Hall</span>
          )}
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
          <label>From <input type="date" value={auditFrom} onChange={(e) => setAuditFrom(e.target.value)} /></label>
          <label>To <input type="date" value={auditTo} onChange={(e) => setAuditTo(e.target.value)} /></label>
          <label>Before ID <input type="number" value={auditBeforeId} onChange={(e) => setAuditBeforeId(e.target.value ? Number(e.target.value) : "")} /></label>
          <label>After ID <input type="number" value={auditAfterId} onChange={(e) => setAuditAfterId(e.target.value ? Number(e.target.value) : "")} /></label>
          <label>Limit <input type="number" min={1} max={500} value={auditLimit} onChange={(e) => setAuditLimit(Number(e.target.value))} style={{ width: 90 }} /></label>
          <button onClick={loadAuditLogs} disabled={auditLoading}>{auditLoading ? "Loading..." : "Load"}</button>
          <button onClick={downloadAuditCsv} disabled={auditLoading}>CSV</button>
          <button onClick={() => { setAuditAfterId(""); setAuditBeforeId(""); loadAuditLogs(); }} disabled={auditLoading}>Latest</button>
          <button onClick={loadAuditPrevPage} disabled={auditLoading || auditLogs.length === 0}>Prev page</button>
          <button onClick={loadAuditNextPage} disabled={auditLoading || auditLogs.length === 0}>Next page</button>
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
        <div style={{ marginBottom: 8, display: "flex", gap: 8, alignItems: "center", flexWrap: "wrap" }}>
          <input
            placeholder="Search (RU/RO/EN)"
            value={menuSearch}
            onChange={(e) => setMenuSearch(e.target.value)}
          />
          <select value={menuFilterCategoryId} onChange={(e) => setMenuFilterCategoryId(e.target.value ? Number(e.target.value) : "")}>
            <option value="">All categories</option>
            {categories.map((c) => (
              <option key={c.id} value={c.id}>{c.nameRu}</option>
            ))}
          </select>
          <select value={menuFilterActive} onChange={(e) => setMenuFilterActive(e.target.value)}>
            <option value="">All statuses</option>
            <option value="ACTIVE">Active</option>
            <option value="INACTIVE">Inactive</option>
          </select>
          <select value={menuFilterStopList} onChange={(e) => setMenuFilterStopList(e.target.value)}>
            <option value="">Stop list: any</option>
            <option value="STOP">Stop list</option>
            <option value="OK">Not in stop list</option>
          </select>
          <button onClick={() => setMenuSearch("")}>Clear</button>
          <button onClick={() => { setMenuSearch(""); setMenuFilterCategoryId(""); setMenuFilterActive(""); setMenuFilterStopList(""); }}>Reset filters</button>
        </div>
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
          {items
            .filter((it) => {
              const q = menuSearch.trim().toLowerCase();
              if (!q) return true;
              const nRu = (it.nameRu ?? "").toLowerCase();
              const nRo = (it.nameRo ?? "").toLowerCase();
              const nEn = (it.nameEn ?? "").toLowerCase();
              return nRu.includes(q) || nRo.includes(q) || nEn.includes(q);
            })
            .filter((it) => {
              if (menuFilterCategoryId !== "" && it.categoryId !== menuFilterCategoryId) return false;
              if (menuFilterActive) {
                const active = menuFilterActive === "ACTIVE";
                if (it.isActive !== active) return false;
              }
              if (menuFilterStopList) {
                const stop = menuFilterStopList === "STOP";
                if (it.isStopList !== stop) return false;
              }
              return true;
            })
            .map((it) => (
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
        <h2>Floor Plan</h2>
        <div style={{ display: "flex", gap: 8, flexWrap: "wrap", alignItems: "center" }}>
          <label style={{ display: "flex", alignItems: "center", gap: 6 }}>
            <input type="checkbox" checked={planEditMode} onChange={(e) => setPlanEditMode(e.target.checked)} />
            Edit mode
          </label>
          <label style={{ display: "flex", alignItems: "center", gap: 6 }}>
            <input type="checkbox" checked={snapEnabled} onChange={(e) => setSnapEnabled(e.target.checked)} />
            Snap to grid
          </label>
          <label style={{ display: "flex", alignItems: "center", gap: 6 }}>
            <input type="checkbox" checked={planPreview} onChange={(e) => setPlanPreview(e.target.checked)} />
            Preview
          </label>
          <label style={{ display: "flex", alignItems: "center", gap: 6 }}>
            <input type="checkbox" checked={panMode} onChange={(e) => setPanMode(e.target.checked)} />
            Pan mode
          </label>
          <button onClick={fitPlanToScreen}>Fit to screen</button>
          <button onClick={() => setPlanZoom(1)}>Reset zoom</button>
          <button onClick={() => setPlanPan({ x: 0, y: 0 })}>Reset pan</button>
          <button onClick={() => setPlanZoom((z) => Math.max(0.3, Number((z - 0.1).toFixed(2))))}>-</button>
          <button onClick={() => setPlanZoom((z) => Math.min(2, Number((z + 0.1).toFixed(2))))}>+</button>
          <input
            type="range"
            min={0.3}
            max={2}
            step={0.05}
            value={planZoom}
            onChange={(e) => setPlanZoom(Number(e.target.value))}
          />
          <span style={{ color: "#666" }}>Zoom: {Math.round(planZoom * 100)}%</span>
          <select value={hallId} onChange={(e) => setHallId(e.target.value ? Number(e.target.value) : "")}>
            <option value="">Select hall</option>
            {halls.map((h) => {
              const planName = hallPlans.find((p) => p.id === h.activePlanId)?.name;
              return (
                <option key={h.id} value={h.id}>
                  {h.name}{planName ? ` • ${planName}` : ""}
                </option>
              );
            })}
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
              loadAll();
            }}
            disabled={!hallId || !hallPlanId}
          >
            Set active
          </button>
          <button
            onClick={async () => {
              if (!hallPlanId) return;
              await api(`/api/admin/hall-plans/${hallPlanId}/duplicate`, { method: "POST", body: JSON.stringify({}) });
              loadAll();
            }}
            disabled={!hallPlanId}
          >
            Duplicate plan
          </button>
          <button
            onClick={async () => {
              if (!hallPlanId) return;
              await api(`/api/admin/hall-plans/${hallPlanId}`, { method: "DELETE" });
              setHallPlanId("");
              loadAll();
            }}
            disabled={!hallPlanId}
          >
            Delete plan
          </button>
          <button onClick={autoLayoutTables}>Auto layout</button>
          <button onClick={saveTableLayout}>Save layout</button>
          <button onClick={exportPlanJson} disabled={!hallPlanId}>Export JSON</button>
          <label style={{ display: "inline-flex", alignItems: "center", gap: 6 }}>
            <span style={{ fontSize: 12, color: "#666" }}>Import JSON</span>
            <label style={{ display: "inline-flex", alignItems: "center", gap: 6, fontSize: 12, color: "#666" }}>
              <input
                type="checkbox"
                checked={applyLayoutsOnImport}
                onChange={(e) => setApplyLayoutsOnImport(e.target.checked)}
              />
              Apply layouts (overwrite)
            </label>
            <input
              type="file"
              accept="application/json"
              onChange={(e) => {
                const f = e.target.files?.[0];
                if (f) importPlanJson(f, applyLayoutsOnImport);
                e.currentTarget.value = "";
              }}
            />
          </label>
          <span style={{ color: "#666" }}>Drag tables on the map. Click a table to edit size/shape.</span>
        </div>
        <div style={{ display: "flex", gap: 8, flexWrap: "wrap", alignItems: "center", marginTop: 8 }}>
          <input placeholder="New hall name" value={newHallName} onChange={(e) => setNewHallName(e.target.value)} />
          <input type="number" placeholder="Sort" value={newHallSort} onChange={(e) => setNewHallSort(Number(e.target.value))} />
          <button
            onClick={async () => {
              if (!newHallName.trim()) return;
              await api("/api/admin/halls", { method: "POST", body: JSON.stringify({ name: newHallName.trim(), sortOrder: newHallSort }) });
              setNewHallName("");
              setNewHallSort(0);
              loadAll();
            }}
          >
            Add hall
          </button>
          <input placeholder="New plan name" value={newPlanName} onChange={(e) => setNewPlanName(e.target.value)} />
          <input type="number" placeholder="Plan sort" value={newPlanSort} onChange={(e) => setNewPlanSort(Number(e.target.value))} />
          <button
            onClick={async () => {
              if (!hallId || !newPlanName.trim()) return;
              await api(`/api/admin/halls/${hallId}/plans`, {
                method: "POST",
                body: JSON.stringify({ name: newPlanName.trim(), sortOrder: newPlanSort }),
              });
              setNewPlanName("");
              setNewPlanSort(0);
              loadAll();
            }}
          >
            Add plan
          </button>
        </div>
        {hallId && (
          <div style={{ display: "flex", gap: 8, flexWrap: "wrap", alignItems: "center", marginTop: 8 }}>
            <span style={{ fontSize: 12, color: "#666" }}>Quick switch:</span>
            {["Day", "Evening", "Banquet"].map((name) => (
              <button
                key={name}
                onClick={async () => {
                  if (!hallId) return;
                  let plan = hallPlans.find((p) => (p.name ?? "").trim().toLowerCase() === name.toLowerCase());
                  if (!plan) {
                    const res = await api(`/api/admin/halls/${hallId}/plans`, {
                      method: "POST",
                      body: JSON.stringify({ name, sortOrder: 0 }),
                    });
                    plan = await res.json();
                  }
                  await api(`/api/admin/halls/${hallId}`, { method: "PATCH", body: JSON.stringify({ activePlanId: plan.id }) });
                  setHallPlanId(plan.id);
                  loadAll();
                }}
                style={{
                  padding: "4px 10px",
                  borderRadius: 999,
                  border:
                    hallPlans.find((p) => (p.name ?? "").trim().toLowerCase() === name.toLowerCase())?.id === hallPlanId
                      ? "1px solid #111"
                      : "1px solid #ddd",
                  background:
                    hallPlans.find((p) => (p.name ?? "").trim().toLowerCase() === name.toLowerCase())?.id === hallPlanId
                      ? "#111"
                      : "#fff",
                  color:
                    hallPlans.find((p) => (p.name ?? "").trim().toLowerCase() === name.toLowerCase())?.id === hallPlanId
                      ? "#fff"
                      : "#111",
                  cursor: "pointer",
                }}
              >
                {name}
              </button>
            ))}
            {hallPlans.length > 0 &&
              hallPlans.map((p) => (
                <button
                  key={p.id}
                  onClick={async () => {
                    await api(`/api/admin/halls/${hallId}`, { method: "PATCH", body: JSON.stringify({ activePlanId: p.id }) });
                    setHallPlanId(p.id);
                    loadAll();
                  }}
                  style={{
                    padding: "4px 10px",
                    borderRadius: 999,
                    border: p.id === hallPlanId ? "1px solid #111" : "1px solid #ddd",
                    background: p.id === hallPlanId ? "#111" : "#fff",
                    color: p.id === hallPlanId ? "#fff" : "#111",
                    cursor: "pointer",
                  }}
                >
                  {p.name}
                </button>
              ))}
          </div>
        )}
        {hallId && (
          <div style={{ display: "flex", gap: 8, flexWrap: "wrap", alignItems: "center", marginTop: 8 }}>
            <span style={{ fontSize: 12, color: "#666" }}>Templates:</span>
            <button onClick={saveTemplate} disabled={!hallPlanId}>Save current</button>
            {planTemplates.map((t) => (
              <span key={t.id} style={{ display: "inline-flex", gap: 6, alignItems: "center" }}>
                <button onClick={() => applyTemplate(t)}>{t.name}</button>
                <button onClick={() => removeTemplate(t.id)} style={{ color: "#b00" }}>×</button>
              </span>
            ))}
            {planTemplates.length === 0 && <span style={{ fontSize: 12, color: "#999" }}>No templates</span>}
          </div>
        )}
        <div style={{ display: "flex", gap: 10, flexWrap: "wrap", alignItems: "center", marginTop: 8 }}>
          <span style={{ fontSize: 12, color: "#666" }}>Legend:</span>
          {staff.filter((s) => s.role === "WAITER").map((s) => (
            <span key={s.id} style={{ display: "inline-flex", alignItems: "center", gap: 6, fontSize: 12 }}>
              <span
                style={{
                  width: 10,
                  height: 10,
                  borderRadius: "50%",
                  background: waiterColor(s.id),
                  display: "inline-block",
                }}
              />
              {s.username} #{s.id}
            </span>
          ))}
          {staff.filter((s) => s.role === "WAITER").length === 0 && (
            <span style={{ fontSize: 12, color: "#999" }}>Нет официантов</span>
          )}
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
            onPointerDown={(e) => {
              if (!panMode) return;
              if (e.target !== e.currentTarget) return;
              panDragRef.current = {
                startX: e.clientX,
                startY: e.clientY,
                baseX: planPan.x,
                baseY: planPan.y,
              };
            }}
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
              cursor: panMode ? "grab" : "default",
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
                transform: `translate(${planPan.x}px, ${planPan.y}px) scale(${planZoom})`,
                transformOrigin: "top left",
              }}
            >
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
                  if (!isInteractive || panMode) return;
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
                  {isInteractive && (
                    <>
                      {["nw", "ne", "sw", "se"].map((corner) => (
                        <div
                        key={corner}
                        onPointerDown={(e) => {
                          if (!isInteractive || panMode) return;
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
                  const isDropTarget = dragWaiterId !== null && dragOverTableId === t.id;
                  return (
                    <div
                      key={t.id}
                      onPointerDown={(e) => {
                        if (!isInteractive || panMode) return;
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
                      onDragOver={(e) => {
                        if (dragWaiterId === null || !isInteractive) return;
                        e.preventDefault();
                        e.dataTransfer.dropEffect = "move";
                      }}
                      onDragEnter={() => {
                        if (dragWaiterId === null || !isInteractive) return;
                        setDragOverTableId(t.id);
                      }}
                      onDragLeave={() => {
                        if (dragOverTableId === t.id) setDragOverTableId(null);
                      }}
                      onDrop={(e) => {
                        if (!isInteractive) return;
                        e.preventDefault();
                        const data = e.dataTransfer.getData("text/plain");
                        const waiterId = Number(data);
                        if (Number.isFinite(waiterId)) {
                          assignWaiter(t.id, waiterId);
                        }
                        setDragWaiterId(null);
                        setDragOverTableId(null);
                      }}
                      onClick={() => setSelectedTableId(t.id)}
                      style={{
                        position: "absolute",
                        left: `${layout.layoutX}%`,
                        top: `${layout.layoutY}%`,
                        width: `${layout.layoutW}%`,
                        height: `${layout.layoutH}%`,
                        borderRadius: layout.layoutShape === "ROUND" ? 999 : 14,
                        border: isDropTarget ? `2px dashed ${color}` : selected ? `2px solid ${color}` : "1px solid rgba(0,0,0,0.12)",
                        background: selected ? "rgba(255,255,255,0.95)" : "rgba(255,255,255,0.9)",
                        boxShadow: isDropTarget ? "0 0 0 2px rgba(0,0,0,0.12)" : selected ? "0 10px 28px rgba(0,0,0,0.18)" : "0 6px 18px rgba(0,0,0,0.12)",
                        transform: `rotate(${layout.layoutRotation ?? 0}deg)`,
                        display: "flex",
                        alignItems: "center",
                        justifyContent: "center",
                        flexDirection: "column",
                        gap: 4,
                        cursor: isInteractive ? "grab" : "default",
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
                      {isInteractive && selected && (
                        <>
                          {["nw", "ne", "sw", "se"].map((corner) => (
                            <div
                              key={corner}
                              onPointerDown={(e) => {
                                if (!isInteractive || panMode) return;
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
                        </>
                      )}
                    </div>
                  );
                })}
            </div>
          </div>
          <div style={{ border: "1px solid #eee", borderRadius: 12, padding: 12, background: "#fff" }}>
            <h3 style={{ marginTop: 0 }}>Table settings</h3>
            <div style={{ marginBottom: 12 }}>
              <div style={{ fontWeight: 600, marginBottom: 6 }}>Assign waiter (drag onto table)</div>
              <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
                {staff.filter((s) => s.role === "WAITER").map((s) => (
                  <div
                    key={s.id}
                    draggable={isInteractive}
                    onDragStart={(e) => handleWaiterDragStart(e, s.id)}
                    onDragEnd={handleWaiterDragEnd}
                    style={{
                      padding: "6px 10px",
                      borderRadius: 999,
                      background: "#f6f6f6",
                      border: `1px solid ${waiterColor(s.id)}`,
                      color: "#333",
                      fontSize: 12,
                      cursor: isInteractive ? "grab" : "not-allowed",
                      opacity: isInteractive ? 1 : 0.6,
                    }}
                  >
                    {s.username} #{s.id}
                  </div>
                ))}
                {staff.filter((s) => s.role === "WAITER").length === 0 && (
                  <div style={{ color: "#777", fontSize: 12 }}>No waiters yet.</div>
                )}
              </div>
              {!isInteractive && (
                <div style={{ marginTop: 6, fontSize: 12, color: "#777" }}>
                  Switch to Edit mode to enable drag & drop.
                </div>
              )}
            </div>
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
            {hallId && (
              <div style={{ marginTop: 16, borderTop: "1px solid #eee", paddingTop: 12 }}>
                <h4 style={{ margin: 0 }}>Hall settings</h4>
                <div style={{ display: "flex", gap: 8, marginTop: 8 }}>
                  <button
                    onClick={async () => {
                      await api(`/api/admin/halls/${hallId}`, { method: "PATCH", body: JSON.stringify({ isActive: true }) });
                      loadAll();
                    }}
                  >
                    Activate
                  </button>
                  <button
                    onClick={async () => {
                      await api(`/api/admin/halls/${hallId}`, { method: "PATCH", body: JSON.stringify({ isActive: false }) });
                      loadAll();
                    }}
                  >
                    Deactivate
                  </button>
                  <button
                    onClick={async () => {
                      await api(`/api/admin/halls/${hallId}`, { method: "DELETE" });
                      setHallId("");
                      loadAll();
                    }}
                  >
                    Delete
                  </button>
                </div>
              </div>
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
        <h2>Tables & QR</h2>
        <div style={{ marginBottom: 8, color: "#666" }}>
          Note: QR links include a timestamp. Re‑generate before printing if links were created long ago.
        </div>
        <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
          <input type="number" placeholder="Table #" value={newTableNumber} onChange={(e) => setNewTableNumber(Number(e.target.value))} />
          <input placeholder="Public ID (optional)" value={newTablePublicId} onChange={(e) => setNewTablePublicId(e.target.value)} />
          <select value={hallId} onChange={(e) => setHallId(e.target.value ? Number(e.target.value) : "")}>
            <option value="">Select hall</option>
            {halls.map((h) => (
              <option key={h.id} value={h.id}>{h.name}</option>
            ))}
          </select>
          <select value={newTableWaiterId} onChange={(e) => setNewTableWaiterId(e.target.value ? Number(e.target.value) : "")}>
            <option value="">Assign waiter</option>
            {staff.filter((s) => s.role === "WAITER").map((s) => (
              <option key={s.id} value={s.id}>{s.username}</option>
            ))}
          </select>
          <button onClick={createTable}>Add</button>
          <button onClick={refreshAllQrs}>Refresh all QR</button>
        </div>
        <div style={{ marginTop: 8, display: "flex", gap: 8, flexWrap: "wrap", alignItems: "center" }}>
          <input placeholder="Filter by table # or publicId" value={tableFilterText} onChange={(e) => setTableFilterText(e.target.value)} />
          <select value={tableFilterWaiterId} onChange={(e) => setTableFilterWaiterId(e.target.value ? Number(e.target.value) : "")}>
            <option value="">All waiters</option>
            {staff.filter((s) => s.role === "WAITER").map((s) => (
              <option key={s.id} value={s.id}>{s.username}</option>
            ))}
          </select>
          <select value={tableFilterHallId} onChange={(e) => setTableFilterHallId(e.target.value ? Number(e.target.value) : "")}>
            <option value="">All halls</option>
            {halls.map((h) => (
              <option key={h.id} value={h.id}>{h.name}</option>
            ))}
          </select>
          <select value={tableFilterAssigned} onChange={(e) => setTableFilterAssigned(e.target.value)}>
            <option value="">All assignments</option>
            <option value="ASSIGNED">Assigned</option>
            <option value="UNASSIGNED">Unassigned</option>
          </select>
          <select value={bulkHallId} onChange={(e) => setBulkHallId(e.target.value ? Number(e.target.value) : "")}>
            <option value="">Bulk hall</option>
            {halls.map((h) => (
              <option key={h.id} value={h.id}>{h.name}</option>
            ))}
          </select>
          <button
            onClick={async () => {
              if (bulkHallId === "") return;
              const filtered = tables.filter((t) => {
                const q = tableFilterText.trim().toLowerCase();
                if (q) {
                  const hit = String(t.number).includes(q) || t.publicId.toLowerCase().includes(q);
                  if (!hit) return false;
                }
                if (tableFilterWaiterId !== "" && t.assignedWaiterId !== tableFilterWaiterId) return false;
                if (tableFilterHallId !== "" && t.hallId !== tableFilterHallId) return false;
                if (tableFilterAssigned === "ASSIGNED" && !t.assignedWaiterId) return false;
                if (tableFilterAssigned === "UNASSIGNED" && t.assignedWaiterId) return false;
                return true;
              });
              await api("/api/admin/tables/bulk-assign-hall", {
                method: "POST",
                body: JSON.stringify({ tableIds: filtered.map((t) => t.id), hallId: bulkHallId }),
              });
              loadAll();
            }}
          >
            Assign filtered to hall
          </button>
          <button onClick={() => { setTableFilterText(""); setTableFilterWaiterId(""); setTableFilterHallId(""); setTableFilterAssigned(""); }}>Clear</button>
        </div>
        <div style={{ marginTop: 10 }}>
          {tables
            .filter((t) => {
              const q = tableFilterText.trim().toLowerCase();
              if (q) {
                const hit = String(t.number).includes(q) || t.publicId.toLowerCase().includes(q);
                if (!hit) return false;
              }
              if (tableFilterWaiterId !== "" && t.assignedWaiterId !== tableFilterWaiterId) return false;
              if (tableFilterHallId !== "" && t.hallId !== tableFilterHallId) return false;
              if (tableFilterAssigned === "ASSIGNED" && !t.assignedWaiterId) return false;
              if (tableFilterAssigned === "UNASSIGNED" && t.assignedWaiterId) return false;
              return true;
            })
            .map((t) => (
            <div key={t.id} style={{ display: "flex", gap: 10, alignItems: "center", padding: "6px 0", borderBottom: "1px solid #eee" }}>
              <strong>Table #{t.number}</strong>
              <span>{t.publicId}</span>
              <select
                value={t.hallId ?? ""}
                onChange={(e) => assignHall(t.id, e.target.value ? Number(e.target.value) : null)}
              >
                <option value="">No hall</option>
                {halls.map((h) => (
                  <option key={h.id} value={h.id}>{h.name}</option>
                ))}
              </select>
              <select
                value={t.assignedWaiterId ?? ""}
                onChange={(e) => assignWaiter(t.id, e.target.value ? Number(e.target.value) : null)}
              >
                <option value="">No waiter</option>
                {staff.filter((s) => s.role === "WAITER").map((s) => (
                  <option key={s.id} value={s.id}>{s.username}</option>
                ))}
              </select>
              {t.assignedWaiterId && (
                <button onClick={() => assignWaiter(t.id, null)}>Clear waiter</button>
              )}
              <button onClick={() => getSignedUrl(t.publicId)}>QR URL</button>
              <button onClick={() => showQr(t.id, t.publicId)}>Show QR</button>
              <button onClick={() => showQr(t.id, t.publicId)}>Refresh QR</button>
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
        <div style={{ marginTop: 8, display: "flex", gap: 8, flexWrap: "wrap", alignItems: "center" }}>
          <input placeholder="Filter by username" value={staffFilterText} onChange={(e) => setStaffFilterText(e.target.value)} />
          <select value={staffFilterRole} onChange={(e) => setStaffFilterRole(e.target.value)}>
            <option value="">All roles</option>
            <option value="WAITER">WAITER</option>
            <option value="KITCHEN">KITCHEN</option>
            <option value="ADMIN">ADMIN</option>
          </select>
          <select value={staffFilterActive} onChange={(e) => setStaffFilterActive(e.target.value)}>
            <option value="">All statuses</option>
            <option value="ACTIVE">Active</option>
            <option value="INACTIVE">Inactive</option>
          </select>
          <button onClick={() => { setStaffFilterText(""); setStaffFilterRole(""); setStaffFilterActive(""); }}>Clear</button>
        </div>
        <div style={{ marginTop: 10 }}>
          {staff
            .filter((su) => {
              const q = staffFilterText.trim().toLowerCase();
              if (q && !su.username.toLowerCase().includes(q)) return false;
              if (staffFilterRole && su.role !== staffFilterRole) return false;
              if (staffFilterActive) {
                const active = staffFilterActive === "ACTIVE";
                if (su.isActive !== active) return false;
              }
              return true;
            })
            .map((su) => (
            <div key={su.id} style={{ display: "flex", gap: 10, alignItems: "center", padding: "6px 0", borderBottom: "1px solid #eee" }}>
              <strong>{su.username}</strong>
              <select value={su.role} onChange={(e) => updateStaffRole(su, e.target.value)}>
                <option value="WAITER">WAITER</option>
                <option value="KITCHEN">KITCHEN</option>
                <option value="ADMIN">ADMIN</option>
              </select>
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
