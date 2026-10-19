# SmartHub — Claude Project Instructions

## Project Goal

SmartHub is a premium web-based smart-home dashboard inspired by the usability of products such as Echo Hub, Sonos, Apple Home and high-end consumer smart-home interfaces.

The goal is NOT to build a generic admin dashboard.

The interface must feel like a polished consumer product designed for a wall-mounted tablet, while remaining responsive on desktop and mobile.

Primary stack:

- Frontend: React + TypeScript + Vite
- Backend: Java + Spring Boot
- Smart-home integration: Samsung SmartThings
- Primary target device: tablet
- Secondary targets: desktop and mobile

---

# PERMANENT WORKING RULES

These rules apply to every task in this repository. Read them before starting any work. They take priority over default habits. The user should not need to repeat them.

## 1. Git workflow (mandatory)

For every new feature, significant bug fix or requested refactoring:

1. Check `git status` and make sure there are no local changes that would be lost.
2. Switch to `main`: `git checkout main`.
3. Update it: `git pull origin main`.
4. Create a dedicated branch from the updated `main`.
5. Only after the branch exists, start editing code.

Branch naming:

```text
feature/<feature-name>
fix/<fix-name>
refactor/<refactor-name>
```

Examples: `feature/smartthings-device-categories`, `feature/room-image-upload`, `fix/device-type-detection`, `refactor/device-mapping`.

Rules:

- Never implement features directly on `main`.
- A new functional request normally gets a new branch. Do not reuse the branch of a previous task automatically: go back to `main`, pull and branch again, unless the user explicitly says the request belongs to the same feature.
- Uncommitted local changes are never deleted, overwritten or discarded. No destructive operations (`git reset --hard`, `git checkout -- <files>`, `git clean`, force-push, deleting unmerged branches). Preserve the existing work (commit it, or stash it with a clear message) and handle the situation safely before continuing.
- Do not merge into `main` without an explicit request.
- Do not push without an explicit request.

## 2. One feature = one branch

Keep every branch focused on a single feature, fix or refactoring. Do not mix unrelated changes. If independent work emerges during development, report it instead of including it automatically.

## 3. Stack

See "Primary stack" above. Concretely:

- Frontend: `frontend/` (React, TypeScript, Vite), responsive web app.
- Backend: `backend/` (Java, Spring Boot, Maven).
- Integration: Samsung SmartThings API.

Keep compatibility with the existing architecture. Do not introduce new frameworks or heavy dependencies without a concrete reason.

## 4. SmartThings device classification

- Do not infer the device type mainly from capabilities. Capabilities describe what a device can do, not necessarily what kind of device it is.
- The primary source for classifying a device is the SmartThings category, `components[].categories[].name`.
- Classification priority:
  1. SmartThings category of the `main` component;
  2. other specific categories present in the components;
  3. metadata / device profile when available;
  4. inference from a combination of capabilities;
  5. fallback `GENERIC` / `OTHER`.
- Never classify a device as a thermostat only because it exposes `temperatureMeasurement`. An oven, fridge, air conditioner, temperature sensor or other appliance can expose temperature without being a thermostat.
- Keep both the original SmartThings category and a normalized Smart Hub type in the model:

  ```json
  {
    "smartThingsCategory": "Refrigerator",
    "deviceType": "REFRIGERATOR"
  }
  ```

- Unknown SmartThings categories must never cause errors: always keep the original value and fall back to `GENERIC` / `OTHER`.

## 5. Capabilities

Use capabilities to determine state, available commands, switch, level, temperature, setpoint, mode, lock, media, appliance state and other specific features.

Capabilities must never override an explicit and more reliable SmartThings category.

## 6. Frontend

Keep:

- strictly typed TypeScript;
- readable, reusable components;
- responsive layout for desktop, tablet and mobile;
- visual consistency with the existing dashboard;
- no regressions in controls that already work.

When adding a new device type:

- use the normalized type to choose card, icon and behaviour;
- if no specific UI exists yet, use an appropriate generic card;
- never show semantically wrong UI, for example an oven or fridge displayed as a thermostat.

## 7. Backend

Keep:

- clear separation between controllers, services, mappers and models;
- SmartThings logic centralized and not duplicated;
- robust handling of missing data and unrecognized devices;
- compatibility with the existing behaviour.

Avoid hardcoding anything tied only to the devices present in the demo home today. The solution must extend to new SmartThings categories.

## 8. Testing (mandatory)

Every change includes relevant tests. When applicable, run before declaring a task complete:

- Backend, from `backend/`: `mvn test`, and `mvn -B clean verify` / package when packaging is affected.
- Frontend, from `frontend/`: `npm test`, `npx tsc -b` (typecheck), `npm run build`.

Do not declare a task complete if relevant tests fail. If a failure is pre-existing or unrelated, explain clearly why.

## 9. Functional and visual verification

When a change touches UI or user-facing behaviour, also verify:

- desktop, tablet (landscape and portrait) and mobile when relevant;
- the browser console;
- runtime errors;
- obvious regressions.

The detailed visual procedure is in "Mandatory visual review with Playwright" and "Workflow for every major UI task" below.

Before attributing a console error to the current change, check whether it is actually new: compare it with the base branch.

## 10. Definition of Done

Before declaring a feature complete, verify:

- correct branch;
- implementation complete;
- no unrelated changes;
- backend tests run;
- frontend tests run;
- build succeeds;
- functional / visual verification done when relevant;
- no evident regressions.

The final report always states:

- branch name;
- files and main areas modified;
- what was implemented;
- tests run and their results;
- build results;
- visual checks performed;
- remaining issues or limitations.

## 11. Forbidden

- Developing directly on `main`.
- Deleting local changes without consent, or running destructive resets.
- Merging into `main` or pushing without a request.
- Classifying SmartThings devices from a single capability when an explicit category exists.
- Introducing regressions in devices that already work.

## 12. Permanent application

These are permanent repository rules. Read and follow this file before starting any new task.

---

# DESIGN REFERENCES (VISUAL SOURCE OF TRUTH)

The directory `docs/design-reference/` contains the official visual references of SmartHub.

- These images are the **visual source of truth** for the frontend.
- Before any significant redesign, analyze the relevant references first.
- References take priority over default shadcn styling, 21st.dev components or any other component library.
- shadcn and 21st.dev are implementation accelerators, not a source of visual identity.
- Use Playwright to compare the real UI against the references.
- After significant UI changes, save final screenshots to `docs/mockups/final/`.
- Judge visual quality against the references, not only against technical correctness, build success or compilation.

Main references:

- `docs/design-reference/01-home-desktop.png` — home, desktop
- `docs/design-reference/02-room-soggiorno-desktop.png` — room (Soggiorno), desktop
- `docs/design-reference/03-home-mobile.png` — home, mobile
- `docs/design-reference/04-room-soggiorno-mobile.png` — room (Soggiorno), mobile
- `docs/design-reference/05-room-camera-mobile.png` — room (Camera), mobile
- `docs/design-reference/06-room-cucina-mobile.png` — room (Cucina), mobile
- `docs/design-reference/07-shutter-control-mobile.png` — shutter control, mobile
- `docs/design-reference/08-room-images-collage.png` — secondary photo reference for room style and mood only
- `docs/design-reference/09-room-assets-collage.png` — official source for the real room photos (see Room imagery rules)
- `docs/design-reference/smarthub-reference-collage.png` — overview collage of the visual direction

Priority: `01`–`07` are the source of truth for UI, layout, components and responsive behaviour. `09` is the primary source for room photos. `08` is secondary. The overview collage sets the general direction.

---

# OFFICIAL ROOMS AND ROOM IMAGERY

## Official rooms

The main real rooms are:

- Bagno Grande
- Bagno Piccolo
- Cucina
- Corridoio
- Salotto
- Cameretta
- Camera da Letto

Any other room must be supported through the fallback (`default-room`).

## Room imagery rules

- Room images are resolved centrally in `frontend/src/config/roomImages.ts`.
- They must not be hardcoded inside RoomCard or RoomPage.
- They must work in both the Home and the Room View, from the same resolver.
- Unknown names, empty strings, `undefined` and `null` use `default-room`.
- A room that SmartThings returns with a new name must never cause a broken image or an error.
- Assets live in `frontend/src/assets/rooms/` as WebP, cropped from `docs/design-reference/09-room-assets-collage.png`, with no text or UI baked in.
- Responsive crops are set per room with `desktop`, `tablet` and `mobile` object positions, if needed.

## User-uploaded room photos

- Built-in room imagery is application UI content. User-uploaded room imagery is runtime user data and must never be stored under `frontend/src/assets`.
- Custom photos are keyed by `roomId`, never by room name: SmartThings may rename rooms. The name only selects the built-in fallback.
- Resolution order: custom image (`imageUrl` from the backend) → built-in image for the room name (`resolveRoomImage`) → `default-room.webp`. Use `resolveRoomPhoto` everywhere a room photo is shown, so Home and Room View stay consistent.
- The backend stores custom photos under `<SMARTHUB_DATA_DIR>/room-images/` (Docker: `/data`), with a UUID file name and an atomic `index.json`. It validates content by magic bytes, re-encodes to JPEG (no EXIF/GPS) and caps the size at 10 MB.
- Image URLs are versioned (`?v=<updatedAt>`), so a replaced photo is never served from browser cache.
- Visual QA is mandatory for any image upload UX: check the sheet on mobile and desktop, the error toasts above the sheet, and that a new photo appears without reload.

## Mock data

The demo home uses the official rooms plus `Ripostiglio`, an unofficial test room that must always show the default photo.

# UI DESIGN PRINCIPLES

SmartHub must feel:

- premium
- calm
- modern
- tactile
- touch-friendly
- highly readable
- visually balanced
- consumer-oriented

Avoid:

- generic SaaS dashboard aesthetics
- excessive tables
- enterprise admin panel layouts
- excessive borders
- excessive gradients
- excessive glow effects
- decorative animations without purpose
- tiny controls
- dense layouts
- unnecessary icons
- generic AI-generated UI patterns

Prefer:

- large touch targets
- generous spacing
- strong visual hierarchy
- rounded cards
- subtle depth
- restrained motion
- excellent typography
- clear device states
- immediate feedback after interactions

---

# VISUAL QUALITY BAR

Do not consider a UI task complete merely because the code compiles.

Every important UI implementation must be visually inspected.

The quality target is closer to:

- Apple Home
- Sonos
- Echo Hub
- premium automotive infotainment systems
- high-end consumer smart-home panels

than to:

- admin dashboards
- CRM interfaces
- analytics dashboards
- Material Design demo applications

---

# DESIGN TOOLCHAIN

Use the available tools actively.

## Figma

Use Figma as the visual source of truth whenever a Figma design exists.

Before implementing a major screen:

1. inspect the Figma design
2. identify layout, spacing, typography and hierarchy
3. understand reusable patterns
4. map the design to React components
5. preserve visual intent rather than blindly copying pixels

If the design does not exist yet, Figma may be used to explore layout directions before implementation.

---

## 21st.dev

Before inventing a complex UI component from scratch, search 21st.dev for relevant patterns.

Examples:

- smart-home cards
- bottom navigation
- command panels
- sliders
- modal sheets
- quick-action cards
- segmented controls
- contextual menus
- status components

When alternatives exist:

1. inspect multiple options
2. select the one most compatible with SmartHub's visual language
3. simplify it if necessary
4. do not import excessive visual effects

Never allow the component library to dictate the product identity.

SmartHub must still look like SmartHub.

---

## shadcn

Use shadcn as the base component system where appropriate.

Prefer reusable primitives for:

- Dialog
- Drawer
- Sheet
- Button
- Slider
- Tabs
- Tooltip
- Dropdown
- Popover
- Toast
- Skeleton
- Switch

Customize components to match SmartHub rather than leaving them with default styling.

---

## Context7

Use Context7 automatically whenever implementation depends on current documentation or library APIs.

Examples:

- React
- Vite
- TypeScript
- shadcn
- Tailwind
- Playwright
- animation libraries
- accessibility APIs

Do not rely on remembered or potentially outdated library behavior when Context7 can provide current documentation.

---

# MANDATORY VISUAL REVIEW WITH PLAYWRIGHT

This rule is critical.

Never consider a significant UI task complete immediately after writing the code.

For every important UI change:

1. run the application
2. open SmartHub using Playwright
3. inspect the rendered application
4. interact with the controls
5. take screenshots
6. critically evaluate the result
7. fix visual problems
8. repeat until the result is polished

Review at least these viewport sizes:

Desktop:

- 1440 x 900

Tablet landscape:

- 1180 x 820

Tablet portrait:

- 820 x 1180

Mobile:

- 390 x 844

---

# VISUAL REVIEW CHECKLIST

During Playwright review check:

## Layout

- alignment
- spacing
- consistent margins
- visual balance
- card sizing
- empty space
- content density

## Typography

- heading hierarchy
- body readability
- labels
- numerical values
- contrast
- truncation
- line wrapping

## Components

- consistent border radius
- consistent padding
- clear active states
- clear disabled states
- clear online/offline states
- hover states
- pressed states

## Touch UX

- controls large enough for touch
- sliders easy to manipulate
- no tiny icons
- no accidental controls placed too close together

## Responsive behavior

Ensure the application feels intentionally designed at every viewport.

Do not merely stack desktop components vertically on mobile.

---

# SMART HOME UX PRINCIPLES

Device state must be understandable immediately.

Example:

A user should instantly understand:

- which lights are on
- which shutters are open
- current temperature
- whether heating is active
- whether a sensor is triggered
- whether a device is offline

The interface must communicate state using multiple signals:

- layout
- typography
- subtle color
- icon state
- labels

Do not depend on color alone.

---

# HOME DASHBOARD

The home dashboard should prioritize:

1. house status
2. quick actions
3. rooms
4. active devices
5. climate
6. warnings
7. scenes

Avoid overwhelming the user with all devices simultaneously.

The first screen should provide situational awareness.

---

# ROOM VIEW

The room screen is one of the most important SmartHub experiences.

It should include:

- room name
- temperature if available
- contextual room status
- quick actions
- grouped devices
- clear interactive controls

Devices should be grouped logically:

- Lights
- Shutters
- Climate
- Sensors
- Media
- Other

Avoid rendering every device using the exact same generic card.

Use device-specific interfaces.

---

# DEVICE COMPONENTS

Prefer specialized components.

Examples:

- LightCard
- ShutterCard
- ThermostatCard
- SensorCard
- SceneCard

Avoid one giant generic DeviceCard containing dozens of conditional branches.

---

# LIGHT CONTROLS

Light interfaces should support:

- on/off
- dimming
- color temperature when supported
- color when supported

The current state must be immediately visible.

Brightness controls should be comfortable on touch devices.

---

# SHUTTER CONTROLS

Shutters should support:

- open
- close
- stop
- percentage position

Position should be visually obvious.

Avoid tiny increment/decrement buttons as the primary interaction.

---

# THERMOSTAT UX

Show:

- current temperature
- target temperature
- HVAC mode
- heating/cooling state

Temperature values should have strong visual hierarchy.

Do not make thermostat interaction look like a generic form.

---

# SENSORS

Sensor cards should prioritize state clarity.

Examples:

Motion:

- detected
- clear

Contact:

- open
- closed

Battery:

- percentage
- low-battery warning

Connectivity:

- online
- offline

---

# MOTION AND ANIMATION

Use animations only when they improve understanding.

Good uses:

- card press feedback
- toggle transitions
- sheet transitions
- slider feedback
- subtle state transitions

Avoid:

- constant floating effects
- excessive glowing
- decorative particles
- large entrance animations
- animations that delay interaction

Motion should feel restrained and premium.

---

# COLOR

SmartHub supports a light and a dark theme.

- By default the theme follows the operating system (`prefers-color-scheme`).
- The user can force light or dark from the theme selector in the top bar. The choice is stored in localStorage under `smarthub.theme`.
- Dark is the reference theme; the light palette must keep the same visual hierarchy.
- Colour tokens live in `frontend/src/styles/tokens.css`. Do not hardcode colours in components.
- Text placed over room photos uses the `--on-photo` tokens and must stay light in both themes, because the photo overlay is always dark.

Colors should be intentional.

Avoid using many unrelated accent colors.

Color should primarily communicate:

- active
- inactive
- warning
- error
- climate
- device state

Surfaces should remain visually calm.

---

# SPACING

Prefer generous spacing.

Use a consistent spacing scale.

Avoid arbitrary values unless required by the design.

Cards should not feel cramped.

---

# COMPONENT ARCHITECTURE

Prefer reusable components.

Example structure:

src/
  components/
    layout/
    navigation/
    rooms/
    devices/
    scenes/
    climate/
    common/

Possible components:

- AppShell
- TopBar
- SideNavigation
- BottomNavigation
- RoomCard
- RoomHeader
- LightCard
- ShutterCard
- ThermostatCard
- SensorCard
- SceneCard
- QuickAction
- StatusBadge
- DeviceIcon
- SectionHeader

Keep components focused.

Avoid extremely large JSX files.

---

# DATA MODEL

The frontend must not depend directly on raw SmartThings JSON.

Consume normalized backend DTOs. The DTO carries both the original SmartThings category (`smartThingsCategory`) and the normalized `deviceType`; see "SmartThings device classification" in the permanent rules.

Example:

{
  "id": "...",
  "name": "Lampadario",
  "roomId": "...",
  "type": "LIGHT",
  "online": true,
  "state": {
    "power": "ON",
    "brightness": 70
  },
  "capabilities": [
    "POWER",
    "DIMMER"
  ]
}

---

# FRONTEND STATE

Always handle:

- loading
- error
- empty
- offline
- optimistic updates

When controlling a device:

1. update the UI immediately when appropriate
2. send the command
3. confirm the actual backend state
4. revert if the operation fails
5. show a subtle error message

Avoid modal errors for normal device command failures.

---

# ACCESSIBILITY

The dashboard must remain accessible.

Ensure:

- sufficient contrast
- visible focus states
- usable keyboard navigation
- semantic HTML
- meaningful aria labels
- controls not dependent solely on icons
- controls not dependent solely on color

---

# RESPONSIVENESS

SmartHub is tablet-first.

Priority:

1. tablet landscape
2. tablet portrait
3. desktop
4. smartphone

Do not design desktop first and simply shrink it.

Tablet must feel like the primary product.

---

# PERFORMANCE

Avoid unnecessary re-renders.

Avoid loading huge UI libraries for isolated effects.

Prefer lightweight interactions.

Use code splitting if useful.

Images and icons should be optimized.

---

# CODE QUALITY

Frontend:

- strict TypeScript
- avoid `any`
- reusable components
- separate API client
- clear hooks
- predictable state flow

Backend:

- controllers
- services
- DTOs
- SmartThings client layer
- centralized error handling
- logging
- environment-based configuration

---

# MOCK MODE

The UI must remain fully usable in mock mode.

Mock data should feel realistic.

Do not build mock screens that differ structurally from live mode.

The frontend must not know whether the backend is using mock or live SmartThings data.

---

# WORKFLOW FOR EVERY MAJOR UI TASK

Follow this exact workflow:

1. Understand the requested behavior.
2. Inspect current code.
3. Inspect Figma if relevant.
4. Search 21st.dev/shadcn for useful patterns.
5. Check Context7 if current API documentation is needed.
6. Implement.
7. Run the application.
8. Inspect with Playwright.
9. Test desktop.
10. Test tablet landscape.
11. Test tablet portrait.
12. Test mobile.
13. Capture screenshots.
14. Critique the UI.
15. Fix issues.
16. Repeat Playwright review.
17. Run build/tests.
18. Only then consider the task complete.

---

# FINAL QUALITY RULE

Never return a UI implementation without visually validating it.

If the result looks generic, unfinished, dense, inconsistent or like a typical AI-generated dashboard, continue iterating.

SmartHub should feel like a real premium product someone would want permanently mounted on the wall of their home.