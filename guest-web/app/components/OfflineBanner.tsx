"use client";

import { useEffect, useMemo, useState } from "react";
import { usePathname } from "next/navigation";

type Lang = "ru" | "ro" | "en";

function resolveLang(langParam: string | null) : Lang {
  const raw = (langParam || "").toLowerCase();
  if (raw === "ru" || raw === "ro" || raw === "en") return raw;
  const nav = (navigator.language || "en").toLowerCase();
  if (nav.startsWith("ru")) return "ru";
  if (nav.startsWith("ro")) return "ro";
  return "en";
}

export function OfflineBanner() {
  const [online, setOnline] = useState(true);
  const pathname = usePathname();
  const [langParam, setLangParam] = useState<string | null>(null);
  const lang = useMemo(() => resolveLang(langParam), [langParam]);

  useEffect(() => {
    if (typeof window !== "undefined") {
      const params = new URLSearchParams(window.location.search);
      setLangParam(params.get("lang"));
    }
    setOnline(typeof navigator !== "undefined" ? navigator.onLine : true);
    const onOnline = () => setOnline(true);
    const onOffline = () => setOnline(false);
    window.addEventListener("online", onOnline);
    window.addEventListener("offline", onOffline);
    return () => {
      window.removeEventListener("online", onOnline);
      window.removeEventListener("offline", onOffline);
    };
  }, []);

  if (!pathname || (!pathname.startsWith("/t/") && pathname !== "/")) return null;
  if (online) return null;

  const text = lang === "ru"
    ? "Вы офлайн. Меню доступно, если было открыто ранее."
    : lang === "ro"
      ? "Ești offline. Meniul e disponibil dacă a fost deschis anterior."
      : "You are offline. Menu is available if it was opened earlier.";

  return (
    <div
      style={{
        position: "fixed",
        top: 0,
        left: 0,
        right: 0,
        padding: "8px 12px",
        background: "#fee2e2",
        color: "#7f1d1d",
        fontWeight: 600,
        fontSize: 12,
        textAlign: "center",
        zIndex: 60,
      }}
    >
      {text}
    </div>
  );
}
