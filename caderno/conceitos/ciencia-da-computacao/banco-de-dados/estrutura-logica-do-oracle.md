---
conceito: Estrutura lógica do Oracle (tablespace, segmento, extensão e bloco)
area: ciencia-da-computacao
topico: banco-de-dados
disciplinas: []
nivel: não avaliado
origem: anterior ao método (Notion)
pre-requisitos: []
criado: 2026-11-17
---

# Estrutura lógica do Oracle (tablespace, segmento, extensão e bloco)

## Explicação

(Pendente: escrita pelo Matheus quando o conceito voltar num estudo.)

## Material de origem

Transcrição de três prints guardados no Notion (Área do Conhecimento › Ciência da Computação › Banco de Dados › Oracle), do artigo "Introdução ao conceito de Tablespaces", de Eduardo Legatti, publicado em setembro de 2011. Os prints originais estão em [anexos/](anexos/), na ordem do artigo.

**Print 1** ([estrutura-logica-do-oracle-1.png](anexos/estrutura-logica-do-oracle-1.png)):

> Comecemos com a seguinte pergunta: Qual é a relação entre tablespaces e arquivos de dados? Bom, para responder esta pergunta, primeiro precisamos entender como os dados são armazenados no banco de dados. O Oracle armazena dados logicamente em tablespaces e fisicamente em arquivos de dados (datafiles). Apesar dos arquivos de dados e os tablespaces estarem muito "inter-relacionados", os mesmos possuem diferenças importantes:
>
> Um banco de dados Oracle consiste em uma ou mais unidades de armazenamento lógicas denominadas tablespaces, que armazenam coletivamente todos os dados do banco de dados.
>
> Cada tablespace em um banco de dados Oracle consiste em um ou mais arquivos denominados arquivos de dados (datafiles), que são estruturas físicas compatíveis com o sistema operacional no qual o Oracle é executado.
>
> [?] (a linha seguinte está cortada no print)

**Print 2** ([estrutura-logica-do-oracle-2.png](anexos/estrutura-logica-do-oracle-2.png)):

> Os dados de um banco de dados são armazenados coletivamente nos arquivos de dados que constituem cada tablespace do banco de dados. Como um banco de dados é um conjunto de arquivos de dados, é muito importante que entendamos como um banco de dados Oracle agrupa esses arquivos. Como dito anteriormente, o Oracle faz isso sob a proteção de um objeto de banco de dados chamado tablespace. Antes de poder inserir dados em um banco de dados Oracle, primeiro é necessário criar um tablespace e depois uma tabela dentro desse tablespace que conterá os dados. Podemos observar que na criação de um banco de dados utilizando o **DBCA**, o Oracle como padrão sempre cria um tablespace de dados chamado USERS. Ao criar uma tabela é necessário incluir todas as informações sobre o tipo de dados que deseja manter. O código abaixo, gerado para criar a tabela **CLIENTE**, ilustra como o Oracle armazena informações sobre o tipo de dado que irá registrar:

```text
SQL> create table cliente
  2  (cod_cliente   number constraint pk_cliente primary key,
  3   nome          varchar2(60)  not null,
  4   endereco      varchar2(100) not null,
  5   telefone      number,
  6   data_cadastro date)
  7  tablespace users;

Tabela criada.

SQL> desc cliente
Nome                           Nulo?    Tipo
------------------------------ -------- --------------------
COD_CLIENTE                    NOT NULL NUMBER
NOME                           NOT NULL VARCHAR2(60)
ENDERECO                       NOT NULL VARCHAR2(100)
TELEFONE                                NUMBER
DATA_CADASTRO                           DATE

SQL> select table_name,tablespace_name
  2  from user_tables
  3  where table_name='CLIENTE';

TABLE_NAME                     TABLESPACE_NAME
------------------------------ ------------------------------
CLIENTE                        USERS
```

**Print 3** ([estrutura-logica-do-oracle-3.png](anexos/estrutura-logica-do-oracle-3.png)):

> Na instrução acima, foi criada uma tabela que é o meio mais comum de armazenar dados em um banco de dados. Os dados de um segmento de tabela são armazenados aleatoriamente no tablespace e o DBA tem pouco controle sobre a localização das linhas dos blocos de uma tabela. Por falar nisso, o que é um segmento? Os segmentos são objetos que ocupam espaço em um banco de dados. Existem vários tipos de segmentos como tabelas, índices, de undo, temporários, LOB, entre outros. Já uma extensão (extent), é um espaço usado por um segmento em um tablespace. Para terminar, um bloco Oracle consiste em um ou mais blocos do sistema operacional e seu tamanho é definido na criação do tablespace. Então a estrutura lógica de um banco de dados Oracle se resume em tablespaces que contém segmentos que contém extensões que contém blocos. A figura abaixo ilustra esta estrutura lógica:

A figura mostra, de fora para dentro: o **Database** com três tablespaces (SYSTEM, 1 e 2); um **Tablespace** com três segmentos; um **Segment** com duas extensões (Extent 1 e Extent 2); e cada extensão dividida em **Data Blocks**.

```text
SQL> select segment_name, segment_type, tablespace_name,
  2  bytes, blocks, extents
  3  fromuser_segments
  4  where segment_name='CLIENTE';

SEGMENT_NAME   SEGMENT_TYPE TABLESPACE_NAME  BYTES    BLOCKS  EXTENTS
-------------- ------------ ---------------- -------- ------- ---------
CLIENTE        TABLE        USERS               65536       8         1
```

Na linha 3 da consulta, o artigo traz `fromuser_segments`, sem o espaço; o correto é `from user_segments`.

## Materiais

- Página Oracle no Notion (Área do Conhecimento › Ciência da Computação › Banco de Dados), de onde vieram os prints.

## Perguntas de revisão

(Nenhuma: conceito anterior ao método.)
