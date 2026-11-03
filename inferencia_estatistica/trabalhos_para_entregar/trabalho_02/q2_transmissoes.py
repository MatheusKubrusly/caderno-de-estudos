"""
GCC1625 - Inferência Estatística - Trabalho 02
Q2 - Transmissões: automática versus manual

Compara o consumo (mpg, milhas por galão) dos carros do data/mtcars.csv com
transmissão automática (am = 0) e manual (am = 1). Os dois grupos são formados
por carros diferentes, então as amostras são independentes.

Execução (a partir da raiz da entrega):
    python q2_transmissoes.py
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

ARQUIVO_DADOS = Path("data") / "mtcars.csv"
PASTA_RESULTADOS = Path("resultados")
ARQUIVO_JSON = PASTA_RESULTADOS / "q2_resultados.json"
ARQUIVO_BOXPLOT = PASTA_RESULTADOS / "q2_boxplot.png"
ARQUIVO_HISTOGRAMAS = PASTA_RESULTADOS / "q2_histogramas.png"
ARQUIVO_QQ = PASTA_RESULTADOS / "q2_qqplot.png"

ALFA = 0.05        # itens (ii) e (iv)
CONFIANCA = 0.95   # item (iii)

COR = {"automatic": "#e07b39", "manual": "#3a78b5"}
NOME = {"automatic": "Automática (am = 0)", "manual": "Manual (am = 1)"}
NOME_CURTO = {"automatic": "automática", "manual": "manual"}


def carregar_grupos(caminho):
    df = pd.read_csv(caminho)
    df.columns = [str(c).strip() for c in df.columns]
    df = df.dropna(subset=["am", "mpg"])
    automatico = df.loc[df["am"] == 0, "mpg"].to_numpy(dtype=float)
    manual = df.loc[df["am"] == 1, "mpg"].to_numpy(dtype=float)
    return automatico, manual


def resumo(x):
    q1, q3 = np.percentile(x, [25, 75])
    return {
        "n": int(x.size),
        "mean": float(x.mean()),
        "median": float(np.median(x)),
        "std": float(x.std(ddof=1)),
        "min": float(x.min()),
        "q1": float(q1),
        "q3": float(q3),
        "max": float(x.max()),
    }


def testes_normalidade(x, alfa):
    """Shapiro-Wilk e D'Agostino-Pearson (H0: a amostra vem de uma normal)."""
    sw = stats.shapiro(x)
    with warnings.catch_warnings():
        # versões mais antigas do scipy avisam que a parte de curtose do teste
        # de D'Agostino foi pensada para n >= 20 (o grupo manual tem 13 carros);
        # essa limitação é comentada no RESPOSTAS.md
        warnings.simplefilter("ignore", UserWarning)
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
            "small_sample_note": bool(x.size < 20),
        },
    }


def grafico_boxplot(grupos, caminho):
    fig, ax = plt.subplots(figsize=(7.5, 5.5))
    chaves = list(grupos)
    dados = [grupos[k] for k in chaves]
    caixas = ax.boxplot(dados, widths=0.5, patch_artist=True, showmeans=True,
                        meanprops={"marker": "D", "markerfacecolor": "black",
                                   "markeredgecolor": "black", "markersize": 7})
    rng = np.random.default_rng(1625)
    for i, (k, caixa) in enumerate(zip(chaves, caixas["boxes"]), start=1):
        caixa.set_facecolor(COR[k])
        caixa.set_alpha(0.35)
        x = grupos[k]
        ax.scatter(i + rng.uniform(-0.1, 0.1, x.size), x, s=26, color=COR[k],
                   edgecolor="black", linewidth=0.4, zorder=3)
        ax.annotate(f"média = {x.mean():.2f}", (i + 0.27, x.mean()), fontsize=9,
                    va="center")
    ax.set_xticks(range(1, len(chaves) + 1))
    ax.set_xticklabels([f"{NOME[k]}\nn = {grupos[k].size}" for k in chaves])
    ax.set_ylabel("consumo (milhas por galão)")
    ax.set_title("Q2 – Consumo de combustível por tipo de transmissão\n"
                 "(◆ = média; pontos = carros)")
    ax.grid(axis="y", alpha=0.3)
    fig.tight_layout()
    fig.savefig(caminho, dpi=150)
    plt.close(fig)


def grafico_histogramas(grupos, caminho):
    todos = np.concatenate(list(grupos.values()))
    largura = 2.0
    bordas = np.arange(np.floor(todos.min() / largura) * largura,
                       np.ceil(todos.max() / largura) * largura + largura, largura)
    xs = np.linspace(bordas[0] - 4, bordas[-1] + 4, 300)

    fig, ax = plt.subplots(figsize=(9, 5.5))
    for k, x in grupos.items():
        ax.hist(x, bins=bordas, alpha=0.5, color=COR[k], edgecolor="white",
                label=f"{NOME_CURTO[k].capitalize()}, n = {x.size}")
        # curva normal com a média e o desvio padrão do grupo, na escala de contagens
        ax.plot(xs, x.size * largura * stats.norm.pdf(xs, x.mean(), x.std(ddof=1)),
                color=COR[k], lw=2, ls="--")
        ax.axvline(x.mean(), color=COR[k], lw=2.2,
                   label=f"média {NOME_CURTO[k]} = {x.mean():.2f}")
    ax.set_xlabel("consumo (milhas por galão)")
    ax.set_ylabel("número de carros")
    ax.set_title("Q2 – Histogramas sobrepostos do consumo (mpg)\n"
                 "linhas tracejadas: normal ajustada a cada grupo")
    ax.legend(fontsize=9)
    ax.grid(alpha=0.3)
    fig.tight_layout()
    fig.savefig(caminho, dpi=150)
    plt.close(fig)


def grafico_qq(grupos, normalidade, caminho):
    fig, eixos = plt.subplots(1, 2, figsize=(11, 4.8))
    for ax, (k, x) in zip(eixos, grupos.items()):
        (teoricos, ordenados), (incl, inter, _) = stats.probplot(x, dist="norm")
        ax.scatter(teoricos, ordenados, color=COR[k], edgecolor="black",
                   linewidth=0.4, s=30, zorder=3)
        ax.plot(teoricos, incl * teoricos + inter, color="black", lw=1.5)
        sw = normalidade[k]["shapiro_wilk"]
        ax.set_title(f"{NOME[k]}, n = {x.size}\nShapiro-Wilk: W = {sw['statistic']:.3f}, "
                     f"p = {sw['pvalue']:.3f}")
        ax.set_xlabel("quantis teóricos da N(0, 1)")
        ax.set_ylabel("mpg ordenado")
        ax.grid(alpha=0.3)
    fig.suptitle("Q2 – Gráficos Q-Q do consumo em cada grupo")
    fig.tight_layout()
    fig.savefig(caminho, dpi=150)
    plt.close(fig)


def main():
    if not ARQUIVO_DADOS.exists():
        print(f"Arquivo de dados não encontrado: {ARQUIVO_DADOS}", file=sys.stderr)
        return 1
    PASTA_RESULTADOS.mkdir(parents=True, exist_ok=True)

    automatico, manual = carregar_grupos(ARQUIVO_DADOS)
    grupos = {"automatic": automatico, "manual": manual}
    descritivas = {k: resumo(x) for k, x in grupos.items()}

    # item (i): análise exploratória
    print("Q2 - Consumo (mpg): transmissão automática x manual")
    print("\n(i) Estatísticas descritivas")
    print(f"    {'grupo':<11}{'n':>4}{'média':>9}{'mediana':>9}{'desvio':>9}{'mín':>7}{'máx':>7}")
    for k, d in descritivas.items():
        print(f"    {k:<11}{d['n']:>4}{d['mean']:>9.3f}{d['median']:>9.2f}{d['std']:>9.3f}"
              f"{d['min']:>7.1f}{d['max']:>7.1f}")
    grafico_boxplot(grupos, ARQUIVO_BOXPLOT)
    grafico_histogramas(grupos, ARQUIVO_HISTOGRAMAS)

    # item (ii): normalidade de cada grupo
    normalidade = {k: testes_normalidade(x, ALFA) for k, x in grupos.items()}
    grafico_qq(grupos, normalidade, ARQUIVO_QQ)
    print(f"\n(ii) Testes de normalidade (alfa = {ALFA}; H0: a amostra vem de uma normal)")
    for k, testes in normalidade.items():
        for nome_teste, r in testes.items():
            decisao = "rejeita H0" if r["reject_normality"] else "não rejeita H0"
            print(f"    {k:<10} {nome_teste:<18} estatística = {r['statistic']:.4f}  "
                  f"p-valor = {r['pvalue']:.4f}  -> {decisao}")

    # item (iii): IC de Welch para mu_automática - mu_manual
    n_a, n_m = automatico.size, manual.size
    v_a = automatico.var(ddof=1) / n_a
    v_m = manual.var(ddof=1) / n_m
    erro_padrao = np.sqrt(v_a + v_m)
    gl = (v_a + v_m) ** 2 / (v_a ** 2 / (n_a - 1) + v_m ** 2 / (n_m - 1))  # Welch-Satterthwaite
    t_ic = stats.t.ppf(1 - (1 - CONFIANCA) / 2, df=gl)
    diferenca = automatico.mean() - manual.mean()
    margem = t_ic * erro_padrao
    ic95 = [float(diferenca - margem), float(diferenca + margem)]
    print(f"\n(iii) IC de {CONFIANCA:.0%} (Welch) para mu_automática - mu_manual")
    print(f"    diferença observada = {diferenca:.4f} mpg | erro padrão = {erro_padrao:.4f}")
    print(f"    gl (Welch-Satterthwaite) = {gl:.4f} | t crítico = {t_ic:.4f} | margem = {margem:.4f}")
    print(f"    IC95% = [{ic95[0]:.4f}, {ic95[1]:.4f}]")

    # item (iv): teste de Welch unicaudal. Ha: mu_manual > mu_automática
    t_obs = (manual.mean() - automatico.mean()) / erro_padrao
    p_valor = float(stats.t.sf(t_obs, df=gl))
    t_critico = float(stats.t.ppf(1 - ALFA, df=gl))
    rejeita = p_valor <= ALFA
    conferencia = stats.ttest_ind(manual, automatico, equal_var=False, alternative="greater")

    # só para reforçar a escolha do Welch: as variâncias parecem iguais?
    levene = stats.levene(automatico, manual, center="mean")

    decisao = "Rejeitar H0" if rejeita else "Não rejeitar H0"
    if rejeita:
        conclusao = (
            "Ao nível de 5%, há evidência de que os carros com transmissão manual têm, em média, "
            "maior rendimento (mpg) do que os carros com transmissão automática."
        )
    else:
        conclusao = (
            "Ao nível de 5%, não há evidência suficiente de que os carros manuais tenham "
            "rendimento médio maior que os automáticos."
        )

    print(f"\n(iv) Teste t de Welch, unicaudal à direita (alfa = {ALFA})")
    print("    Passo 1: H0: mu_manual - mu_automática <= 0   x   Ha: mu_manual - mu_automática > 0")
    print(f"    Passo 2: t = ({manual.mean():.4f} - {automatico.mean():.4f}) / {erro_padrao:.4f} "
          f"= {t_obs:.4f}  (t com {gl:.2f} gl)")
    print(f"    Passo 3: p-valor = P(T > {t_obs:.4f}) = {p_valor:.6f}")
    print(f"    Passo 4: p-valor {'<=' if rejeita else '>'} {ALFA} -> {decisao}"
          f"  (região crítica: t > {t_critico:.4f})")
    print(f"    Conferência scipy.stats.ttest_ind(equal_var=False): t = {float(conferencia.statistic):.4f}, "
          f"p = {float(conferencia.pvalue):.6f}")
    print(f"    Levene (center='mean') para as variâncias: W = {float(levene.statistic):.4f}, "
          f"p = {float(levene.pvalue):.4f}")
    print(f"    Conclusão: {conclusao}")

    resultados = {
        "question": "Q2 - Transmissões: automática versus manual",
        "data_file": str(ARQUIVO_DADOS.as_posix()),
        "groups": {"automatic": "am = 0", "manual": "am = 1"},
        "n_automatic": int(n_a),
        "n_manual": int(n_m),
        "descriptive_statistics": descritivas,
        "normality_alpha": ALFA,
        "normality_tests": normalidade,
        "welch_ci95": ic95,
        "ci_parameter": "mu_automatic - mu_manual",
        "ci_point_estimate": float(diferenca),
        "ci_standard_error": float(erro_padrao),
        "ci_t_critical": float(t_ic),
        "ci_margin_of_error": float(margem),
        "test": "t de Welch para duas amostras independentes (unicaudal à direita)",
        "alpha": ALFA,
        "hypotheses": {
            "H0": "mu_manual - mu_automatic <= 0",
            "Ha": "mu_manual - mu_automatic > 0 (carros manuais têm mpg médio maior)",
        },
        "difference_tested": "mean_manual - mean_automatic",
        "alternative": "greater",
        "t_statistic": float(t_obs),
        "df": float(gl),
        "pvalue": p_valor,
        "t_critical": t_critico,
        "rejection_region": f"t > {t_critico:.4f}",
        "scipy_ttest_ind_check": {
            "statistic": float(conferencia.statistic),
            "pvalue": float(conferencia.pvalue),
        },
        "levene_variance_check": {
            "center": "mean",
            "statistic": float(levene.statistic),
            "pvalue": float(levene.pvalue),
        },
        "decision": decisao,
        "reject_h0": bool(rejeita),
        "conclusion": conclusao,
        "figures": [ARQUIVO_BOXPLOT.as_posix(), ARQUIVO_HISTOGRAMAS.as_posix(),
                    ARQUIVO_QQ.as_posix()],
    }
    with open(ARQUIVO_JSON, "w", encoding="utf-8") as f:
        json.dump(resultados, f, ensure_ascii=False, indent=2)

    print(f"\nArquivos gerados: {ARQUIVO_JSON.as_posix()}, {ARQUIVO_BOXPLOT.as_posix()}, "
          f"{ARQUIVO_HISTOGRAMAS.as_posix()} e {ARQUIVO_QQ.as_posix()}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
