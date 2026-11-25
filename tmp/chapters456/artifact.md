# GreenFarm continuation contract

Reference: C:/Users/LEGION/Videos/CHƯƠNG 1.docx
SHA256: 848c5114c4e98cbca86c09891da53615907441095cf4ad729f5ef1267a5ec75d
Reference render: reference.pdf and reference-render/page-1.png through page-78.png.
Source inventory: source_inventory.json. Scope is an appended continuation, chapters 4 to 6.

The source is the authoritative package, including cover text boxes, images, diagrams,
tables, relationships, bookmarks, styles and numbering. Preserve every existing body
child and the final section properties. Add new body elements immediately before the
terminal sectPr. All ZIP parts except word/document.xml are preserve-only. No fields
were found in the main document. Do not rebuild the package with python-docx or save
it through another editor. The supplied source remains unchanged.

Page system: one section, portrait Letter 12240 by 15840 twips; 1440-twip margins on
all sides; header and footer offsets 720 twips. No new headers or footers. New chapters
start with pageBreakBefore. Retain the original section size and geometry.

Source roles: chapter title body child 301, Title, 16 pt bold centered, Times New Roman;
body child 302, BodyText, 13 pt justified, first indent 283 twips, after 100 twips,
line 360 auto; section child 303, Heading2, 14 pt bold; subsection child 304,
ListParagraph, 13 pt. New elements clone the paragraph properties and explicitly set
font, black color, size, spacing, indentation and keep behavior. Add outline levels
0/1/2 to new chapter/section/subsection paragraphs only. Preserve shared styles.

Caption role: body child 531, centered 12 pt italic, line 276 auto, no first indent.
User supplies screenshots later. Add an ordinary editable paragraph saying
[Chèn ảnh kết quả tại đây], kept with the numbered caption. Do not create screenshots
or imply screenshots have already been captured. Eighteen numbered figure slots.

Tables: six new tables, fixed total width 9360 twips within the original text area;
unequal column grids; 11.5 pt Times New Roman; 276 line spacing; 75-twip top/bottom
and 90-twip side padding; vertical centering; no fixed heights or split rows;
repeat header rows. Gray header with black bold text, visible D9D9D9 grid.

Content authority: supplied PDF defines the chapter progression: implemented results,
testing/evaluation, conclusion/limitations/future work. GreenFarm code, UI routes and
local API documentation define functionality. Public URL confirmed by user and HTTP
200 read. Do not copy TopikLearn functionality or results into GreenFarm.

Evidence: backend-tests.log and test-summary.json, 161 tests, 158 passed, three skipped;
frontend-quality.log with exit 0 for lint/build. Manual test matrix has expected results,
not invented public acceptance results. GHN staging and external integration limits
are explicit. Do not include configuration secrets or claim unmeasured performance.

Render method: runtime.json confirms no bundled LibreOffice on Windows. Export a
read-only copy through native Word, then use the current packaged render_docx.py
rasterizer with a native-Word PDF conversion adapter. Only the DOCX is a deliverable.

Gates: source hash unchanged, only document.xml changed, original body C14N comparison
passes, first 78 pages pixel-identical to the source, all appended pages visually
reviewed at their rendered resolution, figure and table numbering sequential, test
counts consistent, no old-project content introduced. Existing source formatting
issues outside the requested chapters are preserved rather than silently rewritten.
