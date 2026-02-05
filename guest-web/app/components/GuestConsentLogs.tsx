"use client";

import React from "react";

type ConsentLog = {
  consentType: string;
  accepted: boolean;
  textVersion: string | null;
  ip: string | null;
  userAgent: string | null;
  createdAt: string | null;
};

type Lang = "ru" | "ro" | "en";

type Props = {
  lang: Lang;
  t: (lang: Lang, key: string) => string;
  phone: string;
  onPhoneChange: (v: string) => void;
  consentType: "" | "PRIVACY" | "MARKETING";
  onConsentTypeChange: (v: "" | "PRIVACY" | "MARKETING") => void;
  accepted: "" | "true" | "false";
  onAcceptedChange: (v: "" | "true" | "false") => void;
  limit: number;
  onLimitChange: (v: number) => void;
  page: number;
  onPageChange: (v: number) => void;
  onLoad: () => void;
  loading: boolean;
  error: string | null;
  logs: ConsentLog[];
  extraFilters?: React.ReactNode;
};

export default function GuestConsentLogs({
  lang,
  t,
  phone,
  onPhoneChange,
  consentType,
  onConsentTypeChange,
  accepted,
  onAcceptedChange,
  limit,
  onLimitChange,
  page,
  onPageChange,
  onLoad,
  loading,
  error,
  logs,
  extraFilters,
}: Props) {
  return (
    <div>
      <div style={{ display: "flex", gap: 8, flexWrap: "wrap", alignItems: "center" }}>
        <label>
          {t(lang, "guestPhone")}
          <input value={phone} onChange={(e) => onPhoneChange(e.target.value)} placeholder="+373..." />
        </label>
        <label>
          {t(lang, "guestConsentType")}
          <select value={consentType} onChange={(e) => onConsentTypeChange(e.target.value as "" | "PRIVACY" | "MARKETING")}>
            <option value="">{t(lang, "all")}</option>
            <option value="PRIVACY">PRIVACY</option>
            <option value="MARKETING">MARKETING</option>
          </select>
        </label>
        <label>
          {t(lang, "guestConsentAccepted")}
          <select value={accepted} onChange={(e) => onAcceptedChange(e.target.value as "" | "true" | "false")}>
            <option value="">{t(lang, "all")}</option>
            <option value="true">{t(lang, "yes")}</option>
            <option value="false">{t(lang, "no")}</option>
          </select>
        </label>
        <label>
          Limit
          <input
            type="number"
            min={1}
            max={500}
            value={limit}
            onChange={(e) => onLimitChange(Number(e.target.value || 0))}
            style={{ width: 90 }}
          />
        </label>
        <div style={{ display: "flex", gap: 6, alignItems: "center" }}>
          <button
            type="button"
            onClick={() => onPageChange(Math.max(0, page - 1))}
            disabled={page <= 0 || loading}
          >
            ←
          </button>
          <span style={{ fontSize: 12 }}>Page {page + 1}</span>
          <button
            type="button"
            onClick={() => onPageChange(page + 1)}
            disabled={loading}
          >
            →
          </button>
        </div>
        {extraFilters}
        <button onClick={onLoad} disabled={loading}>
          {loading ? t(lang, "loading") : t(lang, "load")}
        </button>
      </div>
      {error && <div style={{ color: "#b11e46", marginTop: 8 }}>{error}</div>}
      {logs.length === 0 ? (
        <div style={{ marginTop: 8, fontSize: 12, color: "#666" }}>{t(lang, "guestConsentEmpty")}</div>
      ) : (
        <div style={{ marginTop: 8, display: "grid", gap: 6 }}>
          {logs.map((c, idx) => (
            <div key={`${c.consentType}-${c.createdAt ?? idx}`} style={{ fontSize: 12, display: "flex", gap: 10, flexWrap: "wrap" }}>
              <span>{t(lang, "guestConsentType")}: {c.consentType}</span>
              <span>{t(lang, "guestConsentAccepted")}: {c.accepted ? t(lang, "yes") : t(lang, "no")}</span>
              <span>{t(lang, "guestConsentVersion")}: {c.textVersion ?? "—"}</span>
              <span>{t(lang, "guestConsentIp")}: {c.ip ?? "—"}</span>
              <span>{t(lang, "guestConsentUa")}: {c.userAgent ?? "—"}</span>
              <span>{t(lang, "guestConsentAt")}: {c.createdAt ?? "—"}</span>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
