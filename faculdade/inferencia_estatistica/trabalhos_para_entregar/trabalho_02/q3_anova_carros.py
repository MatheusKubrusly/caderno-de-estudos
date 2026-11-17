"""
GCC1625 - Inferência Estatística - Trabalho 02
Q3 - Teste ANOVA: categorias de carros (Situação-problema I)

Estimativas de milhas por galão (MPG) de modelos de 1993 em três categorias:
compactos, médios e grandes/vans (Consumer Reports, abril de 1993). Os valores
são os da Listagem 1 do enunciado (mesmos da Tabela 3).

Execução (a partir da raiz da entrega):
    python q3_anova_carros.py
"""

import json
import sys
import warnings
from pathlib import Path

import matplotlib

matplotlib.use("Agg")  # só salva as figuras em arquivo (sem abrir janela)
import matplotlib.pyplot as plt
import numpy as np
from scipy import stats

PASTA_RESULTADOS = Path("resultados")
ARQUIVO_JSON = PASTA_RESULTADOS / "q3_resultados.json"
ARQUIVO_GRAFICO = PASTA_RESULTADOS / "q3_grupos.png"
ARQUIVO_QQ = PASTA_RESULTADOS / "q3_qqplots.png"

ALFA = 0.05

# Listagem 1 do enunciado
compactos = np.array([20, 25, 25, 20, 22, 22, 24, 26, 20, 24,
                      24, 23, 20, 23, 21, 21])
medios = np.array([18, 19, 22, 22, 19, 16, 21, 21, 21, 20, 17,
                   18, 18, 17, 19, 19, 18, 21, 23, 19, 22, 20])
grandes = np.array([19, 16, 16, 17, 20, 20, 20, 18, 18, 19, 19,
                    15, 18, 17, 15, 18, 17, 18, 18, 17])

GRUPOS = {"compactos": compactos, "medios": medios, "grandes": grandes}
ROTULOS = {"compactos": "Compactos", "medios": "Médios", "grandes": "Grandes/Vans"}
CORES = {"compactos": "#3a78b5", "medios": "#5aa469", "grandes": "#d1603d"}


def formata_p(p):
    return f"{p:.2e}" if p < 1e-4 else f"{p:.4f}"


def testes_normalidade(x, alfa):
    """Shapiro-Wilk e D'Agostino-Pearson (H0: a amostra vem de uma normal)."""
    sw = stats.shapiro(x)
    with warnings.catch_warnings():
        # o scipy pode avisar que a parte de curtose do D'Agostino pede n >= 20
        # (o grupo de compactos tem 16 carros); isso é discutido no RESPOSTAS.md
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


def tabela_anova(grupos):
    """Monta a tabela da ANOVA de uma via a partir das somas de quadrados."""
    todos = np.concatenate(grupos)
    N, k = todos.size, len(grupos)
    media_geral = todos.mean()
    sq_entre = sum(g.size * (g.mean() - media_geral) ** 2 for g in grupos)
    sq_dentro = sum(((g - g.mean()) ** 2).sum() for g in grupos)
    gl_entre, gl_dentro = k - 1, N - k
    qm_entre = sq_entre / gl_entre
    qm_dentro = sq_dentro / gl_dentro
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


def comparacoes_tukey(grupos, nomes, confianca=0.95):
    res = stats.tukey_hsd(*grupos)
    ic = res.confidence_interval(confidence_level=confianca)
    pares = []
    for i in range(len(nomes)):
        for j in range(i + 1, len(nomes)):
            pares.append({
                "pair": f"{nomes[i]} - {nomes[j]}",
                "mean_difference": float(res.statistic[i, j]),
                "pvalue": float(res.pvalue[i, j]),
                "ci95": [float(ic.low[i, j]), float(ic.high[i, j])],
                "significant_5pct": bool(res.pvalue[i, j] <= 0.05),
            })
    return pares


def grafico_grupos(grupos, anova, caminho):
    chaves = list(grupos)
    fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(13, 5.3))

    # (a) boxplots com os pontos de cada modelo (jitter por causa dos empates)
    caixas = ax1.boxplot([grupos[k] for k in chaves], widths=0.5, patch_artist=True,
                         showmeans=True,
                         meanprops={"marker": "D", "markerfacecolor": "black",
                                    "markeredgecolor": "black", "markersize": 6})
    rng = np.random.default_rng(1625)
    for i, (k, caixa) in enumerate(zip(chaves, caixas["boxes"]), start=1):
        caixa.set_facecolor(CORES[k])
        caixa.set_alpha(0.3)
        x = grupos[k]
        ax1.scatter(i + rng.uniform(-0.13, 0.13, x.size), x + rng.uniform(-0.12, 0.12, x.size),
                    s=22, color=CORES[k], edgecolor="black", linewidth=0.3, zorder=3)
    ax1.set_xticks(range(1, len(chaves) + 1))
    ax1.set_xticklabels([f"{ROTULOS[k]}\nn = {grupos[k].size}" for k in chaves])
    ax1.set_ylabel("MPG (milhas por galão)")
    ax1.set_title("Distribuição do MPG por categoria (◆ = média)")
    ax1.grid(axis="y", alpha=0.3)

    # (b) médias com intervalos de confiança de 95% (t) e média geral
    for i, k in enumerate(chaves, start=1):
        x = grupos[k]
        margem = stats.t.ppf(0.975, x.size - 1) * x.std(ddof=1) / np.sqrt(x.size)
        ax2.errorbar(i, x.mean(), yerr=margem, fmt="o", color=CORES[k], capsize=8,
                     markersize=8, lw=2)
        ax2.annotate(f"{x.mean():.2f}", (i + 0.08, x.mean()), fontsize=10, va="center")
    ax2.axhline(anova["grand_mean"], color="gray", ls="--", lw=1.3,
                label=f"média geral = {anova['grand_mean']:.2f}")
    ax2.set_xticks(range(1, len(chaves) + 1))
    ax2.set_xticklabels([ROTULOS[k] for k in chaves])
    ax2.set_xlim(0.5, len(chaves) + 0.5)
    ax2.set_ylabel("MPG médio")
    ax2.set_title("Médias com IC de 95%")
    ax2.legend(loc="upper right")
    ax2.grid(axis="y", alpha=0.3)

    fig.suptitle(f"Q3 – MPG por categoria de carro (ANOVA: F = {anova['F']:.2f}, "
                 f"p = {anova['pvalue']:.2e})", fontsize=12)
    fig.tight_layout()
    fig.savefig(caminho, dpi=150)
    plt.close(fig)


def grafico_qq(grupos, normalidade, caminho):
    fig, eixos = plt.subplots(1, len(grupos), figsize=(15, 4.6))
    for ax, (k, x) in zip(eixos, grupos.items()):
        (teoricos, ordenados), (incl, inter, _) = stats.probplot(x, dist="norm")
        ax.scatter(teoricos, ordenados, color=CORES[k], edgecolor="black", linewidth=0.3,
                   s=28, zorder=3)
        ax.plot(teoricos, incl * teoricos + inter, color="black", lw=1.4)
        sw = normalidade[k]["shapiro_wilk"]
        ax.set_title(f"{ROTULOS[k]} (n = {x.size})\nShapiro-Wilk: W = {sw['statistic']:.3f}, "
                     f"p = {sw['pvalue']:.3f}")
        ax.set_xlabel("quantis teóricos da N(0, 1)")
        ax.set_ylabel("MPG ordenado")
        ax.grid(alpha=0.3)
    fig.suptitle("Q3 – Gráficos Q-Q por categoria")
    fig.tight_layout()
    fig.savefig(caminho, dpi=150)
    plt.close(fig)


def main():
    PASTA_RESULTADOS.mkdir(parents=True, exist_ok=True)
    nomes = list(GRUPOS)
    amostras = [GRUPOS[k].astype(float) for k in nomes]

    print("Q3 - ANOVA: MPG de carros de 1993 por categoria")
    print(f"    {'grupo':<11}{'n':>4}{'média':>9}{'desvio':>9}{'variância':>11}")
    for k, x in zip(nomes, amostras):
        print(f"    {k:<11}{x.size:>4}{x.mean():>9.3f}{x.std(ddof=1):>9.3f}{x.var(ddof=1):>11.3f}")

    # item (i): condições de aplicabilidade
    normalidade = {k: testes_normalidade(x, ALFA) for k, x in zip(nomes, amostras)}
    print(f"\n(i) Normalidade de cada grupo (alfa = {ALFA}; H0: a amostra vem de uma normal)")
    for k, testes in normalidade.items():
        for nome_teste, r in testes.items():
            decisao = "rejeita H0" if r["reject_normality"] else "não rejeita H0"
            print(f"    {k:<10} {nome_teste:<18} estatística = {r['statistic']:.4f}  "
                  f"p-valor = {r['pvalue']:.4f}  -> {decisao}")

    # Levene com centralização na média (versão usada nas notas de aula); a versão
    # com a mediana (Brown-Forsythe) entra só como conferência
    levene = stats.levene(*amostras, center="mean")
    levene_mediana = stats.levene(*amostras, center="median")
    variancias_iguais = levene.pvalue > ALFA
    desvios = [x.std(ddof=1) for x in amostras]
    razao_dp = max(desvios) / min(desvios)
    print("\n    Teste de Levene (H0: as variâncias populacionais são iguais)")
    print(f"    center='mean':   W = {float(levene.statistic):.4f}  p-valor = {float(levene.pvalue):.4f}"
          f"  -> {'não rejeita H0' if variancias_iguais else 'rejeita H0'}")
    print(f"    center='median': W = {float(levene_mediana.statistic):.4f}  "
          f"p-valor = {float(levene_mediana.pvalue):.4f}")
    print(f"    razão entre o maior e o menor desvio padrão = {razao_dp:.3f}")

    # item (ii): ANOVA de uma via
    anova = tabela_anova(amostras)
    f_scipy = stats.f_oneway(*amostras)
    f_critico = float(stats.f.ppf(1 - ALFA, anova["between"]["df"], anova["within"]["df"]))
    rejeita = float(f_scipy.pvalue) <= ALFA
    decisao = "Rejeitar H0" if rejeita else "Não rejeitar H0"

    print(f"\n(ii) ANOVA de uma via (alfa = {ALFA})")
    print("    Passo 1: H0: mu_compactos = mu_médios = mu_grandes   x   "
          "Ha: ao menos um par de médias difere")
    print(f"    {'fonte':<8}{'SQ':>11}{'gl':>5}{'QM':>11}")
    for fonte, chave in (("entre", "between"), ("dentro", "within")):
        linha = anova[chave]
        print(f"    {fonte:<8}{linha['ss']:>11.4f}{linha['df']:>5}{linha['ms']:>11.4f}")
    print(f"    {'total':<8}{anova['total']['ss']:>11.4f}{anova['total']['df']:>5}")
    print(f"    Passo 2: F = QM_entre / QM_dentro = {anova['F']:.4f}  "
          f"(F com {anova['between']['df']} e {anova['within']['df']} gl)")
    print(f"    Passo 3: p-valor = P(F > {anova['F']:.4f}) = {anova['pvalue']:.4e}")
    print(f"    Passo 4: p-valor {'<=' if rejeita else '>'} {ALFA} -> {decisao}"
          f"  (região crítica: F > {f_critico:.4f})")
    print(f"    Conferência scipy.stats.f_oneway: F = {float(f_scipy.statistic):.4f}, "
          f"p = {float(f_scipy.pvalue):.4e}")
    print(f"    eta² = SQ_entre / SQ_total = {anova['eta_squared']:.4f}")

    tukey = comparacoes_tukey(amostras, nomes) if rejeita else []
    if tukey:
        print("\n    Post-hoc de Tukey HSD (IC de 95% para a diferença de médias)")
        for c in tukey:
            print(f"    {c['pair']:<22} dif = {c['mean_difference']:>7.4f}  "
                  f"p = {formata_p(c['pvalue'])}  IC95% = [{c['ci95'][0]:.4f}, {c['ci95'][1]:.4f}]")

    if rejeita:
        conclusao = ("Ao nível de 5%, rejeita-se a hipótese de que as três categorias tenham o "
                     "mesmo MPG médio: há evidência de que ao menos uma média difere.")
        if all(c["significant_5pct"] for c in tukey):
            ordem = sorted(nomes, key=lambda k: GRUPOS[k].mean(), reverse=True)
            conclusao += (" Pelo teste de Tukey, as três comparações par a par são significativas ("
                          + " > ".join(ROTULOS[k].lower() for k in ordem) + ").")
    else:
        conclusao = ("Ao nível de 5%, não há evidência suficiente de diferença entre as médias "
                     "de MPG das três categorias.")
    print(f"\n    Conclusão: {conclusao}")

    grafico_grupos(dict(zip(nomes, amostras)), anova, ARQUIVO_GRAFICO)
    grafico_qq(dict(zip(nomes, amostras)), normalidade, ARQUIVO_QQ)

    resultados = {
        "question": "Q3 - Teste ANOVA: categorias de carros",
        "data_source": "Listagem 1 do enunciado (Consumer Reports, abril de 1993)",
        "group_sizes": {k: int(x.size) for k, x in zip(nomes, amostras)},
        "group_means": {k: float(x.mean()) for k, x in zip(nomes, amostras)},
        "group_stds": {k: float(x.std(ddof=1)) for k, x in zip(nomes, amostras)},
        "group_variances": {k: float(x.var(ddof=1)) for k, x in zip(nomes, amostras)},
        "normality_alpha": ALFA,
        "normality_tests": normalidade,
        "levene_center": "mean",
        "levene_statistic": float(levene.statistic),
        "levene_pvalue": float(levene.pvalue),
        "levene_decision": ("Não rejeitar H0 (variâncias iguais)" if variancias_iguais
                            else "Rejeitar H0 (variâncias iguais)"),
        "levene_median_check": {
            "statistic": float(levene_mediana.statistic),
            "pvalue": float(levene_mediana.pvalue),
        },
        "max_min_std_ratio": float(razao_dp),
        "alpha": ALFA,
        "hypotheses": {
            "H0": "mu_compactos = mu_medios = mu_grandes",
            "Ha": "ao menos um par de médias difere",
        },
        "anova_table": anova,
        "anova_statistic": float(f_scipy.statistic),
        "anova_df": [anova["between"]["df"], anova["within"]["df"]],
        "anova_pvalue": float(f_scipy.pvalue),
        "f_critical": f_critico,
        "decision": decisao,
        "reject_h0": bool(rejeita),
        "tukey_hsd": tukey,
        "conclusion": conclusao,
        "figures": [ARQUIVO_GRAFICO.as_posix(), ARQUIVO_QQ.as_posix()],
    }
    with open(ARQUIVO_JSON, "w", encoding="utf-8") as f:
        json.dump(resultados, f, ensure_ascii=False, indent=2)

    print(f"\nArquivos gerados: {ARQUIVO_JSON.as_posix()}, {ARQUIVO_GRAFICO.as_posix()} e "
          f"{ARQUIVO_QQ.as_posix()}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
