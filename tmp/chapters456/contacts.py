from pathlib import Path
from PIL import Image,ImageOps,ImageDraw
p=Path('tmp/chapters456/reference-render');files=sorted(p.glob('page-*.png'),key=lambda x:int(x.stem.split('-')[-1]))
for start in range(0,len(files),6):
 canvas=Image.new('RGB',(2550,2260),'#bfbfbf');d=ImageDraw.Draw(canvas)
 for j,f in enumerate(files[start:start+6]):
  im=Image.open(f).convert('RGB');im.thumbnail((850,1100));x=(j%3)*850;y=(j//3)*1130;canvas.paste(im,(x,y+25));d.text((x+8,y+5),f.stem,fill='black')
 canvas.save(p/f'contact-{start//6+1}.jpg')
print('Reference contact sheets',len(list(p.glob('contact-*.jpg'))))
