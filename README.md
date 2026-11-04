# Tactical Task Management System

A high-performance, industrial-brutalist tactical task management application built with **Vue 3** and **Vite**.

Designed as a **100% local-first, zero-backend single page application (SPA)**: all task records and project data persist securely in your browser's `localStorage`, with direct client-side AI inference powered by your own API keys.

---

## ⚡ Highlights

- **Local-First & Zero Cold Boots**: Runs completely client-side in the browser. Zero server costs, zero 404s, zero downtime, and instant (<100ms) page loads.
- **AI Task Copilot (BYOK)**: Connects directly from your browser to leading LLMs:
  - **Google Gemini API** (`gemini-2.5-flash`, `gemini-2.0-flash`, `gemini-1.5-flash`)
  - **OpenAI Direct API** (`gpt-4o-mini`, `gpt-4o`, `o1-mini`)
  - **Anthropic Claude API** (`claude-3-5-sonnet`, `claude-3-5-haiku`)
  - **Pollinations / Custom Gateways** (Groq, OpenRouter, DeepSeek, or local Ollama)
- **AI Capabilities**:
  - Automated Subtask Decomposition
  - Smart Missing Subtask Suggestions
  - Realistic Due Date & Effort Estimation
  - Priority & Urgency Impact Analysis
  - Operational Executive Summaries
  - One-Click Full Task Audit
- **Data Sovereignty & Portability**:
  - One-click **`[EXPORT DB ⤓]`** to JSON backup.
  - One-click **`[IMPORT DB ⤒]`** to restore from JSON.
  - API keys are stored solely in your local browser disk and never transmitted to any third-party server.
- **Industrial Brutalist Aesthetic**: High-contrast tactical telemetry, CRT scanline atmosphere, real-time counters, status LEDs, and keyboard accessibility.

---

## 🚀 Quick Start

### Prerequisites

- [Node.js](https://nodejs.org/) (version 18 or 20+)
- npm

### Installation & Development

```bash
# Clone the repository
git clone https://github.com/NDTNHT/ccmtpt.git
cd ccmtpt

# Install dependencies
npm install

# Start development server
npm run dev
```

Visit `http://localhost:5173/` in your browser.

### Production Build

```bash
npm run build
```

The output bundle is generated in `dist/`.

---

## 🌐 Deployment (Vercel, Netlify, GitHub Pages)

### Vercel Deployment
Because this repository is a pure Vite application with `vercel.json` included at root, deploying to Vercel requires zero configuration:

1. Import this repository in [Vercel](https://vercel.com).
2. Framework Preset: **Vite** (auto-detected).
3. Root Directory: `./` (default).
4. Click **Deploy**.

---

## 📂 Project Architecture

```
├── .github/
│   └── workflows/
│       └── ci.yml               # Fast GitHub Actions build verification (~10s)
├── src/
│   ├── components/
│   │   ├── ai/                  # AI Copilot, prompt editor, and telemetry logs
│   │   ├── layout/              # Tactical header, nav bar, and alert notifier
│   │   └── tasks/               # Task cards, detail modals, board and ops view
│   ├── services/
│   │   ├── aiService.js         # Direct browser LLM client (Gemini, OpenAI, Claude)
│   │   └── taskService.js       # LocalStorage persistence, task transitions, JSON backup
│   ├── store/
│   │   ├── useAiStore.js        # AI configuration and inference reactive state
│   │   └── useTaskStore.js      # Task board reactive state and stats
│   ├── styles/
│   │   └── brutalist-tokens.css # Industrial brutalist design system & colors
│   ├── App.vue                  # Main application shell with CRT overlays
│   └── main.js                  # Vue 3 bootstrap entrypoint
├── index.html                   # HTML entry point with telemetry fonts
├── package.json                 # npm dependencies & build scripts
├── vercel.json                  # Vercel deployment configuration
└── vite.config.js               # Vite build configuration
```

---

## 📄 License

MIT
