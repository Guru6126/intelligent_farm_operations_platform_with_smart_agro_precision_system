import { createFileRoute } from '@tanstack/react-router';

export const Route = createFileRoute('/_authenticated/assistant')({
  component: AssistantPage,
});

function AssistantPage() {
  const chatbotUrl = 'http://localhost:8081/';
  const openChatbot = () => {
    window.open(chatbotUrl, '_blank');
  };
  return (
    <div className="relative flex flex-col h-screen overflow-hidden">
      {/* iframe fills the container */}
      <iframe
        src={chatbotUrl}
        className="flex-1 w-full max-h-[85vh] border-3 border-green-800 rounded-lg"
        title="AI Assistant"
      />  
    </div>
  );
}
