from pathlib import Path
from pdf2image import convert_from_path
out=Path('tmp/report/combined-render')
images=convert_from_path(str(out/'GreenFarm_Chuong_1_2.pdf'),dpi=90,poppler_path=r'C:\Users\LEGION\.cache\codex-runtimes\codex-primary-runtime\dependencies\native\poppler\Library\bin')
for i,im in enumerate(images,1):im.save(out/f'page-{i}.png')
print('Rendered',len(images),'pages')
