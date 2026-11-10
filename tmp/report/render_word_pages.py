from pdf2image import convert_from_path
from pathlib import Path
out=Path('tmp/report/word-render')
images=convert_from_path(str(out/'GreenFarm_Chuong_2.pdf'),dpi=95,poppler_path=r'C:\Users\LEGION\.cache\codex-runtimes\codex-primary-runtime\dependencies\native\poppler\Library\bin')
for i,im in enumerate(images,1):im.save(out/f'page-{i}.png')
print('Rendered pages:',len(images))
