"""Download compatible public Modrinth releases into an isolated test profile.

Only required dependencies are installed; files are checked against Modrinth's SHA-512.
This does not alter the user's Minecraft installation or redistribute any mod jars.
"""
import hashlib
import argparse
import json
from pathlib import Path
import urllib.parse
import urllib.request

API = "https://api.modrinth.com/v2"
OUTPUT = Path(__file__).resolve().parents[1] / "run-compat" / "mods"
PROJECTS = ["terralith", "biolith", "biomes-o-plenty", "integrated-villages", "underwater-village",
            "streams-reflowing", "railways-untold", "regions-unexplored", "natural-temperature",
            "supplementaries", "integrated-api", "create"]

def get(url):
    request = urllib.request.Request(url, headers={"User-Agent": "AkisIslands/0.4.1 (local compatibility testing)"})
    with urllib.request.urlopen(request, timeout=60) as response:
        return response.read()

def api(path):
    return json.loads(get(API + path))

def find_version(project):
    query = urllib.parse.urlencode({"game_versions": json.dumps(["1.21.1"]), "loaders": json.dumps(["neoforge"])})
    versions = api(f"/project/{urllib.parse.quote(project)}/version?{query}")
    if not versions:
        raise ValueError("No declared NeoForge 1.21.1 release on Modrinth")
    return next((v for v in versions if v["version_type"] == "release"), versions[0])

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--locked', action='store_true', help='Replay docs/tested-mods.json instead of selecting current releases')
    arguments = parser.parse_args()
    OUTPUT.mkdir(parents=True, exist_ok=True)
    if arguments.locked:
        manifest = json.loads((Path(__file__).resolve().parents[1] / 'docs' / 'tested-mods.json').read_text())
        for item in manifest['installed'].values():
            filename = Path(item['filename']).name
            if not filename.endswith('.jar'):
                raise ValueError('Expected a jar: ' + filename)
            destination = OUTPUT / filename
            blob = destination.read_bytes() if destination.exists() else get(item['url'])
            if hashlib.sha512(blob).hexdigest() != item['sha512']:
                raise ValueError('SHA-512 mismatch: ' + filename)
            destination.write_bytes(blob)
            print('Verified ' + filename, flush=True)
        (OUTPUT.parent / 'downloads.json').write_text(json.dumps(manifest, indent=2))
        return
    installed = {}
    unavailable = {}

    def install(version):
        project = version["project_id"]
        if project in installed:
            if installed[project]["version_id"] != version["id"]:
                raise ValueError(f"Dependency version conflict for {project}")
            return
        file = next((f for f in version["files"] if f["primary"]), version["files"][0])
        filename = Path(file["filename"]).name
        if not filename.endswith(".jar"):
            raise ValueError("Expected a mod jar: " + filename)
        destination = OUTPUT / filename
        blob = destination.read_bytes() if destination.exists() else get(file["url"])
        if hashlib.sha512(blob).hexdigest() != file["hashes"]["sha512"]:
            raise ValueError("SHA-512 mismatch: " + filename)
        destination.write_bytes(blob)
        installed[project] = {"version_id": version["id"], "version": version["version_number"],
                              "filename": filename, "url": file["url"], "sha512": file["hashes"]["sha512"]}
        print("Installed " + filename, flush=True)
        for dependency in version["dependencies"]:
            if dependency["dependency_type"] == "required":
                install(api("/version/" + dependency["version_id"]) if dependency["version_id"]
                        else find_version(dependency["project_id"]))

    for project in PROJECTS:
        try:
            install(find_version(project))
        except Exception as error:
            unavailable[project] = str(error)
            print(f"Unavailable: {project}: {error}", flush=True)
    (OUTPUT.parent / "downloads.json").write_text(json.dumps({"installed": installed, "unavailable": unavailable}, indent=2))

if __name__ == "__main__":
    main()
