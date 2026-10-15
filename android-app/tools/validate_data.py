#!/usr/bin/env python3
"""Validate the app dataset before it is published or cached."""
import json,re
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
def validate(data):
 assert data['schema_version']==1,'Unsupported schema'
 assert type(data['sample_data']) is bool,'sample_data must be true/false'
 assert isinstance(data['source'],str) and data['source'].strip(),'Source is required'
 assert re.fullmatch(r'\d{4}-\d{2}-\d{2}',data['updated_at']),'Date must be YYYY-MM-DD'
 rows=data['branches']; assert 0<len(rows)<=250000,'Invalid dataset size'
 seen=set()
 for b in rows:
  code=b['ifsc'];assert re.fullmatch(r'[A-Z]{4}0[A-Z0-9]{6}',code),f'Invalid IFSC: {code}'
  assert code not in seen,f'Duplicate IFSC: {code}';seen.add(code)
  assert not b.get('micr') or re.fullmatch(r'\d{9}',b['micr']),f'Invalid MICR: {code}'
  for k in ['bank_name','branch_name','address']:assert isinstance(b.get(k),str) and b[k].strip(),f'Missing {k}: {code}'
  image=b.get('bank_image','')
  assert not image or re.fullmatch(r'images/[\w-]+\.(png|webp|jpg)',image),f'Invalid image path: {image}'
  for k in ['is_active','neft','rtgs','imps','upi']:assert b.get(k) is None or type(b[k]) is bool,f'{k} must be boolean or null'
 return len(rows)
if __name__=='__main__':
 import sys
 main=Path(sys.argv[1]) if len(sys.argv)>1 else ROOT/'data/branches.json';data=json.loads(main.read_text());count=validate(data)
 print(f'Validated {count} branches; sample_data={data["sample_data"]}')
