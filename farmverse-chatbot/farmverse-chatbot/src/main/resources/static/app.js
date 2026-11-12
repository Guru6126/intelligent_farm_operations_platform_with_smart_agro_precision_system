

(function () {
  const params = new URLSearchParams(location.search);
  const API_BASE = (params.get("api") || `${location.protocol}//${location.hostname}:8081`).replace(/\/$/, "");

  const TOPICS = {
    crops: { title: "Crops", qs: ["What is crop rotation?", "How can I grow rice?", "When should I harvest wheat?"] },
    soil: { title: "Soil", qs: ["How do I improve soil fertility?", "What are the advantages of clay soil?", "How can I prevent soil erosion?"] },
    water: { title: "Water", qs: ["What are the methods of irrigation?", "What is drip irrigation?", "How much water does maize need?"] },
    pests: { title: "Pests & Diseases", qs: ["How do I control armyworm?", "How can I control pests naturally?", "What are the signs of fungal disease in crops?"] },
    fertilizer: { title: "Fertilizer", qs: ["How do I make farmyard manure?", "When should I apply nitrogen fertilizer?", "What is organic farming?"] },
    seeds: { title: "Seeds", qs: ["How do I choose good seeds?", "How should I treat seeds before sowing?", "What is the right time to sow seeds?"] },

    records: { title: "Farm Records", qs: ["Kumaran last month harvest", "What crops did Kumaran grow?", "Priya Sharma last month harvest", "Show last month harvest of Ramesh", "Kumaran ki gata nela harvest details cheppu"] }
  };
  const TIPS = [
    "Rotate crops each season to protect the soil and break pest cycles.",
    "Test your soil before adding fertilizer, so you apply only what is needed.",
    "Water early in the morning to lose less water to the sun and wind.",
    "Check leaves under the surface every few days to catch pests early.",
    "Treat seeds before sowing to reduce disease at the start.",
    "Add compost or farmyard manure to keep the soil alive and crumbly."
  ];

  const $ = (id) => document.getElementById(id);
  const messages = $("messages"), chat = $("chat"), sendBtn = $("send");
  const formMain = $("form-main"), inMain = $("input-main");
  const formChat = $("form-chat"), inChat = $("input-chat");
  const panel = $("panel"), toast = $("toast");
  const langTop = $("lang-top"), langMain = $("lang-main");
  const navBtns = document.querySelectorAll("[data-nav]");
  const cardBtns = document.querySelectorAll("[data-topic]");
  const stacked = () => window.matchMedia("(max-width: 1100px)").matches;

  const ICON = (id) => '<svg class="i"><use href="#i-' + id + '"/></svg>';

  let size = 1;
  try { size = Number(localStorage.getItem("fv-size")) || 1; } catch (e) {}
  function applySize() {
    document.documentElement.classList.remove("size-2", "size-3");
    if (size > 1) document.documentElement.classList.add("size-" + size);
    try { localStorage.setItem("fv-size", String(size)); } catch (e) {}
  }
  applySize();
  $("fontsize").addEventListener("click", () => { size = size % 3 + 1; applySize(); });

  let lang = "en-IN";
  const HINTS = {
    "en-IN": ["Type your question here...", "Type your message..."],
    "hi-IN": ["अपना प्रश्न यहाँ लिखें...", "अपना संदेश लिखें..."],
    "te-IN": ["మీ ప్రశ్నను ఇక్కడ టైప్ చేయండి...", "మీ సందేశాన్ని టైప్ చేయండి..."]
  };
  function setLang(v) {
    lang = v; langTop.value = v; langMain.value = v;
    inMain.placeholder = HINTS[v][0];
    inChat.placeholder = HINTS[v][1];
  }
  langTop.addEventListener("change", () => setLang(langTop.value));
  langMain.addEventListener("change", () => setLang(langMain.value));

  function showToast(text) {
    toast.textContent = text;
    toast.classList.add("show");
    clearTimeout(showToast.t);
    showToast.t = setTimeout(() => toast.classList.remove("show"), 2200);
  }

  const canSpeak = "speechSynthesis" in window;

  function speechLang(text) {
    if (/[\u0C00-\u0C7F]/.test(text)) return "te-IN";
    if (/[\u0900-\u097F]/.test(text)) return "hi-IN";
    return "en-IN";
  }
  let autoRead = false;
  function speak(text, btn) {
    if (!canSpeak) return;
    const wasOn = btn && btn.getAttribute("aria-pressed") === "true";
    speechSynthesis.cancel();
    document.querySelectorAll(".meta button").forEach((b) => b.setAttribute("aria-pressed", "false"));
    if (wasOn) return;
    const u = new SpeechSynthesisUtterance(text);
    u.lang = speechLang(text);
    if (btn) {
      btn.setAttribute("aria-pressed", "true");
      u.onend = u.onerror = () => btn.setAttribute("aria-pressed", "false");
    }
    speechSynthesis.speak(u);
  }
  const autoBtn = $("autoread");
  if (!canSpeak) autoBtn.hidden = true;
  autoBtn.addEventListener("click", () => {
    autoRead = !autoRead;
    autoBtn.setAttribute("aria-pressed", String(autoRead));
    $("autoread-state").textContent = autoRead ? "On" : "Off";
    if (!autoRead && canSpeak) speechSynthesis.cancel();
  });

  const timeNow = () => new Date().toLocaleTimeString([], { hour: "numeric", minute: "2-digit" });
  function scrollChat() { messages.scrollTop = messages.scrollHeight; }

  function addMessage(role, text, opts) {
    opts = opts || {};
    const row = document.createElement("div");
    row.className = "row " + role + (opts.error ? " error" : "") + (opts.typing ? " typing" : "");

    const av = document.createElement("span");
    av.className = "bot-av" + (role === "user" ? " user-av" : "");
    av.innerHTML = ICON(role === "user" ? "user" : "crops");

    const col = document.createElement("div");
    col.className = "col";
    const bubble = document.createElement("div");
    bubble.className = "bubble";
    if (opts.typing) bubble.innerHTML = "<span></span><span></span><span></span>";
    else bubble.textContent = text;
    col.appendChild(bubble);

    if (!opts.typing) {
      const meta = document.createElement("div");
      meta.className = "meta";
      const t = document.createElement("span");
      t.textContent = timeNow();
      meta.appendChild(t);
      if (opts.ai) {
        const a = document.createElement("span");
        a.className = "ai";
        a.textContent = "AI answer. Please confirm with your local agriculture officer.";
        meta.appendChild(a);
      }
      if (opts.records) {
        const r = document.createElement("span");
        r.className = "rec";
        r.textContent = "From the farm records database";
        meta.appendChild(r);
      }
      if (opts.note) {
        const n = document.createElement("span");
        n.className = "ai";
        n.textContent = opts.note;
        meta.appendChild(n);
      }
      if (role === "bot" && !opts.error && canSpeak) {
        const l = document.createElement("button");
        l.type = "button"; l.setAttribute("aria-pressed", "false");
        l.innerHTML = ICON("speaker") + "Listen";
        l.addEventListener("click", () => speak(text, l));
        meta.appendChild(l);
      }
      col.appendChild(meta);
    }
    row.appendChild(av);
    row.appendChild(col);
    messages.appendChild(row);
    scrollChat();
    return row;
  }

  addMessage("bot", "Hello! 👋\nI'm the FarmVerse Nexus assistant.\nAsk me anything about crops, soil, irrigation, pests, fertilizer or seeds.");

  function setStatus(kind, text, title) {
    const dot = $("dot");
    dot.className = "dot " + kind;
    dot.title = title || text;
    $("status-text").textContent = text;
  }
  async function checkHealth() {
    try {
      const res = await fetch(API_BASE + "/api/chatbot/health");
      if (!res.ok) throw new Error("HTTP " + res.status);
      const h = await res.json();
      const miss = h.ollama && h.multilingualReady === false;
      setStatus(miss ? "wait" : "ok",
        miss ? "Online. For Telugu/Hindi run: ollama pull " + h.multilingualModel : "Online. Ask anything about farming...",
        h.records + " answers, " + (h.semanticSearch ? "smart search on" : "keyword search")
        + ", AI " + (h.ollama ? "online" : "offline")
        + ", farm records " + (h.farmerDb ? "connected" : "not connected"));
    } catch (e) {
      setStatus("down", "Not connected to the server", "Cannot reach " + (API_BASE || location.origin));
    }
  }

  async function ask(question) {
    clearPanel();
    if (canSpeak) speechSynthesis.cancel();
    if (stacked()) chat.scrollIntoView({ behavior: "smooth", block: "start" });
    chat.classList.remove("collapsed");
    $("minimize").setAttribute("aria-expanded", "true");
    addMessage("user", question);
    const typing = addMessage("bot", "", { typing: true });
    sendBtn.disabled = true;
    try {
      const res = await fetch(API_BASE + "/api/chatbot/ask", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ question, language: lang.slice(0, 2) })
      });
      if (!res.ok) throw new Error("Server returned " + res.status);
      const data = await res.json();
      typing.remove();
      const answer = data.answer || data.message || "No answer received.";
      const type = data.sourceType || "";
      const failed = type === "none" || /unavailable/i.test(data.source || "");
      const isAi = type.indexOf("ollama") === 0;
      addMessage("bot", answer, { error: failed, ai: isAi, records: type === "farm-records", note: data.note, lang: data.language });
      if (autoRead && !failed) speak(answer, null);
      setStatus("ok", "Online. Ask anything about farming...");
    } catch (e) {
      typing.remove();
      const msg = String(e.message || e);
      addMessage("bot", /^Server returned/.test(msg)
        ? "The server had an error (" + msg + "). Check the red error lines in the terminal where the backend is running."
        : "Cannot reach the FarmVerse server (" + msg + "). Start the backend on " + (API_BASE || location.origin) + ", then ask again.",
        { error: true });
      setStatus("down", "Not connected to the server", String(e.message || e));
    } finally {
      sendBtn.disabled = false;
    }
  }

  function wire(form, input) {
    form.addEventListener("submit", (ev) => {
      ev.preventDefault();
      const q = input.value.trim();
      if (!q || sendBtn.disabled) return;
      input.value = "";
      ask(q);
    });
  }
  wire(formMain, inMain);
  wire(formChat, inChat);

  $("minimize").addEventListener("click", () => {
    const c = chat.classList.toggle("collapsed");
    $("minimize").setAttribute("aria-expanded", String(!c));
  });

  function setActive(name) {
    navBtns.forEach((b) => {
      const on = b.dataset.nav === name;
      b.classList.toggle("active", on);
      if (on) b.setAttribute("aria-current", "page"); else b.removeAttribute("aria-current");
    });
    cardBtns.forEach((b) => b.setAttribute("aria-pressed", String(b.dataset.topic === name)));
  }
  function clearPanel() { panel.hidden = true; panel.innerHTML = ""; setActive("home"); }

  function showTopic(key) {
    const t = TOPICS[key];
    setActive(key);
    panel.innerHTML = "";
    const h = document.createElement("h3");
    h.textContent = t.title + ": tap a question";
    const opts = document.createElement("div");
    opts.className = "opts";
    t.qs.forEach((q) => {
      const b = document.createElement("button");
      b.type = "button"; b.textContent = q;
      b.addEventListener("click", () => { if (!sendBtn.disabled) ask(q); });
      opts.appendChild(b);
    });
    panel.append(h, opts);
    panel.hidden = false;
    $("ask").scrollIntoView({ behavior: "smooth", block: "start" });
  }

  function showTips() {
    setActive("tips");
    panel.innerHTML = "<h3>Farm tips</h3>";
    const ul = document.createElement("ul");
    TIPS.forEach((tip) => { const li = document.createElement("li"); li.textContent = tip; ul.appendChild(li); });
    panel.appendChild(ul);
    panel.hidden = false;
    $("ask").scrollIntoView({ behavior: "smooth", block: "start" });
  }

  navBtns.forEach((b) => b.addEventListener("click", () => {
    const k = b.dataset.nav;
    if (TOPICS[k]) showTopic(k);
    else if (k === "tips") showTips();
    else if (k === "home") { clearPanel(); $("center").scrollTo({ top: 0, behavior: "smooth" }); window.scrollTo({ top: 0, behavior: "smooth" }); }
    else showToast(b.textContent.trim() + " is coming soon");
  }));
  cardBtns.forEach((b) => b.addEventListener("click", () => showTopic(b.dataset.topic)));

  const SR = window.SpeechRecognition || window.webkitSpeechRecognition;
  const mic = $("mic");
  if (!SR) {
    mic.addEventListener("click", () => showToast("Voice input needs Chrome or Edge"));
  } else {
    let rec = null;
    mic.addEventListener("click", () => {
      if (rec) { rec.stop(); return; }
      rec = new SR();
      rec.lang = lang;
      rec.interimResults = false;
      rec.onresult = (e) => { inMain.value = e.results[0][0].transcript; };
      rec.onend = () => {
        mic.classList.remove("on"); $("mic-text").textContent = "Voice Input"; rec = null;
        if (inMain.value.trim()) formMain.requestSubmit();
      };
      rec.onerror = (e) => {
        mic.classList.remove("on"); $("mic-text").textContent = "Voice Input"; rec = null;
        if (e && e.error === "not-allowed") showToast("Allow the microphone to use voice input");
      };
      mic.classList.add("on");
      $("mic-text").textContent = "Listening...";
      rec.start();
    });
  }

  checkHealth();
  setInterval(checkHealth, 30000);
})();
