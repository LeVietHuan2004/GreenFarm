from pathlib import Path
p=Path('tmp/report/build_combined.py');s=p.read_text(encoding='utf-8')
s=s.replace("para=d.add_paragraph(p[2:],'List Bullet');para.paragraph_format.first_line_indent=Pt(-10);para.paragraph_format.left_indent=Pt(22)","para=d.add_paragraph('• '+p[2:]);para.paragraph_format.first_line_indent=Pt(-10);para.paragraph_format.left_indent=Pt(22);para.paragraph_format.space_after=Pt(0)")
p.write_text(s,encoding='utf-8')
