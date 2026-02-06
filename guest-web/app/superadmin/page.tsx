"use client";

import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import Image from "next/image";
import QRCode from "qrcode";
import GuestConsentLogs from "../components/GuestConsentLogs";

const API_BASE = process.env.NEXT_PUBLIC_API_BASE ?? "http://localhost:8080";

type Lang = "ru" | "ro" | "en";
const TIME_ZONE_OPTIONS = [
  "UTC",
  "Europe/Chisinau",
  "Europe/Kiev",
  "Europe/Bucharest",
  "Europe/Warsaw",
  "Europe/London",
  "Europe/Berlin",
  "Europe/Paris",
  "America/New_York",
  "America/Chicago",
  "America/Denver",
  "America/Los_Angeles",
  "Asia/Dubai",
  "Asia/Tbilisi",
  "Asia/Yerevan",
];
const dict: Record<string, Record<Lang, string>> = {
  loginTitle: { ru: "Вход Super Admin", ro: "Autentificare Super Admin", en: "Super Admin Login" },
  superAdmin: { ru: "Супер‑админ", ro: "Super Admin", en: "Super Admin" },
  username: { ru: "Логин", ro: "Utilizator", en: "Username" },
  password: { ru: "Пароль", ro: "Parolă", en: "Password" },
  totpCode: { ru: "Код 2FA", ro: "Cod 2FA", en: "2FA code" },
  login: { ru: "Войти", ro: "Intră", en: "Login" },
  logout: { ru: "Выйти", ro: "Ieși", en: "Logout" },
  sessionExpired: { ru: "Сессия истекла. Войдите снова.", ro: "Sesiunea a expirat. Autentificați‑vă din nou.", en: "Session expired. Please sign in again." },
  refresh: { ru: "Обновить", ro: "Reîmprospătează", en: "Refresh" },
  scope: { ru: "Контекст", ro: "Context", en: "Scope" },
  tenants: { ru: "Заведения (тенанты)", ro: "Tenanți", en: "Tenants" },
  restaurants: { ru: "Рестораны", ro: "Restaurante", en: "Restaurants" },
  branches: { ru: "Филиалы", ro: "Filiale", en: "Branches" },
  branchSettings: { ru: "Настройки филиала", ro: "Setări filială", en: "Branch settings" },
  defaultLanguage: { ru: "Язык по умолчанию (гость)", ro: "Limba implicită (oaspete)", en: "Default language (guest)" },
  timeZone: { ru: "Часовой пояс", ro: "Fus orar", en: "Time zone" },
  langRu: { ru: "Русский", ro: "Rusă", en: "Russian" },
  langRo: { ru: "Румынский", ro: "Română", en: "Romanian" },
  langEn: { ru: "Английский", ro: "Engleză", en: "English" },
  staffGlobal: { ru: "Персонал (глобально)", ro: "Personal (global)", en: "Staff (global)" },
  rolesPermissionsTitle: { ru: "Права и роли", ro: "Roluri și permisiuni", en: "Roles & permissions" },
  roleColumn: { ru: "Роль", ro: "Rol", en: "Role" },
  permissionsColumn: { ru: "Права по умолчанию", ro: "Permisiuni implicite", en: "Default permissions" },
  permissionsHelp: { ru: "Права можно переопределять для конкретного сотрудника.", ro: "Permisiunile pot fi suprascrise pentru un angajat.", en: "You can override permissions per staff user." },
  selectStaff: { ru: "Сотрудник", ro: "Angajat", en: "Staff user" },
  savePermissions: { ru: "Сохранить права", ro: "Salvează permisiuni", en: "Save permissions" },
  permissionsSaved: { ru: "Права обновлены", ro: "Permisiunile au fost actualizate", en: "Permissions updated" },
  permissionsOverride: { ru: "Переопределить права", ro: "Suprascrie permisiuni", en: "Override permissions" },
  permAdminAccess: { ru: "Доступ к админ‑панели", ro: "Acces admin", en: "Admin access" },
  permSuperadminAccess: { ru: "Доступ супер‑админа", ro: "Acces super‑admin", en: "Superadmin access" },
  permStaffView: { ru: "Просмотр персонала", ro: "Vizualizare personal", en: "Staff view" },
  permStaffManage: { ru: "Управление персоналом", ro: "Gestionare personal", en: "Staff manage" },
  permMenuView: { ru: "Просмотр меню", ro: "Vizualizare meniu", en: "Menu view" },
  permMenuManage: { ru: "Управление меню", ro: "Gestionare meniu", en: "Menu manage" },
  permReportsView: { ru: "Просмотр отчетов", ro: "Vizualizare rapoarte", en: "Reports view" },
  permAuditView: { ru: "Просмотр аудита", ro: "Vizualizare audit", en: "Audit view" },
  permSettingsManage: { ru: "Настройки", ro: "Setări", en: "Settings manage" },
  permPaymentsManage: { ru: "Оплаты", ro: "Plăți", en: "Payments manage" },
  permInventoryManage: { ru: "Склад", ro: "Inventar", en: "Inventory manage" },
  permLoyaltyManage: { ru: "Лояльность", ro: "Loialitate", en: "Loyalty manage" },
  permGuestFlagsManage: { ru: "Флаги гостей", ro: "Flaguri oaspeți", en: "Guest flags manage" },
  permMediaManage: { ru: "Медиа", ro: "Media", en: "Media manage" },
  permHallPlanManage: { ru: "Планы зала", ro: "Plan sală", en: "Hall plan manage" },
  deviceSessions: { ru: "Сессии устройств", ro: "Sesiuni dispozitive", en: "Device sessions" },
  totpTitle: { ru: "Двухфакторная защита", ro: "Autentificare cu doi factori", en: "Two‑factor auth" },
  totpStatusEnabled: { ru: "Включено", ro: "Activat", en: "Enabled" },
  totpStatusDisabled: { ru: "Выключено", ro: "Dezactivat", en: "Disabled" },
  totpSetup: { ru: "Создать секрет", ro: "Generează secret", en: "Generate secret" },
  totpEnable: { ru: "Включить 2FA", ro: "Activează 2FA", en: "Enable 2FA" },
  totpDisable: { ru: "Выключить 2FA", ro: "Dezactivează 2FA", en: "Disable 2FA" },
  totpSecret: { ru: "Секрет", ro: "Secret", en: "Secret" },
  totpHint: { ru: "Сканируйте QR/otpauth в приложении TOTP и введите код.", ro: "Scanează QR/otpauth în aplicația TOTP și introdu codul.", en: "Scan the QR/otpauth in a TOTP app and enter the code." },
  adminIpAllowlist: { ru: "IP‑allowlist для админов", ro: "Allowlist IP pentru admini", en: "Admin IP allowlist" },
  adminIpDenylist: { ru: "IP‑denylist для админов", ro: "Denylist IP pentru admini", en: "Admin IP denylist" },
  adminIpListHelp: { ru: "Формат: IP или CIDR, через запятую/пробел/новую строку", ro: "Format: IP sau CIDR, separat prin virgulă/spațiu/linie", en: "Format: IP or CIDR, separated by comma/space/newline" },
  adminIpAddCurrent: { ru: "Добавить мой IP", ro: "Adaugă IP‑ul meu", en: "Add my IP" },
  adminIpInvalid: { ru: "Неверный формат IP/CIDR", ro: "Format IP/CIDR invalid", en: "Invalid IP/CIDR format" },
  deviceSessionIncludeRevoked: { ru: "Показывать отозванные", ro: "Arată revocate", en: "Show revoked" },
  deviceSessionFilterStaff: { ru: "Сотрудник", ro: "Angajat", en: "Staff" },
  deviceSessionFilterBranch: { ru: "Филиал", ro: "Filială", en: "Branch" },
  deviceSessionRefresh: { ru: "Обновить", ro: "Reîmprospătează", en: "Refresh" },
  deviceSessionRevoke: { ru: "Отозвать", ro: "Revocă", en: "Revoke" },
  deviceSessionRevokeConfirm: { ru: "Отозвать сессию устройства?", ro: "Revocare sesiune dispozitiv?", en: "Revoke device session?" },
  deviceSessionRevokeFiltered: { ru: "Отозвать по фильтру", ro: "Revocă după filtru", en: "Revoke by filter" },
  deviceSessionRevokeFilteredConfirm: { ru: "Отозвать все сессии по текущему фильтру?", ro: "Revocă toate sesiunile după filtru?", en: "Revoke all sessions by current filter?" },
  deviceSessionRevokeDone: { ru: "Отозвано сессий", ro: "Sesiuni revocate", en: "Sessions revoked" },
  deviceSessionEmpty: { ru: "Сессий нет", ro: "Nu sunt sesiuni", en: "No sessions" },
  deviceSessionPlatform: { ru: "Платформа", ro: "Platformă", en: "Platform" },
  deviceSessionDevice: { ru: "Устройство", ro: "Dispozitiv", en: "Device" },
  deviceSessionToken: { ru: "Токен", ro: "Token", en: "Token" },
  deviceSessionLastSeen: { ru: "Последняя активность", ro: "Ultima activitate", en: "Last seen" },
  deviceSessionCreated: { ru: "Создано", ro: "Creat", en: "Created" },
  deviceSessionRevoked: { ru: "Отозвано", ro: "Revocat", en: "Revoked" },
  profileFilter: { ru: "Профиль", ro: "Profil", en: "Profile" },
  profileAny: { ru: "Любой", ro: "Oricare", en: "Any" },
  profileFilled: { ru: "Заполнен", ro: "Completat", en: "Filled" },
  profileEmpty: { ru: "Пустой", ro: "Gol", en: "Empty" },
  active: { ru: "Активен", ro: "Activ", en: "Active" },
  inactive: { ru: "Неактивен", ro: "Inactiv", en: "Inactive" },
  allStatuses: { ru: "Все статусы", ro: "Toate statusurile", en: "All statuses" },
  selectTenant: { ru: "Выберите тeнанта", ro: "Selectați tenantul", en: "Select tenant" },
  selectRestaurant: { ru: "Выберите ресторан", ro: "Selectați restaurantul", en: "Select restaurant" },
  selectBranch: { ru: "Выберите филиал", ro: "Selectați filiala", en: "Select branch" },
  newTenantName: { ru: "Имя нового тенанта", ro: "Nume tenant nou", en: "New tenant name" },
  createTenant: { ru: "Создать тенанта", ro: "Creează tenant", en: "Create tenant" },
  newRestaurantName: { ru: "Имя нового ресторана", ro: "Nume restaurant nou", en: "New restaurant name" },
  createRestaurant: { ru: "Создать ресторан", ro: "Creează restaurant", en: "Create restaurant" },
  newBranchName: { ru: "Имя нового филиала", ro: "Nume filială nouă", en: "New branch name" },
  createBranch: { ru: "Создать филиал", ro: "Creează filială", en: "Create branch" },
  enable: { ru: "Включить", ro: "Activează", en: "Enable" },
  disable: { ru: "Выключить", ro: "Dezactivează", en: "Disable" },
  delete: { ru: "Удалить", ro: "Șterge", en: "Delete" },
  tenant: { ru: "Тенант", ro: "Tenant", en: "Tenant" },
  restaurant: { ru: "Ресторан", ro: "Restaurant", en: "Restaurant" },
  loadStaff: { ru: "Загрузить персонал", ro: "Încarcă personal", en: "Load staff" },
  firstName: { ru: "Имя", ro: "Prenume", en: "First name" },
  lastName: { ru: "Фамилия", ro: "Nume", en: "Last name" },
  age: { ru: "Возраст", ro: "Vârstă", en: "Age" },
  gender: { ru: "Пол", ro: "Gen", en: "Gender" },
  genderMale: { ru: "Мужской", ro: "Masculin", en: "Male" },
  genderFemale: { ru: "Женский", ro: "Feminin", en: "Female" },
  genderOther: { ru: "Другое", ro: "Altul", en: "Other" },
  photoUrl: { ru: "Фото", ro: "Foto", en: "Photo" },
  photoUpload: { ru: "Загрузить фото", ro: "Încarcă foto", en: "Upload photo" },
  uploading: { ru: "Загрузка...", ro: "Se încarcă...", en: "Uploading..." },
  loading: { ru: "Загрузка...", ro: "Se încarcă...", en: "Loading..." },
  rating: { ru: "Рейтинг (0–5)", ro: "Rating (0–5)", en: "Rating (0–5)" },
  recommended: { ru: "Рекомендуемый", ro: "Recomandat", en: "Recommended" },
  experienceYears: { ru: "Стаж (лет)", ro: "Experiență (ani)", en: "Experience (years)" },
  favoriteItems: { ru: "Любимые блюда (через запятую)", ro: "Feluri preferate (separate prin virgulă)", en: "Favorite items (comma-separated)" },
  invalidAge: { ru: "Неверный возраст (0–120)", ro: "Vârstă invalidă (0–120)", en: "Invalid age (0–120)" },
  invalidRating: { ru: "Неверный рейтинг (0–5)", ro: "Rating invalid (0–5)", en: "Invalid rating (0–5)" },
  invalidExperience: { ru: "Неверный стаж (0–80)", ro: "Experiență invalidă (0–80)", en: "Invalid experience (0–80)" },
  createStaff: { ru: "Создать сотрудника", ro: "Creează personal", en: "Create staff" },
  role: { ru: "Роль", ro: "Rol", en: "Role" },
  roleWaiter: { ru: "Официант", ro: "Chelner", en: "Waiter" },
  roleHost: { ru: "Хост", ro: "Host", en: "Host" },
  roleKitchen: { ru: "Повар", ro: "Bucătar", en: "Kitchen" },
  roleBar: { ru: "Бармен", ro: "Barman", en: "Bar" },
  roleAdmin: { ru: "Администратор", ro: "Administrator", en: "Admin" },
  roleManager: { ru: "Менеджер", ro: "Manager", en: "Manager" },
  roleOwner: { ru: "Владелец ресторана", ro: "Proprietar restaurant", en: "Restaurant owner" },
  roleSuperAdmin: { ru: "Супер‑админ", ro: "Super‑admin", en: "Super admin" },
  roleCashier: { ru: "Кассир", ro: "Casier", en: "Cashier" },
  roleMarketer: { ru: "Маркетолог", ro: "Marketing", en: "Marketer" },
  roleAccountant: { ru: "Бухгалтер", ro: "Contabil", en: "Accountant" },
  roleSupport: { ru: "Техподдержка", ro: "Suport", en: "Support" },
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
  orderStatus: { ru: "Статус заказа", ro: "Status comandă", en: "Order status" },
  guestPhone: { ru: "Телефон гостя", ro: "Telefon oaspete", en: "Guest phone" },
  guestConsentTitle: { ru: "Согласия гостя", ro: "Consimțăminte oaspete", en: "Guest consents" },
  guestConsentEmpty: { ru: "Логов согласий нет", ro: "Nu există loguri", en: "No consent logs" },
  guestConsentType: { ru: "Тип", ro: "Tip", en: "Type" },
  guestConsentAccepted: { ru: "Принято", ro: "Acceptat", en: "Accepted" },
  guestConsentVersion: { ru: "Версия", ro: "Versiune", en: "Version" },
  guestConsentIp: { ru: "IP", ro: "IP", en: "IP" },
  guestConsentUa: { ru: "User‑Agent", ro: "User‑Agent", en: "User‑Agent" },
  guestConsentAt: { ru: "Дата", ro: "Data", en: "Date" },
  yes: { ru: "Да", ro: "Da", en: "Yes" },
  no: { ru: "Нет", ro: "Nu", en: "No" },
  guestFlagsTitle: { ru: "Флаги гостя", ro: "Flaguri oaspete", en: "Guest flags" },
  guestFlagVip: { ru: "VIP", ro: "VIP", en: "VIP" },
  guestFlagNoShow: { ru: "No‑show", ro: "No‑show", en: "No‑show" },
  guestFlagConflict: { ru: "Конфликтный", ro: "Conflictual", en: "Conflict" },
  guestFlagsSave: { ru: "Сохранить флаги", ro: "Salvează flaguri", en: "Save flags" },
  guestFlagsSaved: { ru: "Флаги сохранены", ro: "Flaguri salvate", en: "Flags saved" },
  guestFlagsHistory: { ru: "История изменений", ro: "Istoric modificări", en: "Change history" },
  guestFlagsBulkTitle: { ru: "Массовое применение", ro: "Aplicare în masă", en: "Bulk apply" },
  guestFlagsBulkPhones: { ru: "Телефоны (по одному в строке)", ro: "Telefoane (câte unul pe linie)", en: "Phones (one per line)" },
  guestFlagsBulkApply: { ru: "Применить", ro: "Aplică", en: "Apply" },
  shiftFrom: { ru: "Смена с", ro: "Schimb de la", en: "Shift from" },
  shiftTo: { ru: "Смена по", ro: "Schimb până la", en: "Shift to" },
  avgCheck: { ru: "Средний чек", ro: "Bon mediu", en: "Average check" },
  avgSla: { ru: "Средний SLA (мин.)", ro: "SLA mediu (min.)", en: "Average SLA (min.)" },
  topItems: { ru: "Топ‑позиции", ro: "Top produse", en: "Top items" },
  topWaiters: { ru: "Топ‑официанты", ro: "Top chelneri", en: "Top waiters" },
  qty: { ru: "Кол-во", ro: "Cantitate", en: "Qty" },
  slaMinutes: { ru: "SLA (мин.)", ro: "SLA (min.)", en: "SLA (min.)" },
  branchReviewsAvg: { ru: "Средний рейтинг", ro: "Rating mediu", en: "Average rating" },
  branchReviewsCount: { ru: "Кол-во отзывов", ro: "Număr recenzii", en: "Reviews count" },
  fromDate: { ru: "С", ro: "De la", en: "From" },
  toDate: { ru: "По", ro: "Până la", en: "To" },
  load: { ru: "Загрузить", ro: "Încarcă", en: "Load" },
  summaryCsv: { ru: "Сводка CSV", ro: "Rezumat CSV", en: "Summary CSV" },
  branchesCsv: { ru: "Филиалы CSV", ro: "Filiale CSV", en: "Branches CSV" },
  topItemsCsv: { ru: "Топ‑позиции CSV", ro: "Top produse CSV", en: "Top items CSV" },
  topWaitersCsv: { ru: "Топ‑официанты CSV", ro: "Top chelneri CSV", en: "Top waiters CSV" },
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
  logo: { ru: "Логотип", ro: "Logo", en: "Logo" },
  country: { ru: "Страна", ro: "Țară", en: "Country" },
  address: { ru: "Адрес", ro: "Adresă", en: "Address" },
  phone: { ru: "Телефон", ro: "Telefon", en: "Phone" },
  contactPerson: { ru: "Ответственное лицо", ro: "Persoană responsabilă", en: "Contact person" },
  editEntity: { ru: "Редактировать", ro: "Editează", en: "Edit" },
  cancelEdit: { ru: "Отменить", ro: "Anulează", en: "Cancel" },
  saveEntity: { ru: "Сохранить", ro: "Salvează", en: "Save" },
  onlinePayEnabled: { ru: "Онлайн‑оплата включена", ro: "Plata online activă", en: "Online payments enabled" },
  onlinePayProvider: { ru: "Провайдер онлайн‑оплаты", ro: "Furnizor plăți online", en: "Online payment provider" },
  onlinePayCurrency: { ru: "Валюта онлайн‑оплаты", ro: "Monedă plăți online", en: "Online payment currency" },
  onlinePayProviderHint: { ru: "Оплата только через провайдера", ro: "Plata doar prin furnizor", en: "Payment only via provider" },
  onlinePayRequestUrl: { ru: "Ссылка запроса к PSP", ro: "URL cerere PSP", en: "PSP request URL" },
  onlinePayCacertPath: { ru: "Путь к cacert.pem", ro: "Cale cacert.pem", en: "cacert.pem path" },
  onlinePayPcertPath: { ru: "Путь к pcert.pem", ro: "Cale pcert.pem", en: "pcert.pem path" },
  onlinePayPcertPassword: { ru: "Пароль pcert.pem", ro: "Parolă pcert.pem", en: "pcert.pem password" },
  onlinePayKeyPath: { ru: "Путь к key.pem", ro: "Cale key.pem", en: "key.pem path" },
  onlinePayRedirectUrl: { ru: "URL страницы оплаты", ro: "URL pagină plată", en: "Payment redirect URL" },
  onlinePayReturnUrl: { ru: "URL возврата после оплаты", ro: "URL întoarcere după plată", en: "Return URL after payment" },
  onlinePayRequestUrlHelp: { ru: "HTTPS URL запроса к платежной системе (PSP).", ro: "URL HTTPS pentru cererea către PSP.", en: "HTTPS URL for PSP request." },
  onlinePayCacertPathHelp: { ru: "Полный путь к cacert.pem на сервере.", ro: "Calea completă către cacert.pem pe server.", en: "Full path to cacert.pem on server." },
  onlinePayPcertPathHelp: { ru: "Полный путь к pcert.pem на сервере.", ro: "Calea completă către pcert.pem pe server.", en: "Full path to pcert.pem on server." },
  onlinePayPcertPasswordHelp: { ru: "Пароль для pcert.pem.", ro: "Parola pentru pcert.pem.", en: "Password for pcert.pem." },
  onlinePayKeyPathHelp: { ru: "Полный путь к key.pem на сервере.", ro: "Calea completă către key.pem pe server.", en: "Full path to key.pem on server." },
  onlinePayRedirectUrlHelp: { ru: "URL страницы оплаты провайдера.", ro: "URL-ul paginii de plată a providerului.", en: "Provider payment page URL." },
  onlinePayReturnUrlHelp: { ru: "URL возврата клиента после оплаты.", ro: "URL întoarcere client după plată.", en: "Customer return URL after payment." },
};

const t = (lang: Lang, key: string) => dict[key]?.[lang] ?? key;

const ORDER_STATUS_OPTIONS = ["NEW", "ACCEPTED", "IN_PROGRESS", "READY", "SERVED", "CLOSED", "CANCELLED"];

type Tenant = {
  id: number;
  name: string;
  logoUrl?: string | null;
  country?: string | null;
  address?: string | null;
  phone?: string | null;
  contactPerson?: string | null;
  isActive: boolean;
};
type Restaurant = {
  id: number;
  tenantId: number;
  name: string;
  logoUrl?: string | null;
  country?: string | null;
  address?: string | null;
  phone?: string | null;
  contactPerson?: string | null;
  isActive: boolean;
};
type Branch = {
  id: number;
  tenantId: number;
  restaurantId?: number | null;
  name: string;
  logoUrl?: string | null;
  country?: string | null;
  address?: string | null;
  phone?: string | null;
  contactPerson?: string | null;
  isActive: boolean;
};
type BranchSettings = {
  branchId: number;
  defaultLang?: string;
  timeZone?: string;
  onlinePayEnabled?: boolean;
  onlinePayProvider?: string | null;
  onlinePayCurrencyCode?: string | null;
  onlinePayRequestUrl?: string | null;
  onlinePayCacertPath?: string | null;
  onlinePayPcertPath?: string | null;
  onlinePayPcertPassword?: string | null;
  onlinePayKeyPath?: string | null;
  onlinePayRedirectUrl?: string | null;
  onlinePayReturnUrl?: string | null;
  adminIpAllowlist?: string;
  adminIpDenylist?: string;
};
type StaffUser = {
  id: number;
  branchId: number | null;
  hallId?: number | null;
  username: string;
  role: string;
  permissions?: string | null;
  isActive: boolean;
  firstName?: string | null;
  lastName?: string | null;
  age?: number | null;
  gender?: string | null;
  photoUrl?: string | null;
  rating?: number | null;
  recommended?: boolean | null;
  experienceYears?: number | null;
  favoriteItems?: string | null;
};

type DeviceSession = {
  id: number;
  staffUserId: number;
  username?: string | null;
  branchId?: number | null;
  platform?: string | null;
  deviceId?: string | null;
  deviceName?: string | null;
  tokenMasked?: string | null;
  createdAt?: string | null;
  lastSeenAt?: string | null;
  revokedAt?: string | null;
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
  avgCheckCents?: number;
  avgSlaMinutes?: number | null;
  avgBranchRating?: number;
  branchReviewsCount?: number;
};

type BranchSummaryRow = {
  branchId: number;
  branchName: string;
  restaurantId?: number | null;
  restaurantName?: string | null;
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

type WaiterMotivationRow = {
  staffUserId: number;
  username: string;
  ordersCount: number;
  tipsCents: number;
  avgSlaMinutes?: number | null;
};

type ConsentLog = {
  consentType: string;
  accepted: boolean;
  textVersion: string | null;
  ip: string | null;
  userAgent: string | null;
  createdAt: string | null;
};

function parseIpList(v?: string | null) {
  if (!v) return [];
  return v.split(/[,;\s]+/).map((s) => s.trim()).filter(Boolean);
}

function isIpBlocked(ip: string, allow?: string | null, deny?: string | null) {
  if (!ip) return false;
  const denyList = parseIpList(deny);
  for (const rule of denyList) {
    if (matchIp(ip, rule)) return true;
  }
  const allowList = parseIpList(allow);
  if (allowList.length === 0) return false;
  return !allowList.some((rule) => matchIp(ip, rule));
}

function matchIp(ip: string, rule: string) {
  if (!rule) return false;
  if (rule.includes("/")) return matchCidr(ip, rule);
  return ip.trim() === rule.trim();
}

function matchCidr(ip: string, rule: string) {
  const [net, bitsStr] = rule.split("/");
  const bits = Number(bitsStr);
  if (!Number.isFinite(bits)) return false;
  const ipVal = ipv4ToInt(ip);
  const netVal = ipv4ToInt(net);
  if (ipVal == null || netVal == null) return false;
  const mask = bits === 0 ? 0 : (~0 << (32 - bits)) >>> 0;
  return (ipVal & mask) === (netVal & mask);
}

function ipv4ToInt(ip: string) {
  const parts = ip.trim().split(".");
  if (parts.length !== 4) return null;
  let out = 0;
  for (const p of parts) {
    const n = Number(p);
    if (!Number.isInteger(n) || n < 0 || n > 255) return null;
    out = (out << 8) + n;
  }
  return out >>> 0;
}

function invalidIpTokens(v?: string | null) {
  const tokens = parseIpList(v);
  return tokens.filter((t) => !isValidIpRule(t));
}

function isValidIpRule(rule: string) {
  if (!rule) return false;
  if (rule.includes("/")) {
    const [net, bitsStr] = rule.split("/");
    if (!net || bitsStr == null) return false;
    const bits = Number(bitsStr);
    if (!Number.isInteger(bits) || bits < 0 || bits > 32) return false;
    return ipv4ToInt(net) != null;
  }
  return ipv4ToInt(rule) != null;
}

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
  const [totpLoginCode, setTotpLoginCode] = useState("");
  const [authReady, setAuthReady] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [sessionExpired, setSessionExpired] = useState(false);
  const [totpStatus, setTotpStatus] = useState<{ enabled: boolean; hasSecret: boolean } | null>(null);
  const [totpSecret, setTotpSecret] = useState<string | null>(null);
  const [totpOtpauth, setTotpOtpauth] = useState<string | null>(null);
  const [totpQrDataUrl, setTotpQrDataUrl] = useState<string | null>(null);
  const [totpManageCode, setTotpManageCode] = useState("");
  const [totpError, setTotpError] = useState<string | null>(null);
  const [currentIp, setCurrentIp] = useState<string>("");
  const redirectingRef = useRef(false);
  const [lang, setLang] = useState<Lang>("ru");
  const translate = t;

  const roleLabel = (role?: string | null) => {
    const r = (role ?? "").toUpperCase();
    if (r === "WAITER") return t(lang, "roleWaiter");
    if (r === "HOST") return t(lang, "roleHost");
    if (r === "KITCHEN") return t(lang, "roleKitchen");
    if (r === "BAR") return t(lang, "roleBar");
    if (r === "ADMIN") return t(lang, "roleAdmin");
    if (r === "MANAGER") return t(lang, "roleManager");
    if (r === "OWNER") return t(lang, "roleOwner");
    if (r === "SUPER_ADMIN") return t(lang, "roleSuperAdmin");
    if (r === "CASHIER") return t(lang, "roleCashier");
    if (r === "MARKETER") return t(lang, "roleMarketer");
    if (r === "ACCOUNTANT") return t(lang, "roleAccountant");
    if (r === "SUPPORT") return t(lang, "roleSupport");
    return role ?? "";
  };

  const restaurantLabel = (id?: number | null) => {
    if (!id) return "-";
    return restaurants.find((r) => r.id === id)?.name ?? `#${id}`;
  };

  const tenantLabel = (id?: number | null) => {
    if (!id) return "-";
    return tenants.find((t) => t.id === id)?.name ?? `#${id}`;
  };

  const restaurantOptionLabel = (r: Restaurant) => `${tenantLabel(r.tenantId)} / ${r.name}`;

  const isWaiterRole = (role?: string | null) => {
    const r = (role ?? "").toUpperCase();
    return r === "WAITER" || r === "HOST";
  };

  const permissionLabels: Record<string, string> = {
    ADMIN_ACCESS: "permAdminAccess",
    SUPERADMIN_ACCESS: "permSuperadminAccess",
    STAFF_VIEW: "permStaffView",
    STAFF_MANAGE: "permStaffManage",
    MENU_VIEW: "permMenuView",
    MENU_MANAGE: "permMenuManage",
    REPORTS_VIEW: "permReportsView",
    AUDIT_VIEW: "permAuditView",
    SETTINGS_MANAGE: "permSettingsManage",
    PAYMENTS_MANAGE: "permPaymentsManage",
    INVENTORY_MANAGE: "permInventoryManage",
    LOYALTY_MANAGE: "permLoyaltyManage",
    GUEST_FLAGS_MANAGE: "permGuestFlagsManage",
    MEDIA_MANAGE: "permMediaManage",
    HALL_PLAN_MANAGE: "permHallPlanManage",
  };

  const permissionOrder = useMemo(() => ([
    "ADMIN_ACCESS",
    "SUPERADMIN_ACCESS",
    "STAFF_VIEW",
    "STAFF_MANAGE",
    "MENU_VIEW",
    "MENU_MANAGE",
    "REPORTS_VIEW",
    "AUDIT_VIEW",
    "SETTINGS_MANAGE",
    "PAYMENTS_MANAGE",
    "INVENTORY_MANAGE",
    "LOYALTY_MANAGE",
    "GUEST_FLAGS_MANAGE",
    "MEDIA_MANAGE",
    "HALL_PLAN_MANAGE",
  ]), []);

  const roleDefaultPermissions = useMemo<Record<string, string[]>>(() => ({
    SUPER_ADMIN: [
      "SUPERADMIN_ACCESS",
      "ADMIN_ACCESS",
      "STAFF_VIEW",
      "STAFF_MANAGE",
      "MENU_VIEW",
      "MENU_MANAGE",
      "REPORTS_VIEW",
      "AUDIT_VIEW",
      "SETTINGS_MANAGE",
      "PAYMENTS_MANAGE",
      "INVENTORY_MANAGE",
      "LOYALTY_MANAGE",
      "GUEST_FLAGS_MANAGE",
      "MEDIA_MANAGE",
      "HALL_PLAN_MANAGE",
    ],
    OWNER: [
      "ADMIN_ACCESS",
      "STAFF_VIEW",
      "STAFF_MANAGE",
      "MENU_VIEW",
      "MENU_MANAGE",
      "REPORTS_VIEW",
      "AUDIT_VIEW",
      "SETTINGS_MANAGE",
      "PAYMENTS_MANAGE",
      "INVENTORY_MANAGE",
      "LOYALTY_MANAGE",
      "GUEST_FLAGS_MANAGE",
      "MEDIA_MANAGE",
      "HALL_PLAN_MANAGE",
    ],
    ADMIN: [
      "ADMIN_ACCESS",
      "STAFF_VIEW",
      "STAFF_MANAGE",
      "MENU_VIEW",
      "MENU_MANAGE",
      "REPORTS_VIEW",
      "AUDIT_VIEW",
      "SETTINGS_MANAGE",
      "PAYMENTS_MANAGE",
      "INVENTORY_MANAGE",
      "LOYALTY_MANAGE",
      "GUEST_FLAGS_MANAGE",
      "MEDIA_MANAGE",
      "HALL_PLAN_MANAGE",
    ],
    MANAGER: [
      "ADMIN_ACCESS",
      "STAFF_VIEW",
      "STAFF_MANAGE",
      "MENU_VIEW",
      "MENU_MANAGE",
      "REPORTS_VIEW",
      "AUDIT_VIEW",
      "SETTINGS_MANAGE",
      "PAYMENTS_MANAGE",
      "INVENTORY_MANAGE",
      "LOYALTY_MANAGE",
      "GUEST_FLAGS_MANAGE",
      "MEDIA_MANAGE",
      "HALL_PLAN_MANAGE",
    ],
    CASHIER: ["ADMIN_ACCESS", "REPORTS_VIEW", "PAYMENTS_MANAGE"],
    MARKETER: ["ADMIN_ACCESS", "REPORTS_VIEW", "LOYALTY_MANAGE", "MENU_VIEW"],
    ACCOUNTANT: ["ADMIN_ACCESS", "REPORTS_VIEW", "PAYMENTS_MANAGE", "AUDIT_VIEW"],
    SUPPORT: ["ADMIN_ACCESS", "AUDIT_VIEW", "REPORTS_VIEW"],
  }), []);

  const normalizePermList = useCallback((list: string[]) => {
    const set = new Set(
      list
        .map((s) => s.trim().toUpperCase())
        .filter((s) => s.length > 0)
    );
    return permissionOrder.filter((p) => set.has(p));
  }, [permissionOrder]);

  const parsePermissionsCsv = useCallback((raw?: string | null) => {
    return normalizePermList((raw ?? "").split(/[,\s]+/));
  }, [normalizePermList]);

  const defaultPermsForRole = useCallback((role?: string | null) => {
    return normalizePermList(roleDefaultPermissions[(role ?? "").toUpperCase()] ?? []);
  }, [normalizePermList, roleDefaultPermissions]);

  const formatPermList = (list: string[]) =>
    list.length ? list.map((p) => t(lang, permissionLabels[p] ?? p)).join(", ") : "—";

  const rolesMatrix = [
    "SUPER_ADMIN",
    "OWNER",
    "ADMIN",
    "MANAGER",
    "CASHIER",
    "MARKETER",
    "ACCOUNTANT",
    "SUPPORT",
    "WAITER",
    "HOST",
    "KITCHEN",
    "BAR",
  ];

  const [tenants, setTenants] = useState<Tenant[]>([]);
  const [restaurants, setRestaurants] = useState<Restaurant[]>([]);
  const [branches, setBranches] = useState<Branch[]>([]);
  const [staff, setStaff] = useState<StaffUser[]>([]);
  const [deviceSessions, setDeviceSessions] = useState<DeviceSession[]>([]);
  const [deviceIncludeRevoked, setDeviceIncludeRevoked] = useState(false);
  const [deviceBranchId, setDeviceBranchId] = useState<number | "">("");
  const [deviceStaffFilterId, setDeviceStaffFilterId] = useState<number | "">("");
  const [deviceLoading, setDeviceLoading] = useState(false);
  const [deviceError, setDeviceError] = useState<string | null>(null);
  const [tenantId, setTenantId] = useState<number | "">("");
  const [branchId, setBranchId] = useState<number | "">("");
  const [branchSettings, setBranchSettings] = useState<BranchSettings | null>(null);
  const [tenantStatusFilter, setTenantStatusFilter] = useState<"" | "ACTIVE" | "INACTIVE">("");
  const [branchStatusFilter, setBranchStatusFilter] = useState<"" | "ACTIVE" | "INACTIVE">("");
  const [branchRestaurantFilterId, setBranchRestaurantFilterId] = useState<number | "">("");
  const [statsFrom, setStatsFrom] = useState("");
  const [statsTo, setStatsTo] = useState("");
  const [statsOrderStatus, setStatsOrderStatus] = useState("");
  const [statsGuestPhone, setStatsGuestPhone] = useState("");
  const [statsShiftFrom, setStatsShiftFrom] = useState("");
  const [statsShiftTo, setStatsShiftTo] = useState("");
  const [guestConsentPhone, setGuestConsentPhone] = useState("");
  const [guestConsentType, setGuestConsentType] = useState<"" | "PRIVACY" | "MARKETING">("");
  const [guestConsentAccepted, setGuestConsentAccepted] = useState<"" | "true" | "false">("");
  const [guestConsentLimit, setGuestConsentLimit] = useState(200);
  const [guestConsentPage, setGuestConsentPage] = useState(0);
  const [guestConsentLogs, setGuestConsentLogs] = useState<ConsentLog[]>([]);
  const [guestConsentLoading, setGuestConsentLoading] = useState(false);
  const [guestConsentError, setGuestConsentError] = useState<string | null>(null);
  const [guestFlagVip, setGuestFlagVip] = useState(false);
  const [guestFlagNoShow, setGuestFlagNoShow] = useState(false);
  const [guestFlagConflict, setGuestFlagConflict] = useState(false);
  const [guestFlagsSaving, setGuestFlagsSaving] = useState(false);
  const [guestFlagsSaved, setGuestFlagsSaved] = useState(false);
  const [guestFlagsError, setGuestFlagsError] = useState<string | null>(null);
  const [guestFlagHistory, setGuestFlagHistory] = useState<{
    flagType: string;
    oldActive?: boolean | null;
    newActive: boolean;
    changedByStaffId?: number | null;
    changedAt?: string | null;
  }[]>([]);
  const [guestFlagBulkType, setGuestFlagBulkType] = useState<"" | "VIP" | "NO_SHOW" | "CONFLICT">("");
  const [guestFlagBulkActive, setGuestFlagBulkActive] = useState(true);
  const [guestFlagBulkPhones, setGuestFlagBulkPhones] = useState("");
  const [guestFlagBulkRunning, setGuestFlagBulkRunning] = useState(false);
  const [guestFlagBulkMessage, setGuestFlagBulkMessage] = useState<string | null>(null);
  const [stats, setStats] = useState<StatsSummary | null>(null);
  const [branchStats, setBranchStats] = useState<BranchSummaryRow[]>([]);
  const [topItems, setTopItems] = useState<TopItemRow[]>([]);
  const [topWaiters, setTopWaiters] = useState<WaiterMotivationRow[]>([]);
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
  const [newTenantLogoUrl, setNewTenantLogoUrl] = useState("");
  const [newTenantCountry, setNewTenantCountry] = useState("");
  const [newTenantAddress, setNewTenantAddress] = useState("");
  const [newTenantPhone, setNewTenantPhone] = useState("");
  const [newTenantContactPerson, setNewTenantContactPerson] = useState("");
  const [newTenantLogoUploading, setNewTenantLogoUploading] = useState(false);
  const [newRestaurantName, setNewRestaurantName] = useState("");
  const [newRestaurantLogoUrl, setNewRestaurantLogoUrl] = useState("");
  const [newRestaurantCountry, setNewRestaurantCountry] = useState("");
  const [newRestaurantAddress, setNewRestaurantAddress] = useState("");
  const [newRestaurantPhone, setNewRestaurantPhone] = useState("");
  const [newRestaurantContactPerson, setNewRestaurantContactPerson] = useState("");
  const [newRestaurantLogoUploading, setNewRestaurantLogoUploading] = useState(false);
  const [newBranchName, setNewBranchName] = useState("");
  const [newBranchLogoUrl, setNewBranchLogoUrl] = useState("");
  const [newBranchCountry, setNewBranchCountry] = useState("");
  const [newBranchAddress, setNewBranchAddress] = useState("");
  const [newBranchPhone, setNewBranchPhone] = useState("");
  const [newBranchContactPerson, setNewBranchContactPerson] = useState("");
  const [newBranchLogoUploading, setNewBranchLogoUploading] = useState(false);
  const [newBranchRestaurantId, setNewBranchRestaurantId] = useState<number | "">("");

  const [editingTenantId, setEditingTenantId] = useState<number | null>(null);
  const [editTenantName, setEditTenantName] = useState("");
  const [editTenantLogoUrl, setEditTenantLogoUrl] = useState("");
  const [editTenantCountry, setEditTenantCountry] = useState("");
  const [editTenantAddress, setEditTenantAddress] = useState("");
  const [editTenantPhone, setEditTenantPhone] = useState("");
  const [editTenantContactPerson, setEditTenantContactPerson] = useState("");
  const [editTenantLogoUploading, setEditTenantLogoUploading] = useState(false);
  const [editingRestaurantId, setEditingRestaurantId] = useState<number | null>(null);
  const [editRestaurantName, setEditRestaurantName] = useState("");
  const [editRestaurantLogoUrl, setEditRestaurantLogoUrl] = useState("");
  const [editRestaurantCountry, setEditRestaurantCountry] = useState("");
  const [editRestaurantAddress, setEditRestaurantAddress] = useState("");
  const [editRestaurantPhone, setEditRestaurantPhone] = useState("");
  const [editRestaurantContactPerson, setEditRestaurantContactPerson] = useState("");
  const [editRestaurantLogoUploading, setEditRestaurantLogoUploading] = useState(false);

  const [editingBranchId, setEditingBranchId] = useState<number | null>(null);
  const [editBranchName, setEditBranchName] = useState("");
  const [editBranchLogoUrl, setEditBranchLogoUrl] = useState("");
  const [editBranchCountry, setEditBranchCountry] = useState("");
  const [editBranchAddress, setEditBranchAddress] = useState("");
  const [editBranchPhone, setEditBranchPhone] = useState("");
  const [editBranchContactPerson, setEditBranchContactPerson] = useState("");
  const [editBranchLogoUploading, setEditBranchLogoUploading] = useState(false);
  const [editBranchRestaurantId, setEditBranchRestaurantId] = useState<number | "">("");
  const [newStaffUser, setNewStaffUser] = useState("");
  const [newStaffPass, setNewStaffPass] = useState("");
  const [newStaffRole, setNewStaffRole] = useState("ADMIN");
  const [newStaffHallId, setNewStaffHallId] = useState<number | "">("");
  const [staffProfileFilter, setStaffProfileFilter] = useState("");
  const [permissionsStaffId, setPermissionsStaffId] = useState<number | "">("");
  const [permissionsOverride, setPermissionsOverride] = useState(false);
  const [permissionsSelected, setPermissionsSelected] = useState<string[]>([]);
  const [permissionsSaving, setPermissionsSaving] = useState(false);
  const [permissionsMessage, setPermissionsMessage] = useState<string | null>(null);
  const [editingStaffId, setEditingStaffId] = useState<number | null>(null);
  const [editStaffRole, setEditStaffRole] = useState("ADMIN");
  const [editStaffActive, setEditStaffActive] = useState(true);
  const [editStaffHallId, setEditStaffHallId] = useState<number | "">("");
  const [editStaffFirstName, setEditStaffFirstName] = useState("");
  const [editStaffLastName, setEditStaffLastName] = useState("");
  const [editStaffAge, setEditStaffAge] = useState("");
  const [editStaffGender, setEditStaffGender] = useState("");
  const [editStaffPhotoUrl, setEditStaffPhotoUrl] = useState("");
  const [editStaffPhotoUploading, setEditStaffPhotoUploading] = useState(false);
  const [editStaffRating, setEditStaffRating] = useState("");
  const [editStaffRecommended, setEditStaffRecommended] = useState(false);
  const [editStaffExperienceYears, setEditStaffExperienceYears] = useState("");
  const [editStaffFavoriteItems, setEditStaffFavoriteItems] = useState("");
  const [currencies, setCurrencies] = useState<CurrencyDto[]>([]);
  const [newCurrencyCode, setNewCurrencyCode] = useState("");
  const [newCurrencyName, setNewCurrencyName] = useState("");
  const [newCurrencySymbol, setNewCurrencySymbol] = useState("");

  useEffect(() => {
    const u = localStorage.getItem("superUser") ?? "";
    const l = (localStorage.getItem("superLang") ?? "ru") as Lang;
    const pf = localStorage.getItem("superStaffProfileFilter") ?? "";
    if (u) {
      setUsername(u);
      setAuthReady(true);
    }
    if (l === "ru" || l === "ro" || l === "en") {
      setLang(l);
    }
    if (pf === "FILLED" || pf === "EMPTY") {
      setStaffProfileFilter(pf);
    }
  }, []);

  useEffect(() => {
    localStorage.setItem("superLang", lang);
  }, [lang]);

  useEffect(() => {
    if (staffProfileFilter) {
      localStorage.setItem("superStaffProfileFilter", staffProfileFilter);
    } else {
      localStorage.removeItem("superStaffProfileFilter");
    }
  }, [staffProfileFilter]);

  useEffect(() => {
    if (!permissionsStaffId) return;
    const su = staff.find((s) => s.id === permissionsStaffId);
    if (!su) return;
    const override = !!(su.permissions && su.permissions.trim());
    setPermissionsOverride(override);
    setPermissionsSelected(override ? parsePermissionsCsv(su.permissions) : defaultPermsForRole(su.role));
    setPermissionsMessage(null);
  }, [permissionsStaffId, staff, parsePermissionsCsv, defaultPermsForRole]);

  const api = useCallback(async (path: string, init?: RequestInit) => {
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
  }, [lang]);

  async function uploadMediaFile(file: File, type: string) {
    const form = new FormData();
    form.append("file", file);
    const res = await fetch(`${API_BASE}/api/admin/media/upload?type=${encodeURIComponent(type)}`, {
      method: "POST",
      credentials: "include",
      body: form,
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
    const data = await res.json();
    return data.url as string;
  }

  const waiterPalette = ["#FF6B6B", "#4ECDC4", "#FFD166", "#6C5CE7", "#00B894", "#FD79A8", "#0984E3"];
  const waiterColor = (id?: number | null) => {
    if (!id) return "#9aa0a6";
    return waiterPalette[id % waiterPalette.length];
  };

  const filteredStaff = useMemo(() => {
    if (!staffProfileFilter) return staff;
    return staff.filter((s) => {
      const hasProfile = !!(s.firstName || s.lastName || s.age != null || s.gender || s.photoUrl);
      return staffProfileFilter === "FILLED" ? hasProfile : !hasProfile;
    });
  }, [staff, staffProfileFilter]);

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
        body: JSON.stringify({ username, password, totpCode: totpLoginCode || null }),
      });
      localStorage.setItem("superUser", username);
      setAuthReady(true);
      loadTenants();
      loadCurrencies();
      loadTotpStatus(true);
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
      setTotpLoginCode("");
      setAuthReady(false);
      setError(null);
      setSessionExpired(false);
      setTotpStatus(null);
      setTotpSecret(null);
      setTotpOtpauth(null);
      setTotpQrDataUrl(null);
      setTotpManageCode("");
      setTotpError(null);
    }
  }

  const loadTotpStatus = useCallback(async (force = false) => {
    if (!authReady && !force) return;
    try {
      const res = await api("/api/super/2fa/status");
      if (!res.ok) {
        setTotpStatus(null);
        return;
      }
      const body = await res.json();
      setTotpStatus(body);
    } catch {
      setTotpStatus(null);
    }
  }, [authReady, api]);

  async function setupTotp() {
    setTotpError(null);
    try {
      const res = await api("/api/super/2fa/setup", { method: "POST" });
      const body = await res.json();
      setTotpSecret(body.secret);
      setTotpOtpauth(body.otpauthUrl);
      setTotpStatus({ enabled: body.enabled, hasSecret: true });
    } catch (e: any) {
      setTotpError(e?.message ?? "2FA error");
    }
  }

  async function enableTotp() {
    setTotpError(null);
    try {
      const res = await api("/api/super/2fa/enable", {
        method: "POST",
        body: JSON.stringify({ code: totpManageCode }),
      });
      const body = await res.json();
      setTotpStatus(body);
      setTotpManageCode("");
    } catch (e: any) {
      setTotpError(e?.message ?? "2FA error");
    }
  }

  async function disableTotp() {
    setTotpError(null);
    try {
      const res = await api("/api/super/2fa/disable", {
        method: "POST",
        body: JSON.stringify({ code: totpManageCode }),
      });
      const body = await res.json();
      setTotpStatus(body);
      setTotpManageCode("");
    } catch (e: any) {
      setTotpError(e?.message ?? "2FA error");
    }
  }

  const loadTenants = useCallback(async () => {
    if (!authReady) return;
    setError(null);
    try {
      const qsTenants = new URLSearchParams();
      if (tenantStatusFilter) qsTenants.set("isActive", tenantStatusFilter === "ACTIVE" ? "true" : "false");
      const res = await api(`/api/super/tenants${qsTenants.toString() ? `?${qsTenants.toString()}` : ""}`);
      setTenants(await res.json());
      const qsRestaurants = new URLSearchParams();
      if (tenantId) qsRestaurants.set("tenantId", String(tenantId));
      const resRestaurants = await api(`/api/super/restaurants${qsRestaurants.toString() ? `?${qsRestaurants.toString()}` : ""}`);
      setRestaurants(await resRestaurants.json());
      const qsBranches = new URLSearchParams();
      if (tenantId) qsBranches.set("tenantId", String(tenantId));
      if (branchRestaurantFilterId) qsBranches.set("restaurantId", String(branchRestaurantFilterId));
      if (branchStatusFilter) qsBranches.set("isActive", branchStatusFilter === "ACTIVE" ? "true" : "false");
      const resBranches = await api(`/api/super/branches${qsBranches.toString() ? `?${qsBranches.toString()}` : ""}`);
      setBranches(await resBranches.json());
    } catch (e: any) {
      setError(e?.message ?? t(lang, "loadError"));
    }
  }, [authReady, tenantStatusFilter, tenantId, branchRestaurantFilterId, branchStatusFilter, api, lang]);

  const loadCurrencies = useCallback(async () => {
    if (!authReady) return;
    try {
      const res = await api("/api/admin/currencies?includeInactive=true");
      setCurrencies(await res.json());
    } catch (_) {
      setCurrencies([]);
    }
  }, [authReady, api]);

  useEffect(() => {
    loadTenants();
    loadCurrencies();
    loadTotpStatus();
  }, [loadTenants, loadCurrencies, loadTotpStatus]);

  useEffect(() => {
    let cancelled = false;
    async function buildQr() {
      if (!totpOtpauth) {
        setTotpQrDataUrl(null);
        return;
      }
      try {
        const url = await QRCode.toDataURL(totpOtpauth, { width: 160, margin: 1 });
        if (!cancelled) setTotpQrDataUrl(url);
      } catch {
        if (!cancelled) setTotpQrDataUrl(null);
      }
    }
    buildQr();
    return () => {
      cancelled = true;
    };
  }, [totpOtpauth]);

  const loadTables = useCallback(async () => {
    if (!branchId) return;
    const hallsRes = await api(`/api/admin/halls?branchId=${branchId}`);
    const hallsBody = await hallsRes.json();
    setHalls(hallsBody);
    if (!hallId && hallsBody.length > 0) {
      setHallId(hallsBody[0].id);
    }
    const res = await api(`/api/admin/tables?branchId=${branchId}`);
    setTables(await res.json());
  }, [api, branchId, hallId]);

  const loadBranchSettings = useCallback(async () => {
    if (!branchId) {
      setBranchSettings(null);
      setCurrentIp("");
      return;
    }
    try {
      const res = await api(`/api/admin/branch-settings?branchId=${branchId}`);
      const body = await res.json();
      setBranchSettings(body);
      try {
        const ipRes = await api("/api/admin/my-ip");
        const ipBody = await ipRes.json();
        setCurrentIp(ipBody?.ip ?? "");
      } catch {
        setCurrentIp("");
      }
    } catch (_) {
      setBranchSettings(null);
      setCurrentIp("");
    }
  }, [api, branchId]);

  async function saveBranchSettings() {
    if (!branchId || !branchSettings) return;
    await api(`/api/admin/branch-settings?branchId=${branchId}`, {
      method: "PUT",
      body: JSON.stringify({
        defaultLang: branchSettings.defaultLang ?? "ru",
        timeZone: branchSettings.timeZone,
        onlinePayEnabled: branchSettings.onlinePayEnabled,
        onlinePayProvider: branchSettings.onlinePayProvider,
        onlinePayCurrencyCode: branchSettings.onlinePayCurrencyCode,
        onlinePayRequestUrl: branchSettings.onlinePayRequestUrl,
        onlinePayCacertPath: branchSettings.onlinePayCacertPath,
        onlinePayPcertPath: branchSettings.onlinePayPcertPath,
        onlinePayPcertPassword: branchSettings.onlinePayPcertPassword,
        onlinePayKeyPath: branchSettings.onlinePayKeyPath,
        onlinePayRedirectUrl: branchSettings.onlinePayRedirectUrl,
        onlinePayReturnUrl: branchSettings.onlinePayReturnUrl,
        adminIpAllowlist: branchSettings.adminIpAllowlist ?? "",
        adminIpDenylist: branchSettings.adminIpDenylist ?? "",
      }),
    });
    loadBranchSettings();
  }

  function missingOnlinePayFields() {
    if (!branchSettings?.onlinePayEnabled) return [];
    const missing: string[] = [];
    if (!branchSettings.onlinePayRequestUrl?.trim()) missing.push(t(lang, "onlinePayRequestUrl"));
    if (!branchSettings.onlinePayCacertPath?.trim()) missing.push(t(lang, "onlinePayCacertPath"));
    if (!branchSettings.onlinePayPcertPath?.trim()) missing.push(t(lang, "onlinePayPcertPath"));
    if (!branchSettings.onlinePayPcertPassword?.trim()) missing.push(t(lang, "onlinePayPcertPassword"));
    if (!branchSettings.onlinePayKeyPath?.trim()) missing.push(t(lang, "onlinePayKeyPath"));
    if (!branchSettings.onlinePayRedirectUrl?.trim()) missing.push(t(lang, "onlinePayRedirectUrl"));
    if (!branchSettings.onlinePayReturnUrl?.trim()) missing.push(t(lang, "onlinePayReturnUrl"));
    return missing;
  }

  function onlinePayFieldStyle(value?: string | null) {
    if (!branchSettings?.onlinePayEnabled) return undefined;
    const missing = !value || !value.trim();
    return missing ? { border: "1px solid #dc2626" } : undefined;
  }

  function onlinePayFieldError(kind: "url" | "pem" | "returnUrl", value?: string | null) {
    if (!branchSettings?.onlinePayEnabled) return "";
    const v = (value ?? "").trim();
    if (!v) return "";
    if (kind === "url" || kind === "returnUrl") {
      if (!/^https?:\/\//i.test(v)) return "URL должен начинаться с http:// или https://";
      if (kind !== "returnUrl" && !/^https:\/\//i.test(v)) return "Рекомендуется использовать https://";
    }
    if (kind === "pem") {
      if (!v.endsWith(".pem")) return "Ожидается путь к *.pem";
      if (!v.startsWith("/")) return "Ожидается абсолютный путь";
    }
    return "";
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
  }, [branchId, loadTables, loadBranchSettings]);

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
  }, [api, hallId, branchId]);

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
    if (branchId) qs.set("branchId", String(branchId));
    if (branchId && hallId) qs.set("hallId", String(hallId));
    if (statsOrderStatus) qs.set("status", statsOrderStatus);
    if (statsGuestPhone) qs.set("guestPhone", statsGuestPhone.trim());
    if (statsShiftFrom) qs.set("shiftFrom", statsShiftFrom);
    if (statsShiftTo) qs.set("shiftTo", statsShiftTo);
    const res = await api(`/api/super/stats/summary?${qs.toString()}`);
    const body = await res.json();
    setStats(body);
    const resBranches = await api(`/api/super/stats/branches?${qs.toString()}`);
    setBranchStats(await resBranches.json());
    const resTopItems = await api(`/api/super/stats/top-items?${qs.toString()}`);
    setTopItems(await resTopItems.json());
    const resTopWaiters = await api(`/api/super/stats/top-waiters?${qs.toString()}`);
    setTopWaiters(await resTopWaiters.json());
  }

  async function loadGuestConsents() {
    const phone = guestConsentPhone.trim();
    if (!phone) {
      setGuestConsentError(t(lang, "guestConsentEmpty"));
      setGuestConsentLogs([]);
      return;
    }
    setGuestConsentLoading(true);
    setGuestConsentError(null);
    try {
      const qs = new URLSearchParams();
      qs.set("phone", phone);
      if (branchId) qs.set("branchId", String(branchId));
      if (guestConsentType) qs.set("consentType", guestConsentType);
      if (guestConsentAccepted) qs.set("accepted", guestConsentAccepted);
      if (guestConsentLimit) qs.set("limit", String(guestConsentLimit));
      if (guestConsentPage) qs.set("page", String(guestConsentPage));
      const res = await api(`/api/super/guest-consents?${qs.toString()}`);
      if (!res.ok) throw new Error(await res.text());
      const body = await res.json();
      const normalized = (Array.isArray(body) ? body : []).map((log: any) => ({
        consentType: String(log?.consentType ?? ""),
        accepted: !!log?.accepted,
        textVersion: log?.textVersion ?? null,
        ip: log?.ip ?? null,
        userAgent: log?.userAgent ?? null,
        createdAt: log?.createdAt ?? null,
      }));
      setGuestConsentLogs(normalized);
    } catch (err: any) {
      setGuestConsentError(err?.message ?? "Failed to load");
      setGuestConsentLogs([]);
    } finally {
      setGuestConsentLoading(false);
    }
  }

  async function loadGuestFlagsSuper() {
    const phone = guestConsentPhone.trim();
    if (!phone) return;
    setGuestFlagsError(null);
    try {
      const qs = new URLSearchParams();
      qs.set("phone", phone);
      if (branchId) qs.set("branchId", String(branchId));
      const res = await api(`/api/super/guest-flags?${qs.toString()}`);
      if (!res.ok) throw new Error(await res.text());
      const body = await res.json();
      setGuestFlagVip(!!body.vip);
      setGuestFlagNoShow(!!body.noShow);
      setGuestFlagConflict(!!body.conflict);
      await loadGuestFlagHistorySuper();
    } catch (err: any) {
      setGuestFlagsError(err?.message ?? "Failed to load guest flags");
    }
  }

  async function saveGuestFlagsSuper() {
    const phone = guestConsentPhone.trim();
    if (!phone) return;
    setGuestFlagsSaving(true);
    setGuestFlagsSaved(false);
    setGuestFlagsError(null);
    try {
      const res = await api("/api/super/guest-flags", {
        method: "POST",
        body: JSON.stringify({
          phone,
          branchId: branchId || null,
          vip: guestFlagVip,
          noShow: guestFlagNoShow,
          conflict: guestFlagConflict,
        }),
      });
      if (!res.ok) throw new Error(await res.text());
      const body = await res.json();
      setGuestFlagVip(!!body.vip);
      setGuestFlagNoShow(!!body.noShow);
      setGuestFlagConflict(!!body.conflict);
      setGuestFlagsSaved(true);
    } catch (err: any) {
      setGuestFlagsError(err?.message ?? "Failed to save guest flags");
    } finally {
      setGuestFlagsSaving(false);
    }
  }

  async function loadGuestFlagHistorySuper() {
    const phone = guestConsentPhone.trim();
    if (!phone) return;
    try {
      const qs = new URLSearchParams();
      qs.set("phone", phone);
      if (branchId) qs.set("branchId", String(branchId));
      const res = await api(`/api/super/guest-flags/history?${qs.toString()}`);
      if (!res.ok) throw new Error(await res.text());
      setGuestFlagHistory(await res.json());
    } catch (_) {
      setGuestFlagHistory([]);
    }
  }

  async function applyGuestFlagsBulkSuper() {
    const phones = guestFlagBulkPhones
      .split(/\r?\n/)
      .map((s) => s.trim())
      .filter(Boolean);
    if (phones.length === 0 || !guestFlagBulkType) {
      setGuestFlagBulkMessage("Заполните список телефонов и тип флага");
      return;
    }
    setGuestFlagBulkRunning(true);
    setGuestFlagBulkMessage(null);
    try {
      const res = await api("/api/super/guest-flags/bulk", {
        method: "POST",
        body: JSON.stringify({
          phones,
          branchId: branchId || null,
          flagType: guestFlagBulkType,
          active: guestFlagBulkActive,
        }),
      });
      if (!res.ok) throw new Error(await res.text());
      setGuestFlagBulkMessage("Готово");
    } catch (e: any) {
      setGuestFlagBulkMessage(e?.message ?? "Ошибка");
    } finally {
      setGuestFlagBulkRunning(false);
    }
  }

  async function downloadSummaryCsv() {
    if (!tenantId) return;
    const qs = new URLSearchParams();
    qs.set("tenantId", String(tenantId));
    if (statsFrom) qs.set("from", statsFrom);
    if (statsTo) qs.set("to", statsTo);
    if (branchId) qs.set("branchId", String(branchId));
    if (branchId && hallId) qs.set("hallId", String(hallId));
    if (statsOrderStatus) qs.set("status", statsOrderStatus);
    if (statsGuestPhone) qs.set("guestPhone", statsGuestPhone.trim());
    if (statsShiftFrom) qs.set("shiftFrom", statsShiftFrom);
    if (statsShiftTo) qs.set("shiftTo", statsShiftTo);
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
    if (branchId) qs.set("branchId", String(branchId));
    if (statsOrderStatus) qs.set("status", statsOrderStatus);
    if (statsGuestPhone) qs.set("guestPhone", statsGuestPhone.trim());
    if (statsShiftFrom) qs.set("shiftFrom", statsShiftFrom);
    if (statsShiftTo) qs.set("shiftTo", statsShiftTo);
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

  async function downloadTopItemsCsv() {
    if (!tenantId) return;
    const qs = new URLSearchParams();
    qs.set("tenantId", String(tenantId));
    if (statsFrom) qs.set("from", statsFrom);
    if (statsTo) qs.set("to", statsTo);
    if (branchId) qs.set("branchId", String(branchId));
    if (branchId && hallId) qs.set("hallId", String(hallId));
    if (statsOrderStatus) qs.set("status", statsOrderStatus);
    if (statsGuestPhone) qs.set("guestPhone", statsGuestPhone.trim());
    if (statsShiftFrom) qs.set("shiftFrom", statsShiftFrom);
    if (statsShiftTo) qs.set("shiftTo", statsShiftTo);
    const res = await api(`/api/super/stats/top-items.csv?${qs.toString()}`);
    const blob = await res.blob();
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = "tenant-top-items.csv";
    document.body.appendChild(a);
    a.click();
    a.remove();
    URL.revokeObjectURL(url);
  }

  async function downloadTopWaitersCsv() {
    if (!tenantId) return;
    const qs = new URLSearchParams();
    qs.set("tenantId", String(tenantId));
    if (statsFrom) qs.set("from", statsFrom);
    if (statsTo) qs.set("to", statsTo);
    if (branchId) qs.set("branchId", String(branchId));
    if (branchId && hallId) qs.set("hallId", String(hallId));
    if (statsOrderStatus) qs.set("status", statsOrderStatus);
    if (statsGuestPhone) qs.set("guestPhone", statsGuestPhone.trim());
    if (statsShiftFrom) qs.set("shiftFrom", statsShiftFrom);
    if (statsShiftTo) qs.set("shiftTo", statsShiftTo);
    const res = await api(`/api/super/stats/top-waiters.csv?${qs.toString()}`);
    const blob = await res.blob();
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = "tenant-top-waiters.csv";
    document.body.appendChild(a);
    a.click();
    a.remove();
    URL.revokeObjectURL(url);
  }

  async function createTenant() {
    await api("/api/super/tenants", {
      method: "POST",
      body: JSON.stringify({
        name: newTenantName,
        logoUrl: newTenantLogoUrl || null,
        country: newTenantCountry || null,
        address: newTenantAddress || null,
        phone: newTenantPhone || null,
        contactPerson: newTenantContactPerson || null,
      }),
    });
    setNewTenantName("");
    setNewTenantLogoUrl("");
    setNewTenantCountry("");
    setNewTenantAddress("");
    setNewTenantPhone("");
    setNewTenantContactPerson("");
    loadTenants();
  }

  async function toggleTenant(t: Tenant) {
    await api(`/api/super/tenants/${t.id}`, {
      method: "PATCH",
      body: JSON.stringify({ isActive: !t.isActive }),
    });
    loadTenants();
  }

  async function updateTenant(id: number) {
    await api(`/api/super/tenants/${id}`, {
      method: "PATCH",
      body: JSON.stringify({
        name: editTenantName,
        logoUrl: editTenantLogoUrl || null,
        country: editTenantCountry || null,
        address: editTenantAddress || null,
        phone: editTenantPhone || null,
        contactPerson: editTenantContactPerson || null,
      }),
    });
    setEditingTenantId(null);
    loadTenants();
  }

  async function createRestaurant() {
    if (!tenantId) return;
    await api(`/api/super/tenants/${tenantId}/restaurants`, {
      method: "POST",
      body: JSON.stringify({
        name: newRestaurantName,
        logoUrl: newRestaurantLogoUrl || null,
        country: newRestaurantCountry || null,
        address: newRestaurantAddress || null,
        phone: newRestaurantPhone || null,
        contactPerson: newRestaurantContactPerson || null,
      }),
    });
    setNewRestaurantName("");
    setNewRestaurantLogoUrl("");
    setNewRestaurantCountry("");
    setNewRestaurantAddress("");
    setNewRestaurantPhone("");
    setNewRestaurantContactPerson("");
    loadTenants();
  }

  async function toggleRestaurant(r: Restaurant) {
    await api(`/api/super/restaurants/${r.id}`, {
      method: "PATCH",
      body: JSON.stringify({ isActive: !r.isActive }),
    });
    loadTenants();
  }

  async function updateRestaurant(id: number) {
    await api(`/api/super/restaurants/${id}`, {
      method: "PATCH",
      body: JSON.stringify({
        name: editRestaurantName,
        logoUrl: editRestaurantLogoUrl || null,
        country: editRestaurantCountry || null,
        address: editRestaurantAddress || null,
        phone: editRestaurantPhone || null,
        contactPerson: editRestaurantContactPerson || null,
      }),
    });
    setEditingRestaurantId(null);
    loadTenants();
  }

  async function deleteRestaurant(id: number) {
    await api(`/api/super/restaurants/${id}`, { method: "DELETE" });
    loadTenants();
  }

  async function createBranch() {
    if (!tenantId) return;
    await api(`/api/super/tenants/${tenantId}/branches`, {
      method: "POST",
      body: JSON.stringify({
        name: newBranchName,
        restaurantId: newBranchRestaurantId || null,
        logoUrl: newBranchLogoUrl || null,
        country: newBranchCountry || null,
        address: newBranchAddress || null,
        phone: newBranchPhone || null,
        contactPerson: newBranchContactPerson || null,
      }),
    });
    setNewBranchName("");
    setNewBranchLogoUrl("");
    setNewBranchCountry("");
    setNewBranchAddress("");
    setNewBranchPhone("");
    setNewBranchContactPerson("");
    setNewBranchRestaurantId("");
    loadTenants();
  }

  async function toggleBranch(b: Branch) {
    await api(`/api/super/branches/${b.id}`, {
      method: "PATCH",
      body: JSON.stringify({ isActive: !b.isActive }),
    });
    loadTenants();
  }

  async function updateBranch(id: number) {
    await api(`/api/super/branches/${id}`, {
      method: "PATCH",
      body: JSON.stringify({
        name: editBranchName,
        restaurantId: editBranchRestaurantId || null,
        logoUrl: editBranchLogoUrl || null,
        country: editBranchCountry || null,
        address: editBranchAddress || null,
        phone: editBranchPhone || null,
        contactPerson: editBranchContactPerson || null,
      }),
    });
    setEditingBranchId(null);
    loadTenants();
  }

  async function loadStaff() {
    const res = await api("/api/admin/staff?branchId=" + (branchId || ""));
    setStaff(await res.json());
  }

  async function loadDeviceSessions() {
    if (!authReady) return;
    setDeviceLoading(true);
    setDeviceError(null);
    try {
      const params = new URLSearchParams();
      if (deviceIncludeRevoked) params.set("includeRevoked", "true");
      if (deviceBranchId !== "") params.set("branchId", String(deviceBranchId));
      if (deviceStaffFilterId !== "") params.set("staffUserId", String(deviceStaffFilterId));
      const qs = params.toString();
      const res = await api(`/api/super/devices${qs ? `?${qs}` : ""}`);
      setDeviceSessions(await res.json());
    } catch (e: any) {
      setDeviceError(e?.message ?? t(lang, "requestFailed"));
    } finally {
      setDeviceLoading(false);
    }
  }

  async function revokeDeviceSession(id: number) {
    if (!confirm(t(lang, "deviceSessionRevokeConfirm"))) return;
    try {
      await api(`/api/super/devices/${id}/revoke`, { method: "POST" });
      await loadDeviceSessions();
    } catch (e: any) {
      setDeviceError(e?.message ?? t(lang, "requestFailed"));
    }
  }

  async function revokeDeviceSessionsFiltered() {
    if (!confirm(t(lang, "deviceSessionRevokeFilteredConfirm"))) return;
    try {
      let revoked = 0;
      if (deviceStaffFilterId !== "") {
        const res = await api("/api/super/devices/revoke-by-user", {
          method: "POST",
          body: JSON.stringify({ staffUserId: deviceStaffFilterId }),
        });
        const body = await res.json().catch(() => ({}));
        revoked = body?.revoked ?? 0;
      } else if (deviceBranchId !== "") {
        const res = await api("/api/super/devices/revoke-by-branch", {
          method: "POST",
          body: JSON.stringify({ branchId: deviceBranchId }),
        });
        const body = await res.json().catch(() => ({}));
        revoked = body?.revoked ?? 0;
      } else {
        setDeviceError(t(lang, "selectBranch"));
        return;
      }
      alert(`${t(lang, "deviceSessionRevokeDone")}: ${revoked}`);
      await loadDeviceSessions();
    } catch (e: any) {
      setDeviceError(e?.message ?? t(lang, "requestFailed"));
    }
  }

  async function createStaff() {
    if (!branchId) return;
    const hallId = newStaffHallId === "" ? null : newStaffHallId;
    await api("/api/super/staff", {
      method: "POST",
      body: JSON.stringify({ branchId, username: newStaffUser, password: newStaffPass, role: newStaffRole, hallId }),
    });
    setNewStaffUser("");
    setNewStaffPass("");
    setNewStaffRole("ADMIN");
    setNewStaffHallId("");
    loadStaff();
  }

  async function updateStaff(id: number) {
    const ageVal = editStaffAge.trim() ? Number(editStaffAge.trim()) : null;
    if (ageVal != null && (Number.isNaN(ageVal) || ageVal < 0 || ageVal > 120)) {
      setError(t(lang, "invalidAge"));
      return;
    }
    const ratingVal = editStaffRating.trim() ? Number(editStaffRating.trim()) : null;
    if (ratingVal != null && (Number.isNaN(ratingVal) || ratingVal < 0 || ratingVal > 5)) {
      setError(t(lang, "invalidRating"));
      return;
    }
    const expVal = editStaffExperienceYears.trim() ? Number(editStaffExperienceYears.trim()) : null;
    if (expVal != null && (Number.isNaN(expVal) || expVal < 0 || expVal > 80)) {
      setError(t(lang, "invalidExperience"));
      return;
    }
    await api(`/api/super/staff/${id}`, {
      method: "PATCH",
      body: JSON.stringify({
        role: editStaffRole,
        isActive: editStaffActive,
        hallId: editStaffHallId === "" ? null : editStaffHallId,
        firstName: editStaffFirstName,
        lastName: editStaffLastName,
        age: ageVal,
        gender: editStaffGender,
        photoUrl: editStaffPhotoUrl,
        rating: Number.isNaN(ratingVal) ? null : ratingVal,
        recommended: editStaffRecommended,
        experienceYears: Number.isNaN(expVal) ? null : expVal,
        favoriteItems: editStaffFavoriteItems,
      }),
    });
    setEditingStaffId(null);
    loadStaff();
  }

  async function saveStaffPermissions() {
    if (!permissionsStaffId) return;
    setPermissionsSaving(true);
    try {
      const normalized = normalizePermList(permissionsSelected);
      await api(`/api/super/staff/${permissionsStaffId}`, {
        method: "PATCH",
        body: JSON.stringify({ permissions: permissionsOverride ? normalized.join(",") : "" }),
      });
      setPermissionsMessage(t(lang, "permissionsSaved"));
      loadStaff();
    } catch (e: any) {
      setError(e?.message ?? "Permissions update failed");
    } finally {
      setPermissionsSaving(false);
    }
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
          <input value={totpLoginCode} onChange={(e) => setTotpLoginCode(e.target.value)} placeholder={t(lang, "totpCode")} />
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
      {totpStatus && (
        <section
          style={{
            marginTop: 12,
            padding: 12,
            border: "1px solid #e2e8f0",
            borderRadius: 10,
            background: "#f8fafc",
          }}
        >
          <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", gap: 12, flexWrap: "wrap" }}>
            <div style={{ fontSize: 16, fontWeight: 600 }}>{t(lang, "totpTitle")}</div>
            <span style={{ fontSize: 12, padding: "2px 8px", borderRadius: 999, background: totpStatus.enabled ? "#dcfce7" : "#fee2e2", color: "#0f172a" }}>
              {totpStatus.enabled ? t(lang, "totpStatusEnabled") : t(lang, "totpStatusDisabled")}
            </span>
          </div>
          <div style={{ marginTop: 8, display: "flex", gap: 8, flexWrap: "wrap", alignItems: "center" }}>
            <button onClick={setupTotp}>{t(lang, "totpSetup")}</button>
            <input value={totpManageCode} onChange={(e) => setTotpManageCode(e.target.value)} placeholder={t(lang, "totpCode")} />
            <button onClick={enableTotp} disabled={!totpManageCode}>{t(lang, "totpEnable")}</button>
            <button onClick={disableTotp} disabled={!totpManageCode}>{t(lang, "totpDisable")}</button>
            {totpError && <span style={{ color: "#b11e46" }}>{totpError}</span>}
          </div>
          {totpSecret && (
            <div style={{ marginTop: 8, fontSize: 13, color: "#475569", display: "flex", gap: 12, alignItems: "center", flexWrap: "wrap" }}>
              {totpQrDataUrl && (
                <Image src={totpQrDataUrl} alt="TOTP QR" width={160} height={160} style={{ border: "1px solid #e2e8f0", borderRadius: 8, background: "#fff" }} />
              )}
              <div style={{ minWidth: 240 }}>
                <div>{t(lang, "totpSecret")}: <code>{totpSecret}</code></div>
                {totpOtpauth && (
                  <div style={{ wordBreak: "break-all", marginTop: 4 }}>
                    otpauth: <code>{totpOtpauth}</code>
                  </div>
                )}
                <div style={{ marginTop: 4 }}>{t(lang, "totpHint")}</div>
              </div>
            </div>
          )}
        </section>
      )}

      <section style={{ marginTop: 16 }}>
        <h2>{t(lang, "scope")}</h2>
        <div style={{ display: "flex", gap: 8, flexWrap: "wrap", alignItems: "center" }}>
          <select value={tenantId} onChange={(e) => setTenantId(e.target.value ? Number(e.target.value) : "")}>
            <option value="">{t(lang, "selectTenant")}</option>
            {tenants.map((t) => (
              <option key={t.id} value={t.id}>{t.name}</option>
            ))}
          </select>
          <select value={branchRestaurantFilterId} onChange={(e) => setBranchRestaurantFilterId(e.target.value ? Number(e.target.value) : "")}>
            <option value="">{t(lang, "selectRestaurant")}</option>
            {restaurants.filter((r) => !tenantId || r.tenantId === tenantId).map((r) => (
              <option key={r.id} value={r.id}>{restaurantOptionLabel(r)}</option>
            ))}
          </select>
        </div>
      </section>

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
          <label style={{ display: "flex", alignItems: "center", gap: 6 }}>
            {t(lang, "photoUpload")}
            <input
              type="file"
              accept="image/*"
              onChange={async (e) => {
                const file = e.target.files?.[0];
                if (!file) return;
                setNewTenantLogoUploading(true);
                try {
                  const url = await uploadMediaFile(file, "logo");
                  setNewTenantLogoUrl(url);
                } catch (err: any) {
                  setError(err?.message ?? "Upload error");
                } finally {
                  setNewTenantLogoUploading(false);
                  e.currentTarget.value = "";
                }
              }}
            />
          </label>
          <input placeholder={t(lang, "logo")} value={newTenantLogoUrl} readOnly />
          <input placeholder={t(lang, "country")} value={newTenantCountry} onChange={(e) => setNewTenantCountry(e.target.value)} />
          <input placeholder={t(lang, "address")} value={newTenantAddress} onChange={(e) => setNewTenantAddress(e.target.value)} />
          <input placeholder={t(lang, "phone")} value={newTenantPhone} onChange={(e) => setNewTenantPhone(e.target.value)} />
          <input placeholder={t(lang, "contactPerson")} value={newTenantContactPerson} onChange={(e) => setNewTenantContactPerson(e.target.value)} />
          <button onClick={createTenant}>{t(lang, "createTenant")}</button>
          {newTenantLogoUploading && <span style={{ fontSize: 12 }}>{t(lang, "uploading")}</span>}
        </div>
        <div style={{ marginTop: 10 }}>
          {tenants.map((tenant) => (
            <div key={tenant.id} style={{ display: "flex", gap: 8, alignItems: "center", padding: "4px 0", flexWrap: "wrap" }}>
              <strong>{tenant.name}</strong>
              <span>{tenant.isActive ? translate(lang, "active") : translate(lang, "inactive")}</span>
              {tenant.country && <span>{translate(lang, "country")}: {tenant.country}</span>}
              {tenant.phone && <span>{translate(lang, "phone")}: {tenant.phone}</span>}
              <button onClick={() => toggleTenant(tenant)}>{tenant.isActive ? translate(lang, "disable") : translate(lang, "enable")}</button>
              <button onClick={() => {
                setEditingTenantId(tenant.id);
                setEditTenantName(tenant.name ?? "");
                setEditTenantLogoUrl(tenant.logoUrl ?? "");
                setEditTenantCountry(tenant.country ?? "");
                setEditTenantAddress(tenant.address ?? "");
                setEditTenantPhone(tenant.phone ?? "");
                setEditTenantContactPerson(tenant.contactPerson ?? "");
              }}>{translate(lang, "editEntity")}</button>
              <button onClick={() => deleteTenant(tenant.id)}>{translate(lang, "delete")}</button>
            </div>
          ))}
        </div>
        {editingTenantId && (
          <div style={{ marginTop: 8, border: "1px dashed #ddd", padding: 10 }}>
            <div style={{ display: "flex", gap: 8, flexWrap: "wrap", alignItems: "center" }}>
              <input placeholder={t(lang, "newTenantName")} value={editTenantName} onChange={(e) => setEditTenantName(e.target.value)} />
              <label style={{ display: "flex", alignItems: "center", gap: 6 }}>
                {t(lang, "photoUpload")}
                <input
                  type="file"
                  accept="image/*"
                  onChange={async (e) => {
                    const file = e.target.files?.[0];
                    if (!file) return;
                    setEditTenantLogoUploading(true);
                    try {
                      const url = await uploadMediaFile(file, "logo");
                      setEditTenantLogoUrl(url);
                    } catch (err: any) {
                      setError(err?.message ?? "Upload error");
                    } finally {
                      setEditTenantLogoUploading(false);
                      e.currentTarget.value = "";
                    }
                  }}
                />
              </label>
              <input placeholder={t(lang, "logo")} value={editTenantLogoUrl} readOnly />
              <input placeholder={t(lang, "country")} value={editTenantCountry} onChange={(e) => setEditTenantCountry(e.target.value)} />
              <input placeholder={t(lang, "address")} value={editTenantAddress} onChange={(e) => setEditTenantAddress(e.target.value)} />
              <input placeholder={t(lang, "phone")} value={editTenantPhone} onChange={(e) => setEditTenantPhone(e.target.value)} />
              <input placeholder={t(lang, "contactPerson")} value={editTenantContactPerson} onChange={(e) => setEditTenantContactPerson(e.target.value)} />
              <button onClick={() => updateTenant(editingTenantId)}>{t(lang, "saveEntity")}</button>
              <button onClick={() => setEditingTenantId(null)}>{t(lang, "cancelEdit")}</button>
              {editTenantLogoUploading && <span style={{ fontSize: 12 }}>{t(lang, "uploading")}</span>}
            </div>
          </div>
        )}
      </section>

      <section style={{ marginTop: 24 }}>
        <h2>{t(lang, "restaurants")}</h2>
        <div style={{ display: "flex", gap: 8, flexWrap: "wrap", alignItems: "center" }}>
          <select value={tenantId} onChange={(e) => setTenantId(e.target.value ? Number(e.target.value) : "")}>
            <option value="">{t(lang, "selectTenant")}</option>
            {tenants.map((t) => (
              <option key={t.id} value={t.id}>{t.name}</option>
            ))}
          </select>
          <input placeholder={t(lang, "newRestaurantName")} value={newRestaurantName} onChange={(e) => setNewRestaurantName(e.target.value)} />
          <label style={{ display: "flex", alignItems: "center", gap: 6 }}>
            {t(lang, "photoUpload")}
            <input
              type="file"
              accept="image/*"
              onChange={async (e) => {
                const file = e.target.files?.[0];
                if (!file) return;
                setNewRestaurantLogoUploading(true);
                try {
                  const url = await uploadMediaFile(file, "logo");
                  setNewRestaurantLogoUrl(url);
                } catch (err: any) {
                  setError(err?.message ?? "Upload error");
                } finally {
                  setNewRestaurantLogoUploading(false);
                  e.currentTarget.value = "";
                }
              }}
            />
          </label>
          <input placeholder={t(lang, "logo")} value={newRestaurantLogoUrl} readOnly />
          <input placeholder={t(lang, "country")} value={newRestaurantCountry} onChange={(e) => setNewRestaurantCountry(e.target.value)} />
          <input placeholder={t(lang, "address")} value={newRestaurantAddress} onChange={(e) => setNewRestaurantAddress(e.target.value)} />
          <input placeholder={t(lang, "phone")} value={newRestaurantPhone} onChange={(e) => setNewRestaurantPhone(e.target.value)} />
          <input placeholder={t(lang, "contactPerson")} value={newRestaurantContactPerson} onChange={(e) => setNewRestaurantContactPerson(e.target.value)} />
          <button onClick={createRestaurant} disabled={!tenantId}>{t(lang, "createRestaurant")}</button>
          {newRestaurantLogoUploading && <span style={{ fontSize: 12 }}>{t(lang, "uploading")}</span>}
        </div>
        <div style={{ marginTop: 10 }}>
          {restaurants.filter((r) => !tenantId || r.tenantId === tenantId).map((r) => (
            <div key={r.id} style={{ display: "flex", gap: 8, alignItems: "center", padding: "4px 0", flexWrap: "wrap" }}>
              <strong>{r.name}</strong>
              <span>{t(lang, "tenant")} #{r.tenantId}</span>
              <span>{r.isActive ? t(lang, "active") : t(lang, "inactive")}</span>
              {r.country && <span>{t(lang, "country")}: {r.country}</span>}
              {r.phone && <span>{t(lang, "phone")}: {r.phone}</span>}
              <button onClick={() => toggleRestaurant(r)}>{r.isActive ? t(lang, "disable") : t(lang, "enable")}</button>
              <button onClick={() => {
                setEditingRestaurantId(r.id);
                setEditRestaurantName(r.name ?? "");
                setEditRestaurantLogoUrl(r.logoUrl ?? "");
                setEditRestaurantCountry(r.country ?? "");
                setEditRestaurantAddress(r.address ?? "");
                setEditRestaurantPhone(r.phone ?? "");
                setEditRestaurantContactPerson(r.contactPerson ?? "");
              }}>{t(lang, "editEntity")}</button>
              <button onClick={() => deleteRestaurant(r.id)}>{t(lang, "delete")}</button>
            </div>
          ))}
        </div>
        {editingRestaurantId && (
          <div style={{ marginTop: 8, border: "1px dashed #ddd", padding: 10 }}>
            <div style={{ display: "flex", gap: 8, flexWrap: "wrap", alignItems: "center" }}>
              <input placeholder={t(lang, "newRestaurantName")} value={editRestaurantName} onChange={(e) => setEditRestaurantName(e.target.value)} />
              <label style={{ display: "flex", alignItems: "center", gap: 6 }}>
                {t(lang, "photoUpload")}
                <input
                  type="file"
                  accept="image/*"
                  onChange={async (e) => {
                    const file = e.target.files?.[0];
                    if (!file) return;
                    setEditRestaurantLogoUploading(true);
                    try {
                      const url = await uploadMediaFile(file, "logo");
                      setEditRestaurantLogoUrl(url);
                    } catch (err: any) {
                      setError(err?.message ?? "Upload error");
                    } finally {
                      setEditRestaurantLogoUploading(false);
                      e.currentTarget.value = "";
                    }
                  }}
                />
              </label>
              <input placeholder={t(lang, "logo")} value={editRestaurantLogoUrl} readOnly />
              <input placeholder={t(lang, "country")} value={editRestaurantCountry} onChange={(e) => setEditRestaurantCountry(e.target.value)} />
              <input placeholder={t(lang, "address")} value={editRestaurantAddress} onChange={(e) => setEditRestaurantAddress(e.target.value)} />
              <input placeholder={t(lang, "phone")} value={editRestaurantPhone} onChange={(e) => setEditRestaurantPhone(e.target.value)} />
              <input placeholder={t(lang, "contactPerson")} value={editRestaurantContactPerson} onChange={(e) => setEditRestaurantContactPerson(e.target.value)} />
              <button onClick={() => updateRestaurant(editingRestaurantId)}>{t(lang, "saveEntity")}</button>
              <button onClick={() => setEditingRestaurantId(null)}>{t(lang, "cancelEdit")}</button>
              {editRestaurantLogoUploading && <span style={{ fontSize: 12 }}>{t(lang, "uploading")}</span>}
            </div>
          </div>
        )}
      </section>

      <section style={{ marginTop: 24 }}>
        <h2>{t(lang, "branches")}</h2>
        <div style={{ display: "flex", gap: 8, flexWrap: "wrap", alignItems: "center" }}>
          <select value={branchStatusFilter} onChange={(e) => setBranchStatusFilter(e.target.value as any)}>
            <option value="">{t(lang, "allStatuses")}</option>
            <option value="ACTIVE">{t(lang, "active")}</option>
            <option value="INACTIVE">{t(lang, "inactive")}</option>
          </select>
          <select value={newBranchRestaurantId} onChange={(e) => setNewBranchRestaurantId(e.target.value ? Number(e.target.value) : "")}>
            <option value="">{t(lang, "selectRestaurant")}</option>
            {restaurants.filter((r) => !tenantId || r.tenantId === tenantId).map((r) => (
              <option key={r.id} value={r.id}>{restaurantOptionLabel(r)}</option>
            ))}
          </select>
          <input placeholder={t(lang, "newBranchName")} value={newBranchName} onChange={(e) => setNewBranchName(e.target.value)} />
          <label style={{ display: "flex", alignItems: "center", gap: 6 }}>
            {t(lang, "photoUpload")}
            <input
              type="file"
              accept="image/*"
              onChange={async (e) => {
                const file = e.target.files?.[0];
                if (!file) return;
                setNewBranchLogoUploading(true);
                try {
                  const url = await uploadMediaFile(file, "logo");
                  setNewBranchLogoUrl(url);
                } catch (err: any) {
                  setError(err?.message ?? "Upload error");
                } finally {
                  setNewBranchLogoUploading(false);
                  e.currentTarget.value = "";
                }
              }}
            />
          </label>
          <input placeholder={t(lang, "logo")} value={newBranchLogoUrl} readOnly />
          <input placeholder={t(lang, "country")} value={newBranchCountry} onChange={(e) => setNewBranchCountry(e.target.value)} />
          <input placeholder={t(lang, "address")} value={newBranchAddress} onChange={(e) => setNewBranchAddress(e.target.value)} />
          <input placeholder={t(lang, "phone")} value={newBranchPhone} onChange={(e) => setNewBranchPhone(e.target.value)} />
          <input placeholder={t(lang, "contactPerson")} value={newBranchContactPerson} onChange={(e) => setNewBranchContactPerson(e.target.value)} />
          <button onClick={createBranch} disabled={!tenantId}>{t(lang, "createBranch")}</button>
          {newBranchLogoUploading && <span style={{ fontSize: 12 }}>{t(lang, "uploading")}</span>}
        </div>
        <div style={{ marginTop: 10 }}>
          {branches.filter((b) => (!tenantId || b.tenantId === tenantId) && (!branchRestaurantFilterId || b.restaurantId === branchRestaurantFilterId)).map((b) => (
            <div key={b.id} style={{ display: "flex", gap: 8, alignItems: "center", padding: "4px 0", flexWrap: "wrap" }}>
              <strong>{b.name}</strong>
              <span>{t(lang, "tenant")} #{b.tenantId}</span>
              <span>{t(lang, "restaurant")}: {restaurantLabel(b.restaurantId)}</span>
              <span>{b.isActive ? t(lang, "active") : t(lang, "inactive")}</span>
              {b.country && <span>{translate(lang, "country")}: {b.country}</span>}
              {b.phone && <span>{translate(lang, "phone")}: {b.phone}</span>}
              <button onClick={() => toggleBranch(b)}>{b.isActive ? t(lang, "disable") : t(lang, "enable")}</button>
              <button onClick={() => {
                setEditingBranchId(b.id);
                setEditBranchName(b.name ?? "");
                setEditBranchLogoUrl(b.logoUrl ?? "");
                setEditBranchCountry(b.country ?? "");
                setEditBranchAddress(b.address ?? "");
                setEditBranchPhone(b.phone ?? "");
                setEditBranchContactPerson(b.contactPerson ?? "");
                setEditBranchRestaurantId(b.restaurantId ?? "");
              }}>{t(lang, "editEntity")}</button>
              <button onClick={() => deleteBranch(b.id)}>{t(lang, "delete")}</button>
            </div>
          ))}
        </div>
        {editingBranchId && (
          <div style={{ marginTop: 8, border: "1px dashed #ddd", padding: 10 }}>
            <div style={{ display: "flex", gap: 8, flexWrap: "wrap", alignItems: "center" }}>
              <select value={editBranchRestaurantId} onChange={(e) => setEditBranchRestaurantId(e.target.value ? Number(e.target.value) : "")}>
                <option value="">{t(lang, "selectRestaurant")}</option>
                {restaurants.filter((r) => !tenantId || r.tenantId === tenantId).map((r) => (
                  <option key={r.id} value={r.id}>{restaurantOptionLabel(r)}</option>
                ))}
              </select>
              <input placeholder={t(lang, "newBranchName")} value={editBranchName} onChange={(e) => setEditBranchName(e.target.value)} />
              <label style={{ display: "flex", alignItems: "center", gap: 6 }}>
                {t(lang, "photoUpload")}
                <input
                  type="file"
                  accept="image/*"
                  onChange={async (e) => {
                    const file = e.target.files?.[0];
                    if (!file) return;
                    setEditBranchLogoUploading(true);
                    try {
                      const url = await uploadMediaFile(file, "logo");
                      setEditBranchLogoUrl(url);
                    } catch (err: any) {
                      setError(err?.message ?? "Upload error");
                    } finally {
                      setEditBranchLogoUploading(false);
                      e.currentTarget.value = "";
                    }
                  }}
                />
              </label>
              <input placeholder={t(lang, "logo")} value={editBranchLogoUrl} readOnly />
              <input placeholder={t(lang, "country")} value={editBranchCountry} onChange={(e) => setEditBranchCountry(e.target.value)} />
              <input placeholder={t(lang, "address")} value={editBranchAddress} onChange={(e) => setEditBranchAddress(e.target.value)} />
              <input placeholder={t(lang, "phone")} value={editBranchPhone} onChange={(e) => setEditBranchPhone(e.target.value)} />
              <input placeholder={t(lang, "contactPerson")} value={editBranchContactPerson} onChange={(e) => setEditBranchContactPerson(e.target.value)} />
              <button onClick={() => updateBranch(editingBranchId)}>{t(lang, "saveEntity")}</button>
              <button onClick={() => setEditingBranchId(null)}>{t(lang, "cancelEdit")}</button>
              {editBranchLogoUploading && <span style={{ fontSize: 12 }}>{t(lang, "uploading")}</span>}
            </div>
          </div>
        )}
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
              <option key={b.id} value={b.id}>{`${b.name} · ${tenantLabel(b.tenantId)} / ${restaurantLabel(b.restaurantId)}`}</option>
            ))}
          </select>
          <button onClick={loadBranchSettings} disabled={!branchId}>{t(lang, "load")}</button>
        </div>
        {branchSettings && (
          <div style={{ marginTop: 10, display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(240px, 1fr))", gap: 10, alignItems: "center" }}>
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
            <label>
              {t(lang, "timeZone")}
              <input
                list="tz-options"
                value={branchSettings.timeZone ?? ""}
                onChange={(e) => setBranchSettings({ ...branchSettings, timeZone: e.target.value })}
                placeholder="Europe/Chisinau"
              />
            </label>
            <datalist id="tz-options">
              {TIME_ZONE_OPTIONS.map((tz) => (
                <option key={tz} value={tz} />
              ))}
            </datalist>
            <label><input type="checkbox" checked={!!branchSettings.onlinePayEnabled} onChange={(e) => setBranchSettings({ ...branchSettings, onlinePayEnabled: e.target.checked })} /> {t(lang, "onlinePayEnabled")}</label>
            <label>
              {t(lang, "onlinePayProvider")}
              <select
                value={branchSettings.onlinePayProvider ?? ""}
                onChange={(e) => setBranchSettings({ ...branchSettings, onlinePayProvider: e.target.value })}
              >
                <option value="">{t(lang, "onlinePayProviderHint")}</option>
                <option value="MAIB">MAIB</option>
                <option value="PAYNET">Paynet</option>
                <option value="MIA">MIA</option>
              </select>
            </label>
            <label>
              {t(lang, "onlinePayCurrency")}
              <select
                value={branchSettings.onlinePayCurrencyCode ?? "MDL"}
                onChange={(e) => setBranchSettings({ ...branchSettings, onlinePayCurrencyCode: e.target.value })}
              >
                {currencies.map((c) => (
                  <option key={c.code} value={c.code}>
                    {c.code} — {c.name}
                  </option>
                ))}
                {currencies.length === 0 && <option value="MDL">MDL</option>}
              </select>
            </label>
            <label>
              {t(lang, "onlinePayRequestUrl")}
              <input value={branchSettings.onlinePayRequestUrl ?? ""} style={onlinePayFieldStyle(branchSettings.onlinePayRequestUrl)} onChange={(e) => setBranchSettings({ ...branchSettings, onlinePayRequestUrl: e.target.value })} />
              <div style={{ fontSize: 12, color: "#6b7280" }}>{t(lang, "onlinePayRequestUrlHelp")}</div>
              {onlinePayFieldError("url", branchSettings.onlinePayRequestUrl) && (
                <div style={{ fontSize: 12, color: "#b11e46" }}>{onlinePayFieldError("url", branchSettings.onlinePayRequestUrl)}</div>
              )}
            </label>
            <label>
              {t(lang, "onlinePayCacertPath")}
              <input value={branchSettings.onlinePayCacertPath ?? ""} style={onlinePayFieldStyle(branchSettings.onlinePayCacertPath)} onChange={(e) => setBranchSettings({ ...branchSettings, onlinePayCacertPath: e.target.value })} />
              <div style={{ fontSize: 12, color: "#6b7280" }}>{t(lang, "onlinePayCacertPathHelp")}</div>
              {onlinePayFieldError("pem", branchSettings.onlinePayCacertPath) && (
                <div style={{ fontSize: 12, color: "#b11e46" }}>{onlinePayFieldError("pem", branchSettings.onlinePayCacertPath)}</div>
              )}
            </label>
            <label>
              {t(lang, "onlinePayPcertPath")}
              <input value={branchSettings.onlinePayPcertPath ?? ""} style={onlinePayFieldStyle(branchSettings.onlinePayPcertPath)} onChange={(e) => setBranchSettings({ ...branchSettings, onlinePayPcertPath: e.target.value })} />
              <div style={{ fontSize: 12, color: "#6b7280" }}>{t(lang, "onlinePayPcertPathHelp")}</div>
              {onlinePayFieldError("pem", branchSettings.onlinePayPcertPath) && (
                <div style={{ fontSize: 12, color: "#b11e46" }}>{onlinePayFieldError("pem", branchSettings.onlinePayPcertPath)}</div>
              )}
            </label>
            <label>
              {t(lang, "onlinePayPcertPassword")}
              <input type="password" value={branchSettings.onlinePayPcertPassword ?? ""} style={onlinePayFieldStyle(branchSettings.onlinePayPcertPassword)} onChange={(e) => setBranchSettings({ ...branchSettings, onlinePayPcertPassword: e.target.value })} />
              <div style={{ fontSize: 12, color: "#6b7280" }}>{t(lang, "onlinePayPcertPasswordHelp")}</div>
            </label>
            <label>
              {t(lang, "onlinePayKeyPath")}
              <input value={branchSettings.onlinePayKeyPath ?? ""} style={onlinePayFieldStyle(branchSettings.onlinePayKeyPath)} onChange={(e) => setBranchSettings({ ...branchSettings, onlinePayKeyPath: e.target.value })} />
              <div style={{ fontSize: 12, color: "#6b7280" }}>{t(lang, "onlinePayKeyPathHelp")}</div>
              {onlinePayFieldError("pem", branchSettings.onlinePayKeyPath) && (
                <div style={{ fontSize: 12, color: "#b11e46" }}>{onlinePayFieldError("pem", branchSettings.onlinePayKeyPath)}</div>
              )}
            </label>
            <label>
              {t(lang, "onlinePayRedirectUrl")}
              <input value={branchSettings.onlinePayRedirectUrl ?? ""} style={onlinePayFieldStyle(branchSettings.onlinePayRedirectUrl)} onChange={(e) => setBranchSettings({ ...branchSettings, onlinePayRedirectUrl: e.target.value })} />
              <div style={{ fontSize: 12, color: "#6b7280" }}>{t(lang, "onlinePayRedirectUrlHelp")}</div>
              {onlinePayFieldError("url", branchSettings.onlinePayRedirectUrl) && (
                <div style={{ fontSize: 12, color: "#b11e46" }}>{onlinePayFieldError("url", branchSettings.onlinePayRedirectUrl)}</div>
              )}
            </label>
            <label>
              {t(lang, "onlinePayReturnUrl")}
              <input value={branchSettings.onlinePayReturnUrl ?? ""} style={onlinePayFieldStyle(branchSettings.onlinePayReturnUrl)} onChange={(e) => setBranchSettings({ ...branchSettings, onlinePayReturnUrl: e.target.value })} />
              <div style={{ fontSize: 12, color: "#6b7280" }}>{t(lang, "onlinePayReturnUrlHelp")}</div>
              {onlinePayFieldError("returnUrl", branchSettings.onlinePayReturnUrl) && (
                <div style={{ fontSize: 12, color: "#b11e46" }}>{onlinePayFieldError("returnUrl", branchSettings.onlinePayReturnUrl)}</div>
              )}
            </label>
            <label>
              {t(lang, "adminIpAllowlist")}
              <textarea
                rows={3}
                value={branchSettings.adminIpAllowlist ?? ""}
                onChange={(e) => setBranchSettings({ ...branchSettings, adminIpAllowlist: e.target.value })}
                placeholder="10.0.0.0/24, 203.0.113.5"
              />
              <div style={{ fontSize: 12, color: "#6b7280" }}>{t(lang, "adminIpListHelp")}</div>
              <div style={{ display: "flex", gap: 8, marginTop: 4 }}>
                <button
                  type="button"
                  onClick={() => {
                    if (!currentIp) return;
                    const next = `${branchSettings.adminIpAllowlist ?? ""}${branchSettings.adminIpAllowlist?.trim() ? ", " : ""}${currentIp}`;
                    setBranchSettings({ ...branchSettings, adminIpAllowlist: next });
                  }}
                >
                  {t(lang, "adminIpAddCurrent")}
                </button>
              </div>
              {invalidIpTokens(branchSettings.adminIpAllowlist).length > 0 && (
                <div style={{ fontSize: 12, color: "#b11e46", marginTop: 4 }}>
                  {t(lang, "adminIpInvalid")}: {invalidIpTokens(branchSettings.adminIpAllowlist).join(", ")}
                </div>
              )}
            </label>
            <label>
              {t(lang, "adminIpDenylist")}
              <textarea
                rows={3}
                value={branchSettings.adminIpDenylist ?? ""}
                onChange={(e) => setBranchSettings({ ...branchSettings, adminIpDenylist: e.target.value })}
                placeholder="0.0.0.0/0"
              />
              <div style={{ fontSize: 12, color: "#6b7280" }}>{t(lang, "adminIpListHelp")}</div>
              {invalidIpTokens(branchSettings.adminIpDenylist).length > 0 && (
                <div style={{ fontSize: 12, color: "#b11e46", marginTop: 4 }}>
                  {t(lang, "adminIpInvalid")}: {invalidIpTokens(branchSettings.adminIpDenylist).join(", ")}
                </div>
              )}
            </label>
            {currentIp && (
              <div style={{ fontSize: 12, color: isIpBlocked(currentIp, branchSettings.adminIpAllowlist, branchSettings.adminIpDenylist) ? "#b11e46" : "#065f46" }}>
                IP: {currentIp} {isIpBlocked(currentIp, branchSettings.adminIpAllowlist, branchSettings.adminIpDenylist) ? "— доступ будет заблокирован" : "— доступ разрешен"}
              </div>
            )}
            {branchSettings.onlinePayEnabled && missingOnlinePayFields().length > 0 && (
              <div style={{ color: "#b11e46", fontSize: 12 }}>
                {t(lang, "onlinePayProviderHint")}: {missingOnlinePayFields().join(", ")}
              </div>
            )}
            <button onClick={saveBranchSettings} disabled={branchSettings.onlinePayEnabled && missingOnlinePayFields().length > 0}>{t(lang, "save")}</button>
          </div>
        )}
      </section>

      <section style={{ marginTop: 24 }}>
        <h2>{t(lang, "staffGlobal")}</h2>
        <div style={{ display: "flex", gap: 8, flexWrap: "wrap", alignItems: "center" }}>
          <select value={branchId} onChange={(e) => setBranchId(e.target.value ? Number(e.target.value) : "")}>
            <option value="">{t(lang, "selectBranch")}</option>
            {branches.map((b) => (
              <option key={b.id} value={b.id}>{`${b.name} · ${tenantLabel(b.tenantId)} / ${restaurantLabel(b.restaurantId)}`}</option>
            ))}
          </select>
          <button onClick={loadStaff} disabled={!branchId}>{t(lang, "loadStaff")}</button>
        </div>
        <div style={{ marginTop: 10, display: "flex", gap: 8, flexWrap: "wrap" }}>
          <input placeholder={t(lang, "username")} value={newStaffUser} onChange={(e) => setNewStaffUser(e.target.value)} />
          <input placeholder={t(lang, "password")} value={newStaffPass} onChange={(e) => setNewStaffPass(e.target.value)} />
          <select value={newStaffRole} onChange={(e) => setNewStaffRole(e.target.value)}>
            <option value="WAITER">{roleLabel("WAITER")}</option>
            <option value="HOST">{roleLabel("HOST")}</option>
            <option value="KITCHEN">{roleLabel("KITCHEN")}</option>
            <option value="BAR">{roleLabel("BAR")}</option>
            <option value="CASHIER">{roleLabel("CASHIER")}</option>
            <option value="MARKETER">{roleLabel("MARKETER")}</option>
            <option value="ACCOUNTANT">{roleLabel("ACCOUNTANT")}</option>
            <option value="SUPPORT">{roleLabel("SUPPORT")}</option>
            <option value="ADMIN">{roleLabel("ADMIN")}</option>
            <option value="MANAGER">{roleLabel("MANAGER")}</option>
            <option value="OWNER">{roleLabel("OWNER")}</option>
          </select>
          <select value={newStaffHallId} onChange={(e) => setNewStaffHallId(e.target.value ? Number(e.target.value) : "")}>
            <option value="">{t(lang, "hall")} — {t(lang, "all")}</option>
            {halls.map((h) => (
              <option key={h.id} value={h.id}>{h.name}</option>
            ))}
          </select>
          <button onClick={createStaff} disabled={!branchId}>{t(lang, "createStaff")}</button>
        </div>
        <div style={{ marginTop: 8, display: "flex", gap: 8, flexWrap: "wrap", alignItems: "center" }}>
          <select value={staffProfileFilter} onChange={(e) => setStaffProfileFilter(e.target.value)}>
            <option value="">{t(lang, "profileFilter")}: {t(lang, "profileAny")}</option>
            <option value="FILLED">{t(lang, "profileFilled")}</option>
            <option value="EMPTY">{t(lang, "profileEmpty")}</option>
          </select>
          <span style={{ fontSize: 12, color: "#667085" }}>
            {t(lang, "filteredCount")}: {filteredStaff.length}
          </span>
        </div>
        <div style={{ marginTop: 10 }}>
          {filteredStaff.map((s) => (
            <div key={s.id} style={{ display: "flex", gap: 8, alignItems: "center", padding: "4px 0" }}>
              <strong>{s.username}</strong>
              <span>{roleLabel(s.role)}</span>
              {s.hallId && (
                <span style={{ color: "#6b7280" }}>
                  {t(lang, "hall")}: {halls.find((h) => h.id === s.hallId)?.name ?? `#${s.hallId}`}
                </span>
              )}
              <span>{s.isActive ? t(lang, "active") : t(lang, "inactive")}</span>
              <button onClick={() => {
                setEditingStaffId(s.id);
                setEditStaffRole(s.role);
                setEditStaffActive(s.isActive);
                setEditStaffHallId(s.hallId ?? "");
                setEditStaffFirstName(s.firstName ?? "");
                setEditStaffLastName(s.lastName ?? "");
                setEditStaffAge(s.age != null ? String(s.age) : "");
                setEditStaffGender(s.gender ?? "");
                setEditStaffPhotoUrl(s.photoUrl ?? "");
                setEditStaffRating(s.rating != null ? String(s.rating) : "");
                setEditStaffRecommended(!!s.recommended);
                setEditStaffExperienceYears(s.experienceYears != null ? String(s.experienceYears) : "");
                setEditStaffFavoriteItems(s.favoriteItems ?? "");
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
                <option value="WAITER">{roleLabel("WAITER")}</option>
                <option value="HOST">{roleLabel("HOST")}</option>
                <option value="KITCHEN">{roleLabel("KITCHEN")}</option>
                <option value="BAR">{roleLabel("BAR")}</option>
                <option value="CASHIER">{roleLabel("CASHIER")}</option>
                <option value="MARKETER">{roleLabel("MARKETER")}</option>
                <option value="ACCOUNTANT">{roleLabel("ACCOUNTANT")}</option>
                <option value="SUPPORT">{roleLabel("SUPPORT")}</option>
                <option value="ADMIN">{roleLabel("ADMIN")}</option>
                <option value="MANAGER">{roleLabel("MANAGER")}</option>
                <option value="OWNER">{roleLabel("OWNER")}</option>
                <option value="SUPER_ADMIN">{roleLabel("SUPER_ADMIN")}</option>
              </select>
              <select value={editStaffHallId} onChange={(e) => setEditStaffHallId(e.target.value ? Number(e.target.value) : "")}>
                <option value="">{t(lang, "hall")} — {t(lang, "all")}</option>
                {halls.map((h) => (
                  <option key={h.id} value={h.id}>{h.name}</option>
                ))}
              </select>
              <label><input type="checkbox" checked={editStaffActive} onChange={(e) => setEditStaffActive(e.target.checked)} /> {t(lang, "active")}</label>
              <input placeholder={t(lang, "firstName")} value={editStaffFirstName} onChange={(e) => setEditStaffFirstName(e.target.value)} />
              <input placeholder={t(lang, "lastName")} value={editStaffLastName} onChange={(e) => setEditStaffLastName(e.target.value)} />
              <input placeholder={t(lang, "age")} value={editStaffAge} onChange={(e) => setEditStaffAge(e.target.value)} style={{ width: 90 }} />
              <input placeholder={t(lang, "rating")} value={editStaffRating} onChange={(e) => setEditStaffRating(e.target.value)} style={{ width: 90 }} />
              <select value={editStaffGender} onChange={(e) => setEditStaffGender(e.target.value)}>
                <option value="">{t(lang, "gender")}</option>
                <option value="male">{t(lang, "genderMale")}</option>
                <option value="female">{t(lang, "genderFemale")}</option>
                <option value="other">{t(lang, "genderOther")}</option>
              </select>
              <input placeholder={t(lang, "experienceYears")} value={editStaffExperienceYears} onChange={(e) => setEditStaffExperienceYears(e.target.value)} style={{ width: 140 }} />
              <label style={{ display: "flex", alignItems: "center", gap: 6 }}>
                <input type="checkbox" checked={editStaffRecommended} onChange={(e) => setEditStaffRecommended(e.target.checked)} /> {t(lang, "recommended")}
              </label>
              <label style={{ display: "flex", alignItems: "center", gap: 6 }}>
                {t(lang, "photoUpload")}
                <input
                  type="file"
                  accept="image/*"
                  onChange={async (e) => {
                    const file = e.target.files?.[0];
                    if (!file) return;
                    setEditStaffPhotoUploading(true);
                    try {
                      const url = await uploadMediaFile(file, "staff");
                      setEditStaffPhotoUrl(url);
                    } catch (err: any) {
                      setError(err?.message ?? "Upload error");
                    } finally {
                      setEditStaffPhotoUploading(false);
                      e.currentTarget.value = "";
                    }
                  }}
                />
              </label>
              <input placeholder={t(lang, "photoUrl")} value={editStaffPhotoUrl} readOnly style={{ minWidth: 220 }} />
              {editStaffPhotoUploading && <span style={{ fontSize: 12 }}>{t(lang, "uploading")}</span>}
              <input placeholder={t(lang, "favoriteItems")} value={editStaffFavoriteItems} onChange={(e) => setEditStaffFavoriteItems(e.target.value)} style={{ minWidth: 220 }} />
              <button onClick={() => updateStaff(editingStaffId)}>{t(lang, "save")}</button>
              <button onClick={() => setEditingStaffId(null)}>{t(lang, "cancel")}</button>
            </div>
          </div>
        )}
      </section>

      <section style={{ marginTop: 24 }}>
        <h2>{t(lang, "rolesPermissionsTitle")}</h2>
        <div style={{ color: "#667085", fontSize: 12 }}>{t(lang, "permissionsHelp")}</div>
        <div style={{ marginTop: 10, overflowX: "auto" }}>
          <table style={{ width: "100%", borderCollapse: "collapse" }}>
            <thead>
              <tr>
                <th style={{ textAlign: "left", borderBottom: "1px solid #ddd", padding: "6px 4px" }}>{t(lang, "roleColumn")}</th>
                <th style={{ textAlign: "left", borderBottom: "1px solid #ddd", padding: "6px 4px" }}>{t(lang, "permissionsColumn")}</th>
              </tr>
            </thead>
            <tbody>
              {rolesMatrix.map((role) => (
                <tr key={role}>
                  <td style={{ padding: "6px 4px", borderBottom: "1px solid #f0f0f0" }}>{roleLabel(role)}</td>
                  <td style={{ padding: "6px 4px", borderBottom: "1px solid #f0f0f0", color: "#344054" }}>
                    {formatPermList(defaultPermsForRole(role))}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <div style={{ marginTop: 12, border: "1px solid #eee", borderRadius: 8, padding: 12 }}>
          <div style={{ display: "flex", gap: 8, flexWrap: "wrap", alignItems: "center" }}>
            <label style={{ display: "flex", alignItems: "center", gap: 6 }}>
              {t(lang, "selectStaff")}
              <select
                value={permissionsStaffId}
                onChange={(e) => {
                  const next = e.target.value ? Number(e.target.value) : "";
                  setPermissionsStaffId(next);
                  if (!next) {
                    setPermissionsOverride(false);
                    setPermissionsSelected([]);
                    setPermissionsMessage(null);
                  }
                }}
              >
                <option value="">{t(lang, "selectStaff")}</option>
                {staff.map((s) => (
                  <option key={s.id} value={s.id}>{s.username}</option>
                ))}
              </select>
            </label>
            <label style={{ display: "flex", alignItems: "center", gap: 6 }}>
              <input
                type="checkbox"
                checked={permissionsOverride}
                disabled={!permissionsStaffId}
                onChange={(e) => {
                  const next = e.target.checked;
                  setPermissionsOverride(next);
                  if (!next && permissionsStaffId) {
                    const su = staff.find((s) => s.id === permissionsStaffId);
                    if (su) setPermissionsSelected(defaultPermsForRole(su.role));
                  }
                }}
              />
              {t(lang, "permissionsOverride")}
            </label>
            <button onClick={saveStaffPermissions} disabled={!permissionsStaffId || permissionsSaving}>
              {permissionsSaving ? t(lang, "loading") : t(lang, "savePermissions")}
            </button>
            {permissionsMessage && <span style={{ color: "#065f46", fontSize: 12 }}>{permissionsMessage}</span>}
          </div>
          <div style={{ marginTop: 10, display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(220px, 1fr))", gap: 8 }}>
            {permissionOrder.map((perm) => (
              <label key={perm} style={{ display: "flex", gap: 6, alignItems: "center" }}>
                <input
                  type="checkbox"
                  checked={permissionsSelected.includes(perm)}
                  disabled={!permissionsStaffId || !permissionsOverride}
                  onChange={() => {
                    setPermissionsSelected((prev) => {
                      const next = prev.includes(perm)
                        ? prev.filter((p) => p !== perm)
                        : [...prev, perm];
                      return normalizePermList(next);
                    });
                  }}
                />
                {t(lang, permissionLabels[perm] ?? perm)}
              </label>
            ))}
          </div>
        </div>
      </section>

      <section style={{ marginTop: 24 }}>
        <h2>{t(lang, "deviceSessions")}</h2>
        <div style={{ display: "flex", gap: 8, flexWrap: "wrap", alignItems: "center" }}>
          <label style={{ display: "flex", alignItems: "center", gap: 6 }}>
            {t(lang, "deviceSessionFilterBranch")}
            <select value={deviceBranchId} onChange={(e) => setDeviceBranchId(e.target.value ? Number(e.target.value) : "")}>
              <option value="">{t(lang, "selectBranch")}</option>
              {branches.map((b) => (
                <option key={b.id} value={b.id}>{`${b.name} · ${tenantLabel(b.tenantId)} / ${restaurantLabel(b.restaurantId)}`}</option>
              ))}
            </select>
          </label>
          <label style={{ display: "flex", alignItems: "center", gap: 6 }}>
            {t(lang, "deviceSessionFilterStaff")}
            <select value={deviceStaffFilterId} onChange={(e) => setDeviceStaffFilterId(e.target.value ? Number(e.target.value) : "")}>
              <option value="">{t(lang, "profileAny")}</option>
              {staff.map((s) => (
                <option key={s.id} value={s.id}>{s.username}</option>
              ))}
            </select>
          </label>
          <label style={{ display: "flex", alignItems: "center", gap: 6 }}>
            <input
              type="checkbox"
              checked={deviceIncludeRevoked}
              onChange={(e) => setDeviceIncludeRevoked(e.target.checked)}
            />
            {t(lang, "deviceSessionIncludeRevoked")}
          </label>
          <button onClick={loadDeviceSessions} disabled={deviceLoading}>
            {deviceLoading ? t(lang, "loading") : t(lang, "deviceSessionRefresh")}
          </button>
          <button onClick={revokeDeviceSessionsFiltered} disabled={deviceLoading}>
            {t(lang, "deviceSessionRevokeFiltered")}
          </button>
        </div>
        {deviceError && <div style={{ color: "#b11e46", marginTop: 6 }}>{deviceError}</div>}
        <div style={{ marginTop: 10 }}>
          {deviceSessions.length === 0 ? (
            <div style={{ color: "#666" }}>{t(lang, "deviceSessionEmpty")}</div>
          ) : (
            <table style={{ width: "100%", borderCollapse: "collapse" }}>
              <thead>
                <tr>
                  <th style={{ textAlign: "left", borderBottom: "1px solid #ddd", padding: "6px 4px" }}>ID</th>
                  <th style={{ textAlign: "left", borderBottom: "1px solid #ddd", padding: "6px 4px" }}>{t(lang, "deviceSessionFilterStaff")}</th>
                  <th style={{ textAlign: "left", borderBottom: "1px solid #ddd", padding: "6px 4px" }}>{t(lang, "deviceSessionFilterBranch")}</th>
                  <th style={{ textAlign: "left", borderBottom: "1px solid #ddd", padding: "6px 4px" }}>{t(lang, "deviceSessionPlatform")}</th>
                  <th style={{ textAlign: "left", borderBottom: "1px solid #ddd", padding: "6px 4px" }}>{t(lang, "deviceSessionDevice")}</th>
                  <th style={{ textAlign: "left", borderBottom: "1px solid #ddd", padding: "6px 4px" }}>{t(lang, "deviceSessionToken")}</th>
                  <th style={{ textAlign: "left", borderBottom: "1px solid #ddd", padding: "6px 4px" }}>{t(lang, "deviceSessionCreated")}</th>
                  <th style={{ textAlign: "left", borderBottom: "1px solid #ddd", padding: "6px 4px" }}>{t(lang, "deviceSessionLastSeen")}</th>
                  <th style={{ textAlign: "left", borderBottom: "1px solid #ddd", padding: "6px 4px" }}>{t(lang, "deviceSessionRevoked")}</th>
                  <th style={{ textAlign: "left", borderBottom: "1px solid #ddd", padding: "6px 4px" }} />
                </tr>
              </thead>
              <tbody>
                {deviceSessions.map((d) => (
                  <tr key={d.id}>
                    <td style={{ padding: "6px 4px", borderBottom: "1px solid #f0f0f0" }}>{d.id}</td>
                    <td style={{ padding: "6px 4px", borderBottom: "1px solid #f0f0f0" }}>{d.username ?? `#${d.staffUserId}`}</td>
                    <td style={{ padding: "6px 4px", borderBottom: "1px solid #f0f0f0" }}>{d.branchId ?? "-"}</td>
                    <td style={{ padding: "6px 4px", borderBottom: "1px solid #f0f0f0" }}>{d.platform ?? "-"}</td>
                    <td style={{ padding: "6px 4px", borderBottom: "1px solid #f0f0f0" }}>{d.deviceName ?? d.deviceId ?? "-"}</td>
                    <td style={{ padding: "6px 4px", borderBottom: "1px solid #f0f0f0" }}>{d.tokenMasked ?? "-"}</td>
                    <td style={{ padding: "6px 4px", borderBottom: "1px solid #f0f0f0" }}>{d.createdAt ? new Date(d.createdAt).toLocaleString() : "-"}</td>
                    <td style={{ padding: "6px 4px", borderBottom: "1px solid #f0f0f0" }}>{d.lastSeenAt ? new Date(d.lastSeenAt).toLocaleString() : "-"}</td>
                    <td style={{ padding: "6px 4px", borderBottom: "1px solid #f0f0f0" }}>{d.revokedAt ? new Date(d.revokedAt).toLocaleString() : "-"}</td>
                    <td style={{ padding: "6px 4px", borderBottom: "1px solid #f0f0f0" }}>
                      <button onClick={() => revokeDeviceSession(d.id)} disabled={!!d.revokedAt}>
                        {t(lang, "deviceSessionRevoke")}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </section>

      <section style={{ marginTop: 24 }}>
        <h2>{t(lang, "floorPlanBranch")}</h2>
        <div style={{ display: "flex", gap: 8, flexWrap: "wrap", alignItems: "center" }}>
          <select value={branchId} onChange={(e) => setBranchId(e.target.value ? Number(e.target.value) : "")}>
            <option value="">{t(lang, "selectBranch")}</option>
            {branches.map((b) => (
              <option key={b.id} value={b.id}>{`${b.name} · ${tenantLabel(b.tenantId)} / ${restaurantLabel(b.restaurantId)}`}</option>
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
          {staff.filter((s) => isWaiterRole(s.role)).map((s) => (
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
          {staff.filter((s) => isWaiterRole(s.role)).length === 0 && (
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
          <label>
            {t(lang, "branch")}
            <select
              value={branchId}
              onChange={(e) => {
                const next = e.target.value ? Number(e.target.value) : "";
                setBranchId(next);
                if (!next) setHallId("");
              }}
            >
              <option value="">{t(lang, "all")}</option>
              {branches
                .filter((b) => !tenantId || b.tenantId === Number(tenantId))
                .map((b) => (
                  <option key={b.id} value={b.id}>{b.name}</option>
                ))}
            </select>
          </label>
          <label>
            {t(lang, "hall")}
            <select value={hallId} onChange={(e) => setHallId(e.target.value ? Number(e.target.value) : "")} disabled={!branchId}>
              <option value="">{t(lang, "all")}</option>
              {halls.map((h) => (
                <option key={h.id} value={h.id}>{h.name}</option>
              ))}
            </select>
          </label>
          <label>
            {t(lang, "orderStatus")}
            <select value={statsOrderStatus} onChange={(e) => setStatsOrderStatus(e.target.value)}>
              <option value="">{t(lang, "all")}</option>
              {ORDER_STATUS_OPTIONS.map((s) => (
                <option key={s} value={s}>{s}</option>
              ))}
            </select>
          </label>
          <label>
            {t(lang, "guestPhone")}
            <input value={statsGuestPhone} onChange={(e) => setStatsGuestPhone(e.target.value)} placeholder="+373..." />
          </label>
          <label>{t(lang, "shiftFrom")} <input type="date" value={statsShiftFrom} onChange={(e) => setStatsShiftFrom(e.target.value)} /></label>
          <label>{t(lang, "shiftTo")} <input type="date" value={statsShiftTo} onChange={(e) => setStatsShiftTo(e.target.value)} /></label>
          <button onClick={loadStats} disabled={!tenantId}>{t(lang, "load")}</button>
          <button onClick={downloadSummaryCsv} disabled={!tenantId}>{t(lang, "summaryCsv")}</button>
          <button onClick={downloadBranchesCsv} disabled={!tenantId}>{t(lang, "branchesCsv")}</button>
          <button onClick={downloadTopItemsCsv} disabled={!tenantId}>{t(lang, "topItemsCsv")}</button>
          <button onClick={downloadTopWaitersCsv} disabled={!tenantId}>{t(lang, "topWaitersCsv")}</button>
        </div>
        {stats && (
          <div style={{ marginTop: 10, border: "1px solid #eee", borderRadius: 8, padding: 10 }}>
            <div>{t(lang, "period")}: {stats.from} → {stats.to}</div>
            <div>{t(lang, "orders")}: {stats.ordersCount}</div>
            <div>{t(lang, "waiterCalls")}: {stats.callsCount}</div>
            <div>{t(lang, "paidBills")}: {stats.paidBillsCount}</div>
            <div>{t(lang, "gross")}: {money(stats.grossCents)}</div>
            <div>{t(lang, "tips")}: {money(stats.tipsCents)}</div>
            {stats.avgCheckCents != null && (
              <div>{t(lang, "avgCheck")}: {money(stats.avgCheckCents)}</div>
            )}
            {stats.avgSlaMinutes != null && (
              <div>{t(lang, "avgSla")}: {stats.avgSlaMinutes.toFixed(1)}</div>
            )}
            <div>{t(lang, "activeTables")}: {stats.activeTablesCount}</div>
            {stats.avgBranchRating != null && (
              <div>
                {t(lang, "branchReviewsAvg")}: {stats.avgBranchRating.toFixed(2)} • {t(lang, "branchReviewsCount")}: {stats.branchReviewsCount ?? 0}
              </div>
            )}
          </div>
        )}
        {branchStats.length > 0 && (
          <div style={{ marginTop: 12 }}>
            <h3>{t(lang, "byBranch")}</h3>
            <table style={{ width: "100%", borderCollapse: "collapse" }}>
              <thead>
                <tr>
                  <th style={{ textAlign: "left", borderBottom: "1px solid #ddd", padding: "6px 4px" }}>{t(lang, "branch")}</th>
                  <th style={{ textAlign: "left", borderBottom: "1px solid #ddd", padding: "6px 4px" }}>{t(lang, "restaurant")}</th>
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
                    <td style={{ padding: "6px 4px", borderBottom: "1px solid #f0f0f0" }}>{r.restaurantName ?? restaurantLabel(r.restaurantId)}</td>
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
        {(topItems.length > 0 || topWaiters.length > 0) && (
          <div style={{ marginTop: 12, display: "grid", gap: 12, gridTemplateColumns: "repeat(auto-fit, minmax(280px, 1fr))" }}>
            <div>
              <h3>{t(lang, "topItems")}</h3>
              <table style={{ width: "100%", borderCollapse: "collapse" }}>
                <thead>
                  <tr>
                    <th style={{ textAlign: "left", borderBottom: "1px solid #ddd", padding: "6px 4px" }}>{t(lang, "items")}</th>
                    <th style={{ textAlign: "right", borderBottom: "1px solid #ddd", padding: "6px 4px" }}>{t(lang, "qty")}</th>
                    <th style={{ textAlign: "right", borderBottom: "1px solid #ddd", padding: "6px 4px" }}>{t(lang, "gross")}</th>
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
                  {topItems.length === 0 && (
                    <tr>
                      <td colSpan={3} style={{ padding: "6px 4px", color: "#666" }}>{t(lang, "noData")}</td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
            <div>
              <h3>{t(lang, "topWaiters")}</h3>
              <table style={{ width: "100%", borderCollapse: "collapse" }}>
                <thead>
                  <tr>
                    <th style={{ textAlign: "left", borderBottom: "1px solid #ddd", padding: "6px 4px" }}>{t(lang, "waiter")}</th>
                    <th style={{ textAlign: "right", borderBottom: "1px solid #ddd", padding: "6px 4px" }}>{t(lang, "orders")}</th>
                    <th style={{ textAlign: "right", borderBottom: "1px solid #ddd", padding: "6px 4px" }}>{t(lang, "tips")}</th>
                    <th style={{ textAlign: "right", borderBottom: "1px solid #ddd", padding: "6px 4px" }}>{t(lang, "slaMinutes")}</th>
                  </tr>
                </thead>
                <tbody>
                  {topWaiters.map((r) => (
                    <tr key={r.staffUserId}>
                      <td style={{ padding: "6px 4px", borderBottom: "1px solid #f0f0f0" }}>{r.username} #{r.staffUserId}</td>
                      <td style={{ padding: "6px 4px", textAlign: "right", borderBottom: "1px solid #f0f0f0" }}>{r.ordersCount}</td>
                      <td style={{ padding: "6px 4px", textAlign: "right", borderBottom: "1px solid #f0f0f0" }}>{money(r.tipsCents)}</td>
                      <td style={{ padding: "6px 4px", textAlign: "right", borderBottom: "1px solid #f0f0f0" }}>{r.avgSlaMinutes == null ? "—" : r.avgSlaMinutes.toFixed(1)}</td>
                    </tr>
                  ))}
                  {topWaiters.length === 0 && (
                    <tr>
                      <td colSpan={4} style={{ padding: "6px 4px", color: "#666" }}>{t(lang, "noData")}</td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>
        )}
      </section>

      <section style={{ marginTop: 24 }}>
        <h2>{t(lang, "guestConsentTitle")}</h2>
        <GuestConsentLogs
          lang={lang}
          t={t}
          phone={guestConsentPhone}
          onPhoneChange={setGuestConsentPhone}
          consentType={guestConsentType}
          onConsentTypeChange={setGuestConsentType}
          accepted={guestConsentAccepted}
          onAcceptedChange={setGuestConsentAccepted}
          limit={guestConsentLimit}
          onLimitChange={setGuestConsentLimit}
          page={guestConsentPage}
          onPageChange={setGuestConsentPage}
          onLoad={loadGuestConsents}
          loading={guestConsentLoading}
          error={guestConsentError}
          logs={guestConsentLogs}
          extraFilters={(
            <label>
              {t(lang, "branch")}
              <select
                value={branchId}
                onChange={(e) => {
                  const next = e.target.value ? Number(e.target.value) : "";
                  setBranchId(next);
                  if (!next) setHallId("");
                }}
              >
                <option value="">{t(lang, "all")}</option>
                {branches
                  .filter((b) => !tenantId || b.tenantId === Number(tenantId))
                  .map((b) => (
                    <option key={b.id} value={b.id}>{b.name}</option>
                  ))}
              </select>
            </label>
          )}
        />
        <div style={{ marginTop: 10 }}>
          <div style={{ fontWeight: 600, marginBottom: 6 }}>{t(lang, "guestFlagsTitle")}</div>
          <div style={{ display: "flex", gap: 12, flexWrap: "wrap", alignItems: "center" }}>
            <label style={{ display: "flex", gap: 6, alignItems: "center", fontSize: 12 }}>
              <input type="checkbox" checked={guestFlagVip} onChange={(e) => setGuestFlagVip(e.target.checked)} />
              {t(lang, "guestFlagVip")}
            </label>
            <label style={{ display: "flex", gap: 6, alignItems: "center", fontSize: 12 }}>
              <input type="checkbox" checked={guestFlagNoShow} onChange={(e) => setGuestFlagNoShow(e.target.checked)} />
              {t(lang, "guestFlagNoShow")}
            </label>
            <label style={{ display: "flex", gap: 6, alignItems: "center", fontSize: 12 }}>
              <input type="checkbox" checked={guestFlagConflict} onChange={(e) => setGuestFlagConflict(e.target.checked)} />
              {t(lang, "guestFlagConflict")}
            </label>
            <button onClick={loadGuestFlagsSuper} style={{ padding: "6px 10px" }}>
              {t(lang, "load")}
            </button>
            <button onClick={saveGuestFlagsSuper} disabled={guestFlagsSaving} style={{ padding: "6px 10px" }}>
              {guestFlagsSaving ? t(lang, "saving") : t(lang, "guestFlagsSave")}
            </button>
            {guestFlagsSaved && <span style={{ color: "#059669", fontSize: 12 }}>{t(lang, "guestFlagsSaved")}</span>}
            {guestFlagsError && <span style={{ color: "#b11e46", fontSize: 12 }}>{guestFlagsError}</span>}
          </div>
          <div style={{ marginTop: 10 }}>
            <div style={{ fontWeight: 600, marginBottom: 6 }}>{t(lang, "guestFlagsHistory")}</div>
            {guestFlagHistory.length === 0 ? (
              <div style={{ fontSize: 12, color: "#666" }}>—</div>
            ) : (
              <div style={{ display: "grid", gap: 6 }}>
                {guestFlagHistory.map((h, idx) => (
                  <div key={`${h.flagType}-${h.changedAt ?? idx}`} style={{ fontSize: 12, display: "flex", gap: 10, flexWrap: "wrap" }}>
                    <span>{t(lang, "guestConsentType")}: {h.flagType}</span>
                    <span>old: {h.oldActive == null ? "—" : h.oldActive ? t(lang, "yes") : t(lang, "no")}</span>
                    <span>new: {h.newActive ? t(lang, "yes") : t(lang, "no")}</span>
                    <span>by: {h.changedByStaffId ?? "—"}</span>
                    <span>{h.changedAt ?? "—"}</span>
                  </div>
                ))}
              </div>
            )}
          </div>
          <div style={{ marginTop: 10 }}>
            <div style={{ fontWeight: 600, marginBottom: 6 }}>{t(lang, "guestFlagsBulkTitle")}</div>
            <div style={{ display: "grid", gap: 6 }}>
              <label>
                {t(lang, "guestFlagsBulkPhones")}
                <textarea rows={3} value={guestFlagBulkPhones} onChange={(e) => setGuestFlagBulkPhones(e.target.value)} />
              </label>
              <div style={{ display: "flex", gap: 8, flexWrap: "wrap", alignItems: "center" }}>
                <select value={guestFlagBulkType} onChange={(e) => setGuestFlagBulkType(e.target.value as any)}>
                  <option value="">{t(lang, "guestConsentType")}</option>
                  <option value="VIP">VIP</option>
                  <option value="NO_SHOW">NO_SHOW</option>
                  <option value="CONFLICT">CONFLICT</option>
                </select>
                <label style={{ display: "flex", gap: 6, alignItems: "center", fontSize: 12 }}>
                  <input type="checkbox" checked={guestFlagBulkActive} onChange={(e) => setGuestFlagBulkActive(e.target.checked)} />
                  {t(lang, "guestConsentAccepted")}
                </label>
                <button onClick={applyGuestFlagsBulkSuper} disabled={guestFlagBulkRunning}>
                  {guestFlagBulkRunning ? t(lang, "saving") : t(lang, "guestFlagsBulkApply")}
                </button>
                {guestFlagBulkMessage && <span style={{ fontSize: 12, color: "#6b7280" }}>{guestFlagBulkMessage}</span>}
              </div>
            </div>
          </div>
        </div>
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
