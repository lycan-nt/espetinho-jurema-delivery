/**
 * Fallback quando não há impressão no servidor: bobina ~76&nbsp;mm de largura.
 *
 * Fonte **24px** monoespaçada (bem maior que o baseline antigo), **alinhada à esquerda**, + área mínima alta e rodapé em branco.
 *
 * Abre uma **nova aba/janela** com HTML via `blob:` e dispara `print()` **somente dentro dessa janela**.
 * Evita chamar `print()` no contexto da SPA: no Chrome isso costuma bloquear o processo compartilhado e
 * “travar” a tela do balcão até o diálogo de impressão fechar.
 */
export function imprimirTextoTerminalBrowser(texto: string, tituloDocumento: string): void {
  const titulo = escapeHtml(tituloDocumento);
  const textoJson = JSON.stringify(texto ?? '');

  const html = `<!DOCTYPE html>
<html lang="pt-BR">
<head>
  <meta charset="utf-8"/>
  <meta name="viewport" content="width=device-width, initial-scale=1"/>
  <title>${titulo}</title>
  <style>
    @page {
      margin: 2mm;
      size: 76mm auto;
    }
    html, body {
      margin: 0;
      padding: 0;
      width: 76mm;
      max-width: 76mm;
      box-sizing: border-box;
      text-align: left;
    }
    body {
      height: auto;
      font: 24px/1.36 ui-monospace, Consolas, "Courier New", monospace;
      color: #000;
      background: #fff;
      -webkit-print-color-adjust: exact;
      print-color-adjust: exact;
    }
    pre {
      margin: 0;
      box-sizing: border-box;
      min-height: 110mm;
      padding: 4mm 2mm 42mm 2mm;
      white-space: pre-wrap;
      word-break: break-word;
      font: inherit;
      letter-spacing: 0;
      text-align: left;
      width: 100%;
    }
    @media print {
      html, body {
        width: 76mm;
        max-width: 76mm;
        text-align: left;
      }
      body, pre {
        font-size: 24px !important;
        line-height: 1.36 !important;
      }
      pre {
        min-height: 110mm !important;
        padding-top: 4mm !important;
        padding-bottom: 42mm !important;
        text-align: left !important;
      }
      @page {
        margin: 2mm;
        size: 76mm auto;
      }
    }
  </style>
</head>
<body>
  <pre id="pj-print-root"></pre>
  <script>
(function () {
  var root = document.getElementById('pj-print-root');
  if (root) root.textContent = ${textoJson};
  function imprimir() {
    setTimeout(function () {
      window.focus();
      window.print();
    }, 100);
  }
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', imprimir, { once: true });
  } else {
    imprimir();
  }
})();
  </script>
</body>
</html>`;

  const blob = new Blob([html], { type: 'text/html;charset=utf-8' });
  const url = URL.createObjectURL(blob);

  const popup = window.open(
    url,
    '_blank',
    'popup=yes,width=520,height=780,noopener,noreferrer',
  );

  if (!popup) {
    URL.revokeObjectURL(url);
    return;
  }

  window.setTimeout(() => URL.revokeObjectURL(url), 120_000);
}

function escapeHtml(s: string): string {
  return s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}
