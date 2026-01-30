"use client";

import { useEffect, useMemo, useState } from "react";
import { t, type Lang } from "@/app/i18n";

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
  const rawLang = String(searchParams?.lang ?? "ru").toLowerCase();
  const lang: Lang = rawLang === "ro" || rawLang === "en" ? rawLang : "ru";
  const sig: string = (searchParams?.sig ?? "");
  // If QR signature is missing (e.g., dev link), show a friendly error.
  // In production, all QR links must include ?sig=...


  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [session, setSession] = useState<StartSessionResponse | null>(null);
  const [menu, setMenu] = useState<MenuResponse | null>(null);
  const [cart, setCart] = useState<CartLine[]>([]);
  const [placing, setPlacing] = useState(false);
  const [orderId, setOrderId] = useState<number | null>(null);
  const [billRequestId, setBillRequestId] = useState<number | null>(null);
  const [billStatus, setBillStatus] = useState<string | null>(null);
  const [billError, setBillError] = useState<string | null>(null);
  const [billOptions, setBillOptions] = useState<BillOptionsResponse | null>(null);
  const [billMode, setBillMode] = useState<'MY' | 'SELECTED' | 'WHOLE_TABLE'>('MY');
  const [selectedItemIds, setSelectedItemIds] = useState<number[]>([]);
  const [billPayMethod, setBillPayMethod] = useState<'CASH' | 'TERMINAL'>('CASH');
  const [billTipsPercent, setBillTipsPercent] = useState<number | ''>('');
  const [billLoading, setBillLoading] = useState(false);
  const [party, setParty] = useState<{ partyId: number; pin: string; expiresAt: string } | null>(null);
  const [modifiersByItem, setModifiersByItem] = useState<Record<number, MenuItemModifiersResponse>>({});
  const [modOpenByItem, setModOpenByItem] = useState<Record<number, boolean>>({});
  const [phone, setPhone] = useState("");
  const [otpChallengeId, setOtpChallengeId] = useState<number | null>(null);
  const [otpCode, setOtpCode] = useState("");
  const [otpSending, setOtpSending] = useState(false);
  const [otpVerifying, setOtpVerifying] = useState(false);
  const [billRefreshLoading, setBillRefreshLoading] = useState(false);

  useEffect(() => {
    let cancelled = false;
    async function load() {
      setLoading(true);
      setError(null);
      try {
        const ssRes = await fetch(`${API_BASE}/api/public/session/start`, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ tablePublicId, sig, locale: lang }),
        });
        if (!ssRes.ok) throw new Error(`${t(lang, "sessionStartFailed")} (${ssRes.status})`);
        const ss: StartSessionResponse = await ssRes.json();
        if (cancelled) return;
        setSession(ss);

        const mRes = await fetch(`${API_BASE}/api/public/menu?tablePublicId=${encodeURIComponent(tablePublicId)}&sig=${encodeURIComponent(sig)}&locale=${lang}`);
        if (!mRes.ok) throw new Error(`${t(lang, "menuLoadFailed")} (${mRes.status})`);
        const m: MenuResponse = await mRes.json();
        if (cancelled) return;
        setMenu(m);

        const boRes = await fetch(`${API_BASE}/api/public/bill-options?guestSessionId=${ss.guestSessionId}`);
        if (boRes.ok) {
          const bo: BillOptionsResponse = await boRes.json();
          if (!cancelled) setBillOptions(bo);
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
  }, [tablePublicId, lang, sig]);

  const cartTotalCents = useMemo(() => cart.reduce((sum, l) => sum + l.item.priceCents * l.qty, 0), [cart]);

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
      setBillPayMethod(billOptions.payTerminalEnabled === false ? 'CASH' : 'TERMINAL');
    }
    if (billPayMethod === 'TERMINAL' && billOptions.payTerminalEnabled === false) {
      setBillPayMethod(billOptions.payCashEnabled === false ? 'TERMINAL' : 'CASH');
    }
  }, [billOptions, billPayMethod]);

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
    const res = await fetch(`${API_BASE}/api/public/menu-item/${itemId}/modifiers?tablePublicId=${encodeURIComponent(tablePublicId)}&sig=${encodeURIComponent(sig)}&locale=${lang}`);
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

  function missingRequiredGroups(itemId: number, selectedIds: number[]) {
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
  }

  const cartHasMissingModifiers = useMemo(() => {
    return cart.some((l) => missingRequiredGroups(l.item.id, l.modifierOptionIds ?? []).length > 0);
  }, [cart, modifiersByItem]);

  function sessionHeaders() {
    return session?.sessionSecret ? { "X-Session-Secret": session.sessionSecret } : {};
  }

  async function refreshBillStatus() {
    if (!session || !billRequestId) return;
    setBillRefreshLoading(true);
    try {
      const res = await fetch(`${API_BASE}/api/public/bill-request/${billRequestId}?guestSessionId=${session.guestSessionId}`, {
        headers: { ...sessionHeaders() },
      });
      if (!res.ok) throw new Error(`Bill status failed (${res.status})`);
      const body = await res.json();
      setBillStatus(body.status);
    } catch (e: any) {
      setBillError(e?.message ?? t(lang, "billRequestFailed"));
    } finally {
      setBillRefreshLoading(false);
    }
  }

  useEffect(() => {
    if (!billRequestId || !session) return;
    const id = setInterval(() => {
      refreshBillStatus();
    }, 10000);
    return () => clearInterval(id);
  }, [billRequestId, session]);

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
        throw new Error(`${t(lang, "orderFailed")} (${res.status})`);
      }
      const body = await res.json();
      setOrderId(body.orderId);
      setCart([]);
      // refresh bill options after ordering
      const boRes = await fetch(`${API_BASE}/api/public/bill-options?guestSessionId=${session.guestSessionId}`);
      if (boRes.ok) setBillOptions(await boRes.json());
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
      if (!res.ok) throw new Error(`${t(lang, "otpSendFailed")} (${res.status})`);
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
      if (!res.ok) throw new Error(`${t(lang, "otpVerifyFailed")} (${res.status})`);
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
      await fetch(`${API_BASE}/api/public/waiter-call`, {
        method: "POST",
        headers: { "Content-Type": "application/json", ...sessionHeaders() },
        body: JSON.stringify({ guestSessionId: session.guestSessionId }),
      });
      alert(t(lang, "waiterCalled"));
    } catch {
      alert(t(lang, "errorGeneric"));
    }
  }
  async function createPin() {
    if (!session) return;
    try {
      const res = await fetch(`${API_BASE}/api/public/party/create`, {
        method: "POST",
        headers: { "Content-Type": "application/json", ...sessionHeaders() },
        body: JSON.stringify({ guestSessionId: session.guestSessionId }),
      });
      if (!res.ok) throw new Error(`${t(lang, "pinCreateFailed")} (${res.status})`);
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
      if (!res.ok) throw new Error(`${t(lang, "pinJoinFailed")} (${res.status})`);
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
      if (!res.ok) throw new Error(`${t(lang, "partyCloseFailed")} (${res.status})`);
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
      const payload: any = {
        guestSessionId: session.guestSessionId,
        mode: billMode,
        paymentMethod: billPayMethod,
      };
      if (billMode === 'SELECTED') payload.orderItemIds = selectedItemIds;
      if (billTipsPercent !== "") payload.tipsPercent = billTipsPercent;

      const res = await fetch(`${API_BASE}/api/public/bill-request/create`, {
        method: "POST",
        headers: { "Content-Type": "application/json", ...sessionHeaders() },
        body: JSON.stringify(payload),
      });
      const body = await res.json().catch(() => ({}));
      if (!res.ok) throw new Error(body?.message ?? `${t(lang, "billRequestFailed")} (${res.status})`);
      setBillRequestId(body.billRequestId);
      setBillStatus(body.status);
      alert(t(lang, "billRequested"));
    } catch (e: any) {
      setBillError(e?.message ?? t(lang, "billRequestFailed"));
    } finally {
      setBillLoading(false);
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
      const body = await res.json().catch(() => ({}));
      if (!res.ok) throw new Error(body?.message ?? `Cancel failed (${res.status})`);
      setBillStatus(body.status);
      alert(body.status === "CANCELLED" ? (lang === "en" ? "Bill cancelled" : lang === "ro" ? "Nota anulată" : "Счёт отменён") : t(lang, "billRequested"));
    } catch (e: any) {
      setBillError(e?.message ?? t(lang, "billRequestFailed"));
    } finally {
      setBillLoading(false);
    }
  }

  async function toggleModifiers(itemId: number) {
    const isOpen = !!modOpenByItem[itemId];
    if (!isOpen && !modifiersByItem[itemId]) {
      const res = await fetch(`${API_BASE}/api/public/menu-item/${itemId}/modifiers?tablePublicId=${encodeURIComponent(tablePublicId)}&sig=${encodeURIComponent(sig)}&locale=${lang}`);
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
          <a href={`/t/${tablePublicId}?lang=ru&sig=${encodeURIComponent(sig)}`}>RU</a>
          <a href={`/t/${tablePublicId}?lang=ro&sig=${encodeURIComponent(sig)}`}>RO</a>
          <a href={`/t/${tablePublicId}?lang=en&sig=${encodeURIComponent(sig)}`}>EN</a>
        </div>
      </div>

      <div style={{ marginTop: 12, display: "flex", gap: 12, flexWrap: "wrap" }}>
        <button onClick={callWaiter} style={{ padding: "10px 14px" }}>
          {t(lang, "callWaiter")}
        </button>

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
                  <img
                    src={it.photos[0]}
                    alt={it.name}
                    style={{ width: "100%", height: 140, objectFit: "cover", borderRadius: 8, marginBottom: 8 }}
                  />
                )}
                <div style={{ display: "flex", justifyContent: "space-between", gap: 8 }}>
                  <strong>{it.name}</strong>
                  <span>{money(it.priceCents, it.currency)}</span>
                </div>
                {it.weight && <div style={{ color: "#666", fontSize: 12 }}>{it.weight}</div>}
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
                <a href={`/t/${tablePublicId}/item/${it.id}?lang=${lang}&sig=${encodeURIComponent(sig)}`} style={{ display: "inline-block", marginTop: 6 }}>
                  {t(lang, "details")}
                </a>
                {modOpenByItem[it.id] && modifiersByItem[it.id]?.groups?.length ? (
                  <div style={{ marginTop: 8, borderTop: "1px dashed #ddd", paddingTop: 8 }}>
                    {modifiersByItem[it.id].groups.map((g) => (
                      <div key={g.id} style={{ marginBottom: 8 }}>
                        <div style={{ fontWeight: 600 }}>
                          {g.name} {g.isRequired ? `(${t(lang, "required")})` : ""}
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
                                  borderRadius: 6,
                                  border: selected ? "2px solid #333" : "1px solid #ddd",
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
            <strong>{money(cartTotalCents, "MDL")}</strong>
          </div>
          {cartHasMissingModifiers && (
            <div style={{ color: "#b11e46", marginTop: 8 }}>{t(lang, "modifiersRequired")}</div>
          )}
          <button disabled={placing || cartHasMissingModifiers} onClick={placeOrder} style={{ marginTop: 10, padding: "10px 14px", width: "100%" }}>
            {placing ? t(lang, "sending") : t(lang, "placeOrder")}
          </button>
        </div>
      )}

      <h2 style={{ marginTop: 24 }}>{t(lang, "payment")}</h2>
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
                      <div style={{ color: "#666", fontSize: 12 }}>{it.qty} × {money(it.unitPriceCents, "MDL")}</div>
                      {billOptions.allowPayOtherGuestsItems && it.guestSessionId !== session?.guestSessionId && (
                        <div style={{ color: "#999", fontSize: 12 }}>{t(lang, "otherGuest")}</div>
                      )}
                    </div>
                    <div><strong>{money(it.lineTotalCents, "MDL")}</strong></div>
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
          </div>

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

          {billError && <div style={{ color: "#b11e46", marginTop: 10 }}>{billError}</div>}
          {billRequestId && (
            <div style={{ marginTop: 12, paddingTop: 8, borderTop: "1px dashed #eee" }}>
              <div style={{ display: "flex", gap: 8, flexWrap: "wrap", alignItems: "center" }}>
                <strong>{t(lang, "status")}:</strong> <span>{billStatus ?? "?"}</span>
                <button onClick={refreshBillStatus} disabled={billRefreshLoading} style={{ padding: "6px 10px" }}>
                  {billRefreshLoading ? t(lang, "loading") : (lang === "en" ? "Refresh" : lang === "ro" ? "Reîmprospătează" : "Обновить")}
                </button>
                {billStatus === "CREATED" && (
                  <button onClick={cancelBillRequest} disabled={billLoading} style={{ padding: "6px 10px" }}>
                    {lang === "en" ? "Cancel bill" : lang === "ro" ? "Anulează nota" : "Отменить счёт"}
                  </button>
                )}
              </div>
            </div>
          )}
        </div>
      )}
    </main>
  );
}
