#!/usr/bin/env python3
"""Import an authorized, verified branch CSV; preserve MICR leading zeros."""
import argparse,csv,json,datetime
from pathlib import Path
from validate_data import ROOT,validate
p=argparse.ArgumentParser()
p.add_argument('csv_file',type=Path);p.add_argument('--source',required=True,help='Verified source URL/name and retrieval date')
p.add_argument('--verified',action='store_true',help='Assert the records have been verified with the source')
a=p.parse_args()
if not a.verified:p.error('Use --verified only after verifying the source and your permission to redistribute it')
banks_path=ROOT/'data/banks.json'
banks={b['bank_name'].casefold():b.get('bank_image',b.get('logo_url','')) for b in json.loads(banks_path.read_text())} if banks_path.exists() else {}
rows=[]
with a.csv_file.open(newline='',encoding='utf-8-sig') as f:
 for raw in csv.DictReader(f):
  b={k.strip().lower():(v or '').strip() for k,v in raw.items() if k}
  for k in ['ifsc','micr','bank_name','branch_name','address']: b.setdefault(k,'')
  b['ifsc']=b['ifsc'].upper();b.setdefault('branch_code',b['ifsc'][5:]);b.setdefault('bank_image',banks.get(b['bank_name'].casefold(),''))
  if b['bank_image'] and not b['bank_image'].startswith('images/'):b['bank_image']='images/'+b['bank_image']
  for k in ['is_active','neft','rtgs','imps','upi']:
   v=b.get(k,'').lower()
   if v not in ('','true','false'):raise ValueError(f'{k}: expected true/false/empty')
   b[k]=None if not v else v=='true'
  rows.append(b)
d=dict(schema_version=1,sample_data=False,updated_at=datetime.date.today().isoformat(),source=a.source,branches=rows)
validate(d);text=json.dumps(d,ensure_ascii=False,indent=2)+'\n'
output=ROOT/'data/branches.json';output.parent.mkdir(exist_ok=True);output.write_text(text,encoding='utf-8')
print(f'Imported {len(rows)} verified records. Review and upload data/branches.json to GitHub.')
