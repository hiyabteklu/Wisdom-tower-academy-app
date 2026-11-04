"use client";

/**
 * Notifications v2, slice R3-b1 (session R3-B). `[~]` NOT compiled, not opened in a browser.
 *
 * `NotificationBlockEditor`: the admin writes a notice as blocks (title, subtitle, paragraph, callout,
 * bullets, divider) with Bold and Highlight buttons, and `NotificationPreview` shows it in the gold look
 * the Android reading view uses (colours copied from NotificationReaderScreen.kt: gold #FCD34D, subtitle
 * 75% gold, body #E2E8F0, highlight = 90% gold background with dark text, callout = glass card with a
 * gold left bar, divider = thin gold gradient). The two glass colours and the dark text colour are close
 * approximations (the app uses theme tokens), so the preview is a guide, not a pixel match.
 *
 * 2026-10-15 (Admin mobile redesign, slice A6 part 2): LAYOUT ONLY (3-column add grid, 2-column Bold /
 * Highlight grid, 44 px targets). Editing logic is unchanged.
 *
 * 2026-10-16 (Admin UI V2, slice U4 part 3b): LAYOUT ONLY. The editing controls use the kit look (kit card per
 * block, kit textarea, kit pill colours, delete in the danger colour). The PREVIEW is untouched on purpose: it
 * must keep the gold look of the Android reading view. Editing logic and exports are unchanged.
 */

import { useRef, type CSSProperties } from "react";
import {
  editorToBlocks,
  newEditorBlock,
  type EditorBlock,
  type EditorBlockType,
} from "@/lib/notification-authoring";
import type { ContentBlock, ContentRun } from "@/lib/notification-v2";
import { AdminCard, AdminTextarea } from "./ui";

const GOLD = "#FCD34D";
const GOLD_SOFT = "rgba(252, 211, 77, 0.75)";
const BODY = "#E2E8F0";
const DARK_ON_GOLD = "#0B1220";

const ADD_BUTTONS: { t: EditorBlockType; label: string }[] = [
  { t: "title", label: "Title" },
  { t: "subtitle", label: "Subtitle" },
  { t: "p", label: "Paragraph" },
  { t: "callout", label: "Callout" },
  { t: "list", label: "Bullets" },
  { t: "divider", label: "Divider" },
];

const BLOCK_LABEL: Record<EditorBlockType, string> = {
  title: "Title",
  subtitle: "Subtitle",
  p: "Paragraph",
  callout: "Callout",
  list: "Bullets (one per line)",
  divider: "Divider",
};

const SMALL_BUTTON =
  "min-h-[44px] min-w-[44px] px-3 grid place-items-center rounded-full text-xs font-semibold cursor-pointer disabled:opacity-30";

const KIT_PILL =
  "min-h-[44px] rounded-full border border-white/10 bg-wisdom-card px-2 text-sm font-semibold text-wisdom-muted active:bg-white/10 cursor-pointer";

export function NotificationBlockEditor({
  value,
  onChange,
}: {
  value: EditorBlock[];
  onChange: (next: EditorBlock[]) => void;
}) {
  const activeEl = useRef<HTMLTextAreaElement | null>(null);
  const activeId = useRef<string | null>(null);

  const update = (id: string, text: string) =>
    onChange(value.map((b) => (b.id === id ? { ...b, text } : b)));

  const move = (index: number, delta: number) => {
    const target = index + delta;
    if (target < 0 || target >= value.length) return;
    const next = value.slice();
    const [item] = next.splice(index, 1);
    next.splice(target, 0, item);
    onChange(next);
  };

  const remove = (id: string) => onChange(value.filter((b) => b.id !== id));

  const wrap = (marker: string) => {
    const el = activeEl.current;
    const id = activeId.current;
    if (!el || !id) return;
    const start = el.selectionStart;
    const end = el.selectionEnd;
    if (start === end) return;
    const text = el.value;
    update(id, text.slice(0, start) + marker + text.slice(start, end) + marker + text.slice(end));
    requestAnimationFrame(() => {
      el.focus();
      el.setSelectionRange(start, end + marker.length * 2);
    });
  };

  const keepFocus = (e: React.MouseEvent) => e.preventDefault();

  return (
    <div className="space-y-3">
      <div className="space-y-1.5">
        <p className="text-xs text-wisdom-muted">Add a block:</p>
        <div className="grid grid-cols-3 gap-2">
          {ADD_BUTTONS.map((b) => (
            <button
              key={b.t}
              type="button"
              onClick={() => onChange([...value, newEditorBlock(b.t)])}
              className={KIT_PILL}
            >
              + {b.label}
            </button>
          ))}
        </div>
      </div>

      <div className="space-y-1.5">
        <p className="text-xs text-wisdom-muted">Select text in a box, then:</p>
        <div className="grid grid-cols-2 gap-2">
          <button
            type="button"
            onMouseDown={keepFocus}
            onClick={() => wrap("**")}
            className="min-h-[44px] cursor-pointer rounded-full border border-white/10 bg-wisdom-card px-3 text-sm font-bold text-white active:bg-white/10"
          >
            Bold
          </button>
          <button
            type="button"
            onMouseDown={keepFocus}
            onClick={() => wrap("==")}
            className="min-h-[44px] cursor-pointer rounded-full px-3 text-sm font-bold active:opacity-80"
            style={{ background: GOLD, color: DARK_ON_GOLD }}
          >
            Highlight
          </button>
        </div>
        <p className="text-xs text-slate-500">
          (or type **bold** and ==highlight== yourself)
        </p>
      </div>

      {value.length === 0 && (
        <p className="rounded-2xl border border-dashed border-white/15 p-3 text-sm text-wisdom-muted">
          Nothing yet. Add a Title, then a Paragraph.
        </p>
      )}

      {value.map((block, index) => (
        <AdminCard key={block.id}>
          <div className="mb-1 flex flex-wrap items-center justify-between gap-x-2">
            <span
              className="min-w-0 text-[11px] font-semibold uppercase tracking-wide"
              style={{ color: GOLD_SOFT }}
            >
              {BLOCK_LABEL[block.t]}
            </span>
            <div className="flex items-center">
              <button
                type="button"
                onClick={() => move(index, -1)}
                disabled={index === 0}
                aria-label="Move up"
                className={`${SMALL_BUTTON} text-slate-300 active:bg-white/10`}
              >
                ↑
              </button>
              <button
                type="button"
                onClick={() => move(index, 1)}
                disabled={index === value.length - 1}
                aria-label="Move down"
                className={`${SMALL_BUTTON} text-slate-300 active:bg-white/10`}
              >
                ↓
              </button>
              <button
                type="button"
                onClick={() => remove(block.id)}
                aria-label="Delete block"
                className={`${SMALL_BUTTON} text-rose-300 active:bg-rose-500/10`}
              >
                Delete
              </button>
            </div>
          </div>

          {block.t === "divider" ? (
            <div
              style={{
                height: 1,
                margin: "8px 0",
                background: `linear-gradient(90deg, transparent, ${GOLD}, transparent)`,
              }}
            />
          ) : (
            <AdminTextarea
              value={block.text}
              rows={block.t === "title" || block.t === "subtitle" ? 1 : 3}
              onFocus={(e) => {
                activeEl.current = e.currentTarget;
                activeId.current = block.id;
              }}
              onChange={(e) => update(block.id, e.target.value)}
              className="resize-y"
              placeholder={
                block.t === "list" ? "One bullet per line" : "Write here"
              }
            />
          )}
        </AdminCard>
      ))}
    </div>
  );
}

function Runs({ runs }: { runs: ContentRun[] }) {
  return (
    <>
      {runs.map((run, i) => {
        const style: CSSProperties = {};
        if (run.hl) {
          style.background = "rgba(252, 211, 77, 0.9)";
          style.color = DARK_ON_GOLD;
          style.fontWeight = run.b ? 800 : 600;
          style.borderRadius = 3;
          style.padding = "0 2px";
        } else if (run.b) {
          style.color = "#FFFFFF";
          style.fontWeight = 800;
        }
        return (
          <span key={i} style={style}>
            {run.text}
          </span>
        );
      })}
    </>
  );
}

function BlockPreview({ block }: { block: ContentBlock }) {
  switch (block.t) {
    case "title":
      return (
        <h3 style={{ color: GOLD, fontSize: 22, lineHeight: 1.25, fontWeight: 800, margin: "10px 0 4px" }}>
          {block.text}
        </h3>
      );
    case "subtitle":
      return (
        <p style={{ color: GOLD_SOFT, fontSize: 15, lineHeight: 1.4, fontWeight: 600, margin: "4px 0 8px" }}>
          {block.text}
        </p>
      );
    case "p":
      return (
        <p style={{ color: BODY, fontSize: 14, lineHeight: 1.6, margin: "6px 0", whiteSpace: "pre-wrap" }}>
          <Runs runs={block.runs} />
        </p>
      );
    case "callout":
      return (
        <div
          style={{
            display: "flex",
            margin: "8px 0",
            borderRadius: 14,
            overflow: "hidden",
            background: "rgba(255,255,255,0.06)",
            border: "1px solid rgba(255,255,255,0.12)",
          }}
        >
          <div style={{ width: 4, background: GOLD, flexShrink: 0 }} />
          <p style={{ color: BODY, fontSize: 14, lineHeight: 1.55, margin: 0, padding: 14, whiteSpace: "pre-wrap" }}>
            <Runs runs={block.runs} />
          </p>
        </div>
      );
    case "list":
      return (
        <ul style={{ listStyle: "none", margin: "6px 0", padding: 0 }}>
          {block.items.map((runs, i) => (
            <li key={i} style={{ display: "flex", gap: 10, color: BODY, fontSize: 14, lineHeight: 1.55, padding: "3px 0" }}>
              <span style={{ color: GOLD, fontWeight: 700 }}>•</span>
              <span style={{ flex: 1 }}>
                <Runs runs={runs} />
              </span>
            </li>
          ))}
        </ul>
      );
    case "divider":
      return (
        <div
          style={{
            height: 1,
            margin: "14px 0",
            background: `linear-gradient(90deg, transparent, rgba(252,211,77,0.8), transparent)`,
          }}
        />
      );
  }
}

/** The reading view as the student will see it (blocks as the admin typed them). */
export function NotificationPreview({ blocks }: { blocks: EditorBlock[] }) {
  const list = editorToBlocks(blocks);
  return (
    <div
      className="rounded-2xl border border-white/10 p-3"
      style={{ background: "#0B1220", maxWidth: 380, overflowWrap: "anywhere" }}
    >
      {list.length === 0 ? (
        <p className="text-sm text-slate-500">The preview appears as you type.</p>
      ) : (
        list.map((block, i) => <BlockPreview key={i} block={block} />)
      )}
    </div>
  );
}
