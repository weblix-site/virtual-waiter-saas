"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import Image from "next/image";
import { t, type Lang } from "@/app/i18n";

async function readApiError(res: Response, fallback: string): Promise<string> {
  try {
    const data = await res.json();
    if (data?.message) {
      return data.code ? `${data.message} (code: ${data.code})` : String(data.message);
    }
  } catch {
    // ignore parse errors
  }
  return fallback;
}

type MenuItem = {
  id: number;
  name: string;
  description?: string | null;
  ingredients?: string | null;
  allergens?: string | null;
  weight?: string | null;
  kcal?: number | null;
  proteinG?: number | null;
  fatG?: number | null;
  carbsG?: number | null;
  photos: string[];
  tags: string[];
  priceCents: number;
  currency: string;
};

type MenuCategory = {
  id: number;
  name: string;
  sortOrder: number;
  items: MenuItem[];
};

type MenuResponse = {
  branchId: number;
  locale: "ru" | "ro" | "en";
  categories: MenuCategory[];
};

type StartSessionResponse = {
  guestSessionId: number;
  tableId: number;
  tableNumber: number;
  branchId: number;
  locale: "ru" | "ro" | "en";
  otpRequired: boolean;
  isVerified: boolean;
  sessionSecret: string;
  currencyCode?: string;
  waiterName?: string | null;
  waiterPhotoUrl?: string | null;
  waiterRating?: number | null;
  waiterRecommended?: boolean | null;
  waiterExperienceYears?: number | null;
  waiterFavoriteItems?: string[] | null;
  waiterAvgRating?: number | null;
  waiterReviewsCount?: number | null;
};

type BillOptionsItem = {
  orderItemId: number;
  guestSessionId: number;
  name: string;
  qty: number;
  unitPriceCents: number;
  lineTotalCents: number;
};

type BillOptionsResponse = {
  allowPayOtherGuestsItems: boolean;
  allowPayWholeTable: boolean;
  tipsEnabled: boolean;
  tipsPercentages: number[];
  enablePartyPin: boolean;
  partyId: number | null;
  partyStatus?: string | null;
  partyExpiresAt?: string | null;
  partyGuestSessionIds?: number[];
  partyGuestCount?: number;
  myItems: BillOptionsItem[];
  tableItems: BillOptionsItem[];
  payCashEnabled?: boolean;
  payTerminalEnabled?: boolean;
  onlinePayEnabled?: boolean;
  onlinePayProvider?: string | null;
  onlinePayCurrencyCode?: string | null;
};

type BillItemLine = {
  orderItemId: number;
  name: string;
  qty: number;
  unitPriceCents: number;
  lineTotalCents: number;
};

type BillRequestResponse = {
  billRequestId: number;
  status: string;
  paymentMethod: string;
  mode: string;
  subtotalCents: number;
  discountCents: number;
  discountCode?: string | null;
  discountLabel?: string | null;
  serviceFeePercent?: number | null;
  serviceFeeCents?: number | null;
  taxPercent?: number | null;
  taxCents?: number | null;
  tipsPercent?: number | null;
  tipsAmountCents: number;
  totalCents: number;
  items: BillItemLine[];
};

type LoyaltyProfileResponse = {
  phone: string | null;
  pointsBalance: number;
  favorites: { menuItemId: number; name: string; qtyTotal: number }[];
  offers: { id: number; title: string; body?: string | null; discountCode?: string | null; startsAt?: string | null; endsAt?: string | null; isActive?: boolean | null }[];
};

type ModOption = { id: number; name: string; priceCents: number };
type ModGroup = { id: number; name: string; isRequired: boolean; minSelect?: number | null; maxSelect?: number | null; options: ModOption[] };
type MenuItemModifiersResponse = { menuItemId: number; groups: ModGroup[] };

type CartLine = { item: MenuItem; qty: number; comment?: string; modifierOptionIds?: number[]; modifierSummary?: string };

const API_BASE = process.env.NEXT_PUBLIC_API_BASE ?? "http://localhost:8080";

function money(priceCents: number, currency: string) {
  const v = (priceCents / 100).toFixed(2);
  return `${v} ${currency}`;
}

export default function TablePage({ params, searchParams }: any) {
  const tablePublicId: string = params.tablePublicId;
  const rawLang = String(searchParams?.lang ?? "").toLowerCase();
  const hasUrlLang = rawLang === "ru" || rawLang === "ro" || rawLang === "en";
  const requestLocale = hasUrlLang ? rawLang : "auto";
  const [lang, setLang] = useState<Lang>(hasUrlLang ? (rawLang as Lang) : "ru");
  const sig: string = (searchParams?.sig ?? "");
  const ts: string = (searchParams?.ts ?? "");
  // If QR signature is missing (e.g., dev link), show a friendly error.
  // In production, all QR links must include ?sig=...


  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [session, setSession] = useState<StartSessionResponse | null>(null);
  const [menu, setMenu] = useState<MenuResponse | null>(null);
  const [cart, setCart] = useState<CartLine[]>([]);
  const [placing, setPlacing] = useState(false);
  const [orderId, setOrderId] = useState<number | null>(null);
  const [orderStatus, setOrderStatus] = useState<string | null>(null);
  const [orderRefreshLoading, setOrderRefreshLoading] = useState(false);
  const [billRequestId, setBillRequestId] = useState<number | null>(null);
  const [billStatus, setBillStatus] = useState<string | null>(null);
  const [billError, setBillError] = useState<string | null>(null);
  const [billOptions, setBillOptions] = useState<BillOptionsResponse | null>(null);
  const [billMode, setBillMode] = useState<'MY' | 'SELECTED' | 'WHOLE_TABLE'>('MY');
  const [selectedItemIds, setSelectedItemIds] = useState<number[]>([]);
  const [billPayMethod, setBillPayMethod] = useState<'CASH' | 'TERMINAL' | 'ONLINE'>('CASH');
  const [billTipsPercent, setBillTipsPercent] = useState<number | ''>('');
  const [billPromoCode, setBillPromoCode] = useState('');
  const [promoApplied, setPromoApplied] = useState(false);
  const [billSummary, setBillSummary] = useState<BillRequestResponse | null>(null);
  const [billLoading, setBillLoading] = useState(false);
  const [billOptionsLoading, setBillOptionsLoading] = useState(false);
  const [onlineProvider, setOnlineProvider] = useState<string>("");
  const [paymentIntentId, setPaymentIntentId] = useState<number | null>(null);
  const [paymentRedirecting, setPaymentRedirecting] = useState(false);
  const [paymentConfirming, setPaymentConfirming] = useState(false);
  const [party, setParty] = useState<{ partyId: number; pin: string; expiresAt: string } | null>(null);
  const [modifiersByItem, setModifiersByItem] = useState<Record<number, MenuItemModifiersResponse>>({});
  const [modOpenByItem, setModOpenByItem] = useState<Record<number, boolean>>({});
  const [phone, setPhone] = useState("");
  const [otpChallengeId, setOtpChallengeId] = useState<number | null>(null);
  const [otpCode, setOtpCode] = useState("");
  const [otpSending, setOtpSending] = useState(false);
  const [otpVerifying, setOtpVerifying] = useState(false);
  const [billRefreshLoading, setBillRefreshLoading] = useState(false);
  const [ordersHistory, setOrdersHistory] = useState<any[]>([]);
  const [ordersLoading, setOrdersLoading] = useState(false);
  const [ordersExpanded, setOrdersExpanded] = useState(true);
  const [waiterCallActive, setWaiterCallActive] = useState(false);
  const [waiterCallStatus, setWaiterCallStatus] = useState<string | null>(null);
  const [waiterPhotoFailed, setWaiterPhotoFailed] = useState(false);
  const [waiterReviewRating, setWaiterReviewRating] = useState(5);
  const [waiterReviewComment, setWaiterReviewComment] = useState("");
  const [waiterReviewSending, setWaiterReviewSending] = useState(false);
  const [waiterReviewSent, setWaiterReviewSent] = useState(false);
  const [waiterReviewError, setWaiterReviewError] = useState<string | null>(null);
  const [chatMessages, setChatMessages] = useState<{ id: number; senderRole: string; message: string; createdAt?: string | null }[]>([]);
  const [chatMessage, setChatMessage] = useState("");
  const [chatSending, setChatSending] = useState(false);
  const [branchReviewRating, setBranchReviewRating] = useState(5);
  const [branchReviewComment, setBranchReviewComment] = useState("");
  const [branchReviewSending, setBranchReviewSending] = useState(false);
  const [branchReviewSent, setBranchReviewSent] = useState(false);
  const [branchReviewError, setBranchReviewError] = useState<string | null>(null);
  const [loyaltyProfile, setLoyaltyProfile] = useState<LoyaltyProfileResponse | null>(null);
  const [loyaltyLoading, setLoyaltyLoading] = useState(false);
  const [revealedOffers, setRevealedOffers] = useState<Record<number, boolean>>({});
  const [offersFilter, setOffersFilter] = useState<"ALL" | "ACTIVE" | "EXPIRING">("ALL");

  const menuItemById = useMemo(() => {
    const map = new Map<number, MenuItem>();
    if (menu?.categories) {
      for (const c of menu.categories) {
        for (const it of c.items) {
          map.set(it.id, it);
        }
      }
    }
    return map;
  }, [menu]);

  const formatOfferDate = useCallback((iso?: string | null) => {
    if (!iso) return "";
    const d = new Date(iso);
    if (Number.isNaN(d.getTime())) return "";
    return d.toLocaleString(lang === "ru" ? "ru-RU" : lang === "ro" ? "ro-RO" : "en-US", {
      year: "numeric",
      month: "2-digit",
      day: "2-digit",
      hour: "2-digit",
      minute: "2-digit",
    });
  }, [lang]);

  const cartTotalCents = useMemo(() => cart.reduce((sum, l) => sum + l.item.priceCents * l.qty, 0), [cart]);
  const myChargesCents = useMemo(() => (billOptions?.myItems ?? []).reduce((sum, it) => sum + it.lineTotalCents, 0), [billOptions]);
  const tableChargesCents = useMemo(() => (billOptions?.tableItems ?? []).reduce((sum, it) => sum + it.lineTotalCents, 0), [billOptions]);
  const myChargesCount = useMemo(() => (billOptions?.myItems ?? []).reduce((sum, it) => sum + it.qty, 0), [billOptions]);
  const tableChargesCount = useMemo(() => (billOptions?.tableItems ?? []).reduce((sum, it) => sum + it.qty, 0), [billOptions]);
  const currencyCode = session?.currencyCode ?? "MDL";

  const billItemsForMode = useMemo(() => {
    if (!billOptions) return [] as BillOptionsItem[];
    if (billMode === 'WHOLE_TABLE') return billOptions.tableItems;
    if (billMode === 'SELECTED') {
      return (billOptions.allowPayOtherGuestsItems ? billOptions.tableItems : billOptions.myItems);
    }
    return billOptions.myItems;
  }, [billOptions, billMode]);

  useEffect(() => {
    if (billMode !== 'SELECTED') {
      setSelectedItemIds([]);
    }
  }, [billMode]);

  useEffect(() => {
    if (!billOptions) return;
    if (billPayMethod === 'CASH' && billOptions.payCashEnabled === false) {
      if (billOptions.payTerminalEnabled === false) {
        setBillPayMethod(billOptions.onlinePayEnabled ? 'ONLINE' : 'CASH');
      } else {
        setBillPayMethod('TERMINAL');
      }
    }
    if (billPayMethod === 'TERMINAL' && billOptions.payTerminalEnabled === false) {
      if (billOptions.payCashEnabled === false) {
        setBillPayMethod(billOptions.onlinePayEnabled ? 'ONLINE' : 'TERMINAL');
      } else {
        setBillPayMethod('CASH');
      }
    }
    if (billPayMethod === 'ONLINE' && !billOptions.onlinePayEnabled) {
      if (billOptions.payCashEnabled !== false) setBillPayMethod('CASH');
      else if (billOptions.payTerminalEnabled !== false) setBillPayMethod('TERMINAL');
    }
    if (billOptions.onlinePayEnabled && billOptions.onlinePayProvider && !onlineProvider) {
      setOnlineProvider(billOptions.onlinePayProvider);
    }
  }, [billOptions, billPayMethod, onlineProvider]);

  const sessionHeaders = useCallback((): Record<string, string> => {
    const headers: Record<string, string> = {};
    if (session?.sessionSecret) headers["X-Session-Secret"] = session.sessionSecret;
    return headers;
  }, [session?.sessionSecret]);

  const loadLoyaltyProfile = useCallback(async () => {
    if (!session) return;
    if (!session.isVerified) {
      setLoyaltyProfile(null);
      return;
    }
    setLoyaltyLoading(true);
    try {
      const res = await fetch(`${API_BASE}/api/public/loyalty/profile?guestSessionId=${session.guestSessionId}`, {
        headers: { ...sessionHeaders() },
      });
      if (!res.ok) throw new Error(await readApiError(res, t(lang, "errorGeneric")));
      const body: LoyaltyProfileResponse = await res.json();
      setLoyaltyProfile(body);
    } catch {
      setLoyaltyProfile(null);
    } finally {
      setLoyaltyLoading(false);
    }
  }, [lang, session, sessionHeaders]);

  useEffect(() => {
    if (!session) return;
    loadLoyaltyProfile();
  }, [session, loadLoyaltyProfile]);

  useEffect(() => {
    if (typeof window === "undefined") return;
    const saved = window.localStorage.getItem("vw_offers_filter");
    if (saved === "ALL" || saved === "ACTIVE" || saved === "EXPIRING") {
      setOffersFilter(saved);
    }
  }, []);

  useEffect(() => {
    if (typeof window === "undefined") return;
    if (tablePublicId) {
      window.localStorage.removeItem("vw_offers_filter");
      setOffersFilter("ALL");
    }
  }, [tablePublicId, lang]);

  useEffect(() => {
    if (typeof window === "undefined") return;
    window.localStorage.setItem("vw_offers_filter", offersFilter);
  }, [offersFilter]);

  async function addToCart(item: MenuItem) {
    setOrderId(null);
    const loaded = await ensureModifiersLoaded(item.id);
    const mods = loaded ?? modifiersByItem[item.id];
    const hasRequired = !!mods?.groups?.some((g) => {
      const min = g.minSelect ?? (g.isRequired ? 1 : 0);
      return min > 0;
    });
    setCart((prev) => {
      const idx = prev.findIndex((x) => x.item.id === item.id);
      if (idx >= 0) {
        const next = prev.slice();
        next[idx] = { ...next[idx], qty: next[idx].qty + 1 };
        return next;
      }
      return [...prev, { item, qty: 1, comment: "", modifierOptionIds: [], modifierSummary: "" }];
    });
    if (hasRequired) {
      setModOpenByItem((prev) => ({ ...prev, [item.id]: true }));
    }
  }

  function dec(itemId: number) {
    setCart((prev) => {
      const idx = prev.findIndex((x) => x.item.id === itemId);
      if (idx < 0) return prev;
      const next = prev.slice();
      const q = next[idx].qty - 1;
      if (q <= 0) next.splice(idx, 1);
      else next[idx] = { ...next[idx], qty: q };
      return next;
    });
  }

  async function ensureModifiersLoaded(itemId: number) {
    if (modifiersByItem[itemId]) return modifiersByItem[itemId];
    const res = await fetch(`${API_BASE}/api/public/menu-item/${itemId}/modifiers?tablePublicId=${encodeURIComponent(tablePublicId)}&sig=${encodeURIComponent(sig)}&ts=${encodeURIComponent(ts)}&locale=${lang}`);
    if (res.ok) {
      const body: MenuItemModifiersResponse = await res.json();
      setModifiersByItem((prev) => ({ ...prev, [itemId]: body }));
      return body;
    }
    return null;
  }

  function validateModifiers(itemId: number, selectedIds: number[]) {
    const mods = modifiersByItem[itemId];
    if (!mods || mods.groups.length === 0) return true;
    for (const g of mods.groups) {
      const optionIds = new Set(g.options.map((o) => o.id));
      const count = selectedIds.filter((id) => optionIds.has(id)).length;
      const min = g.minSelect ?? (g.isRequired ? 1 : 0);
      const max = g.maxSelect ?? (g.isRequired ? 1 : undefined);
      if (count < min) return false;
      if (max != null && count > max) return false;
    }
    return true;
  }

  const missingRequiredGroups = useCallback((itemId: number, selectedIds: number[]) => {
    const mods = modifiersByItem[itemId];
    if (!mods || mods.groups.length === 0) return [] as string[];
    const missing: string[] = [];
    for (const g of mods.groups) {
      const optionIds = new Set(g.options.map((o) => o.id));
      const count = selectedIds.filter((id) => optionIds.has(id)).length;
      const min = g.minSelect ?? (g.isRequired ? 1 : 0);
      if (count < min) missing.push(g.name);
    }
    return missing;
  }, [modifiersByItem]);

  const cartHasMissingModifiers = useMemo(() => {
    return cart.some((l) => missingRequiredGroups(l.item.id, l.modifierOptionIds ?? []).length > 0);
  }, [cart, missingRequiredGroups]);

  const refreshBillOptions = useCallback(async () => {
    if (!session) return;
    setBillOptionsLoading(true);
    try {
      const boRes = await fetch(`${API_BASE}/api/public/bill-options?guestSessionId=${session.guestSessionId}`, {
        headers: { ...sessionHeaders() },
      });
      if (boRes.ok) {
        const bo: BillOptionsResponse = await boRes.json();
        setBillOptions(bo);
      }
    } catch (e: any) {
      setError(e?.message ?? t(lang, "errorGeneric"));
    } finally {
      setBillOptionsLoading(false);
    }
  }, [lang, session, sessionHeaders]);

  const loadOrdersHistory = useCallback(async () => {
    if (!session) return;
    setOrdersLoading(true);
    try {
      const res = await fetch(`${API_BASE}/api/public/orders?guestSessionId=${session.guestSessionId}`, {
        headers: { ...sessionHeaders() },
      });
      if (!res.ok) throw new Error(await readApiError(res, `Orders load failed (${res.status})`));
      const body = await res.json();
      setOrdersHistory(body);
    } catch (e: any) {
      setError(e?.message ?? t(lang, "errorGeneric"));
    } finally {
      setOrdersLoading(false);
    }
  }, [lang, session, sessionHeaders]);

  const refreshOrderStatus = useCallback(async () => {
    if (!session || !orderId) return;
    setOrderRefreshLoading(true);
    try {
      const res = await fetch(`${API_BASE}/api/public/orders/${orderId}?guestSessionId=${session.guestSessionId}`, {
        headers: { ...sessionHeaders() },
      });
      if (!res.ok) throw new Error(await readApiError(res, `Order status failed (${res.status})`));
      const body = await res.json();
      setOrderStatus(body.status);
    } catch (e: any) {
      setError(e?.message ?? t(lang, "orderFailed"));
    } finally {
      setOrderRefreshLoading(false);
    }
  }, [lang, orderId, session, sessionHeaders]);

  const refreshBillStatus = useCallback(async () => {
    if (!session || !billRequestId) return;
    setBillRefreshLoading(true);
    try {
      const res = await fetch(`${API_BASE}/api/public/bill-request/${billRequestId}?guestSessionId=${session.guestSessionId}`, {
        headers: { ...sessionHeaders() },
      });
      if (!res.ok) throw new Error(await readApiError(res, `Bill status failed (${res.status})`));
      const body: BillRequestResponse = await res.json();
      setBillStatus(body.status);
      setBillSummary(body);
    } catch (e: any) {
      setBillError(e?.message ?? t(lang, "billRequestFailed"));
    } finally {
      setBillRefreshLoading(false);
    }
  }, [billRequestId, lang, session, sessionHeaders]);

  useEffect(() => {
    let cancelled = false;
    async function load() {
      setLoading(true);
      setError(null);
      try {
        const ssRes = await fetch(`${API_BASE}/api/public/session/start`, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ tablePublicId, sig, ts: Number(ts), locale: requestLocale }),
        });
        if (!ssRes.ok) {
          throw new Error(await readApiError(ssRes, `${t(lang, "sessionStartFailed")} (${ssRes.status})`));
        }
        const ss: StartSessionResponse = await ssRes.json();
        if (cancelled) return;
        setSession(ss);
        if (!hasUrlLang && (ss.locale === "ru" || ss.locale === "ro" || ss.locale === "en") && ss.locale !== lang) {
          const nextLang = ss.locale;
          setLang(nextLang);
          try {
            const params = new URLSearchParams(searchParams || {});
            params.set("lang", nextLang);
            const nextUrl = `/t/${tablePublicId}?${params.toString()}`;
            if (typeof window !== "undefined") {
              window.history.replaceState({}, "", nextUrl);
            }
          } catch (_) {}
        }

        const menuLocale = (!hasUrlLang && (ss.locale === "ru" || ss.locale === "ro" || ss.locale === "en")) ? ss.locale : (hasUrlLang ? (rawLang as Lang) : "ru");
        const mRes = await fetch(`${API_BASE}/api/public/menu?tablePublicId=${encodeURIComponent(tablePublicId)}&sig=${encodeURIComponent(sig)}&ts=${encodeURIComponent(ts)}&locale=${menuLocale}`);
        if (!mRes.ok) {
          throw new Error(await readApiError(mRes, `${t(lang, "menuLoadFailed")} (${mRes.status})`));
        }
        const m: MenuResponse = await mRes.json();
        if (cancelled) return;
        setMenu(m);

        await refreshBillOptions();

        const wcRes = await fetch(`${API_BASE}/api/public/waiter-call/latest?guestSessionId=${ss.guestSessionId}`, {
          headers: { ...sessionHeaders() },
        });
        if (wcRes.ok && !cancelled) {
          const wc = await wcRes.json();
          setWaiterCallActive(true);
          setWaiterCallStatus(wc.status ?? "NEW");
        } else if (!cancelled) {
          setWaiterCallActive(false);
          setWaiterCallStatus(null);
        }

        const lastBillRes = await fetch(`${API_BASE}/api/public/bill-request/latest?guestSessionId=${ss.guestSessionId}`, {
          headers: { ...sessionHeaders() },
        });
        if (lastBillRes.ok) {
          const last: BillRequestResponse = await lastBillRes.json();
          if (!cancelled) {
            setBillRequestId(last.billRequestId);
            setBillStatus(last.status);
            setBillSummary(last);
          }
        }
      } catch (e: any) {
        if (cancelled) return;
        setError(e?.message ?? t(lang, "errorGeneric"));
      } finally {
        if (!cancelled) setLoading(false);
      }
    }
    load();
    return () => {
      cancelled = true;
    };
  }, [tablePublicId, requestLocale, sig, ts, hasUrlLang, lang, rawLang, refreshBillOptions, searchParams, sessionHeaders]);

  useEffect(() => {
    setWaiterPhotoFailed(false);
  }, [session?.waiterPhotoUrl]);

  useEffect(() => {
    if (!billRequestId || !session) return;
    const id = setInterval(() => {
      refreshBillStatus();
    }, 10000);
    return () => clearInterval(id);
  }, [billRequestId, refreshBillStatus, session]);

  useEffect(() => {
    if (!orderId || !session) return;
    const id = setInterval(() => {
      refreshOrderStatus();
    }, 10000);
    return () => clearInterval(id);
  }, [orderId, refreshOrderStatus, session]);

  const loadChat = useCallback(async () => {
    if (!session) return;
    const res = await fetch(`${API_BASE}/api/public/chat/messages?guestSessionId=${session.guestSessionId}`, {
      headers: { ...sessionHeaders() },
    });
    if (res.ok) {
      setChatMessages(await res.json());
    }
  }, [session, sessionHeaders]);

  useEffect(() => {
    if (!session) return;
    loadChat();
    const id = setInterval(() => {
      loadChat();
    }, 8000);
    return () => clearInterval(id);
  }, [session, loadChat]);

  useEffect(() => {
    if (!session) return;
    loadOrdersHistory();
    const id = setInterval(() => {
      loadOrdersHistory();
    }, 15000);
    return () => clearInterval(id);
  }, [session, loadOrdersHistory]);

  function orderStatusLabel(status: string) {
    const s = (status || "").toUpperCase();
    if (s === "NEW") return t(lang, "orderStatusNew");
    if (s === "ACCEPTED") return t(lang, "orderStatusAccepted");
    if (s === "IN_PROGRESS" || s === "COOKING") return t(lang, "orderStatusInProgress");
    if (s === "READY") return t(lang, "orderStatusReady");
    if (s === "SERVED") return t(lang, "orderStatusServed");
    if (s === "CLOSED") return t(lang, "orderStatusClosed");
    if (s === "CANCELLED") return t(lang, "orderStatusCancelled");
    return s || t(lang, "status");
  }

  function orderStatusColor(status: string) {
    const s = (status || "").toUpperCase();
    if (s === "NEW") return { bg: "#e0f2fe", fg: "#0369a1" };
    if (s === "ACCEPTED" || s === "IN_PROGRESS" || s === "COOKING") return { bg: "#ffedd5", fg: "#9a3412" };
    if (s === "READY") return { bg: "#dcfce7", fg: "#166534" };
    if (s === "SERVED" || s === "CLOSED") return { bg: "#e5e7eb", fg: "#374151" };
    if (s === "CANCELLED") return { bg: "#fee2e2", fg: "#991b1b" };
    return { bg: "#e5e7eb", fg: "#374151" };
  }

  async function placeOrder() {
    if (!session) return;
    if (cart.length === 0) return;
    setPlacing(true);
    setError(null);
    try {
      for (const l of cart) {
        await ensureModifiersLoaded(l.item.id);
        const ok = validateModifiers(l.item.id, l.modifierOptionIds ?? []);
        if (!ok) {
          throw new Error(t(lang, "modifiersRequired"));
        }
      }
      const res = await fetch(`${API_BASE}/api/public/orders`, {
        method: "POST",
        headers: { "Content-Type": "application/json", ...sessionHeaders() },
        body: JSON.stringify({
          guestSessionId: session.guestSessionId,
          items: cart.map((l) => ({
            menuItemId: l.item.id,
            qty: l.qty,
            comment: l.comment?.trim() || undefined,
            modifiersJson: l.modifierOptionIds && l.modifierOptionIds.length > 0
              ? JSON.stringify({ optionIds: l.modifierOptionIds })
              : undefined,
          })),
        }),
      });
      if (!res.ok) {
        if (res.status === 403 && session?.otpRequired && !session?.isVerified) {
          throw new Error(t(lang, "otpRequired"));
        }
        throw new Error(await readApiError(res, `${t(lang, "orderFailed")} (${res.status})`));
      }
      const body = await res.json();
      setOrderId(body.orderId);
      setOrderStatus(body.status);
      setCart([]);
      loadOrdersHistory();
      await refreshBillOptions();
    } catch (e: any) {
      setError(e?.message ?? t(lang, "orderFailed"));
    } finally {
      setPlacing(false);
    }
  }

  async function sendOtp() {
    if (!session) return;
    if (!phone.trim()) {
      alert(t(lang, "phoneRequired"));
      return;
    }
    setOtpSending(true);
    try {
      const res = await fetch(`${API_BASE}/api/public/otp/send`, {
        method: "POST",
        headers: { "Content-Type": "application/json", ...sessionHeaders() },
        body: JSON.stringify({ guestSessionId: session.guestSessionId, phoneE164: phone.trim(), locale: lang }),
      });
      if (!res.ok) throw new Error(await readApiError(res, `${t(lang, "otpSendFailed")} (${res.status})`));
      const body = await res.json();
      setOtpChallengeId(body.challengeId);
      if (body.devCode) alert(t(lang, "devCodePrefix") + body.devCode);
    } catch (e: any) {
      alert(e?.message ?? t(lang, "otpSendFailed"));
    } finally {
      setOtpSending(false);
    }
  }

  async function verifyOtp() {
    if (!session) return;
    if (!otpChallengeId) {
      alert(t(lang, "sendCodeFirst"));
      return;
    }
    if (!otpCode.trim()) {
      alert(t(lang, "enterCode"));
      return;
    }
    setOtpVerifying(true);
    try {
      const res = await fetch(`${API_BASE}/api/public/otp/verify`, {
        method: "POST",
        headers: { "Content-Type": "application/json", ...sessionHeaders() },
        body: JSON.stringify({ guestSessionId: session.guestSessionId, challengeId: otpChallengeId, code: otpCode.trim() }),
      });
      if (!res.ok) throw new Error(await readApiError(res, `${t(lang, "otpVerifyFailed")} (${res.status})`));
      setSession({ ...session, isVerified: true });
      alert(t(lang, "verified"));
    } catch (e: any) {
      alert(e?.message ?? t(lang, "otpVerifyFailed"));
    } finally {
      setOtpVerifying(false);
    }
  }

  async function callWaiter() {
    if (!session) return;
    try {
      const res = await fetch(`${API_BASE}/api/public/waiter-call`, {
        method: "POST",
        headers: { "Content-Type": "application/json", ...sessionHeaders() },
        body: JSON.stringify({ guestSessionId: session.guestSessionId }),
      });
      if (!res.ok) throw new Error(await readApiError(res, t(lang, "errorGeneric")));
      setWaiterCallActive(true);
      setWaiterCallStatus("NEW");
      alert(t(lang, "waiterCalled"));
    } catch (e: any) {
      alert(e?.message ?? t(lang, "errorGeneric"));
    }
  }

  async function cancelWaiterCall() {
    if (!session) return;
    try {
      const res = await fetch(`${API_BASE}/api/public/waiter-call/cancel`, {
        method: "POST",
        headers: { "Content-Type": "application/json", ...sessionHeaders() },
        body: JSON.stringify({ guestSessionId: session.guestSessionId }),
      });
      if (!res.ok) throw new Error(await readApiError(res, t(lang, "errorGeneric")));
      setWaiterCallActive(false);
      setWaiterCallStatus(null);
      alert(t(lang, "waiterCallCancelled"));
    } catch (e: any) {
      alert(e?.message ?? t(lang, "errorGeneric"));
    }
  }

  async function submitWaiterReview() {
    if (!session) return;
    if (!session.waiterName) {
      setWaiterReviewError(t(lang, "waiterNotAssigned"));
      return;
    }
    setWaiterReviewSending(true);
    setWaiterReviewError(null);
    try {
      const res = await fetch(`${API_BASE}/api/public/waiter-review`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          ...sessionHeaders(),
        },
        body: JSON.stringify({
          guestSessionId: session.guestSessionId,
          rating: waiterReviewRating,
          comment: waiterReviewComment,
        }),
      });
      if (!res.ok) {
        throw new Error(await readApiError(res, t(lang, "waiterReviewFailed")));
      }
      setWaiterReviewSent(true);
    } catch (e: any) {
      setWaiterReviewError(e?.message ?? t(lang, "waiterReviewFailed"));
    } finally {
      setWaiterReviewSending(false);
    }
  }

  async function sendChat() {
    if (!session) return;
    const msg = chatMessage.trim();
    if (!msg) return;
    setChatSending(true);
    try {
      const res = await fetch(`${API_BASE}/api/public/chat/send`, {
        method: "POST",
        headers: { "Content-Type": "application/json", ...sessionHeaders() },
        body: JSON.stringify({ guestSessionId: session.guestSessionId, message: msg }),
      });
      if (!res.ok) throw new Error(await readApiError(res, t(lang, "errorGeneric")));
      setChatMessage("");
      await loadChat();
    } finally {
      setChatSending(false);
    }
  }

  async function submitBranchReview() {
    if (!session) return;
    setBranchReviewSending(true);
    setBranchReviewError(null);
    try {
      const res = await fetch(`${API_BASE}/api/public/branch-review`, {
        method: "POST",
        headers: { "Content-Type": "application/json", ...sessionHeaders() },
        body: JSON.stringify({
          guestSessionId: session.guestSessionId,
          rating: branchReviewRating,
          comment: branchReviewComment,
        }),
      });
      if (!res.ok) throw new Error(await readApiError(res, t(lang, "branchReviewFailed")));
      setBranchReviewSent(true);
    } catch (e: any) {
      setBranchReviewError(e?.message ?? t(lang, "branchReviewFailed"));
    } finally {
      setBranchReviewSending(false);
    }
  }

  useEffect(() => {
    if (!session) return;
    let stopped = false;
    const tick = async () => {
      if (stopped) return;
      try {
        const wcRes = await fetch(`${API_BASE}/api/public/waiter-call/latest?guestSessionId=${session.guestSessionId}`, {
          headers: { ...sessionHeaders() },
        });
        if (stopped) return;
        if (wcRes.ok) {
          const wc = await wcRes.json();
          setWaiterCallActive(true);
          setWaiterCallStatus(wc.status ?? "NEW");
        } else {
          setWaiterCallActive(false);
          setWaiterCallStatus(null);
        }
      } catch (_) {
        // ignore polling errors
      }
    };
    const id = window.setInterval(tick, 8000);
    tick();
    return () => {
      stopped = true;
      window.clearInterval(id);
    };
  }, [session, sessionHeaders]);
  async function createPin() {
    if (!session) return;
    try {
      const res = await fetch(`${API_BASE}/api/public/party/create`, {
        method: "POST",
        headers: { "Content-Type": "application/json", ...sessionHeaders() },
        body: JSON.stringify({ guestSessionId: session.guestSessionId }),
      });
      if (!res.ok) throw new Error(await readApiError(res, `${t(lang, "pinCreateFailed")} (${res.status})`));
      const body = await res.json();
      setParty({ partyId: body.partyId, pin: body.pin, expiresAt: body.expiresAt });
      alert(t(lang, "pinCreated") + body.pin);
    } catch (e: any) {
      alert(e?.message ?? t(lang, "pinCreateFailed"));
    }
  }

  async function joinByPin() {
    if (!session) return;
    const pin = prompt(t(lang, "pinPrompt"));
    if (!pin) return;
    try {
      const res = await fetch(`${API_BASE}/api/public/party/join`, {
        method: "POST",
        headers: { "Content-Type": "application/json", ...sessionHeaders() },
        body: JSON.stringify({ guestSessionId: session.guestSessionId, pin }),
      });
      if (!res.ok) throw new Error(await readApiError(res, `${t(lang, "pinJoinFailed")} (${res.status})`));
      const body = await res.json();
      setParty({ partyId: body.partyId, pin: body.pin, expiresAt: body.expiresAt });
      alert(t(lang, "pinJoined") + body.pin);
    } catch (e: any) {
      alert(e?.message ?? t(lang, "pinJoinFailed"));
    }
  }

  async function closeParty() {
    if (!session) return;
    try {
      const res = await fetch(`${API_BASE}/api/public/party/close`, {
        method: "POST",
        headers: { "Content-Type": "application/json", ...sessionHeaders() },
        body: JSON.stringify({ guestSessionId: session.guestSessionId }),
      });
      if (!res.ok) throw new Error(await readApiError(res, `${t(lang, "partyCloseFailed")} (${res.status})`));
      setParty(null);
      alert(t(lang, "partyClosed"));
    } catch (e: any) {
      alert(e?.message ?? t(lang, "partyCloseFailed"));
    }
  }


  async function requestBill() {
    if (!session) return;
    setBillError(null);
    setBillLoading(true);
    try {
      if (billMode === 'SELECTED' && selectedItemIds.length === 0) {
        throw new Error(t(lang, "billSelectItems"));
      }
      if (billPayMethod === 'ONLINE' && !onlineProvider) {
        throw new Error(t(lang, "onlinePayNotReady"));
      }
      const payload: any = {
        guestSessionId: session.guestSessionId,
        mode: billMode,
        paymentMethod: billPayMethod,
      };
      if (billMode === 'SELECTED') payload.orderItemIds = selectedItemIds;
      if (billTipsPercent !== "") payload.tipsPercent = billTipsPercent;
      if (billPromoCode.trim()) payload.promoCode = billPromoCode.trim();

      const res = await fetch(`${API_BASE}/api/public/bill-request/create`, {
        method: "POST",
        headers: { "Content-Type": "application/json", ...sessionHeaders() },
        body: JSON.stringify(payload),
      });
      if (!res.ok) {
        throw new Error(await readApiError(res, `${t(lang, "billRequestFailed")} (${res.status})`));
      }
      const body: BillRequestResponse = await res.json().catch(() => ({} as BillRequestResponse));
      setBillRequestId(body.billRequestId);
      setBillStatus(body.status);
      setBillSummary(body);
      setPromoApplied(!!billPromoCode.trim());
      alert(t(lang, "billRequested"));
    } catch (e: any) {
      setBillError(e?.message ?? t(lang, "billRequestFailed"));
    } finally {
      setBillLoading(false);
    }
  }

  async function startOnlinePayment() {
    if (!session || !billRequestId) return;
    if (!onlineProvider) {
      setBillError(t(lang, "onlinePayNotReady"));
      return;
    }
    setBillError(null);
    setPaymentRedirecting(true);
    try {
      const res = await fetch(`${API_BASE}/api/public/payments/create`, {
        method: "POST",
        headers: { "Content-Type": "application/json", ...sessionHeaders() },
        body: JSON.stringify({
          guestSessionId: session.guestSessionId,
          billRequestId,
          provider: onlineProvider,
          returnUrl: window.location.href,
        }),
      });
      if (!res.ok) {
        throw new Error(await readApiError(res, `${t(lang, "billRequestFailed")} (${res.status})`));
      }
      const body = await res.json();
      const intentId = Number(body.intentId);
      if (!Number.isNaN(intentId)) {
        setPaymentIntentId(intentId);
        try {
          localStorage.setItem("vw_last_intent_id", String(intentId));
        } catch {
          // ignore storage errors
        }
      }
      if (body.redirectUrl) {
        window.location.href = body.redirectUrl;
      }
    } catch (e: any) {
      setBillError(e?.message ?? t(lang, "billRequestFailed"));
    } finally {
      setPaymentRedirecting(false);
    }
  }

  async function confirmOnlinePayment() {
    if (!session) return;
    let intentId = paymentIntentId;
    if (!intentId) {
      try {
        const saved = localStorage.getItem("vw_last_intent_id");
        if (saved) intentId = Number(saved);
      } catch {
        // ignore
      }
    }
    if (!intentId || Number.isNaN(intentId)) {
      setBillError(t(lang, "onlinePayNotReady"));
      return;
    }
    setBillError(null);
    setPaymentConfirming(true);
    try {
      const res = await fetch(`${API_BASE}/api/public/payments/confirm`, {
        method: "POST",
        headers: { "Content-Type": "application/json", ...sessionHeaders() },
        body: JSON.stringify({ guestSessionId: session.guestSessionId, intentId }),
      });
      if (!res.ok) {
        throw new Error(await readApiError(res, `${t(lang, "billRequestFailed")} (${res.status})`));
      }
      const body = await res.json();
      if (body.status && String(body.status).toUpperCase() === "PAID") {
        setBillStatus("PAID_CONFIRMED");
      }
    } catch (e: any) {
      setBillError(e?.message ?? t(lang, "billRequestFailed"));
    } finally {
      setPaymentConfirming(false);
    }
  }

  async function cancelBillRequest() {
    if (!session || !billRequestId) return;
    setBillError(null);
    setBillLoading(true);
    try {
      const res = await fetch(`${API_BASE}/api/public/bill-request/${billRequestId}/cancel`, {
        method: "POST",
        headers: { "Content-Type": "application/json", ...sessionHeaders() },
        body: JSON.stringify({ guestSessionId: session.guestSessionId }),
      });
      if (!res.ok) throw new Error(await readApiError(res, `Cancel failed (${res.status})`));
      const body = await res.json().catch(() => ({}));
      setBillStatus(body.status);
      alert(body.status === "CANCELLED" ? t(lang, "billCancelled") : t(lang, "billRequested"));
    } catch (e: any) {
      setBillError(e?.message ?? t(lang, "billRequestFailed"));
    } finally {
      setBillLoading(false);
    }
  }

  async function closeBillRequest() {
    if (!session || !billRequestId) return;
    setBillError(null);
    setBillLoading(true);
    try {
      const res = await fetch(`${API_BASE}/api/public/bill-request/${billRequestId}/close`, {
        method: "POST",
        headers: { "Content-Type": "application/json", ...sessionHeaders() },
        body: JSON.stringify({ guestSessionId: session.guestSessionId }),
      });
      if (!res.ok) throw new Error(await readApiError(res, `Close failed (${res.status})`));
      const body = await res.json().catch(() => ({}));
      setBillStatus(body.status);
      alert(t(lang, "billClosed"));
    } catch (e: any) {
      setBillError(e?.message ?? t(lang, "billRequestFailed"));
    } finally {
      setBillLoading(false);
    }
  }

  async function toggleModifiers(itemId: number) {
    const isOpen = !!modOpenByItem[itemId];
    if (!isOpen && !modifiersByItem[itemId]) {
      const res = await fetch(`${API_BASE}/api/public/menu-item/${itemId}/modifiers?tablePublicId=${encodeURIComponent(tablePublicId)}&sig=${encodeURIComponent(sig)}&ts=${encodeURIComponent(ts)}&locale=${lang}`);
      if (res.ok) {
        const body: MenuItemModifiersResponse = await res.json();
        setModifiersByItem((prev) => ({ ...prev, [itemId]: body }));
      }
    }
    setModOpenByItem((prev) => ({ ...prev, [itemId]: !isOpen }));
  }

  function toggleOption(item: MenuItem, group: ModGroup, opt: ModOption) {
    setCart((prev) => {
      const idx = prev.findIndex((l) => l.item.id === item.id);
      const next = prev.slice();
      if (idx < 0) {
        next.push({ item, qty: 1, comment: "", modifierOptionIds: [], modifierSummary: "" });
      }
      return next.map((l) => {
        if (l.item.id !== item.id) return l;
        const current = new Set(l.modifierOptionIds ?? []);
        const groupOptionIds = new Set((group.options ?? []).map((o) => o.id));
        const selectedInGroup = Array.from(current).filter((id) => groupOptionIds.has(id));
        const max = group.maxSelect ?? (group.isRequired ? 1 : undefined);
        const min = group.minSelect ?? (group.isRequired ? 1 : 0);

        if (current.has(opt.id)) {
          current.delete(opt.id);
        } else {
          if (max != null && selectedInGroup.length >= max) {
            if (max === 1) {
              for (const id of selectedInGroup) current.delete(id);
            } else {
              return l;
            }
          }
          current.add(opt.id);
        }

        if (min > 0 && Array.from(current).filter((id) => groupOptionIds.has(id)).length < min) {
          // allow empty; server will validate
        }

        const mods = modifiersByItem[item.id];
        let summary = "";
        if (mods) {
          const allOptions = mods.groups.flatMap((g) => g.options);
          summary = Array.from(current)
            .map((id) => allOptions.find((o) => o.id === id)?.name)
            .filter(Boolean)
            .join(", ");
        }
        return { ...l, modifierOptionIds: Array.from(current), modifierSummary: summary };
      });
    });
  }




  if (loading) {
    return <main style={{ padding: 20 }}>{t(lang, "loading")}</main>;
  }

  if (error) {
    return (
      <main style={{ padding: 20 }}>
        <h2 style={{ marginTop: 0 }}>{t(lang, "error")}</h2>
        <pre style={{ whiteSpace: "pre-wrap" }}>{error}</pre>
      </main>
    );
  }

  return (
    <main style={{ padding: 16, maxWidth: 900, margin: "0 auto", fontFamily: "system-ui, -apple-system, Segoe UI, Roboto" }}>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", gap: 12, flexWrap: "wrap" }}>
        <div>
          <h1 style={{ margin: 0 }}>{t(lang, "table")} #{session?.tableNumber}</h1>
          <div style={{ color: "#666" }}>{t(lang, "session")}: {session?.guestSessionId}</div>
        </div>
        <div style={{ display: "flex", gap: 8 }}>
          <a href={`/t/${tablePublicId}?lang=ru&sig=${encodeURIComponent(sig)}&ts=${encodeURIComponent(ts)}`}>RU</a>
          <a href={`/t/${tablePublicId}?lang=ro&sig=${encodeURIComponent(sig)}&ts=${encodeURIComponent(ts)}`}>RO</a>
          <a href={`/t/${tablePublicId}?lang=en&sig=${encodeURIComponent(sig)}&ts=${encodeURIComponent(ts)}`}>EN</a>
        </div>
      </div>

      <section style={{ marginTop: 14, border: "1px solid #eee", borderRadius: 12, padding: 12 }}>
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", gap: 12, flexWrap: "wrap" }}>
          <strong>{t(lang, "waiterSection")}</strong>
          {waiterCallActive && (
            <span style={{ fontSize: 12, color: "#666" }}>
              {t(lang, "waiterCallStatus")}: {waiterCallStatus === "ACKNOWLEDGED"
                ? t(lang, "waiterCallAcknowledged")
                : t(lang, "waiterCallPending")}
            </span>
          )}
        </div>
        <div style={{ display: "flex", alignItems: "center", gap: 12, marginTop: 10, flexWrap: "wrap" }}>
          <div style={{
            width: 56,
            height: 56,
            borderRadius: "50%",
            background: "#f3f4f6",
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            overflow: "hidden"
          }}>
            {session?.waiterPhotoUrl && !waiterPhotoFailed ? (
              <Image
                src={session.waiterPhotoUrl}
                alt={session.waiterName ?? "Waiter"}
                width={56}
                height={56}
                style={{ width: 56, height: 56, objectFit: "cover" }}
                unoptimized
                onError={() => setWaiterPhotoFailed(true)}
              />
            ) : (
              <span style={{ color: "#9ca3af", fontSize: 12 }}>{t(lang, "waiterLabel")}</span>
            )}
          </div>
          <div style={{ minWidth: 180 }}>
            <div style={{ fontWeight: 600 }}>
              {session?.waiterName ? session.waiterName : t(lang, "waiterNotAssigned")}
            </div>
            <div style={{ fontSize: 12, color: "#666" }}>{t(lang, "waiterLabel")}</div>
            {(session?.waiterRecommended || session?.waiterRating != null || session?.waiterExperienceYears != null || (session?.waiterFavoriteItems && session.waiterFavoriteItems.length > 0)) && (
              <div style={{ marginTop: 6, fontSize: 12, color: "#4b5563" }}>
                {session?.waiterRecommended && (
                  <span style={{ display: "inline-block", padding: "2px 6px", borderRadius: 999, background: "#ecfeff", color: "#0f766e", marginRight: 6 }}>
                    {t(lang, "waiterRecommended")}
                  </span>
                )}
                {session?.waiterRating != null && (
                  <div>{t(lang, "waiterRating")}: {session.waiterRating}/5</div>
                )}
                {session?.waiterAvgRating != null && (
                  <div>
                    {t(lang, "waiterAvgRating")}: {session.waiterAvgRating.toFixed(1)}/5
                    {session.waiterReviewsCount != null ? ` (${session.waiterReviewsCount})` : ""}
                  </div>
                )}
                {session?.waiterExperienceYears != null && (
                  <div>{t(lang, "waiterExperienceYears")}: {session.waiterExperienceYears} {t(lang, "yearsShort")}</div>
                )}
                {session?.waiterFavoriteItems && session.waiterFavoriteItems.length > 0 && (
                  <div>{t(lang, "waiterFavoriteItems")}: {session.waiterFavoriteItems.join(", ")}</div>
                )}
              </div>
            )}
          </div>
          <div style={{ display: "flex", gap: 10, flexWrap: "wrap" }}>
            {!waiterCallActive ? (
              <button onClick={callWaiter} style={{ padding: "10px 14px" }}>
                {t(lang, "callWaiter")}
              </button>
            ) : (
              <button onClick={cancelWaiterCall} style={{ padding: "10px 14px" }}>
                {t(lang, "cancelWaiterCall")}
              </button>
            )}
          </div>
        </div>
        {session?.waiterName && (
          <div style={{ marginTop: 12, borderTop: "1px dashed #eee", paddingTop: 10 }}>
            <div style={{ fontWeight: 600, marginBottom: 6 }}>{t(lang, "waiterReviewTitle")}</div>
            {waiterReviewSent ? (
              <div style={{ color: "#059669", fontSize: 12 }}>{t(lang, "waiterReviewThanks")}</div>
            ) : (
              <div style={{ display: "flex", gap: 8, flexWrap: "wrap", alignItems: "center" }}>
                <label style={{ fontSize: 12 }}>{t(lang, "waiterReviewRate")}</label>
                <select value={waiterReviewRating} onChange={(e) => setWaiterReviewRating(Number(e.target.value))}>
                  {[1,2,3,4,5].map((n) => (
                    <option key={n} value={n}>{n}</option>
                  ))}
                </select>
                <input
                  placeholder={t(lang, "waiterReviewComment")}
                  value={waiterReviewComment}
                  onChange={(e) => setWaiterReviewComment(e.target.value)}
                  style={{ minWidth: 240 }}
                />
                <button onClick={submitWaiterReview} disabled={waiterReviewSending}>
                  {waiterReviewSending ? t(lang, "sending") : t(lang, "waiterReviewSubmit")}
                </button>
                {waiterReviewError && <span style={{ color: "#dc2626", fontSize: 12 }}>{waiterReviewError}</span>}
              </div>
            )}
          </div>
        )}
        {session?.waiterName && (
          <div style={{ marginTop: 12, borderTop: "1px dashed #eee", paddingTop: 10 }}>
            <div style={{ fontWeight: 600, marginBottom: 6 }}>{t(lang, "chatTitle")}</div>
            <div style={{ maxHeight: 160, overflowY: "auto", border: "1px solid #eee", borderRadius: 8, padding: 8 }}>
              {chatMessages.length === 0 ? (
                <div style={{ color: "#666", fontSize: 12 }}>{t(lang, "chatEmpty")}</div>
              ) : (
                chatMessages.map((m) => (
                  <div key={m.id} style={{ marginBottom: 6, textAlign: m.senderRole === "GUEST" ? "right" : "left" }}>
                    <span
                      style={{
                        display: "inline-block",
                        padding: "6px 8px",
                        borderRadius: 10,
                        background: m.senderRole === "GUEST" ? "#e0f2fe" : "#f3f4f6",
                        fontSize: 12,
                      }}
                    >
                      {m.message}
                    </span>
                  </div>
                ))
              )}
            </div>
            <div style={{ display: "flex", gap: 8, marginTop: 8 }}>
              <input
                placeholder={t(lang, "chatPlaceholder")}
                value={chatMessage}
                onChange={(e) => setChatMessage(e.target.value)}
                style={{ flex: 1 }}
              />
              <button onClick={sendChat} disabled={chatSending}>
                {chatSending ? t(lang, "sending") : t(lang, "chatSend")}
              </button>
            </div>
          </div>
        )}
        <div style={{ marginTop: 12, borderTop: "1px dashed #eee", paddingTop: 10 }}>
          <div style={{ fontWeight: 600, marginBottom: 6 }}>{t(lang, "branchReviewTitle")}</div>
          {branchReviewSent ? (
            <div style={{ color: "#059669", fontSize: 12 }}>{t(lang, "branchReviewThanks")}</div>
          ) : (
            <div style={{ display: "flex", gap: 8, flexWrap: "wrap", alignItems: "center" }}>
              <label style={{ fontSize: 12 }}>{t(lang, "branchReviewRate")}</label>
              <select value={branchReviewRating} onChange={(e) => setBranchReviewRating(Number(e.target.value))}>
                {[1,2,3,4,5].map((n) => (
                  <option key={n} value={n}>{n}</option>
                ))}
              </select>
              <input
                placeholder={t(lang, "branchReviewComment")}
                value={branchReviewComment}
                onChange={(e) => setBranchReviewComment(e.target.value)}
                style={{ minWidth: 240 }}
              />
              <button onClick={submitBranchReview} disabled={branchReviewSending}>
                {branchReviewSending ? t(lang, "sending") : t(lang, "branchReviewSubmit")}
              </button>
              {branchReviewError && <span style={{ color: "#dc2626", fontSize: 12 }}>{branchReviewError}</span>}
            </div>
          )}
        </div>
      </section>

      <section style={{ marginTop: 14, border: "1px solid #eee", borderRadius: 12, padding: 12 }}>
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", gap: 12, flexWrap: "wrap" }}>
          <strong>{t(lang, "loyaltyTitle")}</strong>
          <button onClick={loadLoyaltyProfile} disabled={loyaltyLoading} style={{ padding: "6px 10px" }}>
            {loyaltyLoading ? t(lang, "loading") : t(lang, "billRefresh")}
          </button>
        </div>
        {!session?.isVerified ? (
          <div style={{ marginTop: 8, color: "#666", fontSize: 12 }}>{t(lang, "loyaltyUnavailable")}</div>
        ) : (
          <div style={{ marginTop: 8 }}>
            <div style={{ fontWeight: 600 }}>{t(lang, "loyaltyPoints")}: {loyaltyProfile?.pointsBalance ?? 0}</div>
            <div style={{ marginTop: 10 }}>
              <div style={{ fontWeight: 600, marginBottom: 4 }}>{t(lang, "favoriteItems")}</div>
              {(loyaltyProfile?.favorites?.length ?? 0) === 0 ? (
                <div style={{ color: "#666", fontSize: 12 }}>{t(lang, "noFavorites")}</div>
              ) : (
                <div style={{ display: "grid", gap: 6 }}>
                  {loyaltyProfile?.favorites?.map((f) => {
                    const item = menuItemById.get(f.menuItemId);
                    return (
                      <div key={f.menuItemId} style={{ display: "flex", alignItems: "center", justifyContent: "space-between", gap: 8 }}>
                        <span style={{ border: "1px solid #ddd", borderRadius: 999, padding: "4px 8px", fontSize: 12 }}>
                          {f.name} × {f.qtyTotal}
                        </span>
                        {item && (
                          <button onClick={() => addToCart(item)} style={{ padding: "4px 8px", fontSize: 12 }}>
                            {t(lang, "addToCart")}
                          </button>
                        )}
                      </div>
                    );
                  })}
                </div>
              )}
            </div>
            <div style={{ marginTop: 10 }}>
              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", gap: 8, marginBottom: 4, flexWrap: "wrap" }}>
                <div style={{ fontWeight: 600 }}>{t(lang, "personalOffers")}</div>
                <div style={{ display: "flex", gap: 6, flexWrap: "wrap" }}>
                  <button
                    onClick={() => setOffersFilter("ALL")}
                    style={{ padding: "4px 8px", fontSize: 12, border: offersFilter === "ALL" ? "2px solid #111" : "1px solid #ddd", borderRadius: 999 }}
                  >
                    {t(lang, "offersFilterAll")}
                  </button>
                  <button
                    onClick={() => setOffersFilter("ACTIVE")}
                    style={{ padding: "4px 8px", fontSize: 12, border: offersFilter === "ACTIVE" ? "2px solid #111" : "1px solid #ddd", borderRadius: 999 }}
                  >
                    {t(lang, "offersFilterActive")}
                  </button>
                  <button
                    onClick={() => setOffersFilter("EXPIRING")}
                    style={{ padding: "4px 8px", fontSize: 12, border: offersFilter === "EXPIRING" ? "2px solid #111" : "1px solid #ddd", borderRadius: 999 }}
                  >
                    {t(lang, "offersFilterExpiring")}
                  </button>
                </div>
              </div>
              {(() => {
                const now = Date.now();
                const list = (loyaltyProfile?.offers ?? []).filter((o) => {
                  if (o.isActive === false) return false;
                  const startOk = !o.startsAt || Date.parse(o.startsAt) <= now;
                  const endOk = !o.endsAt || Date.parse(o.endsAt) >= now;
                  if (!startOk || !endOk) return false;
                  if (offersFilter === "EXPIRING") {
                    if (!o.endsAt) return false;
                    const endMs = Date.parse(o.endsAt);
                    if (Number.isNaN(endMs)) return false;
                    const diffHours = (endMs - now) / (1000 * 60 * 60);
                    return diffHours > 0 && diffHours <= 72;
                  }
                  return true;
                }).sort((a, b) => {
                  const aEnd = a.endsAt ? Date.parse(a.endsAt) : Number.POSITIVE_INFINITY;
                  const bEnd = b.endsAt ? Date.parse(b.endsAt) : Number.POSITIVE_INFINITY;
                  return aEnd - bEnd;
                });
                if (list.length === 0) {
                  return <div style={{ color: "#666", fontSize: 12 }}>{t(lang, "noOffers")}</div>;
                }
                return (
                  <div style={{ display: "grid", gap: 8 }}>
                    {list.map((o) => {
                      const endMs = o.endsAt ? Date.parse(o.endsAt) : null;
                      const nowDate = new Date();
                      const endDate = endMs ? new Date(endMs) : null;
                      const sameDay = endDate ? nowDate.toDateString() === endDate.toDateString() : false;
                      const diffHours = endMs ? (endMs - now) / (1000 * 60 * 60) : null;
                      const expiringSoon = diffHours != null && diffHours > 0 && diffHours <= 72;
                      const cardBorder = sameDay ? "1px solid #f59e0b" : expiringSoon ? "1px solid #fbbf24" : "1px solid #eee";
                      const cardBg = sameDay ? "#fffbeb" : expiringSoon ? "#fffbf5" : "#fff";
                      return (
                      <div key={o.id} style={{ border: cardBorder, background: cardBg, borderRadius: 8, padding: 8 }}>
                        <div style={{ fontWeight: 600 }}>{o.title}</div>
                        {o.body && <div style={{ fontSize: 12, color: "#555", marginTop: 4 }}>{o.body}</div>}
                        <div style={{ marginTop: 6, fontSize: 12, color: "#666" }}>
                          {o.startsAt && (
                            <div>{t(lang, "offerValidFrom")}: {formatOfferDate(o.startsAt)}</div>
                          )}
                          {o.endsAt && (
                            <div>{t(lang, "offerValidUntil")}: {formatOfferDate(o.endsAt)}</div>
                          )}
                        </div>
                        {sameDay && (
                          <div style={{ marginTop: 6, fontSize: 12, color: "#b45309", fontWeight: 600 }}>{t(lang, "offerExpiresToday")}</div>
                        )}
                        {!sameDay && expiringSoon && (
                          <div style={{ marginTop: 6, fontSize: 12, color: "#b45309", fontWeight: 600 }}>{t(lang, "offerExpiresSoon")}</div>
                        )}
                        {diffHours != null && diffHours > 0 && (
                          <div style={{ marginTop: 4, fontSize: 12, color: "#6b7280" }}>
                            {t(lang, "offerHoursLeft")}: {Math.ceil(diffHours)}
                          </div>
                        )}
                        {o.discountCode && (
                          <div style={{ marginTop: 6, display: "flex", gap: 8, flexWrap: "wrap", alignItems: "center" }}>
                            {!revealedOffers[o.id] ? (
                              <button
                                onClick={() => setRevealedOffers((prev) => ({ ...prev, [o.id]: true }))}
                                style={{ padding: "4px 8px", fontSize: 12 }}
                              >
                                {t(lang, "showPromo")}
                              </button>
                            ) : (
                              <>
                                <div style={{ fontSize: 12 }}>
                                  {t(lang, "promoCode")}: <strong>{o.discountCode}</strong>
                                </div>
                                <button
                                  onClick={() => setRevealedOffers((prev) => ({ ...prev, [o.id]: false }))}
                                  style={{ padding: "4px 8px", fontSize: 12 }}
                                >
                                  {t(lang, "hidePromo")}
                                </button>
                              </>
                            )}
                          </div>
                        )}
                        {o.discountCode && revealedOffers[o.id] && (
                          <button
                            onClick={() => {
                              setBillPromoCode(o.discountCode ?? "");
                              setPromoApplied(true);
                              setTimeout(() => setPromoApplied(false), 2000);
                              const el = document.getElementById("bill-section");
                              if (el) {
                                el.scrollIntoView({ behavior: "smooth", block: "start" });
                              }
                            }}
                            style={{ marginTop: 6, padding: "4px 8px", fontSize: 12 }}
                          >
                            {t(lang, "usePromo")}
                          </button>
                        )}
                      </div>
                    );
                  })}
                  </div>
                );
              })()}
            </div>
          </div>
        )}
      </section>

      <div style={{ marginTop: 12, display: "flex", gap: 12, flexWrap: "wrap", alignItems: "center" }}>

        {billOptions?.enablePartyPin && (
          <>
            <button onClick={createPin} style={{ padding: "10px 14px" }}>
              {t(lang, "createPin")}
            </button>
            <button onClick={joinByPin} style={{ padding: "10px 14px" }}>
              {t(lang, "joinPin")}
            </button>
            {party && (
              <button onClick={closeParty} style={{ padding: "10px 14px" }}>
                {t(lang, "partyClose")}
              </button>
            )}
          </>
        )}
        <button onClick={requestBill} disabled={billLoading} style={{ padding: "10px 14px" }}>
          {billLoading
            ? t(lang, "sending")
            : t(lang, "requestBill")}
        </button>
        {party && (
          <div style={{ padding: "10px 14px", border: "1px dashed #ccc", borderRadius: 8 }}>
            <strong>{t(lang, "pin")}:</strong> {party.pin}
            {billOptions?.partyStatus && (
              <div style={{ color: "#666", fontSize: 12, marginTop: 4 }}>
                {t(lang, "status")}: {billOptions.partyStatus}
                {billOptions.partyExpiresAt ? ` • ${t(lang, "expires")}: ${billOptions.partyExpiresAt}` : ""}
              </div>
            )}
            {(billOptions?.partyGuestCount ?? 0) > 0 && (
              <div style={{ color: "#666", fontSize: 12, marginTop: 4 }}>
                {t(lang, "partyGuests")}: {billOptions?.partyGuestCount}
              </div>
            )}
            {billOptions?.partyGuestSessionIds && billOptions.partyGuestSessionIds.length > 0 && (
              <div style={{ marginTop: 6, display: "flex", gap: 6, flexWrap: "wrap" }}>
                {billOptions.partyGuestSessionIds.map((id) => (
                  <span key={id} style={{ padding: "2px 6px", border: "1px solid #ddd", borderRadius: 999, fontSize: 12 }}>
                    {t(lang, "guest")} #{id}
                  </span>
                ))}
              </div>
            )}
          </div>
        )}

      </div>

      {session?.otpRequired && !session?.isVerified && (
        <div style={{ marginTop: 12, padding: 12, border: "1px solid #f0c", borderRadius: 8 }}>
          <div style={{ fontWeight: 600, marginBottom: 8 }}>
            {t(lang, "otpTitle")}
          </div>
          <div style={{ display: "flex", gap: 8, flexWrap: "wrap", alignItems: "center" }}>
            <input
              value={phone}
              onChange={(e) => setPhone(e.target.value)}
              placeholder={t(lang, "phonePlaceholder")}
              style={{ padding: "10px 12px", minWidth: 220, border: "1px solid #ddd", borderRadius: 8 }}
            />
            <button disabled={otpSending} onClick={sendOtp} style={{ padding: "10px 14px" }}>
              {otpSending ? t(lang, "sendCodeProgress") : t(lang, "sendCode")}
            </button>
          </div>
          <div style={{ display: "flex", gap: 8, flexWrap: "wrap", alignItems: "center", marginTop: 10 }}>
            <input
              value={otpCode}
              onChange={(e) => setOtpCode(e.target.value)}
              placeholder={t(lang, "otpCodePlaceholder")}
              style={{ padding: "10px 12px", width: 120, border: "1px solid #ddd", borderRadius: 8 }}
            />
            <button disabled={otpVerifying} onClick={verifyOtp} style={{ padding: "10px 14px" }}>
              {otpVerifying ? t(lang, "verifying") : t(lang, "verify")}
            </button>
          </div>
          <div style={{ marginTop: 8, color: "#666", fontSize: 12 }}>
            {t(lang, "otpDevHint")}
          </div>
        </div>
      )}

      {orderId && (
        <div style={{ marginTop: 12, padding: 12, border: "1px solid #ddd", borderRadius: 8 }}>
          #{orderId} {t(lang, "orderSent")}.
          <div style={{ marginTop: 6, display: "flex", gap: 8, alignItems: "center", flexWrap: "wrap" }}>
            <strong>{t(lang, "status")}:</strong> <span>{orderStatus ?? "?"}</span>
            <button onClick={refreshOrderStatus} disabled={orderRefreshLoading} style={{ padding: "6px 10px" }}>
              {orderRefreshLoading ? t(lang, "loading") : (lang === "en" ? "Refresh" : lang === "ro" ? "Reîmprospătează" : "Обновить")}
            </button>
          </div>
        </div>
      )}

      <h2 style={{ marginTop: 20 }}>{t(lang, "menu")}</h2>
      {menu?.categories.map((cat) => (
        <section key={cat.id} style={{ marginBottom: 18 }}>
          <h3 style={{ margin: "10px 0" }}>{cat.name}</h3>
          <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(260px, 1fr))", gap: 10 }}>
            {cat.items.map((it) => (
              <div key={it.id} style={{ border: "1px solid #eee", borderRadius: 10, padding: 12 }}>
                {it.photos?.[0] && (
                  <Image
                    src={it.photos[0]}
                    alt={it.name}
                    width={520}
                    height={280}
                    style={{ width: "100%", height: 140, objectFit: "cover", borderRadius: 8, marginBottom: 8 }}
                    unoptimized
                  />
                )}
                <div style={{ display: "flex", justifyContent: "space-between", gap: 8 }}>
                  <strong>{it.name}</strong>
                  <span>{money(it.priceCents, it.currency)}</span>
                </div>
                {(it.weight || it.kcal || it.proteinG || it.fatG || it.carbsG) && (
                  <div style={{ display: "flex", gap: 8, flexWrap: "wrap", fontSize: 12, color: "#666", marginTop: 4 }}>
                    {it.weight && (
                      <span style={{ padding: "2px 6px", border: "1px solid #eee", borderRadius: 999 }}>
                        {t(lang, "weightShort")}: {it.weight}
                      </span>
                    )}
                    {it.kcal && (
                      <span style={{ padding: "2px 6px", border: "1px solid #eee", borderRadius: 999 }}>
                        {t(lang, "calories")}: {it.kcal}
                      </span>
                    )}
                    {it.proteinG && (
                      <span style={{ padding: "2px 6px", border: "1px solid #eee", borderRadius: 999 }}>
                        {t(lang, "protein")}: {it.proteinG}г
                      </span>
                    )}
                    {it.fatG && (
                      <span style={{ padding: "2px 6px", border: "1px solid #eee", borderRadius: 999 }}>
                        {t(lang, "fat")}: {it.fatG}г
                      </span>
                    )}
                    {it.carbsG && (
                      <span style={{ padding: "2px 6px", border: "1px solid #eee", borderRadius: 999 }}>
                        {t(lang, "carbs")}: {it.carbsG}г
                      </span>
                    )}
                  </div>
                )}
                {it.description && <p style={{ margin: "8px 0", color: "#444" }}>{it.description}</p>}
                {it.ingredients && <div style={{ fontSize: 12, color: "#666" }}>{it.ingredients}</div>}
                {it.allergens && <div style={{ fontSize: 12, color: "#b11e46" }}>{it.allergens}</div>}
                <div style={{ marginTop: 10, display: "flex", gap: 8, alignItems: "center", flexWrap: "wrap" }}>
                  {cart.find((c) => c.item.id === it.id) ? (
                    <>
                      <button onClick={() => dec(it.id)}>-</button>
                      <span>{cart.find((c) => c.item.id === it.id)?.qty ?? 0}</span>
                      <button onClick={() => addToCart(it)}>+</button>
                    </>
                  ) : (
                    <button onClick={() => addToCart(it)} style={{ padding: "8px 12px" }}>
                      {t(lang, "addToCart")}
                    </button>
                  )}
                  <button onClick={() => toggleModifiers(it.id)} style={{ padding: "6px 10px" }}>
                    {t(lang, "modifiers")}
                  </button>
                </div>
                <a href={`/t/${tablePublicId}/item/${it.id}?lang=${lang}&sig=${encodeURIComponent(sig)}&ts=${encodeURIComponent(ts)}`} style={{ display: "inline-block", marginTop: 6 }}>
                  {t(lang, "details")}
                </a>
                {modOpenByItem[it.id] && modifiersByItem[it.id]?.groups?.length ? (
                  <div style={{ marginTop: 8, borderTop: "1px dashed #ddd", paddingTop: 8 }}>
                    {modifiersByItem[it.id].groups.map((g) => (
                      <div key={g.id} style={{ marginBottom: 8 }}>
                        <div style={{ fontWeight: 600, display: "flex", alignItems: "center", gap: 6, flexWrap: "wrap" }}>
                          {g.name}
                          <span
                            style={{
                              fontSize: 11,
                              padding: "2px 6px",
                              borderRadius: 999,
                              background: g.isRequired ? "#fee2e2" : "#e0f2fe",
                              color: g.isRequired ? "#991b1b" : "#0369a1",
                            }}
                          >
                            {g.isRequired ? t(lang, "modifiersRequiredLabel") : t(lang, "modifiersOptionalLabel")}
                          </span>
                          <span style={{ fontSize: 11, color: "#666" }}>
                            {t(lang, "modifiersSelected")}:{" "}
                            {(() => {
                              const optionIds = new Set(g.options.map((o) => o.id));
                              const selected = cart.find((c) => c.item.id === it.id)?.modifierOptionIds ?? [];
                              return selected.filter((id) => optionIds.has(id)).length;
                            })()}
                          </span>
                        </div>
                        <div style={{ display: "flex", gap: 6, flexWrap: "wrap" }}>
                          {g.options.map((o) => {
                            const selected = cart.find((c) => c.item.id === it.id)?.modifierOptionIds?.includes(o.id) ?? false;
                            return (
                              <button
                                key={o.id}
                                onClick={() => toggleOption(it, g, o)}
                                style={{
                                  padding: "6px 8px",
                                  borderRadius: 8,
                                  border: selected ? "2px solid #333" : "1px solid #ddd",
                                  background: selected ? "#111" : "#fff",
                                  color: selected ? "#fff" : "#111",
                                }}
                              >
                                {o.name} {o.priceCents ? `+${money(o.priceCents, it.currency)}` : ""}
                              </button>
                            );
                          })}
                        </div>
                        <div style={{ color: "#666", fontSize: 12, marginTop: 4 }}>
                          {g.minSelect ? `${t(lang, "choose")} ${g.minSelect}` : ""}
                          {g.maxSelect ? ` / ${g.maxSelect}` : ""}
                        </div>
                      </div>
                    ))}
                  </div>
                ) : null}
              </div>
            ))}
          </div>
        </section>
      ))}

      <h2 style={{ marginTop: 20 }}>{t(lang, "cart")}</h2>
      {cart.length === 0 ? (
        <p style={{ color: "#666" }}>{t(lang, "cartEmpty")}</p>
      ) : (
        <div style={{ border: "1px solid #eee", borderRadius: 10, padding: 12 }}>
          {cart.map((l) => {
            const missing = missingRequiredGroups(l.item.id, l.modifierOptionIds ?? []);
            return (
              <div key={l.item.id} style={{ display: "flex", justifyContent: "space-between", alignItems: "center", padding: "6px 0" }}>
              <div>
                <div><strong>{l.item.name}</strong></div>
                <div style={{ color: "#666", fontSize: 12 }}>{money(l.item.priceCents, l.item.currency)}</div>
                {l.modifierSummary && (
                  <div style={{ color: "#666", fontSize: 12 }}>{l.modifierSummary}</div>
                )}
                {missing.length > 0 && (
                  <div style={{ color: "#b11e46", fontSize: 12 }}>{t(lang, "modifiersMissing")}: {missing.join(", ")}</div>
                )}
                <div style={{ marginTop: 6, display: "flex", gap: 8, flexWrap: "wrap" }}>
                  <input
                    value={l.comment ?? ""}
                    onChange={(e) => setCart((prev) => prev.map((x) => x.item.id === l.item.id ? { ...x, comment: e.target.value } : x))}
                    placeholder={t(lang, "comment")}
                    style={{ padding: "6px 8px", border: "1px solid #ddd", borderRadius: 6, minWidth: 160 }}
                  />
                </div>
              </div>
              <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
                <button onClick={() => dec(l.item.id)}>-</button>
                <span>{l.qty}</span>
                <button onClick={() => addToCart(l.item)}>+</button>
              </div>
              </div>
            );
          })}
          <hr style={{ border: 0, borderTop: "1px solid #eee", margin: "10px 0" }} />
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
            <strong>{t(lang, "total")}</strong>
            <strong>{money(cartTotalCents, currencyCode)}</strong>
          </div>
          {cartHasMissingModifiers && (
            <div style={{ color: "#b11e46", marginTop: 8 }}>{t(lang, "modifiersRequired")}</div>
          )}
          <button disabled={placing || cartHasMissingModifiers} onClick={placeOrder} style={{ marginTop: 10, padding: "10px 14px", width: "100%" }}>
            {placing ? t(lang, "sending") : t(lang, "placeOrder")}
          </button>
        </div>
      )}

      <h2 style={{ marginTop: 24 }}>{t(lang, "myOrders")}</h2>
      <div style={{ display: "flex", gap: 8, flexWrap: "wrap", marginTop: 6 }}>
        <button onClick={loadOrdersHistory} disabled={ordersLoading}>
          {ordersLoading ? t(lang, "loading") : t(lang, "refreshMyOrders")}
        </button>
        <button onClick={() => setOrdersExpanded((v) => !v)}>
          {ordersExpanded ? t(lang, "hideOrders") : t(lang, "showOrders")}
        </button>
      </div>
      {ordersLoading ? (
        <p style={{ color: "#666" }}>{t(lang, "loading")}</p>
      ) : !ordersExpanded ? null : ordersHistory.length === 0 ? (
        <p style={{ color: "#666" }}>{t(lang, "noOrders")}</p>
      ) : (
        <div style={{ border: "1px solid #eee", borderRadius: 10, padding: 12 }}>
          {ordersHistory.map((o) => (
            <div key={o.orderId} style={{ padding: "8px 0", borderBottom: "1px dashed #eee" }}>
              <div style={{ display: "flex", justifyContent: "space-between", gap: 8 }}>
                <strong>{t(lang, "order")} #{o.orderId}</strong>
                <span
                  style={{
                    padding: "2px 8px",
                    borderRadius: 999,
                    fontSize: 12,
                    background: orderStatusColor(o.status).bg,
                    color: orderStatusColor(o.status).fg,
                    alignSelf: "center",
                  }}
                >
                  {orderStatusLabel(o.status)}
                </span>
              </div>
              <div style={{ color: "#666", fontSize: 12 }}>{o.createdAt}</div>
              <div style={{ marginTop: 6 }}>
                {(o.items ?? []).map((it: any) => (
                  <div key={it.id} style={{ fontSize: 13 }}>
                    {it.name} × {it.qty} — {money(it.unitPriceCents, currencyCode)}
                  </div>
                ))}
              </div>
            </div>
          ))}
        </div>
      )}

      <h2 style={{ marginTop: 24 }}>{t(lang, "currentCharges")}</h2>
      {!billOptions ? (
        <p style={{ color: "#666" }}>{t(lang, "loading")}</p>
      ) : billOptions.myItems.length === 0 && billOptions.tableItems.length === 0 ? (
        <p style={{ color: "#666" }}>{t(lang, "noCharges")}</p>
      ) : (
        <div style={{ border: "1px solid #eee", borderRadius: 10, padding: 12 }}>
          <div style={{ display: "flex", gap: 8, flexWrap: "wrap", alignItems: "center" }}>
            <button onClick={refreshBillOptions} disabled={billOptionsLoading}>
              {billOptionsLoading ? t(lang, "loading") : t(lang, "refreshCharges")}
            </button>
          </div>
          <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(220px, 1fr))", gap: 10, marginTop: 10 }}>
            <div style={{ border: "1px solid #f0f0f0", borderRadius: 8, padding: 10 }}>
              <div style={{ fontWeight: 600 }}>{t(lang, "myItemsLabel")}</div>
              <div style={{ color: "#666", fontSize: 12, marginTop: 4 }}>
                {myChargesCount} {t(lang, "itemsCount")}
              </div>
              <div style={{ marginTop: 6, fontSize: 16 }}>{money(myChargesCents, currencyCode)}</div>
            </div>
            <div style={{ border: "1px solid #f0f0f0", borderRadius: 8, padding: 10 }}>
              <div style={{ fontWeight: 600 }}>{t(lang, "tableItemsLabel")}</div>
              <div style={{ color: "#666", fontSize: 12, marginTop: 4 }}>
                {tableChargesCount} {t(lang, "itemsCount")}
              </div>
              <div style={{ marginTop: 6, fontSize: 16 }}>{money(tableChargesCents, currencyCode)}</div>
            </div>
          </div>
        </div>
      )}

      <h2 id="bill-section" style={{ marginTop: 24 }}>{t(lang, "payment")}</h2>
      {!billOptions ? (
        <p style={{ color: "#666" }}>{t(lang, "loading")}</p>
      ) : (
        <div style={{ border: "1px solid #eee", borderRadius: 10, padding: 12 }}>
          <div style={{ display: "flex", gap: 12, flexWrap: "wrap" }}>
            <label><input type="radio" checked={billMode === 'MY'} onChange={() => setBillMode('MY')} /> {t(lang, "myItems")}</label>
            <label><input type="radio" checked={billMode === 'SELECTED'} onChange={() => setBillMode('SELECTED')} /> {t(lang, "selected")}</label>
            {billOptions.allowPayWholeTable && (
              <label><input type="radio" checked={billMode === 'WHOLE_TABLE'} onChange={() => setBillMode('WHOLE_TABLE')} /> {t(lang, "wholeTable")}</label>
            )}
          </div>

          <div style={{ marginTop: 12 }}>
            {billItemsForMode.length === 0 ? (
              <p style={{ color: "#666" }}>{t(lang, "noUnpaid")}</p>
            ) : (
              <div style={{ border: "1px solid #f0f0f0", borderRadius: 8, padding: 8 }}>
                {billItemsForMode.map((it) => (
                  <div key={it.orderItemId} style={{ display: "flex", justifyContent: "space-between", alignItems: "center", padding: "6px 0", borderBottom: "1px dashed #eee" }}>
                    <div>
                      <div>
                        {billMode === 'SELECTED' && (
                          <input
                            type="checkbox"
                            checked={selectedItemIds.includes(it.orderItemId)}
                            onChange={(e) => {
                              setSelectedItemIds((prev) => e.target.checked
                                ? [...prev, it.orderItemId]
                                : prev.filter((id) => id !== it.orderItemId)
                              );
                            }}
                            style={{ marginRight: 8 }}
                          />
                        )}
                        <strong>{it.name}</strong>
                      </div>
                      <div style={{ color: "#666", fontSize: 12 }}>{it.qty} × {money(it.unitPriceCents, currencyCode)}</div>
                      {billOptions.allowPayOtherGuestsItems && it.guestSessionId !== session?.guestSessionId && (
                        <div style={{ color: "#999", fontSize: 12 }}>{t(lang, "otherGuest")}</div>
                      )}
                    </div>
                    <div><strong>{money(it.lineTotalCents, currencyCode)}</strong></div>
                  </div>
                ))}
              </div>
            )}
          </div>

          <div style={{ marginTop: 12, display: "flex", gap: 12, flexWrap: "wrap" }}>
            <label>
              <input
                type="radio"
                disabled={billOptions.payCashEnabled === false}
                checked={billPayMethod === 'CASH'}
                onChange={() => setBillPayMethod('CASH')}
              /> {t(lang, "cash")}
            </label>
            <label>
              <input
                type="radio"
                disabled={billOptions.payTerminalEnabled === false}
                checked={billPayMethod === 'TERMINAL'}
                onChange={() => setBillPayMethod('TERMINAL')}
              /> {t(lang, "terminal")}
            </label>
            {billOptions.onlinePayEnabled && (
              <label>
                <input
                  type="radio"
                  checked={billPayMethod === 'ONLINE'}
                  onChange={() => setBillPayMethod('ONLINE')}
                /> {t(lang, "onlinePay")}
              </label>
            )}
          </div>

          {billOptions.onlinePayEnabled && billPayMethod === 'ONLINE' && (
            <div style={{ marginTop: 10, display: "flex", gap: 8, flexWrap: "wrap", alignItems: "center" }}>
              <label style={{ minWidth: 120 }}>{t(lang, "onlineProvider")}</label>
              <select
                value={onlineProvider}
                onChange={(e) => setOnlineProvider(e.target.value)}
                disabled={!!billOptions.onlinePayProvider}
                style={{ minWidth: 180 }}
              >
                <option value="">{t(lang, "onlinePaySelectProvider")}</option>
                {(!billOptions.onlinePayProvider || billOptions.onlinePayProvider === "MAIB") && (
                  <option value="MAIB">{t(lang, "providerMaib")}</option>
                )}
                {(!billOptions.onlinePayProvider || billOptions.onlinePayProvider === "PAYNET") && (
                  <option value="PAYNET">{t(lang, "providerPaynet")}</option>
                )}
                {(!billOptions.onlinePayProvider || billOptions.onlinePayProvider === "MIA") && (
                  <option value="MIA">{t(lang, "providerMia")}</option>
                )}
              </select>
              <div style={{ fontSize: 12, color: "#6b7280" }}>{t(lang, "onlinePayProviderHint")}</div>
            </div>
          )}

          {billOptions.tipsEnabled && (
            <div style={{ marginTop: 12 }}>
              <div style={{ marginBottom: 6 }}>{t(lang, "tips")}</div>
              <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
                {billOptions.tipsPercentages.map((p) => (
                  <button
                    key={p}
                    onClick={() => setBillTipsPercent(p)}
                    style={{ padding: "6px 10px", border: billTipsPercent === p ? "2px solid #333" : "1px solid #ddd", borderRadius: 8 }}
                  >
                    {p}%
                  </button>
                ))}
                <button onClick={() => setBillTipsPercent('')} style={{ padding: "6px 10px", border: "1px solid #ddd", borderRadius: 8 }}>
                  {t(lang, "none")}
                </button>
              </div>
            </div>
          )}

          <div style={{ marginTop: 12, display: "flex", gap: 8, flexWrap: "wrap", alignItems: "center" }}>
            <label style={{ minWidth: 120 }}>{t(lang, "promoCode")}</label>
            <input
              value={billPromoCode}
              onChange={(e) => setBillPromoCode(e.target.value)}
              placeholder={t(lang, "promoCodePlaceholder")}
              style={{ padding: "6px 10px", border: "1px solid #ddd", borderRadius: 8, minWidth: 180 }}
            />
            {promoApplied && (
              <span style={{ fontSize: 12, color: "#059669" }}>{t(lang, "promoApplied")}</span>
            )}
          </div>

          {billSummary && (
            <div style={{ marginTop: 12, borderTop: "1px dashed #eee", paddingTop: 8, fontSize: 14 }}>
              <div style={{ display: "flex", justifyContent: "space-between" }}>
                <span>{t(lang, "subtotal")}</span>
                <span>{money(billSummary.subtotalCents, currencyCode)}</span>
              </div>
              {billSummary.discountCents > 0 && (
                <div style={{ display: "flex", justifyContent: "space-between" }}>
                  <span>
                    {t(lang, "discount")}
                    {billSummary.discountLabel || billSummary.discountCode ? ` (${billSummary.discountLabel || billSummary.discountCode})` : ""}
                  </span>
                  <span>-{money(billSummary.discountCents, currencyCode)}</span>
                </div>
              )}
              {(billSummary.serviceFeeCents ?? 0) > 0 && (
                <div style={{ display: "flex", justifyContent: "space-between" }}>
                  <span>{t(lang, "serviceFee")}</span>
                  <span>{money(billSummary.serviceFeeCents ?? 0, currencyCode)}</span>
                </div>
              )}
              {(billSummary.taxCents ?? 0) > 0 && (
                <div style={{ display: "flex", justifyContent: "space-between" }}>
                  <span>{t(lang, "tax")}</span>
                  <span>{money(billSummary.taxCents ?? 0, currencyCode)}</span>
                </div>
              )}
              {billSummary.tipsAmountCents > 0 && (
                <div style={{ display: "flex", justifyContent: "space-between" }}>
                  <span>{t(lang, "tips")}</span>
                  <span>{money(billSummary.tipsAmountCents, currencyCode)}</span>
                </div>
              )}
              <div style={{ display: "flex", justifyContent: "space-between", fontWeight: 600, marginTop: 6 }}>
                <span>{t(lang, "total")}</span>
                <span>{money(billSummary.totalCents, currencyCode)}</span>
              </div>
            </div>
          )}

          {billError && <div style={{ color: "#b11e46", marginTop: 10 }}>{billError}</div>}
        {billRequestId && (
            <div style={{ marginTop: 12, paddingTop: 8, borderTop: "1px dashed #eee" }}>
              <div style={{ display: "flex", gap: 8, flexWrap: "wrap", alignItems: "center" }}>
                <strong>{t(lang, "status")}:</strong>{" "}
                <span>
                  {billStatus === "CREATED"
                    ? t(lang, "billStatusCreated")
                    : billStatus === "PAID_CONFIRMED"
                      ? t(lang, "billStatusPaid")
                      : billStatus === "CANCELLED"
                        ? t(lang, "billStatusCancelled")
                        : billStatus === "CLOSED"
                          ? t(lang, "billStatusClosed")
                          : billStatus === "EXPIRED"
                            ? t(lang, "billStatusExpired")
                            : t(lang, "billStatusUnknown")}
                </span>
                <button onClick={refreshBillStatus} disabled={billRefreshLoading} style={{ padding: "6px 10px" }}>
                  {billRefreshLoading ? t(lang, "loading") : t(lang, "billRefresh")}
                </button>
                {billStatus === "CREATED" && (
                  <button onClick={cancelBillRequest} disabled={billLoading} style={{ padding: "6px 10px" }}>
                    {t(lang, "billCancel")}
                  </button>
                )}
                {billStatus === "CREATED" && billSummary?.paymentMethod === "ONLINE" && (
                  <>
                    <button onClick={startOnlinePayment} disabled={paymentRedirecting} style={{ padding: "6px 10px" }}>
                      {paymentRedirecting ? t(lang, "loading") : t(lang, "onlinePayStart")}
                    </button>
                    <button onClick={confirmOnlinePayment} disabled={paymentConfirming} style={{ padding: "6px 10px" }}>
                      {paymentConfirming ? t(lang, "loading") : t(lang, "onlinePayConfirm")}
                    </button>
                  </>
                )}
                {billStatus === "PAID_CONFIRMED" && (
                  <button onClick={closeBillRequest} disabled={billLoading} style={{ padding: "6px 10px" }}>
                    {t(lang, "billClose")}
                  </button>
                )}
              </div>
              {billStatus === "EXPIRED" && (
                <div style={{ marginTop: 8, color: "#b11e46" }}>{t(lang, "billExpiredNote")}</div>
              )}
            </div>
          )}
        </div>
      )}
    </main>
  );
}
