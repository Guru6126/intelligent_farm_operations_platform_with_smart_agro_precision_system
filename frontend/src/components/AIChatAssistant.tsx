
import { useState, type FormEvent } from "react";
import {
  Bot,
  Send,
  Sprout,
  Droplets,
  Bug,
  Wheat,
  FlaskConical,
  Leaf,
  User,
  LoaderCircle,
  Globe2,
  Mic,
  MicOff,
} from "lucide-react";

type Message = {
  role: "user" | "assistant";
  content: string;
};

type SpeechRecognitionResult = {
  isFinal: boolean;
  [index: number]: { transcript: string };
};

type SpeechRecognitionEvent = {
  resultIndex: number;
  results: ArrayLike<SpeechRecognitionResult>;
};

type SpeechRecognitionInstance = {
  lang: string;
  continuous: boolean;
  interimResults: boolean;
  onresult: ((event: SpeechRecognitionEvent) => void) | null;
  onerror: (() => void) | null;
  onend: (() => void) | null;
  start: () => void;
};

type SpeechRecognitionConstructor = new () => SpeechRecognitionInstance;

declare global {
  interface Window {
    SpeechRecognition?: SpeechRecognitionConstructor;
    webkitSpeechRecognition?: SpeechRecognitionConstructor;
  }
}

const topics = [
  {
    title: "Crop Guidance",
    subtitle: "Growing and crop care",
    icon: Sprout,
    color: "bg-green-100 text-green-700",
    question: "How can I improve crop growth?",
  },
  {
    title: "Soil Health",
    subtitle: "Soil testing and fertility",
    icon: FlaskConical,
    color: "bg-amber-100 text-amber-700",
    question: "How can I improve soil health?",
  },
  {
    title: "Water Management",
    subtitle: "Smart irrigation tips",
    icon: Droplets,
    color: "bg-blue-100 text-blue-700",
    question: "What is the best irrigation method for crops?",
  },
  {
    title: "Pest Control",
    subtitle: "Pest and disease guidance",
    icon: Bug,
    color: "bg-red-100 text-red-700",
    question: "How can I control common crop pests?",
  },
  {
    title: "Fertilizer Guide",
    subtitle: "Nutrients and fertilizer use",
    icon: FlaskConical,
    color: "bg-purple-100 text-purple-700",
    question: "Which fertilizer is suitable for my crops?",
  },
  {
    title: "Seed Selection",
    subtitle: "Seed varieties and selection",
    icon: Wheat,
    color: "bg-orange-100 text-orange-700",
    question: "How do I select good quality seeds?",
  },
];

export default function AIChatAssistant() {
  const [messages, setMessages] = useState<Message[]>([
    {
      role: "assistant",
      content:
        "Hello! Welcome to FarmVerse AI Assistant. Ask me anything about crops, soil, water, pests, fertilizers or seeds. How can I help you today?",
    },
  ]);

  const [question, setQuestion] = useState("");
  const [language, setLanguage] = useState("en");
  const [loading, setLoading] = useState(false);
  const [listening, setListening] = useState(false);
  const [micError, setMicError] = useState("");

  const askQuestion = async (text: string) => {
    const trimmed = text.trim();

    if (!trimmed || loading) return;

    setMessages((previous) => [
      ...previous,
      { role: "user", content: trimmed },
    ]);

    setQuestion("");
    setLoading(true);

    try {
      const response = await fetch(
        "http://localhost:8082/api/chatbot/ask",
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify({
            question: trimmed,
            language,
          }),
        }
      );

      if (!response.ok) {
        throw new Error(`Server error: ${response.status}`);
      }

      const data = await response.json();

      setMessages((previous) => [
        ...previous,
        {
          role: "assistant",
          content: data.answer || "Sorry, I could not find an answer.",
        },
      ]);
    } catch (error) {
      console.error("Chatbot API error:", error);

      setMessages((previous) => [
        ...previous,
        {
          role: "assistant",
          content:
            "Unable to connect to the FarmVerse server. Please check whether the chatbot backend is running on port 8082.",
        },
      ]);
    } finally {
      setLoading(false);
    }
  };

  const startVoiceInput = () => {
    setMicError("");

    const SpeechRecognition =
      window.SpeechRecognition || window.webkitSpeechRecognition;

    if (!SpeechRecognition) {
      setMicError(
        "Voice input is not supported. Please use Google Chrome."
      );
      return;
    }

    if (listening) return;

    const recognition = new SpeechRecognition();

    recognition.lang =
      language === "te"
        ? "te-IN"
        : language === "hi"
          ? "hi-IN"
          : "en-IN";

    recognition.continuous = false;
    recognition.interimResults = true;

    setListening(true);

    recognition.onresult = (event) => {
      let transcript = "";

      for (let i = event.resultIndex; i < event.results.length; i++) {
        transcript += event.results[i][0].transcript;
      }

      setQuestion(transcript);
    };

    recognition.onerror = () => {
      setListening(false);
      setMicError(
        "Microphone access failed. Please allow microphone permission and try again."
      );
    };

    recognition.onend = () => {
      setListening(false);
    };

    try {
      recognition.start();
    } catch {
      setListening(false);
      setMicError("Could not start microphone. Please try again.");
    }
  };

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    askQuestion(question);
  };

  return (
    <div className="min-h-full space-y-4 bg-background p-3 md:p-5">

      {/* Page Heading */}
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <div className="flex items-center gap-2">
            <div className="rounded-lg bg-green-100 p-2 text-green-700">
              <Bot size={22} />
            </div>

            <h1 className="text-xl font-bold text-foreground">
              AI Assistant
            </h1>
          </div>

          <p className="mt-1 text-xs text-muted-foreground">
            Your intelligent farming companion powered by FarmVerse Nexus
          </p>
        </div>

        <div className="flex items-center gap-2 rounded-full border bg-card px-3 py-1.5 text-xs">
          <span className="h-2 w-2 rounded-full bg-green-500" />
          FarmVerse Assistant
        </div>
      </div>

      {/* Small Topic Cards */}
      <div className="grid grid-cols-2 gap-1.5 lg:grid-cols-3">
        {topics.map((topic) => {
          const Icon = topic.icon;

          return (
            <button
              key={topic.title}
              type="button"
              onClick={() => askQuestion(topic.question)}
              disabled={loading}
              className="rounded-lg border bg-card p-2 text-left transition-all hover:border-green-400 hover:shadow-sm disabled:opacity-50"
            >
              <div
                className={`mb-1 flex h-6 w-6 items-center justify-center rounded-md ${topic.color}`}
              >
                <Icon size={14} />
              </div>

              <h3 className="text-xs font-semibold text-foreground">
                {topic.title}
              </h3>

              <p className="mt-0.5 text-[10px] text-muted-foreground">
                {topic.subtitle}
              </p>
            </button>
          );
        })}
      </div>

      {/* Chat Section */}
      <div className="flex min-h-[420px] flex-col overflow-hidden rounded-xl border bg-card shadow-sm">

        {/* Chat Header */}
        <div className="flex items-center justify-between border-b p-3">
          <div className="flex items-center gap-2">
            <div className="rounded-full bg-green-100 p-2 text-green-700">
              <Bot size={19} />
            </div>

            <div>
              <h2 className="text-sm font-semibold text-foreground">
                FarmVerse AI Chat
              </h2>

              <p className="text-[11px] text-green-600">
                Ask your farming questions
              </p>
            </div>
          </div>

          <div className="flex items-center gap-1.5">
            <Globe2 size={15} className="text-muted-foreground" />

            <select
              value={language}
              onChange={(event) => {
                setLanguage(event.target.value);
                setMicError("");
              }}
              className="rounded-lg border bg-background px-2 py-1.5 text-xs outline-none"
              aria-label="Select language"
            >
              <option value="en">English</option>
              <option value="te">తెలుగు</option>
              <option value="hi">हिन्दी</option>
            </select>
          </div>
        </div>

        {/* Messages */}
        <div className="flex-1 space-y-3 overflow-y-auto bg-muted/20 p-3">
          {messages.map((message, index) => (
            <div
              key={index}
              className={`flex items-start gap-2 ${
                message.role === "user"
                  ? "justify-end"
                  : "justify-start"
              }`}
            >
              {message.role === "assistant" && (
                <div className="rounded-full bg-green-100 p-1.5 text-green-700">
                  <Bot size={15} />
                </div>
              )}

              <div
                className={`max-w-[85%] whitespace-pre-wrap rounded-xl px-3 py-2 text-xs leading-5 ${
                  message.role === "user"
                    ? "rounded-br-sm bg-green-700 text-white"
                    : "rounded-bl-sm border bg-card text-foreground"
                }`}
              >
                {message.content}
              </div>

              {message.role === "user" && (
                <div className="rounded-full bg-muted p-1.5 text-muted-foreground">
                  <User size={15} />
                </div>
              )}
            </div>
          ))}

          {loading && (
            <div className="flex items-center gap-2 text-xs text-muted-foreground">
              <LoaderCircle size={16} className="animate-spin" />
              FarmVerse AI is preparing your answer...
            </div>
          )}
        </div>

        {/* Input, Mic and Send */}
        <form
          onSubmit={handleSubmit}
          className="border-t p-2.5"
        >
          <div className="flex items-center gap-2">
            <input
              value={question}
              onChange={(event) => setQuestion(event.target.value)}
              placeholder="Ask anything about farming..."
              maxLength={500}
              className="min-w-0 flex-1 rounded-lg border bg-background px-3 py-2.5 text-xs outline-none focus:border-green-500"
            />

            <button
              type="button"
              onClick={startVoiceInput}
              disabled={loading || listening}
              title="Speak your question"
              aria-label="Speak your question"
              className={`flex items-center justify-center rounded-lg p-2.5 transition ${
                listening
                  ? "bg-red-100 text-red-700"
                  : "border bg-background text-green-700 hover:bg-green-50"
              } disabled:cursor-not-allowed disabled:opacity-50`}
            >
              {listening ? (
                <MicOff size={18} />
              ) : (
                <Mic size={18} />
              )}
            </button>

            <button
              type="submit"
              disabled={!question.trim() || loading}
              className="flex items-center gap-1.5 rounded-lg bg-green-700 px-3 py-2.5 text-xs font-medium text-white transition hover:bg-green-800 disabled:cursor-not-allowed disabled:opacity-50"
            >
              <Send size={15} />
              <span>Send</span>
            </button>
          </div>

          {listening && (
            <p className="mt-1.5 text-[11px] text-red-600">
              Listening... Speak now.
            </p>
          )}

          {micError && (
            <p className="mt-1.5 text-[11px] text-red-600">
              {micError}
            </p>
          )}
        </form>
      </div>

      {/* Footer */}
      <div className="flex items-center justify-center gap-2 text-[11px] text-muted-foreground">
        <Leaf size={13} className="text-green-600" />
        Smart Farming • Better Tomorrow
      </div>
    </div>
  );
}
