#!/usr/bin/env bash
# Gera os diagramas (SVG e PDF) e, quando existirem, os PDFs da Descrição Arquitetural.
set -e
cd "$(dirname "$0")"
PLANTUML_JAR="${PLANTUML_JAR:-$HOME/.local/lib/plantuml/plantuml.jar}"

java -jar "$PLANTUML_JAR" -charset UTF-8 -tsvg diagramas/*.puml
for svg in diagramas/*.svg; do
  rsvg-convert -f pdf -o "${svg%.svg}.pdf" "$svg"
done

for tex in *.tex; do
  [ -e "$tex" ] || continue
  latexmk -pdf -interaction=nonstopmode -halt-on-error "$tex"
done
