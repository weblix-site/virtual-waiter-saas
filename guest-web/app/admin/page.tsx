"use client";

import { Fragment, useCallback, useEffect, useMemo, useRef, useState, type DragEvent } from "react";
import Image from "next/image";

const API_BASE = process.env.NEXT_PUBLIC_API_BASE ?? "http://localhost:8080";

type Lang = "ru" | "ro" | "en";
const dict: Record<string, Record<Lang, string>> = {
  admin: { ru: "Админ", ro: "Admin", en: "Admin" },
  loginTitle: { ru: "Вход", ro: "Autentificare", en: "Login" },
  username: { ru: "Логин", ro: "Utilizator", en: "Username" },
  password: { ru: "Пароль", ro: "Parolă", en: "Password" },
  login: { ru: "Войти", ro: "Intră", en: "Login" },
  logout: { ru: "Выйти", ro: "Ieși", en: "Logout" },
  sessionExpired: { ru: "Сессия истекла. Войдите снова.", ro: "Sesiunea a expirat. Autentificați‑vă din nou.", en: "Session expired. Please sign in again." },
  refresh: { ru: "Обновить", ro: "Reîmprospătează", en: "Refresh" },
  resetFilters: { ru: "Сбросить фильтры", ro: "Resetează filtrele", en: "Reset filters" },
  filtersActive: { ru: "Фильтров активно", ro: "Filtre active", en: "Filters active" },
  settings: { ru: "Настройки", ro: "Setări", en: "Settings" },
  saveSettings: { ru: "Сохранить настройки", ro: "Salvează setările", en: "Save settings" },
  menu: { ru: "Меню", ro: "Meniu", en: "Menu" },
  categories: { ru: "Категории", ro: "Categorii", en: "Categories" },
  items: { ru: "Блюда", ro: "Produse", en: "Items" },
  staff: { ru: "Персонал", ro: "Personal", en: "Staff" },
  staffBulk: { ru: "Массовое редактирование", ro: "Editare în masă", en: "Bulk edit" },
  staffSelectAll: { ru: "Выбрать всех", ro: "Selectează tot", en: "Select all" },
  staffClearSelection: { ru: "Сбросить выбор", ro: "Resetează selecția", en: "Clear selection" },
  staffSelected: { ru: "Выбрано", ro: "Selectate", en: "Selected" },
  staffApplyBulk: { ru: "Применить к выбранным", ro: "Aplică la selectați", en: "Apply to selected" },
  staffNoSelection: { ru: "Не выбраны сотрудники", ro: "Nu sunt selectați", en: "No staff selected" },
  staffBulkRole: { ru: "Роль (массово)", ro: "Rol (în masă)", en: "Role (bulk)" },
  staffBulkActive: { ru: "Статус (массово)", ro: "Status (în masă)", en: "Status (bulk)" },
  staffBulkSkip: { ru: "Не менять", ro: "Nu schimba", en: "Do not change" },
  staffBulkConfirm: { ru: "Применить изменения ко всем выбранным?", ro: "Aplicați modificările pentru toți selectați?", en: "Apply changes to all selected?" },
  staffBulkDone: { ru: "Изменения применены", ro: "Modificările au fost aplicate", en: "Changes applied" },
  staffBulkError: { ru: "Ошибка массового изменения", ro: "Eroare la modificarea în masă", en: "Bulk update failed" },
  staffReviews: { ru: "Отзывы официантов", ro: "Recenzii chelneri", en: "Waiter reviews" },
  loadReviews: { ru: "Загрузить отзывы", ro: "Încarcă recenzii", en: "Load reviews" },
  noReviews: { ru: "Отзывов пока нет", ro: "Nu există recenzii", en: "No reviews yet" },
  branchReviews: { ru: "Отзывы о заведении", ro: "Recenzii despre local", en: "Venue reviews" },
  branchReviewsAvg: { ru: "Средний рейтинг", ro: "Rating mediu", en: "Average rating" },
  branchReviewsCount: { ru: "Кол-во отзывов", ro: "Număr recenzii", en: "Reviews count" },
  branchReviewsExport: { ru: "Экспорт отзывов", ro: "Export recenzii", en: "Export reviews" },
  branchReviewsDownload: { ru: "Скачать CSV", ro: "Descarcă CSV", en: "Download CSV" },
  chatExport: { ru: "Экспорт чатов", ro: "Export chat", en: "Chat export" },
  chatExportDownload: { ru: "Скачать CSV", ro: "Descarcă CSV", en: "Download CSV" },
  reviewRating: { ru: "Оценка", ro: "Rating", en: "Rating" },
  reviewComment: { ru: "Комментарий", ro: "Comentariu", en: "Comment" },
  reviewCreatedAt: { ru: "Дата", ro: "Data", en: "Date" },
  reviewGuest: { ru: "Сессия", ro: "Sesiune", en: "Session" },
  reviewTable: { ru: "Стол", ro: "Masă", en: "Table" },
  reviewWaiter: { ru: "Официант", ro: "Chelner", en: "Waiter" },
  reviewLimit: { ru: "Лимит", ro: "Limită", en: "Limit" },
  tables: { ru: "Столы", ro: "Mese", en: "Tables" },
  floorPlan: { ru: "План зала", ro: "Plan sală", en: "Floor Plan" },
  stats: { ru: "Статистика", ro: "Statistici", en: "Stats" },
  audit: { ru: "Аудит", ro: "Audit", en: "Audit" },
  add: { ru: "Добавить", ro: "Adaugă", en: "Add" },
  edit: { ru: "Редактировать", ro: "Editează", en: "Edit" },
  delete: { ru: "Удалить", ro: "Șterge", en: "Delete" },
  enable: { ru: "Включить", ro: "Activează", en: "Enable" },
  disable: { ru: "Выключить", ro: "Dezactivează", en: "Disable" },
  save: { ru: "Сохранить", ro: "Salvează", en: "Save" },
  cancel: { ru: "Отмена", ro: "Anulează", en: "Cancel" },
  statusActive: { ru: "Активный", ro: "Activ", en: "Active" },
  statusClosed: { ru: "Закрыт", ro: "Închis", en: "Closed" },
  requireOtpForFirstOrder: { ru: "OTP перед первым заказом", ro: "OTP înainte de prima comandă", en: "OTP before first order" },
  enablePartyPin: { ru: "Party PIN", ro: "PIN Party", en: "Party PIN" },
  allowPayOtherGuestsItems: { ru: "Оплата чужих позиций", ro: "Plată pentru alții", en: "Pay other guests items" },
  allowPayWholeTable: { ru: "Оплатить весь стол", ro: "Plătește toată masa", en: "Pay whole table" },
  tipsEnabled: { ru: "Чаевые включены", ro: "Bacșiș activ", en: "Tips enabled" },
  payCashEnabled: { ru: "Наличные доступны", ro: "Numerar activ", en: "Cash enabled" },
  payTerminalEnabled: { ru: "Терминал доступен", ro: "Terminal activ", en: "Terminal enabled" },
  currency: { ru: "Валюта", ro: "Valută", en: "Currency" },
  currencyManagedBySuperadmin: {
    ru: "Список валют управляется в Super Admin.",
    ro: "Lista valutelor este gestionată în Super Admin.",
    en: "Currency list is managed in Super Admin.",
  },
  defaultLanguage: { ru: "Язык по умолчанию (гость)", ro: "Limba implicită (oaspete)", en: "Default language (guest)" },
  langRu: { ru: "Русский", ro: "Rusă", en: "Russian" },
  langRo: { ru: "Румынский", ro: "Română", en: "Romanian" },
  langEn: { ru: "Английский", ro: "Engleză", en: "English" },
  otpTtlSeconds: { ru: "TTL OTP (сек)", ro: "TTL OTP (sec)", en: "OTP TTL (sec)" },
  otpMaxAttempts: { ru: "Лимит попыток OTP", ro: "Limită încercări OTP", en: "OTP max attempts" },
  otpResendCooldownSeconds: { ru: "Пауза OTP (сек)", ro: "Pauză OTP (sec)", en: "OTP resend cooldown (sec)" },
  otpLength: { ru: "Длина OTP", ro: "Lungime OTP", en: "OTP length" },
  otpDevEchoCode: { ru: "Показывать OTP в dev", ro: "Arată OTP în dev", en: "Show OTP in dev" },
  tipsPercentages: { ru: "Проценты чаевых (csv)", ro: "Procente bacșiș (csv)", en: "Tips percentages (csv)" },
  none: { ru: "Без", ro: "Fără", en: "None" },
  parties: { ru: "Группы (Party)", ro: "Party", en: "Parties" },
  status: { ru: "Статус", ro: "Status", en: "Status" },
  table: { ru: "Стол", ro: "Masă", en: "Table" },
  pin: { ru: "PIN", ro: "PIN", en: "PIN" },
  expiringMin: { ru: "Скоро истекает (мин)", ro: "Expiră curând (min)", en: "Expiring (min)" },
  expiring: { ru: "Скоро истекает", ro: "Expiră curând", en: "Expiring" },
  refreshParties: { ru: "Обновить", ro: "Reîmprospătează", en: "Refresh" },
  noParties: { ru: "Групп нет", ro: "Nu sunt party", en: "No parties" },
  participants: { ru: "Участники", ro: "Participanți", en: "Participants" },
  created: { ru: "Создано", ro: "Creat", en: "Created" },
  expires: { ru: "Истекает", ro: "Expiră", en: "Expires" },
  waiter: { ru: "Официант", ro: "Chelner", en: "Waiter" },
  all: { ru: "Все", ro: "Toate", en: "All" },
  topLimit: { ru: "Лимит топ", ro: "Limită top", en: "Top limit" },
  load: { ru: "Загрузить", ro: "Încarcă", en: "Load" },
  downloadCsv: { ru: "Скачать CSV", ro: "Descarcă CSV", en: "Download CSV" },
  period: { ru: "Период", ro: "Perioadă", en: "Period" },
  ordersCount: { ru: "Заказы", ro: "Comenzi", en: "Orders" },
  callsCount: { ru: "Вызовы", ro: "Apeluri", en: "Calls" },
  paidBills: { ru: "Оплаченные счета", ro: "Note plătite", en: "Paid bills" },
  gross: { ru: "Выручка", ro: "Brut", en: "Gross" },
  tips: { ru: "Чаевые", ro: "Bacșiș", en: "Tips" },
  motivation: { ru: "Мотивация официантов", ro: "Motivație chelneri", en: "Waiter motivation" },
  avgSla: { ru: "Средний SLA (мин)", ro: "SLA mediu (min)", en: "Avg SLA (min)" },
  badges: { ru: "Награды", ro: "Insigne", en: "Badges" },
  badgeTopOrders: { ru: "Топ по заказам", ro: "Top comenzi", en: "Top orders" },
  badgeTopTips: { ru: "Топ по чаевым", ro: "Top bacșiș", en: "Top tips" },
  badgeBestSla: { ru: "Лучший SLA", ro: "Cel mai bun SLA", en: "Best SLA" },
  activeTables: { ru: "Активные столы", ro: "Mese active", en: "Active tables" },
  day: { ru: "День", ro: "Zi", en: "Day" },
  qty: { ru: "Кол-во", ro: "Cant.", en: "Qty" },
  topItems: { ru: "Топ блюда", ro: "Top produse", en: "Top items" },
  topCategories: { ru: "Топ категории", ro: "Top categorii", en: "Top categories" },
  noData: { ru: "Нет данных", ro: "Fără date", en: "No data" },
  action: { ru: "Действие", ro: "Acțiune", en: "Action" },
  entity: { ru: "Сущность", ro: "Entitate", en: "Entity" },
  actor: { ru: "Пользователь", ro: "Utilizator", en: "Actor" },
  from: { ru: "С", ro: "De la", en: "From" },
  to: { ru: "По", ro: "Până la", en: "To" },
  beforeId: { ru: "До ID", ro: "Până la ID", en: "Before ID" },
  afterId: { ru: "После ID", ro: "După ID", en: "After ID" },
  limit: { ru: "Лимит", ro: "Limită", en: "Limit" },
  loading: { ru: "Загрузка...", ro: "Se încarcă...", en: "Loading..." },
  latest: { ru: "Последние", ro: "Ultimele", en: "Latest" },
  prevPage: { ru: "Назад", ro: "Înapoi", en: "Prev page" },
  nextPage: { ru: "Вперёд", ro: "Înainte", en: "Next page" },
  clear: { ru: "Очистить", ro: "Curăță", en: "Clear" },
  when: { ru: "Когда", ro: "Când", en: "When" },
  menuCategoryNameRu: { ru: "Название (RU)", ro: "Denumire (RU)", en: "Name (RU)" },
  menuCategoryNameRo: { ru: "Название (RO)", ro: "Denumire (RO)", en: "Name (RO)" },
  menuCategoryNameEn: { ru: "Название (EN)", ro: "Denumire (EN)", en: "Name (EN)" },
  sort: { ru: "Сортировка", ro: "Sortare", en: "Sort" },
  search: { ru: "Поиск", ro: "Căutare", en: "Search" },
  allCategories: { ru: "Все категории", ro: "Toate categoriile", en: "All categories" },
  allStatuses: { ru: "Все статусы", ro: "Toate statusurile", en: "All statuses" },
  stopListAny: { ru: "Стоп‑лист: любой", ro: "Stop‑listă: oricare", en: "Stop list: any" },
  stopList: { ru: "Стоп‑лист", ro: "Stop‑listă", en: "Stop list" },
  notInStopList: { ru: "Не в стоп‑листе", ro: "Nu în stop‑listă", en: "Not in stop list" },
  descRu: { ru: "Описание (RU)", ro: "Descriere (RU)", en: "Desc (RU)" },
  descRo: { ru: "Описание (RO)", ro: "Descriere (RO)", en: "Desc (RO)" },
  descEn: { ru: "Описание (EN)", ro: "Descriere (EN)", en: "Desc (EN)" },
  ingredientsRu: { ru: "Состав (RU)", ro: "Ingrediente (RU)", en: "Ingredients (RU)" },
  ingredientsRo: { ru: "Состав (RO)", ro: "Ingrediente (RO)", en: "Ingredients (RO)" },
  ingredientsEn: { ru: "Состав (EN)", ro: "Ingrediente (EN)", en: "Ingredients (EN)" },
  allergens: { ru: "Аллергены", ro: "Alergeni", en: "Allergens" },
  weight: { ru: "Вес", ro: "Greutate", en: "Weight" },
  tagsCsv: { ru: "Теги (csv)", ro: "Etichete (csv)", en: "Tags (csv)" },
  photosCsv: { ru: "Фото (csv)", ro: "Poze (csv)", en: "Photo URLs (csv)" },
  kcal: { ru: "Ккал", ro: "Kcal", en: "Kcal" },
  proteinG: { ru: "Белки (г)", ro: "Proteine (g)", en: "Protein (g)" },
  fatG: { ru: "Жиры (г)", ro: "Grăsimi (g)", en: "Fat (g)" },
  carbsG: { ru: "Углеводы (г)", ro: "Carbohidrați (g)", en: "Carbs (g)" },
  priceCents: { ru: "Цена (центы)", ro: "Preț (cenți)", en: "Price (cents)" },
  active: { ru: "Активно", ro: "Activ", en: "Active" },
  inactive: { ru: "Неактивно", ro: "Inactiv", en: "Inactive" },
  editMode: { ru: "Режим редактирования", ro: "Mod editare", en: "Edit mode" },
  snapToGrid: { ru: "Привязка к сетке", ro: "Aliniază la grilă", en: "Snap to grid" },
  preview: { ru: "Предпросмотр", ro: "Previzualizare", en: "Preview" },
  panMode: { ru: "Перемещение", ro: "Pan", en: "Pan mode" },
  fitToScreen: { ru: "Вписать в экран", ro: "Potrivește ecranul", en: "Fit to screen" },
  resetZoom: { ru: "Сбросить масштаб", ro: "Reset zoom", en: "Reset zoom" },
  resetPan: { ru: "Сбросить панораму", ro: "Reset pan", en: "Reset pan" },
  zoom: { ru: "Масштаб", ro: "Zoom", en: "Zoom" },
  showingLatest: { ru: "Показаны последние 20", ro: "Afișate ultimele 20", en: "Showing latest 20" },
  newHallName: { ru: "Новый зал", ro: "Sală nouă", en: "New hall name" },
  newPlanName: { ru: "Новый план", ro: "Plan nou", en: "New plan name" },
  planSort: { ru: "Сортировка плана", ro: "Sortare plan", en: "Plan sort" },
  addHall: { ru: "Добавить зал", ro: "Adaugă sală", en: "Add hall" },
  addPlan: { ru: "Добавить план", ro: "Adaugă plan", en: "Add plan" },
  quickSwitch: { ru: "Быстрое переключение", ro: "Comutare rapidă", en: "Quick switch" },
  templates: { ru: "Шаблоны", ro: "Șabloane", en: "Templates" },
  saveCurrent: { ru: "Сохранить текущий", ro: "Salvează curent", en: "Save current" },
  noTemplates: { ru: "Шаблонов нет", ro: "Nu sunt șabloane", en: "No templates" },
  legend: { ru: "Легенда", ro: "Legendă", en: "Legend" },
  noWaiters: { ru: "Нет официантов", ro: "Nu sunt chelneri", en: "No waiters" },
  backgroundUrl: { ru: "Фон (URL)", ro: "Fundal (URL)", en: "Background URL" },
  hallSettings: { ru: "Настройки зала", ro: "Setări sală", en: "Hall settings" },
  activate: { ru: "Активировать", ro: "Activează", en: "Activate" },
  deactivate: { ru: "Деактивировать", ro: "Dezactivează", en: "Deactivate" },
  fromDate: { ru: "С", ro: "De la", en: "From" },
  toDate: { ru: "По", ro: "Până la", en: "To" },
  hall: { ru: "Зал", ro: "Sală", en: "Hall" },
  plan: { ru: "План", ro: "Plan", en: "Plan" },
  planAvailableAfterHall: { ru: "План доступен после выбора зала", ro: "Planul e disponibil după alegerea sălii", en: "Plan available after selecting hall" },
  participantsLabel: { ru: "Участники (ID сессий)", ro: "Participanți (ID sesiuni)", en: "Participants (Session IDs)" },
  noParticipants: { ru: "Нет участников", ro: "Fără participanți", en: "No participants" },
  copyIds: { ru: "Копировать ID", ro: "Copiază ID", en: "Copy IDs" },
  auditId: { ru: "ID", ro: "ID", en: "ID" },
  auditActor: { ru: "Пользователь", ro: "Utilizator", en: "Actor" },
  auditAction: { ru: "Действие", ro: "Acțiune", en: "Action" },
  auditEntity: { ru: "Сущность", ro: "Entitate", en: "Entity" },
  auditActionPlaceholder: { ru: "CREATE/UPDATE/DELETE", ro: "CREATE/UPDATE/DELETE", en: "CREATE/UPDATE/DELETE" },
  auditEntityPlaceholder: { ru: "MenuItem/StaffUser", ro: "MenuItem/StaffUser", en: "MenuItem/StaffUser" },
  planSelectHall: { ru: "Выберите зал", ro: "Selectează sala", en: "Select hall" },
  planDefault: { ru: "План по умолчанию", ro: "Plan implicit", en: "Default plan" },
  setActivePlan: { ru: "Сделать активным", ro: "Setează activ", en: "Set active" },
  duplicatePlan: { ru: "Дублировать план", ro: "Duplică planul", en: "Duplicate plan" },
  deletePlan: { ru: "Удалить план", ro: "Șterge planul", en: "Delete plan" },
  autoLayout: { ru: "Авто‑раскладка", ro: "Auto aranjare", en: "Auto layout" },
  resetLayout: { ru: "Сброс раскладки", ro: "Resetare aranjare", en: "Reset layout" },
  snapLayout: { ru: "Привязать к сетке", ro: "Aliniază la grilă", en: "Snap layout" },
  saveLayout: { ru: "Сохранить раскладку", ro: "Salvează aranjarea", en: "Save layout" },
  exportJson: { ru: "Экспорт JSON", ro: "Export JSON", en: "Export JSON" },
  showHistory: { ru: "Показать историю", ro: "Arată istoricul", en: "Show history" },
  hideHistory: { ru: "Скрыть историю", ro: "Ascunde istoricul", en: "Hide history" },
  importJson: { ru: "Импорт JSON", ro: "Import JSON", en: "Import JSON" },
  applyLayouts: { ru: "Применить layout (с перезаписью)", ro: "Aplică layout (suprascrie)", en: "Apply layouts (overwrite)" },
  applyTables: { ru: "Применить столы (позиции)", ro: "Aplică mesele (poziții)", en: "Apply tables (positions)" },
  importHint: { ru: "Снимите “Применить столы”, чтобы импортировать только фон/зоны.", ro: "Debifați “Aplică mesele” pentru doar fundal/zone.", en: "Uncheck “Apply tables” to import background/zones only." },
  dragHint: { ru: "Перетаскивайте столы на плане. Клик по столу — редактирование.", ro: "Trage mesele pe plan. Click pe masă pentru editare.", en: "Drag tables on the map. Click a table to edit." },
  planHistory: { ru: "История плана", ro: "Istoric plan", en: "Plan history" },
  versionsCount: { ru: "версий", ro: "versiuni", en: "versions" },
  noVersions: { ru: "Пока нет версий.", ro: "Încă nu sunt versiuni.", en: "No versions yet." },
  restore: { ru: "Восстановить", ro: "Restabilește", en: "Restore" },
  restoreConfirm: { ru: "Восстановить эту версию? Текущий план будет перезаписан.", ro: "Restaurezi această versiune? Planul curent va fi suprascris.", en: "Restore this version? Current plan will be overwritten." },
  planHistoryLoading: { ru: "Загрузка...", ro: "Se încarcă...", en: "Loading..." },
  dayPlan: { ru: "День", ro: "Zi", en: "Day" },
  eveningPlan: { ru: "Вечер", ro: "Seară", en: "Evening" },
  banquetPlan: { ru: "Банкет", ro: "Banchet", en: "Banquet" },
  assignWaiterDrag: { ru: "Назначить официанта (перетащите на стол)", ro: "Atribuie chelner (trage pe masă)", en: "Assign waiter (drag onto table)" },
  noWaitersYet: { ru: "Официантов пока нет.", ro: "Încă nu sunt chelneri.", en: "No waiters yet." },
  tableSelected: { ru: "Стол №", ro: "Masă №", en: "Table #" },
  widthPercent: { ru: "Ширина (%)", ro: "Lățime (%)", en: "Width (%)" },
  heightPercent: { ru: "Высота (%)", ro: "Înălțime (%)", en: "Height (%)" },
  rotationDeg: { ru: "Поворот (°)", ro: "Rotire (°)", en: "Rotation (deg)" },
  zone: { ru: "Зона", ro: "Zonă", en: "Zone" },
  zonePlaceholder: { ru: "например, Терраса, Зал A", ro: "ex. Terasă, Sala A", en: "e.g. Terrace, Hall A" },
  resetShape: { ru: "Сброс формы", ro: "Reset formă", en: "Reset shape" },
  deleteHallConfirm: { ru: "Удалить зал? Это удалит все планы/шаблоны зала.", ro: "Ștergi sala? Asta va elimina toate planurile/șabloanele.", en: "Delete hall? This removes all plans/templates for the hall." },
  deleteHallBlocked: { ru: "Нельзя удалить зал: есть привязанные столы. Сначала перенесите столы в другой зал.", ro: "Nu se poate șterge sala: există mese alocate. Mutați mesele în altă sală.", en: "Cannot delete hall: tables are assigned. Move tables first." },
  deleteHallFailed: { ru: "Ошибка удаления зала", ro: "Eroare la ștergere sală", en: "Failed to delete hall" },
  zones: { ru: "Зоны", ro: "Zone", en: "Zones" },
  zoneWaiter: { ru: "Официант зоны", ro: "Chelner zonă", en: "Zone waiter" },
  zoneName: { ru: "Название зоны", ro: "Nume zonă", en: "Zone name" },
  addZone: { ru: "Добавить зону", ro: "Adaugă zonă", en: "Add zone" },
  qrNote: { ru: "QR содержит timestamp. Перегенерируйте перед печатью, если ссылка старая.", ro: "QR include timestamp. Regenerează înainte de tipărire dacă e vechi.", en: "QR links include a timestamp. Re-generate before printing if old." },
  tableNumber: { ru: "Стол №", ro: "Masă #", en: "Table #" },
  publicIdOptional: { ru: "Public ID (необязательно)", ro: "Public ID (opțional)", en: "Public ID (optional)" },
  assignWaiter: { ru: "Назначить официанта", ro: "Atribuie chelner", en: "Assign waiter" },
  addTable: { ru: "Добавить", ro: "Adaugă", en: "Add" },
  refreshAllQr: { ru: "Обновить все QR", ro: "Reînnoiește toate QR", en: "Refresh all QR" },
  filterTablePlaceholder: { ru: "Фильтр по № стола или publicId", ro: "Filtru după nr. masă sau publicId", en: "Filter by table # or publicId" },
  allWaiters: { ru: "Все официанты", ro: "Toți chelnerii", en: "All waiters" },
  allHalls: { ru: "Все залы", ro: "Toate sălile", en: "All halls" },
  allAssignments: { ru: "Все назначения", ro: "Toate asignările", en: "All assignments" },
  assigned: { ru: "Назначено", ro: "Atribuit", en: "Assigned" },
  bulkHall: { ru: "Массово зал", ro: "Sală în masă", en: "Bulk hall" },
  assignFilteredToHall: { ru: "Назначить отфильтрованные в зал", ro: "Atribuie filtratele în sală", en: "Assign filtered to hall" },
  noHall: { ru: "Без зала", ro: "Fără sală", en: "No hall" },
  noWaiter: { ru: "Без официанта", ro: "Fără chelner", en: "No waiter" },
  clearWaiter: { ru: "Очистить официанта", ro: "Elimină chelner", en: "Clear waiter" },
  qrUrl: { ru: "QR URL", ro: "QR URL", en: "QR URL" },
  showQr: { ru: "Показать QR", ro: "Arată QR", en: "Show QR" },
  refreshQr: { ru: "Обновить QR", ro: "Reînnoiește QR", en: "Refresh QR" },
  downloadQr: { ru: "Скачать QR", ro: "Descarcă QR", en: "Download QR" },
  staffUsername: { ru: "Логин", ro: "Utilizator", en: "Username" },
  staffPassword: { ru: "Пароль", ro: "Parolă", en: "Password" },
  staffProfile: { ru: "Профиль официанта", ro: "Profil chelner", en: "Waiter profile" },
  editProfile: { ru: "Профиль", ro: "Profil", en: "Profile" },
  firstName: { ru: "Имя", ro: "Prenume", en: "First name" },
  lastName: { ru: "Фамилия", ro: "Nume", en: "Last name" },
  age: { ru: "Возраст", ro: "Vârstă", en: "Age" },
  gender: { ru: "Пол", ro: "Gen", en: "Gender" },
  genderMale: { ru: "Мужской", ro: "Masculin", en: "Male" },
  genderFemale: { ru: "Женский", ro: "Feminin", en: "Female" },
  genderOther: { ru: "Другое", ro: "Altul", en: "Other" },
  photoUrl: { ru: "Фото (URL)", ro: "Foto (URL)", en: "Photo URL" },
  rating: { ru: "Рейтинг (0–5)", ro: "Rating (0–5)", en: "Rating (0–5)" },
  recommended: { ru: "Рекомендуемый", ro: "Recomandat", en: "Recommended" },
  experienceYears: { ru: "Стаж (лет)", ro: "Experiență (ani)", en: "Experience (years)" },
  favoriteItems: { ru: "Любимые блюда (через запятую)", ro: "Feluri preferate (separate prin virgulă)", en: "Favorite items (comma-separated)" },
  saveProfile: { ru: "Сохранить профиль", ro: "Salvează profilul", en: "Save profile" },
  invalidAge: { ru: "Неверный возраст (0–120)", ro: "Vârstă invalidă (0–120)", en: "Invalid age (0–120)" },
  invalidRating: { ru: "Неверный рейтинг (0–5)", ro: "Rating invalid (0–5)", en: "Invalid rating (0–5)" },
  invalidExperience: { ru: "Неверный стаж (0–80)", ro: "Experiență invalidă (0–80)", en: "Invalid experience (0–80)" },
  filterByUsername: { ru: "Фильтр по логину", ro: "Filtru după utilizator", en: "Filter by username" },
  allRoles: { ru: "Все роли", ro: "Toate rolurile", en: "All roles" },
  resetPassword: { ru: "Сбросить пароль", ro: "Resetează parola", en: "Reset password" },
  modifiers: { ru: "Модификаторы", ro: "Modificatori", en: "Modifiers" },
  groupNameRu: { ru: "Название группы (RU)", ro: "Nume grup (RU)", en: "Group name (RU)" },
  addGroup: { ru: "Добавить группу", ro: "Adaugă grup", en: "Add group" },
  loadOptions: { ru: "Загрузить опции", ro: "Încarcă opțiuni", en: "Load options" },
  optionRu: { ru: "Опция (RU)", ro: "Opțiune (RU)", en: "Option (RU)" },
  priceCentsShort: { ru: "Цена (центы)", ro: "Preț (cenți)", en: "Price (cents)" },
  addOption: { ru: "Добавить опцию", ro: "Adaugă opțiune", en: "Add option" },
  itemModifierGroups: { ru: "Блюдо → группы модификаторов", ro: "Produs → grupuri modificatori", en: "Item → Modifier Groups" },
  groupLabel: { ru: "Группа №", ro: "Grup #", en: "Group #" },
  required: { ru: "обязательно", ro: "obligatoriu", en: "required" },
  min: { ru: "мин", ro: "min", en: "min" },
  max: { ru: "макс", ro: "max", en: "max" },
  addGroupOption: { ru: "Добавить группу", ro: "Adaugă grup", en: "Add group" },
  waiterLabel: { ru: "Официант", ro: "Chelner", en: "Waiter" },
  unassigned: { ru: "Не назначено", ro: "Neatribuit", en: "Unassigned" },
  tableSettings: { ru: "Настройки стола", ro: "Setări masă", en: "Table settings" },
  switchToEdit: { ru: "Переключитесь в режим редактирования для drag & drop.", ro: "Treceți în modul editare pentru drag & drop.", en: "Switch to Edit mode to enable drag & drop." },
  shape: { ru: "Форма", ro: "Formă", en: "Shape" },
  shapeRound: { ru: "Круглая", ro: "Rotundă", en: "Round" },
  shapeRect: { ru: "Прямоугольная", ro: "Dreptunghiulară", en: "Rectangle" },
  confirmApplyLayouts: {
    ru: "Применение раскладки перезапишет текущие позиции/размеры столов для этого зала. Продолжить?",
    ro: "Aplicarea layout‑ului va suprascrie pozițiile/dimensiunile meselor pentru această sală. Continui?",
    en: "Apply layouts will overwrite current table positions/sizes for this hall. Continue?",
  },
  confirmApplyTemplate: {
    ru: "Применение шаблона перезапишет текущие позиции/размеры столов для этого зала. Продолжить?",
    ro: "Aplicarea șablonului va suprascrie pozițiile/dimensiunile meselor pentru această sală. Continui?",
    en: "Apply template will overwrite current table positions/sizes for this hall. Continue?",
  },
  confirmAutoLayoutAll: {
    ru: "Авто‑раскладка переразместит столы для ВСЕХ залов. Продолжить?",
    ro: "Auto‑aranjarea va repoziționa mesele pentru TOATE sălile. Continui?",
    en: "Auto layout will reposition tables for ALL halls. Continue?",
  },
  confirmAutoLayoutHall: {
    ru: "Авто‑раскладка переразместит столы для выбранного зала. Продолжить?",
    ro: "Auto‑aranjarea va repoziționa mesele pentru sala selectată. Continui?",
    en: "Auto layout will reposition tables for the selected hall. Continue?",
  },
  confirmResetLayoutAll: {
    ru: "Сброс раскладки перезапишет позиции столов для ВСЕХ залов. Продолжить?",
    ro: "Resetarea aranjării va suprascrie pozițiile meselor pentru TOATE sălile. Continui?",
    en: "Reset layout will overwrite table positions for ALL halls. Continue?",
  },
  confirmResetLayoutHall: {
    ru: "Сброс раскладки перезапишет позиции столов для выбранного зала. Продолжить?",
    ro: "Resetarea aranjării va suprascrie pozițiile meselor pentru sala selectată. Continui?",
    en: "Reset layout will overwrite table positions for the selected hall. Continue?",
  },
  confirmSnapLayoutAll: {
    ru: "Привязка к сетке изменит позиции столов для ВСЕХ залов. Продолжить?",
    ro: "Alinierea la grilă va ajusta pozițiile meselor pentru TOATE sălile. Continui?",
    en: "Snap layout will adjust table positions for ALL halls. Continue?",
  },
  confirmSnapLayoutHall: {
    ru: "Привязка к сетке изменит позиции столов для выбранного зала. Продолжить?",
    ro: "Alinierea la grilă va ajusta pozițiile meselor pentru sala selectată. Continui?",
    en: "Snap layout will adjust table positions for the selected hall. Continue?",
  },
  templateNamePrompt: { ru: "Название шаблона", ro: "Nume șablon", en: "Template name" },
  templateDefaultName: { ru: "Шаблон", ro: "Șablon", en: "Template" },
  importedPlanPrefix: { ru: "Импорт", ro: "Import", en: "Imported" },
};

const t = (lang: Lang, key: string) => dict[key]?.[lang] ?? key;

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

type StaffReview = {
  id: number;
  staffUserId: number;
  staffUsername: string;
  tableNumber?: number | null;
  guestSessionId: number;
  rating: number;
  comment?: string | null;
  createdAt?: string | null;
};

type BranchReview = {
  id: number;
  guestSessionId: number;
  rating: number;
  comment?: string | null;
  createdAt?: string | null;
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
  currencyCode?: string;
  defaultLang?: string;
};

type CurrencyDto = {
  code: string;
  name: string;
  symbol?: string | null;
  isActive: boolean;
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
  avgBranchRating?: number;
  branchReviewsCount?: number;
};

type StatsDailyRow = {
  day: string;
  ordersCount: number;
  callsCount: number;
  paidBillsCount: number;
  grossCents: number;
  tipsCents: number;
};

type WaiterMotivationRow = {
  staffUserId: number;
  username: string;
  ordersCount: number;
  tipsCents: number;
  avgSlaMinutes?: number | null;
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
type HallPlanVersionDto = {
  id: number;
  name: string;
  action?: string | null;
  createdAt?: string | null;
  createdByStaffId?: number | null;
};

function money(priceCents: number, currency = "MDL") {
  return `${(priceCents / 100).toFixed(2)} ${currency}`;
}

export default function AdminPage() {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [authReady, setAuthReady] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [sessionExpired, setSessionExpired] = useState(false);
  const redirectingRef = useRef(false);
  const [lang, setLang] = useState<Lang>("ru");
  const translate = t;

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
  const [planZones, setPlanZones] = useState<{ id: string; name: string; x: number; y: number; w: number; h: number; color: string; waiterId?: number | null }[]>([]);
  const planRef = useRef<HTMLDivElement | null>(null);
  const [dragWaiterId, setDragWaiterId] = useState<number | null>(null);
  const [dragOverTableId, setDragOverTableId] = useState<number | null>(null);
  const [planTemplates, setPlanTemplates] = useState<HallPlanTemplateDto[]>([]);
  const [planVersions, setPlanVersions] = useState<HallPlanVersionDto[]>([]);
  const [planVersionsOpen, setPlanVersionsOpen] = useState(false);
  const [planVersionsLoading, setPlanVersionsLoading] = useState(false);
  const [applyLayoutsOnImport, setApplyLayoutsOnImport] = useState(true);
  const [applyTablesOnImport, setApplyTablesOnImport] = useState(true);
  const [currencies, setCurrencies] = useState<CurrencyDto[]>([]);
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
  const [profileEditingId, setProfileEditingId] = useState<number | null>(null);
  const [profileDraft, setProfileDraft] = useState<{
    firstName: string;
    lastName: string;
    age: string;
    gender: string;
    photoUrl: string;
    rating: string;
    recommended: boolean;
    experienceYears: string;
    favoriteItems: string;
  }>({
    firstName: "",
    lastName: "",
    age: "",
    gender: "",
    photoUrl: "",
    rating: "",
    recommended: false,
    experienceYears: "",
    favoriteItems: "",
  });
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
  const [waiterMotivation, setWaiterMotivation] = useState<WaiterMotivationRow[]>([]);
  const [waiterMotivationLoading, setWaiterMotivationLoading] = useState(false);
  const [auditLogs, setAuditLogs] = useState<AuditLog[]>([]);
  const [auditLoading, setAuditLoading] = useState(false);
  const [auditBeforeId, setAuditBeforeId] = useState<number | "">("");
  const [auditAfterId, setAuditAfterId] = useState<number | "">("");
  const [auditAction, setAuditAction] = useState("");
  const [auditEntityType, setAuditEntityType] = useState("");
  const [staffReviews, setStaffReviews] = useState<StaffReview[]>([]);
  const [staffReviewLoading, setStaffReviewLoading] = useState(false);
  const [staffReviewWaiterId, setStaffReviewWaiterId] = useState<number | "">("");
  const [staffReviewLimit, setStaffReviewLimit] = useState(50);
  const [branchReviews, setBranchReviews] = useState<BranchReview[]>([]);
  const [branchReviewLoading, setBranchReviewLoading] = useState(false);
  const [branchReviewLimit, setBranchReviewLimit] = useState(50);
  const [branchReviewSummary, setBranchReviewSummary] = useState<{ avgRating: number; count: number } | null>(null);
  const [branchReviewTableId, setBranchReviewTableId] = useState<number | "">("");
  const [branchReviewHallId, setBranchReviewHallId] = useState<number | "">("");
  const [branchReviewExportFrom, setBranchReviewExportFrom] = useState("");
  const [branchReviewExportTo, setBranchReviewExportTo] = useState("");
  const [chatExportFrom, setChatExportFrom] = useState("");
  const [chatExportTo, setChatExportTo] = useState("");
  const [chatExportWaiterId, setChatExportWaiterId] = useState<number | "">("");
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
  const [staffSelectedIds, setStaffSelectedIds] = useState<number[]>([]);
  const [bulkRole, setBulkRole] = useState<string>("__SKIP__");
  const [bulkActive, setBulkActive] = useState<string>("__SKIP__");
  const [bulkFirstName, setBulkFirstName] = useState("");
  const [bulkLastName, setBulkLastName] = useState("");
  const [bulkAge, setBulkAge] = useState("");
  const [bulkGender, setBulkGender] = useState("");
  const [bulkPhotoUrl, setBulkPhotoUrl] = useState("");
  const [bulkApplying, setBulkApplying] = useState(false);
  const [bulkProgress, setBulkProgress] = useState(0);
  const [newModGroupNameRu, setNewModGroupNameRu] = useState("");
  const [newModOptionNameRu, setNewModOptionNameRu] = useState("");
  const [newModOptionPrice, setNewModOptionPrice] = useState(0);
  const [activeModGroupId, setActiveModGroupId] = useState<number | null>(null);

  useEffect(() => {
    const u = localStorage.getItem("adminUser") ?? "";
    const exp = localStorage.getItem("partyExpiringMinutes");
    const l = (localStorage.getItem("adminLang") ?? "ru") as Lang;
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
    if (u) {
      setUsername(u);
      setAuthReady(true);
    }
    if (l === "ru" || l === "ro" || l === "en") {
      setLang(l);
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

  useEffect(() => {
    localStorage.setItem("adminLang", lang);
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
        localStorage.removeItem("adminUser");
        setAuthReady(false);
        setSessionExpired(true);
        if (!redirectingRef.current && typeof window !== "undefined") {
          redirectingRef.current = true;
          window.location.href = "/admin";
        }
      }
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

  const snapValue = (v: number, step = 2) => Math.round(v / step) * step;

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
      const [catsRes, itemsRes, tablesRes, staffRes, settingsRes, modGroupsRes, partiesRes, hallsRes, currenciesRes] = await Promise.all([
        api("/api/admin/menu/categories"),
        api("/api/admin/menu/items"),
        api("/api/admin/tables"),
        api("/api/admin/staff"),
        api("/api/admin/branch-settings"),
        api("/api/admin/modifier-groups"),
        api(`/api/admin/parties?status=${encodeURIComponent(partyStatusFilter)}`),
        api("/api/admin/halls"),
        api("/api/admin/currencies"),
      ]);
      setCategories(await catsRes.json());
      setItems(await itemsRes.json());
      setTables(await tablesRes.json());
      setStaff(await staffRes.json());
      const settingsBody = await settingsRes.json();
      setSettings(settingsBody);
      setModGroups(await modGroupsRes.json());
      setParties(await partiesRes.json());
      const hallsBody = await hallsRes.json();
      setHalls(hallsBody);
      setCurrencies(await currenciesRes.json());
      if (!hallId && hallsBody.length > 0) {
        setHallId(hallsBody[0].id);
      }
      if (settingsBody?.currencyCode && newItemCurrency === "MDL") {
        setNewItemCurrency(settingsBody.currencyCode);
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
    if (!authReady || !hallPlanId) {
      setPlanVersions([]);
      return;
    }
    setPlanVersionsLoading(true);
    api(`/api/admin/hall-plans/${hallPlanId}/versions`)
      .then((r) => r.json())
      .then((body) => setPlanVersions(Array.isArray(body) ? body : []))
      .catch(() => setPlanVersions([]))
      .finally(() => setPlanVersionsLoading(false));
  }, [authReady, hallPlanId]);

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
    setSessionExpired(false);
    try {
      await api("/api/auth/login", {
        method: "POST",
        body: JSON.stringify({ username, password }),
      });
      localStorage.setItem("adminUser", username);
      setAuthReady(true);
      loadAll();
    } catch (e: any) {
      setError(e?.message ?? "Auth error");
    }
  }

  async function logout() {
    try {
      await api("/api/auth/logout", { method: "POST" });
    } finally {
      localStorage.removeItem("adminUser");
      setUsername("");
      setPassword("");
      setAuthReady(false);
      setError(null);
      setSessionExpired(false);
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
    setNewItemCurrency(settings?.currencyCode ?? "MDL");
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

  async function startEditItem(it: MenuItem) {
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

  async function importPlanJson(file: File, applyLayouts = true, applyTables = true) {
    if (!hallId) return;
    if (applyLayouts) {
      const ok = window.confirm(t(lang, "confirmApplyLayouts"));
      if (!ok) return;
    }
    const text = await file.text();
    const parsed = JSON.parse(text);
    const name = parsed.name || `${t(lang, "importedPlanPrefix")} ${new Date().toISOString()}`;
    const payload = {
      name,
      backgroundUrl: parsed.backgroundUrl ?? "",
      zonesJson: parsed.zonesJson ?? "",
      tables: parsed.tables ?? [],
      applyLayouts,
      applyTables,
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

  async function applyTemplate(tpl: HallPlanTemplateDto) {
    if (!hallId) return;
    const ok = window.confirm(t(lang, "confirmApplyTemplate"));
    if (!ok) return;
    let payload: any = null;
    try {
      payload = JSON.parse(tpl.payloadJson);
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
    const ok = window.confirm(
      hallId === "" ? t(lang, "confirmAutoLayoutAll") : t(lang, "confirmAutoLayoutHall")
    );
    if (!ok) return;
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

  function resetLayouts() {
    const ok = window.confirm(
      hallId === "" ? t(lang, "confirmResetLayoutAll") : t(lang, "confirmResetLayoutHall")
    );
    if (!ok) return;
    setTables((prev) =>
      prev.map((t, idx) => {
        if (hallId !== "" && t.hallId !== hallId) return t;
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

  function snapLayouts() {
    const ok = window.confirm(
      hallId === "" ? t(lang, "confirmSnapLayoutAll") : t(lang, "confirmSnapLayoutHall")
    );
    if (!ok) return;
    setTables((prev) =>
      prev.map((t, idx) => {
        if (hallId !== "" && t.hallId !== hallId) return t;
        const l = getTableLayout(t, idx);
        return {
          ...t,
          layoutX: snapValue(l.layoutX),
          layoutY: snapValue(l.layoutY),
          layoutW: snapValue(l.layoutW),
          layoutH: snapValue(l.layoutH),
          layoutShape: l.layoutShape,
          layoutRotation: l.layoutRotation,
          layoutZone: l.layoutZone,
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

  async function applyStaffBulk() {
    if (staffSelectedIds.length === 0) {
      alert(t(lang, "staffNoSelection"));
      return;
    }
    const ok = window.confirm(t(lang, "staffBulkConfirm"));
    if (!ok) return;
    const patch: any = {};
    if (bulkRole !== "__SKIP__") patch.role = bulkRole;
    if (bulkActive !== "__SKIP__") patch.isActive = bulkActive === "ACTIVE";
    if (bulkFirstName.trim()) patch.firstName = bulkFirstName.trim();
    if (bulkLastName.trim()) patch.lastName = bulkLastName.trim();
    if (bulkAge.trim()) patch.age = Number(bulkAge);
    if (bulkGender) patch.gender = bulkGender;
    if (bulkPhotoUrl.trim()) patch.photoUrl = bulkPhotoUrl.trim();
    if (Object.keys(patch).length === 0) return;
    setBulkApplying(true);
    setBulkProgress(0);
    try {
      const res = await api(`/api/admin/staff/bulk`, {
        method: "POST",
        body: JSON.stringify({ ids: staffSelectedIds, patch }),
      });
      const body = await res.json().catch(() => ({}));
      setBulkProgress(body?.updated ?? staffSelectedIds.length);
      alert(t(lang, "staffBulkDone"));
      setBulkRole("__SKIP__");
      setBulkActive("__SKIP__");
      setBulkFirstName("");
      setBulkLastName("");
      setBulkAge("");
      setBulkGender("");
      setBulkPhotoUrl("");
      setStaffSelectedIds([]);
      loadAll();
    } catch (e: any) {
      alert(e?.message ?? t(lang, "staffBulkError"));
    } finally {
      setBulkApplying(false);
    }
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

  function editStaffProfile(su: StaffUser) {
    setProfileEditingId(su.id);
    setProfileDraft({
      firstName: su.firstName ?? "",
      lastName: su.lastName ?? "",
      age: su.age != null ? String(su.age) : "",
      gender: su.gender ?? "",
      photoUrl: su.photoUrl ?? "",
      rating: su.rating != null ? String(su.rating) : "",
      recommended: !!su.recommended,
      experienceYears: su.experienceYears != null ? String(su.experienceYears) : "",
      favoriteItems: su.favoriteItems ?? "",
    });
  }

  async function saveStaffProfile(su: StaffUser) {
    const ageVal = profileDraft.age.trim() ? Number(profileDraft.age.trim()) : null;
    if (ageVal != null && (Number.isNaN(ageVal) || ageVal < 0 || ageVal > 120)) {
      alert(t(lang, "invalidAge"));
      return;
    }
    const ratingVal = profileDraft.rating.trim() ? Number(profileDraft.rating.trim()) : null;
    if (ratingVal != null && (Number.isNaN(ratingVal) || ratingVal < 0 || ratingVal > 5)) {
      alert(t(lang, "invalidRating"));
      return;
    }
    const expVal = profileDraft.experienceYears.trim() ? Number(profileDraft.experienceYears.trim()) : null;
    if (expVal != null && (Number.isNaN(expVal) || expVal < 0 || expVal > 80)) {
      alert(t(lang, "invalidExperience"));
      return;
    }
    await api(`/api/admin/staff/${su.id}`, {
      method: "PATCH",
      body: JSON.stringify({
        firstName: profileDraft.firstName,
        lastName: profileDraft.lastName,
        age: Number.isNaN(ageVal) ? null : ageVal,
        gender: profileDraft.gender,
        photoUrl: profileDraft.photoUrl,
        rating: Number.isNaN(ratingVal) ? null : ratingVal,
        recommended: profileDraft.recommended,
        experienceYears: Number.isNaN(expVal) ? null : expVal,
        favoriteItems: profileDraft.favoriteItems,
      }),
    });
    setProfileEditingId(null);
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
        currencyCode: settings.currencyCode,
        defaultLang: settings.defaultLang,
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
    await loadWaiterMotivation(qs);
  }

  async function loadWaiterMotivation(qs?: URLSearchParams) {
    setWaiterMotivationLoading(true);
    try {
      const params = qs ? new URLSearchParams(qs.toString()) : new URLSearchParams();
      if (!qs) {
        if (statsFrom) params.set("from", statsFrom);
        if (statsTo) params.set("to", statsTo);
        if (statsHallId !== "") params.set("hallId", String(statsHallId));
        if (statsHallPlanId !== "") params.set("planId", String(statsHallPlanId));
      }
      const res = await api(`/api/admin/stats/waiters-motivation?${params.toString()}`);
      setWaiterMotivation(await res.json());
    } finally {
      setWaiterMotivationLoading(false);
    }
  }

  async function loadStaffReviews() {
    setStaffReviewLoading(true);
    try {
      const params = new URLSearchParams();
      if (staffReviewWaiterId) params.set("staffUserId", String(staffReviewWaiterId));
      if (staffReviewLimit) params.set("limit", String(staffReviewLimit));
      const res = await api(`/api/admin/staff-reviews?${params.toString()}`);
      setStaffReviews(await res.json());
    } finally {
      setStaffReviewLoading(false);
    }
  }

  async function loadBranchReviews() {
    setBranchReviewLoading(true);
    try {
      const params = new URLSearchParams();
      if (branchReviewLimit) params.set("limit", String(branchReviewLimit));
      if (branchReviewTableId !== "") params.set("tableId", String(branchReviewTableId));
      if (branchReviewHallId !== "") params.set("hallId", String(branchReviewHallId));
      const res = await api(`/api/admin/branch-reviews?${params.toString()}`);
      setBranchReviews(await res.json());
      const summaryRes = await api(`/api/admin/branch-reviews/summary?${params.toString()}`);
      setBranchReviewSummary(await summaryRes.json());
    } finally {
      setBranchReviewLoading(false);
    }
  }

  async function downloadBranchReviewsCsv() {
    const qs = new URLSearchParams();
    if (branchReviewExportFrom) qs.set("from", branchReviewExportFrom);
    if (branchReviewExportTo) qs.set("to", branchReviewExportTo);
    if (branchReviewTableId !== "") qs.set("tableId", String(branchReviewTableId));
    if (branchReviewHallId !== "") qs.set("hallId", String(branchReviewHallId));
    const res = await api(`/api/admin/branch-reviews/export.csv?${qs.toString()}`);
    const text = await res.text();
    const blob = new Blob([text], { type: "text/csv" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = "branch-reviews.csv";
    a.click();
    URL.revokeObjectURL(url);
  }

  async function downloadCsv() {
    const qs = new URLSearchParams();
    if (statsFrom) qs.set("from", statsFrom);
    if (statsTo) qs.set("to", statsTo);
    if (statsTableId !== "") qs.set("tableId", String(statsTableId));
    if (statsHallId !== "") qs.set("hallId", String(statsHallId));
    if (statsHallPlanId !== "") qs.set("planId", String(statsHallPlanId));
    if (statsWaiterId !== "") qs.set("waiterId", String(statsWaiterId));
    const res = await api(`/api/admin/stats/daily.csv?${qs.toString()}`);
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

  async function downloadChatCsv() {
    const qs = new URLSearchParams();
    if (chatExportFrom) qs.set("from", chatExportFrom);
    if (chatExportTo) qs.set("to", chatExportTo);
    if (chatExportWaiterId !== "") qs.set("waiterId", String(chatExportWaiterId));
    const res = await api(`/api/admin/chat/export.csv?${qs.toString()}`);
    const text = await res.text();
    const blob = new Blob([text], { type: "text/csv" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = "chat-export.csv";
    a.click();
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
    const res = await api(`/api/admin/audit-logs.csv?${qs.toString()}`);
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
        <h1>{t(lang, "loginTitle")}</h1>
        {sessionExpired && <div style={{ color: "#b11e46" }}>{t(lang, "sessionExpired")}</div>}
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
    <main style={{ padding: 16, maxWidth: 1100, margin: "0 auto", fontFamily: "system-ui, -apple-system, Segoe UI, Roboto" }}>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
        <h1 style={{ margin: 0 }}>{t(lang, "admin")}</h1>
        <div style={{ display: "flex", gap: 8, alignItems: "center" }}>
          <button onClick={() => setLang("ru")}>RU</button>
          <button onClick={() => setLang("ro")}>RO</button>
          <button onClick={() => setLang("en")}>EN</button>
          <button onClick={loadAll}>{t(lang, "refresh")}</button>
          <button onClick={logout}>{t(lang, "logout")}</button>
        </div>
      </div>
      <div style={{ marginTop: 8, display: "flex", alignItems: "center", gap: 10 }}>
        <button onClick={resetAllFilters}>{t(lang, "resetFilters")}</button>
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
            {t(lang, "filtersActive")}: {adminFiltersCount}
          </span>
        )}
      </div>
      {error && <div style={{ color: "#b11e46", marginTop: 8 }}>{error}</div>}

      <section style={{ marginTop: 24 }}>
        <h2>{t(lang, "settings")}</h2>
        {settings && (
          <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(260px, 1fr))", gap: 12 }}>
            <label><input type="checkbox" checked={settings.requireOtpForFirstOrder} onChange={(e) => setSettings({ ...settings, requireOtpForFirstOrder: e.target.checked })} /> {t(lang, "requireOtpForFirstOrder")}</label>
            <label><input type="checkbox" checked={settings.enablePartyPin} onChange={(e) => setSettings({ ...settings, enablePartyPin: e.target.checked })} /> {t(lang, "enablePartyPin")}</label>
            <label><input type="checkbox" checked={settings.allowPayOtherGuestsItems} onChange={(e) => setSettings({ ...settings, allowPayOtherGuestsItems: e.target.checked })} /> {t(lang, "allowPayOtherGuestsItems")}</label>
            <label><input type="checkbox" checked={settings.allowPayWholeTable} onChange={(e) => setSettings({ ...settings, allowPayWholeTable: e.target.checked })} /> {t(lang, "allowPayWholeTable")}</label>
            <label><input type="checkbox" checked={settings.tipsEnabled} onChange={(e) => setSettings({ ...settings, tipsEnabled: e.target.checked })} /> {t(lang, "tipsEnabled")}</label>
            <label><input type="checkbox" checked={settings.payCashEnabled} onChange={(e) => setSettings({ ...settings, payCashEnabled: e.target.checked })} /> {t(lang, "payCashEnabled")}</label>
            <label><input type="checkbox" checked={settings.payTerminalEnabled} onChange={(e) => setSettings({ ...settings, payTerminalEnabled: e.target.checked })} /> {t(lang, "payTerminalEnabled")}</label>
            <label>
              {t(lang, "currency")}
              <select
                value={settings.currencyCode ?? "MDL"}
                onChange={(e) => setSettings({ ...settings, currencyCode: e.target.value })}
              >
                {currencies.map((c) => (
                  <option key={c.code} value={c.code}>
                    {c.code} — {c.name}
                  </option>
                ))}
                {currencies.length === 0 && <option value="MDL">MDL</option>}
              </select>
              <div style={{ fontSize: 12, color: "#6b7280", marginTop: 4 }}>
                {t(lang, "currencyManagedBySuperadmin")}
              </div>
            </label>
            <label>
              {t(lang, "defaultLanguage")}
              <select
                value={settings.defaultLang ?? "ru"}
                onChange={(e) => setSettings({ ...settings, defaultLang: e.target.value })}
              >
                <option value="ru">{t(lang, "langRu")}</option>
                <option value="ro">{t(lang, "langRo")}</option>
                <option value="en">{t(lang, "langEn")}</option>
              </select>
            </label>
            <label>{t(lang, "otpTtlSeconds")} <input value={settings.otpTtlSeconds} onChange={(e) => setSettings({ ...settings, otpTtlSeconds: Number(e.target.value) })} /></label>
            <label>{t(lang, "otpMaxAttempts")} <input value={settings.otpMaxAttempts} onChange={(e) => setSettings({ ...settings, otpMaxAttempts: Number(e.target.value) })} /></label>
            <label>{t(lang, "otpResendCooldownSeconds")} <input value={settings.otpResendCooldownSeconds} onChange={(e) => setSettings({ ...settings, otpResendCooldownSeconds: Number(e.target.value) })} /></label>
            <label>{t(lang, "otpLength")} <input value={settings.otpLength} onChange={(e) => setSettings({ ...settings, otpLength: Number(e.target.value) })} /></label>
            <label><input type="checkbox" checked={settings.otpDevEchoCode} onChange={(e) => setSettings({ ...settings, otpDevEchoCode: e.target.checked })} /> {t(lang, "otpDevEchoCode")}</label>
            <label>{t(lang, "tipsPercentages")} <input value={settings.tipsPercentages.join(",")} onChange={(e) => setSettings({ ...settings, tipsPercentages: e.target.value.split(",").map((x) => parseInt(x.trim(), 10)).filter((v) => !Number.isNaN(v)) })} /></label>
          </div>
        )}
        <button onClick={saveSettings} style={{ marginTop: 12, padding: "8px 12px" }}>{t(lang, "saveSettings")}</button>
      </section>

      <section style={{ marginTop: 24 }}>
        <h2>{t(lang, "parties")}</h2>
        <div style={{ display: "flex", gap: 8, flexWrap: "wrap", alignItems: "center" }}>
          <label>
            {t(lang, "status")}
            <select value={partyStatusFilter} onChange={(e) => setPartyStatusFilter(e.target.value)}>
              <option value="ACTIVE">{t(lang, "statusActive")}</option>
              <option value="CLOSED">{t(lang, "statusClosed")}</option>
            </select>
          </label>
          <label>
            {t(lang, "table")}
            <input
              value={partyTableFilter}
              onChange={(e) => setPartyTableFilter(e.target.value)}
              placeholder="#"
              style={{ width: 80 }}
            />
          </label>
          <label>
            {t(lang, "pin")}
            <input
              value={partyPinFilter}
              onChange={(e) => setPartyPinFilter(e.target.value)}
              placeholder="0000"
              style={{ width: 80 }}
            />
          </label>
          <label>
            {t(lang, "expiringMin")}
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
            {t(lang, "expiring")}
          </div>
          <button onClick={loadAll}>{t(lang, "refreshParties")}</button>
        </div>
        {parties.length === 0 ? (
          <div style={{ marginTop: 8, color: "#666" }}>{t(lang, "noParties")}</div>
        ) : (
          <div style={{ marginTop: 10 }}>
            <table style={{ width: "100%", borderCollapse: "collapse" }}>
              <thead>
                <tr>
                  <th style={{ textAlign: "left", borderBottom: "1px solid #ddd", padding: "6px 4px" }}>ID</th>
                  <th style={{ textAlign: "left", borderBottom: "1px solid #ddd", padding: "6px 4px" }}>{t(lang, "table")}</th>
                  <th style={{ textAlign: "left", borderBottom: "1px solid #ddd", padding: "6px 4px" }}>{t(lang, "pin")}</th>
                  <th style={{ textAlign: "left", borderBottom: "1px solid #ddd", padding: "6px 4px" }}>{t(lang, "status")}</th>
                  <th style={{ textAlign: "left", borderBottom: "1px solid #ddd", padding: "6px 4px" }}>{t(lang, "participants")}</th>
                  <th style={{ textAlign: "left", borderBottom: "1px solid #ddd", padding: "6px 4px" }}>{t(lang, "created")}</th>
                  <th style={{ textAlign: "left", borderBottom: "1px solid #ddd", padding: "6px 4px" }}>{t(lang, "expires")}</th>
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
                      <Fragment key={p.id}>
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
                              <div style={{ fontSize: 12, color: "#666", marginBottom: 6 }}>{t(lang, "participantsLabel")}</div>
                              {p.guestSessionIds.length === 0 ? (
                                <div style={{ color: "#999" }}>{t(lang, "noParticipants")}</div>
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
                                  {t(lang, "copyIds")}
                                </button>
                              </div>
                            </td>
                          </tr>
                        )}
                      </Fragment>
                    );
                  })}
              </tbody>
            </table>
          </div>
        )}
      </section>

      <section style={{ marginTop: 24 }}>
        <h2>{t(lang, "stats")}</h2>
        <div style={{ display: "flex", gap: 8, flexWrap: "wrap", alignItems: "center" }}>
          <label>{t(lang, "fromDate")} <input type="date" value={statsFrom} onChange={(e) => setStatsFrom(e.target.value)} /></label>
          <label>{t(lang, "toDate")} <input type="date" value={statsTo} onChange={(e) => setStatsTo(e.target.value)} /></label>
          <label>
            {t(lang, "table")}
            <select value={statsTableId} onChange={(e) => setStatsTableId(e.target.value ? Number(e.target.value) : "")}>
              <option value="">{t(lang, "all")}</option>
              {tables.map((t) => (
                <option key={t.id} value={t.id}>#{t.number}</option>
              ))}
            </select>
          </label>
          <label>
            {t(lang, "hall")}
            <select value={statsHallId} onChange={(e) => setStatsHallId(e.target.value ? Number(e.target.value) : "")}>
              <option value="">{t(lang, "all")}</option>
              {halls.map((h) => (
                <option key={h.id} value={h.id}>{h.name}</option>
              ))}
            </select>
          </label>
          <label>
            {t(lang, "plan")}
            <select
              value={statsHallPlanId}
              onChange={(e) => setStatsHallPlanId(e.target.value ? Number(e.target.value) : "")}
              disabled={statsHallId === ""}
            >
              <option value="">{t(lang, "all")}</option>
              {statsHallPlans.map((p) => (
                <option key={p.id} value={p.id}>{p.name}</option>
              ))}
            </select>
          </label>
          {statsHallId === "" && (
            <span style={{ fontSize: 12, color: "#666" }}>{t(lang, "planAvailableAfterHall")}</span>
          )}
          <label>
            {t(lang, "waiter")}
            <select value={chatExportWaiterId} onChange={(e) => setChatExportWaiterId(e.target.value ? Number(e.target.value) : "")}>
              <option value="">{t(lang, "all")}</option>
              {staff.filter((s) => s.role === "WAITER").map((w) => (
                <option key={w.id} value={w.id}>{w.username} #{w.id}</option>
              ))}
            </select>
          </label>
          <label>{t(lang, "topLimit")} <input type="number" min={1} max={100} value={statsLimit} onChange={(e) => setStatsLimit(Number(e.target.value))} style={{ width: 80 }} /></label>
          <button onClick={loadStats}>{t(lang, "load")}</button>
          <button onClick={downloadCsv}>{t(lang, "downloadCsv")}</button>
        </div>
        {stats && (
          <div style={{ marginTop: 10, border: "1px solid #eee", borderRadius: 8, padding: 10 }}>
            <div>{t(lang, "period")}: {stats.from} → {stats.to}</div>
            <div>{t(lang, "ordersCount")}: {stats.ordersCount}</div>
            <div>{t(lang, "callsCount")}: {stats.callsCount}</div>
            <div>{t(lang, "paidBills")}: {stats.paidBillsCount}</div>
            <div>{t(lang, "gross")}: {money(stats.grossCents)}</div>
            <div>{t(lang, "tips")}: {money(stats.tipsCents)}</div>
            <div>{t(lang, "activeTables")}: {stats.activeTablesCount}</div>
            {stats.avgBranchRating != null && (
              <div>
                {t(lang, "branchReviewsAvg")}: {stats.avgBranchRating.toFixed(2)} • {t(lang, "branchReviewsCount")}: {stats.branchReviewsCount ?? 0}
              </div>
            )}
          </div>
        )}
        {daily.length > 0 && (
          <div style={{ marginTop: 12 }}>
            <table style={{ width: "100%", borderCollapse: "collapse" }}>
              <thead>
                <tr>
                  <th style={{ textAlign: "left", borderBottom: "1px solid #ddd", padding: "6px 4px" }}>{t(lang, "day")}</th>
                  <th style={{ textAlign: "right", borderBottom: "1px solid #ddd", padding: "6px 4px" }}>{t(lang, "ordersCount")}</th>
                  <th style={{ textAlign: "right", borderBottom: "1px solid #ddd", padding: "6px 4px" }}>{t(lang, "callsCount")}</th>
                  <th style={{ textAlign: "right", borderBottom: "1px solid #ddd", padding: "6px 4px" }}>{t(lang, "paidBills")}</th>
                  <th style={{ textAlign: "right", borderBottom: "1px solid #ddd", padding: "6px 4px" }}>{t(lang, "gross")}</th>
                  <th style={{ textAlign: "right", borderBottom: "1px solid #ddd", padding: "6px 4px" }}>{t(lang, "tips")}</th>
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
              <strong>{t(lang, "topItems")}</strong>
              {topItems.length === 0 ? (
                <div style={{ color: "#666", marginTop: 6 }}>{t(lang, "noData")}</div>
              ) : (
                <table style={{ width: "100%", borderCollapse: "collapse", marginTop: 6 }}>
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
                  </tbody>
                </table>
              )}
            </div>
            <div style={{ border: "1px solid #eee", borderRadius: 8, padding: 10 }}>
              <strong>{t(lang, "topCategories")}</strong>
              {topCategories.length === 0 ? (
                <div style={{ color: "#666", marginTop: 6 }}>{t(lang, "noData")}</div>
              ) : (
                <table style={{ width: "100%", borderCollapse: "collapse", marginTop: 6 }}>
                  <thead>
                    <tr>
                      <th style={{ textAlign: "left", borderBottom: "1px solid #ddd", padding: "6px 4px" }}>{t(lang, "categories")}</th>
                      <th style={{ textAlign: "right", borderBottom: "1px solid #ddd", padding: "6px 4px" }}>{t(lang, "qty")}</th>
                      <th style={{ textAlign: "right", borderBottom: "1px solid #ddd", padding: "6px 4px" }}>{t(lang, "gross")}</th>
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

        <div style={{ marginTop: 16, border: "1px solid #eee", borderRadius: 8, padding: 10 }}>
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
            <strong>{t(lang, "motivation")}</strong>
            {waiterMotivationLoading && <span style={{ fontSize: 12, color: "#666" }}>{t(lang, "loading")}</span>}
          </div>
          {waiterMotivation.length === 0 ? (
            <div style={{ marginTop: 6, color: "#666" }}>{t(lang, "noData")}</div>
          ) : (
            (() => {
              const maxOrders = Math.max(...waiterMotivation.map((r) => r.ordersCount));
              const maxTips = Math.max(...waiterMotivation.map((r) => r.tipsCents));
              const slaValues = waiterMotivation.map((r) => r.avgSlaMinutes).filter((v): v is number => v != null);
              const bestSla = slaValues.length ? Math.min(...slaValues) : null;
              return (
            <table style={{ width: "100%", borderCollapse: "collapse", marginTop: 6 }}>
              <thead>
                <tr>
                  <th style={{ textAlign: "left", borderBottom: "1px solid #ddd", padding: "6px 4px" }}>{t(lang, "waiter")}</th>
                  <th style={{ textAlign: "right", borderBottom: "1px solid #ddd", padding: "6px 4px" }}>{t(lang, "ordersCount")}</th>
                  <th style={{ textAlign: "right", borderBottom: "1px solid #ddd", padding: "6px 4px" }}>{t(lang, "tips")}</th>
                  <th style={{ textAlign: "right", borderBottom: "1px solid #ddd", padding: "6px 4px" }}>{t(lang, "avgSla")}</th>
                  <th style={{ textAlign: "left", borderBottom: "1px solid #ddd", padding: "6px 4px" }}>{t(lang, "badges")}</th>
                </tr>
              </thead>
              <tbody>
                {waiterMotivation.map((r) => (
                  <tr key={r.staffUserId}>
                    <td style={{ padding: "6px 4px", borderBottom: "1px solid #f0f0f0" }}>{r.username}</td>
                    <td style={{ padding: "6px 4px", textAlign: "right", borderBottom: "1px solid #f0f0f0" }}>{r.ordersCount}</td>
                    <td style={{ padding: "6px 4px", textAlign: "right", borderBottom: "1px solid #f0f0f0" }}>{money(r.tipsCents)}</td>
                    <td style={{ padding: "6px 4px", textAlign: "right", borderBottom: "1px solid #f0f0f0" }}>
                      {r.avgSlaMinutes != null ? r.avgSlaMinutes.toFixed(1) : "—"}
                    </td>
                    <td style={{ padding: "6px 4px", borderBottom: "1px solid #f0f0f0" }}>
                      <div style={{ display: "flex", gap: 6, flexWrap: "wrap" }}>
                        {r.ordersCount === maxOrders && maxOrders > 0 && (
                          <span style={{ fontSize: 11, padding: "2px 6px", background: "#eef2ff", color: "#3730a3", borderRadius: 999 }}>
                            {t(lang, "badgeTopOrders")}
                          </span>
                        )}
                        {r.tipsCents === maxTips && maxTips > 0 && (
                          <span style={{ fontSize: 11, padding: "2px 6px", background: "#ecfeff", color: "#0f766e", borderRadius: 999 }}>
                            {t(lang, "badgeTopTips")}
                          </span>
                        )}
                        {bestSla != null && r.avgSlaMinutes != null && r.avgSlaMinutes === bestSla && (
                          <span style={{ fontSize: 11, padding: "2px 6px", background: "#fef3c7", color: "#92400e", borderRadius: 999 }}>
                            {t(lang, "badgeBestSla")}
                          </span>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
              );
            })()
          )}
        </div>
      </section>

      <section style={{ marginTop: 24 }}>
        <h2>{t(lang, "audit")}</h2>
        <div style={{ display: "flex", gap: 8, flexWrap: "wrap", alignItems: "center" }}>
          <label>
            {t(lang, "action")}
            <input list="audit-actions" value={auditAction} onChange={(e) => setAuditAction(e.target.value)} placeholder={t(lang, "auditActionPlaceholder")} />
          </label>
          <label>
            {t(lang, "entity")}
            <input list="audit-entities" value={auditEntityType} onChange={(e) => setAuditEntityType(e.target.value)} placeholder={t(lang, "auditEntityPlaceholder")} />
          </label>
          <label>{t(lang, "actor")} <input value={auditActor} onChange={(e) => setAuditActor(e.target.value)} placeholder={t(lang, "username")} /></label>
          <label>{t(lang, "from")} <input type="date" value={auditFrom} onChange={(e) => setAuditFrom(e.target.value)} /></label>
          <label>{t(lang, "to")} <input type="date" value={auditTo} onChange={(e) => setAuditTo(e.target.value)} /></label>
          <label>{t(lang, "beforeId")} <input type="number" value={auditBeforeId} onChange={(e) => setAuditBeforeId(e.target.value ? Number(e.target.value) : "")} /></label>
          <label>{t(lang, "afterId")} <input type="number" value={auditAfterId} onChange={(e) => setAuditAfterId(e.target.value ? Number(e.target.value) : "")} /></label>
          <label>{t(lang, "limit")} <input type="number" min={1} max={500} value={auditLimit} onChange={(e) => setAuditLimit(Number(e.target.value))} style={{ width: 90 }} /></label>
          <button onClick={() => loadAuditLogs()} disabled={auditLoading}>{auditLoading ? t(lang, "loading") : t(lang, "load")}</button>
          <button onClick={downloadAuditCsv} disabled={auditLoading}>CSV</button>
          <button onClick={() => { setAuditAfterId(""); setAuditBeforeId(""); loadAuditLogs(); }} disabled={auditLoading}>{t(lang, "latest")}</button>
          <button onClick={loadAuditPrevPage} disabled={auditLoading || auditLogs.length === 0}>{t(lang, "prevPage")}</button>
          <button onClick={loadAuditNextPage} disabled={auditLoading || auditLogs.length === 0}>{t(lang, "nextPage")}</button>
          <button onClick={clearAuditFilters} disabled={auditLoading}>{t(lang, "clear")}</button>
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
                  <th style={{ textAlign: "left", borderBottom: "1px solid #ddd", padding: "6px 4px" }}>{t(lang, "auditId")}</th>
                  <th style={{ textAlign: "left", borderBottom: "1px solid #ddd", padding: "6px 4px" }}>{t(lang, "when")}</th>
                  <th style={{ textAlign: "left", borderBottom: "1px solid #ddd", padding: "6px 4px" }}>{t(lang, "auditActor")}</th>
                  <th style={{ textAlign: "left", borderBottom: "1px solid #ddd", padding: "6px 4px" }}>{t(lang, "auditAction")}</th>
                  <th style={{ textAlign: "left", borderBottom: "1px solid #ddd", padding: "6px 4px" }}>{t(lang, "auditEntity")}</th>
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
        <h2>{t(lang, "categories")}</h2>
        <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
          <input placeholder={t(lang, "menuCategoryNameRu")} value={newCatNameRu} onChange={(e) => setNewCatNameRu(e.target.value)} />
          <input type="number" placeholder={t(lang, "sort")} value={newCatSort} onChange={(e) => setNewCatSort(Number(e.target.value))} />
          <button onClick={createCategory}>{t(lang, "add")}</button>
        </div>
        {editingCategoryId && (
          <div style={{ marginTop: 10, border: "1px dashed #ddd", padding: 10 }}>
            <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
              <input placeholder={t(lang, "menuCategoryNameRu")} value={editCatNameRu} onChange={(e) => setEditCatNameRu(e.target.value)} />
              <input placeholder={t(lang, "menuCategoryNameRo")} value={editCatNameRo} onChange={(e) => setEditCatNameRo(e.target.value)} />
              <input placeholder={t(lang, "menuCategoryNameEn")} value={editCatNameEn} onChange={(e) => setEditCatNameEn(e.target.value)} />
              <input type="number" placeholder={t(lang, "sort")} value={editCatSort} onChange={(e) => setEditCatSort(Number(e.target.value))} />
              <label><input type="checkbox" checked={editCatActive} onChange={(e) => setEditCatActive(e.target.checked)} /> {t(lang, "active")}</label>
            </div>
            <div style={{ marginTop: 8 }}>
              <button onClick={saveEditedCategory}>{t(lang, "save")}</button>
              <button onClick={() => setEditingCategoryId(null)} style={{ marginLeft: 8 }}>{t(lang, "cancel")}</button>
            </div>
          </div>
        )}
        <div style={{ marginTop: 10 }}>
          {categories.map((c) => (
            <div key={c.id} style={{ display: "flex", gap: 10, alignItems: "center", padding: "6px 0", borderBottom: "1px solid #eee" }}>
              <strong>{c.nameRu}</strong>
              <span>#{c.sortOrder}</span>
              <span>{c.isActive ? t(lang, "active") : t(lang, "inactive")}</span>
              <button onClick={() => {
                setEditingCategoryId(c.id);
                setEditCatNameRu(c.nameRu);
                setEditCatNameRo(c.nameRo ?? "");
                setEditCatNameEn(c.nameEn ?? "");
                setEditCatSort(c.sortOrder);
                setEditCatActive(c.isActive);
              }}>{t(lang, "edit")}</button>
              <button onClick={() => toggleCategory(c)}>{c.isActive ? t(lang, "disable") : t(lang, "enable")}</button>
            </div>
          ))}
        </div>
      </section>

      <section style={{ marginTop: 24 }}>
        <h2>{t(lang, "items")}</h2>
        <div style={{ marginBottom: 8, display: "flex", gap: 8, alignItems: "center", flexWrap: "wrap" }}>
          <input
            placeholder={`${t(lang, "search")} (RU/RO/EN)`}
            value={menuSearch}
            onChange={(e) => setMenuSearch(e.target.value)}
          />
          <select value={menuFilterCategoryId} onChange={(e) => setMenuFilterCategoryId(e.target.value ? Number(e.target.value) : "")}>
            <option value="">{t(lang, "allCategories")}</option>
            {categories.map((c) => (
              <option key={c.id} value={c.id}>{c.nameRu}</option>
            ))}
          </select>
          <select value={menuFilterActive} onChange={(e) => setMenuFilterActive(e.target.value)}>
            <option value="">{t(lang, "allStatuses")}</option>
            <option value="ACTIVE">{t(lang, "active")}</option>
            <option value="INACTIVE">{t(lang, "inactive")}</option>
          </select>
          <select value={menuFilterStopList} onChange={(e) => setMenuFilterStopList(e.target.value)}>
            <option value="">{t(lang, "stopListAny")}</option>
            <option value="STOP">{t(lang, "stopList")}</option>
            <option value="OK">{t(lang, "notInStopList")}</option>
          </select>
          <button onClick={() => setMenuSearch("")}>{t(lang, "clear")}</button>
          <button onClick={() => { setMenuSearch(""); setMenuFilterCategoryId(""); setMenuFilterActive(""); setMenuFilterStopList(""); }}>{t(lang, "resetFilters")}</button>
        </div>
        <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
          <select value={newItemCatId} onChange={(e) => setNewItemCatId(e.target.value ? Number(e.target.value) : "")}>
            <option value="">{t(lang, "categories")}</option>
            {categories.map((c) => (
              <option key={c.id} value={c.id}>{c.nameRu}</option>
            ))}
          </select>
          <input placeholder={t(lang, "menuCategoryNameRu")} value={newItemNameRu} onChange={(e) => setNewItemNameRu(e.target.value)} />
          <input placeholder={t(lang, "menuCategoryNameRo")} value={newItemNameRo} onChange={(e) => setNewItemNameRo(e.target.value)} />
          <input placeholder={t(lang, "menuCategoryNameEn")} value={newItemNameEn} onChange={(e) => setNewItemNameEn(e.target.value)} />
          <input placeholder={t(lang, "descRu")} value={newItemDescRu} onChange={(e) => setNewItemDescRu(e.target.value)} />
          <input placeholder={t(lang, "descRo")} value={newItemDescRo} onChange={(e) => setNewItemDescRo(e.target.value)} />
          <input placeholder={t(lang, "descEn")} value={newItemDescEn} onChange={(e) => setNewItemDescEn(e.target.value)} />
          <input placeholder={t(lang, "ingredientsRu")} value={newItemIngredientsRu} onChange={(e) => setNewItemIngredientsRu(e.target.value)} />
          <input placeholder={t(lang, "ingredientsRo")} value={newItemIngredientsRo} onChange={(e) => setNewItemIngredientsRo(e.target.value)} />
          <input placeholder={t(lang, "ingredientsEn")} value={newItemIngredientsEn} onChange={(e) => setNewItemIngredientsEn(e.target.value)} />
          <input placeholder={t(lang, "allergens")} value={newItemAllergens} onChange={(e) => setNewItemAllergens(e.target.value)} />
          <input placeholder={t(lang, "weight")} value={newItemWeight} onChange={(e) => setNewItemWeight(e.target.value)} />
          <input placeholder={t(lang, "tagsCsv")} value={newItemTags} onChange={(e) => setNewItemTags(e.target.value)} />
          <input placeholder={t(lang, "photosCsv")} value={newItemPhotos} onChange={(e) => setNewItemPhotos(e.target.value)} />
          <input type="number" placeholder={t(lang, "kcal")} value={newItemKcal} onChange={(e) => setNewItemKcal(Number(e.target.value))} />
          <input type="number" placeholder={t(lang, "proteinG")} value={newItemProtein} onChange={(e) => setNewItemProtein(Number(e.target.value))} />
          <input type="number" placeholder={t(lang, "fatG")} value={newItemFat} onChange={(e) => setNewItemFat(Number(e.target.value))} />
          <input type="number" placeholder={t(lang, "carbsG")} value={newItemCarbs} onChange={(e) => setNewItemCarbs(Number(e.target.value))} />
          <input type="number" placeholder={t(lang, "priceCents")} value={newItemPrice} onChange={(e) => setNewItemPrice(Number(e.target.value))} />
          <select value={newItemCurrency} onChange={(e) => setNewItemCurrency(e.target.value)}>
            {currencies.map((c) => (
              <option key={c.code} value={c.code}>
                {c.code}
              </option>
            ))}
            {currencies.length === 0 && <option value={newItemCurrency}>{newItemCurrency}</option>}
          </select>
          <label><input type="checkbox" checked={newItemActive} onChange={(e) => setNewItemActive(e.target.checked)} /> {t(lang, "active")}</label>
          <label><input type="checkbox" checked={newItemStopList} onChange={(e) => setNewItemStopList(e.target.checked)} /> {t(lang, "stopList")}</label>
          <button onClick={createItem}>{t(lang, "add")}</button>
        </div>
        {editingItemId && (
          <div style={{ marginTop: 10, border: "1px dashed #ddd", padding: 10 }}>
            <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(200px, 1fr))", gap: 8 }}>
              <select value={editItem.categoryId ?? ""} onChange={(e) => setEditItem({ ...editItem, categoryId: e.target.value ? Number(e.target.value) : undefined })}>
                <option value="">{t(lang, "categories")}</option>
                {categories.map((c) => (
                  <option key={c.id} value={c.id}>{c.nameRu}</option>
                ))}
              </select>
              <input placeholder={t(lang, "menuCategoryNameRu")} value={editItem.nameRu ?? ""} onChange={(e) => setEditItem({ ...editItem, nameRu: e.target.value })} />
              <input placeholder={t(lang, "menuCategoryNameRo")} value={editItem.nameRo ?? ""} onChange={(e) => setEditItem({ ...editItem, nameRo: e.target.value })} />
              <input placeholder={t(lang, "menuCategoryNameEn")} value={editItem.nameEn ?? ""} onChange={(e) => setEditItem({ ...editItem, nameEn: e.target.value })} />
              <input placeholder={t(lang, "descRu")} value={editItem.descriptionRu ?? ""} onChange={(e) => setEditItem({ ...editItem, descriptionRu: e.target.value })} />
              <input placeholder={t(lang, "descRo")} value={editItem.descriptionRo ?? ""} onChange={(e) => setEditItem({ ...editItem, descriptionRo: e.target.value })} />
              <input placeholder={t(lang, "descEn")} value={editItem.descriptionEn ?? ""} onChange={(e) => setEditItem({ ...editItem, descriptionEn: e.target.value })} />
              <input placeholder={t(lang, "ingredientsRu")} value={editItem.ingredientsRu ?? ""} onChange={(e) => setEditItem({ ...editItem, ingredientsRu: e.target.value })} />
              <input placeholder={t(lang, "ingredientsRo")} value={editItem.ingredientsRo ?? ""} onChange={(e) => setEditItem({ ...editItem, ingredientsRo: e.target.value })} />
              <input placeholder={t(lang, "ingredientsEn")} value={editItem.ingredientsEn ?? ""} onChange={(e) => setEditItem({ ...editItem, ingredientsEn: e.target.value })} />
              <input placeholder={t(lang, "allergens")} value={editItem.allergens ?? ""} onChange={(e) => setEditItem({ ...editItem, allergens: e.target.value })} />
              <input placeholder={t(lang, "weight")} value={editItem.weight ?? ""} onChange={(e) => setEditItem({ ...editItem, weight: e.target.value })} />
              <input placeholder={t(lang, "tagsCsv")} value={editItem.tags ?? ""} onChange={(e) => setEditItem({ ...editItem, tags: e.target.value })} />
              <input placeholder={t(lang, "photosCsv")} value={editItem.photoUrls ?? ""} onChange={(e) => setEditItem({ ...editItem, photoUrls: e.target.value })} />
              <input type="number" placeholder={t(lang, "kcal")} value={editItem.kcal ?? 0} onChange={(e) => setEditItem({ ...editItem, kcal: Number(e.target.value) })} />
              <input type="number" placeholder={t(lang, "proteinG")} value={editItem.proteinG ?? 0} onChange={(e) => setEditItem({ ...editItem, proteinG: Number(e.target.value) })} />
              <input type="number" placeholder={t(lang, "fatG")} value={editItem.fatG ?? 0} onChange={(e) => setEditItem({ ...editItem, fatG: Number(e.target.value) })} />
              <input type="number" placeholder={t(lang, "carbsG")} value={editItem.carbsG ?? 0} onChange={(e) => setEditItem({ ...editItem, carbsG: Number(e.target.value) })} />
              <input type="number" placeholder={t(lang, "priceCents")} value={editItem.priceCents ?? 0} onChange={(e) => setEditItem({ ...editItem, priceCents: Number(e.target.value) })} />
              <select value={editItem.currency ?? "MDL"} onChange={(e) => setEditItem({ ...editItem, currency: e.target.value })}>
                {currencies.map((c) => (
                  <option key={c.code} value={c.code}>
                    {c.code}
                  </option>
                ))}
                {currencies.length === 0 && <option value={editItem.currency ?? "MDL"}>{editItem.currency ?? "MDL"}</option>}
              </select>
              <label><input type="checkbox" checked={!!editItem.isActive} onChange={(e) => setEditItem({ ...editItem, isActive: e.target.checked })} /> {t(lang, "active")}</label>
              <label><input type="checkbox" checked={!!editItem.isStopList} onChange={(e) => setEditItem({ ...editItem, isStopList: e.target.checked })} /> {t(lang, "stopList")}</label>
            </div>
            <div style={{ marginTop: 8 }}>
              <button onClick={saveEditedItem}>{t(lang, "save")}</button>
              <button onClick={() => { setEditingItemId(null); setEditItem({}); }} style={{ marginLeft: 8 }}>{t(lang, "cancel")}</button>
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
              <span>{it.isActive ? t(lang, "active") : t(lang, "inactive")}</span>
              <span>{it.isStopList ? t(lang, "stopList") : ""}</span>
              <button onClick={() => startEditItem(it)}>{t(lang, "edit")}</button>
              <button onClick={() => toggleItem(it)}>{it.isActive ? t(lang, "disable") : t(lang, "enable")}</button>
              <button onClick={() => toggleStopList(it)}>{it.isStopList ? t(lang, "enable") : t(lang, "stopList")}</button>
            </div>
          ))}
        </div>
      </section>

      <section style={{ marginTop: 24 }}>
        <h2>{t(lang, "floorPlan")}</h2>
        <div style={{ display: "flex", gap: 8, flexWrap: "wrap", alignItems: "center" }}>
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
          <button onClick={fitPlanToScreen}>{t(lang, "fitToScreen")}</button>
          <button onClick={() => setPlanZoom(1)}>{t(lang, "resetZoom")}</button>
          <button onClick={() => setPlanPan({ x: 0, y: 0 })}>{t(lang, "resetPan")}</button>
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
          <span style={{ color: "#666" }}>{t(lang, "zoom")}: {Math.round(planZoom * 100)}%</span>
          <select value={hallId} onChange={(e) => setHallId(e.target.value ? Number(e.target.value) : "")}>
            <option value="">{t(lang, "planSelectHall")}</option>
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
            <option value="">{t(lang, "planDefault")}</option>
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
            {t(lang, "setActivePlan")}
          </button>
          <button
            onClick={async () => {
              if (!hallPlanId) return;
              await api(`/api/admin/hall-plans/${hallPlanId}/duplicate`, { method: "POST", body: JSON.stringify({}) });
              loadAll();
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
              loadAll();
            }}
            disabled={!hallPlanId}
          >
            {t(lang, "deletePlan")}
          </button>
          <button onClick={autoLayoutTables}>{t(lang, "autoLayout")}</button>
          <button onClick={resetLayouts}>{t(lang, "resetLayout")}</button>
          <button onClick={snapLayouts}>{t(lang, "snapLayout")}</button>
          <button onClick={saveTableLayout}>{t(lang, "saveLayout")}</button>
          <button onClick={exportPlanJson} disabled={!hallPlanId}>{t(lang, "exportJson")}</button>
          <button
            onClick={() => setPlanVersionsOpen((v) => !v)}
            disabled={!hallPlanId}
          >
            {planVersionsOpen ? t(lang, "hideHistory") : t(lang, "showHistory")}
          </button>
          <label style={{ display: "inline-flex", alignItems: "center", gap: 6 }}>
            <span style={{ fontSize: 12, color: "#666" }}>{t(lang, "importJson")}</span>
            <label style={{ display: "inline-flex", alignItems: "center", gap: 6, fontSize: 12, color: "#666" }}>
              <input
                type="checkbox"
                checked={applyLayoutsOnImport}
                onChange={(e) => setApplyLayoutsOnImport(e.target.checked)}
              />
              {t(lang, "applyLayouts")}
            </label>
            <label style={{ display: "inline-flex", alignItems: "center", gap: 6, fontSize: 12, color: "#666" }}>
              <input
                type="checkbox"
                checked={applyTablesOnImport}
                onChange={(e) => setApplyTablesOnImport(e.target.checked)}
              />
              {t(lang, "applyTables")}
            </label>
            <span style={{ fontSize: 11, color: "#888" }}>{t(lang, "importHint")}</span>
            <input
              type="file"
              accept="application/json"
              onChange={(e) => {
                const f = e.target.files?.[0];
                if (f) importPlanJson(f, applyLayoutsOnImport, applyTablesOnImport);
                e.currentTarget.value = "";
              }}
            />
          </label>
          <span style={{ color: "#666" }}>{t(lang, "dragHint")}</span>
        </div>
        {hallPlanId && planVersionsOpen && (
          <div style={{ marginTop: 10, border: "1px solid #eee", borderRadius: 12, padding: 12, background: "#fafafa" }}>
            <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between" }}>
              <strong>{t(lang, "planHistory")}</strong>
              <span style={{ fontSize: 12, color: "#666" }}>
                {planVersionsLoading ? t(lang, "planHistoryLoading") : `${planVersions.length} ${t(lang, "versionsCount")}`}
              </span>
            </div>
            {planVersions.length === 0 && !planVersionsLoading && (
              <div style={{ fontSize: 12, color: "#666", marginTop: 6 }}>{t(lang, "noVersions")}</div>
            )}
            {planVersions.length > 0 && (
              <div style={{ marginTop: 8, display: "grid", gap: 6 }}>
                {planVersions.slice(0, 20).map((v) => (
                  <div
                    key={v.id}
                    style={{
                      display: "grid",
                      gridTemplateColumns: "1fr auto",
                      gap: 8,
                      alignItems: "center",
                      padding: "6px 8px",
                      background: "#fff",
                      borderRadius: 8,
                      border: "1px solid #eee",
                    }}
                  >
                    <div style={{ fontSize: 12 }}>
                      <div style={{ fontWeight: 600 }}>{v.name}</div>
                      <div style={{ color: "#666" }}>
                        {v.action || "UPDATE"} • {v.createdAt ? new Date(v.createdAt).toLocaleString() : "n/a"}
                        {v.createdByStaffId ? ` • staff #${v.createdByStaffId}` : ""}
                      </div>
                    </div>
                    <button
                      onClick={async () => {
                        const ok = window.confirm(t(lang, "restoreConfirm"));
                        if (!ok) return;
                        await api(`/api/admin/hall-plans/${hallPlanId}/versions/${v.id}/restore`, { method: "POST" });
                        loadAll();
                      }}
                    >
                      {t(lang, "restore")}
                    </button>
                  </div>
                ))}
                {planVersions.length > 20 && (
                  <div style={{ fontSize: 12, color: "#666" }}>{t(lang, "showingLatest")}</div>
                )}
              </div>
            )}
          </div>
        )}
        <div style={{ display: "flex", gap: 8, flexWrap: "wrap", alignItems: "center", marginTop: 8 }}>
          <input placeholder={t(lang, "newHallName")} value={newHallName} onChange={(e) => setNewHallName(e.target.value)} />
          <input type="number" placeholder={t(lang, "sort")} value={newHallSort} onChange={(e) => setNewHallSort(Number(e.target.value))} />
          <button
            onClick={async () => {
              if (!newHallName.trim()) return;
              await api("/api/admin/halls", { method: "POST", body: JSON.stringify({ name: newHallName.trim(), sortOrder: newHallSort }) });
              setNewHallName("");
              setNewHallSort(0);
              loadAll();
            }}
          >
            {t(lang, "addHall")}
          </button>
          <input placeholder={t(lang, "newPlanName")} value={newPlanName} onChange={(e) => setNewPlanName(e.target.value)} />
          <input type="number" placeholder={t(lang, "planSort")} value={newPlanSort} onChange={(e) => setNewPlanSort(Number(e.target.value))} />
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
            <span style={{ fontSize: 12, color: "#666" }}>{t(lang, "templates")}:</span>
            <button onClick={saveTemplate} disabled={!hallPlanId}>{t(lang, "saveCurrent")}</button>
            {planTemplates.map((t) => (
              <span key={t.id} style={{ display: "inline-flex", gap: 6, alignItems: "center" }}>
                <button onClick={() => applyTemplate(t)}>{t.name}</button>
                <button onClick={() => removeTemplate(t.id)} style={{ color: "#b00" }}>×</button>
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
            <div style={{ marginBottom: 12 }}>
              <div style={{ fontWeight: 600, marginBottom: 6 }}>{t(lang, "assignWaiterDrag")}</div>
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
                  <div style={{ color: "#777", fontSize: 12 }}>{t(lang, "noWaitersYet")}</div>
                )}
              </div>
              {!isInteractive && (
                <div style={{ marginTop: 6, fontSize: 12, color: "#777" }}>
                  {t(lang, "switchToEdit")}
                </div>
              )}
            </div>
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
              <div style={{ color: "#666" }}>{t(lang, "dragHint")}</div>
            )}
            {hallId && (
              <div style={{ marginTop: 16, borderTop: "1px solid #eee", paddingTop: 12 }}>
                <h4 style={{ margin: 0 }}>{t(lang, "hallSettings")}</h4>
                <div style={{ display: "flex", gap: 8, marginTop: 8 }}>
                  <button
                    onClick={async () => {
                      await api(`/api/admin/halls/${hallId}`, { method: "PATCH", body: JSON.stringify({ isActive: true }) });
                      loadAll();
                    }}
                  >
                    {t(lang, "activate")}
                  </button>
                  <button
                    onClick={async () => {
                      await api(`/api/admin/halls/${hallId}`, { method: "PATCH", body: JSON.stringify({ isActive: false }) });
                      loadAll();
                    }}
                  >
                    {t(lang, "deactivate")}
                  </button>
                  <button
                    onClick={async () => {
                      const ok = window.confirm(t(lang, "deleteHallConfirm"));
                      if (!ok) return;
                      try {
                        await api(`/api/admin/halls/${hallId}`, { method: "DELETE" });
                        setHallId("");
                        loadAll();
                      } catch (e: any) {
                        const msg = e?.message ?? "";
                        if (msg.includes("409")) {
                          alert(t(lang, "deleteHallBlocked"));
                        } else {
                          alert(msg || t(lang, "deleteHallFailed"));
                        }
                      }
                    }}
                  >
                    {t(lang, "delete")}
                  </button>
                </div>
              </div>
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
                  <select
                    value={z.waiterId ?? ""}
                    onChange={(e) =>
                      setPlanZones((prev) =>
                        prev.map((p, i) => (i === zi ? { ...p, waiterId: e.target.value ? Number(e.target.value) : null } : p))
                      )
                    }
                  >
                    <option value="">{t(lang, "zoneWaiter")}: {t(lang, "none")}</option>
                    {staff.filter((s) => s.role === "WAITER").map((w) => (
                      <option key={w.id} value={w.id}>{w.username}</option>
                    ))}
                  </select>
                </div>
              ))}
              <button
                style={{ marginTop: 8 }}
                onClick={() =>
                  setPlanZones((prev) => [
                    ...prev,
                    { id: String(Date.now()), name: t(lang, "zone"), x: 10, y: 10, w: 30, h: 20, color: "#6C5CE7", waiterId: null },
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
        <h2>{t(lang, "tables")}</h2>
        <div style={{ marginBottom: 8, color: "#666" }}>
          {t(lang, "qrNote")}
        </div>
        <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
          <input type="number" placeholder={t(lang, "tableNumber")} value={newTableNumber} onChange={(e) => setNewTableNumber(Number(e.target.value))} />
          <input placeholder={t(lang, "publicIdOptional")} value={newTablePublicId} onChange={(e) => setNewTablePublicId(e.target.value)} />
          <select value={hallId} onChange={(e) => setHallId(e.target.value ? Number(e.target.value) : "")}>
            <option value="">{t(lang, "planSelectHall")}</option>
            {halls.map((h) => (
              <option key={h.id} value={h.id}>{h.name}</option>
            ))}
          </select>
          <select value={newTableWaiterId} onChange={(e) => setNewTableWaiterId(e.target.value ? Number(e.target.value) : "")}>
            <option value="">{t(lang, "assignWaiter")}</option>
            {staff.filter((s) => s.role === "WAITER").map((s) => (
              <option key={s.id} value={s.id}>{s.username}</option>
            ))}
          </select>
          <button onClick={createTable}>{t(lang, "addTable")}</button>
          <button onClick={refreshAllQrs}>{t(lang, "refreshAllQr")}</button>
        </div>
        <div style={{ marginTop: 8, display: "flex", gap: 8, flexWrap: "wrap", alignItems: "center" }}>
          <input placeholder={t(lang, "filterTablePlaceholder")} value={tableFilterText} onChange={(e) => setTableFilterText(e.target.value)} />
          <select value={tableFilterWaiterId} onChange={(e) => setTableFilterWaiterId(e.target.value ? Number(e.target.value) : "")}>
            <option value="">{t(lang, "allWaiters")}</option>
            {staff.filter((s) => s.role === "WAITER").map((s) => (
              <option key={s.id} value={s.id}>{s.username}</option>
            ))}
          </select>
          <select value={tableFilterHallId} onChange={(e) => setTableFilterHallId(e.target.value ? Number(e.target.value) : "")}>
            <option value="">{t(lang, "allHalls")}</option>
            {halls.map((h) => (
              <option key={h.id} value={h.id}>{h.name}</option>
            ))}
          </select>
          <select value={tableFilterAssigned} onChange={(e) => setTableFilterAssigned(e.target.value)}>
            <option value="">{t(lang, "allAssignments")}</option>
            <option value="ASSIGNED">{t(lang, "assigned")}</option>
            <option value="UNASSIGNED">{t(lang, "unassigned")}</option>
          </select>
          <select value={bulkHallId} onChange={(e) => setBulkHallId(e.target.value ? Number(e.target.value) : "")}>
            <option value="">{t(lang, "bulkHall")}</option>
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
            {t(lang, "assignFilteredToHall")}
          </button>
          <button onClick={() => { setTableFilterText(""); setTableFilterWaiterId(""); setTableFilterHallId(""); setTableFilterAssigned(""); }}>{t(lang, "clear")}</button>
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
              <strong>{translate(lang, "tableSelected")}{t.number}</strong>
              <span>{t.publicId}</span>
              <select
                value={t.hallId ?? ""}
                onChange={(e) => assignHall(t.id, e.target.value ? Number(e.target.value) : null)}
              >
                <option value="">{translate(lang, "noHall")}</option>
                {halls.map((h) => (
                  <option key={h.id} value={h.id}>{h.name}</option>
                ))}
              </select>
              <select
                value={t.assignedWaiterId ?? ""}
                onChange={(e) => assignWaiter(t.id, e.target.value ? Number(e.target.value) : null)}
              >
                <option value="">{translate(lang, "noWaiter")}</option>
                {staff.filter((s) => s.role === "WAITER").map((s) => (
                  <option key={s.id} value={s.id}>{s.username}</option>
                ))}
              </select>
              {t.assignedWaiterId && (
                <button onClick={() => assignWaiter(t.id, null)}>{translate(lang, "clearWaiter")}</button>
              )}
              <button onClick={() => getSignedUrl(t.publicId)}>{translate(lang, "qrUrl")}</button>
              <button onClick={() => showQr(t.id, t.publicId)}>{translate(lang, "showQr")}</button>
              <button onClick={() => showQr(t.id, t.publicId)}>{translate(lang, "refreshQr")}</button>
              {qrByTable[t.id] && (
                <a
                  href={`https://api.qrserver.com/v1/create-qr-code/?size=512x512&data=${encodeURIComponent(qrByTable[t.id])}`}
                  download={`table_${t.number}.png`}
                  style={{ marginLeft: 8 }}
                >
                  {translate(lang, "downloadQr")}
                </a>
              )}
              {qrByTable[t.id] && (
                <Image
                  src={`https://api.qrserver.com/v1/create-qr-code/?size=160x160&data=${encodeURIComponent(qrByTable[t.id])}`}
                  alt="QR"
                  width={160}
                  height={160}
                  style={{ marginLeft: 8, border: "1px solid #eee" }}
                  unoptimized
                />
              )}
            </div>
          ))}
        </div>
      </section>

      <section style={{ marginTop: 24 }}>
        <h2>{t(lang, "staff")}</h2>
        <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
          <input placeholder={t(lang, "staffUsername")} value={newStaffUser} onChange={(e) => setNewStaffUser(e.target.value)} />
          <input placeholder={t(lang, "staffPassword")} value={newStaffPass} onChange={(e) => setNewStaffPass(e.target.value)} />
          <select value={newStaffRole} onChange={(e) => setNewStaffRole(e.target.value)}>
            <option value="WAITER">WAITER</option>
            <option value="KITCHEN">KITCHEN</option>
            <option value="ADMIN">ADMIN</option>
          </select>
          <button onClick={createStaff}>{t(lang, "add")}</button>
        </div>
        <div style={{ marginTop: 8, display: "flex", gap: 8, flexWrap: "wrap", alignItems: "center" }}>
          <input placeholder={t(lang, "filterByUsername")} value={staffFilterText} onChange={(e) => setStaffFilterText(e.target.value)} />
          <select value={staffFilterRole} onChange={(e) => setStaffFilterRole(e.target.value)}>
            <option value="">{t(lang, "allRoles")}</option>
            <option value="WAITER">WAITER</option>
            <option value="KITCHEN">KITCHEN</option>
            <option value="ADMIN">ADMIN</option>
          </select>
          <select value={staffFilterActive} onChange={(e) => setStaffFilterActive(e.target.value)}>
            <option value="">{t(lang, "allStatuses")}</option>
            <option value="ACTIVE">{t(lang, "active")}</option>
            <option value="INACTIVE">{t(lang, "inactive")}</option>
          </select>
          <button onClick={() => { setStaffFilterText(""); setStaffFilterRole(""); setStaffFilterActive(""); }}>{t(lang, "clear")}</button>
        </div>
        <div style={{ marginTop: 10 }}>
          <div style={{ display: "flex", gap: 8, flexWrap: "wrap", alignItems: "center", marginBottom: 8 }}>
            <strong>{t(lang, "staffBulk")}</strong>
            <span style={{ color: "#666" }}>{t(lang, "staffSelected")}: {staffSelectedIds.length}</span>
            <button
              onClick={() => {
                const filtered = staff
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
                  .map((s) => s.id);
                setStaffSelectedIds(filtered);
              }}
            >
              {t(lang, "staffSelectAll")}
            </button>
            <button onClick={() => setStaffSelectedIds([])}>{t(lang, "staffClearSelection")}</button>
          </div>
          <div style={{ display: "flex", gap: 8, flexWrap: "wrap", alignItems: "center", marginBottom: 8 }}>
            <select value={bulkRole} onChange={(e) => setBulkRole(e.target.value)}>
              <option value="__SKIP__">{t(lang, "staffBulkRole")} — {t(lang, "staffBulkSkip")}</option>
              <option value="WAITER">WAITER</option>
              <option value="KITCHEN">KITCHEN</option>
              <option value="ADMIN">ADMIN</option>
            </select>
            <select value={bulkActive} onChange={(e) => setBulkActive(e.target.value)}>
              <option value="__SKIP__">{t(lang, "staffBulkActive")} — {t(lang, "staffBulkSkip")}</option>
              <option value="ACTIVE">{t(lang, "active")}</option>
              <option value="INACTIVE">{t(lang, "inactive")}</option>
            </select>
            <input placeholder={t(lang, "firstName")} value={bulkFirstName} onChange={(e) => setBulkFirstName(e.target.value)} />
            <input placeholder={t(lang, "lastName")} value={bulkLastName} onChange={(e) => setBulkLastName(e.target.value)} />
            <input placeholder={t(lang, "age")} value={bulkAge} onChange={(e) => setBulkAge(e.target.value)} style={{ width: 90 }} />
            <select value={bulkGender} onChange={(e) => setBulkGender(e.target.value)}>
              <option value="">{t(lang, "gender")} — {t(lang, "staffBulkSkip")}</option>
              <option value="male">{t(lang, "genderMale")}</option>
              <option value="female">{t(lang, "genderFemale")}</option>
              <option value="other">{t(lang, "genderOther")}</option>
            </select>
            <input placeholder={t(lang, "photoUrl")} value={bulkPhotoUrl} onChange={(e) => setBulkPhotoUrl(e.target.value)} style={{ minWidth: 240 }} />
            <button onClick={applyStaffBulk} disabled={bulkApplying}>
              {bulkApplying ? `${t(lang, "staffApplyBulk")} (${bulkProgress}/${staffSelectedIds.length})` : t(lang, "staffApplyBulk")}
            </button>
          </div>
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
              <input
                type="checkbox"
                checked={staffSelectedIds.includes(su.id)}
                onChange={(e) => {
                  setStaffSelectedIds((prev) =>
                    e.target.checked ? [...prev, su.id] : prev.filter((id) => id !== su.id)
                  );
                }}
              />
              {su.role === "WAITER" && (
                su.photoUrl ? (
                  <Image
                    src={su.photoUrl}
                    alt={su.firstName ? `${su.firstName} ${su.lastName ?? ""}`.trim() : su.username}
                    width={28}
                    height={28}
                    style={{ borderRadius: "50%", objectFit: "cover" }}
                    unoptimized
                  />
                ) : (
                  <div
                    style={{
                      width: 28,
                      height: 28,
                      borderRadius: "50%",
                      background: "#f1f3f5",
                      color: "#667085",
                      display: "flex",
                      alignItems: "center",
                      justifyContent: "center",
                      fontSize: 12,
                      fontWeight: 600,
                    }}
                  >
                    {(su.firstName || su.lastName) ? `${su.firstName?.[0] ?? ""}${su.lastName?.[0] ?? ""}`.toUpperCase() : "W"}
                  </div>
                )
              )}
              <strong>{su.username}</strong>
              {su.role === "WAITER" && su.firstName && (
                <span style={{ color: "#6b7280" }}>{su.firstName}</span>
              )}
              <select value={su.role} onChange={(e) => updateStaffRole(su, e.target.value)}>
                <option value="WAITER">WAITER</option>
                <option value="KITCHEN">KITCHEN</option>
                <option value="ADMIN">ADMIN</option>
              </select>
              <span>{su.isActive ? t(lang, "active") : t(lang, "inactive")}</span>
              <button onClick={() => resetStaffPassword(su)}>{t(lang, "resetPassword")}</button>
              <button onClick={() => editStaffProfile(su)}>{t(lang, "editProfile")}</button>
              <button onClick={() => toggleStaff(su)}>{su.isActive ? t(lang, "disable") : t(lang, "enable")}</button>
            </div>
          ))}
        </div>
        {profileEditingId != null && (
          <div style={{ marginTop: 10, border: "1px dashed #ddd", padding: 10 }}>
            <div style={{ display: "flex", gap: 8, flexWrap: "wrap", alignItems: "center" }}>
              <input
                placeholder={t(lang, "firstName")}
                value={profileDraft.firstName}
                onChange={(e) => setProfileDraft({ ...profileDraft, firstName: e.target.value })}
              />
              <input
                placeholder={t(lang, "lastName")}
                value={profileDraft.lastName}
                onChange={(e) => setProfileDraft({ ...profileDraft, lastName: e.target.value })}
              />
              <input
                placeholder={t(lang, "age")}
                value={profileDraft.age}
                onChange={(e) => setProfileDraft({ ...profileDraft, age: e.target.value })}
                style={{ width: 90 }}
              />
              <input
                placeholder={t(lang, "rating")}
                value={profileDraft.rating}
                onChange={(e) => setProfileDraft({ ...profileDraft, rating: e.target.value })}
                style={{ width: 90 }}
              />
              <select
                value={profileDraft.gender}
                onChange={(e) => setProfileDraft({ ...profileDraft, gender: e.target.value })}
              >
                <option value="">{t(lang, "gender")}</option>
                <option value="male">{t(lang, "genderMale")}</option>
                <option value="female">{t(lang, "genderFemale")}</option>
                <option value="other">{t(lang, "genderOther")}</option>
              </select>
              <input
                placeholder={t(lang, "experienceYears")}
                value={profileDraft.experienceYears}
                onChange={(e) => setProfileDraft({ ...profileDraft, experienceYears: e.target.value })}
                style={{ width: 140 }}
              />
              <label style={{ display: "flex", alignItems: "center", gap: 6 }}>
                <input
                  type="checkbox"
                  checked={profileDraft.recommended}
                  onChange={(e) => setProfileDraft({ ...profileDraft, recommended: e.target.checked })}
                />
                {t(lang, "recommended")}
              </label>
              <input
                placeholder={t(lang, "photoUrl")}
                value={profileDraft.photoUrl}
                onChange={(e) => setProfileDraft({ ...profileDraft, photoUrl: e.target.value })}
                style={{ minWidth: 240 }}
              />
              <input
                placeholder={t(lang, "favoriteItems")}
                value={profileDraft.favoriteItems}
                onChange={(e) => setProfileDraft({ ...profileDraft, favoriteItems: e.target.value })}
                style={{ minWidth: 240 }}
              />
              <button onClick={() => {
                const su = staff.find((s) => s.id === profileEditingId);
                if (su) saveStaffProfile(su);
              }}>{t(lang, "saveProfile")}</button>
              <button onClick={() => setProfileEditingId(null)}>{t(lang, "cancel")}</button>
            </div>
          </div>
        )}
      </section>

      <section style={{ marginTop: 24 }}>
        <h2>{t(lang, "staffReviews")}</h2>
        <div style={{ display: "flex", gap: 8, flexWrap: "wrap", alignItems: "center" }}>
          <select value={staffReviewWaiterId} onChange={(e) => setStaffReviewWaiterId(e.target.value ? Number(e.target.value) : "")}>
            <option value="">{t(lang, "reviewWaiter")} — {t(lang, "all")}</option>
            {staff.filter((s) => s.role === "WAITER").map((s) => (
              <option key={s.id} value={s.id}>{s.username}</option>
            ))}
          </select>
          <input
            type="number"
            min={1}
            max={200}
            placeholder={t(lang, "reviewLimit")}
            value={staffReviewLimit}
            onChange={(e) => setStaffReviewLimit(Number(e.target.value))}
            style={{ width: 120 }}
          />
          <button onClick={loadStaffReviews} disabled={staffReviewLoading}>
            {staffReviewLoading ? t(lang, "loading") : t(lang, "loadReviews")}
          </button>
        </div>
        <div style={{ marginTop: 10 }}>
          {staffReviews.length === 0 ? (
            <div style={{ color: "#666" }}>{t(lang, "noReviews")}</div>
          ) : (
            <div style={{ display: "grid", gap: 8 }}>
              {staffReviews.map((r) => (
                <div key={r.id} style={{ border: "1px solid #eee", borderRadius: 8, padding: 10 }}>
                  <div style={{ display: "flex", gap: 10, flexWrap: "wrap", alignItems: "center" }}>
                    <strong>{t(lang, "reviewWaiter")}: {r.staffUsername}</strong>
                    {r.tableNumber != null && <span>{t(lang, "reviewTable")}: {r.tableNumber}</span>}
                    <span>{t(lang, "reviewRating")}: {r.rating}/5</span>
                    {r.createdAt && <span>{t(lang, "reviewCreatedAt")}: {new Date(r.createdAt).toLocaleString()}</span>}
                    <span>{t(lang, "reviewGuest")}: {r.guestSessionId}</span>
                  </div>
                  {r.comment && <div style={{ marginTop: 6 }}>{t(lang, "reviewComment")}: {r.comment}</div>}
                </div>
              ))}
            </div>
          )}
        </div>
      </section>

      <section style={{ marginTop: 24 }}>
        <h2>{t(lang, "branchReviews")}</h2>
        <div style={{ display: "flex", gap: 8, flexWrap: "wrap", alignItems: "center" }}>
          <label>{t(lang, "reviewLimit")}
            <input type="number" min={1} max={200} value={branchReviewLimit} onChange={(e) => setBranchReviewLimit(Number(e.target.value))} style={{ width: 120, marginLeft: 6 }} />
          </label>
          <label>
            {t(lang, "hall")}
            <select value={branchReviewHallId} onChange={(e) => setBranchReviewHallId(e.target.value ? Number(e.target.value) : "")}>
              <option value="">{t(lang, "all")}</option>
              {halls.map((h) => (
                <option key={h.id} value={h.id}>{h.name}</option>
              ))}
            </select>
          </label>
          <label>
            {t(lang, "table")}
            <select value={branchReviewTableId} onChange={(e) => setBranchReviewTableId(e.target.value ? Number(e.target.value) : "")}>
              <option value="">{t(lang, "all")}</option>
              {tables.map((t) => (
                <option key={t.id} value={t.id}>#{t.number}</option>
              ))}
            </select>
          </label>
          <button onClick={loadBranchReviews} disabled={branchReviewLoading}>
            {branchReviewLoading ? t(lang, "loading") : t(lang, "loadReviews")}
          </button>
          {branchReviewSummary && (
            <span style={{ color: "#666" }}>
              {t(lang, "branchReviewsAvg")}: {branchReviewSummary.avgRating.toFixed(2)} • {t(lang, "branchReviewsCount")}: {branchReviewSummary.count}
            </span>
          )}
        </div>
        <div style={{ marginTop: 8, display: "flex", gap: 8, flexWrap: "wrap", alignItems: "center" }}>
          <strong>{t(lang, "branchReviewsExport")}</strong>
          <label>
            {t(lang, "from")}
            <input type="date" value={branchReviewExportFrom} onChange={(e) => setBranchReviewExportFrom(e.target.value)} />
          </label>
          <label>
            {t(lang, "to")}
            <input type="date" value={branchReviewExportTo} onChange={(e) => setBranchReviewExportTo(e.target.value)} />
          </label>
          <button onClick={downloadBranchReviewsCsv}>{t(lang, "branchReviewsDownload")}</button>
        </div>
        <div style={{ marginTop: 10 }}>
          {branchReviews.length === 0 ? (
            <div style={{ color: "#666" }}>{t(lang, "noReviews")}</div>
          ) : (
            <div style={{ display: "grid", gap: 8 }}>
              {branchReviews.map((r) => (
                <div key={r.id} style={{ border: "1px solid #eee", borderRadius: 8, padding: 10 }}>
                  <div style={{ display: "flex", gap: 10, flexWrap: "wrap", alignItems: "center" }}>
                    <span>{t(lang, "reviewRating")}: {r.rating}/5</span>
                    {r.createdAt && <span>{t(lang, "reviewCreatedAt")}: {new Date(r.createdAt).toLocaleString()}</span>}
                    <span>{t(lang, "reviewGuest")}: {r.guestSessionId}</span>
                  </div>
                  {r.comment && <div style={{ marginTop: 6 }}>{t(lang, "reviewComment")}: {r.comment}</div>}
                </div>
              ))}
            </div>
          )}
        </div>
      </section>

      <section style={{ marginTop: 24 }}>
        <h2>{t(lang, "chatExport")}</h2>
        <div style={{ display: "flex", gap: 8, flexWrap: "wrap", alignItems: "center" }}>
          <label>
            {t(lang, "from")}
            <input type="date" value={chatExportFrom} onChange={(e) => setChatExportFrom(e.target.value)} />
          </label>
          <label>
            {t(lang, "to")}
            <input type="date" value={chatExportTo} onChange={(e) => setChatExportTo(e.target.value)} />
          </label>
          <label>
            {t(lang, "waiter")}
            <select value={statsWaiterId} onChange={(e) => setStatsWaiterId(e.target.value ? Number(e.target.value) : "")}>
              <option value="">{t(lang, "all")}</option>
              {staff.filter((s) => s.role === "WAITER").map((w) => (
                <option key={w.id} value={w.id}>{w.username} #{w.id}</option>
              ))}
            </select>
          </label>
          <button onClick={downloadChatCsv}>{t(lang, "chatExportDownload")}</button>
        </div>
      </section>

      <section style={{ marginTop: 24 }}>
        <h2>{t(lang, "modifiers")}</h2>
        <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
          <input placeholder={t(lang, "groupNameRu")} value={newModGroupNameRu} onChange={(e) => setNewModGroupNameRu(e.target.value)} />
          <button onClick={createModGroup}>{t(lang, "addGroup")}</button>
        </div>
        <div style={{ marginTop: 10, display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(300px, 1fr))", gap: 12 }}>
          {modGroups.map((g) => (
            <div key={g.id} style={{ border: "1px solid #eee", borderRadius: 8, padding: 10 }}>
              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                <strong>{g.nameRu}</strong>
                <button onClick={() => toggleModGroup(g)}>{g.isActive ? t(lang, "disable") : t(lang, "enable")}</button>
              </div>
              <button onClick={() => { setActiveModGroupId(g.id); loadModOptions(g.id); }} style={{ marginTop: 8 }}>{t(lang, "loadOptions")}</button>
              {activeModGroupId === g.id && (
                <div style={{ marginTop: 8 }}>
                  <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
                    <input placeholder={t(lang, "optionRu")} value={newModOptionNameRu} onChange={(e) => setNewModOptionNameRu(e.target.value)} />
                    <input type="number" placeholder={t(lang, "priceCentsShort")} value={newModOptionPrice} onChange={(e) => setNewModOptionPrice(Number(e.target.value))} />
                    <button onClick={createModOption}>{t(lang, "addOption")}</button>
                  </div>
                  {(modOptions[g.id] ?? []).map((o) => (
                    <div key={o.id} style={{ display: "flex", gap: 8, alignItems: "center", padding: "4px 0" }}>
                      <span>{o.nameRu}</span>
                      <span>{o.priceCents}</span>
                      <button onClick={() => toggleModOption(g.id, o)}>{o.isActive ? t(lang, "disable") : t(lang, "enable")}</button>
                    </div>
                  ))}
                </div>
              )}
            </div>
          ))}
        </div>
      </section>

      <section style={{ marginTop: 24 }}>
        <h2>{t(lang, "itemModifierGroups")}</h2>
        <div style={{ marginTop: 10 }}>
          {items.map((it) => (
            <div key={it.id} style={{ border: "1px solid #eee", borderRadius: 8, padding: 10, marginBottom: 10 }}>
              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                <strong>{it.nameRu}</strong>
                <button onClick={() => loadItemModGroups(it.id)}>{t(lang, "load")}</button>
              </div>
              {(itemModGroups[it.id] ?? []).map((g, idx) => (
                <div key={`${it.id}-${g.groupId}`} style={{ display: "flex", gap: 8, alignItems: "center", marginTop: 6 }}>
                  <span>{t(lang, "groupLabel")}{g.groupId}</span>
                  <label><input type="checkbox" checked={g.isRequired} onChange={(e) => {
                    const next = (itemModGroups[it.id] ?? []).slice();
                    next[idx] = { ...g, isRequired: e.target.checked };
                    setItemModGroups((prev) => ({ ...prev, [it.id]: next }));
                  }} /> {t(lang, "required")}</label>
                  <input type="number" placeholder={t(lang, "min")} value={g.minSelect ?? ""} onChange={(e) => {
                    const next = (itemModGroups[it.id] ?? []).slice();
                    next[idx] = { ...g, minSelect: e.target.value ? Number(e.target.value) : null };
                    setItemModGroups((prev) => ({ ...prev, [it.id]: next }));
                  }} />
                  <input type="number" placeholder={t(lang, "max")} value={g.maxSelect ?? ""} onChange={(e) => {
                    const next = (itemModGroups[it.id] ?? []).slice();
                    next[idx] = { ...g, maxSelect: e.target.value ? Number(e.target.value) : null };
                    setItemModGroups((prev) => ({ ...prev, [it.id]: next }));
                  }} />
                  <input type="number" placeholder={t(lang, "sort")} value={g.sortOrder} onChange={(e) => {
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
                  <option value="">{t(lang, "addGroupOption")}</option>
                  {modGroups.map((g) => (
                    <option key={g.id} value={g.id}>{g.nameRu}</option>
                  ))}
                </select>
                <button onClick={() => saveItemModGroups(it.id, itemModGroups[it.id] ?? [])}>{t(lang, "save")}</button>
              </div>
            </div>
          ))}
        </div>
      </section>
    </main>
  );
}
