"use client";

import { useEffect, useState } from "react";
import { usePathname } from "next/navigation";

type BeforeInstallPromptEvent = Event & {
  prompt: () => Promise<void>;
  userChoice: Promise<{ outcome: "accepted" | "dismissed"; platform: string }>;
};

export function PwaClient() {
  const [promptEvent, setPromptEvent] = useState<BeforeInstallPromptEvent | null>(null);
  const [installed, setInstalled] = useState(false);
  const pathname = usePathname();
  const [langParam, setLangParam] = useState("");
  const [label, setLabel] = useState("Install app");

  useEffect(() => {
    if (typeof window !== "undefined") {
      const params = new URLSearchParams(window.location.search);
      setLangParam(params.get("lang") || "");
    }
    if (typeof navigator !== "undefined" && "serviceWorker" in navigator) {
      navigator.serviceWorker.register("/sw.js").catch(() => undefined);
    }
    const onPrompt = (e: Event) => {
      e.preventDefault();
      setPromptEvent(e as BeforeInstallPromptEvent);
    };
    const onInstalled = () => {
      setInstalled(true);
      setPromptEvent(null);
    };
    window.addEventListener("beforeinstallprompt", onPrompt);
    window.addEventListener("appinstalled", onInstalled);
    return () => {
      window.removeEventListener("beforeinstallprompt", onPrompt);
      window.removeEventListener("appinstalled", onInstalled);
    };
  }, []);

  useEffect(() => {
    const lang = (langParam || (typeof navigator !== "undefined" ? navigator.language : "en") || "en").toLowerCase();
    if (lang.startsWith("ru")) {
      setLabel("Установить приложение");
      return;
    }
    if (lang.startsWith("ro")) {
      setLabel("Instalează aplicația");
      return;
    }
    setLabel("Install app");
  }, [langParam]);

  if (installed || !promptEvent) return null;
  if (!pathname || (!pathname.startsWith("/t/") && pathname !== "/")) return null;

  return (
    <button
      type="button"
      onClick={async () => {
        await promptEvent.prompt();
        await promptEvent.userChoice;
        setPromptEvent(null);
      }}
      style={{
        position: "fixed",
        bottom: 16,
        right: 16,
        padding: "10px 14px",
        borderRadius: 10,
        border: "1px solid #e2e8f0",
        background: "#0f172a",
        color: "#fff",
        fontWeight: 600,
        zIndex: 50,
      }}
    >
      {label}
    </button>
  );
}
