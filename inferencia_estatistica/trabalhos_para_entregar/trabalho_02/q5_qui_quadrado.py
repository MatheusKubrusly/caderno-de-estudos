"""
GCC1625 - Inferência Estatística - Trabalho 02
Q5 - Teste qui-quadrado de independência

Estudo hipotético com 105 pacientes: 50 tratados com o medicamento e 55 no
grupo de controle. Depois de uma semana foi registrado se o paciente melhorou
ou não. O teste verifica se há associação entre as variáveis treatment e
improvement (data/treatment.csv).

Execução (a partir da raiz da entrega):
    python q5_qui_quadrado.py
"""

import json
import sys
from pathlib import Path

import matplotlib

matplotlib.use("Agg")  # só salva as figuras em arquivo (sem abrir janela)
import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
from scipy import stats

ARQUIVO_DADOS = Path("data") / "treatment.csv"
PASTA_RESULTADOS = Path("resultados")
ARQUIVO_JSON = PASTA_RESULTADOS / "q5_resultados.json"
ARQUIVO_GRAFICO = PASTA_RESULTADOS / "q5_contingencia.png"

ALFA = 0.05


def carregar_dados(caminho):
    dados = pd.read_csv(caminho)  # o arquivo usa CR como fim de linha; o pandas lida com isso
    dados.columns = [str(c).strip() for c in dados.columns]
    for col in ("treatment", "improvement"):
        dados[col] = dados[col].astype(str).str.strip()
    return dados


def classifica_phi(phi):
    """Faixas de referência para phi em tabelas 2x2 (notas de aula)."""
    if phi < 0.10:
        return "abaixo da referência de efeito pequeno"
    if phi < 0.30:
        return "pequena"
    if phi < 0.50:
        return "moderada"
    return "grande"


def tabela_para_dict(tabela):
    """DataFrame -> {linha: {coluna: valor}} com tipos nativos do Python."""
    return {str(i): {str(j): float(tabela.loc[i, j]) for j in tabela.columns}
            for i in tabela.index}


def grafico_contingencia(observada, esperada, resumo, caminho):
    linhas, colunas = list(observada.index), list(observada.columns)
    obs = observada.to_numpy(dtype=float)
    esp = esperada.to_numpy(dtype=float)
    fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(13, 5.4),
                                   gridspec_kw={"width_ratios": [1.15, 1]})

    # (a) tabela observada, com a frequência esperada sob H0 em cada célula
    ax1.imshow(obs, cmap="Blues", vmin=0, vmax=obs.max() * 1.25)
    for i in range(len(linhas)):
        for j in range(len(colunas)):
            cor = "white" if obs[i, j] > 0.75 * obs.max() else "#10283f"  # contraste
            ax1.text(j, i - 0.08, f"{obs[i, j]:.0f}", ha="center", va="center",
                     fontsize=22, fontweight="bold", color=cor)
            ax1.text(j, i + 0.22, f"esperado: {esp[i, j]:.2f}", ha="center", va="center",
                     fontsize=10, color=cor)
    ax1.set_xticks(range(len(colunas)))
    ax1.set_xticklabels([f"{c}\n(total = {obs[:, j].sum():.0f})" for j, c in enumerate(colunas)])
    ax1.set_yticks(range(len(linhas)))
    ax1.set_yticklabels([f"{r}\n(total = {obs[i].sum():.0f})" for i, r in enumerate(linhas)])
    ax1.set_xlabel("improvement")
    ax1.set_ylabel("treatment")
    ax1.set_title(f"Tabela de contingência observada (N = {obs.sum():.0f})")

    # (b) proporção de pacientes que melhoraram em cada grupo
    col_melhora = colunas.index("improved") if "improved" in colunas else 0
    proporcoes = obs[:, col_melhora] / obs.sum(axis=1)
    cores = ["#9bb7d4" if "not" in r else "#2f6ea8" for r in linhas]
    barras = ax2.bar(range(len(linhas)), proporcoes * 100, color=cores, width=0.55)
    for b, p, i in zip(barras, proporcoes, range(len(linhas))):
        ax2.text(b.get_x() + b.get_width() / 2, b.get_height() / 2,
                 f"{p:.1%}\n({obs[i, col_melhora]:.0f} de {obs[i].sum():.0f})",
                 ha="center", va="center", fontsize=11,
                 color="#10283f" if "not" in linhas[i] else "white")
    taxa_geral = obs[:, col_melhora].sum() / obs.sum()
    ax2.axhline(taxa_geral * 100, color="gray", ls="--", lw=1.2,
                label=f"taxa geral = {taxa_geral:.1%}")
    ax2.set_xticks(range(len(linhas)))
    ax2.set_xticklabels(linhas)
    ax2.set_ylim(0, 100)
    ax2.set_ylabel(f"% de pacientes '{colunas[col_melhora]}'")
    ax2.set_title("Proporção de pacientes que melhoraram")
    ax2.legend(loc="upper left")
    ax2.grid(axis="y", alpha=0.3)

    fig.suptitle(f"Q5 – treatment × improvement ({resumo})", fontsize=12)
    fig.tight_layout()
    fig.savefig(caminho, dpi=150)
    plt.close(fig)


def main():
    if not ARQUIVO_DADOS.exists():
        print(f"Arquivo de dados não encontrado: {ARQUIVO_DADOS}", file=sys.stderr)
        return 1
    PASTA_RESULTADOS.mkdir(parents=True, exist_ok=True)

    dados = carregar_dados(ARQUIVO_DADOS)

    # (i) tabela de contingência observada (mesma construção da Listagem 2)
    observada = pd.crosstab(dados["treatment"], dados["improvement"], margins=False)
    N = int(observada.to_numpy().sum())
    print("Q5 - Teste qui-quadrado de independência: treatment x improvement")
    print(f"\n(i) Tabela de contingência observada (N = {N})")
    print(pd.crosstab(dados["treatment"], dados["improvement"], margins=True,
                      margins_name="Total").to_string())

    # (ii) H0: treatment e improvement são independentes; Ha: há associação
    print("\n(ii) H0: as variáveis treatment e improvement são independentes")
    print("     Ha: as variáveis são dependentes (há associação entre tratamento e melhora)")

    # estatística de Pearson sem correção de continuidade (como nas notas de aula)
    chi2, p_valor, gl, esperado = stats.chi2_contingency(observada, correction=False)
    esperada = pd.DataFrame(esperado, index=observada.index, columns=observada.columns)
    obs = observada.to_numpy(dtype=float)
    contribuicoes = (obs - esperado) ** 2 / esperado
    chi2_manual = float(contribuicoes.sum())
    residuos = (obs - esperado) / np.sqrt(esperado)
    chi2_critico = float(stats.chi2.ppf(1 - ALFA, gl))
    phi = float(np.sqrt(chi2 / N))
    menor_esperado = float(esperado.min())
    rejeita = float(p_valor) <= ALFA

    # conferência com a correção de Yates (alternativa conservadora para tabelas 2x2)
    chi2_yates, p_yates, _, _ = stats.chi2_contingency(observada, correction=True)

    print("\n     Frequências esperadas sob H0 (E_ij = total_linha * total_coluna / N)")
    print(esperada.round(4).to_string())
    print(f"     Menor frequência esperada = {menor_esperado:.4f} "
          f"({'todas' if menor_esperado >= 5 else 'nem todas'} >= 5)")

    print("\n(iii) Estatística qui-quadrado de Pearson: soma de (O - E)^2 / E")
    for i, linha in enumerate(observada.index):
        for j, coluna in enumerate(observada.columns):
            print(f"     {linha:>11} / {coluna:<12} O = {obs[i, j]:>4.0f}  E = {esperado[i, j]:>8.4f}  "
                  f"(O-E)^2/E = {contribuicoes[i, j]:.4f}  resíduo = {residuos[i, j]:+.4f}")
    print(f"     chi2 = {chi2_manual:.4f}  (scipy: {float(chi2):.4f})")
    print(f"(iv) Graus de liberdade = (2 - 1) x (2 - 1) = {int(gl)}")
    print(f"(v)  p-valor = P(chi2_{int(gl)} > {float(chi2):.4f}) = {float(p_valor):.6f}"
          f"   (valor crítico a 5% = {chi2_critico:.4f})")

    decisao = "Rejeitar H0" if rejeita else "Não rejeitar H0"
    # categorias "positivas" (sem o prefixo not-): tratado e melhorou
    col_melhora = next(c for c in observada.columns if not c.lower().startswith("not"))
    tratado = next(r for r in observada.index if not r.lower().startswith("not"))
    controle = next(r for r in observada.index if r != tratado)
    taxa = observada[col_melhora] / observada.sum(axis=1)
    magnitude = classifica_phi(phi)
    if rejeita:
        conclusao = (
            "Ao nível de 5%, rejeita-se a hipótese de independência: há evidência de associação "
            "entre receber o medicamento e apresentar melhora após uma semana. A melhora foi mais "
            f"frequente entre os tratados ({taxa[tratado]:.1%}) do que no grupo de controle "
            f"({taxa[controle]:.1%}); pelo coeficiente phi = {phi:.3f}, a associação é de "
            f"magnitude {magnitude}."
        )
    else:
        conclusao = ("Ao nível de 5%, os dados não fornecem evidência suficiente de associação "
                     "entre o tratamento e a melhora dos pacientes.")
    print(f"(vi) p-valor {'<=' if rejeita else '>'} {ALFA} -> {decisao}")
    print(f"     phi = sqrt(chi2 / N) = {phi:.4f} (associação {magnitude})")
    print(f"     Conferência com correção de Yates: chi2 = {float(chi2_yates):.4f}, "
          f"p = {float(p_yates):.6f}")
    print(f"     Conclusão: {conclusao}")

    resumo = (f"χ² = {float(chi2):.3f}, gl = {int(gl)}, p = {float(p_valor):.4f}, "
              f"φ = {phi:.3f}; α = 5% → {decisao}")
    grafico_contingencia(observada, esperada, resumo, ARQUIVO_GRAFICO)

    resultados = {
        "question": "Q5 - Teste qui-quadrado de independência",
        "data_file": str(ARQUIVO_DADOS.as_posix()),
        "row_variable": "treatment",
        "column_variable": "improvement",
        "n_total": N,
        "observed_table": {i: {j: int(observada.loc[i, j]) for j in observada.columns}
                           for i in observada.index},
        "row_totals": {i: int(v) for i, v in observada.sum(axis=1).items()},
        "column_totals": {j: int(v) for j, v in observada.sum(axis=0).items()},
        "expected_table": tabela_para_dict(esperada),
        "min_expected_frequency": menor_esperado,
        "all_expected_at_least_5": bool(menor_esperado >= 5),
        "hypotheses": {
            "H0": "treatment e improvement são independentes",
            "Ha": "treatment e improvement são dependentes (há associação)",
        },
        "test": "qui-quadrado de independência de Pearson (sem correção de continuidade)",
        "chi2_statistic": float(chi2),
        "chi2_cell_contributions": tabela_para_dict(
            pd.DataFrame(contribuicoes, index=observada.index, columns=observada.columns)),
        "standardized_residuals": tabela_para_dict(
            pd.DataFrame(residuos, index=observada.index, columns=observada.columns)),
        "df": int(gl),
        "pvalue": float(p_valor),
        "alpha": ALFA,
        "chi2_critical": chi2_critico,
        "decision": decisao,
        "reject_h0": bool(rejeita),
        "improvement_rate_by_group": {i: float(v) for i, v in taxa.items()},
        "phi_coefficient": phi,
        "phi_magnitude": magnitude,
        "yates_correction_check": {
            "chi2_statistic": float(chi2_yates),
            "pvalue": float(p_yates),
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
