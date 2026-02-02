export const metadata = {
  title: "Virtual Waiter",
  description: "Virtual Waiter Guest Web",
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en">
      <body style={{ margin: 0 }}>{children}</body>
    </html>
  );
}
