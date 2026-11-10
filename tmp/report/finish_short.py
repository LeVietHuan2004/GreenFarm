from pathlib import Path
p=Path('tmp/report/build_report.py');s=p.read_text(encoding='utf-8');s=s.replace("if 'Tài liệu tham khảo' in p:story.append(PageBreak())","if 'Tài liệu tham khảo' in p:story.append(Spacer(1,12))")
p.write_text(s,encoding='utf-8')
p=Path('tmp/report/authoring.md');s=p.read_text(encoding='utf-8');s=s.replace('Bảng 2.4 tổng hợp công nghệ theo cấu hình GreenFarm local.','Các công nghệ theo cấu hình GreenFarm local được tổng hợp trong Bảng 2.4.')
p.write_text(s,encoding='utf-8')
