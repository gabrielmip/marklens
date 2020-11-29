# Marklens

[Leia em português](README-BR.md)

Marklens indexes the contents of your web browser bookmarked pages, enhancing your search results.

## Instalation

There is no installer for now. Make sure you have Java 8+, download the latest release in the Releases page, unpack it and you are done.

## Usage

Find your web browser's JSON bookmark file. This file's path will be refered as `$BOOKMARK_FILE` below.

Index your bookmarked pages:

```bash
java -jar marklens.jar index $BOOKMARK_FILE
```
Now, search. This example searches for pages about Plato.

```
> java -jar marklens.jar search plato

2. reference request - Most effective ways to self-learn philosophy - Philosophy Stack Exchange
   URL: https://philosophy.stackexchange.com/questions/36953/most-effective-ways-to-self-learn-philosophy/36961#36961
   Folder: Bookmarks > Arquivo

1. On compositionality – Jules Hedges
   URL: https://julesh.com/2017/04/22/on-compositionality/
   Folder: Bookmarks > Para depois
```

Get help:

```bash
java -jar marklens.jar help
```
