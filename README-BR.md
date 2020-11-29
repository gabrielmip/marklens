# Marklens

[Read in English](README.md)

Marklens cria um índice do conteúdo das páginas dos favoritos do seu navegador, melhorando os resultados das suas buscas.

## Instalação

Não há instalador por ora. Garanta que você possui o Java 8+, baixe a última versão na página Releases, descomprima-o e pronto.

## Uso

Encontre o arquivo JSON com os favoritos do seu navegador. O caminho para este arquivo será referenciado abaixo por `$BOOKMARK_FILE`.

Crie o índice das páginas favoritadas:

```bash
java -jar marklens.jar index $BOOKMARK_FILE
```

Agora, busque. Este exemplo busca por páginas sobre Platão.

```
> java -jar marklens.jar search platão

3. Farofa Filosófica – Ciências Humanas em debate: conteúdo para descascar abacaxis…
   URL: https://farofafilosofica.com/
   Folder: Bookmarks > Blogs

2. Livro & Café - por meio dos livros, todos os caminhos
   URL: https://livroecafe.com/
   Folder: Bookmarks > Blogs

1. Como ficam as grandes questões da humanidade durante a pandemia
   URL: https://tab.uol.com.br/edicao/filosofia-e-pandemia/index.htm#cover
   Folder: Bookmarks > Para depois
```

Encontre ajuda:

```bash
java -jar marklens.jar help
```
