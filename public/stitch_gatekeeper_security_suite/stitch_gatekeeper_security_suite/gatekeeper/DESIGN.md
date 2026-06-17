---
name: GateKeeper
colors:
  surface: '#10141a'
  surface-dim: '#10141a'
  surface-bright: '#353940'
  surface-container-lowest: '#0a0e14'
  surface-container-low: '#181c22'
  surface-container: '#1c2026'
  surface-container-high: '#262a31'
  surface-container-highest: '#31353c'
  on-surface: '#dfe2eb'
  on-surface-variant: '#bbc9cf'
  inverse-surface: '#dfe2eb'
  inverse-on-surface: '#2d3137'
  outline: '#859398'
  outline-variant: '#3c494e'
  surface-tint: '#3cd7ff'
  primary: '#a8e8ff'
  on-primary: '#003642'
  primary-container: '#00d4ff'
  on-primary-container: '#00586b'
  inverse-primary: '#00677e'
  secondary: '#cdbdff'
  on-secondary: '#370096'
  secondary-container: '#5203d5'
  on-secondary-container: '#c0acff'
  tertiary: '#5bfc80'
  on-tertiary: '#003912'
  tertiary-container: '#37df66'
  on-tertiary-container: '#005d23'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#b4ebff'
  primary-fixed-dim: '#3cd7ff'
  on-primary-fixed: '#001f27'
  on-primary-fixed-variant: '#004e5f'
  secondary-fixed: '#e8deff'
  secondary-fixed-dim: '#cdbdff'
  on-secondary-fixed: '#20005f'
  on-secondary-fixed-variant: '#4f00d0'
  tertiary-fixed: '#69ff87'
  tertiary-fixed-dim: '#3ce36a'
  on-tertiary-fixed: '#002108'
  on-tertiary-fixed-variant: '#00531e'
  background: '#10141a'
  on-background: '#dfe2eb'
  surface-variant: '#31353c'
typography:
  headline-lg:
    fontFamily: Inter
    fontSize: 32px
    fontWeight: '700'
    lineHeight: 40px
    letterSpacing: -0.02em
  headline-lg-mobile:
    fontFamily: Inter
    fontSize: 28px
    fontWeight: '700'
    lineHeight: 34px
    letterSpacing: -0.01em
  headline-md:
    fontFamily: Inter
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
  title-lg:
    fontFamily: Inter
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 28px
  body-md:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '500'
    lineHeight: 24px
  body-sm:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  label-caps:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '700'
    lineHeight: 16px
    letterSpacing: 0.05em
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  container-margin: 20px
  stack-gap: 16px
  inner-padding: 24px
  grid-gutter: 12px
  safe-area-bottom: 32px
---

## Brand & Style

This design system is built on a high-fidelity **Dark Glassmorphism** aesthetic tailored for a premium mobile security experience. The personality is authoritative, sophisticated, and technologically advanced, evoking a sense of impenetrable digital protection.

The visual narrative centers on depth and luminosity. UI elements are treated as physical layers of glass floating over a deep, infinite void. This system utilizes frosted translucency, background blurs (32px to 64px), and subtle inner glows to create a high-end "command center" feel. Interaction states prioritize "active protection" signals, using kinetic energy and light to reassure the user that the system is vigilant.

## Colors

The foundation of the design system is a deep-space palette. The background is a vertical gradient from `#0D1117` to `#161B22`, ensuring deep blacks that make the glowing glass elements pop. 

The primary accent, **Cyan (#00D4FF)**, represents active energy and system "health." Each functional security module is assigned a distinct semantic color from the Module Palette to aid in rapid cognitive recognition. These colors should be used for iconography containers and as subtle glowing underlays beneath their respective glass cards to signify an "active" status.

## Typography

The design system utilizes **Inter** for its technical precision and exceptional legibility on high-density mobile screens. The hierarchy is defined by high contrast in weight; titles are bold and tight to create an impactful, secure presence.

Use `headline-lg` for primary dashboard status (e.g., "System Secure"). Use `body-md` with `Medium (500)` weight for primary descriptions to maintain a premium feel over standard `Regular` weights. `label-caps` should be used for category headers above glass cards to provide structure without clutter.

## Layout & Spacing

The layout follows a **Fluid Grid** model optimized for Android handheld devices. It utilizes a 4-column system with a 20px outer margin to ensure content feels contained but breathable.

Spacing follows an 8px base unit. Most interactive cards use a 24px internal padding to accommodate the frosted border effects without crowding the content. Between major security modules, use a 16px stack gap. Vertical rhythm should prioritize "Safe Areas" at the bottom for thumb-driven navigation through a glass-morphic bottom bar.

## Elevation & Depth

Depth is not achieved through traditional drop shadows but through **Layered Refraction**. 

1.  **Level 0 (Base):** The deep navy gradient background.
2.  **Level 1 (Surface):** Glass cards with a 70% opacity, a 40px background blur, and a 1px solid border at 10% white opacity.
3.  **Level 2 (Active):** Interactive elements that, when engaged, emit a "Glow" effect—a soft, 20px Gaussian blur of the primary Cyan or module-specific color positioned *behind* the glass surface.
4.  **Level 3 (Overlay):** Modals and alerts use a darker 85% opacity glass with a more pronounced 1.5px border to separate them from background content.

## Shapes

The shape language is sophisticated and modern. All main containers use `rounded-lg` (16px) to soften the "industrial" feel of security software. Interactive components like buttons and toggle tracks use `rounded-xl` (24px) for a more ergonomic, touch-friendly appearance. Icon containers are distinct 48x48px squares with a `rounded-md` (12px) corner radius to provide a systematic anchor within the fluid glass cards.

## Components

### Glass Cards
The primary container. Must have a `backdrop-filter: blur(40px)`, a subtle linear gradient border (top-left to bottom-right: `white@15%` to `white@0%`), and a background of `rgba(255, 255, 255, 0.05)`.

### Action Buttons
Primary buttons use a solid Cyan (#00D4FF) to Deep Blue gradient. Secondary buttons are "Ghost Glass" style: transparent backgrounds with a solid 1.5px border and white text.

### Icon Containers
A 48px rounded box with a 10% opacity fill of the module's specific color. Inside, 24px minimalist line icons are used with the 100% opacity module color.

### Status Toggles
Custom-designed toggles. The "On" state should trigger a glow effect that spills slightly outside the toggle track, signifying that a "Gate" is active.

### Progress Orbs
For the main dashboard status, use a large circular "Orb" with animating gradient mesh. If "Threats Found," the orb transitions from Cyan to Red (#F44336) with a pulse animation.

### Scanning List
List items should have a 1px bottom divider with a 5% white opacity. Use a "shimmer" loading state across the glass surface during active privacy scans.