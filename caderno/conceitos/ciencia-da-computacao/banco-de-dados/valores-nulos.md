---
conceito: Valores nulos (NULL)
area: ciencia-da-computacao
topico: banco-de-dados
disciplinas: [Projeto de Banco de Dados]
nivel: não avaliado
origem: anterior ao método (faculdade e Notion)
pre-requisitos: []
criado: 2026-11-17
---

# Valores nulos (NULL)

## Explicação

(Pendente: escrita pelo Matheus quando o conceito voltar num estudo.)

## Material de origem

### Faculdade

Comentários do Matheus nos arquivos da disciplina:

- `aula09062026.sql`, linha 15: "estas duas inserções abaixo possuem o mesmo efeito, posto que o último atributo permite a atribuição de um valor nulo"

  ```sql
  --insert into itemconsumo values (5,'água',5.00, 'bebida', null);
  insert into itemconsumo values (5,'água',5.00, 'bebida');
  ```

- `lista_exercicios_p2.sql`, linha 44: "podemos preencher o local e o publico_estimado após já termos criado a tabela desfile" (as duas colunas foram criadas com `null`)
- `resolucaoP2.sql`, linha 14: "o cadastro do custo total não é obrigatório"

Consultas com `IS NULL` e `IS NOT NULL`:

```sql
-- aula16062026.sql: itens que não têm descrição
select nomeitem from itemconsumo where descricao is null;

-- lista_exercicios_p2.sql: blocos sem trajeto definido
select id_bloco,nome from bloco_de_rua where trajeto is null;

-- resolucaoP2.sql: receitas com custo cadastrado
select custoTotal from Receita where custoTotal is not null;
```

Texto vazio não é o mesmo que nulo no PostgreSQL: a aula de 02/06/2026 insere um cliente com nome `''` (linha 60), e a aula de 09/06/2026 o apaga com `where nomecliente = ''` (linha 28).

### Notion

Transcrição de um print guardado no Notion (Área do Conhecimento › Ciência da Computação › Banco de Dados › Oracle): a resposta de uma IA à pergunta "O count() irá contar também as linhas que tiverem valor nulo?". O print original não foi guardado, porque mostrava dados de outro computador; aqui, o caminho do arquivo foi removido e os nomes da tabela e da coluna foram trocados por `tabela` e `coluna`.

> Depende de como você escreve o `COUNT`:
>
> | Forma | Conta linhas com NULL? | O que conta |
> |---|---|---|
> | `COUNT(*)` | Sim | Todas as linhas da tabela ou do resultado filtrado |
> | `COUNT(coluna)` | Não | Só as linhas em que aquela coluna **não** é NULL |
> | `COUNT(DISTINCT coluna)` | Não | Os valores diferentes e não nulos da coluna |
>
> Exemplo:
>
> ```sql
> -- Total de linhas da tabela (inclui as que têm coluna nula)
> select count(*) from tabela;
>
> -- Só as linhas em que coluna tem valor
> select count(coluna) from tabela;
> ```
>
> Se o primeiro der 1000 e o segundo der 950, existem 50 linhas com essa coluna nula. Você pode confirmar assim:
>
> ```sql
> select count(*) from tabela
> where coluna is null;
> ```
>
> **Detalhe do Oracle:** no Oracle, uma string vazia (`''`) é tratada como NULL. Por isso, `COUNT(coluna)` também deixa de fora as linhas com texto vazio em colunas `VARCHAR2`. Em outros bancos, como PostgreSQL e SQL Server, isso não acontece. `COUNT(1)` funciona igual ao `COUNT(*)`: conta todas as linhas e tem o mesmo desempenho no Oracle.

## Materiais

- [aula02062026.sql](../../../../faculdade/projeto_de_banco_de_dados/aula02062026.sql): colunas criadas com `null` e cliente inserido com nome vazio
- [aula09062026.sql](../../../../faculdade/projeto_de_banco_de_dados/aula09062026.sql): inserção que omite a coluna que aceita nulo
- [aula16062026.sql](../../../../faculdade/projeto_de_banco_de_dados/aula16062026.sql): consulta com `is null`
- [lista_exercicios_p2.sql](../../../../faculdade/projeto_de_banco_de_dados/lista_exercicios/lista_exercicios_p2.sql): colunas opcionais e consulta com `is null`
- [resolucaoP2.sql](../../../../faculdade/projeto_de_banco_de_dados/provas_antigas/p2_2024_1/resolucaoP2.sql): `drop not null` e consulta com `is not null`
- Página Oracle no Notion (Área do Conhecimento › Ciência da Computação › Banco de Dados), de onde veio o print.

## Perguntas de revisão

(Nenhuma: conceito anterior ao método.)
