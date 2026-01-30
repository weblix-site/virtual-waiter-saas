"use client";

import { useEffect, useMemo, useState } from "react";

type MenuItem = {
  id: number;
  name: string;
  description?: string | null;
  ingredients?: string | null;
  allergens?: string | null;
  weight?: string | null;
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
  myItems: BillOptionsItem[];
  tableItems: BillOptionsItem[];
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
  const lang: "ru" | "ro" | "en" = (searchParams?.lang ?? "ru").toLowerCase();
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
        if (!ssRes.ok) throw new Error(`Session start failed (${ssRes.status})`);
        const ss: StartSessionResponse = await ssRes.json();
        if (cancelled) return;
        setSession(ss);

        const mRes = await fetch(`${API_BASE}/api/public/menu?tablePublicId=${encodeURIComponent(tablePublicId)}&sig=${encodeURIComponent(sig)}&locale=${lang}`);
        if (!mRes.ok) throw new Error(`Menu load failed (${mRes.status})`);
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
        setError(e?.message ?? "Unexpected error");
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

  function addToCart(item: MenuItem) {
    setOrderId(null);
    setCart((prev) => {
      const idx = prev.findIndex((x) => x.item.id === item.id);
      if (idx >= 0) {
        const next = prev.slice();
        next[idx] = { ...next[idx], qty: next[idx].qty + 1 };
        return next;
      }
      return [...prev, { item, qty: 1, comment: "", modifierOptionIds: [], modifierSummary: "" }];
    });
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

  async function placeOrder() {
    if (!session) return;
    if (cart.length === 0) return;
    setPlacing(true);
    setError(null);
    try {
      const res = await fetch(`${API_BASE}/api/public/orders`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
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
          throw new Error(lang === "ro" ? "Este necesară verificarea prin SMS" : lang === "en" ? "SMS verification required" : "Требуется SMS-верификация");
        }
        throw new Error(`Order failed (${res.status})`);
      }
      const body = await res.json();
      setOrderId(body.orderId);
      setCart([]);
      // refresh bill options after ordering
      const boRes = await fetch(`${API_BASE}/api/public/bill-options?guestSessionId=${session.guestSessionId}`);
      if (boRes.ok) setBillOptions(await boRes.json());
    } catch (e: any) {
      setError(e?.message ?? "Order error");
    } finally {
      setPlacing(false);
    }
  }

  async function sendOtp() {
    if (!session) return;
    if (!phone.trim()) {
      alert(lang === "ro" ? "Introduceți numărul" : lang === "en" ? "Enter phone" : "Введите номер");
      return;
    }
    setOtpSending(true);
    try {
      const res = await fetch(`${API_BASE}/api/public/otp/send`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ guestSessionId: session.guestSessionId, phoneE164: phone.trim(), locale: lang }),
      });
      if (!res.ok) throw new Error(`OTP send failed (${res.status})`);
      const body = await res.json();
      setOtpChallengeId(body.challengeId);
      if (body.devCode) alert((lang === "en" ? "DEV code: " : lang === "ro" ? "Cod DEV: " : "DEV код: ") + body.devCode);
    } catch (e: any) {
      alert(e?.message ?? "Error");
    } finally {
      setOtpSending(false);
    }
  }

  async function verifyOtp() {
    if (!session) return;
    if (!otpChallengeId) {
      alert(lang === "ro" ? "Trimiteți codul mai întâi" : lang === "en" ? "Send code first" : "Сначала отправьте код");
      return;
    }
    if (!otpCode.trim()) {
      alert(lang === "ro" ? "Introduceți codul" : lang === "en" ? "Enter code" : "Введите код");
      return;
    }
    setOtpVerifying(true);
    try {
      const res = await fetch(`${API_BASE}/api/public/otp/verify`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ guestSessionId: session.guestSessionId, challengeId: otpChallengeId, code: otpCode.trim() }),
      });
      if (!res.ok) throw new Error(`OTP verify failed (${res.status})`);
      setSession({ ...session, isVerified: true });
      alert(lang === "ro" ? "Verificat" : lang === "en" ? "Verified" : "Подтверждено");
    } catch (e: any) {
      alert(e?.message ?? "Error");
    } finally {
      setOtpVerifying(false);
    }
  }

  async function callWaiter() {
    if (!session) return;
    try {
      await fetch(`${API_BASE}/api/public/waiter-call`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ guestSessionId: session.guestSessionId }),
      });
      alert(lang === "ro" ? "Chelnerul a fost chemat" : lang === "en" ? "Waiter was called" : "Официант вызван");
    } catch {
      alert("Error");
    }
  }
  async function createPin() {
    if (!session) return;
    try {
      const res = await fetch(`${API_BASE}/api/public/party/create`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ guestSessionId: session.guestSessionId }),
      });
      if (!res.ok) throw new Error(`PIN create failed (${res.status})`);
      const body = await res.json();
      setParty({ partyId: body.partyId, pin: body.pin, expiresAt: body.expiresAt });
      alert((lang === "ro" ? "PIN creat: " : lang === "en" ? "PIN created: " : "PIN создан: ") + body.pin);
    } catch (e: any) {
      alert(e?.message ?? "Error");
    }
  }

  async function joinByPin() {
    if (!session) return;
    const pin = prompt(lang === "ro" ? "Introdu PIN (4 cifre)" : lang === "en" ? "Enter PIN (4 digits)" : "Введите PIN (4 цифры)");
    if (!pin) return;
    try {
      const res = await fetch(`${API_BASE}/api/public/party/join`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ guestSessionId: session.guestSessionId, pin }),
      });
      if (!res.ok) throw new Error(`PIN join failed (${res.status})`);
      const body = await res.json();
      setParty({ partyId: body.partyId, pin: body.pin, expiresAt: body.expiresAt });
      alert((lang === "ro" ? "Conectat la PIN: " : lang === "en" ? "Joined PIN: " : "Подключились к PIN: ") + body.pin);
    } catch (e: any) {
      alert(e?.message ?? "Error");
    }
  }


  async function requestBill() {
    if (!session) return;
    setBillError(null);
    setBillLoading(true);
    try {
      if (billMode === 'SELECTED' && selectedItemIds.length === 0) {
        throw new Error(lang === 'ro' ? 'Selectați pozițiile' : lang === 'en' ? 'Select items' : 'Выберите позиции');
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
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
      });
      const body = await res.json().catch(() => ({}));
      if (!res.ok) throw new Error(body?.message ?? `Bill request failed (${res.status})`);
      setBillRequestId(body.billRequestId);
      setBillStatus(body.status);
      alert(lang === "ro" ? "Cerere de plată trimisă" : lang === "en" ? "Bill requested" : "Счет запрошен");
    } catch (e: any) {
      setBillError(e?.message ?? "Error");
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

  function toggleOption(itemId: number, group: ModGroup, opt: ModOption) {
    setCart((prev) => prev.map((l) => {
      if (l.item.id !== itemId) return l;
      const current = new Set(l.modifierOptionIds ?? []);
      const groupOptionIds = new Set((group.options ?? []).map((o) => o.id));
      const selectedInGroup = Array.from(current).filter((id) => groupOptionIds.has(id));
      const max = group.maxSelect ?? (group.isRequired ? 1 : undefined);
      const min = group.minSelect ?? (group.isRequired ? 1 : 0);

      if (current.has(opt.id)) {
        current.delete(opt.id);
      } else {
        if (max != null && selectedInGroup.length >= max) {
          // if max=1, replace
          if (max === 1) {
            for (const id of selectedInGroup) current.delete(id);
          } else {
            return l;
          }
        }
        current.add(opt.id);
      }

      // basic required guard (UI only)
      if (min > 0 && Array.from(current).filter((id) => groupOptionIds.has(id)).length < min) {
        // allow empty; server will validate
      }

      const mods = modifiersByItem[itemId];
      let summary = "";
      if (mods) {
        const allOptions = mods.groups.flatMap((g) => g.options);
        summary = Array.from(current)
          .map((id) => allOptions.find((o) => o.id === id)?.name)
          .filter(Boolean)
          .join(", ");
      }
      return { ...l, modifierOptionIds: Array.from(current), modifierSummary: summary };
    }));
  }




  if (loading) {
    return <main style={{ padding: 20 }}>Loading...</main>;
  }

  if (error) {
    return (
      <main style={{ padding: 20 }}>
        <h2 style={{ marginTop: 0 }}>Error</h2>
        <pre style={{ whiteSpace: "pre-wrap" }}>{error}</pre>
      </main>
    );
  }

  return (
    <main style={{ padding: 16, maxWidth: 900, margin: "0 auto", fontFamily: "system-ui, -apple-system, Segoe UI, Roboto" }}>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", gap: 12, flexWrap: "wrap" }}>
        <div>
          <h1 style={{ margin: 0 }}>Table #{session?.tableNumber}</h1>
          <div style={{ color: "#666" }}>Session: {session?.guestSessionId}</div>
        </div>
        <div style={{ display: "flex", gap: 8 }}>
          <a href={`/t/${tablePublicId}?lang=ru&sig=${encodeURIComponent(sig)}`}>RU</a>
          <a href={`/t/${tablePublicId}?lang=ro&sig=${encodeURIComponent(sig)}`}>RO</a>
          <a href={`/t/${tablePublicId}?lang=en&sig=${encodeURIComponent(sig)}`}>EN</a>
        </div>
      </div>

      <div style={{ marginTop: 12, display: "flex", gap: 12, flexWrap: "wrap" }}>
        <button onClick={callWaiter} style={{ padding: "10px 14px" }}>
          {lang === "ro" ? "Cheamă chelnerul" : lang === "en" ? "Call waiter" : "Вызвать официанта"}
        </button>

        {billOptions?.enablePartyPin && (
          <>
            <button onClick={createPin} style={{ padding: "10px 14px" }}>
              {lang === "ro" ? "Creează PIN" : lang === "en" ? "Create PIN" : "Создать PIN"}
            </button>
            <button onClick={joinByPin} style={{ padding: "10px 14px" }}>
              {lang === "ro" ? "Conectează-te la PIN" : lang === "en" ? "Join by PIN" : "Объединиться по PIN"}
            </button>
          </>
        )}
        <button onClick={requestBill} disabled={billLoading} style={{ padding: "10px 14px" }}>
          {billLoading
            ? (lang === "ro" ? "Se trimite..." : lang === "en" ? "Sending..." : "Отправка...")
            : (lang === "ro" ? "Cere nota" : lang === "en" ? "Request bill" : "Запросить счёт")}
        </button>
        {party && (
          <div style={{ padding: "10px 14px", border: "1px dashed #ccc", borderRadius: 8 }}>
            <strong>PIN:</strong> {party.pin}
          </div>
        )}

      </div>

      {session?.otpRequired && !session?.isVerified && (
        <div style={{ marginTop: 12, padding: 12, border: "1px solid #f0c", borderRadius: 8 }}>
          <div style={{ fontWeight: 600, marginBottom: 8 }}>
            {lang === "ro" ? "Verificare SMS înainte de prima comandă" : lang === "en" ? "SMS verification before first order" : "SMS-верификация перед первым заказом"}
          </div>
          <div style={{ display: "flex", gap: 8, flexWrap: "wrap", alignItems: "center" }}>
            <input
              value={phone}
              onChange={(e) => setPhone(e.target.value)}
              placeholder={lang === "ro" ? "+3736......" : lang === "en" ? "+3736......" : "+3736......"}
              style={{ padding: "10px 12px", minWidth: 220, border: "1px solid #ddd", borderRadius: 8 }}
            />
            <button disabled={otpSending} onClick={sendOtp} style={{ padding: "10px 14px" }}>
              {otpSending ? (lang === "ro" ? "Se trimite..." : lang === "en" ? "Sending..." : "Отправка...") : (lang === "ro" ? "Trimite cod" : lang === "en" ? "Send code" : "Отправить код")}
            </button>
          </div>
          <div style={{ display: "flex", gap: 8, flexWrap: "wrap", alignItems: "center", marginTop: 10 }}>
            <input
              value={otpCode}
              onChange={(e) => setOtpCode(e.target.value)}
              placeholder={lang === "ro" ? "Cod" : lang === "en" ? "Code" : "Код"}
              style={{ padding: "10px 12px", width: 120, border: "1px solid #ddd", borderRadius: 8 }}
            />
            <button disabled={otpVerifying} onClick={verifyOtp} style={{ padding: "10px 14px" }}>
              {otpVerifying ? (lang === "ro" ? "Se verifică..." : lang === "en" ? "Verifying..." : "Проверка...") : (lang === "ro" ? "Verifică" : lang === "en" ? "Verify" : "Подтвердить")}
            </button>
          </div>
          <div style={{ marginTop: 8, color: "#666", fontSize: 12 }}>
            {lang === "ro" ? "În dev, codul poate apărea într-un popup. În producție va veni prin SMS." : lang === "en" ? "In dev, the code may appear in a popup. In production it will arrive via SMS." : "В dev-режиме код может показываться в popup. В продакшене придёт по SMS."}
          </div>
        </div>
      )}

      {orderId && (
        <div style={{ marginTop: 12, padding: 12, border: "1px solid #ddd", borderRadius: 8 }}>
          {lang === "ro" ? `Comanda #${orderId} a fost trimisă.` : lang === "en" ? `Order #${orderId} sent.` : `Заказ #${orderId} отправлен.`}
        </div>
      )}

      <h2 style={{ marginTop: 20 }}>Menu</h2>
      {menu?.categories.map((cat) => (
        <section key={cat.id} style={{ marginBottom: 18 }}>
          <h3 style={{ margin: "10px 0" }}>{cat.name}</h3>
          <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(260px, 1fr))", gap: 10 }}>
            {cat.items.map((it) => (
              <div key={it.id} style={{ border: "1px solid #eee", borderRadius: 10, padding: 12 }}>
                <div style={{ display: "flex", justifyContent: "space-between", gap: 8 }}>
                  <strong>{it.name}</strong>
                  <span>{money(it.priceCents, it.currency)}</span>
                </div>
                {it.weight && <div style={{ color: "#666", fontSize: 12 }}>{it.weight}</div>}
                {it.description && <p style={{ margin: "8px 0", color: "#444" }}>{it.description}</p>}
                {it.ingredients && <div style={{ fontSize: 12, color: "#666" }}>{it.ingredients}</div>}
                {it.allergens && <div style={{ fontSize: 12, color: "#b11e46" }}>{it.allergens}</div>}
                <button onClick={() => addToCart(it)} style={{ marginTop: 10, padding: "8px 12px" }}>
                  {lang === "ro" ? "Adaugă" : lang === "en" ? "Add" : "Добавить"}
                </button>
                <button onClick={() => toggleModifiers(it.id)} style={{ marginTop: 8, padding: "6px 10px" }}>
                  {lang === "ro" ? "Modificatori" : lang === "en" ? "Modifiers" : "Модификаторы"}
                </button>
                {modOpenByItem[it.id] && modifiersByItem[it.id]?.groups?.length ? (
                  <div style={{ marginTop: 8, borderTop: "1px dashed #ddd", paddingTop: 8 }}>
                    {modifiersByItem[it.id].groups.map((g) => (
                      <div key={g.id} style={{ marginBottom: 8 }}>
                        <div style={{ fontWeight: 600 }}>
                          {g.name} {g.isRequired ? "*" : ""}
                        </div>
                        <div style={{ display: "flex", gap: 6, flexWrap: "wrap" }}>
                          {g.options.map((o) => {
                            const selected = cart.find((c) => c.item.id === it.id)?.modifierOptionIds?.includes(o.id) ?? false;
                            return (
                              <button
                                key={o.id}
                                onClick={() => toggleOption(it.id, g, o)}
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
                      </div>
                    ))}
                  </div>
                ) : null}
              </div>
            ))}
          </div>
        </section>
      ))}

      <h2 style={{ marginTop: 20 }}>{lang === "ro" ? "Coș" : lang === "en" ? "Cart" : "Корзина"}</h2>
      {cart.length === 0 ? (
        <p style={{ color: "#666" }}>{lang === "ro" ? "Coșul e gol" : lang === "en" ? "Cart is empty" : "Корзина пуста"}</p>
      ) : (
        <div style={{ border: "1px solid #eee", borderRadius: 10, padding: 12 }}>
          {cart.map((l) => (
            <div key={l.item.id} style={{ display: "flex", justifyContent: "space-between", alignItems: "center", padding: "6px 0" }}>
              <div>
                <div><strong>{l.item.name}</strong></div>
                <div style={{ color: "#666", fontSize: 12 }}>{money(l.item.priceCents, l.item.currency)}</div>
                {l.modifierSummary && (
                  <div style={{ color: "#666", fontSize: 12 }}>{l.modifierSummary}</div>
                )}
                <div style={{ marginTop: 6, display: "flex", gap: 8, flexWrap: "wrap" }}>
                  <input
                    value={l.comment ?? ""}
                    onChange={(e) => setCart((prev) => prev.map((x) => x.item.id === l.item.id ? { ...x, comment: e.target.value } : x))}
                    placeholder={lang === "ro" ? "Comentariu" : lang === "en" ? "Comment" : "Комментарий"}
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
          ))}
          <hr style={{ border: 0, borderTop: "1px solid #eee", margin: "10px 0" }} />
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
            <strong>Total</strong>
            <strong>{money(cartTotalCents, "MDL")}</strong>
          </div>
          <button disabled={placing} onClick={placeOrder} style={{ marginTop: 10, padding: "10px 14px", width: "100%" }}>
            {placing ? (lang === "ro" ? "Se trimite..." : lang === "en" ? "Sending..." : "Отправляем...") : (lang === "ro" ? "Trimite comanda" : lang === "en" ? "Place order" : "Сделать заказ")}
          </button>
        </div>
      )}

      <h2 style={{ marginTop: 24 }}>{lang === "ro" ? "Plată" : lang === "en" ? "Payment" : "Оплата"}</h2>
      {!billOptions ? (
        <p style={{ color: "#666" }}>{lang === "ro" ? "Se încarcă opțiunile..." : lang === "en" ? "Loading options..." : "Загрузка опций..."}</p>
      ) : (
        <div style={{ border: "1px solid #eee", borderRadius: 10, padding: 12 }}>
          <div style={{ display: "flex", gap: 12, flexWrap: "wrap" }}>
            <label><input type="radio" checked={billMode === 'MY'} onChange={() => setBillMode('MY')} /> {lang === "ro" ? "Doar al meu" : lang === "en" ? "My items" : "Только моё"}</label>
            <label><input type="radio" checked={billMode === 'SELECTED'} onChange={() => setBillMode('SELECTED')} /> {lang === "ro" ? "Ales" : lang === "en" ? "Selected" : "Выбранные"}</label>
            {billOptions.allowPayWholeTable && (
              <label><input type="radio" checked={billMode === 'WHOLE_TABLE'} onChange={() => setBillMode('WHOLE_TABLE')} /> {lang === "ro" ? "Totul la masă" : lang === "en" ? "Whole table" : "Весь стол"}</label>
            )}
          </div>

          <div style={{ marginTop: 12 }}>
            {billItemsForMode.length === 0 ? (
              <p style={{ color: "#666" }}>{lang === "ro" ? "Nu există poziții neplătite" : lang === "en" ? "No unpaid items" : "Нет неоплаченных позиций"}</p>
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
                        <div style={{ color: "#999", fontSize: 12 }}>{lang === "ro" ? "Alt oaspete" : lang === "en" ? "Other guest" : "Другой гость"}</div>
                      )}
                    </div>
                    <div><strong>{money(it.lineTotalCents, "MDL")}</strong></div>
                  </div>
                ))}
              </div>
            )}
          </div>

          <div style={{ marginTop: 12, display: "flex", gap: 12, flexWrap: "wrap" }}>
            <label><input type="radio" checked={billPayMethod === 'CASH'} onChange={() => setBillPayMethod('CASH')} /> {lang === "ro" ? "Numerar" : lang === "en" ? "Cash" : "Наличные"}</label>
            <label><input type="radio" checked={billPayMethod === 'TERMINAL'} onChange={() => setBillPayMethod('TERMINAL')} /> {lang === "ro" ? "Terminal" : lang === "en" ? "Terminal" : "Терминал"}</label>
          </div>

          {billOptions.tipsEnabled && (
            <div style={{ marginTop: 12 }}>
              <div style={{ marginBottom: 6 }}>{lang === "ro" ? "Bacșiș" : lang === "en" ? "Tips" : "Чаевые"}</div>
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
                  {lang === "ro" ? "Fără" : lang === "en" ? "None" : "Без"}
                </button>
              </div>
            </div>
          )}

          {billError && <div style={{ color: "#b11e46", marginTop: 10 }}>{billError}</div>}
        </div>
      )}
    </main>
  );
}
