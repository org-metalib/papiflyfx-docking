### 3–5 step incremental plan to improve DnD UX (minimal refactors first)

#### 1) Stabilize hover + hint rendering (no behavioral redesign yet)
- **Goal:** eliminate flicker/jitter and make the drop hint feel “locked” to the target.
- **Changes:**
    - In `org.metalib.papifly.fx.docks.drag.DragManager`, add a small hover stabilizer:
        - only update the active `HitTestResult` when it actually changes (target element or zone)
        - optionally add a tiny hysteresis/debounce (e.g., 30–60 ms) so micro-mouse movements don’t cause constant zone flipping.
    - In `org.metalib.papifly.fx.docks.render.OverlayCanvas`, ensure redraw is conditional:
        - redraw only when `HitTestResult` changes (or when drag starts/ends)
- **UX outcome:** smoother hint tracking, less visual noise.
- **Verification:** manual: drag slowly across boundaries and around split dividers; ensure hint doesn’t flicker.

#### 2) Make hit-testing output richer (small API extension, big UX payoff)
- **Goal:** enable better hints without changing layout logic.
- **Changes:**
    - Extend `HitTestResult` to carry:
        - `Bounds targetBounds` (in overlay/docking-layer coordinates)
        - `DropZone zone` (already exists conceptually)
        - optionally `double zoneConfidence` or a simple `boolean isNearEdge`.
    - Ensure `HitTester` is the single source of truth for:
        - “which element is under the mouse?”
        - “which zone (N/S/E/W/C)?”
- **UX outcome:** Overlay can draw consistent rectangles exactly aligned to the target, and you can refine zone logic later without touching rendering.
- **Verification:** add a tiny debug mode (temporary `System.out.println`) or overlay labels to confirm bounds/zone are consistent.

#### 3) Improve the zone selection model (edge bias + divider awareness)
- **Goal:** make it easier to intentionally choose split vs tab-add, especially near edges.
- **Changes:**
    - In `HitTester`, replace the naive “3x3 equal grid” with an edge-biased approach:
        - define an “edge band” thickness like `max(24px, min(width,height) * 0.18)`
        - if cursor is in edge band → N/S/E/W
        - else → CENTER
    - Add divider awareness:
        - if hovering near an existing split divider, prefer CENTER (tab-add) or keep current zone stable to avoid toggling.
- **UX outcome:** predictable drops; users don’t accidentally split when aiming for center.
- **Verification:** manual: try to tab-drop in the middle repeatedly; try edge-splits; confirm intent is matched.

#### 4) Add UX polish: cursor/preview + clearer center hint
- **Goal:** communicate “what will happen on drop” clearly.
- **Changes:**
    - Overlay:
        - draw CENTER hint distinct from split hints (e.g., a smaller inset rectangle or a tab-strip highlight region)
        - optionally draw a lightweight preview title (tab text) near the cursor.
    - Cursor feedback:
        - set cursor to `MOVE` while dragging; maybe `COPY`-style when modifier key pressed (future).
- **UX outcome:** fewer surprises; feels more “IDE-like”.
- **Verification:** manual: users can identify tab-add vs split instantly.

#### 5) Correctness + UX: handle “same-group reorder” and no-op drops
- **Goal:** make common interactions feel complete.
- **Changes:**
    - If dragging a tab within the same `DockTabGroup`:
        - treat CENTER zone as reorder (compute insertion index from mouse X)
    - If dropping on itself / same slot:
        - no-op (keep state, don’t rebuild tree)
    - Add minimal size guards when creating splits (avoid creating unusable tiny panes).
- **UX outcome:** tab reordering works; fewer accidental layout churns.
- **Verification:** manual: reorder tabs; drop onto same position; split creation respects minimum sizes.

---

### Suggested order of work (why this is “minimal refactors first”)
- Steps 1–2 are mostly about reducing redraw churn and making data flow explicit (`HitTester` → `HitTestResult` → `OverlayCanvas`). They typically don’t change layout mutation code.
- Step 3 changes the *feel* dramatically while still staying inside hit-testing.
- Steps 4–5 are UX completeness/polish once the core feels stable.

If you want, I can map each step to the exact methods in `DragManager`, `HitTester`, and `OverlayCanvas` once you confirm whether your current zone logic is already 3x3 (per spec) or something slightly different in code.