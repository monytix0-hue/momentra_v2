/**
 * Phase 2/3 artifact index — booklet HTML, chapter SVG pack, and video reel spec
 * are produced by `modules/story/service.ts` on every Story generation.
 *
 * Artifact types:
 * - CHAPTER_HTML — interactive web / in-app WebView source
 * - BOOKLET_HTML — print-friendly light theme (download + Print to PDF)
 * - CHAPTER_PNG_SVG — JSON map of chapterId → SVG (social image pack)
 * - SHARE_COVER — cover SVG for WhatsApp/email pack
 * - VIDEO_REEL_SPEC — 15–30s reel composition spec (encode worker can consume later)
 *
 * Logo: `design/momentra-official-logo.png`
 */
export const MOMENT_STORY_ARTIFACT_TYPES = [
  'STORY_PAYLOAD',
  'CHAPTER_HTML',
  'CHAPTER_PNG_SVG',
  'BOOKLET_HTML',
  'SHARE_COVER',
  'VIDEO_REEL_SPEC',
] as const;
