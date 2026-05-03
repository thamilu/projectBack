import re
from pathlib import Path

root = Path('src')
java_files = list(root.rglob('*.java'))
removed = 0
for p in java_files:
    text = p.read_text(encoding='utf-8')
    lines = text.splitlines()
    import_lines = []
    for i,l in enumerate(lines):
        if l.strip().startswith('import ') and not l.strip().startswith('import static'):
            import_lines.append((i,l))
    if not import_lines:
        continue
    to_remove_idxs = []
    for i,l in import_lines:
        m = re.match(r'\s*import\s+([\w\.]+)\.(\w+)\s*;\s*', l)
        if not m:
            continue
        simple = m.group(2)
        # check usage elsewhere
        # remove import lines and compute if simple appears in remaining text
        # Exclude the import lines from search
        remaining = '\n'.join([ln for idx,ln in enumerate(lines) if idx!=i])
        if re.search(r'\b' + re.escape(simple) + r'\b', remaining) is None:
            to_remove_idxs.append(i)
    if to_remove_idxs:
        new_lines = [ln for idx,ln in enumerate(lines) if idx not in to_remove_idxs]
        p.write_text('\n'.join(new_lines), encoding='utf-8')
        removed += len(to_remove_idxs)
        print(f'Removed {len(to_remove_idxs)} imports from {p}')

print(f'DONE. Total removed: {removed}')
