# FreshCart frontend

React 19 + Vite + react-router 7 + axios (plain JavaScript). Backend: Spring Boot at `VITE_API_BASE_URL`.

## Rules (source of truth)
- Visual design expertise: the official `frontend-design` plugin (enabled in `.claude/settings.json`).
- FreshCart-specific tokens and UI rules: `.claude/skills/design-system/SKILL.md`. Follow it over generic taste.
- Prices, payment status and order state come from the server. Never trust or compute them client-side.
- No secrets in the bundle. No `dangerouslySetInnerHTML`. Route guards are UX only; the backend authorizes.
- Every screen needs loading, empty, error and success states. Never render a blank page on failure.
- `src/design.css` is the design-system source of truth: use its tokens for color, spacing, typography, radius, controls, shadows and motion.
- Keep keyboard focus visible, respect `prefers-reduced-motion`, keep touch targets at least 44px.
- Do not add page-specific visual tokens or arbitrary colors/radii without a design-system reason.

## Commands
`npm run dev` · `npm run build` (no lint or test tooling is set up yet)
