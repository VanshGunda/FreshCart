# FreshCart Design System Skill

## Source of truth

`src/design.css` is the source of truth for customer-facing layout, colors, spacing, typography, radii, controls, shadows and motion.

## Visual language

FreshCart uses a dark teal + warm cream base, mint as the hero/background accent, coral for primary promotional emphasis and a small yellow highlight.

## Rules

- Use the shared token scale; do not add arbitrary colors, radius values or page-specific heading sizes.
- Use the 4px spacing grid.
- Minimum interactive target: 44px.
- Product/order/address cards use the shared card radius.
- Hero/major panels use the shared panel radius.
- Customer checkout and status surfaces should prioritize clarity over decoration.
- Avoid emoji for primary navigation or action icons. Decorative category imagery may use content illustrations when available.
- Keep focus visible and preserve reduced-motion behavior.
- Prefer shared primitives/classes over one-off selectors.
- Preserve existing React business logic and API contracts when making visual changes.
- Keep server-derived price, payment, inventory and order status authoritative.
