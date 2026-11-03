import json

with open('AI/data/kanji_breakdown_full.json', 'r', encoding='utf-8') as f:
    breakdown = json.load(f)

with open('AI/data/update_enriched_breakdown.sql', 'w', encoding='utf-8') as f:
    f.write('BEGIN;\n')
    for ch, val in breakdown.items():
        comp_details = val.get('compDetail')
        comp_graph = val.get('compGraph')
        
        if comp_details:
            s_det = json.dumps(comp_details, ensure_ascii=False).replace("'", "''")
            comp_det_sql = f"'{s_det}'::jsonb"
        else:
            comp_det_sql = 'NULL'

        if comp_graph:
            s_graph = json.dumps(comp_graph, ensure_ascii=False).replace("'", "''")
            comp_graph_sql = f"'{s_graph}'::jsonb"
        else:
            comp_graph_sql = 'NULL'
        
        escaped_ch = ch.replace("'", "''")
        f.write(f"UPDATE writing_characters SET comp_details_json = {comp_det_sql}, comp_graph_json = {comp_graph_sql} WHERE character = '{escaped_ch}' AND char_type = 'KANJI';\n")
    f.write('COMMIT;\n')

print('Generated AI/data/update_enriched_breakdown.sql successfully')
