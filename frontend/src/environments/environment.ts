/**
 * Em desenvolvimento, se você abrir o app pelo IP da rede (ex.: celular em `http://192.168.x.x:4200`),
 * API e WebSocket usam o mesmo host — não é preciso trocar arquivo ao mudar de máquina.
 * No PC com `http://localhost:4200`, continua apontando para `localhost:9090`.
 */
function devApiHost(): string {
  if (typeof window === 'undefined' || !window.location?.hostname) {
    return 'localhost';
  }
  const h = window.location.hostname;
  if (h === 'localhost' || h === '127.0.0.1' || h === '[::1]') {
    return 'localhost';
  }
  return h;
}

const host = devApiHost();

export const environment = {
  production: false,
  apiUrl: `http://${host}:9090`,
  wsUrl: `http://${host}:9090/ws`,
};
