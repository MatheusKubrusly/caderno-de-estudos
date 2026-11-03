"""
GCC1625 - Inferência Estatística - Trabalho 02
Q1 - Um estilo diferente ajuda?

Pontuações de uma rodada de 75 jogadores, primeiro com os tacos originais e,
dois meses depois, com os tacos de estilo experimental. Como são os mesmos
jogadores nas duas medições, as amostras são pareadas e toda a análise é
feita sobre as diferenças d = original - novo.

No golfe, quanto menor a pontuação (número de tacadas), melhor. Por isso,
uma diferença positiva significa que o jogador melhorou com o taco novo.

Execução (a partir da raiz da entrega):
    python q1_tacos_golfe.py
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

ARQUIVO_DADOS = Path("data") / "golfe.csv"
PASTA_RESULTADOS = Path("resultados")
ARQUIVO_JSON = PASTA_RESULTADOS / "q1_resultados.json"
ARQUIVO_GRAFICO = PASTA_RESULTADOS / "q1_diferencas.png"

ALFA_NORMALIDADE = 0.05  # item (i)
ALFA_TESTE = 0.01        # item (ii)


def localizar_coluna(colunas, trechos):
    """Devolve a primeira coluna cujo nome contém algum dos trechos."""
    for col in colunas:
        if any(t in col.lower() for t in trechos):
            return col
    return None


def carregar_pontuacoes(caminho):
    # O golfe.csv usa ';' como separador e começa com BOM. Com sep=None o
    # pandas descobre o separador, e o encoding utf-8-sig descarta o BOM.
    df = pd.read_csv(caminho, sep=None, engine="python", encoding="utf-8-sig")
    df.columns = [str(c).strip() for c in df.columns]  # há espaços sobrando nos nomes

    col_original = localizar_coluna(df.columns, ["old", "orig", "antig"])
    col_novo = localizar_coluna(df.columns, ["new", "novo", "exper"])
    if col_original is None or col_novo is None:
        # se os nomes forem outros, uso as duas últimas colunas (a primeira é o ID)
        col_original, col_novo = df.columns[-2], df.columns[-1]

    pares = df[[col_original, col_novo]].apply(pd.to_numeric, errors="coerce").dropna()
    original = pares[col_original].to_numpy(dtype=float)
    novo = pares[col_novo].to_numpy(dtype=float)
    return original, novo, col_original, col_novo


def testes_normalidade(x, alfa):
    """Shapiro-Wilk e D'Agostino-Pearson (H0: os dados vêm de uma normal)."""
    sw = stats.shapiro(x)
    k2 = stats.normaltest(x)
    return {
        "shapiro_wilk": {
            "statistic": float(sw.statistic),
            "pvalue": float(sw.pvalue),
            "reject_normality": bool(sw.pvalue <= alfa),
        },
        "dagostino_pearson": {
            "statistic": float(k2.statistic),
            "pvalue": float(k2.pvalue),
            "reject_normality": bool(k2.pvalue <= alfa),
        },
    }


def grafico_diferencas(dif, normalidade, caminho):
    n = dif.size
    media, dp = dif.mean(), dif.std(ddof=1)
    fig, eixos = plt.subplots(1, 3, figsize=(15, 4.8))

    # (a) histograma com a curva normal de mesma média e desvio padrão
    ax = eixos[0]
    bordas = np.arange(dif.min() - 0.5, dif.max() + 1.5, 1.0)  # as diferenças são inteiras
    ax.hist(dif, bins=bordas, density=True, color="#6a9fd4", edgecolor="white",
            label="diferenças observadas")
    xs = np.linspace(dif.min() - 3, dif.max() + 3, 300)
    ax.plot(xs, stats.norm.pdf(xs, media, dp), color="#c0392b", lw=2,
            label="normal ajustada")
    ax.axvline(0, color="gray", ls=":", lw=1.5, label="d = 0 (sem efeito)")
    ax.axvline(media, color="black", ls="--", lw=1.3, label=f"média = {media:.2f}")
    ax.set_title("Histograma das diferenças")
    ax.set_xlabel("d = original − novo (tacadas)")
    ax.set_ylabel("densidade")
    ax.legend(fontsize=8, loc="upper right")
    ax.grid(alpha=0.3)

    # (b) gráfico Q-Q contra a normal
    ax = eixos[1]
    (teoricos, ordenados), (inclinacao, intercepto, _) = stats.probplot(dif, dist="norm")
    ax.scatter(teoricos, ordenados, s=18, color="#2c5f8a", alpha=0.8)
    ax.plot(teoricos, inclinacao * teoricos + intercepto, color="#c0392b", lw=1.8)
    ax.set_title("Gráfico Q-Q (normal)")
    ax.set_xlabel("quantis teóricos da N(0, 1)")
    ax.set_ylabel("diferenças ordenadas")
    ax.grid(alpha=0.3)

    # (c) boxplot com os pontos (jitter leve só para enxergar as repetições)
    ax = eixos[2]
    ax.boxplot(dif, widths=0.45, showmeans=True,
               meanprops={"marker": "D", "markerfacecolor": "black",
                          "markeredgecolor": "black", "markersize": 6})
    rng = np.random.default_rng(1625)
    ax.scatter(1 + rng.uniform(-0.12, 0.12, n), dif, s=14, alpha=0.45, color="#2c5f8a")
    ax.axhline(0, color="gray", ls=":", lw=1.5)
    ax.set_xticks([1])
    ax.set_xticklabels([f"diferenças (n = {n})"])
    ax.set_ylabel("d = original − novo (tacadas)")
    ax.set_title("Boxplot das diferenças (◆ = média)")
    ax.grid(alpha=0.3)

    sw = normalidade["shapiro_wilk"]
    k2 = normalidade["dagostino_pearson"]
    fig.suptitle(
        "Q1 – Diagnóstico das diferenças de pontuação "
        f"(Shapiro-Wilk: W = {sw['statistic']:.4f}, p = {sw['pvalue']:.4f}; "
        f"D'Agostino-Pearson: K² = {k2['statistic']:.2f}, p = {k2['pvalue']:.4f})",
        fontsize=11,
    )
    fig.tight_layout()
    fig.savefig(caminho, dpi=150)
    plt.close(fig)


def main():
    if not ARQUIVO_DADOS.exists():
        print(f"Arquivo de dados não encontrado: {ARQUIVO_DADOS}", file=sys.stderr)
        return 1
    PASTA_RESULTADOS.mkdir(parents=True, exist_ok=True)

    original, novo, col_original, col_novo = carregar_pontuacoes(ARQUIVO_DADOS)
    dif = original - novo  # d > 0: o jogador fez menos tacadas com o taco novo
    n = int(dif.size)
    media_d = float(dif.mean())
    dp_d = float(dif.std(ddof=1))
    ep = dp_d / np.sqrt(n)

    print("Q1 - Tacos de golfe (amostras pareadas)")
    print(f"Colunas usadas: original = '{col_original}', novo = '{col_novo}'")
    print(f"n = {n} jogadores")
    print(f"Média com taco original: {original.mean():.4f} | com taco novo: {novo.mean():.4f}")
    print(f"Diferenças d = original - novo: média = {media_d:.4f}, "
          f"desvio padrão = {dp_d:.4f}, erro padrão = {ep:.4f}")
    print(f"Jogadores que melhoraram / empataram / pioraram: "
          f"{int((dif > 0).sum())} / {int((dif == 0).sum())} / {int((dif < 0).sum())}")

    # item (i): normalidade das diferenças
    normalidade = testes_normalidade(dif, ALFA_NORMALIDADE)
    sw = normalidade["shapiro_wilk"]
    k2 = normalidade["dagostino_pearson"]
    assimetria = float(stats.skew(dif, bias=False))
    curtose = float(stats.kurtosis(dif, bias=False))  # curtose em excesso (normal = 0)

    print(f"\n(i) Normalidade das diferenças (alfa = {ALFA_NORMALIDADE})")
    print("    H0: as diferenças vêm de uma distribuição normal")
    for nome, r in (("Shapiro-Wilk", sw), ("D'Agostino-Pearson", k2)):
        decisao = "rejeita H0" if r["reject_normality"] else "não rejeita H0"
        print(f"    {nome:<19} estatística = {r['statistic']:.4f}  p-valor = {r['pvalue']:.6f}  -> {decisao}")
    print(f"    assimetria = {assimetria:.4f} | curtose em excesso = {curtose:.4f}")
    print(f"    valores distintos entre as {n} diferenças: {np.unique(dif).size}")

    # item (ii): t-teste pareado
    # Ha: mu_d > 0 (o taco novo reduz a pontuação média) -> unicaudal à direita
    gl = n - 1
    t_obs = media_d / ep
    p_valor = float(stats.t.sf(t_obs, df=gl))
    t_critico = float(stats.t.ppf(1 - ALFA_TESTE, df=gl))
    rejeita = p_valor <= ALFA_TESTE

    # conferência com a função pronta do scipy (mesma alternativa)
    conferencia = stats.ttest_rel(original, novo, alternative="greater")

    # estimativa do efeito: limite inferior de confiança de 99% (coerente com o
    # teste unilateral) e, como referência, o IC bilateral de 99%
    limite_inf_99 = media_d - t_critico * ep
    t_bilateral = stats.t.ppf(1 - ALFA_TESTE / 2, df=gl)
    ic99 = [media_d - t_bilateral * ep, media_d + t_bilateral * ep]

    # checagem não paramétrica, só para confirmar que a conclusão não depende
    # da suposição de normalidade (diferenças nulas são descartadas)
    with warnings.catch_warnings():
        warnings.simplefilter("ignore")
        wilcoxon = stats.wilcoxon(original, novo, alternative="greater")

    decisao = "Rejeitar H0" if rejeita else "Não rejeitar H0"
    if rejeita:
        conclusao = (
            "Ao nível de 1%, os dados fornecem evidência de que o novo estilo de taco "
            "reduz a pontuação média dos jogadores, ou seja, ajuda no desempenho."
        )
    else:
        conclusao = (
            "Ao nível de 1%, os dados não fornecem evidência suficiente de que o novo "
            "estilo de taco reduz a pontuação média dos jogadores."
        )

    print(f"\n(ii) t-teste pareado, unicaudal à direita (alfa = {ALFA_TESTE})")
    print("    Passo 1: H0: mu_d <= 0   x   Ha: mu_d > 0   (d = original - novo)")
    print(f"    Passo 2: t = d_barra / (s_d / sqrt(n)) = {media_d:.4f} / {ep:.4f} = {t_obs:.4f}"
          f"  (distribuição t com {gl} gl)")
    print(f"    Passo 3: p-valor = P(T_{gl} > {t_obs:.4f}) = {p_valor:.4e}")
    print(f"    Passo 4: p-valor {'<=' if rejeita else '>'} {ALFA_TESTE} -> {decisao}"
          f"  (região crítica: t > {t_critico:.4f})")
    print(f"    Conferência scipy.stats.ttest_rel: t = {float(conferencia.statistic):.4f}, "
          f"p = {float(conferencia.pvalue):.4e}")
    print(f"    Redução média estimada: {media_d:.4f} tacadas; limite inferior de 99%: "
          f"{limite_inf_99:.4f}; IC bilateral de 99%: [{ic99[0]:.4f}, {ic99[1]:.4f}]")
    print(f"    Wilcoxon (checagem): estatística = {float(wilcoxon.statistic):.1f}, "
          f"p = {float(wilcoxon.pvalue):.4e}")
    print(f"    Conclusão: {conclusao}")

    grafico_diferencas(dif, normalidade, ARQUIVO_GRAFICO)

    resultados = {
        "question": "Q1 - Um estilo diferente ajuda?",
        "data_file": str(ARQUIVO_DADOS.as_posix()),
        "columns": {"original_clubs": col_original, "new_clubs": col_novo},
        "difference_definition": "d = pontuação com taco original - pontuação com taco novo "
                                 "(d > 0 indica menos tacadas com o taco novo)",
        "n": n,
        "mean_original": float(original.mean()),
        "mean_new": float(novo.mean()),
        "mean_difference": media_d,
        "std_difference": dp_d,
        "standard_error": float(ep),
        "skewness_difference": assimetria,
        "excess_kurtosis_difference": curtose,
        "normality_alpha": ALFA_NORMALIDADE,
        "normality_test": "Shapiro-Wilk",
        "normality_statistic": sw["statistic"],
        "normality_pvalue": sw["pvalue"],
        "normality_decision": (
            "Rejeitar H0 (normalidade) ao nível de 5%" if sw["reject_normality"]
            else "Não rejeitar H0 (normalidade) ao nível de 5%"
        ),
        "normality_tests": normalidade,
        "test": "t-teste pareado (unicaudal à direita)",
        "alpha": ALFA_TESTE,
        "hypotheses": {
            "H0": "mu_d <= 0 (o taco novo não reduz a pontuação média)",
            "Ha": "mu_d > 0 (o taco novo reduz a pontuação média)",
        },
        "alternative": "greater",
        "t_statistic": float(t_obs),
        "df": int(gl),
        "pvalue": p_valor,
        "t_critical": t_critico,
        "rejection_region": f"t > {t_critico:.4f}",
        "scipy_ttest_rel_check": {
            "statistic": float(conferencia.statistic),
            "pvalue": float(conferencia.pvalue),
        },
        "lower_confidence_bound_99": float(limite_inf_99),
        "ci99_mean_difference": [float(ic99[0]), float(ic99[1])],
        "wilcoxon_check": {
            "statistic": float(wilcoxon.statistic),
            "pvalue": float(wilcoxon.pvalue),
        },
        "decision": decisao,
        "reject_h0": bool(rejeita),
        "conclusion": conclusao,
        "figure": str(ARQUIVO_GRAFICO.as_posix()),
    }
    with open(ARQUIVO_JSON, "w", encoding="utf-8") as f:
        json.dump(resultados, f, ensure_ascii=False, indent=2)

    print(f"\nArquivos gerados: {ARQUIVO_JSON.as_posix()} e {ARQUIVO_GRAFICO.as_posix()}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
