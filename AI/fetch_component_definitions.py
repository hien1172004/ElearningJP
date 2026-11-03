import sys, json, os, urllib.request, time
from concurrent.futures import ThreadPoolExecutor

sys.stdout.reconfigure(encoding='utf-8')

print("1. Loading existing breakdown and kanjidict_vn...")
with open('AI/data/kanji_breakdown_full.json', 'r', encoding='utf-8') as f:
    breakdown = json.load(f)

with open('BE/elearningJP/tools/seed_pipeline/datasets/dict_cache/kanjidict_vn.json', 'r', encoding='utf-8') as f:
    kanjidict = json.load(f)

kdict_map = {}
for item in kanjidict:
    ch = item[0]
    hv = item[1].upper() if item[1] else ''
    meanings = item[4] if len(item) > 4 and item[4] else []
    kdict_map[ch] = {
        'hanViet': hv,
        'meanings': meanings
    }

# Known radical variants
RADICAL_VARIANTS = {
    '亻': {'hanViet': 'NHÂN', 'detail': 'Bộ Nhân đứng (biến thể bộ Nhân 人: con người)'},
    '艹': {'hanViet': 'THẢO', 'detail': 'Bộ Thảo đầu (cỏ, thảo mộc, thực vật)'},
    '氵': {'hanViet': 'THỦY', 'detail': 'Bộ Ba chấm thủy (biến thể bộ Thủy 水: nước, sông hồ)'},
    '⻖': {'hanViet': 'PHỤ / ẤP', 'detail': 'Bộ Phụ (gò đất, phía bên trái) hoặc bộ Ấp (làng xóm, bên phải)'},
    '⺶': {'hanViet': 'DƯƠNG', 'detail': 'Bộ Dương (biến thể bộ Dương 羊: con dê)'},
    '⺪': {'hanViet': 'SƠ', 'detail': 'Bộ Sơ (biến thể bộ Sơ 疋: chân, cuộn vải)'},
    '⺢': {'hanViet': 'THỦY', 'detail': 'Bộ Thủy (biến thể phía dưới của bộ Thủy 水)'},
    '⺤': {'hanViet': 'TRẢO', 'detail': 'Bộ Trảo (biến thể bộ Trảo 爪: móng vuốt)'},
    '⺼': {'hanViet': 'NHỤC', 'detail': 'Bộ Nhục (thịt, biến thể giống chữ Nguyệt 月 chỉ các bộ phận cơ thể)'},
    '忄': {'hanViet': 'TÂM', 'detail': 'Bộ Tâm đứng (biến thể bộ Tâm 心: tim, tấm lòng, cảm xúc)'},
    '扌': {'hanViet': 'THỦ', 'detail': 'Bộ Tài gảy (biến thể bộ Thủ 手: bàn tay, hành động)'},
    '犭': {'hanViet': 'KHUYỂN', 'detail': 'Bộ Khuyển (biến thể bộ Khuyển 犬: chó, thú vật)'},
    '礻': {'hanViet': 'THỊ', 'detail': 'Bộ Thị (biến thể bộ Thị 示: thần linh, thờ phụng)'},
    '衤': {'hanViet': 'Y', 'detail': 'Bộ Y (biến thể bộ Y 衣: y phục, quần áo)'},
    '辶': {'hanViet': 'SƯỚC', 'detail': 'Bộ Quai xước (bộ Sước 辵: bước đi, di chuyển)'},
    '阝': {'hanViet': 'PHỤ / ẤP', 'detail': 'Bộ Phụ (bên trái) / bộ Ấp (bên phải)'},
    'マ': {'hanViet': 'MA', 'detail': 'Nét gấp Katakana Ma (thành phần cấu tạo Hán tự)'},
    '𠂉': {'hanViet': '', 'detail': 'Thành phần nét phẩy và nét ngang'},
    'ユ': {'hanViet': 'YU', 'detail': 'Nét Katakana Yu (thành phần cấu tạo Hán tự)'},
    'ヨ': {'hanViet': 'YO', 'detail': 'Nét Katakana Yo (thành phần cấu tạo Hán tự)'},
    'タ': {'hanViet': 'TỊCH', 'detail': 'Bộ Tịch hoặc nét Katakana Ta'},
    'ノ': {'hanViet': 'PHIỆT', 'detail': 'Nét Phiệt (nét phẩy từ phải sang trái)'},
    '一': {'hanViet': 'NHẤT', 'detail': '1. một; số một; một lần\nVD: 一列 (một hàng), 一度 (một lần), 一個 (một cái)'},
}

unique_words = set()
for k, v in breakdown.items():
    unique_words.add(k)
    graph = v.get('compGraph') or {}
    for node in graph.get('nodes', []):
        w = node.get('word')
        if w:
            unique_words.add(w)

unique_words = sorted(list(unique_words))
print(f"2. Fetching details for {len(unique_words)} words...")

results_dict = {}

def fetch_mazii(w):
    # Check RADICAL_VARIANTS first
    if w in RADICAL_VARIANTS and len(w) == 1 and ord(w) > 12000 and w not in ['一', '人', '大']:
        return w, RADICAL_VARIANTS[w]['hanViet'], RADICAL_VARIANTS[w]['detail']
    
    url = "https://mazii.net/api/search"
    req = urllib.request.Request(
        url,
        data=json.dumps({'dict': 'javi', 'type': 'kanji', 'query': w, 'page': 1}).encode('utf-8'),
        headers={'Content-Type': 'application/json', 'User-Agent': 'Mozilla/5.0'}
    )
    try:
        with urllib.request.urlopen(req, timeout=5) as resp:
            data = json.loads(resp.read().decode('utf-8'))
            items = data.get('results', [])
            if items:
                item = items[0]
                mean = item.get('mean') or ''
                detail = item.get('detail') or ''
                if detail:
                    return w, mean, detail
    except Exception:
        pass
    
    # Fallback to kanjidict_vn
    kd = kdict_map.get(w)
    if kd:
        hv = kd['hanViet']
        meanings = "; ".join(kd['meanings'])
        if meanings:
            return w, hv, meanings
        return w, hv, f"Hán Việt: {hv}"
    
    # Fallback to RADICAL_VARIANTS
    if w in RADICAL_VARIANTS:
        return w, RADICAL_VARIANTS[w]['hanViet'], RADICAL_VARIANTS[w]['detail']
    
    return w, '', ''

t0 = time.time()
with ThreadPoolExecutor(max_workers=25) as executor:
    for i, (w, m, d) in enumerate(executor.map(fetch_mazii, unique_words)):
        results_dict[w] = {
            'hanViet': m or (kdict_map.get(w, {}).get('hanViet', '')),
            'detail': d or ''
        }
        if (i + 1) % 500 == 0 or (i + 1) == len(unique_words):
            print(f"  Processed {i + 1}/{len(unique_words)} ({time.time() - t0:.1f}s)")

# Fill in empty details with kanjidict fallback
for w, info in results_dict.items():
    if not info['detail'] and w in kdict_map:
        info['detail'] = "; ".join(kdict_map[w]['meanings'])
    if not info['hanViet'] and w in kdict_map:
        info['hanViet'] = kdict_map[w]['hanViet']
    if not info['detail'] and w in RADICAL_VARIANTS:
        info['detail'] = RADICAL_VARIANTS[w]['detail']
    if not info['hanViet'] and w in RADICAL_VARIANTS:
        info['hanViet'] = RADICAL_VARIANTS[w]['hanViet']

print(f"Done fetching in {time.time() - t0:.1f}s!")

# Save to component_definitions.json
with open('AI/data/component_definitions.json', 'w', encoding='utf-8') as f:
    json.dump(results_dict, f, ensure_ascii=False, indent=2)

print("Saved AI/data/component_definitions.json")

