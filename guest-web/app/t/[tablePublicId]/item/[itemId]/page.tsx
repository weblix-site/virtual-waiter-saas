"use client";

import { useEffect, useState } from "react";
import { t, type Lang } from "@/app/i18n";

const API_BASE = process.env.NEXT_PUBLIC_API_BASE ?? "http://localhost:8080";

type ItemDetail = {
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

function money(priceCents: number, currency: string) {
  return `${(priceCents / 100).toFixed(2)} ${currency}`;
}

export default function ItemPage({ params, searchParams }: any) {
  const tablePublicId: string = params.tablePublicId;
  const itemId: string = params.itemId;
  const lang: Lang = (searchParams?.lang ?? "ru").toLowerCase();
  const sig: string = (searchParams?.sig ?? "");

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [item, setItem] = useState<ItemDetail | null>(null);
  const [activePhoto, setActivePhoto] = useState(0);

  useEffect(() => {
    let cancelled = false;
    async function load() {
      setLoading(true);
      setError(null);
      try {
        const res = await fetch(`${API_BASE}/api/public/menu-item/${itemId}?tablePublicId=${encodeURIComponent(tablePublicId)}&sig=${encodeURIComponent(sig)}&locale=${lang}`);
        if (!res.ok) throw new Error(`${t(lang, "loadFailed")} (${res.status})`);
        const data = await res.json();
        if (!cancelled) {
          setItem(data);
          setActivePhoto(0);
        }
      } catch (e: any) {
        if (!cancelled) setError(e?.message ?? t(lang, "errorGeneric"));
      } finally {
        if (!cancelled) setLoading(false);
      }
    }
    load();
    return () => {
      cancelled = true;
    };
  }, [tablePublicId, itemId, sig, lang]);

  if (loading) return <main style={{ padding: 20 }}>{t(lang, "loading")}</main>;
  if (error) return <main style={{ padding: 20 }}><h2>{t(lang, "error")}</h2><pre>{error}</pre></main>;
  if (!item) return <main style={{ padding: 20 }}>{t(lang, "error")}</main>;

  return (
    <main style={{ padding: 16, maxWidth: 900, margin: "0 auto", fontFamily: "system-ui, -apple-system, Segoe UI, Roboto" }}>
      <a href={`/t/${tablePublicId}?lang=${lang}&sig=${encodeURIComponent(sig)}`}>{t(lang, "back")}</a>
      <h1 style={{ marginTop: 8 }}>{item.name}</h1>
      <div style={{ color: "#666", marginBottom: 8 }}>{money(item.priceCents, item.currency)}</div>

      {item.photos && item.photos.length > 0 ? (
        <div style={{ marginBottom: 12 }}>
          <img
            src={item.photos[Math.min(activePhoto, item.photos.length - 1)]}
            alt={item.name}
            style={{ width: "100%", maxHeight: 360, objectFit: "cover", borderRadius: 12, marginBottom: 8 }}
          />
          <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
            {item.photos.map((p, i) => (
              <button
                key={i}
                onClick={() => setActivePhoto(i)}
                style={{ border: activePhoto === i ? "2px solid #333" : "1px solid #ddd", padding: 0, borderRadius: 8 }}
              >
                <img src={p} alt={item.name} style={{ width: 96, height: 72, objectFit: "cover", borderRadius: 6 }} />
              </button>
            ))}
          </div>
        </div>
      ) : (
        <div style={{ color: "#666", marginBottom: 12 }}>{t(lang, "noPhotos")}</div>
      )}

      {item.description && <p>{item.description}</p>}
      {item.ingredients && <div><strong>{t(lang, "ingredients")}:</strong> {item.ingredients}</div>}
      {item.allergens && <div style={{ color: "#b11e46" }}><strong>{t(lang, "allergens")}:</strong> {item.allergens}</div>}
      {item.weight && <div><strong>{t(lang, "weight")}:</strong> {item.weight}</div>}

      {(item.kcal || item.proteinG || item.fatG || item.carbsG) && (
        <div style={{ marginTop: 12 }}>
          <strong>{t(lang, "kbju")}:</strong>{" "}
          {item.kcal ? `Kcal ${item.kcal}` : ""}{" "}
          {item.proteinG ? `P ${item.proteinG}g` : ""}{" "}
          {item.fatG ? `F ${item.fatG}g` : ""}{" "}
          {item.carbsG ? `C ${item.carbsG}g` : ""}
        </div>
      )}

      {item.tags && item.tags.length > 0 && (
        <div style={{ marginTop: 12, display: "flex", gap: 6, flexWrap: "wrap" }}>
          {item.tags.map((tag, i) => (
            <span key={i} style={{ padding: "4px 8px", border: "1px solid #ddd", borderRadius: 999 }}>{tag}</span>
          ))}
        </div>
      )}
    </main>
  );
}
