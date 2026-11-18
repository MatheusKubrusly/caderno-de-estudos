---
conceito: Chave primária e chave estrangeira
area: ciencia-da-computacao
topico: banco-de-dados
disciplinas: [Projeto de Banco de Dados]
nivel: não avaliado
origem: anterior ao método (faculdade)
pre-requisitos: []
criado: 2026-11-17
---

# Chave primária e chave estrangeira

## Explicação

(Pendente: escrita pelo Matheus quando o conceito voltar num estudo.)

## Material de origem

Comentários do Matheus nos arquivos da disciplina:

- `aula02062026.sql`, linha 35: "uma chave estrangeira só existe se o atributo referenciado for uma chave primária!"
- `aula09062026.sql`, linha 33: "como não temos nenhum cliente com o respectivo id = 33, teremos um problema de referenciamento, pois este id é uma chave estrangeira que faz referência a cliente(id)"
- `lista_exercicios_p2.sql`, linha 28: "obrigatoriamente terei que ter um organizador cadastrado para que eu possa vir a cadastrar um bloco_de_rua"
- `lista_exercicios_p2.sql`, linha 40: "caso tenhamos alguma tabela que utilize de uma outra dentro de sua chave estrangeira, precisamos declarar as tabelas que irão ser utilizadas antes!"

Exemplo da aula de 02/06/2026, com chave primária composta (duas colunas) e chave estrangeira:

```sql
create table telcliente (
idcliente integer not null,
telefone char(11) not null,
primary key(idcliente, telefone),
foreign key(idcliente) references cliente(idcliente)
);
```

Observação do Claude: no PostgreSQL, a chave estrangeira pode apontar para a chave primária ou para qualquer coluna com restrição `UNIQUE` ([documentação do PostgreSQL](https://www.postgresql.org/docs/current/ddl-constraints.html#DDL-CONSTRAINTS-FK)). O comentário da linha 35 vale para o caso mais comum, mas não é a regra completa.

## Materiais

- [aula02062026.sql](../../../../faculdade/projeto_de_banco_de_dados/aula02062026.sql): criação das tabelas do restaurante, com chaves primárias simples e compostas
- [aula09062026.sql](../../../../faculdade/projeto_de_banco_de_dados/aula09062026.sql): inserção que falha por chave estrangeira sem correspondente
- [lista_exercicios_p2.sql](../../../../faculdade/projeto_de_banco_de_dados/lista_exercicios/lista_exercicios_p2.sql): tabelas do carnaval, com chave estrangeira como parte da especialização
- [resolucaoP2.sql](../../../../faculdade/projeto_de_banco_de_dados/provas_antigas/p2_2024_1/resolucaoP2.sql): tabela `Ingrediente`, com chave primária composta por duas chaves estrangeiras

## Perguntas de revisão

(Nenhuma: conceito anterior ao método.)
