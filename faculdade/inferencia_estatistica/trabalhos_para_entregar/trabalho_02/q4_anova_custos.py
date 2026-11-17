"""
GCC1625 - Inferência Estatística - Trabalho 02
Q4 - Teste ANOVA: custos de materiais

O arquivo data/data9.xls traz o custo dos materiais (livro, manual de soluções,
taxas de laboratório etc.) de dez cursos em cada uma de três disciplinas:
química, ciência da computação e matemática. A disciplina não aparece em uma
coluna própria; ela é identificada pelo prefixo do código do curso
(CHEM1..CHEM10, COMP1..COMP10, MATH1..MATH10).

Execução (a partir da raiz da entrega):
    python q4_anova_custos.py
"""

import json
import sys
import warnings
from pathlib import Path

import matplotlib

matplotlib.use("Agg")  # só salva as figuras em arquivo (sem abrir janela)
import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
from scipy import stats

ARQUIVO_DADOS = Path("data") / "data9.xls"
PASTA_RESULTADOS = Path("resultados")
ARQUIVO_JSON = PASTA_RESULTADOS / "q4_resultados.json"
ARQUIVO_GRAFICO = PASTA_RESULTADOS / "q4_custos.png"

ALFA = 0.01

# prefixo do código do curso -> (chave usada no JSON, nome para os gráficos)
DISCIPLINAS = {
    "CHEM": ("quimica", "Química"),
    "COMP": ("ciencia_da_computacao", "Ciência da Computação"),
    "MATH": ("matematica", "Matemática"),
}
CORES = {"CHEM": "#8e6bbf", "COMP": "#3a78b5", "MATH": "#d1603d"}
PALAVRAS = {"CHEM": ("chem", "quim", "quím"), "COMP": ("comp",), "MATH": ("math", "mat")}


def ler_planilha(caminho):
    with warnings.catch_warnings():
        # versões novas do pandas avisam que o leitor xlrd (usado para .xls)
        # vai ser substituído; o aviso não afeta a leitura
        warnings.filterwarnings("ignore", message=".*xlrd.*")
        try:
            return pd.read_excel(caminho)
        except ImportError:
            # sem o xlrd instalado, tenta o leitor calamine (se existir)
            return pd.read_excel(caminho, engine="calamine")


def organizar_custos(df):
    """Separa os custos por disciplina. Devolve {prefixo: (custos, códigos)}."""
    df = df.dropna(how="all")
    df.columns = [str(c).strip() for c in df.columns]
    numericas = [c for c in df.columns if pd.api.types.is_numeric_dtype(df[c])]
    textuais = [c for c in df.columns if c not in numericas]

    grupos = {}
    if textuais and numericas:
        # formato do data9.xls: uma linha por curso (coluna "Cource" e coluna "Cost")
        col_curso, col_custo = textuais[0], numericas[-1]
        codigos = df[col_curso].astype(str).str.strip().str.upper()
        prefixos = codigos.str.extract(r"^([A-Z]+)", expand=False)
        for sigla in DISCIPLINAS:
            linhas = df[prefixos == sigla]
            custos = pd.to_numeric(linhas[col_custo], errors="coerce")
            ok = custos.notna()
            grupos[sigla] = (custos[ok].to_numpy(dtype=float),
                             codigos[prefixos == sigla][ok].tolist())
    else:
        # caso a planilha venha "larga", com uma coluna por disciplina
        for sigla, palavras in PALAVRAS.items():
            coluna = next(c for c in df.columns if any(p in c.lower() for p in palavras))
            custos = pd.to_numeric(df[coluna], errors="coerce").dropna()
            grupos[sigla] = (custos.to_numpy(dtype=float), [])
    return grupos


def tabela_anova(amostras):
    todos = np.concatenate(amostras)
    N, k = todos.size, len(amostras)
    media_geral = todos.mean()
    sq_entre = sum(g.size * (g.mean() - media_geral) ** 2 for g in amostras)
    sq_dentro = sum(((g - g.mean()) ** 2).sum() for g in amostras)
    gl_entre, gl_dentro = k - 1, N - k
    qm_entre, qm_dentro = sq_entre / gl_entre, sq_dentro / gl_dentro
    f = qm_entre / qm_dentro
    return {
        "grand_mean": float(media_geral),
        "between": {"ss": float(sq_entre), "df": int(gl_entre), "ms": float(qm_entre)},
        "within": {"ss": float(sq_dentro), "df": int(gl_dentro), "ms": float(qm_dentro)},
        "total": {"ss": float(sq_entre + sq_dentro), "df": int(N - 1)},
        "F": float(f),
        "pvalue": float(stats.f.sf(f, gl_entre, gl_dentro)),
        "eta_squared": float(sq_entre / (sq_entre + sq_dentro)),
    }


def grafico_custos(grupos, anova, resultado_texto, caminho):
    siglas = list(grupos)
    fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(13, 5.5))

    # (a) boxplot com os cursos de cada disciplina
    caixas = ax1.boxplot([grupos[s][0] for s in siglas], widths=0.5, patch_artist=True,
                         showmeans=True,
                         meanprops={"marker": "D", "markerfacecolor": "black",
                                    "markeredgecolor": "black", "markersize": 6},
                         flierprops={"marker": ""})  # os pontos já aparecem no scatter
    rng = np.random.default_rng(1625)
    for i, (s, caixa) in enumerate(zip(siglas, caixas["boxes"]), start=1):
        caixa.set_facecolor(CORES[s])
        caixa.set_alpha(0.3)
        custos, codigos = grupos[s]
        ax1.scatter(i + rng.uniform(-0.12, 0.12, custos.size), custos, s=30, color=CORES[s],
                    edgecolor="black", linewidth=0.4, zorder=3)
        # identifica valores fora dos limites de 1,5 IQR
        q1, q3 = np.percentile(custos, [25, 75])
        limite = q3 + 1.5 * (q3 - q1)
        for valor, codigo in zip(custos, codigos):
            if valor > limite:
                ax1.annotate(f"{codigo}: {valor:.0f}", (i, valor), xytext=(12, 0),
                             textcoords="offset points", fontsize=9, va="center")
    ax1.axhline(anova["grand_mean"], color="gray", ls="--", lw=1.2,
                label=f"média geral = {anova['grand_mean']:.2f}")
    ax1.set_xticks(range(1, len(siglas) + 1))
    ax1.set_xticklabels([f"{DISCIPLINAS[s][1]}\nn = {grupos[s][0].size}" for s in siglas])
    ax1.set_ylabel("custo dos materiais")
    ax1.set_title("Custos por disciplina (◆ = média)")
    ax1.legend(loc="upper left")
    ax1.grid(axis="y", alpha=0.3)

    # (b) médias com intervalos de confiança de 99%
    for i, s in enumerate(siglas, start=1):
        x = grupos[s][0]
        margem = stats.t.ppf(1 - ALFA / 2, x.size - 1) * x.std(ddof=1) / np.sqrt(x.size)
        ax2.errorbar(i, x.mean(), yerr=margem, fmt="o", color=CORES[s], capsize=8,
                     markersize=8, lw=2)
        ax2.annotate(f"{x.mean():.1f}", (i + 0.08, x.mean()), fontsize=10, va="center")
    ax2.axhline(anova["grand_mean"], color="gray", ls="--", lw=1.2)
    ax2.set_xticks(range(1, len(siglas) + 1))
    ax2.set_xticklabels([DISCIPLINAS[s][1] for s in siglas])
    ax2.set_xlim(0.5, len(siglas) + 0.5)
    ax2.set_ylabel("custo médio")
    ax2.set_title("Médias com IC de 99%")
    ax2.grid(axis="y", alpha=0.3)

    fig.suptitle(f"Q4 – Custo dos materiais por disciplina ({resultado_texto})", fontsize=12)
    fig.tight_layout()
    fig.savefig(caminho, dpi=150)
    plt.close(fig)


def main():
    if not ARQUIVO_DADOS.exists():
        print(f"Arquivo de dados não encontrado: {ARQUIVO_DADOS}", file=sys.stderr)
        return 1
    PASTA_RESULTADOS.mkdir(parents=True, exist_ok=True)

    grupos = organizar_custos(ler_planilha(ARQUIVO_DADOS))
    siglas = list(grupos)
    amostras = [grupos[s][0] for s in siglas]
    chaves = [DISCIPLINAS[s][0] for s in siglas]

    print("Q4 - ANOVA: custo dos materiais em química, ciência da computação e matemática")
    print(f"    {'disciplina':<23}{'n':>4}{'média':>10}{'mediana':>10}{'desvio':>10}")
    for s, x in zip(siglas, amostras):
        print(f"    {DISCIPLINAS[s][1]:<23}{x.size:>4}{x.mean():>10.2f}{np.median(x):>10.2f}"
              f"{x.std(ddof=1):>10.2f}")

    # condições de aplicabilidade (verificação prévia, com alfa = 5%)
    alfa_cond = 0.05
    normalidade = {}
    for chave, x in zip(chaves, amostras):
        sw = stats.shapiro(x)
        normalidade[chave] = {
            "test": "Shapiro-Wilk",
            "statistic": float(sw.statistic),
            "pvalue": float(sw.pvalue),
            "reject_normality": bool(sw.pvalue <= alfa_cond),
        }
    levene = stats.levene(*amostras, center="mean")
    print(f"\n    Condições (alfa = {alfa_cond}): Shapiro-Wilk por disciplina e Levene")
    for chave, r in normalidade.items():
        print(f"    {chave:<23} W = {r['statistic']:.4f}  p-valor = {r['pvalue']:.4f}  -> "
              f"{'rejeita' if r['reject_normality'] else 'não rejeita'} a normalidade")
    print(f"    Levene (center='mean'): W = {float(levene.statistic):.4f}  "
          f"p-valor = {float(levene.pvalue):.4f}")

    # ANOVA de uma via, com alfa = 1%
    anova = tabela_anova(amostras)
    f_scipy = stats.f_oneway(*amostras)
    f_critico = float(stats.f.ppf(1 - ALFA, anova["between"]["df"], anova["within"]["df"]))
    rejeita = float(f_scipy.pvalue) <= ALFA
    decisao = "Rejeitar H0" if rejeita else "Não rejeitar H0"
    kruskal = stats.kruskal(*amostras)  # checagem sem supor normalidade

    print(f"\n    ANOVA de uma via (alfa = {ALFA})")
    print("    Passo 1: H0: mu_química = mu_computação = mu_matemática   x   "
          "Ha: ao menos um par de médias difere")
    print(f"    {'fonte':<8}{'SQ':>13}{'gl':>5}{'QM':>13}")
    for fonte, chave in (("entre", "between"), ("dentro", "within")):
        linha = anova[chave]
        print(f"    {fonte:<8}{linha['ss']:>13.4f}{linha['df']:>5}{linha['ms']:>13.4f}")
    print(f"    {'total':<8}{anova['total']['ss']:>13.4f}{anova['total']['df']:>5}")
    print(f"    Passo 2: F = QM_entre / QM_dentro = {anova['F']:.4f}  "
          f"(F com {anova['between']['df']} e {anova['within']['df']} gl)")
    print(f"    Passo 3: p-valor = P(F > {anova['F']:.4f}) = {anova['pvalue']:.4f}")
    print(f"    Passo 4: p-valor {'<=' if rejeita else '>'} {ALFA} -> {decisao}"
          f"  (região crítica: F > {f_critico:.4f})")
    print(f"    Conferência scipy.stats.f_oneway: F = {float(f_scipy.statistic):.4f}, "
          f"p = {float(f_scipy.pvalue):.4f}")
    print(f"    eta² = {anova['eta_squared']:.4f} | Kruskal-Wallis (checagem): "
          f"H = {float(kruskal.statistic):.4f}, p = {float(kruskal.pvalue):.4f}")

    if rejeita:
        conclusao = ("Ao nível de 1%, os dados fornecem evidência suficiente para concluir que "
                     "os custos médios dos materiais não são todos iguais nas três disciplinas.")
    else:
        conclusao = ("Ao nível de 1%, os dados não fornecem evidência suficiente para concluir "
                     "que os custos médios dos materiais nas três disciplinas não são todos iguais; "
                     "as diferenças entre as médias amostrais são compatíveis com a variação "
                     "entre cursos de uma mesma disciplina.")
    print(f"    Conclusão: {conclusao}")

    texto = f"ANOVA: F = {anova['F']:.3f}, p = {anova['pvalue']:.3f}; α = {ALFA:.0%} → {decisao}"
    grafico_custos(grupos, anova, texto, ARQUIVO_GRAFICO)

    resultados = {
        "question": "Q4 - Teste ANOVA: custos de materiais",
        "data_file": str(ARQUIVO_DADOS.as_posix()),
        "groups": {DISCIPLINAS[s][0]: f"cursos com código {s}*" for s in siglas},
        "group_sizes": {c: int(x.size) for c, x in zip(chaves, amostras)},
        "group_means": {c: float(x.mean()) for c, x in zip(chaves, amostras)},
        "group_medians": {c: float(np.median(x)) for c, x in zip(chaves, amostras)},
        "group_stds": {c: float(x.std(ddof=1)) for c, x in zip(chaves, amostras)},
        "assumption_checks": {
            "alpha": alfa_cond,
            "normality": normalidade,
            "levene_center_mean": {
                "statistic": float(levene.statistic),
                "pvalue": float(levene.pvalue),
            },
        },
        "hypotheses": {
            "H0": "mu_quimica = mu_ciencia_da_computacao = mu_matematica",
            "Ha": "ao menos um par de médias difere",
        },
        "anova_table": anova,
        "anova_statistic": float(f_scipy.statistic),
        "anova_df": [anova["between"]["df"], anova["within"]["df"]],
        "anova_pvalue": float(f_scipy.pvalue),
        "alpha": ALFA,
        "f_critical": f_critico,
        "decision": decisao,
        "reject_h0": bool(rejeita),
        "kruskal_wallis_check": {
            "statistic": float(kruskal.statistic),
            "pvalue": float(kruskal.pvalue),
        },
        "conclusion": conclusao,
        "figure": ARQUIVO_GRAFICO.as_posix(),
    }
    with open(ARQUIVO_JSON, "w", encoding="utf-8") as f:
        json.dump(resultados, f, ensure_ascii=False, indent=2)

    print(f"\nArquivos gerados: {ARQUIVO_JSON.as_posix()} e {ARQUIVO_GRAFICO.as_posix()}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
