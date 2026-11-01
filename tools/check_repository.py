"""Check publication essentials, exact-copy artwork, and tracked development-file exclusions."""
import hashlib
import json
from pathlib import Path
import re
import struct
import subprocess

ROOT = Path(__file__).resolve().parents[1]


def main():
    required = (
        'README.md', 'LICENSE', 'CREDITS.md', 'LEGAL.md', 'TEMPLATE_LICENSE.txt', 'BRANDING.md',
        'DEVELOPMENT.md', 'CONTRIBUTING.md', 'SECURITY.md', 'CHANGELOG.md',
        'docs/GUIDE.md', 'docs/COMPATIBILITY.md', 'docs/tested-mods.json',
        'docs/cover.png', 'src/main/resources/icon.png', 'src/main/resources/pack.png',
        'src/main/templates/META-INF/neoforge.mods.toml', 'gradle/wrapper/gradle-wrapper.jar',
        '.github/workflows/build.yml',
    )
    for relative in required:
        if not (ROOT / relative).is_file():
            raise ValueError('Missing ' + relative)
    artwork = (ROOT / 'docs/cover.png').read_bytes()
    if artwork[:8] != b'\x89PNG\r\n\x1a\n' or struct.unpack('>II', artwork[16:24]) != (1024, 1024):
        raise ValueError('Expected the supplied 1024 x 1024 PNG cover')
    digest = hashlib.sha256(artwork).digest()
    for relative in ('src/main/resources/icon.png', 'src/main/resources/pack.png'):
        if hashlib.sha256((ROOT / relative).read_bytes()).digest() != digest:
            raise ValueError('Artwork changed while packaging ' + relative)
    for path in (ROOT / 'src/main/resources').rglob('*.json'):
        json.loads(path.read_text(encoding='utf-8'))
    json.loads((ROOT / 'docs/tested-mods.json').read_text(encoding='utf-8'))
    readme = (ROOT / 'README.md').read_text(encoding='utf-8')
    for text in ('Product of Achilleus', 'docs/cover.png', 'docs/COMPATIBILITY.md', 'All Rights Reserved'):
        if text not in readme:
            raise ValueError('README is missing ' + text)
    metadata = (ROOT / 'src/main/templates/META-INF/neoforge.mods.toml').read_text(encoding='utf-8')
    for text in ('logoFile="icon.png"', 'authors="Achilleus (Achilleus-1)"', 'Achilleus-1/Akis-Islands-Mod'):
        if text not in metadata:
            raise ValueError('Metadata is missing ' + text)
    if (ROOT / '.git').exists():
        result = subprocess.run(['git', '-c', f'safe.directory={ROOT.as_posix()}', 'ls-files', '-z'],
                                cwd=ROOT, check=True, capture_output=True)
        for name in result.stdout.decode('utf-8').split('\0'):
            if not name:
                continue
            if re.match(r'^(?:build/|run(?:/|-)|\.gradle/|\.bootstrap/|\.dev-sources/|\.vscode/)', name) or name.endswith('.log'):
                raise ValueError('Development output tracked: ' + name)
            if name.endswith('.jar') and name != 'gradle/wrapper/gradle-wrapper.jar':
                raise ValueError('Unexpected jar tracked: ' + name)
    print('Repository checks passed; cover and both packaged icons are byte-identical.')


if __name__ == '__main__':
    main()
