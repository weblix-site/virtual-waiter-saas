export default function Home() {
  return (
    <main style={{ padding: 20, fontFamily: "system-ui, -apple-system, Segoe UI, Roboto" }}>
      <h1 style={{ marginTop: 0 }}>Virtual Waiter (MVP)</h1>
      <p>
        Demo table: <a href="/t/TBL_DEMO_0001">/t/TBL_DEMO_0001</a>
      </p>
      <p style={{ color: "#666" }}>
        Tip: append <code>?lang=ru</code>, <code>?lang=ro</code>, or <code>?lang=en</code>.
      </p>
    </main>
  );
}
