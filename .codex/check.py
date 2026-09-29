"""Check shared guidance and native execpolicy decisions without executing example commands."""

import json
from pathlib import Path
import re
import shutil
import subprocess
import tomllib
from urllib.parse import urlsplit


def main():
    root = Path(__file__).resolve().parents[1]
    config = tomllib.loads((root / ".codex/config.toml").read_text(encoding="utf-8"))
    if config.get("project_root_markers") != [".git"]:
        raise SystemExit("Project root markers must not split the Maven module tree.")

    entries = [root / path for path in ("AGENTS.md", "backend/AGENTS.md", "frontend/AGENTS.md")]
    documents = entries + sorted((root / ".codex").rglob("*.md"))
    for document in documents:
        for target in re.findall(r"\]\(([^)]+)\)", document.read_text(encoding="utf-8")):
            url = urlsplit(target)
            if not url.scheme and url.path and not (document.parent / url.path).is_file():
                raise SystemExit(f"Broken link in {document.relative_to(root)}: {target}")

    for entry in entries:
        chain = [entries[0]] if entry == entries[0] else [entries[0], entry]
        if sum(path.stat().st_size for path in chain) > config["project_doc_max_bytes"]:
            raise SystemExit(f"Instruction chain exceeds project_doc_max_bytes: {entry.relative_to(root)}")

    codex = shutil.which("codex")
    if not codex:
        raise SystemExit("Codex CLI with execpolicy check is required on PATH.")

    cases = [
        (["git", "reset", "--hard"], "forbidden"),
        (["git", "reset", "--hard", "HEAD~1"], "forbidden"),
        (["git", "clean", "-fd"], "forbidden"),
        (["git", "clean", "-fdx"], "forbidden"),
        (["git", "status", "--short"], "prompt"),
        (["git", "push", "origin", "codex/example"], "prompt"),
        (["git", "-C", ".", "push", "origin", "codex/example"], "prompt"),
        (["git", "-C", ".", "reset", "--hard"], "prompt"),
        (["git", "reset", "HEAD", "--hard"], "prompt"),
        (["npm", "publish"], "prompt"),
        (["pnpm", "--dir", "frontend", "publish"], "prompt"),
        (["pnpm", "--filter", "@cordys/web", "publish"], "prompt"),
        (["pnpm", "--dir", "frontend", "build"], "prompt"),
        (["./mvnw", "-f", "backend/pom.xml", "clean", "deploy"], "prompt"),
        (["./mvnw", "clean", "package"], "prompt"),
        (["mvn", "deploy"], "prompt"),
        (["mvnw.cmd", "deploy"], "prompt"),
        ([r".\mvnw.cmd", "deploy"], "prompt"),
        (["git", "-c", "push.default=current", "push"], "prompt"),
        (["npm", "--prefix", "frontend", "publish"], "prompt"),
        (["rg", "--files"], None),
        (["node", "--version"], None),
        (["java", "-version"], None),
    ]
    for command, expected in cases:
        result = subprocess.run(
            [codex, "execpolicy", "check", "--rules", str(root / ".codex/rules/default.rules"), "--", *command],
            cwd=root,
            capture_output=True,
            text=True,
            encoding="utf-8",
            timeout=30,
        )
        if result.returncode:
            raise SystemExit(f"Native rule validation failed:\n{result.stderr}")
        actual = json.loads(result.stdout).get("decision")
        if actual != expected:
            raise SystemExit(f"Unexpected rule decision for {command!r}: {actual!r}, expected {expected!r}")

    print(f"OK: TOML, {len(documents)} guidance documents, instruction budgets, {len(cases)} execpolicy checks.")


if __name__ == "__main__":
    main()
