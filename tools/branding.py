"""Copy the supplied PNG into cover/icon locations without changing its bytes."""
import argparse
from pathlib import Path
import shutil

ROOT = Path(__file__).resolve().parents[1]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source', type=Path, default=ROOT / 'docs' / 'cover.png')
    arguments = parser.parse_args()
    source = arguments.source.resolve()
    if source.read_bytes()[:8] != b'\x89PNG\r\n\x1a\n':
        raise ValueError('Artwork must be a PNG')
    for relative in ('docs/cover.png', 'src/main/resources/icon.png', 'src/main/resources/pack.png'):
        destination = ROOT / relative
        destination.parent.mkdir(parents=True, exist_ok=True)
        if source != destination.resolve():
            shutil.copyfile(source, destination)
        print(relative)


if __name__ == '__main__':
    main()
