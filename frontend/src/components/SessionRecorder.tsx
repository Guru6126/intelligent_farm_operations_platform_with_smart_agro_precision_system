import { useState, useRef, useEffect } from "react";
import { record, type eventWithTime } from "rrweb";
import rrwebPlayer from "rrweb-player";
import "rrweb-player/dist/style.css";
import { Circle, Square, Play, Download, X, Video } from "lucide-react";

export function SessionRecorder() {
  const [isRecording, setIsRecording] = useState(false);
  const [recordedEvents, setRecordedEvents] = useState<eventWithTime[]>([]);
  const [elapsedSeconds, setElapsedSeconds] = useState(0);
  const [showPlayerModal, setShowPlayerModal] = useState(false);

  const stopFnRef = useRef<(() => void) | null>(null);
  const eventsRef = useRef<eventWithTime[]>([]);
  const timerRef = useRef<number | null>(null);
  const playerContainerRef = useRef<HTMLDivElement | null>(null);

  // Clean up stop recorder on unmount
  useEffect(() => {
    return () => {
      if (stopFnRef.current) stopFnRef.current();
      if (timerRef.current) window.clearInterval(timerRef.current);
    };
  }, []);

  // Initialize or re-render the rrweb-player when modal opens
  useEffect(() => {
    if (showPlayerModal && playerContainerRef.current && recordedEvents.length > 0) {
      playerContainerRef.current.innerHTML = "";
      new rrwebPlayer({
        target: playerContainerRef.current,
        props: {
          events: recordedEvents,
          width: Math.min(window.innerWidth - 64, 960),
          height: Math.min(window.innerHeight - 160, 540),
          autoPlay: true,
        },
      });
    }
  }, [showPlayerModal, recordedEvents]);

  const startRecording = () => {
    eventsRef.current = [];
    setRecordedEvents([]);
    setElapsedSeconds(0);
    setIsRecording(true);

    // Start timer
    timerRef.current = window.setInterval(() => {
      setElapsedSeconds((prev) => prev + 1);
    }, 1000);

    // Start rrweb recorder
    const stop = record({
      emit(event) {
        eventsRef.current.push(event);
      },
      // Optional: Mask password inputs for security
      maskInputOptions: {
        password: true,
      },
    });

    stopFnRef.current = stop || null;
  };

  const stopRecording = () => {
    if (stopFnRef.current) {
      stopFnRef.current();
      stopFnRef.current = null;
    }
    if (timerRef.current) {
      window.clearInterval(timerRef.current);
      timerRef.current = null;
    }
    setIsRecording(false);
    setRecordedEvents([...eventsRef.current]);
  };

  const downloadSession = () => {
    if (recordedEvents.length === 0) return;
    const blob = new Blob([JSON.stringify(recordedEvents, null, 2)], {
      type: "application/json",
    });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `agroprecision-session-${new Date().toISOString().slice(0, 19)}.json`;
    a.click();
    URL.revokeObjectURL(url);
  };

  const formatTimer = (totalSecs: number) => {
    const mins = Math.floor(totalSecs / 60).toString().padStart(2, "0");
    const secs = (totalSecs % 60).toString().padStart(2, "0");
    return `${mins}:${secs}`;
  };

  return (
    <>
      {/* Floating Control Bar in Bottom Right */}
      <div className="fixed bottom-6 right-6 z-50 flex items-center gap-2 rounded-full border border-border/60 bg-card/95 px-3 py-2 shadow-2xl backdrop-blur transition-all">
        {!isRecording ? (
          <>
            <button
              onClick={startRecording}
              className="flex items-center gap-2 rounded-full bg-destructive px-3.5 py-1.5 text-xs font-semibold text-destructive-foreground transition hover:opacity-90"
            >
              <Circle className="size-3 fill-current" />
              <span>Record Session</span>
            </button>

            {recordedEvents.length > 0 && (
              <>
                <button
                  onClick={() => setShowPlayerModal(true)}
                  className="flex items-center gap-1.5 rounded-full bg-secondary px-3 py-1.5 text-xs font-medium text-secondary-foreground transition hover:bg-secondary/80"
                  title="Replay last session"
                >
                  <Play className="size-3" />
                  <span>Replay ({recordedEvents.length})</span>
                </button>
                <button
                  onClick={downloadSession}
                  className="flex items-center gap-1.5 rounded-full border px-2.5 py-1.5 text-xs text-muted-foreground transition hover:bg-accent hover:text-accent-foreground"
                  title="Download session JSON"
                >
                  <Download className="size-3" />
                </button>
              </>
            )}
          </>
        ) : (
          <div className="flex items-center gap-3">
            <div className="flex items-center gap-2">
              <span className="relative flex size-2.5">
                <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-destructive opacity-75" />
                <span className="relative inline-flex size-2.5 rounded-full bg-destructive" />
              </span>
              <span className="font-mono text-xs font-semibold text-foreground">
                {formatTimer(elapsedSeconds)}
              </span>
            </div>

            <button
              onClick={stopRecording}
              className="flex items-center gap-1.5 rounded-full bg-foreground px-3 py-1 text-xs font-medium text-background transition hover:bg-foreground/80"
            >
              <Square className="size-3 fill-current" />
              <span>Stop</span>
            </button>
          </div>
        )}
      </div>

      {/* In-App Replay Modal */}
      {showPlayerModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-background/80 p-4 backdrop-blur-sm">
          <div className="relative flex max-w-[1020px] flex-col rounded-2xl border bg-card p-5 shadow-2xl">
            <div className="mb-4 flex items-center justify-between border-b pb-3">
              <div className="flex items-center gap-2">
                <Video className="size-5 text-primary" />
                <h3 className="font-display text-base font-semibold">
                  Session Replay ({recordedEvents.length} events)
                </h3>
              </div>
              <div className="flex items-center gap-2">
                <button
                  onClick={downloadSession}
                  className="flex items-center gap-1 rounded-lg border px-2.5 py-1 text-xs text-muted-foreground hover:bg-accent"
                >
                  <Download className="size-3.5" /> Export JSON
                </button>
                <button
                  onClick={() => setShowPlayerModal(false)}
                  className="rounded-lg p-1 text-muted-foreground hover:bg-accent"
                >
                  <X className="size-5" />
                </button>
              </div>
            </div>

            {/* Container where rrweb-player injects iframe and controls */}
            <div
              ref={playerContainerRef}
              className="flex items-center justify-center overflow-hidden rounded-xl bg-black"
            />
          </div>
        </div>
      )}
    </>
  );
}
