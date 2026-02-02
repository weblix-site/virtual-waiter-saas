"use client";

import { useCallback, useEffect, useMemo, useRef, useState } from "react";

const API_BASE = process.env.NEXT_PUBLIC_API_BASE ?? "http://localhost:8080";

type Lang = "ru" | "ro" | "en";
const dict: Record<string, Record<Lang, string>> = {
  loginTitle: { ru: "Вход Super Admin", ro: "Autentificare Super Admin", en: "Super Admin Login" },
  superAdmin: { ru: "Супер‑админ", ro: "Super Admin", en: "Super Admin" },
  username: { ru: "Логин", ro: "Utilizator", en: "Username" },
  password: { ru: "Пароль", ro: "Parolă", en: "Password" },
  login: { ru: "Войти", ro: "Intră", en: "Login" },
  logout: { ru: "Выйти", ro: "Ieși", en: "Logout" },
  sessionExpired: { ru: "Сессия истекла. Войдите снова.", ro: "Sesiunea a expirat. Autentificați‑vă din nou.", en: "Session expired. Please sign in again." },
  refresh: { ru: "Обновить", ro: "Reîmprospătează", en: "Refresh" },
  tenants: { ru: "Заведения (тенанты)", ro: "Tenanți", en: "Tenants" },
  branches: { ru: "Филиалы", ro: "Filiale", en: "Branches" },
  branchSettings: { ru: "Настройки филиала", ro: "Setări filială", en: "Branch settings" },
  defaultLanguage: { ru: "Язык по умолчанию (гость)", ro: "Limba implicită (oaspete)", en: "Default language (guest)" },
  langRu: { ru: "Русский", ro: "Rusă", en: "Russian" },
  langRo: { ru: "Румынский", ro: "Română", en: "Romanian" },
  langEn: { ru: "Английский", ro: "Engleză", en: "English" },
  staffGlobal: { ru: "Персонал (глобально)", ro: "Personal (global)", en: "Staff (global)" },
  active: { ru: "Активен", ro: "Activ", en: "Active" },
  inactive: { ru: "Неактивен", ro: "Inactiv", en: "Inactive" },
  allStatuses: { ru: "Все статусы", ro: "Toate statusurile", en: "All statuses" },
  selectTenant: { ru: "Выберите тeнанта", ro: "Selectați tenantul", en: "Select tenant" },
  selectBranch: { ru: "Выберите филиал", ro: "Selectați filiala", en: "Select branch" },
  newTenantName: { ru: "Имя нового тенанта", ro: "Nume tenant nou", en: "New tenant name" },
  createTenant: { ru: "Создать тенанта", ro: "Creează tenant", en: "Create tenant" },
  newBranchName: { ru: "Имя нового филиала", ro: "Nume filială nouă", en: "New branch name" },
  createBranch: { ru: "Создать филиал", ro: "Creează filială", en: "Create branch" },
  enable: { ru: "Включить", ro: "Activează", en: "Enable" },
  disable: { ru: "Выключить", ro: "Dezactivează", en: "Disable" },
  delete: { ru: "Удалить", ro: "Șterge", en: "Delete" },
  tenant: { ru: "Тенант", ro: "Tenant", en: "Tenant" },
  loadStaff: { ru: "Загрузить персонал", ro: "Încarcă personal", en: "Load staff" },
  firstName: { ru: "Имя", ro: "Prenume", en: "First name" },
  lastName: { ru: "Фамилия", ro: "Nume", en: "Last name" },
  age: { ru: "Возраст", ro: "Vârstă", en: "Age" },
  gender: { ru: "Пол", ro: "Gen", en: "Gender" },
  genderMale: { ru: "Мужской", ro: "Masculin", en: "Male" },
  genderFemale: { ru: "Женский", ro: "Feminin", en: "Female" },
  genderOther: { ru: "Другое", ro: "Altul", en: "Other" },
  photoUrl: { ru: "Фото (URL)", ro: "Foto (URL)", en: "Photo URL" },
  invalidAge: { ru: "Неверный возраст (0–120)", ro: "Vârstă invalidă (0–120)", en: "Invalid age (0–120)" },
  createStaff: { ru: "Создать сотрудника", ro: "Creează personal", en: "Create staff" },
  role: { ru: "Роль", ro: "Rol", en: "Role" },
  currencies: { ru: "Валюты", ro: "Valute", en: "Currencies" },
  addCurrency: { ru: "Добавить валюту", ro: "Adaugă valută", en: "Add currency" },
  code: { ru: "Код", ro: "Cod", en: "Code" },
  name: { ru: "Название", ro: "Denumire", en: "Name" },
  symbol: { ru: "Символ", ro: "Simbol", en: "Symbol" },
  noCurrencies: { ru: "Нет валют", ro: "Nu sunt valute", en: "No currencies" },
  inactiveSuffix: { ru: "(неактивен)", ro: "(inactiv)", en: "(inactive)" },
  edit: { ru: "Редактировать", ro: "Editează", en: "Edit" },
  resetPassword: { ru: "Сбросить пароль", ro: "Resetează parola", en: "Reset password" },
  save: { ru: "Сохранить", ro: "Salvează", en: "Save" },
  cancel: { ru: "Отмена", ro: "Anulează", en: "Cancel" },
  floorPlanBranch: { ru: "План зала (по филиалу)", ro: "Plan sală (pe filială)", en: "Floor Plan (by branch)" },
  editMode: { ru: "Режим редактирования", ro: "Mod editare", en: "Edit mode" },
  snapToGrid: { ru: "Привязка к сетке", ro: "Aliniază la grilă", en: "Snap to grid" },
  preview: { ru: "Предпросмотр", ro: "Previzualizare", en: "Preview" },
  panMode: { ru: "Перемещение", ro: "Pan", en: "Pan mode" },
  fitToScreen: { ru: "Вписать в экран", ro: "Potrivește ecranul", en: "Fit to screen" },
  resetZoom: { ru: "Сбросить масштаб", ro: "Reset zoom", en: "Reset zoom" },
  resetPan: { ru: "Сбросить панораму", ro: "Reset pan", en: "Reset pan" },
  zoom: { ru: "Масштаб", ro: "Zoom", en: "Zoom" },
  planSelectHall: { ru: "Выберите зал", ro: "Selectează sala", en: "Select hall" },
  planDefault: { ru: "План по умолчанию", ro: "Plan implicit", en: "Default plan" },
  setActivePlan: { ru: "Сделать активным", ro: "Setează activ", en: "Set active" },
  duplicatePlan: { ru: "Дублировать план", ro: "Duplică planul", en: "Duplicate plan" },
  deletePlan: { ru: "Удалить план", ro: "Șterge planul", en: "Delete plan" },
  loadTables: { ru: "Загрузить столы", ro: "Încarcă mesele", en: "Load tables" },
  autoLayout: { ru: "Авто‑раскладка", ro: "Auto aranjare", en: "Auto layout" },
  saveLayout: { ru: "Сохранить раскладку", ro: "Salvează aranjarea", en: "Save layout" },
  exportJson: { ru: "Экспорт JSON", ro: "Export JSON", en: "Export JSON" },
  importJson: { ru: "Импорт JSON", ro: "Import JSON", en: "Import JSON" },
  newHallName: { ru: "Новый зал", ro: "Sală nouă", en: "New hall name" },
  sort: { ru: "Сортировка", ro: "Sortare", en: "Sort" },
  addHall: { ru: "Добавить зал", ro: "Adaugă sală", en: "Add hall" },
  newPlanName: { ru: "Новый план", ro: "Plan nou", en: "New plan name" },
  planSort: { ru: "Сортировка плана", ro: "Sortare plan", en: "Plan sort" },
  addPlan: { ru: "Добавить план", ro: "Adaugă plan", en: "Add plan" },
  quickSwitch: { ru: "Быстрое переключение", ro: "Comutare rapidă", en: "Quick switch" },
  dayPlan: { ru: "День", ro: "Zi", en: "Day" },
  eveningPlan: { ru: "Вечер", ro: "Seară", en: "Evening" },
  banquetPlan: { ru: "Банкет", ro: "Banchet", en: "Banquet" },
  templates: { ru: "Шаблоны", ro: "Șabloane", en: "Templates" },
  saveCurrent: { ru: "Сохранить текущий", ro: "Salvează curent", en: "Save current" },
  noTemplates: { ru: "Шаблонов нет", ro: "Nu sunt șabloane", en: "No templates" },
  legend: { ru: "Легенда", ro: "Legendă", en: "Legend" },
  noWaiters: { ru: "Нет официантов", ro: "Nu sunt chelneri", en: "No waiters" },
  backgroundUrl: { ru: "Фон (URL)", ro: "Fundal (URL)", en: "Background URL" },
  waiterLabel: { ru: "Официант", ro: "Chelner", en: "Waiter" },
  unassigned: { ru: "Не назначено", ro: "Neatribuit", en: "Unassigned" },
  tableSettings: { ru: "Настройки стола", ro: "Setări masă", en: "Table settings" },
  tableSelected: { ru: "Стол №", ro: "Masă #", en: "Table #" },
  shape: { ru: "Форма", ro: "Formă", en: "Shape" },
  shapeRound: { ru: "Круглая", ro: "Rotundă", en: "Round" },
  shapeRect: { ru: "Прямоугольная", ro: "Dreptunghiulară", en: "Rectangle" },
  widthPercent: { ru: "Ширина (%)", ro: "Lățime (%)", en: "Width (%)" },
  heightPercent: { ru: "Высота (%)", ro: "Înălțime (%)", en: "Height (%)" },
  rotationDeg: { ru: "Поворот (°)", ro: "Rotire (°)", en: "Rotation (deg)" },
  zone: { ru: "Зона", ro: "Zonă", en: "Zone" },
  zonePlaceholder: { ru: "например, Терраса, Зал A", ro: "ex. Terasă, Sala A", en: "e.g. Terrace, Hall A" },
  resetShape: { ru: "Сброс формы", ro: "Reset formă", en: "Reset" },
  clickTableToEdit: { ru: "Нажмите на стол на плане для редактирования.", ro: "Faceți click pe masă pentru editare.", en: "Click a table on the map to edit." },
  zones: { ru: "Зоны", ro: "Zone", en: "Zones" },
  zoneName: { ru: "Название зоны", ro: "Nume zonă", en: "Zone name" },
  addZone: { ru: "Добавить зону", ro: "Adaugă zonă", en: "Add zone" },
  stats: { ru: "Статистика", ro: "Statistici", en: "Stats" },
  fromDate: { ru: "С", ro: "De la", en: "From" },
  toDate: { ru: "По", ro: "Până la", en: "To" },
  load: { ru: "Загрузить", ro: "Încarcă", en: "Load" },
  summaryCsv: { ru: "Сводка CSV", ro: "Rezumat CSV", en: "Summary CSV" },
  branchesCsv: { ru: "Филиалы CSV", ro: "Filiale CSV", en: "Branches CSV" },
  period: { ru: "Период", ro: "Perioadă", en: "Period" },
  orders: { ru: "Заказы", ro: "Comenzi", en: "Orders" },
  waiterCalls: { ru: "Вызовы", ro: "Apeluri", en: "Waiter calls" },
  paidBills: { ru: "Оплаченные счета", ro: "Note plătite", en: "Paid bills" },
  gross: { ru: "Выручка", ro: "Brut", en: "Gross" },
  tips: { ru: "Чаевые", ro: "Bacșiș", en: "Tips" },
  activeTables: { ru: "Активные столы", ro: "Mese active", en: "Active tables" },
  byBranch: { ru: "По филиалам", ro: "Pe filiale", en: "By Branch" },
  branch: { ru: "Филиал", ro: "Filială", en: "Branch" },
  calls: { ru: "Вызовы", ro: "Apeluri", en: "Calls" },
  rbacMatrix: { ru: "Матрица ролей", ro: "Matrice roluri", en: "RBAC Matrix" },
  roleLabel: { ru: "Роль", ro: "Rol", en: "Role" },
  accessLabel: { ru: "Доступ", ro: "Acces", en: "Access" },
  waiterAccess: { ru: "Заказы, вызовы, счета, подтверждение оплаты", ro: "Comenzi, apeluri, note, confirmare plată", en: "Orders, waiter calls, bill requests, confirm paid" },
  kitchenAccess: { ru: "Очередь кухни, статусы заказов", ro: "Coada bucătăriei, status comenzi", en: "Kitchen queue, order status updates" },
  adminAccess: { ru: "Меню, Столы/QR, Персонал, Настройки, Статистика", ro: "Meniu, Mese/QR, Personal, Setări, Statistici", en: "Menu, Tables/QR, Staff, Settings, Stats" },
  superAdminAccess: { ru: "Тенанты/Филиалы, Глобальный персонал, Глобальная статистика", ro: "Tenanți/Filiale, Personal global, Statistici globale", en: "Tenants/Branches, Global Staff, Global Stats" },
  templateNamePrompt: { ru: "Название шаблона", ro: "Nume șablon", en: "Template name" },
  templateDefaultName: { ru: "Шаблон", ro: "Șablon", en: "Template" },
  loadError: { ru: "Ошибка загрузки", ro: "Eroare la încărcare", en: "Load error" },
  requestFailed: { ru: "Ошибка запроса", ro: "Eroare solicitare", en: "Request failed" },
};

const t = (lang: Lang, key: string) => dict[key]?.[lang] ?? key;

type Tenant = { id: number; name: string; isActive: boolean };
type Branch = { id: number; tenantId: number; name: string; isActive: boolean };
type BranchSettings = { branchId: number; defaultLang?: string };
type StaffUser = {
  id: number;
  branchId: number | null;
  username: string;
  role: string;
  isActive: boolean;
  firstName?: string | null;
  lastName?: string | null;
  age?: number | null;
  gender?: string | null;
  photoUrl?: string | null;
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

type CurrencyDto = {
  code: string;
  name: string;
  symbol?: string | null;
  isActive: boolean;
};

function money(priceCents: number, currency = "MDL") {
  return `${(priceCents / 100).toFixed(2)} ${currency}`;
}

export default function SuperAdminPage() {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [authReady, setAuthReady] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [sessionExpired, setSessionExpired] = useState(false);
  const redirectingRef = useRef(false);
  const [lang, setLang] = useState<Lang>("ru");
  const translate = t;

  const [tenants, setTenants] = useState<Tenant[]>([]);
  const [branches, setBranches] = useState<Branch[]>([]);
  const [staff, setStaff] = useState<StaffUser[]>([]);
  const [tenantId, setTenantId] = useState<number | "">("");
  const [branchId, setBranchId] = useState<number | "">("");
  const [branchSettings, setBranchSettings] = useState<BranchSettings | null>(null);
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
  const [newPlanName, setNewPlanName] = useState("");
  const [newPlanSort, setNewPlanSort] = useState(0);
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
  const [planTemplates, setPlanTemplates] = useState<{ name: string; payload: any }[]>([]);
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

  const [newTenantName, setNewTenantName] = useState("");
  const [newBranchName, setNewBranchName] = useState("");
  const [newStaffUser, setNewStaffUser] = useState("");
  const [newStaffPass, setNewStaffPass] = useState("");
  const [newStaffRole, setNewStaffRole] = useState("ADMIN");
  const [editingStaffId, setEditingStaffId] = useState<number | null>(null);
  const [editStaffRole, setEditStaffRole] = useState("ADMIN");
  const [editStaffActive, setEditStaffActive] = useState(true);
  const [editStaffFirstName, setEditStaffFirstName] = useState("");
  const [editStaffLastName, setEditStaffLastName] = useState("");
  const [editStaffAge, setEditStaffAge] = useState("");
  const [editStaffGender, setEditStaffGender] = useState("");
  const [editStaffPhotoUrl, setEditStaffPhotoUrl] = useState("");
  const [currencies, setCurrencies] = useState<CurrencyDto[]>([]);
  const [newCurrencyCode, setNewCurrencyCode] = useState("");
  const [newCurrencyName, setNewCurrencyName] = useState("");
  const [newCurrencySymbol, setNewCurrencySymbol] = useState("");

  useEffect(() => {
    const u = localStorage.getItem("superUser") ?? "";
    const l = (localStorage.getItem("superLang") ?? "ru") as Lang;
    if (u) {
      setUsername(u);
      setAuthReady(true);
    }
    if (l === "ru" || l === "ro" || l === "en") {
      setLang(l);
    }
  }, []);

  useEffect(() => {
    localStorage.setItem("superLang", lang);
  }, [lang]);

  async function api(path: string, init?: RequestInit) {
    const res = await fetch(`${API_BASE}${path}`, {
      ...init,
      credentials: "include",
      headers: {
        "Content-Type": "application/json",
        ...(init?.headers ?? {}),
      },
    });
    if (!res.ok) {
      if (res.status === 401 || res.status === 403) {
        localStorage.removeItem("superUser");
        setAuthReady(false);
        setSessionExpired(true);
        if (!redirectingRef.current && typeof window !== "undefined") {
          redirectingRef.current = true;
          window.location.href = "/superadmin";
        }
      }
      const body = await res.json().catch(() => ({}));
      throw new Error(body?.message ?? `${t(lang, "requestFailed")} (${res.status})`);
    }
    return res;
  }

  const waiterPalette = ["#FF6B6B", "#4ECDC4", "#FFD166", "#6C5CE7", "#00B894", "#FD79A8", "#0984E3"];
  const waiterColor = (id?: number | null) => {
    if (!id) return "#9aa0a6";
    return waiterPalette[id % waiterPalette.length];
  };

  const clamp = (v: number, min: number, max: number) => Math.min(Math.max(v, min), max);
  const snap = useCallback((v: number, step = 2) => (snapEnabled ? Math.round(v / step) * step : v), [snapEnabled]);
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
  }, [planZoom, snap]);

  async function login() {
    setError(null);
    setSessionExpired(false);
    try {
      await api("/api/auth/login", {
        method: "POST",
        body: JSON.stringify({ username, password }),
      });
      localStorage.setItem("superUser", username);
      setAuthReady(true);
      loadTenants();
      loadCurrencies();
    } catch (e: any) {
      setError(e?.message ?? "Auth error");
    }
  }

  async function logout() {
    try {
      await api("/api/auth/logout", { method: "POST" });
    } finally {
      localStorage.removeItem("superUser");
      setUsername("");
      setPassword("");
      setAuthReady(false);
      setError(null);
      setSessionExpired(false);
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
      setError(e?.message ?? t(lang, "loadError"));
    }
  }

  async function loadCurrencies() {
    if (!authReady) return;
    try {
      const res = await api("/api/admin/currencies?includeInactive=true");
      setCurrencies(await res.json());
    } catch (_) {
      setCurrencies([]);
    }
  }

  useEffect(() => {
    loadTenants();
    loadCurrencies();
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

  async function loadBranchSettings() {
    if (!branchId) {
      setBranchSettings(null);
      return;
    }
    try {
      const res = await api(`/api/admin/branch-settings?branchId=${branchId}`);
      const body = await res.json();
      setBranchSettings(body);
    } catch (_) {
      setBranchSettings(null);
    }
  }

  async function saveBranchSettings() {
    if (!branchId || !branchSettings) return;
    await api(`/api/admin/branch-settings?branchId=${branchId}`, {
      method: "PUT",
      body: JSON.stringify({
        defaultLang: branchSettings.defaultLang ?? "ru",
      }),
    });
    loadBranchSettings();
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

  async function createCurrency() {
    if (!newCurrencyCode.trim() || !newCurrencyName.trim()) return;
    await api("/api/admin/currencies", {
      method: "POST",
      body: JSON.stringify({
        code: newCurrencyCode.trim().toUpperCase(),
        name: newCurrencyName.trim(),
        symbol: newCurrencySymbol.trim() || null,
        isActive: true,
      }),
    });
    setNewCurrencyCode("");
    setNewCurrencyName("");
    setNewCurrencySymbol("");
    loadCurrencies();
  }

  async function toggleCurrency(c: CurrencyDto) {
    await api(`/api/admin/currencies/${c.code}`, {
      method: "PATCH",
      body: JSON.stringify({ isActive: !c.isActive }),
    });
    loadCurrencies();
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
    loadTables();
  }

  function saveTemplate() {
    if (!hallPlanId) return;
    const name = prompt(t(lang, "templateNamePrompt"), hallPlans.find((p) => p.id === hallPlanId)?.name ?? t(lang, "templateDefaultName"));
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
    const next = [...planTemplates.filter((t) => t.name !== name), { name, payload }];
    setPlanTemplates(next);
    localStorage.setItem(`vw_plan_templates_${hallId}`, JSON.stringify(next));
  }

  async function applyTemplate(t: { name: string; payload: any }) {
    if (!hallId) return;
    const res = await api(`/api/admin/halls/${hallId}/plans/import`, {
      method: "POST",
      body: JSON.stringify({ ...t.payload, applyLayouts: true }),
    });
    const plan = await res.json();
    setHallPlanId(plan.id);
    loadTables();
  }

  function removeTemplate(name: string) {
    const next = planTemplates.filter((t) => t.name !== name);
    setPlanTemplates(next);
    if (hallId) localStorage.setItem(`vw_plan_templates_${hallId}`, JSON.stringify(next));
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
    if (branchId) {
      loadTables();
      loadBranchSettings();
    } else {
      setBranchSettings(null);
    }
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
    if (!hallId) return;
    try {
      const raw = localStorage.getItem(`vw_plan_templates_${hallId}`);
      if (raw) setPlanTemplates(JSON.parse(raw));
      else setPlanTemplates([]);
    } catch (_) {
      setPlanTemplates([]);
    }
  }, [hallId]);

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
    const res = await api(`/api/super/stats/summary.csv?${qs.toString()}`);
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
    const res = await api(`/api/super/stats/branches.csv?${qs.toString()}`);
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
    const ageVal = editStaffAge.trim() ? Number(editStaffAge.trim()) : null;
    if (ageVal != null && (Number.isNaN(ageVal) || ageVal < 0 || ageVal > 120)) {
      setError(t(lang, "invalidAge"));
      return;
    }
    await api(`/api/super/staff/${id}`, {
      method: "PATCH",
      body: JSON.stringify({
        role: editStaffRole,
        isActive: editStaffActive,
        firstName: editStaffFirstName,
        lastName: editStaffLastName,
        age: ageVal,
        gender: editStaffGender,
        photoUrl: editStaffPhotoUrl,
      }),
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
        <h1>{t(lang, "loginTitle")}</h1>
        {error && <div style={{ color: "#b11e46" }}>{error}</div>}
        <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
          <div style={{ display: "flex", gap: 8 }}>
            <button onClick={() => setLang("ru")}>RU</button>
            <button onClick={() => setLang("ro")}>RO</button>
            <button onClick={() => setLang("en")}>EN</button>
          </div>
          <input value={username} onChange={(e) => setUsername(e.target.value)} placeholder={t(lang, "username")} />
          <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} placeholder={t(lang, "password")} />
          <button onClick={login} style={{ padding: "10px 14px" }}>{t(lang, "login")}</button>
        </div>
      </main>
    );
  }

  return (
    <main style={{ padding: 16, maxWidth: 900, margin: "0 auto", fontFamily: "system-ui, -apple-system, Segoe UI, Roboto" }}>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
        <h1 style={{ margin: 0 }}>{t(lang, "superAdmin")}</h1>
        <div style={{ display: "flex", gap: 8, alignItems: "center" }}>
          <button onClick={() => setLang("ru")}>RU</button>
          <button onClick={() => setLang("ro")}>RO</button>
          <button onClick={() => setLang("en")}>EN</button>
          <button onClick={() => { loadTenants(); loadCurrencies(); }}>{t(lang, "refresh")}</button>
          <button onClick={logout}>{t(lang, "logout")}</button>
        </div>
      </div>
      {sessionExpired && <div style={{ color: "#b11e46", marginTop: 8 }}>{t(lang, "sessionExpired")}</div>}
      {error && <div style={{ color: "#b11e46", marginTop: 8 }}>{error}</div>}

      <section style={{ marginTop: 24 }}>
        <h2>{t(lang, "tenants")}</h2>
        <div style={{ display: "flex", gap: 8, flexWrap: "wrap", alignItems: "center" }}>
          <select value={tenantStatusFilter} onChange={(e) => setTenantStatusFilter(e.target.value as any)}>
            <option value="">{t(lang, "allStatuses")}</option>
            <option value="ACTIVE">{t(lang, "active")}</option>
            <option value="INACTIVE">{t(lang, "inactive")}</option>
          </select>
          <select value={tenantId} onChange={(e) => setTenantId(e.target.value ? Number(e.target.value) : "")}>
            <option value="">{t(lang, "selectTenant")}</option>
            {tenants.map((t) => (
              <option key={t.id} value={t.id}>{t.name} {t.isActive ? "" : translate(lang, "inactiveSuffix")}</option>
            ))}
          </select>
          <input placeholder={t(lang, "newTenantName")} value={newTenantName} onChange={(e) => setNewTenantName(e.target.value)} />
          <button onClick={createTenant}>{t(lang, "createTenant")}</button>
        </div>
        <div style={{ marginTop: 10 }}>
          {tenants.map((t) => (
            <div key={t.id} style={{ display: "flex", gap: 8, alignItems: "center", padding: "4px 0" }}>
              <strong>{t.name}</strong>
              <span>{t.isActive ? translate(lang, "active") : translate(lang, "inactive")}</span>
              <button onClick={() => toggleTenant(t)}>{t.isActive ? translate(lang, "disable") : translate(lang, "enable")}</button>
              <button onClick={() => deleteTenant(t.id)}>{translate(lang, "delete")}</button>
            </div>
          ))}
        </div>
      </section>

      <section style={{ marginTop: 24 }}>
        <h2>{t(lang, "branches")}</h2>
        <div style={{ display: "flex", gap: 8, flexWrap: "wrap", alignItems: "center" }}>
          <select value={branchStatusFilter} onChange={(e) => setBranchStatusFilter(e.target.value as any)}>
            <option value="">{t(lang, "allStatuses")}</option>
            <option value="ACTIVE">{t(lang, "active")}</option>
            <option value="INACTIVE">{t(lang, "inactive")}</option>
          </select>
          <select value={tenantId} onChange={(e) => setTenantId(e.target.value ? Number(e.target.value) : "")}>
            <option value="">{t(lang, "tenant")}</option>
            {tenants.map((t) => (
              <option key={t.id} value={t.id}>{t.name}</option>
            ))}
          </select>
          <input placeholder={t(lang, "newBranchName")} value={newBranchName} onChange={(e) => setNewBranchName(e.target.value)} />
          <button onClick={createBranch} disabled={!tenantId}>{t(lang, "createBranch")}</button>
        </div>
        <div style={{ marginTop: 10 }}>
          {branches.filter((b) => !tenantId || b.tenantId === tenantId).map((b) => (
            <div key={b.id} style={{ display: "flex", gap: 8, alignItems: "center", padding: "4px 0" }}>
              <strong>{b.name}</strong>
              <span>{t(lang, "tenant")} #{b.tenantId}</span>
              <span>{b.isActive ? t(lang, "active") : t(lang, "inactive")}</span>
              <button onClick={() => toggleBranch(b)}>{b.isActive ? t(lang, "disable") : t(lang, "enable")}</button>
              <button onClick={() => deleteBranch(b.id)}>{t(lang, "delete")}</button>
            </div>
          ))}
        </div>
      </section>

      <section style={{ marginTop: 24 }}>
        <h2>{t(lang, "currencies")}</h2>
        <div style={{ display: "flex", gap: 8, flexWrap: "wrap", alignItems: "center" }}>
          <input placeholder={`${t(lang, "code")} (EUR)`} value={newCurrencyCode} onChange={(e) => setNewCurrencyCode(e.target.value)} />
          <input placeholder={t(lang, "name")} value={newCurrencyName} onChange={(e) => setNewCurrencyName(e.target.value)} />
          <input placeholder={t(lang, "symbol")} value={newCurrencySymbol} onChange={(e) => setNewCurrencySymbol(e.target.value)} />
          <button onClick={createCurrency}>{t(lang, "addCurrency")}</button>
        </div>
        <div style={{ marginTop: 10 }}>
          {currencies.map((c) => (
            <div key={c.code} style={{ display: "flex", gap: 8, alignItems: "center", padding: "4px 0" }}>
              <strong>{c.code}</strong>
              <span>{c.name}</span>
              <span>{c.symbol || "-"}</span>
              <span>{c.isActive ? t(lang, "active") : t(lang, "inactive")}</span>
              <button onClick={() => toggleCurrency(c)}>{c.isActive ? t(lang, "disable") : t(lang, "enable")}</button>
            </div>
          ))}
          {currencies.length === 0 && <div style={{ color: "#666" }}>{t(lang, "noCurrencies")}</div>}
        </div>
      </section>

      <section style={{ marginTop: 24 }}>
        <h2>{t(lang, "branchSettings")}</h2>
        <div style={{ display: "flex", gap: 8, flexWrap: "wrap", alignItems: "center" }}>
          <select value={branchId} onChange={(e) => setBranchId(e.target.value ? Number(e.target.value) : "")}>
            <option value="">{t(lang, "selectBranch")}</option>
            {branches.map((b) => (
              <option key={b.id} value={b.id}>{b.name}</option>
            ))}
          </select>
          <button onClick={loadBranchSettings} disabled={!branchId}>{t(lang, "load")}</button>
        </div>
        {branchSettings && (
          <div style={{ marginTop: 10, display: "flex", gap: 8, flexWrap: "wrap", alignItems: "center" }}>
            <label>
              {t(lang, "defaultLanguage")}
              <select
                value={branchSettings.defaultLang ?? "ru"}
                onChange={(e) => setBranchSettings({ ...branchSettings, defaultLang: e.target.value })}
              >
                <option value="ru">{t(lang, "langRu")}</option>
                <option value="ro">{t(lang, "langRo")}</option>
                <option value="en">{t(lang, "langEn")}</option>
              </select>
            </label>
            <button onClick={saveBranchSettings}>{t(lang, "save")}</button>
          </div>
        )}
      </section>

      <section style={{ marginTop: 24 }}>
        <h2>{t(lang, "staffGlobal")}</h2>
        <div style={{ display: "flex", gap: 8, flexWrap: "wrap", alignItems: "center" }}>
          <select value={branchId} onChange={(e) => setBranchId(e.target.value ? Number(e.target.value) : "")}>
            <option value="">{t(lang, "selectBranch")}</option>
            {branches.map((b) => (
              <option key={b.id} value={b.id}>{b.name}</option>
            ))}
          </select>
          <button onClick={loadStaff} disabled={!branchId}>{t(lang, "loadStaff")}</button>
        </div>
        <div style={{ marginTop: 10, display: "flex", gap: 8, flexWrap: "wrap" }}>
          <input placeholder={t(lang, "username")} value={newStaffUser} onChange={(e) => setNewStaffUser(e.target.value)} />
          <input placeholder={t(lang, "password")} value={newStaffPass} onChange={(e) => setNewStaffPass(e.target.value)} />
          <select value={newStaffRole} onChange={(e) => setNewStaffRole(e.target.value)}>
            <option value="WAITER">WAITER</option>
            <option value="KITCHEN">KITCHEN</option>
            <option value="ADMIN">ADMIN</option>
          </select>
          <button onClick={createStaff} disabled={!branchId}>{t(lang, "createStaff")}</button>
        </div>
        <div style={{ marginTop: 10 }}>
          {staff.map((s) => (
            <div key={s.id} style={{ display: "flex", gap: 8, alignItems: "center", padding: "4px 0" }}>
              <strong>{s.username}</strong>
              <span>{s.role}</span>
              <span>{s.isActive ? t(lang, "active") : t(lang, "inactive")}</span>
              <button onClick={() => {
                setEditingStaffId(s.id);
                setEditStaffRole(s.role);
                setEditStaffActive(s.isActive);
                setEditStaffFirstName(s.firstName ?? "");
                setEditStaffLastName(s.lastName ?? "");
                setEditStaffAge(s.age != null ? String(s.age) : "");
                setEditStaffGender(s.gender ?? "");
                setEditStaffPhotoUrl(s.photoUrl ?? "");
              }}>{t(lang, "edit")}</button>
              <button onClick={() => resetStaffPassword(s.id)}>{t(lang, "resetPassword")}</button>
              <button onClick={() => deleteStaff(s.id)}>{t(lang, "delete")}</button>
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
              <label><input type="checkbox" checked={editStaffActive} onChange={(e) => setEditStaffActive(e.target.checked)} /> {t(lang, "active")}</label>
              <input placeholder={t(lang, "firstName")} value={editStaffFirstName} onChange={(e) => setEditStaffFirstName(e.target.value)} />
              <input placeholder={t(lang, "lastName")} value={editStaffLastName} onChange={(e) => setEditStaffLastName(e.target.value)} />
              <input placeholder={t(lang, "age")} value={editStaffAge} onChange={(e) => setEditStaffAge(e.target.value)} style={{ width: 90 }} />
              <select value={editStaffGender} onChange={(e) => setEditStaffGender(e.target.value)}>
                <option value="">{t(lang, "gender")}</option>
                <option value="male">{t(lang, "genderMale")}</option>
                <option value="female">{t(lang, "genderFemale")}</option>
                <option value="other">{t(lang, "genderOther")}</option>
              </select>
              <input placeholder={t(lang, "photoUrl")} value={editStaffPhotoUrl} onChange={(e) => setEditStaffPhotoUrl(e.target.value)} style={{ minWidth: 220 }} />
              <button onClick={() => updateStaff(editingStaffId)}>{t(lang, "save")}</button>
              <button onClick={() => setEditingStaffId(null)}>{t(lang, "cancel")}</button>
            </div>
          </div>
        )}
      </section>

      <section style={{ marginTop: 24 }}>
        <h2>{t(lang, "floorPlanBranch")}</h2>
        <div style={{ display: "flex", gap: 8, flexWrap: "wrap", alignItems: "center" }}>
          <select value={branchId} onChange={(e) => setBranchId(e.target.value ? Number(e.target.value) : "")}>
            <option value="">{t(lang, "selectBranch")}</option>
            {branches.map((b) => (
              <option key={b.id} value={b.id}>{b.name}</option>
            ))}
          </select>
          <label style={{ display: "flex", alignItems: "center", gap: 6 }}>
            <input type="checkbox" checked={planEditMode} onChange={(e) => setPlanEditMode(e.target.checked)} />
            {t(lang, "editMode")}
          </label>
          <label style={{ display: "flex", alignItems: "center", gap: 6 }}>
            <input type="checkbox" checked={snapEnabled} onChange={(e) => setSnapEnabled(e.target.checked)} />
            {t(lang, "snapToGrid")}
          </label>
          <label style={{ display: "flex", alignItems: "center", gap: 6 }}>
            <input type="checkbox" checked={planPreview} onChange={(e) => setPlanPreview(e.target.checked)} />
            {t(lang, "preview")}
          </label>
          <label style={{ display: "flex", alignItems: "center", gap: 6 }}>
            <input type="checkbox" checked={panMode} onChange={(e) => setPanMode(e.target.checked)} />
            {t(lang, "panMode")}
          </label>
          <button onClick={fitPlanToScreen} disabled={!branchId}>{t(lang, "fitToScreen")}</button>
          <button onClick={() => setPlanZoom(1)} disabled={!branchId}>{t(lang, "resetZoom")}</button>
          <button onClick={() => setPlanPan({ x: 0, y: 0 })} disabled={!branchId}>{t(lang, "resetPan")}</button>
          <button onClick={() => setPlanZoom((z) => Math.max(0.3, Number((z - 0.1).toFixed(2))))} disabled={!branchId}>-</button>
          <button onClick={() => setPlanZoom((z) => Math.min(2, Number((z + 0.1).toFixed(2))))} disabled={!branchId}>+</button>
          <input
            type="range"
            min={0.3}
            max={2}
            step={0.05}
            value={planZoom}
            onChange={(e) => setPlanZoom(Number(e.target.value))}
            disabled={!branchId}
          />
          <span style={{ color: "#666" }}>{t(lang, "zoom")}: {Math.round(planZoom * 100)}%</span>
          <select value={hallId} onChange={(e) => setHallId(e.target.value ? Number(e.target.value) : "")} disabled={!branchId}>
            <option value="">{t(lang, "planSelectHall")}</option>
            {halls.map((h) => (
              <option key={h.id} value={h.id}>{h.name}</option>
            ))}
          </select>
          <select value={hallPlanId} onChange={(e) => setHallPlanId(e.target.value ? Number(e.target.value) : "")} disabled={!hallId}>
            <option value="">{t(lang, "planDefault")}</option>
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
            {t(lang, "setActivePlan")}
          </button>
          <button
            onClick={async () => {
              if (!hallPlanId) return;
              await api(`/api/admin/hall-plans/${hallPlanId}/duplicate`, { method: "POST", body: JSON.stringify({}) });
              loadTables();
            }}
            disabled={!hallPlanId}
          >
            {t(lang, "duplicatePlan")}
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
            {t(lang, "deletePlan")}
          </button>
          <button onClick={loadTables} disabled={!branchId}>{t(lang, "loadTables")}</button>
          <button onClick={autoLayoutTables} disabled={!branchId}>{t(lang, "autoLayout")}</button>
          <button onClick={saveTableLayout} disabled={!branchId || !hallId}>{t(lang, "saveLayout")}</button>
          <button onClick={exportPlanJson} disabled={!hallPlanId}>{t(lang, "exportJson")}</button>
          <label style={{ display: "inline-flex", alignItems: "center", gap: 6 }}>
            <span style={{ fontSize: 12, color: "#666" }}>{t(lang, "importJson")}</span>
            <input
              type="file"
              accept="application/json"
              onChange={(e) => {
                const f = e.target.files?.[0];
                if (f) importPlanJson(f, true);
                e.currentTarget.value = "";
              }}
            />
          </label>
        </div>
        <div style={{ display: "flex", gap: 8, flexWrap: "wrap", alignItems: "center", marginTop: 8 }}>
          <input placeholder={t(lang, "newHallName")} value={newHallName} onChange={(e) => setNewHallName(e.target.value)} />
          <input type="number" placeholder={t(lang, "sort")} value={newHallSort} onChange={(e) => setNewHallSort(Number(e.target.value))} />
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
            {t(lang, "addHall")}
          </button>
          <input placeholder={t(lang, "newPlanName")} value={newPlanName} onChange={(e) => setNewPlanName(e.target.value)} />
          <input type="number" placeholder={t(lang, "planSort")} value={newPlanSort} onChange={(e) => setNewPlanSort(Number(e.target.value))} />
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
            {t(lang, "addPlan")}
          </button>
        </div>
        {hallId && (
          <div style={{ display: "flex", gap: 8, flexWrap: "wrap", alignItems: "center", marginTop: 8 }}>
            <span style={{ fontSize: 12, color: "#666" }}>{t(lang, "quickSwitch")}:</span>
            {[
              { key: "dayPlan", name: "Day" },
              { key: "eveningPlan", name: "Evening" },
              { key: "banquetPlan", name: "Banquet" },
            ].map(({ key, name }) => (
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
                  if (!plan) return;
                  await api(`/api/admin/halls/${hallId}`, { method: "PATCH", body: JSON.stringify({ activePlanId: plan.id }) });
                  setHallPlanId(plan.id);
                  loadTables();
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
                {t(lang, key)}
              </button>
            ))}
            {hallPlans.length > 0 &&
              hallPlans.map((p) => (
                <button
                  key={p.id}
                  onClick={async () => {
                    await api(`/api/admin/halls/${hallId}`, { method: "PATCH", body: JSON.stringify({ activePlanId: p.id }) });
                    setHallPlanId(p.id);
                    loadTables();
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
            <span style={{ fontSize: 12, color: "#666" }}>{t(lang, "templates")}:</span>
            <button onClick={saveTemplate} disabled={!hallPlanId}>{t(lang, "saveCurrent")}</button>
            {planTemplates.map((t) => (
              <span key={t.name} style={{ display: "inline-flex", gap: 6, alignItems: "center" }}>
                <button onClick={() => applyTemplate(t)}>{t.name}</button>
                <button onClick={() => removeTemplate(t.name)} style={{ color: "#b00" }}>×</button>
              </span>
            ))}
            {planTemplates.length === 0 && <span style={{ fontSize: 12, color: "#999" }}>{t(lang, "noTemplates")}</span>}
          </div>
        )}
        <div style={{ display: "flex", gap: 10, flexWrap: "wrap", alignItems: "center", marginTop: 8 }}>
          <span style={{ fontSize: 12, color: "#666" }}>{t(lang, "legend")}:</span>
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
            <span style={{ fontSize: 12, color: "#999" }}>{t(lang, "noWaiters")}</span>
          )}
        </div>
        <div style={{ display: "flex", gap: 8, flexWrap: "wrap", alignItems: "center", marginTop: 8 }}>
          <label>
            {t(lang, "backgroundUrl")}
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
                        cursor: isInteractive ? "grab" : "default",
                        userSelect: "none",
                      }}
                    >
                      <div style={{ fontWeight: 700, fontSize: 16 }}>#{t.number}</div>
                      <div style={{ fontSize: 12, color }}>
                        {t.assignedWaiterId ? `${translate(lang, "waiterLabel")} #${t.assignedWaiterId}` : translate(lang, "unassigned")}
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
            <h3 style={{ marginTop: 0 }}>{t(lang, "tableSettings")}</h3>
            {selectedTable ? (
              <div style={{ display: "flex", flexDirection: "column", gap: 10 }}>
                <div><strong>{t(lang, "tableSelected")}{selectedTable.number}</strong></div>
                <label>
                  {t(lang, "shape")}
                  <select
                    value={selectedTable.layoutShape ?? "ROUND"}
                    onChange={(e) => updateSelectedTable({ layoutShape: e.target.value })}
                  >
                    <option value="ROUND">{t(lang, "shapeRound")}</option>
                    <option value="RECT">{t(lang, "shapeRect")}</option>
                  </select>
                </label>
                <label>
                  {t(lang, "widthPercent")}
                  <input
                    type="number"
                    min={4}
                    max={30}
                    value={selectedTable.layoutW ?? 10}
                    onChange={(e) => updateSelectedTable({ layoutW: Number(e.target.value) })}
                  />
                </label>
                <label>
                  {t(lang, "heightPercent")}
                  <input
                    type="number"
                    min={4}
                    max={30}
                    value={selectedTable.layoutH ?? 10}
                    onChange={(e) => updateSelectedTable({ layoutH: Number(e.target.value) })}
                  />
                </label>
                <label>
                  {t(lang, "rotationDeg")}
                  <input
                    type="number"
                    min={0}
                    max={360}
                    value={selectedTable.layoutRotation ?? 0}
                    onChange={(e) => updateSelectedTable({ layoutRotation: Number(e.target.value) })}
                  />
                </label>
                <label>
                  {t(lang, "zone")}
                  <input
                    placeholder={t(lang, "zonePlaceholder")}
                    value={selectedTable.layoutZone ?? ""}
                    onChange={(e) => updateSelectedTable({ layoutZone: e.target.value })}
                  />
                </label>
                <div style={{ display: "flex", gap: 8 }}>
                  <button onClick={saveTableLayout}>{t(lang, "save")}</button>
                  <button onClick={() => updateSelectedTable({ layoutShape: "ROUND", layoutW: 10, layoutH: 10, layoutRotation: 0 })}>
                    {t(lang, "resetShape")}
                  </button>
                </div>
              </div>
            ) : (
              <div style={{ color: "#666" }}>{t(lang, "clickTableToEdit")}</div>
            )}
            <div style={{ marginTop: 16, borderTop: "1px solid #eee", paddingTop: 12 }}>
              <h4 style={{ margin: 0 }}>{t(lang, "zones")}</h4>
              {planZones.map((z, zi) => (
                <div key={z.id} style={{ display: "grid", gridTemplateColumns: "1fr 60px 60px", gap: 6, marginTop: 8 }}>
                  <input
                    placeholder={t(lang, "zoneName")}
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
                  <button onClick={() => setPlanZones((prev) => prev.filter((_, i) => i !== zi))}>{t(lang, "delete")}</button>
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
                    { id: String(Date.now()), name: t(lang, "zone"), x: 10, y: 10, w: 30, h: 20, color: "#6C5CE7" },
                  ])
                }
              >
                {t(lang, "addZone")}
              </button>
            </div>
          </div>
        </div>
      </section>

      <section style={{ marginTop: 24 }}>
        <h2>{t(lang, "stats")}</h2>
        <div style={{ display: "flex", gap: 8, flexWrap: "wrap", alignItems: "center" }}>
          <label>{t(lang, "fromDate")} <input type="date" value={statsFrom} onChange={(e) => setStatsFrom(e.target.value)} /></label>
          <label>{t(lang, "toDate")} <input type="date" value={statsTo} onChange={(e) => setStatsTo(e.target.value)} /></label>
          <button onClick={loadStats} disabled={!tenantId}>{t(lang, "load")}</button>
          <button onClick={downloadSummaryCsv} disabled={!tenantId}>{t(lang, "summaryCsv")}</button>
          <button onClick={downloadBranchesCsv} disabled={!tenantId}>{t(lang, "branchesCsv")}</button>
        </div>
        {stats && (
          <div style={{ marginTop: 10, border: "1px solid #eee", borderRadius: 8, padding: 10 }}>
            <div>{t(lang, "period")}: {stats.from} → {stats.to}</div>
            <div>{t(lang, "orders")}: {stats.ordersCount}</div>
            <div>{t(lang, "waiterCalls")}: {stats.callsCount}</div>
            <div>{t(lang, "paidBills")}: {stats.paidBillsCount}</div>
            <div>{t(lang, "gross")}: {money(stats.grossCents)}</div>
            <div>{t(lang, "tips")}: {money(stats.tipsCents)}</div>
            <div>{t(lang, "activeTables")}: {stats.activeTablesCount}</div>
          </div>
        )}
        {branchStats.length > 0 && (
          <div style={{ marginTop: 12 }}>
            <h3>{t(lang, "byBranch")}</h3>
            <table style={{ width: "100%", borderCollapse: "collapse" }}>
              <thead>
                <tr>
                  <th style={{ textAlign: "left", borderBottom: "1px solid #ddd", padding: "6px 4px" }}>{t(lang, "branch")}</th>
                  <th style={{ textAlign: "right", borderBottom: "1px solid #ddd", padding: "6px 4px" }}>{t(lang, "orders")}</th>
                  <th style={{ textAlign: "right", borderBottom: "1px solid #ddd", padding: "6px 4px" }}>{t(lang, "calls")}</th>
                  <th style={{ textAlign: "right", borderBottom: "1px solid #ddd", padding: "6px 4px" }}>{t(lang, "paidBills")}</th>
                  <th style={{ textAlign: "right", borderBottom: "1px solid #ddd", padding: "6px 4px" }}>{t(lang, "gross")}</th>
                  <th style={{ textAlign: "right", borderBottom: "1px solid #ddd", padding: "6px 4px" }}>{t(lang, "tips")}</th>
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
        <h2>{t(lang, "rbacMatrix")}</h2>
        <table style={{ width: "100%", borderCollapse: "collapse" }}>
          <thead>
            <tr>
              <th style={{ textAlign: "left", borderBottom: "1px solid #ddd", padding: "6px 4px" }}>{t(lang, "roleLabel")}</th>
              <th style={{ textAlign: "left", borderBottom: "1px solid #ddd", padding: "6px 4px" }}>{t(lang, "accessLabel")}</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td style={{ padding: "6px 4px" }}>WAITER</td>
              <td style={{ padding: "6px 4px" }}>{t(lang, "waiterAccess")}</td>
            </tr>
            <tr>
              <td style={{ padding: "6px 4px" }}>KITCHEN</td>
              <td style={{ padding: "6px 4px" }}>{t(lang, "kitchenAccess")}</td>
            </tr>
            <tr>
              <td style={{ padding: "6px 4px" }}>ADMIN</td>
              <td style={{ padding: "6px 4px" }}>{t(lang, "adminAccess")}</td>
            </tr>
            <tr>
              <td style={{ padding: "6px 4px" }}>SUPER_ADMIN</td>
              <td style={{ padding: "6px 4px" }}>{t(lang, "superAdminAccess")}</td>
            </tr>
          </tbody>
        </table>
      </section>
    </main>
  );
}
