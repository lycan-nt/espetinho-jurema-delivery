/**
 * sockjs-client e dependências antigas assumem `global` (Node).
 * No browser isso não existe — define antes de carregar a app.
 */
const g = globalThis as typeof globalThis & { global?: typeof globalThis };
if (g.global === undefined) {
  g.global = globalThis;
}
