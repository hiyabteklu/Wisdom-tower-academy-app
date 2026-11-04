/** Step-by-step admin navigation mirroring the public Academy structure. */

import { freshmanSubjects } from "@/data/freshman";
import { grades, resourceHubs } from "@/data/academy";
import { subjectsForGrade } from "@/data/grade-subjects";
import { remedialSubjects } from "@/data/remedial";
import { specialPackages } from "@/data/special-packages";
import { packageIdForGrade } from "@/data/packages";
import {
  ENTRANCE_PACKAGE_ID,
  ENTRANCE_STREAMS,
  ENTRANCE_YEARS,
  entranceScope,
} from "@/data/entrance-questions";

export type AdminNavNode = {
  id: string;
  label: string;
  packageId?: string;
  scopePath?: string;
  /** When true, file uploads for this branch go to Appwrite instead of Supabase storage */
  useAppwrite?: boolean;
  children?: AdminNavNode[];
};

const gradeNodes: AdminNavNode[] = grades.map((g) => {
  const subjects = subjectsForGrade(g.id);
  const packageId = packageIdForGrade(g.id);

  return {
    id: `grade-${g.id}`,
    label: g.label,
    packageId,
    useAppwrite: true,
    children: subjects.map((sub) => ({
      id: sub.id,
      label: sub.name,
      packageId,
      scopePath: `grade/${g.id}/${sub.id}`,
      useAppwrite: true,
      children: resourceHubs.map((h) => ({
        id: h.id,
        label: h.name,
        packageId,
        scopePath: `grade/${g.id}/${sub.id}`,
        useAppwrite: true,
      })),
    })),
  };
});

/**
 * Entrance Questions: stream > subject > year > Exams.
 * Scope = entrance/{stream}/{subject}/{year}, hub = exams (read by the public year page).
 */
const entranceNode: AdminNavNode = {
  id: "entrance-questions",
  label: "EUEE · Entrance Questions",
  packageId: ENTRANCE_PACKAGE_ID,
  useAppwrite: true,
  children: ENTRANCE_STREAMS.map((stream) => ({
    id: `entrance-${stream.id}`,
    label: stream.label,
    packageId: ENTRANCE_PACKAGE_ID,
    useAppwrite: true,
    children: stream.subjects.map((sub) => ({
      id: `${stream.id}-${sub.id}`,
      label: sub.name,
      packageId: ENTRANCE_PACKAGE_ID,
      useAppwrite: true,
      children: ENTRANCE_YEARS.map((year) => {
        const scopePath = entranceScope(stream.id, sub.id, year);
        return {
          id: `${stream.id}-${sub.id}-${year}`,
          label: year,
          packageId: ENTRANCE_PACKAGE_ID,
          scopePath,
          useAppwrite: true,
          children: resourceHubs
            .filter((h) => h.id === "exams")
            .map((h) => ({
              id: h.id,
              label: h.name,
              packageId: ENTRANCE_PACKAGE_ID,
              scopePath,
              useAppwrite: true,
            })),
        };
      }),
    })),
  })),
};

const remedialNode: AdminNavNode = {
  id: "remedial",
  label: "Remedial",
  packageId: "remedial",
  useAppwrite: true,
  children: remedialSubjects.map((subject) => ({
    id: subject.id,
    label: subject.name,
    packageId: "remedial",
    scopePath: `remedial/${subject.id}`,
    useAppwrite: true,
    children: resourceHubs.map((hub) => ({
      id: hub.id,
      label: hub.name,
      packageId: "remedial",
      scopePath: `remedial/${subject.id}`,
      useAppwrite: true,
    })),
  })),
};

export const ADMIN_CONTENT_TREE: AdminNavNode[] = [
  {
    id: "grades",
    label: "Grades 9–12",
    useAppwrite: true,
    children: gradeNodes,
  },
  entranceNode,
  {
    id: "freshman",
    label: "Freshman",
    packageId: "freshman",
    useAppwrite: true,
    children: freshmanSubjects.map((s) => ({
      id: s.id,
      label: s.name,
      packageId: "freshman",
      scopePath: `freshman/${s.id}`,
      useAppwrite: true,
      children: resourceHubs.map((h) => ({
        id: h.id,
        label: h.name,
        packageId: "freshman",
        scopePath: `freshman/${s.id}`,
        useAppwrite: true,
      })),
    })),
  },
  remedialNode,
  {
    id: "special",
    label: "Special packages",
    useAppwrite: true,
    children: specialPackages.flatMap((pkg) =>
      pkg.semesters.map((sem) => ({
        id: `${pkg.slug}-${sem.id}`,
        label: `${pkg.name} · ${sem.shortLabel}`,
        packageId: sem.packageId,
        useAppwrite: true,
        children: sem.courses.map((c) => ({
          id: c.slug,
          label: `${c.code} · ${c.title}`,
          packageId: sem.packageId,
          scopePath: `ece/${sem.id}/${c.slug}`,
          useAppwrite: true,
          children: resourceHubs.map((h) => ({
            id: h.id,
            label: h.name,
            packageId: sem.packageId,
            scopePath: `ece/${sem.id}/${c.slug}`,
            useAppwrite: true,
          })),
        })),
      }))
    ),
  },
];

export const HUB_CONTENT_DEFAULTS: Record<
  string,
  { contentType: string; hint: string }
> = {
  books: {
    contentType: "pdf",
    hint: "Upload a PDF (Appwrite). Large files: paste File ID from Appwrite Console.",
  },
  "short-notes": {
    contentType: "markdown",
    hint: "Markdown notes in the text box (Amharic OK). Images: paste image URLs in markdown. No file upload required.",
  },
  videos: {
    contentType: "video_url",
    hint: "Paste a YouTube/Vimeo URL. Optional file upload goes to Appwrite.",
  },
  flashcards: {
    contentType: "flashcard_deck",
    hint: "JSON in meta only (stored in Supabase DB, not Appwrite). Example: { cards: [{front, back}] }.",
  },
  "question-banks": {
    contentType: "quiz",
    hint: "JSON in meta only (Supabase DB). Example: { questions: [{prompt, choices, correct, solution}] }.",
  },
  exams: {
    contentType: "exam",
    hint: "JSON in meta only (Supabase DB). For rich solutions with images, put markdown image URLs in solution text.",
  },
  "life-savers": {
    contentType: "pdf",
    hint: "Upload a PDF (Appwrite). Large files: paste File ID from Appwrite Console.",
  },
};

/**
 * Learning file uploads always go to Appwrite.
 * Any new scope path (new subject, package, or hub) is covered automatically.
 * Metadata (titles, JSON quizzes, markdown notes) stays in Supabase tables.
 */
export function scopeUsesAppwrite(scopePath?: string | null): boolean {
  // Default ON for every learning scope so future packages need no rewire.
  return Boolean(scopePath && scopePath.trim().length > 0);
}
