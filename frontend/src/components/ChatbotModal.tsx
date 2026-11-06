// src/components/ChatbotModal.tsx
import { useEffect } from "react";
import { createPortal } from "react-dom";

type Props = {
  open: boolean;
  onClose: () => void;
};

export const ChatbotModal = ({ open, onClose }: Props) => {
  // Close on ESC
  useEffect(() => {
    const handler = (e: KeyboardEvent) => {
      if (e.key === "Escape") onClose();
    };
    document.addEventListener("keydown", handler);
    return () => document.removeEventListener("keydown", handler);
  }, [onClose]);

  if (!open) return null;

  // The modal is rendered into a div#modal-root that you add to index.html
  return createPortal(
    <div className="fixed inset-0 bg-black/30 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg overflow-hidden shadow-xl w-[90%] max-w-2xl h-[80vh]">
        {/* Header with close button */}
        <div className="flex justify-between items-center p-2 border-b">
          <h2 className="text-lg font-medium">🤖 FarmVerse Assistant</h2>
          <button
            className="text-gray-600 hover:text-gray-800"
            onClick={onClose}
            aria-label="Close"
          >
            ✕
          </button>
        </div>

        {/* Iframe that loads the full chatbot UI */}
        <iframe
          src={import.meta.env.VITE_CHATBOT_STATIC?.replace(/\/static$/, "/")} // → http://localhost:8081/
          className="w-full h-full border-0"
          title="FarmVerse Chatbot"
        />
      </div>
    </div>,
    document.getElementById("modal-root")!
  );
};