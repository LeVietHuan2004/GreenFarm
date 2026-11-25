from pathlib import Path
from pypdf import PdfReader
from PIL import Image,ImageChops,ImageDraw
import json
root=Path('tmp/chapters456');p=root/'final-render-v1';pdf=PdfReader(p/'GreenFarm_BaoCao_Chuong_1_6.pdf')
print('FINAL PAGES',len(pdf.pages))
texts=[x.extract_text() for x in pdf.pages]
(root/'final-text.txt').write_text('\n'.join(f'--- PAGE {i+1} ---\n'+t for i,t in enumerate(texts)),encoding='utf8')
for i,t in enumerate(texts):
 if any(s in t for s in ['CHƯƠNG 4 KẾT','CHƯƠNG 5 KIỂM','CHƯƠNG 6 KẾT']):print('CHAPTER PAGE',i+1,t[:120])
changed=[]
for i in range(1,79):
 a=Image.open(root/f'reference-render/page-{i}.png').convert('RGB');b=Image.open(p/f'page-{i}.png').convert('RGB')
 if a.size!=b.size or ImageChops.difference(a,b).getbbox():changed.append(i)
print('ORIGINAL PAGE CHANGES',changed)
(root/'page-fidelity.json').write_text(json.dumps({'pages':len(pdf.pages),'original_page_changes':changed}),encoding='utf8')
for start in range(79,len(pdf.pages)+1,2):
 canvas=Image.new('RGB',(1700,1130),'#bfbfbf');d=ImageDraw.Draw(canvas)
 for j,i in enumerate(range(start,min(start+2,len(pdf.pages)+1))):
  im=Image.open(p/f'page-{i}.png').convert('RGB');canvas.paste(im,(j*850,25));d.text((j*850+8,5),f'page-{i}',fill='black')
 canvas.save(p/f'new-{start}.jpg')
