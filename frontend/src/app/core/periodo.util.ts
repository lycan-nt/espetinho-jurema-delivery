/** Início do dia local (00:00) e fim exclusivo (meia-noite do dia seguinte), em ISO-8601. */
export function periodoDiaLocal(date: Date): { inicio: string; fim: string } {
  const y = date.getFullYear();
  const m = date.getMonth();
  const d = date.getDate();
  const inicio = new Date(y, m, d, 0, 0, 0, 0);
  const fim = new Date(y, m, d + 1, 0, 0, 0, 0);
  return { inicio: inicio.toISOString(), fim: fim.toISOString() };
}

/** Primeiro e último instante do mês local (fim exclusivo = 1º dia do mês seguinte). */
export function periodoMesLocal(date: Date): { inicio: string; fim: string } {
  const y = date.getFullYear();
  const m = date.getMonth();
  const inicio = new Date(y, m, 1, 0, 0, 0, 0);
  const fim = new Date(y, m + 1, 1, 0, 0, 0, 0);
  return { inicio: inicio.toISOString(), fim: fim.toISOString() };
}
