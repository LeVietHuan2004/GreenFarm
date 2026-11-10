# GreenFarm chapter 3 execution contract

Reference retained unchanged: C:/Users/LEGION/Downloads/PhanVanTinh_22010122_in.docx
SHA256: 206519fb83722f480faab4337c2e6f4c3f0eb399a180caa5016f4a73eb038ede
Evidence: reference_structure.txt, reference_evidence.txt, reference_chapter3.txt,
reference_package.json and reference-render. The source has many reconstructed
sections and manual line-spacing/character-spacing artifacts from its conversion.
The requested scope is a standalone GreenFarm chapter, following the chapter 3
structure and table/diagram conventions, not an edit to other chapters.

Editable slot: word/document.xml / w:body children 558 through 1088 (chapter 3).
All other body content is intentionally removed from the working copy because
the deliverable contains chapter 3 only. Source media are reference-only and
replaced by newly drawn GreenFarm figures. All other package parts, relationships,
styles, numbering, and footer furniture are preserved byte for byte where not
explicitly adjusted in the document body. The copied source ZIP is the base.

Page system: use the normal chapter's A4 portrait single-column pattern, width
11910 and height 16840 twips; left 1440, right 720, top 1100, bottom 880 twips;
footer 681 twips. Remove reconstructed multi-column section breaks belonging to
the old report. Footer: centered page number, reset to 1 for the standalone chapter.

Typography: Times New Roman 13 pt black; justified body, first indent 0.5 cm,
1.5-line spacing. Title centered bold 15 pt; section headings bold 13 pt;
subheadings 13 pt. Use actual source paragraph/table style IDs (Heading1,
Heading2, ListParagraph, BodyText, TableParagraph) with explicit clean properties
to avoid inherited extraction artifacts. Literal section numbers avoid retaining
the old report's chapter-specific numbering. Keep headings with following text.

Tables: source-derived black thin grid, white header bold, two-column use case
attribute/content specifications, caption above in centered italic 12 pt.
Other matrices use deliberate unequal widths. Body 11.5 or 12 pt, single line
spacing, padding 90 twips. Repeat header rows, never split a row across pages.
Long matrices may span pages, with repeated header. Caption kept with first row.

Figures: black/white UML and architecture figures; stick-figure actors, ellipse
use cases within system boundary, labeled lifelines, dashed returns, and boxed
ERD entities. Caption centered below, Times New Roman 12 pt italic. New diagrams
describe only implemented GreenFarm modules, grouped to keep the chapter concise.
Diagram sources and PNGs are task-local authoring assets, not unrelated artwork.

Ordered content: 3.1 scope and actors; 3.2 functional/nonfunctional requirements
and business rules; 3.3 use cases with compact attribute tables and principal
sequence diagrams; 3.4 architecture, components, Railway/Vercel deployment and AI;
3.5 conceptual/physical database, grouped ERDs and key-field dictionary;
3.6 API conventions, errors, endpoint groups and authorization;
3.7 interface navigation, screen catalog, wireframes and interactions.

Scope truth: code and migrations through V25 are primary evidence. Include
password recovery, provenance, guest commerce, stock batches/FEFO, rewards,
refunds, GHN staging and VNPAY sandbox. Railway/Vercel is the user-specified target
deployment scenario, written as the report's deployment model, without invented
domains, regions, measurements, screenshots, or claims of production acceptance.
All 36 SQL tables are inventoried, including unused legacy password_reset_tokens;
flyway_schema_history is technical metadata excluded from that count.

Fidelity gates: preserve retained source hash; compare preserved ZIP part hashes;
visually inspect all final rendered pages; verify figure/table numbering, all use
case IDs, all endpoint paths against controller inventory, all database table
names against migrations; remove old-project prose from final document body.
Native Word exports PDF because packaged renderer fails for missing soffice.exe;
use packaged render_docx.py rasterizer with the native PDF conversion adapter.
