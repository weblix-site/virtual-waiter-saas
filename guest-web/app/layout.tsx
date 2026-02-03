import { PwaClient } from "./components/PwaClient";
import { OfflineBanner } from "./components/OfflineBanner";

export const metadata = {
  title: "Virtual Waiter",
  description: "Virtual Waiter Guest Web",
  manifest: "/manifest.webmanifest",
  appleWebApp: {
    capable: true,
    title: "Virtual Waiter",
    statusBarStyle: "default",
  },
  icons: {
    icon: "/icon.svg",
  },
};

export const viewport = {
  themeColor: "#0f172a",
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en">
      <body style={{ margin: 0 }}>
        {children}
        <OfflineBanner />
        <PwaClient />
      </body>
    </html>
  );
}
