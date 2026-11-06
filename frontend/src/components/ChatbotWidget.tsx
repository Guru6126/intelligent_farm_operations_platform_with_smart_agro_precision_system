import { lazy, Suspense, useState, useEffect } from 'react';

// Dynamically load the static chatbot bundle served by the Spring Boot app.
// The bundle exports a React component (named ChatbotApp) that renders the full UI.
interface ChatbotModule {
  ChatbotApp?: any;
  default?: any;
}

/* Dynamically import the static bundle served by Spring Boot.
   The cast to ChatbotModule provides a proper type for `mod`. */
const ChatbotApp = lazy(() =>
  // @ts-ignore – Vite cannot statically analyse the runtime URL.
  (import(/* @vite-ignore */ `${import.meta.env.VITE_CHATBOT_STATIC}/app.js`) as Promise<ChatbotModule>)
    .then((mod) => ({ default: mod.ChatbotApp ?? mod.default }))
);

/**
 * Floating green bubble that toggles the chatbot drawer.
 * Appears in the bottom‑right corner of every AgroPrecision page.
 */
export function ChatbotWidget() {
  const [opened, setOpened] = useState(false);

  // Close when clicking outside of the drawer
  useEffect(() => {
    const handler = (e: MouseEvent) => {
      const target = e.target as HTMLElement;
      if (!target.closest('#chatbot-drawer') && !target.closest('#chatbot-toggle')) {
        setOpened(false);
      }
    };
    document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, []);

  return (
    <>
      {/* Toggle button – green bubble */}
      <button
        id="chatbot-toggle"
        className="fixed bottom-4 right-4 rounded-full bg-green-600 w-12 h-12 flex items-center justify-center text-white shadow-lg hover:bg-green-700"
        onClick={() => setOpened((v) => !v)}
        aria-label="Open AI Farm Assistant"
      >
        🤖
      </button>

      {/* Drawer – lazy‑loaded chatbot UI */}
      {opened && (
        <div
          id="chatbot-drawer"
          className="fixed inset-0 flex items-end justify-end bg-black/30"
        >
          <div className="w-full max-w-md h-[80vh] bg-white rounded-t-lg shadow-xl overflow-hidden">
            <Suspense fallback={<div className="p-4">Loading…</div>}>
              <ChatbotApp />
            </Suspense>
          </div>
        </div>
      )}
    </>
  );
}

