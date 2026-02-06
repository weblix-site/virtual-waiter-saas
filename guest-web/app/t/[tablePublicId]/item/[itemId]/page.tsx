"use client";

import { useEffect, useState } from "react";
import Image from "next/image";
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
  videoUrl?: string | null;
  tags: string[];
  priceCents: number;
  currency: string;
  isActive: boolean;
  isStopList: boolean;
  isLowStock: boolean;
};

type RecommendationResponse = { item: ItemDetail; type: string };

function money(priceCents: number, currency: string) {
  return `${(priceCents / 100).toFixed(2)} ${currency}`;
}

export default function ItemPage({ params, searchParams }: any) {
  const tablePublicId: string = params.tablePublicId;
  const itemId: string = params.itemId;
  const lang: Lang = (searchParams?.lang ?? "ru").toLowerCase();
  const sig: string = (searchParams?.sig ?? "");
  const ts: string = (searchParams?.ts ?? "");

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [item, setItem] = useState<ItemDetail | null>(null);
  const [activePhoto, setActivePhoto] = useState(0);
  const [showVideo, setShowVideo] = useState(false);
  const [recommendations, setRecommendations] = useState<RecommendationResponse[]>([]);

  useEffect(() => {
    let cancelled = false;
    async function load() {
      setLoading(true);
      setError(null);
      try {
        const res = await fetch(`${API_BASE}/api/public/menu-item/${itemId}?tablePublicId=${encodeURIComponent(tablePublicId)}&sig=${encodeURIComponent(sig)}&ts=${encodeURIComponent(ts)}&locale=${lang}`);
        if (!res.ok) throw new Error(`${t(lang, "loadFailed")} (${res.status})`);
        const data = await res.json();
        if (!cancelled) {
          setItem(data);
          setActivePhoto(0);
          setShowVideo(false);
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
  }, [tablePublicId, itemId, sig, lang, ts]);

  useEffect(() => {
    let cancelled = false;
    async function loadRecs() {
      try {
        const res = await fetch(`${API_BASE}/api/public/menu-item/${itemId}/recommendations?tablePublicId=${encodeURIComponent(tablePublicId)}&sig=${encodeURIComponent(sig)}&ts=${encodeURIComponent(ts)}&locale=${lang}`);
        if (!res.ok) return;
        const data: RecommendationResponse[] = await res.json();
        if (!cancelled) setRecommendations(Array.isArray(data) ? data : []);
      } catch {
        // ignore
      }
    }
    loadRecs();
    return () => {
      cancelled = true;
    };
  }, [tablePublicId, itemId, sig, lang, ts]);

  if (loading) return <main style={{ padding: 20 }}>{t(lang, "loading")}</main>;
  if (error) return <main style={{ padding: 20 }}><h2>{t(lang, "error")}</h2><pre>{error}</pre></main>;
  if (!item) return <main style={{ padding: 20 }}>{t(lang, "error")}</main>;

  return (
    <main style={{ padding: 16, maxWidth: 900, margin: "0 auto", fontFamily: "system-ui, -apple-system, Segoe UI, Roboto" }}>
      <a href={`/t/${tablePublicId}?lang=${lang}&sig=${encodeURIComponent(sig)}&ts=${encodeURIComponent(ts)}`}>{t(lang, "back")}</a>
      <h1 style={{ marginTop: 8 }}>{item.name}</h1>
      <div style={{ color: "#666", marginBottom: 8 }}>{money(item.priceCents, item.currency)}</div>
      {item.isStopList && (
        <div style={{ marginBottom: 8 }}>
          <span style={{ fontSize: 12, padding: "2px 8px", borderRadius: 999, background: "#fee2e2", color: "#991b1b" }}>
            {t(lang, "outOfStock")}
          </span>
        </div>
      )}
      {!item.isStopList && item.isLowStock && (
        <div style={{ marginBottom: 8 }}>
          <span style={{ fontSize: 12, padding: "2px 8px", borderRadius: 999, background: "#fff7ed", color: "#9a3412" }}>
            {t(lang, "lowStock")}
          </span>
        </div>
      )}

      {item.photos && item.photos.length > 0 ? (
        <div style={{ marginBottom: 12 }}>
          {item.videoUrl && showVideo ? (
            <video controls style={{ width: "100%", borderRadius: 12, marginBottom: 8 }} src={item.videoUrl} />
          ) : (
            <Image
              src={item.photos[Math.min(activePhoto, item.photos.length - 1)]}
              alt={item.name}
              width={900}
              height={540}
              style={{ width: "100%", maxHeight: 360, objectFit: "cover", borderRadius: 12, marginBottom: 8 }}
              unoptimized
            />
          )}
          <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
            {item.photos.map((p, i) => (
              <button
                key={i}
                onClick={() => {
                  setActivePhoto(i);
                  setShowVideo(false);
                }}
                style={{ border: !showVideo && activePhoto === i ? "2px solid #333" : "1px solid #ddd", padding: 0, borderRadius: 8 }}
              >
                <Image
                  src={p}
                  alt={item.name}
                  width={96}
                  height={72}
                  style={{ width: 96, height: 72, objectFit: "cover", borderRadius: 6 }}
                  unoptimized
                />
              </button>
            ))}
            {item.videoUrl && (
              <button
                onClick={() => setShowVideo(true)}
                style={{ border: showVideo ? "2px solid #333" : "1px solid #ddd", padding: "0 10px", borderRadius: 8, fontSize: 12 }}
              >
                {t(lang, "video")}
              </button>
            )}
          </div>
        </div>
      ) : item.videoUrl ? (
        <div style={{ marginBottom: 12 }}>
          <video controls style={{ width: "100%", borderRadius: 12 }} src={item.videoUrl} />
        </div>
      ) : (
        <div style={{ color: "#666", marginBottom: 12 }}>{t(lang, "noPhotos")}</div>
      )}

      {(item.weight || item.kcal || item.proteinG || item.fatG || item.carbsG) && (
        <div style={{ display: "flex", gap: 8, flexWrap: "wrap", fontSize: 13, color: "#444", marginBottom: 10 }}>
          {item.weight && (
            <span style={{ padding: "4px 8px", border: "1px solid #eee", borderRadius: 999 }}>
              {t(lang, "weightShort")}: {item.weight}
            </span>
          )}
          {item.kcal && (
            <span style={{ padding: "4px 8px", border: "1px solid #eee", borderRadius: 999 }}>
              {t(lang, "calories")}: {item.kcal}
            </span>
          )}
          {item.proteinG && (
            <span style={{ padding: "4px 8px", border: "1px solid #eee", borderRadius: 999 }}>
              {t(lang, "protein")}: {item.proteinG}g
            </span>
          )}
          {item.fatG && (
            <span style={{ padding: "4px 8px", border: "1px solid #eee", borderRadius: 999 }}>
              {t(lang, "fat")}: {item.fatG}g
            </span>
          )}
          {item.carbsG && (
            <span style={{ padding: "4px 8px", border: "1px solid #eee", borderRadius: 999 }}>
              {t(lang, "carbs")}: {item.carbsG}g
            </span>
          )}
        </div>
      )}

      {item.description && <p>{item.description}</p>}
      {item.ingredients && <div><strong>{t(lang, "ingredients")}:</strong> {item.ingredients}</div>}
      {item.allergens && <div style={{ color: "#b11e46" }}><strong>{t(lang, "allergens")}:</strong> {item.allergens}</div>}

      {item.tags && item.tags.length > 0 && (
        <div style={{ marginTop: 12, display: "flex", gap: 6, flexWrap: "wrap" }}>
          {item.tags.map((tag, i) => (
            <span key={i} style={{ padding: "4px 8px", border: "1px solid #ddd", borderRadius: 999 }}>{tag}</span>
          ))}
        </div>
      )}

      {recommendations.length > 0 && (
        <section style={{ marginTop: 16 }}>
          <div style={{ fontWeight: 600, marginBottom: 8 }}>{t(lang, "upsellTitle")}</div>
          {(() => {
            const typeOrder = ["WITH", "DRINK", "DESSERT", "SAUCE", "SIDE"];
            const typeLabels: Record<string, string> = {
              WITH: t(lang, "upsellTypeWith"),
              DRINK: t(lang, "upsellTypeDrink"),
              DESSERT: t(lang, "upsellTypeDessert"),
              SAUCE: t(lang, "upsellTypeSauce"),
              SIDE: t(lang, "upsellTypeSide"),
            };
            const grouped = new Map<string, RecommendationResponse[]>();
            for (const rec of recommendations) {
              const key = (rec.type || "WITH").toUpperCase();
              if (!grouped.has(key)) grouped.set(key, []);
              grouped.get(key)!.push(rec);
            }
            const keys = typeOrder.filter((k) => grouped.has(k));
            if (keys.length === 0) keys.push(...grouped.keys());
            return keys.map((key) => (
              <div key={`rec-group-${key}`} style={{ marginBottom: 12 }}>
                <div style={{ fontWeight: 600, fontSize: 13, marginBottom: 6 }}>{typeLabels[key] ?? key}</div>
                <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(200px, 1fr))", gap: 10 }}>
                  {(grouped.get(key) ?? []).map((rec, idx) => (
                    <div key={`${rec.item.id}-${idx}`} style={{ border: "1px solid #eee", borderRadius: 10, padding: 10 }}>
                      {rec.item.photos?.[0] && (
                        <Image
                          src={rec.item.photos[0]}
                          alt={rec.item.name}
                          width={320}
                          height={200}
                          style={{ width: "100%", height: 110, objectFit: "cover", borderRadius: 8, marginBottom: 6 }}
                          unoptimized
                        />
                      )}
                      <div style={{ display: "flex", justifyContent: "space-between", gap: 8 }}>
                        <strong>{rec.item.name}</strong>
                        <span>{money(rec.item.priceCents, rec.item.currency)}</span>
                      </div>
                      {rec.item.isStopList && (
                        <div style={{ marginTop: 6 }}>
                          <span style={{ fontSize: 11, padding: "2px 6px", borderRadius: 999, background: "#fee2e2", color: "#991b1b" }}>
                            {t(lang, "outOfStock")}
                          </span>
                        </div>
                      )}
                      {!rec.item.isStopList && rec.item.isLowStock && (
                        <div style={{ marginTop: 6 }}>
                          <span style={{ fontSize: 11, padding: "2px 6px", borderRadius: 999, background: "#fff7ed", color: "#9a3412" }}>
                            {t(lang, "lowStock")}
                          </span>
                        </div>
                      )}
                      <a
                        href={`/t/${tablePublicId}/item/${rec.item.id}?lang=${lang}&sig=${encodeURIComponent(sig)}&ts=${encodeURIComponent(ts)}`}
                        style={{ display: "inline-block", marginTop: 6, fontSize: 12 }}
                      >
                        {t(lang, "details")}
                      </a>
                    </div>
                  ))}
                </div>
              </div>
            ));
          })()}
        </section>
      )}
    </main>
  );
}
