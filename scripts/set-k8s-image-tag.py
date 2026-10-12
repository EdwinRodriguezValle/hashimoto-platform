#!/usr/bin/env python3
"""Set one K3d application image tag in the core Kustomize file."""

import re
import sys
from pathlib import Path


TAG_PATTERN = re.compile(r"[A-Za-z0-9_][A-Za-z0-9_.-]{0,127}\Z")
KUSTOMIZATION = Path("k8s/core/kustomization.yaml")


def main() -> int:
    services = {"patient-service", "api-gateway", "frontend"}
    if (
        len(sys.argv) != 3
        or sys.argv[1] not in services
        or not TAG_PATTERN.fullmatch(sys.argv[2])
    ):
        print(
            "Usage: set-k8s-image-tag.py <patient-service|api-gateway|frontend> <valid-image-tag>",
            file=sys.stderr,
        )
        return 2

    service, tag = sys.argv[1:]
    text = KUSTOMIZATION.read_text(encoding="utf-8")
    updated, count = re.subn(
        rf"(?m)(^  - name: hashimoto/{re.escape(service)}\n[\s\S]*?^    newTag:\s*)[^\n]+",
        lambda match: f'{match.group(1)}"{tag}"',
        text,
    )
    if count != 1:
        print(
            f"Expected one image entry for {service} in {KUSTOMIZATION}, found {count}; file not changed.",
            file=sys.stderr,
        )
        return 1

    KUSTOMIZATION.write_text(updated, encoding="utf-8")
    print(f"Set {service} image tag to {tag}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
