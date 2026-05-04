# NFC-e em Homologação (Bahia)

## O que foi implementado

- Geração do XML **infNFe 4.0** a partir de uma comanda fechada (um item: açaí por kg).
- Assinatura digital com **certificado A1 (PFX)** (Apache Santuario).
- Envio ao **Web Service de autorização** em ambiente de **homologação** (SVRS – utilizado pela Bahia para NFC-e).
- Gravação da **chave** e do **protocolo** na comanda após autorização.

## Configuração

1. **Certificado de homologação**  
   Obtenha o certificado de homologação no portal da SEFAZ-BA (área do desenvolvedor / NFC-e) ou use um PFX de teste. Salve o arquivo (ex.: `certificado-homologacao.pfx`) em um diretório acessível pelo backend.

2. **application.properties** (ou variáveis de ambiente):

```properties
app.nfce.habilitado=true
app.nfce.ambiente=HOMOLOGACAO
app.nfce.certificado-path=file:./certificado-homologacao.pfx
app.nfce.certificado-senha=senha_do_pfx
app.nfce.emitente-cnpj=00000000000000
```

Preencha:
- `emitente-cnpj`: 14 dígitos (pode ser CNPJ de teste em homologação).
- `emitente-razao-social`, `emitente-fantasia`, `emitente-municipio`, `emitente-uf` (BA), `emitente-codigo-municipio` (IBGE 7 dígitos, ex.: Salvador 2927408).

3. **URL de autorização**  
   Padrão: `https://nfce-homologacao.svrs.rs.gov.br/ws/NfeAutorizacao/NFeAutorizacao4.asmx` (homologação SVRS). A Bahia utiliza o SVRS para NFC-e; se a SEFAZ-BA indicar outro endpoint, altere `app.nfce.url-autorizacao`.

## Uso

1. Feche uma comanda (PATCH `/api/comandas/{id}/fechar` com `formaPagamento`).
2. Emita a NFC-e: POST `/api/comandas/{id}/nfce/emitir`.
3. A resposta devolve a comanda com `chaveNfce` e `protocoloNfce` preenchidos.

## Observações

- Em homologação o CNPJ pode ser de teste; consulte o manual da SEFAZ-BA.
- A numeração (`app.nfce.proximo-numero`) é atualizada em memória; em produção use persistência (arquivo ou banco).
- Para produção: troque para certificado e ambiente de produção e confirme o autorizador (SEFAZ-BA ou SVRS) conforme documentação oficial.
