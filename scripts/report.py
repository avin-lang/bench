"""Turns hyperfine's per-program JSON into results/<date>.json and results/<date>.md.

usage: report.py <raw dir> <date>

The raw directory (results/raw-<date>/) is kept: it holds every measurement of every run.
"""
import json
import math
import os
import subprocess
import sys

raw, stamp = sys.argv[1], sys.argv[2]
root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

programs = ["binarytrees", "fannkuchredux", "nbody", "spectralnorm", "fasta", "knucleotide"]
avin = ["avin-idiomatic", "avin-tuned"]
rivals_plain = ["scala-native", "kotlin-native", "swift", "ocaml", "go", "crystal"]
rivals_tuned = [r + "-tuned" for r in rivals_plain]
ceiling = ["c", "rust"]
columns = avin + [c for pair in zip(rivals_plain, rivals_tuned) for c in pair] + ceiling


def run(cmd):
    env = dict(os.environ)
    env["PATH"] = "/Users/work/.cargo/bin:/opt/homebrew/bin:" + env["PATH"]
    try:
        out = subprocess.run(cmd, capture_output=True, text=True, env=env, input="")
    except OSError as e:
        return str(e)
    return (out.stdout + out.stderr).strip()


def first_line(text):
    return text.splitlines()[0] if text else ""


toolchains = {
    "avin": first_line(run(["avin", "--version"])),
    "scala-native": "scala-cli " + first_line(run(["scala-cli", "--version"])),
    "kotlin-native": first_line(run(["kotlinc-native", "-version"])),
    "swift": first_line(run(["swiftc", "--version"])),
    "ocaml": "ocamlopt " + first_line(run(["ocamlopt", "-version"])) + " (default switch, no flambda)",
    "ocaml-tuned": "ocamlopt " + first_line(run(["opam", "exec", "--switch=flambda", "--", "ocamlopt", "-version"]))
                   + " (flambda switch)",
    "go": first_line(run(["go", "version"])),
    "crystal": first_line(run(["crystal", "--version"])),
    "c": first_line(run(["/usr/bin/clang", "--version"])),
    "rust": first_line(run(["rustc", "--version"])),
    "hyperfine": first_line(run(["hyperfine", "--version"])),
}
machine = {"cpu": run(["sysctl", "-n", "machdep.cpu.brand_string"]),
           "memory_gb": int(run(["sysctl", "-n", "hw.memsize"]) or 0) // 2**30,
           "os": "macOS " + first_line(run(["sw_vers", "-productVersion"]))}


def load(name):
    with open(os.path.join(raw, name)) as f:
        return f.read().strip()


def stats(values):
    n = len(values)
    mean = sum(values) / n
    var = sum((v - mean) ** 2 for v in values) / (n - 1) if n > 1 else 0.0
    return mean, math.sqrt(var), min(values), max(values)


data = {}
for prog in programs:
    with open(os.path.join(raw, prog + ".json")) as f:
        rows = {r["name"]: r for r in json.load(f)["results"]}
    entry = {}
    for lang in columns:
        r = rows[lang]
        wall = r["times"]
        user, system = r.get("user"), r.get("system")
        mean, sd, lo, hi = stats(wall)
        entry[lang] = {"mean": mean, "stddev": sd, "min": lo, "max": hi, "runs": len(wall),
                       "times": wall, "cpu_mean": (user or 0) + (system or 0),
                       "max_rss_bytes": max(r.get("memory_usage_byte") or [0])}
    best_plain = min(rivals_plain, key=lambda l: entry[l]["mean"])
    best_tuned = min(rivals_plain + rivals_tuned, key=lambda l: entry[l]["mean"])
    for lang in columns:
        entry[lang]["ratio_to_c"] = entry[lang]["mean"] / entry["c"]["mean"]
        entry[lang]["ratio_to_best_rival_plain"] = entry[lang]["mean"] / entry[best_plain]["mean"]
    data[prog] = {"columns": entry, "best_rival_plain": best_plain, "best_rival_any": best_tuned}


def geo(lang, key):
    vals = [data[p]["columns"][lang][key] for p in programs]
    return math.exp(sum(math.log(v) for v in vals) / len(vals))


geomean = {lang: {"ratio_to_c": geo(lang, "ratio_to_c"),
                  "ratio_to_best_rival_plain": geo(lang, "ratio_to_best_rival_plain")} for lang in columns}

doc = {"date": stamp, "machine": machine, "toolchains": toolchains,
       "load": {"start": load("load-start.txt"), "end": load("load-end.txt")},
       "raw": os.path.relpath(raw, root), "programs": data, "geometric_mean": geomean}
with open(os.path.join(root, "results", stamp + ".json"), "w") as f:
    json.dump(doc, f, indent=2)
    f.write("\n")

runs = data[programs[0]]["columns"]["c"]["runs"]
lines = [f"# Results {stamp}", "",
         f"{machine['cpu']}, {machine['memory_gb']} GB, {machine['os']}. Single-threaded programs, "
         f"`hyperfine` with {runs} timed runs per column after a warm-up. Raw measurements: "
         f"`{os.path.relpath(raw, root)}/`.", "",
         "Load average at the start and end of the run:", "", "```", doc["load"]["start"],
         doc["load"]["end"], "```", "",
         "## Mean wall time, seconds (± sample standard deviation)", "",
         "| program | " + " | ".join(columns) + " |", "|---|" + "---:|" * len(columns)]
for prog in programs:
    e = data[prog]["columns"]
    lines.append(f"| {prog} | " + " | ".join(f"{e[c]['mean']:.3f} ± {e[c]['stddev']:.3f}" for c in columns) + " |")
lines += ["", "## Ratio to C (1.00 = C's time; higher is slower)", "",
          "| program | " + " | ".join(c for c in columns if c != "c") + " |",
          "|---|" + "---:|" * (len(columns) - 1)]
for prog in programs:
    e = data[prog]["columns"]
    lines.append(f"| {prog} | " + " | ".join(f"{e[c]['ratio_to_c']:.2f}" for c in columns if c != "c") + " |")
lines.append("| **geometric mean** | " + " | ".join(f"{geomean[c]['ratio_to_c']:.2f}" for c in columns if c != "c") + " |")
lines += ["", "## avin against the best rival", "",
          "The best rival plain column is the fastest of the six rivals' plain (idiomatic) programs; the best "
          "rival overall also counts their tuned columns.", "",
          "| program | best rival plain | its time (s) | avin-idiomatic ÷ it | avin-tuned ÷ it | best rival overall | avin-tuned ÷ it |",
          "|---|---|---:|---:|---:|---|---:|"]
for prog in programs:
    d = data[prog]
    e = d["columns"]
    bp, bt = d["best_rival_plain"], d["best_rival_any"]
    lines.append(f"| {prog} | {bp} | {e[bp]['mean']:.3f} | {e['avin-idiomatic']['mean'] / e[bp]['mean']:.2f} | "
                 f"{e['avin-tuned']['mean'] / e[bp]['mean']:.2f} | {bt} | {e['avin-tuned']['mean'] / e[bt]['mean']:.2f} |")
lines += ["", "## Peak memory (max resident set, MB)", "",
          "| program | " + " | ".join(columns) + " |", "|---|" + "---:|" * len(columns)]
for prog in programs:
    e = data[prog]["columns"]
    lines.append(f"| {prog} | " + " | ".join(f"{e[c]['max_rss_bytes'] / 2**20:.0f}" for c in columns) + " |")
lines += ["", "## CPU time (user + system), seconds", "",
          "A runtime whose collector runs on threads of its own shows more CPU than wall time.", "",
          "| program | " + " | ".join(columns) + " |", "|---|" + "---:|" * len(columns)]
for prog in programs:
    e = data[prog]["columns"]
    lines.append(f"| {prog} | " + " | ".join(f"{e[c]['cpu_mean']:.3f}" for c in columns) + " |")
lines += ["", "## Toolchains", ""] + [f"- **{k}**: {v}" for k, v in toolchains.items()] + [""]
with open(os.path.join(root, "results", stamp + ".md"), "w") as f:
    f.write("\n".join(lines))
print("wrote results/%s.json and results/%s.md" % (stamp, stamp))
