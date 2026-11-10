"""Use the packaged DOCX rasterizer with native Word as its PDF converter."""
from pathlib import Path
import importlib.util,shutil,sys,subprocess,os
rt=Path(r'C:/Users/LEGION/.cache/codex-runtimes/codex-primary-runtime/dependencies')
os.environ['PATH']=str(rt/'python')+os.pathsep+str(rt/'native/poppler/Library/bin')+os.pathsep+os.environ['PATH']
skill=Path(r'C:/Users/LEGION/.codex/plugins/cache/openai-primary-runtime/documents/26.1030.10813/skills/documents')
spec=importlib.util.spec_from_file_location('packaged_render',skill/'render_docx.py')
mod=importlib.util.module_from_spec(spec);spec.loader.exec_module(mod)
source=Path(sys.argv[1]).resolve();out=Path(sys.argv[2]).resolve();out.mkdir(parents=True,exist_ok=True)
cached=out/(source.stem+'.pdf')
if not cached.exists():
 subprocess.run(['powershell','-NoProfile','-File',str(Path(__file__).with_name('export_word.ps1')),'-InputDocx',str(source),'-OutputPdf',str(cached)],check=True)
def word_pdf(input_docx,lo_profile_dir,convert_tmp_dir,*args,**kwargs):
 dest=Path(convert_tmp_dir)/(Path(input_docx).stem+'.pdf');shutil.copy2(cached,dest)
 return str(dest),'Native Microsoft Word PDF export used because LibreOffice is unavailable.'
mod.convert_to_pdf=word_pdf
sys.argv=['render_docx.py',str(source),'--output_dir',str(out),'--emit_pdf','--dpi','110']
mod.main()
