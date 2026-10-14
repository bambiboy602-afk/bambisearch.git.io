# Project LeadTrace: Autonomous Web & Archive Discovery Engine

An autonomous, evidence-grounded research and document discovery engine for Android that interrogates public archives, open repositories, and web indices, extracts new leads and entities with Gemini AI, and recursively explores citations within a configurable query budget.

## User Review & Critical Decisions

> [!IMPORTANT]
> **Confirmed Workflow & Safety Scope**:
> - **Search & Extraction Pipeline**: Focuses on authorized public web archives, open document repositories, Reddit public search API, GitHub API, and Wayback Machine to discover documents, public datasets, archived webpages, and research materials.
> - **Lead Chaining & Budget Control**: Gemini extracts terminology, citations, filenames, and links from search findings, deduplicates against visited URLs, ranks evidence confidence, and follows high-value leads recursively until the configurable search budget (e.g., 20 queries default) is reached or strong evidence is confirmed.
> - **Evidence & Verification Separation**: Findings display verified source dates, direct download/archive links, exact matching excerpts, confidence ratings, and clear labeling between verified citations and speculative AI leads.

- **Confirmed Decision 1 (Scope & Ethics)**: Real HTTP-based queries to public search and archive endpoints (Internet Archive Wayback Machine API, GitHub Repositories API, Reddit JSON Search, Wikipedia/Open Library API) with zero unauthorized access or malicious payload tools.
- **Confirmed Decision 2 (Configurable Budget Guardrail)**: Strict search budget counter (default 20 queries, customizable from 5 to 100) preventing credit exhaustion, with early termination thresholds when high-confidence evidence (>85%) is discovered.
- **Confirmed Decision 3 (Local Persistence & Dossier Export)**: Investigations, visited URL graph, extracted leads queue, and ranked evidence cards are saved in a local Room database with JSON/Markdown export capability.

---

## 1. Overview & Core Concept

### What It Does
**LeadTrace** is an autonomous investigative research assistant that automates deep document and archive discovery. When a user enters an initial research question or target query:
1. **Initial Search & Web Ingestion**: The engine queries configured public archive APIs (Internet Archive, GitHub Code/Docs, Reddit discussions, Wikipedia open records) and retrieves document hits, archived captures, and public metadata.
2. **Gemini AI Evidence Analysis & Extraction**: Gemini analyzes retrieved document text and snippets, extracting:
   - Primary names, organizations, and entities
   - Direct document links and file paths (`.pdf`, `.csv`, `.json`, `.txt`, `.docx`)
   - Citations and bibliographic references
   - Novel specialized terminology and alternative search keywords
   - Evidence confidence score (0–100%) with matching verbatim excerpts
3. **Automated Deduplicated Lead Chaining**: Discovered leads and terminology are prioritized in a research queue, checked against visited URLs to prevent loops, and executed sequentially as follow-up queries.
4. **Budget Protection**: A prominent budget monitor (e.g. 20 query cap) decrements per search hop. If high-confidence matching documents are secured, the agent can pause or redirect queries into deeper verification.
5. **Ranked Dossier Presentation**: All results are grouped into an interactive evidence dossier with source dates, direct archive links, duplicate detection flags, and an exportable research memo.

### Target Audience & Persona
- Journalists, academic researchers, and open-source intelligence (OSINT) investigators tracing citations, unearthing archived whitepapers, and mapping public document trails across disparate web repositories.

### Key Value
Eliminates manual citation hopping and tedious search-result parsing by autonomously discovering, extracting, deduplicating, and ranking public document leads while preventing runaway API consumption.

---

## 2. User Experience & Visual Design

### Key User Flows
1. **Launch & Investigation Setup**:
   - The user opens the app and sees the "Active Dossiers" dashboard with recent investigations, quick stats (total documents found, queries used, verified citations), and a floating "New Investigation" button.
   - When tapping "New Investigation", a clean modal or sheet allows entering the primary inquiry, selecting active sources (Internet Archive, GitHub, Reddit, Open Web), and tuning the Query Budget (slider: 5 to 50, default 20).
2. **Autonomous Investigation Loop (Live View)**:
   - Upon launching, the Live Investigation screen renders a dynamic status cockpit:
     - **Query Budget Meter**: Visual radial or bar gauge showing remaining queries (e.g., `14/20 queries left`).
     - **Live Terminal / Agent Log**: Pulsing activity feed displaying real-time actions ("Searching Internet Archive for query #1...", "Gemini extracting terms from 4 candidate pages...", "Found 3 new PDF leads...").
     - **Control Buttons**: "Pause", "Resume", "Add Manual Lead", and "Finish Early".
3. **Evidence & Leads Dashboard**:
   - Tab 1: **Ranked Evidence Cards**: High-confidence evidence items with source tag (e.g., `[Internet Archive]`, `[GitHub]`), file type badges (`PDF`, `DOC`, `DATASET`), publication/capture date, exact verbatim excerpts, confidence meter (color-coded green/amber), and direct tap-to-open links.
   - Tab 2: **Lead Expansion Graph / Queue**: Visual queue showing pending leads, evaluated leads, and discarded duplicates. Users can tap to immediately prioritize a specific lead or remove dead ends.
   - Tab 3: **Extracted Terminology & Citations**: Interactive chip cloud of discovered jargon, names, and references.
4. **Dossier Export & Share**:
   - Tap "Export Dossier" to generate a formatted Markdown or plain-text evidence summary containing verified direct links, source timestamps, and AI-synthesized findings.

### Visual Identity & Theme
- **Aesthetic Direction**: High-tech Investigative OSINT Terminal — dark slate, obsidian backgrounds, sharp typography, neon emerald accents for high confidence, amber for unverified leads, and subtle glassmorphic container cards.
- **Color Palette**:
  - `Background`: Obsidian dark `#0B0E14` and `#121721`
  - `Surface / Cards`: Slate surface `#1A202C` with border `#2D3748`
  - `Primary / Accent`: Signal Teal / Emerald `#10B981` (verified evidence, start actions)
  - `Secondary Accent`: Electric Cyan `#06B6D4` (lead generation, query counter)
  - `Warning / Speculative`: Amber `#F59E0B` (unverified leads, duplicate warnings)
  - `Text`: High-contrast pure white `#F9FAFB` and muted gray `#94A3B8`
- **Typography & Hierarchy**:
  - Clean monospaced accents (`FontFamily.Monospace`) for query budgets, URLs, file hashes, and terminal logs.
  - Crisp sans-serif headers (`headlineMedium`, `titleMedium`) for dossier titles and ranked evidence cards.
- **Interactive Feedback**:
  - Smooth animated progress transitions for budget countdown and confidence scores.
  - Badges with tactile ripple on tap to open links in external browser.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Real Public APIs vs Simulated Web Crawling**
  - *Chosen Approach*: Implement authentic network calls using OkHttp/Retrofit to public APIs:
    1. **Internet Archive Metadata & Wayback Availability API**: `https://archive.org/advancedsearch.php` and `https://web.archive.org/cdx/search/cdx` (retrieves real archived documents, public PDFs, and captures).
    2. **GitHub Search API**: `https://api.github.com/search/repositories` and code/issues (searches public documentation, whitepapers, and repos).
    3. **Reddit Public JSON Search**: `https://www.reddit.com/search.json?q=...` (public forum discussions and shared document links).
    4. **Wikipedia / Wikimedia Search API**: `https://en.wikipedia.org/w/api.php` for references and citations.
  - *Why*: Provides genuine, real-time discovery of public archives without fake data, complying with the platform's "No Mock Data" rule.
- **Decision 2: Gemini Integration for Term Extraction & Lead Generation**
  - *Chosen Approach*: Use Gemini via Firebase AI Logic / Gemini API with structured JSON output (`Schema.obj` / structured JSON prompts) to analyze raw document snippets, extract candidate lead queries, confidence ratings, and citations.
  - *Why*: Offloads natural language entity extraction to Gemini, transforming raw HTML/text into actionable search queries and ranked evidence.
- **Decision 3: Configurable Search Budget Engine**
  - *Chosen Approach*: Coroutine-based investigation state machine with atomic budget counter (`queriesRemaining`), duplicate URL cache (HashSet + Room table), and early stopping criteria (e.g. stop if high-confidence answer threshold is reached).
  - *Why*: Directly fulfills user request: prevents runaway query loops and protects API allowances like a responsible researcher.
- **Decision 4: Local Room Database for Multi-Session Persistence**
  - *Chosen Approach*: Room SQLite database storing `InvestigationEntity`, `EvidenceItemEntity`, `LeadItemEntity`, and `SearchLogEntity`.
  - *Why*: Allows users to save multiple investigation cases, revisit discovered PDF links offline, and resume paused searches anytime.

---

## 4. Technical Architecture & Data Strategy

### System Architecture Diagram

```
┌────────────────────────────────────────────────────────────────────────┐
│                        Jetpack Compose UI Layer                        │
├────────────────────┬────────────────────┬──────────────────────────────┤
│  DossierListScreen │  LiveCockpitScreen │  EvidenceDetailScreen & Leads │
└─────────┬──────────┴─────────┬──────────┴──────────────┬───────────────┘
          │                    │                         │
          ▼                    ▼                         ▼
┌────────────────────────────────────────────────────────────────────────┐
│                        InvestigationViewModel                          │
│  - Coroutine State Machine (Idle, Running, Paused, Completed)          │
│  - Budget Tracker (maxQueries, consumedQueries, remainingBudget)       │
│  - Active Leads Queue & Visited URL Deduplication Set                  │
└─────────┬────────────────────┬─────────────────────────┬───────────────┘
          │                    │                         │
          ▼                    ▼                         ▼
┌──────────────────┐ ┌──────────────────┐ ┌──────────────────────────────┐
│ ArchiveCrawler   │ │ GeminiLeadEngine │ │ Room Local Database          │
│ - Archive.org    │ │ - Structured     │ │ - InvestigationDao           │
│ - GitHub Search  │ │   Evidence       │ │ - EvidenceDao                │
│ - Reddit JSON    │ │ - Term Extractor │ │ - LeadsQueueDao              │
│ - Wikipedia API  │ │ - Confidence     │ │ - LogEntryDao                │
└──────────────────┘ └──────────────────┘ └──────────────────────────────┘
```

### Data Model & Entities
- **InvestigationEntity**:
  - `id: String` (UUID)
  - `title: String`
  - `initialQuery: String`
  - `maxBudget: Int`
  - `queriesUsed: Int`
  - `status: String` (RUNNING, PAUSED, COMPLETED)
  - `createdAt: Long`, `updatedAt: Long`
- **EvidenceItemEntity**:
  - `id: String`
  - `investigationId: String`
  - `sourceName: String` (e.g., "Internet Archive", "GitHub", "Reddit")
  - `title: String`
  - `directUrl: String`
  - `fileType: String` (PDF, DOCX, CSV, HTML, TXT)
  - `sourceDate: String`
  - `excerpt: String`
  - `confidenceScore: Int` (0-100)
  - `isDuplicate: Boolean`
  - `createdAt: Long`
- **LeadItemEntity**:
  - `id: String`
  - `investigationId: String`
  - `queryPhrase: String`
  - `parentEvidenceId: String?`
  - `leadType: String` (CITATION, TERMINOLOGY, FILENAME, LINK)
  - `priority: Int`
  - `status: String` (PENDING, SEARCHED, SKIPPED)
- **SearchLogEntity**:
  - `id: String`
  - `investigationId: String`
  - `stepNumber: Int`
  - `queryExecuted: String`
  - `resultsCount: Int`
  - `timestamp: Long`

### Interactive Component & State Transitions
- **`StartInvestigation(query, budget, sources)`**: Initializes database records, populates seed query into the lead queue, launches the investigation worker coroutine.
- **Autonomous Search Loop**:
  1. While `queriesRemaining > 0` and `leadQueue.hasPending()` and `status == RUNNING`:
  2. Pop top priority lead.
  3. Dispatch API search queries across enabled sources.
  4. Deduplicate resulting URLs against visited cache.
  5. Pass new document snippets to Gemini for entity/lead/citation extraction and confidence scoring.
  6. Insert new evidence items and enqueue newly discovered terms/leads.
  7. Decrement budget counter.
  8. If top evidence confidence exceeds 90% or user requested stop, pause or complete gracefully.
- **User Interactive Interventions**:
  - Pause / Resume at any time.
  - Manually boost or delete leads in the queue.
  - Filter evidence by file type (e.g. only PDFs and documents) or confidence rating.
  - Export full dossier as formatted Markdown.
