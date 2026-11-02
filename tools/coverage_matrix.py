#!/usr/bin/env python3
"""Module x package unit-test coverage matrix from per-module JaCoCo CSVs."""
import argparse, csv, json, os
from collections import defaultdict, namedtuple

Counts = namedtuple("Counts", "line_missed line_covered branch_missed branch_covered method_missed method_covered")
SKIP = {"jmt-app", "jmt-integration-tests"}
CSV = os.path.join("target", "site", "jacoco", "jacoco.csv")
COLS = ("LINE_MISSED", "LINE_COVERED", "BRANCH_MISSED", "BRANCH_COVERED", "METHOD_MISSED", "METHOD_COVERED")


def load_module(csv_path):
    out = defaultdict(lambda: Counts(0, 0, 0, 0, 0, 0))
    with open(csv_path, newline="") as f:
        for r in csv.DictReader(f):
            out[r["PACKAGE"]] = Counts(*(a + int(r[c]) for a, c in zip(out[r["PACKAGE"]], COLS)))
    return dict(out)


def collect(root, modules=None):
    """module -> {package: Counts}; {} when the module has no CSV."""
    root = os.path.abspath(root)
    names = sorted(d for d in os.listdir(root) if os.path.isfile(os.path.join(root, d, CSV))
                   or os.path.isfile(os.path.join(root, d, "pom.xml")))
    out = {}
    for m in names:
        if m in SKIP or (modules and m not in modules):
            continue
        p = os.path.join(root, m, CSV)
        out[m] = load_module(p) if os.path.isfile(p) else {}
    return out


def pct(missed, covered):
    t = missed + covered
    return "-" if t == 0 else f"{100.0 * covered / t:.1f}%"


def _cells(c):
    return (pct(c.line_missed, c.line_covered), pct(c.branch_missed, c.branch_covered),
            pct(c.method_missed, c.method_covered), c.line_missed + c.line_covered,
            c.branch_missed + c.branch_covered)


def format_matrix(data):
    rows = [("module", "package", "line", "branch", "method", "lines", "branches")]
    for m, pkgs in data.items():
        if not pkgs:
            rows.append((m, "no unit tests", "", "", "", "", ""))
            continue
        for p in sorted(pkgs):
            rows.append((m, p) + tuple(str(x) for x in _cells(pkgs[p])))
        tot = Counts(*(sum(c[i] for c in pkgs.values()) for i in range(6)))
        rows.append((m, "TOTAL") + tuple(str(x) for x in _cells(tot)))
    w = [max(len(r[i]) for r in rows) for i in range(7)]
    return "\n".join("  ".join(c.ljust(w[i]) if i < 2 else c.rjust(w[i]) for i, c in enumerate(r)).rstrip()
                     for r in rows)


def diff_matrix(old, new):
    rows = [("module", "package", "line before", "line after", "branch before", "branch after")]
    for m in sorted(set(old) | set(new)):
        for p in sorted(set(old.get(m, {})) | set(new.get(m, {}))):
            o = old.get(m, {}).get(p, Counts(0, 0, 0, 0, 0, 0))
            n = new.get(m, {}).get(p, Counts(0, 0, 0, 0, 0, 0))
            rows.append((m, p, pct(o.line_missed, o.line_covered), pct(n.line_missed, n.line_covered),
                         pct(o.branch_missed, o.branch_covered), pct(n.branch_missed, n.branch_covered)))
    w = [max(len(r[i]) for r in rows) for i in range(6)]
    return "\n".join("  ".join(c.ljust(w[i]) for i, c in enumerate(r)).rstrip() for r in rows)


def worst_classes(csv_path, n, package_prefix=""):
    with open(csv_path, newline="") as f:
        rows = [(f'{r["PACKAGE"]}.{r["CLASS"]}', int(r["BRANCH_MISSED"]), int(r["LINE_MISSED"]))
                for r in csv.DictReader(f) if r["PACKAGE"].startswith(package_prefix)]
    return sorted(rows, key=lambda x: (x[1], x[2]), reverse=True)[:n]


def _load_json(path):
    with open(path) as f:
        return {m: {p: Counts(*c) for p, c in pk.items()} for m, pk in json.load(f).items()}


def main():
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--modules")
    ap.add_argument("--save")
    ap.add_argument("--diff")
    ap.add_argument("--worst", type=int)
    ap.add_argument("--package", default="")
    a = ap.parse_args()
    root = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
    data = collect(root, a.modules.split(",") if a.modules else None)
    if a.worst:
        for m in data:
            p = os.path.join(root, m, CSV)
            if os.path.isfile(p):
                print(f"# {m}")
                for c, b, l in worst_classes(p, a.worst, a.package):
                    print(f"{b:6d} {l:6d}  {c}")
        return
    print(diff_matrix(_load_json(a.diff), data) if a.diff else format_matrix(data))
    if a.save:
        with open(a.save, "w") as f:
            json.dump({m: {p: list(c) for p, c in pk.items()} for m, pk in data.items()}, f, indent=1, sort_keys=True)


if __name__ == "__main__":
    main()
