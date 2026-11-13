# Domain Docs

Layout: **multi-context**.

How the engineering skills should consume this repo's domain documentation when exploring the codebase.

## Before exploring, read these

- **`GLOSSARY-MAP.md`** at the repo root when it exists: it points at one `GLOSSARY.md` per context. Read each one relevant to the topic.
- **`GLOSSARY.md`** at the repo root, if a root-level glossary exists.
- **`docs/adr/`**: read ADRs that touch the area you're about to work in. In multi-context repos, also check each context's `docs/adr/` for context-scoped decisions.

If any of these files don't exist, **proceed silently**. Don't flag their absence; don't suggest creating them upfront. The `/domain-modeling` skill (reached via `/grill-with-docs` and `/improve-codebase-architecture`) creates them lazily when terms or decisions actually get resolved.

This repo is multi-context, but `GLOSSARY-MAP.md` and the per-context `GLOSSARY.md` files are intentionally **not created yet**. They get created the first time a real term is written down; the root `GLOSSARY-MAP.md` will then point at them.

## File structure

Multi-context repo (this repo):

```
/
├── GLOSSARY-MAP.md                    ← to be created on first term
├── docs/adr/                          ← system-wide decisions
├── server/
│   ├── GLOSSARY.md                    ← to be created on first term
│   └── docs/adr/                      ← server context decisions
├── app/
│   ├── GLOSSARY.md
│   └── docs/adr/
└── admin/
    ├── GLOSSARY.md
    └── docs/adr/
```

Single-context repos (not this one) use one root `GLOSSARY.md` + `docs/adr/`.

## Use the glossary's vocabulary

When your output names a domain concept (in an issue title, a refactor proposal, a hypothesis, a test name), use the term as defined in `GLOSSARY.md`. Don't drift to synonyms the glossary explicitly avoids.

If the concept you need isn't in the glossary yet, that's a signal: either you're inventing language the project doesn't use (reconsider) or there's a real gap (note it for `/domain-modeling`).

## Flag ADR conflicts

If your output contradicts an existing ADR, surface it explicitly rather than silently overriding:

> _Contradicts ADR-0007 (event-sourced orders), but worth reopening because…_
