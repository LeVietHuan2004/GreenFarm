from pathlib import Path
p=Path('tmp/report/build_word.py');s=p.read_text(encoding='utf-8-sig');s=s.replace("st.element.get_or_add_rPr().rFonts.set(qn('w:eastAsia'),'Times New Roman')","rf=st.element.get_or_add_rPr().rFonts\n for a in ['asciiTheme','hAnsiTheme','eastAsiaTheme','cstheme']:rf.attrib.pop(qn('w:'+a),None)\n for a in ['ascii','hAnsi','eastAsia','cs']:rf.set(qn('w:'+a),'Times New Roman')")
s=s.replace("cap.font.italic=True","cap.font.italic=True;cap.font.bold=False")
p.write_text(s,encoding='utf-8')
