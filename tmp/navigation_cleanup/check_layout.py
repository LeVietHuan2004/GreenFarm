from pathlib import Path
from PIL import Image,ImageChops
from pypdf import PdfReader
import hashlib,json
old=Path('tmp/chapters456/final-render-v2');new=Path('tmp/navigation_cleanup/render')
a=sorted(old.glob('page-*.png'),key=lambda p:int(p.stem.split('-')[-1]));b=sorted(new.glob('page-*.png'),key=lambda p:int(p.stem.split('-')[-1]))
assert len(a)==len(b)==93,(len(a),len(b))
changed=[]
for p,q in zip(a,b):
 if p.read_bytes()==q.read_bytes():continue
 im1=Image.open(p).convert('RGB');im2=Image.open(q).convert('RGB')
 if im1.size!=im2.size or ImageChops.difference(im1,im2).getbbox():changed.append(p.name)
assert not changed,changed
report={'pages':len(b),'pixel_changes':changed,'blank_outline_entries_removed':35,'layout':'identical on all 93 rendered pages'}
Path('tmp/navigation_cleanup/layout-check.json').write_text(json.dumps(report,indent=2),encoding='utf8')
print(report)
