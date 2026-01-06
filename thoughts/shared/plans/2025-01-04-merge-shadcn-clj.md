# Merge shadcn-clj into Replicant-Component and Scene Modules

## Overview

Merge the shadcn-clj project into the synapse mono repo:
1. Migrate shadcn UI components to `replicant-component/` module (replacing existing versions)
2. Migrate scene showcase files to `scene/` module
3. Delete the shadcn-clj project after migration

## Current State Analysis

**shadcn-clj structure:**
- `src/scene/shadcn/*.cljs` - 56 component showcase files (button.cljs, card.cljs, avatar.cljs, etc.)
- `src/scene/component/*.cljs` - composite component showcases (navbar.cljs, multi-input.cljs, multi-select.cljs, table.cljs, table-filter.cljs, chat.cljs)
- `src/scene/ai/*.cljs` - AI chat showcase scenes (chat.cljs, diff.cljs) - **SKIPPED** per user request
- `src/component/ai/*.cljs` - AI component implementations - **SKIPPED**
- `src/component/app/proxy_options.cljs` - proxy options component
- `src/ui/*.cljc` - basic UI components (button, multi_input, multi_select, popup, removeable_tags, table)
- `resources/public/input.css` - Tailwind CSS config with DaisyUI
- `resources/public/output.css` - compiled CSS (267KB)
- `resources/public/index.html` - dev server HTML
- Dependencies: Tailwind CSS, DaisyUI, TipTap, floating-ui

**synapse existing structure:**
- `replicant-component/` - has `ui/` (avatar, button, popup, removeable_tags, table, tabs) and `component/` (completion_input, server_filter, table, table_filter, multi_input, multi_select)
- `scene/` - has card.cljs (FSRS flashcard scenes), chessboard.cljs, and a minimal scenes.cljs entry point

**Key overlaps (to be replaced with shadcn-clj versions):**
- avatar.cljc vs avatar.cljs
- button.cljc vs button.cljs
- table.cljc vs table.cljs
- tabs.cljc vs tabs.cljs (in shadcn-clj but different content)
- multi_input.cljc vs multi-input.cljs
- multi_select.cljc vs multi-select.cljs

## Desired End State

After this plan is complete:
1. All shadcn UI component implementations are in `components/replicant-component/src/com/zihao/replicant_component/ui/`
2. All composite components are in `components/replicant-component/src/com/zihao/replicant_component/component/`
3. All scene showcase files are in `components/scene/src/com/zihao/scene/shadcn/` and `components/scene/src/com/zihao/scene/component/`
4. The main `scenes.cljs` in `scene/` includes all shadcn showcases
5. Tailwind CSS is properly configured for the scene component
6. The shadcn-clj project can be safely deleted

### Key Discoveries:
- shadcn-clj uses `.cljs` extension (ClojureScript only), while synapse uses `.cljc` (Clojure + ClojureScript)
- Namespace in shadcn-clj is `scene.shadcn.*`, needs to change to `com.zihao.scene.shadcn.*`
- Namespace in shadcn-clj `ui/*` is bare namespace, needs to change to `com.zihao.replicant-component.ui.*`
- The shadcn-clj scene files use `portfolio.replicant/defscene` macro
- Tailwind CSS config in shadcn-clj uses `@import "tailwindcss"` syntax (Tailwind v4)
- synapse scene component already has Tailwind configured with DaisyUI

## What We're NOT Doing

- **NOT** migrating AI components (TipTap-based chat, diff, interpolate) - user requested to skip these
- **NOT** migrating the chat component showcase
- **NOT** migrating `src/component/ai/` directory
- **NOT** migrating `src/component/chat/` directory
- **NOT** keeping the old versions of overlapping components (replacing as requested)
- **NOT** updating the web-app to use new components (future work)

## Implementation Approach

**High-level strategy:**
1. First, migrate UI component implementations to `replicant-component/` (the actual reusable components)
2. Then, migrate composite components to `replicant-component/component/`
3. Then, migrate scene showcases to `scene/` (the portfolio demos)
4. Update the main scenes.cljs to require all new showcases
5. Update Tailwind CSS configuration
6. Verify everything works
7. Clean up shadcn-clj project

## Phase 1: Migrate UI Components to replicant-component

### Overview
Migrate basic UI components from `shadcn-clj/src/ui/` and `shadcn-clj/scene/component/removeable_tags.cljs` to `replicant-component/`.

**Note:** The main shadcn showcase files (`scene/shadcn/*.cljs`) are demo scenes, not reusable components. We'll handle those in Phase 3. This phase focuses on the actual reusable components.

### Changes Required:

#### 1. Migrate and replace existing UI components

**Files to migrate from shadcn-clj:**
- `src/ui/button.cljc` → `replicant-component/src/com/zihao/replicant_component/ui/button.cljc` (REPLACE)
- `src/ui/multi_input.cljc` → `replicant-component/src/com/zihao/replicant_component/ui/multi_input.cljc` (REPLACE)
- `src/ui/multi_select.cljc` → `replicant-component/src/com/zihao/replicant_component/ui/multi_select.cljc` (REPLACE)
- `src/ui/popup.cljc` → `replicant-component/src/com/zihao/replicant_component/ui/popup.cljc` (REPLACE)
- `src/ui/removeable_tags.cljc` → `replicant-component/src/com/zihao/replicant_component/ui/removeable_tags.cljc` (REPLACE)
- `src/ui/table.cljc` → `replicant-component/src/com/zihao/replicant_component/ui/table.cljc` (REPLACE)
- `scene/component/removeable_tags.cljs` → merge into removeable_tags.cljc

**Namespace changes:**
- Existing files use bare namespace or no ns declaration - need to add `com.zihao.replicant-component.ui.*`

**Files to add (new components from shadcn-clj):**
- `scene/shadcn/avatar.cljs` content → create `replicant-component/src/com/zihao/replicant_component/ui/avatar.cljc` (REPLACE)
- `scene/shadcn/tabs.cljs` content → create `replicant-component/src/com/zihao/replicant_component/ui/tabs.cljc` (REPLACE)

### Success Criteria:

#### Automated Verification:
- [ ] All new UI files exist: `ls components/replicant-component/src/com/zihao/replicant_component/ui/`
- [ ] Namespace declarations are correct: `grep "ns com.zihao.replicant-component.ui" components/replicant-component/src/com/zihao/replicant_component/ui/*.cljc`
- [ ] File extensions are `.cljc` not `.cljs`

#### Manual Verification:
- [ ] Review each migrated component for any cljs-specific code that needs to be converted to cljc
- [ ] No references to old namespaces remain

---

## Phase 2: Migrate Composite Components to replicant-component

### Overview
Migrate composite component **logic** (reusable functions) to `replicant-component/component/`, keeping **showcase** parts (defscene) for Phase 3.

**Key principle:** Separate logic from showcase:
- Reusable component functions → `replicant-component/component/*.cljc`
- Showcase defscene macros → `scene/component/*.cljs` (in Phase 3)

### Changes Required:

#### 1. Migrate proxy_options component

**File:** `shadcn-clj/src/component/app/proxy_options.cljs`
**To:** `replicant-component/src/com/zihao/replicant_component/component/app/proxy_options.cljc`

Namespace: `component.app.proxy-options` → `com.zihao.replicant-component.component.proxy-options`

#### 2. Extract and migrate composite component logic

These files contain BOTH reusable logic AND showcase scenes. Extract only the reusable parts:

| Source File | Reusable Function(s) | Target Location |
|-------------|---------------------|-----------------|
| `scene/component/navbar.cljs` | `navbar` function | `replicant-component/src/com/zihao/replicant_component/component/navbar.cljc` |
| `scene/component/table.cljs` | `table` function | `replicant-component/src/com/zihao/replicant_component/component/data-table.cljc` |
| `scene/component/multi-input.cljs` | `multi-input` function | May duplicate `ui/multi_input.cljc` - review and merge |
| `scene/component/multi-select.cljs` | `multi-select` function | May duplicate `ui/multi_select.cljc` - review and merge |
| `scene/component/table-filter.cljs` | `table-filter` function | `replicant-component/src/com/zihao/replicant_component/component/table-filter.cljc` |
| `scene/component/removeable_tags.cljs` | `removeable-tags` function | Already exists in `ui/removeable_tags.cljc` - review and merge |

**For each file:**
1. Read the source file
2. Identify the reusable component function (not defscene macros)
3. Extract to new `.cljc` file with proper namespace
4. Convert any cljs-specific code to cljc if needed
5. Keep defscene showcase for migration in Phase 3

### Success Criteria:

#### Automated Verification:
- [ ] proxy_options.cljc exists in target location
- [ ] navbar.cljc exists with `navbar` function
- [ ] data-table.cljc exists with `table` function
- [ ] table-filter.cljc exists with `table-filter` function
- [ ] All namespaces are correct: `grep "ns com.zihao.replicant-component.component" replicant-component/src/com/zihao/replicant_component/component/*.cljc`

#### Manual Verification:
- [ ] Review each extracted component for cljs-specific code that needs conversion
- [ ] Verify no defscene macros remain in replicant-component files
- [ ] Check for and resolve any duplicate logic (e.g., multi-input, multi-select, removeable-tags)

---

## Phase 3: Migrate Scene Showcases to scene/ Component

### Overview
Migrate all the `defscene` showcase files to `scene/` component under `com.zihao.scene.shadcn.*`.

### Changes Required:

#### 1. Create shadcn showcase directory structure

**Target:** `components/scene/src/com/zihao/scene/shadcn/`

**Files to migrate (56 files):**
```
scene/shadcn/alert.cljs → com.zihao.scene.shadcn/alert.cljs
scene/shadcn/avatar.cljs → com.zihao.scene.shadcn/avatar.cljs
scene/shadcn/badge.cljs → com.zihao.scene.shadcn/badge.cljs
scene/shadcn/breadcrumbs.cljs → com.zihao.scene.shadcn/breadcrumbs.cljs
scene/shadcn/browser_mockup.cljs → com.zihao.scene.shadcn/browser-mockup.cljs
scene/shadcn/button.cljs → com.zihao.scene.shadcn/button.cljs
scene/shadcn/card.cljs → com.zihao.scene.shadcn/card.cljs
scene/shadcn/checkbox.cljs → com.zihao.scene.shadcn/checkbox.cljs
scene/shadcn/class_converter.cljs → com.zihao.scene.shadcn/class-converter.cljs
scene/shadcn/code_mockup.cljs → com.zihao.scene.shadcn/code-mockup.cljs
scene/shadcn/collapse.cljs → com.zihao.scene.shadcn/collapse.cljs
scene/shadcn/countdown.cljs → com.zihao.scene.shadcn/countdown.cljs
scene/shadcn/divider.cljs → com.zihao.scene.shadcn/divider.cljs
scene/shadcn/drawer.cljs → com.zihao.scene.shadcn/drawer.cljs
scene/shadcn/dropdown.cljs → com.zihao.scene.shadcn/dropdown.cljs
scene/shadcn/fieldset.cljs → com.zihao.scene.shadcn/fieldset.cljs
scene/shadcn/file_input.cljs → com.zihao.scene.shadcn/file-input.cljs
scene/shadcn/filter.cljs → com.zihao.scene.shadcn/filter.cljs
scene/shadcn/footer.cljs → com.zihao.scene.shadcn/footer.cljs
scene/shadcn/hero.cljs → com.zihao.scene.shadcn/hero.cljs
scene/shadcn/indicator.cljs → com.zihao.scene.shadcn/indicator.cljs
scene/shadcn/join.cljs → com.zihao.scene.shadcn/join.cljs
scene/shadcn/kbd.cljs → com.zihao.scene.shadcn/kbd.cljs
scene/shadcn/label.cljs → com.zihao.scene.shadcn/label.cljs
scene/shadcn/link.cljs → com.zihao.scene.shadcn/link.cljs
scene/shadcn/list.cljs → com.zihao.scene.shadcn/list.cljs
scene/shadcn/loading.cljs → com.zihao.scene.shadcn/loading.cljs
scene/shadcn/menu.cljs → com.zihao.scene.shadcn/menu.cljs
scene/shadcn/model.cljs → com.zihao.scene.shadcn/model.cljs
scene/shadcn/navbar.cljs → com.zihao.scene.shadcn/navbar.cljs
scene/shadcn/pagination.cljs → com.zihao.scene.shadcn/pagination.cljs
scene/shadcn/phone_mockup.cljs → com.zihao.scene.shadcn/phone-mockup.cljs
scene/shadcn/progress.cljs → com.zihao.scene.shadcn/progress.cljs
scene/shadcn/radial_progress.cljs → com.zihao.scene.shadcn/radial-progress.cljs
scene/shadcn/radio.cljs → com.zihao.scene.shadcn/radio.cljs
scene/shadcn/range.cljs → com.zihao.scene.shadcn/range.cljs
scene/shadcn/rating.cljs → com.zihao.scene.shadcn/rating.cljs
scene/shadcn/select.cljs → com.zihao.scene.shadcn/select.cljs
scene/shadcn/skeleton.cljs → com.zihao.scene.shadcn/skeleton.cljs
scene/shadcn/stack.cljs → com.zihao.scene.shadcn/stack.cljs
scene/shadcn/stat.cljs → com.zihao.scene.shadcn/stat.cljs
scene/shadcn/status.cljs → com.zihao.scene.shadcn/status.cljs
scene/shadcn/swap.cljs → com.zihao.scene.shadcn/swap.cljs
scene/shadcn/tabs.cljs → com.zihao.scene.shadcn/tabs.cljs
scene/shadcn/textarea.cljs → com.zihao.scene.shadcn/textarea.cljs
scene/shadcn/text_input.cljs → com.zihao.scene.shadcn/text-input.cljs
scene/shadcn/timeline.cljs → com.zihao.scene.shadcn/timeline.cljs
scene/shadcn/toggle.cljs → com.zihao.scene.shadcn/toggle.cljs
scene/shadcn/toast.cljs → com.zihao.scene.shadcn/toast.cljs
scene/shadcn/tooltip.cljs → com.zihao.scene.shadcn/tooltip.cljs
scene/shadcn/validator.cljs → com.zihao.scene.shadcn/validator.cljs
scene/shadcn/window_mockup.cljs → com.zihao.scene.shadcn/window-mockup.cljs
```

**Namespace changes:**
- `scene.shadcn.*` → `com.zihao.scene.shadcn.*`

#### 2. Migrate component showcase directory

**Target:** `components/scene/src/com/zihao/scene/component/`

**Files to migrate (showcase defscene parts only):**
```
scene/component/navbar.cljs → com.zihao.scene.component.navbar (defscene only, logic extracted in Phase 2)
scene/component/multi-input.cljs → com.zihao.scene.component.multi-input (defscene only)
scene/component/multi-select.cljs → com.zihao.scene.component.multi-select (defscene only)
scene/component/table.cljs → com.zihao.scene.component.table (defscene only, logic extracted in Phase 2)
scene/component/table-filter.cljs → com.zihao.scene.component.table-filter (defscene only)
scene/component/removeable_tags.cljs → com.zihao.scene.component.removeable-tags (defscene only)
```

**Note:** These files will now only contain the `defscene` showcase macros. The reusable component logic was extracted to `replicant-component/` in Phase 2. Update requires to reference the new component locations.

**Skip:** `scene/component/chat.cljs` (AI-related, skipped per user request)

### Success Criteria:

#### Automated Verification:
- [ ] All 56 shadcn showcase files exist in `components/scene/src/com/zihao/scene/shadcn/`
- [ ] All component showcase files exist in `components/scene/src/com/zihao/scene/component/`
- [ ] All namespaces are `com.zihao.scene.shadcn.*` or `com.zihao.scene.component.*`
- [ ] No `scene.shadcn` namespaces remain (grep check)

#### Manual Verification:
- [ ] All file names with underscores converted to hyphens for namespaces
- [ ] All require statements updated in migrated files

---

## Phase 4: Update Main scenes.cljs

### Overview
Update the main `scenes.cljs` to include all the new shadcn showcases.

### Changes Required:

#### 1. Update scenes.cljs

**File:** `components/scene/src/com/zihao/scene/scenes.cljs`

**Current requires:**
```clojure
[com.zihao.scene.card :as card]
[com.zihao.scene.chessboard :as chessboard]
```

**Add all shadcn showcase requires:**
```clojure
[com.zihao.scene.shadcn.button]
[com.zihao.scene.shadcn.card]
[com.zihao.scene.shadcn.avatar]
;; ... (all 56 shadcn files)
[com.zihao.scene.component.navbar]
[com.zihao.scene.component.multi-input]
[com.zihao.scene.component.multi-select]
[com.zihao.scene.component.table]
[com.zihao.scene.component.table-filter]
[com.zihao.scene.component.removeable-tags]
```

#### 2. Update portfolio config

Update CSS path and viewport configuration from shadcn-clj's scenes.cljs:
```clojure
{:css-paths ["/portfolio-output.css"]
 :viewport/options
 [{:title "Auto"
   :value {:viewport/width 2560
           :viewport/height 1980}}
  {:title "iPhone 12 / 13 Pro"
   :value {:viewport/width 390
           :viewport/height 844}}]}
```

### Success Criteria:

#### Automated Verification:
- [ ] scenes.cljs has all new requires
- [ ] Code compiles: `/clojure-eval (require 'com.zihao.scene.scenes :reload)`

#### Manual Verification:
- [ ] Portfolio server starts correctly
- [ ] All showcases appear in portfolio UI

---

## Phase 5: Update Tailwind CSS Configuration

### Overview
Merge shadcn-clj's Tailwind CSS configuration into scene component.

### Changes Required:

#### 1. Update input.css

**File:** `components/scene/resources/public/input.css`

**Current content:**
```css
@import "tailwindcss";
@plugin "daisyui";

@source "../../../xiangqi/src/**/*.{clj,cljs,cljc}";
@source "../../../xiangqi/resources/public/**/*.html";
```

**Add:**
```css
@plugin "@tailwindcss/typography";
@source "../../../replicant-component/src/**/*.{clj,cljs,cljc}";
```

#### 2. Update bb.edn

**File:** `bb.edn` (root)

Add/ensure scene-tw task includes the Tailwind typography plugin:
```clojure
scene-tw {:doc "tailwind watch"
          :task (shell "npx @tailwindcss/cli -i ./components/scene/resources/public/input.css -o ./components/scene/resources/public/portfolio-output.css --watch")}
```

### Success Criteria:

#### Automated Verification:
- [ ] input.css contains `@plugin "@tailwindcss/typography"`
- [ ] bb scene-tw task runs without errors

#### Manual Verification:
- [ ] Tailwind compiles successfully
- [ ] DaisyUI classes work in the browser

---

## Phase 6: Final Verification and Cleanup

### Overview
Verify everything works and document cleanup steps for shadcn-clj.

### Changes Required:

#### 1. Run full test

**Commands:**
```bash
# Start Tailwind watch
bb scene-tw

# In another terminal, start portfolio dev server
cd components/scene && npx shadow-cljs watch portfolio
```

#### 2. Manual browser verification

1. Open http://localhost:8000
2. Verify all shadcn components render correctly
3. Check a sample of different component types (button, card, form inputs, etc.)

#### 3. Document shadcn-clj deletion steps

**After verification passes, document these steps for the user to manually execute:**

```bash
# The user should manually delete shadcn-clj after verification:
rm -rf C:/Users/Administrator/Desktop/workspace/work/shadcn-clj

# Or via git if it was a git repo:
# (from a different directory)
git clone --no-checkout <repo-url> temp-dir
cd temp-dir
git rm -r shadcn-clj
git commit -m "Remove shadcn-clj after migration to synapse"
```

### Success Criteria:

#### Automated Verification:
- [ ] Tailwind compiles: `bb scene-tw-once` (create this task first)
- [ ] Shadow-CLJS compiles: `cd components/scene && npx shadow-cljs compile portfolio`
- [ ] No namespace errors in compilation output

#### Manual Verification:
- [ ] Portfolio loads at http://localhost:8000
- [ ] All shadcn component categories visible
- [ ] Sample components render correctly
- [ ] No console errors in browser

**Implementation Note:** After completing this phase and all automated verification passes, pause here for manual confirmation from the human that the manual testing was successful before proceeding to delete shadcn-clj.

---

## Testing Strategy

### Unit Tests:
- Not applicable - these are UI showcase components, tested via visual inspection

### Integration Tests:
- Portfolio serves all scenes without errors
- No namespace conflicts

### Manual Testing Steps:
1. Start dev servers (Tailwind + Shadow-CLJS)
2. Open portfolio in browser
3. Navigate to different component categories
4. Verify visual appearance of sample components:
   - Buttons (different colors, sizes, styles)
   - Cards (basic, with images, with actions)
   - Form inputs (text, select, checkbox, radio)
   - Navigation (navbar, breadcrumbs, pagination)
   - Feedback (toast, alert, loading, progress)
   - Layout (stack, divider, join)

## Performance Considerations

- 56 new showcase files may increase initial compilation time
- Large CSS output from shadcn/daisyui - already 267KB in shadcn-clj
- Consider code splitting if portfolio becomes too large

## Migration Notes

### Namespace Mapping Reference

| shadcn-clj | synapse |
|------------|---------|
| `scene.shadcn.*` | `com.zihao.scene.shadcn.*` |
| `scene.component.*` | `com.zihao.scene.component.*` |
| `component.*` | `com.zihao.replicant-component.component.*` |
| (no ns in ui/*) | `com.zihao.replicant-component.ui.*` |

### File Extension Changes
- All `.cljs` files → `.cljs` (keep as is in scene/ component)
- All `.cljs` → `.cljc` for replicant-component/ (for Clojure compatibility)

### Dependencies Added
No new dependencies needed - shadcn-clj uses:
- portfolio (already in scene/deps.edn)
- replicant (already available)
- Tailwind CSS + DaisyUI (already configured)

### Files NOT Migrated
- `src/scene/ai/` - entire directory (AI chat scenes)
- `src/component/ai/` - entire directory (AI components)
- `src/component/chat/` - entire directory (chat core)
- `src/scene/component/chat.cljs` - AI chat showcase
- TipTap-related JS files (tiptap_web_component.js)
- AI action files

## References

- Source project: `C:\Users\Administrator\Desktop\workspace\work\shadcn-clj`
- Target replicant-component: `components/replicant-component/`
- Target scene: `components/scene/`
- Related scene documentation: `components/scene/README.md` (if exists)
